package com.example.ip_asset_management.repository;

import com.example.ip_asset_management.model.ERole;
import com.example.ip_asset_management.model.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RoleRepository extends MongoRepository<Role, String> {
    Optional<Role> findByName(ERole name);
}