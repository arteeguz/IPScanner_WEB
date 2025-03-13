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
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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
            
            // Create or update asset in database
            Optional<Asset> existingAsset = assetRepository.findByIpAddress(ipAddress);
            Asset asset;
            
            if (existingAsset.isPresent()) {
                asset = existingAsset.get();
                asset.setLastSeen(LocalDateTime.now());
                asset.setOnline(reachable);
                asset.setHostname(hostname);
                asset.setLastScanId(scanJobId);
            } else {
                asset = new Asset();
                asset.setIpAddress(ipAddress);
                asset.setHostname(hostname);
                asset.setOnline(reachable);
                asset.setFirstDiscovered(LocalDateTime.now());
                asset.setLastSeen(LocalDateTime.now());
                asset.setLastScanId(scanJobId);
                
                // Try to determine asset type
                AssetType assetType = AssetType.UNKNOWN;
                
                // Simple OS detection based on hostname/response (very basic)
                String hostLower = hostname.toLowerCase();
                if (hostLower.contains("win")) {
                    assetType = AssetType.WINDOWS;
                } else if (hostLower.contains("linux") || hostLower.contains("ubuntu") || 
                        hostLower.contains("debian") || hostLower.contains("cent")) {
                    assetType = AssetType.LINUX;
                } else if (hostLower.contains("mac") || hostLower.contains("apple")) {
                    assetType = AssetType.MAC;
                } else if (hostLower.contains("cisco") || hostLower.contains("router") || 
                        hostLower.contains("switch") || hostLower.contains("gateway")) {
                    assetType = AssetType.NETWORK_DEVICE;
                }
                
                asset.setAssetType(assetType);
            }
            
            // Save the asset and link to scan result
            Asset savedAsset = assetRepository.save(asset);
            result.setAssetId(savedAsset.getId());
            
        } catch (Exception e) {
            result.setSuccessful(false);
            result.setErrorMessage(e.getMessage());
        }
        
        result.setCollectedData(collectedData);
        return scanResultRepository.save(result);
    }
    
    private List<String> expandIpSegment(String segment) {
        List<String> ips = new ArrayList<>();
        
        // Very basic implementation - just for demo purposes
        // In a real application, you'd want a more robust IP range parser
        if (segment.contains("/24")) {
            String prefix = segment.substring(0, segment.lastIndexOf(".") + 1);
            for (int i = 1; i <= 10; i++) { // Just scan 10 IPs for demo
                ips.add(prefix + i);
            }
        } else if (segment.contains("-")) {
            String[] parts = segment.split("-");
            if (parts.length == 2) {
                String prefix = parts[0].substring(0, parts[0].lastIndexOf(".") + 1);
                int start = Integer.parseInt(parts[0].substring(parts[0].lastIndexOf(".") + 1));
                int end = Integer.parseInt(parts[1]);
                for (int i = start; i <= Math.min(end, start + 10); i++) { // Limit to 10 for demo
                    ips.add(prefix + i);
                }
            }
        } else {
            // If format not recognized, just add the segment as is
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