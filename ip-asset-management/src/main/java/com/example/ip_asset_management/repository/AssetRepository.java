package com.example.ip_asset_management.repository;

import com.example.ip_asset_management.model.Asset;
import com.example.ip_asset_management.model.AssetType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends MongoRepository<Asset, String> {
    Optional<Asset> findByIpAddress(String ipAddress);
    List<Asset> findByAssetType(AssetType assetType);
    List<Asset> findByOperatingSystemContaining(String os);
    List<Asset> findByOnline(boolean online);
}