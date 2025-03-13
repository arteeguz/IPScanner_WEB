package com.example.ip_asset_management;

import com.example.ip_asset_management.model.ERole;
import com.example.ip_asset_management.model.Role;
import com.example.ip_asset_management.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class IpAssetManagementApplication {

    @Autowired
    private RoleRepository roleRepository;

    public static void main(String[] args) {
        SpringApplication.run(IpAssetManagementApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // Initialize roles if they don't exist
        for (ERole role : ERole.values()) {
            if (!roleRepository.findByName(role).isPresent()) {
                Role newRole = new Role();
                newRole.setName(role);
                roleRepository.save(newRole);
            }
        }
    }
}