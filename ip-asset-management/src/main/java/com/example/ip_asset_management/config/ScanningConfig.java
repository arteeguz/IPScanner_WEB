package com.example.ip_asset_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ScanningConfig {
    
    @Bean(name = "scanTaskExecutor")
    public ThreadPoolTaskExecutor scanTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Initial configuration - will be adjusted dynamically
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(250);
        executor.setThreadNamePrefix("scan-");
        
        // Rejected tasks are executed by the caller thread
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Initialize the executor
        executor.initialize();
        return executor;
    }
}