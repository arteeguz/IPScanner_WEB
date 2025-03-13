package com.example.ip_asset_management.dto;

import com.example.ip_asset_management.model.ScanSetting;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ScanJobRequest {
    @NotBlank
    private String name;
    
    private String description;
    
    private List<String> ipAddresses;
    
    private List<String> ipSegments;
    
    private boolean recurring;
    
    private String schedule;
    
    private List<ScanSetting> settings;
}