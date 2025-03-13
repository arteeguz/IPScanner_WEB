package com.example.ip_asset_management.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "scan_jobs")
public class ScanJob {
    @Id
    private String id;
    private String userId;
    private String name;
    private String description;
    private List<String> ipAddresses;
    private List<String> ipSegments;
    private boolean recurring;
    private String schedule; // Cron expression if recurring
    private LocalDateTime createdAt;
    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;
    private ScanJobStatus status;
    private int totalTargets;
    private int completedTargets;
    private int successfulTargets;
    private int failedTargets;
    private List<ScanSetting> settings;
}