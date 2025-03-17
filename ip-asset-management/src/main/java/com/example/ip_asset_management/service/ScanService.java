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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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
    
    @Autowired
    private AdaptiveNetworkScanner adaptiveNetworkScanner;

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

    public void runScanJob(String scanJobId) {
        ScanJob scanJob = scanJobRepository.findById(scanJobId)
                .orElseThrow(() -> new RuntimeException("Scan job not found"));
        
        // Use the adaptive network scanner to run the job
        logger.info("Starting scan job {} with adaptive scanning", scanJobId);
        
        // The scanner runs asynchronously and updates job status itself
        CompletableFuture<ScanJob> future = adaptiveNetworkScanner.executeScanJob(scanJobId);
        
        // We could monitor completion here if needed, but everything is handled in the scanner
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
        
        // If job is running, mark it as cancelled
        if (scanJob.getStatus() == ScanJobStatus.RUNNING) {
            scanJob.setStatus(ScanJobStatus.CANCELLED);
            scanJobRepository.save(scanJob);
        } else {
            scanJobRepository.delete(scanJob);
        }
    }
    
    private UserDetailsImpl getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserDetailsImpl) authentication.getPrincipal();
    }
}