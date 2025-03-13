package com.example.ip_asset_management.dto;

import com.example.ip_asset_management.model.AssetType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AssetResponse {
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
}