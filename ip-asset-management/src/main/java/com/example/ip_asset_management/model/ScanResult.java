package com.example.ip_asset_management.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Document(collection = "scan_results")
public class ScanResult {
    @Id
    private String id;
    private String scanJobId;
    private String assetId;
    private String ipAddress;
    private String hostname;
    private boolean successful;
    private String errorMessage;
    private LocalDateTime scanTime;
    private Map<String, Object> collectedData;
}