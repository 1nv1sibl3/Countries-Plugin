package xyz.inv1s1bl3.countries.utils;

import org.bukkit.scheduler.BukkitRunnable;
import xyz.inv1s1bl3.countries.CountriesPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Monitors plugin performance and provides optimization insights.
 */
public class PerformanceMonitor {
    
    private final CountriesPlugin plugin;
    private final Map<String, Long> operationTimes;
    private final Map<String, Integer> operationCounts;
    private final Map<String, Long> lastOperationTime;
    
    private BukkitRunnable monitorTask;
    private boolean enabled;
    
    public PerformanceMonitor(CountriesPlugin plugin) {
        this.plugin = plugin;
        this.operationTimes = new ConcurrentHashMap<>();
        this.operationCounts = new ConcurrentHashMap<>();
        this.lastOperationTime = new ConcurrentHashMap<>();
        this.enabled = plugin.getConfigManager().isDebugEnabled();
    }
    
    /**
     * Start performance monitoring
     */
    public void start() {
        if (!enabled || monitorTask != null) {
            return;
        }
        
        monitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    logPerformanceStats();
                }
            }
        };
        
        // Run every 5 minutes
        monitorTask.runTaskTimerAsynchronously(plugin, 6000L, 6000L);
        plugin.debug("Performance monitoring started");
    }
    
    /**
     * Stop performance monitoring
     */
    public void stop() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
        plugin.debug("Performance monitoring stopped");
    }
    
    /**
     * Record the start of an operation
     */
    public void startOperation(String operationName) {
        if (!enabled) return;
        lastOperationTime.put(operationName, System.nanoTime());
    }
    
    /**
     * Record the end of an operation
     */
    public void endOperation(String operationName) {
        if (!enabled) return;
        
        Long startTime = lastOperationTime.remove(operationName);
        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            operationTimes.merge(operationName, duration, Long::sum);
            operationCounts.merge(operationName, 1, Integer::sum);
        }
    }
    
    /**
     * Time an operation using try-with-resources
     */
    public OperationTimer timeOperation(String operationName) {
        return new OperationTimer(this, operationName);
    }
    
    /**
     * Get average operation time in milliseconds
     */
    public double getAverageOperationTime(String operationName) {
        Long totalTime = operationTimes.get(operationName);
        Integer count = operationCounts.get(operationName);
        
        if (totalTime == null || count == null || count == 0) {
            return 0.0;
        }
        
        return (totalTime / 1_000_000.0) / count; // Convert to milliseconds
    }
    
    /**
     * Get operation count
     */
    public int getOperationCount(String operationName) {
        return operationCounts.getOrDefault(operationName, 0);
    }
    
    /**
     * Get all performance statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        for (String operation : operationTimes.keySet()) {
            Map<String, Object> operationStats = new HashMap<>();
            operationStats.put("count", getOperationCount(operation));
            operationStats.put("average_time_ms", getAverageOperationTime(operation));
            operationStats.put("total_time_ms", operationTimes.get(operation) / 1_000_000.0);
            
            stats.put(operation, operationStats);
        }
        
        return stats;
    }
    
    /**
     * Log performance statistics
     */
    private void logPerformanceStats() {
        if (operationTimes.isEmpty()) {
            return;
        }
        
        plugin.getLogger().info("=== Performance Statistics ===");
        
        for (String operation : operationTimes.keySet()) {
            double avgTime = getAverageOperationTime(operation);
            int count = getOperationCount(operation);
            
            if (avgTime > 10.0) { // Log operations taking more than 10ms on average
                plugin.getLogger().log(Level.WARNING, 
                        String.format("Slow operation: %s - Avg: %.2fms, Count: %d", 
                                operation, avgTime, count));
            } else {
                plugin.debug(String.format("Operation: %s - Avg: %.2fms, Count: %d", 
                        operation, avgTime, count));
            }
        }
    }
    
    /**
     * Clear all statistics
     */
    public void clearStatistics() {
        operationTimes.clear();
        operationCounts.clear();
        lastOperationTime.clear();
        plugin.debug("Performance statistics cleared");
    }
    
    /**
     * Enable or disable monitoring
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            start();
        } else {
            stop();
        }
    }
    
    /**
     * Auto-closeable timer for operations
     */
    public static class OperationTimer implements AutoCloseable {
        private final PerformanceMonitor monitor;
        private final String operationName;
        
        public OperationTimer(PerformanceMonitor monitor, String operationName) {
            this.monitor = monitor;
            this.operationName = operationName;
            monitor.startOperation(operationName);
        }
        
        @Override
        public void close() {
            monitor.endOperation(operationName);
        }
    }
}