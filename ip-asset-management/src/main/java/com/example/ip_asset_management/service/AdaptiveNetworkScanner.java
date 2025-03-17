package com.example.ip_asset_management.service;

import com.example.ip_asset_management.model.Asset;
import com.example.ip_asset_management.model.AssetType;
import com.example.ip_asset_management.model.ScanJob;
import com.example.ip_asset_management.model.ScanJobStatus;
import com.example.ip_asset_management.model.ScanResult;
import com.example.ip_asset_management.repository.AssetRepository;
import com.example.ip_asset_management.repository.ScanJobRepository;
import com.example.ip_asset_management.repository.ScanResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AdaptiveNetworkScanner {
    private static final Logger logger = LoggerFactory.getLogger(AdaptiveNetworkScanner.class);
    
    @Autowired
    private ResourceAwareExecutionController resourceController;
    
    @Autowired
    private ScanJobRepository scanJobRepository;
    
    @Autowired
    private ScanResultRepository scanResultRepository;
    
    @Autowired
    private AssetRepository assetRepository;
    
    @Autowired
    private WindowsScannerService windowsScannerService;
    
    /**
     * Starts a scan job with adaptive performance based on system resources
     */
    @Async("scanTaskExecutor")
    public CompletableFuture<ScanJob> executeScanJob(String scanJobId) {
        ScanJob job = scanJobRepository.findById(scanJobId)
                .orElseThrow(() -> new RuntimeException("Scan job not found"));
        
        try {
            job.setStatus(ScanJobStatus.RUNNING);
            job.setLastRunAt(LocalDateTime.now());
            scanJobRepository.save(job);
            
            // Get all targets to scan (IPs and IP segments)
            List<String> allTargets = getAllTargets(job);
            int totalTargets = allTargets.size();
            
            job.setTotalTargets(totalTargets);
            job.setCompletedTargets(0);
            job.setSuccessfulTargets(0);
            job.setFailedTargets(0);
            scanJobRepository.save(job);
            
            logger.info("Starting scan job {} with {} targets", scanJobId, totalTargets);
            
            // Process in batches based on system resources
            int processed = 0;
            int successful = 0;
            int failed = 0;
            
            while (processed < totalTargets) {
                // Get current optimal batch size
                int batchSize = resourceController.getMaxBatchSize();
                int remaining = totalTargets - processed;
                int currentBatchSize = Math.min(batchSize, remaining);
                
                List<String> batch = allTargets.subList(processed, processed + currentBatchSize);
                logger.debug("Processing batch of {} IPs from job {}", batch.size(), scanJobId);
                
                // Process this batch in parallel
                List<CompletableFuture<ScanResult>> futures = batch.stream()
                    .map(ip -> CompletableFuture.supplyAsync(() -> scanIpAddress(scanJobId, ip)))
                    .collect(Collectors.toList());
                
                // Wait for all scans in this batch to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                // Process results from this batch
                for (CompletableFuture<ScanResult> future : futures) {
                    try {
                        ScanResult result = future.get();
                        if (result.isSuccessful()) {
                            successful++;
                        } else {
                            failed++;
                        }
                    } catch (Exception e) {
                        logger.error("Error processing scan result", e);
                        failed++;
                    }
                }
                
                processed += batch.size();
                
                // Update job progress
                job.setCompletedTargets(processed);
                job.setSuccessfulTargets(successful);
                job.setFailedTargets(failed);
                scanJobRepository.save(job);
                
                // Check if job was cancelled
                ScanJob currentState = scanJobRepository.findById(scanJobId).orElse(job);
                if (currentState.getStatus() == ScanJobStatus.CANCELLED) {
                    logger.info("Scan job {} was cancelled", scanJobId);
                    return CompletableFuture.completedFuture(currentState);
                }
            }
            
            // Complete the job
            job.setStatus(ScanJobStatus.COMPLETED);
            if (job.isRecurring()) {
                // Set next run time based on schedule
                // For simplicity, just adding 24 hours
                job.setNextRunAt(LocalDateTime.now().plusHours(24));
            }
            
            ScanJob completedJob = scanJobRepository.save(job);
            logger.info("Completed scan job {} with {} successful and {} failed targets", 
                     scanJobId, successful, failed);
            
            return CompletableFuture.completedFuture(completedJob);
            
        } catch (Exception e) {
            logger.error("Error executing scan job {}: {}", scanJobId, e.getMessage());
            job.setStatus(ScanJobStatus.FAILED);
            ScanJob failedJob = scanJobRepository.save(job);
            return CompletableFuture.completedFuture(failedJob);
        }
    }
    
    /**
     * Scan a single IP address and capture all required system information
     */
    private ScanResult scanIpAddress(String scanJobId, String ipAddress) {
        ScanResult result = new ScanResult();
        result.setScanJobId(scanJobId);
        result.setIpAddress(ipAddress);
        result.setScanTime(LocalDateTime.now());
        
        Map<String, Object> collectedData = new HashMap<>();
        
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            boolean reachable = address.isReachable(5000); // 5 second timeout
            
            result.setSuccessful(true);
            String hostname = address.getHostName();
            result.setHostname(hostname);
            
            collectedData.put("pingable", reachable);
            collectedData.put("hostname", hostname);
            collectedData.put("canonicalHostname", address.getCanonicalHostName());
            
            // Enhanced port scanning
            Map<Integer, String> commonPorts = new HashMap<>();
            commonPorts.put(21, "FTP");
            commonPorts.put(22, "SSH");
            commonPorts.put(23, "Telnet");
            commonPorts.put(25, "SMTP");
            commonPorts.put(53, "DNS");
            commonPorts.put(80, "HTTP");
            commonPorts.put(443, "HTTPS");
            commonPorts.put(445, "SMB");
            commonPorts.put(3389, "RDP");
            
            Map<String, Boolean> openPorts = new HashMap<>();
            for (Map.Entry<Integer, String> entry : commonPorts.entrySet()) {
                boolean isOpen = isPortOpen(ipAddress, entry.getKey());
                openPorts.put(entry.getValue(), isOpen);
            }
            
            collectedData.put("openPorts", openPorts);
            
            // Try to determine asset type and OS
            String hostLower = hostname.toLowerCase();
            AssetType assetType = determineAssetType(hostname, openPorts);
            String operatingSystem = determineOperatingSystem(hostname, openPorts);
            String osVersion = "Unknown";
            
            // MAC-SPECIFIC SCANNING ENHANCEMENT
            if (assetType == AssetType.MAC) {
                // Enhanced Mac detection
                collectedData.put("osFamily", "macOS");
                operatingSystem = "macOS";
                
                // Try alternative Mac scanning techniques
                try {
                    // Attempt to ping specific Mac ports
                    boolean sshOpen = isPortOpen(ipAddress, 22);
                    boolean airplayOpen = isPortOpen(ipAddress, 7000);
                    boolean bonjourOpen = isPortOpen(ipAddress, 5353);
                    
                    openPorts.put("SSH", sshOpen);
                    openPorts.put("AirPlay", airplayOpen);
                    openPorts.put("Bonjour", bonjourOpen);
                    
                    // Mac-specific data inferences
                    String macType = "Mac";
                    if (hostLower.contains("macbook")) {
                        macType = "MacBook";
                        collectedData.put("macType", "MacBook");
                        collectedData.put("model", "MacBook");
                    } else if (hostLower.contains("imac")) {
                        macType = "iMac";
                        collectedData.put("macType", "iMac");
                        collectedData.put("model", "iMac");
                    } else if (hostLower.contains("mac mini")) {
                        macType = "Mac Mini";
                        collectedData.put("macType", "Mac Mini");
                        collectedData.put("model", "Mac Mini");
                    } else if (hostLower.contains("macpro")) {
                        macType = "Mac Pro";
                        collectedData.put("macType", "Mac Pro");
                        collectedData.put("model", "Mac Pro");
                    } else {
                        collectedData.put("macType", "Mac");
                        collectedData.put("model", "Mac");
                    }
                    
                    // Attempt to gather macOS version from network response timing
                    // This is a heuristic - different macOS versions have different response patterns
                    if (sshOpen) {
                        long sshResponseTime = measureResponseTime(ipAddress, 22);
                        
                        // Simple heuristic based on connection timing profiles (ms)
                        if (sshResponseTime < 20) {
                            collectedData.put("osVersionGuess", "Likely macOS 14 (Sonoma)");
                            collectedData.put("osVersion", "macOS 14 (Sonoma) - Estimated");
                        } else if (sshResponseTime < 30) {
                            collectedData.put("osVersionGuess", "Likely macOS 13 (Ventura)");
                            collectedData.put("osVersion", "macOS 13 (Ventura) - Estimated");
                        } else {
                            collectedData.put("osVersionGuess", "Likely macOS 12 or earlier");
                            collectedData.put("osVersion", "macOS 12 or earlier - Estimated");
                        }
                    }
                    
                    // Set manufacturer to Apple for all Macs
                    collectedData.put("manufacturer", "Apple Inc.");
                    
                } catch (Exception e) {
                    logger.warn("Enhanced Mac scanning failed: {}", e.getMessage());
                }
            }
            // END OF MAC-SPECIFIC ENHANCEMENT
            
            // If Windows detection, use the WindowsScannerService for detailed info
            if (assetType == AssetType.WINDOWS || openPorts.get("RDP") || openPorts.get("SMB")) {
                try {
                    Map<String, Object> windowsInfo = windowsScannerService.getWindowsSystemInfo(ipAddress);
                    if (windowsInfo != null && !windowsInfo.isEmpty()) {
                        // Update OS info if available
                        if (windowsInfo.containsKey("osName")) {
                            operatingSystem = (String) windowsInfo.get("osName");
                        }
                        if (windowsInfo.containsKey("osVersion")) {
                            osVersion = (String) windowsInfo.get("osVersion");
                        }
                        
                        // Add all Windows info to collected data
                        collectedData.putAll(windowsInfo);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to get detailed Windows information from " + ipAddress, e);
                }
            }
            
            // Create or update asset in database
            Optional<Asset> existingAsset = assetRepository.findByIpAddress(ipAddress);
            Asset asset;
            
            if (existingAsset.isPresent()) {
                asset = existingAsset.get();
                asset.setLastSeen(LocalDateTime.now());
                asset.setOnline(reachable);
                asset.setHostname(hostname);
                asset.setLastScanId(scanJobId);
                
                // Update with more info if available
                if (assetType != AssetType.UNKNOWN) {
                    asset.setAssetType(assetType);
                }
                if (!"Unknown".equals(operatingSystem)) {
                    asset.setOperatingSystem(operatingSystem);
                }
                if (!"Unknown".equals(osVersion)) {
                    asset.setOsVersion(osVersion);
                }
                
                // Update hardware info if available
                if (collectedData.containsKey("manufacturer")) {
                    asset.setManufacturer((String) collectedData.get("manufacturer"));
                }
                if (collectedData.containsKey("model")) {
                    asset.setModel((String) collectedData.get("model"));
                }
                if (collectedData.containsKey("macAddress")) {
                    asset.setMacAddress((String) collectedData.get("macAddress"));
                }
                if (collectedData.containsKey("osVersion")) {
                    asset.setOsVersion((String) collectedData.get("osVersion"));
                }
                
                // Update additional info
                Map<String, Object> additionalInfo = asset.getAdditionalInfo();
                if (additionalInfo == null) {
                    additionalInfo = new HashMap<>();
                }
                
                // Add specific hardware details we want to track
                if (collectedData.containsKey("cpuModel")) {
                    additionalInfo.put("cpuModel", collectedData.get("cpuModel"));
                }
                if (collectedData.containsKey("cpuCores")) {
                    additionalInfo.put("cpuCores", collectedData.get("cpuCores")); 
                }
                if (collectedData.containsKey("ramSize")) {
                    additionalInfo.put("ramSize", collectedData.get("ramSize"));
                }
                if (collectedData.containsKey("gpuName")) {
                    additionalInfo.put("gpuName", collectedData.get("gpuName"));
                }
                if (collectedData.containsKey("lastUser")) {
                    additionalInfo.put("lastLoggedUser", collectedData.get("lastUser"));
                }
                
                // Add new collected data to existing additional info
                additionalInfo.putAll(collectedData);
                asset.setAdditionalInfo(additionalInfo);
                
            } else {
                asset = new Asset();
                asset.setIpAddress(ipAddress);
                asset.setHostname(hostname);
                asset.setOnline(reachable);
                asset.setFirstDiscovered(LocalDateTime.now());
                asset.setLastSeen(LocalDateTime.now());
                asset.setLastScanId(scanJobId);
                asset.setAssetType(assetType);
                asset.setOperatingSystem(operatingSystem);
                asset.setOsVersion(osVersion);
                
                // Set hardware info if available
                if (collectedData.containsKey("manufacturer")) {
                    asset.setManufacturer((String) collectedData.get("manufacturer"));
                }
                if (collectedData.containsKey("model")) {
                    asset.setModel((String) collectedData.get("model"));
                }
                if (collectedData.containsKey("macAddress")) {
                    asset.setMacAddress((String) collectedData.get("macAddress"));
                }
                if (collectedData.containsKey("osVersion")) {
                    asset.setOsVersion((String) collectedData.get("osVersion"));
                }
                
                Map<String, Object> additionalInfo = new HashMap<>();
                // Add specific hardware details we want to track
                if (collectedData.containsKey("cpuModel")) {
                    additionalInfo.put("cpuModel", collectedData.get("cpuModel"));
                }
                if (collectedData.containsKey("cpuCores")) {
                    additionalInfo.put("cpuCores", collectedData.get("cpuCores")); 
                }
                if (collectedData.containsKey("ramSize")) {
                    additionalInfo.put("ramSize", collectedData.get("ramSize"));
                }
                if (collectedData.containsKey("gpuName")) {
                    additionalInfo.put("gpuName", collectedData.get("gpuName"));
                }
                if (collectedData.containsKey("lastUser")) {
                    additionalInfo.put("lastLoggedUser", collectedData.get("lastUser"));
                }
                
                additionalInfo.putAll(collectedData);
                asset.setAdditionalInfo(additionalInfo);
            }
            
            // Save the asset and link to scan result
            Asset savedAsset = assetRepository.save(asset);
            result.setAssetId(savedAsset.getId());
            
        } catch (Exception e) {
            result.setSuccessful(false);
            result.setErrorMessage(e.getMessage());
            logger.error("Error scanning IP address " + ipAddress, e);
        }
        
        result.setCollectedData(collectedData);
        return scanResultRepository.save(result);
    }
    
    /**
     * Check if a specific port is open
     */
    private boolean isPortOpen(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 1000); // 1 second timeout
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Determine asset type based on hostname and open ports
     */
    private AssetType determineAssetType(String hostname, Map<String, Boolean> openPorts) {
        String hostLower = hostname.toLowerCase();
        
        // Check based on hostname
        if (hostLower.contains("win") || hostLower.contains("desktop") || 
                hostLower.contains("laptop") || Boolean.TRUE.equals(openPorts.get("RDP"))) {
            return AssetType.WINDOWS;
        } else if (hostLower.contains("linux") || hostLower.contains("ubuntu") || 
                hostLower.contains("debian") || hostLower.contains("cent")) {
            return AssetType.LINUX;
        } else if (hostLower.contains("mac") || hostLower.contains("apple") || 
                hostLower.contains("mbp") || hostLower.contains("imac")) {
            return AssetType.MAC;
        } else if (hostLower.contains("cisco") || hostLower.contains("router") || 
                hostLower.contains("switch") || hostLower.contains("gateway") || 
                hostLower.contains("access-point")) {
            return AssetType.NETWORK_DEVICE;
        }
        
        // Check based on ports
        if (Boolean.TRUE.equals(openPorts.get("RDP"))) {
            return AssetType.WINDOWS;
        } else if (Boolean.TRUE.equals(openPorts.get("SSH")) && !Boolean.TRUE.equals(openPorts.get("SMB"))) {
            // SSH but no SMB often indicates Linux/Unix
            return AssetType.LINUX;
        }
        
        return AssetType.UNKNOWN;
    }
    
    /**
     * Determine OS based on hostname and open ports
     */
    private String determineOperatingSystem(String hostname, Map<String, Boolean> openPorts) {
        String hostLower = hostname.toLowerCase();
        
        if (hostLower.contains("win")) {
            return "Windows";
        } else if (hostLower.contains("ubuntu")) {
            return "Ubuntu Linux";
        } else if (hostLower.contains("debian")) {
            return "Debian Linux";
        } else if (hostLower.contains("cent")) {
            return "CentOS Linux";
        } else if (hostLower.contains("fedora")) {
            return "Fedora Linux";
        } else if (hostLower.contains("red hat") || hostLower.contains("redhat")) {
            return "Red Hat Linux";
        } else if (hostLower.contains("linux")) {
            return "Linux";
        } else if (hostLower.contains("mac") || hostLower.contains("apple") || 
                hostLower.contains("mbp") || hostLower.contains("imac")) {
            return "macOS";
        } else if (Boolean.TRUE.equals(openPorts.get("RDP"))) {
            return "Windows";
        } else if (Boolean.TRUE.equals(openPorts.get("SSH")) && !Boolean.TRUE.equals(openPorts.get("SMB"))) {
            return "Unix/Linux";
        }
        
        return "Unknown";
    }
    
    /**
     * Get all target IPs to scan from a job
     */
    private List<String> getAllTargets(ScanJob job) {
        List<String> allTargets = new ArrayList<>();
        
        // Add individual IP addresses
        if (job.getIpAddresses() != null) {
            allTargets.addAll(job.getIpAddresses());
        }
        
        // Expand IP segments
        if (job.getIpSegments() != null) {
            for (String segment : job.getIpSegments()) {
                allTargets.addAll(expandIpSegment(segment));
            }
        }
        
        return allTargets;
    }
    
    /**
     * Expand an IP segment into individual IPs
     */
    private List<String> expandIpSegment(String segment) {
        List<String> ips = new ArrayList<>();
        
        try {
            // CIDR notation (e.g., 192.168.1.0/24)
            if (segment.contains("/")) {
                String[] parts = segment.split("/");
                String baseIp = parts[0];
                int prefixLength = Integer.parseInt(parts[1]);
                
                if (prefixLength == 24) {
                    // For a /24 network, scan the whole subnet
                    String prefix = baseIp.substring(0, baseIp.lastIndexOf(".") + 1);
                    for (int i = 1; i <= 254; i++) {
                        ips.add(prefix + i);
                    }
                } else if (prefixLength == 16) {
                    // For a /16, just scan a few addresses as an example
                    String prefix = baseIp.substring(0, baseIp.indexOf(".", baseIp.indexOf(".") + 1) + 1);
                    // Just scan the first subnet (.1.*) for demo purposes
                    prefix = prefix + "1.";
                    for (int i = 1; i <= 254; i++) {
                        ips.add(prefix + i);
                    }
                } else {
                    // Add the base IP if we don't know how to expand
                    ips.add(baseIp);
                }
            } 
            // Range notation (e.g., 192.168.1.1-10)
            else if (segment.contains("-")) {
                String[] parts = segment.split("-");
                if (parts.length == 2) {
                    String baseIp = parts[0];
                    int lastDot = baseIp.lastIndexOf(".");
                    String prefix = baseIp.substring(0, lastDot + 1);
                    int start = Integer.parseInt(baseIp.substring(lastDot + 1));
                    int end = Integer.parseInt(parts[1]);
                    
                    for (int i = start; i <= end; i++) {
                        ips.add(prefix + i);
                    }
                } else {
                    // If the format is unexpected, add as is
                    ips.add(segment);
                }
            } else {
                // If it's just a single IP, add it
                ips.add(segment);
            }
        } catch (Exception e) {
            logger.warn("Error expanding IP segment " + segment + ": " + e.getMessage());
            // If there's any error, just add the original segment
            ips.add(segment);
        }
        
        return ips;
    }

    /**
     * Measures connection response time to a specific port
     * @param ipAddress Target IP address
     * @param port Port to connect to
     * @return Response time in milliseconds
     */
    private long measureResponseTime(String ipAddress, int port) {
        long startTime = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ipAddress, port), 1000);
            if (socket.isConnected()) {
                return System.currentTimeMillis() - startTime;
            }
        } catch (Exception ignored) {
            // Connection failed
        }
        return 1000; // Default to 1000ms (timeout) if connection fails
    }
}