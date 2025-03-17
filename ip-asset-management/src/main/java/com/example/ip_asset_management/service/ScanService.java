package com.example.ip_asset_management.service;

import com.example.ip_asset_management.dto.ScanJobRequest;
import com.example.ip_asset_management.model.*;
import com.example.ip_asset_management.repository.AssetRepository;
import com.example.ip_asset_management.repository.ScanJobRepository;
import com.example.ip_asset_management.repository.ScanResultRepository;
import com.example.ip_asset_management.repository.UserRepository;
import com.example.ip_asset_management.security.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ScanService {
    private static final Logger logger = LoggerFactory.getLogger(ScanService.class);

    @Autowired
    private ScanJobRepository scanJobRepository;

    @Autowired
    private ScanResultRepository scanResultRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private WindowsScannerService windowsScannerService;
    
    @Value("${scan.timeout.seconds:30}")
    private int scanTimeoutSeconds;

    public ScanJob createScanJob(ScanJobRequest request) {
        UserDetailsImpl userDetails = getCurrentUserDetails();
        
        ScanJob scanJob = new ScanJob();
        scanJob.setUserId(userDetails.getId());
        scanJob.setName(request.getName());
        scanJob.setDescription(request.getDescription());
        scanJob.setIpAddresses(request.getIpAddresses());
        scanJob.setIpSegments(request.getIpSegments());
        scanJob.setRecurring(request.isRecurring());
        scanJob.setSchedule(request.getSchedule());
        scanJob.setSettings(request.getSettings());
        scanJob.setCreatedAt(LocalDateTime.now());
        scanJob.setStatus(ScanJobStatus.CREATED);
        
        int totalTargets = 0;
        if (request.getIpAddresses() != null) {
            totalTargets += request.getIpAddresses().size();
        }
        if (request.getIpSegments() != null) {
            // Approximate count for segments
            for (String segment : request.getIpSegments()) {
                if (segment.contains("/24")) {
                    totalTargets += 254; // Typical /24 network
                } else if (segment.contains("/16")) {
                    totalTargets += 100; // Just scan a portion of a /16 for demo
                } else {
                    totalTargets += 10; // Default for other cases
                }
            }
        }
        scanJob.setTotalTargets(totalTargets);
        scanJob.setCompletedTargets(0);
        scanJob.setSuccessfulTargets(0);
        scanJob.setFailedTargets(0);
        
        return scanJobRepository.save(scanJob);
    }

    @Async
    public void runScanJob(String scanJobId) {
        ScanJob scanJob = scanJobRepository.findById(scanJobId)
                .orElseThrow(() -> new RuntimeException("Scan job not found"));
        
        try {
            scanJob.setStatus(ScanJobStatus.RUNNING);
            scanJob.setLastRunAt(LocalDateTime.now());
            scanJobRepository.save(scanJob);
            
            List<CompletableFuture<ScanResult>> futures = new ArrayList<>();
            
            // Process individual IP addresses
            if (scanJob.getIpAddresses() != null) {
                for (String ip : scanJob.getIpAddresses()) {
                    futures.add(CompletableFuture.supplyAsync(() -> scanIpAddress(scanJob.getId(), ip)));
                }
            }
            
            // Process IP segments
            if (scanJob.getIpSegments() != null) {
                for (String segment : scanJob.getIpSegments()) {
                    List<String> expandedIps = expandIpSegment(segment);
                    for (String ip : expandedIps) {
                        futures.add(CompletableFuture.supplyAsync(() -> scanIpAddress(scanJob.getId(), ip)));
                    }
                }
            }
            
            // Wait for all scans to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );
            
            // When all scans complete, update the scan job status
            allFutures.thenRun(() -> {
                scanJob.setStatus(ScanJobStatus.COMPLETED);
                scanJob.setCompletedTargets(futures.size());
                int successful = 0;
                int failed = 0;
                
                for (CompletableFuture<ScanResult> future : futures) {
                    try {
                        ScanResult result = future.get();
                        if (result.isSuccessful()) {
                            successful++;
                        } else {
                            failed++;
                        }
                    } catch (Exception e) {
                        failed++;
                        logger.error("Error processing scan result", e);
                    }
                }
                
                scanJob.setSuccessfulTargets(successful);
                scanJob.setFailedTargets(failed);
                
                if (scanJob.isRecurring()) {
                    // Set next run time based on cron expression
                    // For simplicity, just adding 24 hours for now
                    scanJob.setNextRunAt(LocalDateTime.now().plusHours(24));
                }
                
                scanJobRepository.save(scanJob);
            });
        } catch (Exception e) {
            logger.error("Error running scan job", e);
            scanJob.setStatus(ScanJobStatus.FAILED);
            scanJobRepository.save(scanJob);
        }
    }
    
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
            AssetType assetType = determineAssetType(hostname, openPorts);
            String operatingSystem = determineOperatingSystem(hostname, openPorts);
            String osVersion = "Unknown";
            
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
                
                // Update additional info
                Map<String, Object> additionalInfo = asset.getAdditionalInfo();
                if (additionalInfo == null) {
                    additionalInfo = new HashMap<>();
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
                
                asset.setAdditionalInfo(collectedData);
            }
            
            // Special handling for specific asset types
            handleSpecificAssetType(asset, collectedData);
            
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
     * Special handling based on detected asset type
     */
    private void handleSpecificAssetType(Asset asset, Map<String, Object> collectedData) {
        switch (asset.getAssetType()) {
            case WINDOWS:
                enrichWindowsAsset(asset, collectedData);
                break;
            case MAC:
                enrichMacAsset(asset, collectedData);
                break;
            case LINUX:
                enrichLinuxAsset(asset, collectedData);
                break;
            case NETWORK_DEVICE:
                enrichNetworkDevice(asset, collectedData);
                break;
            default:
                // No special handling
                break;
        }
    }
    
    /**
     * Add Windows-specific enrichment
     */
    private void enrichWindowsAsset(Asset asset, Map<String, Object> collectedData) {
        // Add Windows-specific details
        if (collectedData.containsKey("cpuModel")) {
            collectedData.put("processor", collectedData.get("cpuModel"));
        }
        
        if (collectedData.containsKey("ramSize")) {
            collectedData.put("memory", collectedData.get("ramSize"));
        }
        
        if (collectedData.containsKey("gpuName")) {
            collectedData.put("graphicsCard", collectedData.get("gpuName"));
        }
    }
    
    /**
     * Add Mac-specific enrichment
     */
    private void enrichMacAsset(Asset asset, Map<String, Object> collectedData) {
        // Mac-specific enrichments would go here
        collectedData.put("osFamily", "macOS");
        
        // Check if the Mac is likely an iMac, MacBook, etc.
        String hostname = asset.getHostname().toLowerCase();
        if (hostname.contains("imac")) {
            collectedData.put("macType", "iMac");
        } else if (hostname.contains("macbook") || hostname.contains("mbp")) {
            collectedData.put("macType", "MacBook");
        } else if (hostname.contains("macmini")) {
            collectedData.put("macType", "Mac Mini");
        } else if (hostname.contains("macpro")) {
            collectedData.put("macType", "Mac Pro");
        }
    }
    
    /**
     * Add Linux-specific enrichment
     */
    private void enrichLinuxAsset(Asset asset, Map<String, Object> collectedData) {
        // Linux-specific enrichments
        String distro = "Unknown";
        
        // Try to determine Linux distribution from hostname
        String hostname = asset.getHostname().toLowerCase();
        if (hostname.contains("ubuntu")) {
            distro = "Ubuntu";
        } else if (hostname.contains("debian")) {
            distro = "Debian";
        } else if (hostname.contains("cent")) {
            distro = "CentOS";
        } else if (hostname.contains("fedora")) {
            distro = "Fedora";
        } else if (hostname.contains("red") && hostname.contains("hat")) {
            distro = "Red Hat";
        } else if (hostname.contains("suse")) {
            distro = "SUSE";
        }
        
        collectedData.put("distribution", distro);
        collectedData.put("osFamily", "Linux");
    }
    
    /**
     * Add network device-specific enrichment
     */
    private void enrichNetworkDevice(Asset asset, Map<String, Object> collectedData) {
        // Network device specific enrichments
        String deviceType = "Unknown";
        
        // Try to determine device type
        String hostname = asset.getHostname().toLowerCase();
        if (hostname.contains("router")) {
            deviceType = "Router";
        } else if (hostname.contains("switch")) {
            deviceType = "Switch";
        } else if (hostname.contains("firewall")) {
            deviceType = "Firewall";
        } else if (hostname.contains("ap") || hostname.contains("access") && hostname.contains("point")) {
            deviceType = "Access Point";
        }
        
        collectedData.put("deviceType", deviceType);
        
        // Check for common network device ports
        Map<String, Boolean> openPorts = (Map<String, Boolean>) collectedData.getOrDefault("openPorts", new HashMap<>());
        if (openPorts.getOrDefault("HTTP", false) || openPorts.getOrDefault("HTTPS", false)) {
            collectedData.put("hasWebInterface", true);
        }
        
        if (openPorts.getOrDefault("SSH", false) || openPorts.getOrDefault("Telnet", false)) {
            collectedData.put("hasCommandLineAccess", true);
        }
    }
    
    private boolean isPortOpen(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 1000); // 1 second timeout
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private AssetType determineAssetType(String hostname, Map<String, Boolean> openPorts) {
        String hostLower = hostname.toLowerCase();
        
        // Check based on hostname
        if (hostLower.contains("win") || hostLower.contains("desktop") || 
                hostLower.contains("laptop") || openPorts.getOrDefault("RDP", false)) {
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
        if (openPorts.getOrDefault("RDP", false)) {
            return AssetType.WINDOWS;
        } else if (openPorts.getOrDefault("SSH", false) && !openPorts.getOrDefault("SMB", false)) {
            // SSH but no SMB often indicates Linux/Unix
            return AssetType.LINUX;
        }
        
        return AssetType.UNKNOWN;
    }
    
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
        } else if (openPorts.getOrDefault("RDP", false)) {
            return "Windows";
        } else if (openPorts.getOrDefault("SSH", false) && !openPorts.getOrDefault("SMB", false)) {
            return "Unix/Linux";
        }
        
        return "Unknown";
    }
    
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
                    String prefix = baseIp.substring(0, baseIp.lastIndexOf(".") + 1);
                    for (int i = 1; i <= 20; i++) {
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

    public List<ScanJob> getCurrentUserScanJobs() {
        UserDetailsImpl userDetails = getCurrentUserDetails();
        return scanJobRepository.findByUserId(userDetails.getId());
    }

    public ScanJob getScanJob(String scanJobId) {
        return scanJobRepository.findById(scanJobId)
                .orElseThrow(() -> new RuntimeException("Scan job not found"));
    }

    public List<ScanResult> getScanResults(String scanJobId) {
        return scanResultRepository.findByScanJobId(scanJobId);
    }

    public void deleteScanJob(String scanJobId) {
        ScanJob scanJob = scanJobRepository.findById(scanJobId)
                .orElseThrow(() -> new RuntimeException("Scan job not found"));
        
        // Verify user owns this job
        UserDetailsImpl userDetails = getCurrentUserDetails();
        if (!scanJob.getUserId().equals(userDetails.getId())) {
            throw new RuntimeException("You don't have permission to delete this scan job");
        }
        
        scanJobRepository.delete(scanJob);
    }
    
    private UserDetailsImpl getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserDetailsImpl) authentication.getPrincipal();
    }
}