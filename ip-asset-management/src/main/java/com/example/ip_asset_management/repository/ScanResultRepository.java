package com.example.ip_asset_management.repository;

import com.example.ip_asset_management.model.ScanResult;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScanResultRepository extends MongoRepository<ScanResult, String> {
    List<ScanResult> findByScanJobId(String scanJobId);
    List<ScanResult> findByAssetId(String assetId);
    List<ScanResult> findBySuccessful(boolean successful);
    List<ScanResult> findByScanTimeBetween(LocalDateTime start, LocalDateTime end);
}