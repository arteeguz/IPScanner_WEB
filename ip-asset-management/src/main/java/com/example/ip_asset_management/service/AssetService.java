package com.example.ip_asset_management.service;

import com.example.ip_asset_management.dto.AssetResponse;
import com.example.ip_asset_management.model.Asset;
import com.example.ip_asset_management.model.AssetType;
import com.example.ip_asset_management.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssetService {
    
    @Autowired
    private AssetRepository assetRepository;
    
    public List<AssetResponse> getAllAssets() {
        List<Asset> assets = assetRepository.findAll();
        return assets.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public AssetResponse getAssetById(String id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
        return convertToResponse(asset);
    }
    
    public List<AssetResponse> getAssetsByType(AssetType type) {
        List<Asset> assets = assetRepository.findByAssetType(type);
        return assets.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public List<AssetResponse> getAssetsByOnlineStatus(boolean online) {
        List<Asset> assets = assetRepository.findByOnline(online);
        return assets.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public void deleteAsset(String id) {
        assetRepository.deleteById(id);
    }
    
    private AssetResponse convertToResponse(Asset asset) {
        AssetResponse response = new AssetResponse();
        response.setId(asset.getId());
        response.setIpAddress(asset.getIpAddress());
        response.setHostname(asset.getHostname());
        response.setAssetType(asset.getAssetType());
        response.setOperatingSystem(asset.getOperatingSystem());
        response.setOsVersion(asset.getOsVersion());
        response.setMacAddress(asset.getMacAddress());
        response.setManufacturer(asset.getManufacturer());
        response.setModel(asset.getModel());
        response.setAdditionalInfo(asset.getAdditionalInfo());
        response.setOnline(asset.isOnline());
        response.setFirstDiscovered(asset.getFirstDiscovered());
        response.setLastSeen(asset.getLastSeen());
        return response;
    }
}