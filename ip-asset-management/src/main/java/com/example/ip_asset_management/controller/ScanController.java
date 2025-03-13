package com.example.ip_asset_management.controller;

import com.example.ip_asset_management.dto.ScanJobRequest;
import com.example.ip_asset_management.model.ScanJob;
import com.example.ip_asset_management.model.ScanResult;
import com.example.ip_asset_management.service.ScanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/scan")
public class ScanController {
    
    @Autowired
    private ScanService scanService;
    
    @PostMapping("/create")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> createScanJob(@Valid @RequestBody ScanJobRequest scanJobRequest) {
        ScanJob scanJob = scanService.createScanJob(scanJobRequest);
        return ResponseEntity.ok(scanJob);
    }
    
    @PostMapping("/run/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> runScanJob(@PathVariable("id") String scanJobId) {
        scanService.runScanJob(scanJobId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/jobs")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<List<ScanJob>> getScanJobs() {
        List<ScanJob> scanJobs = scanService.getCurrentUserScanJobs();
        return ResponseEntity.ok(scanJobs);
    }
    
    @GetMapping("/jobs/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<ScanJob> getScanJob(@PathVariable("id") String scanJobId) {
        ScanJob scanJob = scanService.getScanJob(scanJobId);
        return ResponseEntity.ok(scanJob);
    }
    
    @GetMapping("/results/{jobId}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<List<ScanResult>> getScanResults(@PathVariable("jobId") String scanJobId) {
        List<ScanResult> results = scanService.getScanResults(scanJobId);
        return ResponseEntity.ok(results);
    }
    
    @DeleteMapping("/jobs/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteScanJob(@PathVariable("id") String scanJobId) {
        scanService.deleteScanJob(scanJobId);
        return ResponseEntity.ok().build();
    }
}