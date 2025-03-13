package com.example.ip_asset_management.repository;

import com.example.ip_asset_management.model.ScanJob;
import com.example.ip_asset_management.model.ScanJobStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScanJobRepository extends MongoRepository<ScanJob, String> {
    List<ScanJob> findByUserId(String userId);
    List<ScanJob> findByUserIdAndStatus(String userId, ScanJobStatus status);
    List<ScanJob> findByStatus(ScanJobStatus status);
    List<ScanJob> findByRecurringAndNextRunAtBefore(boolean recurring, LocalDateTime time);
}