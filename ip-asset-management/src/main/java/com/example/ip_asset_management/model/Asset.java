package com.example.ip_asset_management.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Document(collection = "assets")
public class Asset {
    @Id
    private String id;
    private String ipAddress;
    private String hostname;
    private AssetType assetType;
    private String operatingSystem;
    private String osVersion;
    private String macAddress;
    private String manufacturer;
    private String model;
    private Map<String, Object> additionalInfo;
    private boolean online;
    private LocalDateTime firstDiscovered;
    private LocalDateTime lastSeen;
    private String lastScanId;
}