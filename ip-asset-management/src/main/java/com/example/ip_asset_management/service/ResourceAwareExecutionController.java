package com.example.ip_asset_management.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ResourceAwareExecutionController {
    private static final Logger logger = LoggerFactory.getLogger(ResourceAwareExecutionController.class);
    
    private final ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger optimalThreadCount = new AtomicInteger(4); // Default starting point
    private final AtomicInteger maxScanBatchSize = new AtomicInteger(100);
    
    @Autowired
    private ThreadPoolTaskExecutor scanTaskExecutor;
    
    @PostConstruct
    public void initialize() {
        // Start background resource monitoring
        monitor.scheduleAtFixedRate(this::adjustBasedOnSystemResources, 0, 5, TimeUnit.SECONDS);
        
        // Initial calibration based on system specs
        calibrateToSystem();
    }
    
    @PreDestroy
    public void cleanup() {
        monitor.shutdownNow();
    }
    
    private void calibrateToSystem() {
        // Get available processors - simple starting point for thread calculation
        int availableCores = Runtime.getRuntime().availableProcessors();
        
        // Set initial thread count (75% of available cores)
        optimalThreadCount.set(Math.max(2, (int)(availableCores * 0.75)));
        
        // Update the executor's thread counts
        scanTaskExecutor.setCorePoolSize(optimalThreadCount.get());
        scanTaskExecutor.setMaxPoolSize(Math.max(optimalThreadCount.get() * 2, 10));
        
        // Determine max memory available for the JVM
        long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024); // MB
        
        // Set batch size based on available memory (very conservative initial estimate)
        // Each scan might use ~2MB of memory at peak
        maxScanBatchSize.set((int)Math.min(500, maxMemory / 4));
        
        logger.info("System calibration: {} threads, batch size of {}", 
                    optimalThreadCount.get(), maxScanBatchSize.get());
    }
    
    private void adjustBasedOnSystemResources() {
        try {
            // Get current CPU load (requires OperatingSystemMXBean)
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = getSystemCpuLoad(osBean);
            
            // Get current memory usage
            long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long maxMemory = Runtime.getRuntime().maxMemory();
            double memoryUsage = (double)usedMemory / maxMemory;
            
            // Adjust thread count based on CPU load
            if (cpuLoad > 0.8) { // >80% CPU
                optimalThreadCount.updateAndGet(count -> Math.max(2, count - 1));
                maxScanBatchSize.updateAndGet(size -> Math.max(20, size / 2));
                
                // Update the executor
                scanTaskExecutor.setCorePoolSize(optimalThreadCount.get());
            } else if (cpuLoad < 0.3 && memoryUsage < 0.7) { // <30% CPU and memory ok
                optimalThreadCount.updateAndGet(count -> 
                    Math.min(Runtime.getRuntime().availableProcessors(), count + 1));
                maxScanBatchSize.updateAndGet(size -> Math.min(1000, size + 20));
                
                // Update the executor
                scanTaskExecutor.setCorePoolSize(optimalThreadCount.get());
                scanTaskExecutor.setMaxPoolSize(Math.max(optimalThreadCount.get() * 2, 10));
            }
            
            // Adjust batch size based on memory pressure
            if (memoryUsage > 0.8) { // >80% memory used
                maxScanBatchSize.updateAndGet(size -> Math.max(20, size / 2));
            }
            
            logger.debug("Resource adjustment: CPU load={}, Memory usage={}, Threads={}, Batch size={}",
                       cpuLoad, memoryUsage, optimalThreadCount.get(), maxScanBatchSize.get());
        } catch (Exception e) {
            logger.warn("Error adjusting resources: {}", e.getMessage());
        }
    }
    
    // Helper method to get system CPU load, implementation depends on JVM and OS
    private double getSystemCpuLoad(OperatingSystemMXBean osBean) {
        double loadValue = 0.5; // Default value
        
        try {
            // Try different methods through reflection
            for (String methodName : new String[]{"getCpuLoad", "getSystemCpuLoad", "getSystemLoadAverage"}) {
                try {
                    Method method = osBean.getClass().getMethod(methodName);
                    method.setAccessible(true);
                    Object result = method.invoke(osBean);
                    if (result instanceof Double) {
                        double value = (Double) result;
                        // For getSystemLoadAverage, normalize by CPU count
                        if (methodName.equals("getSystemLoadAverage") && value > 0) {
                            value = value / Runtime.getRuntime().availableProcessors();
                            // Cap it at 1.0
                            value = Math.min(value, 1.0);
                        }
                        return value;
                    }
                } catch (Exception ignored) {
                    // Just try the next method
                }
            }
        } catch (Exception e) {
            logger.warn("CPU load detection failed: {}", e.getMessage());
        }
        
        return loadValue; // Return default if all else fails
    }
    
    public int getOptimalThreadCount() {
        return optimalThreadCount.get();
    }
    
    public int getMaxBatchSize() {
        return maxScanBatchSize.get();
    }
    
    public ThreadPoolTaskExecutor getScanTaskExecutor() {
        return scanTaskExecutor;
    }
}