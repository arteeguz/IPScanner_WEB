package com.example.ip_asset_management.controller;

import com.example.ip_asset_management.dto.AssetResponse;
import com.example.ip_asset_management.model.Asset;
import com.example.ip_asset_management.model.AssetType;
import com.example.ip_asset_management.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/assets")
public class AssetController {
    
    @Autowired
    private AssetService assetService;
    
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<List<AssetResponse>> getAllAssets() {
        List<AssetResponse> assets = assetService.getAllAssets();
        return ResponseEntity.ok(assets);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<AssetResponse> getAssetById(@PathVariable("id") String id) {
        AssetResponse asset = assetService.getAssetById(id);
        return ResponseEntity.ok(asset);
    }
    
    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<List<AssetResponse>> getAssetsByType(@PathVariable("type") AssetType type) {
        List<AssetResponse> assets = assetService.getAssetsByType(type);
        return ResponseEntity.ok(assets);
    }
    
    @GetMapping("/online/{status}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<List<AssetResponse>> getAssetsByOnlineStatus(@PathVariable("status") boolean online) {
        List<AssetResponse> assets = assetService.getAssetsByOnlineStatus(online);
        return ResponseEntity.ok(assets);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAsset(@PathVariable("id") String id) {
        assetService.deleteAsset(id);
        return ResponseEntity.ok().build();
    }
}