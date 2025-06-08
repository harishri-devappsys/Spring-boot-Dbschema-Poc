package com.dbschema.dbschema.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class PerformanceMonitor {
    
    private final ConcurrentHashMap<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> operationTimes = new ConcurrentHashMap<>();
    
    public void recordOperation(String operation, long executionTimeMs) {
        operationCounts.computeIfAbsent(operation, k -> new AtomicLong(0)).incrementAndGet();
        operationTimes.computeIfAbsent(operation, k -> new AtomicLong(0)).addAndGet(executionTimeMs);
        
        log.debug("Operation: {} completed in {}ms", operation, executionTimeMs);
    }
    
    public void logStatistics() {
        log.info("=== Performance Statistics ===");
        operationCounts.forEach((operation, count) -> {
            long totalTime = operationTimes.get(operation).get();
            long avgTime = count.get() > 0 ? totalTime / count.get() : 0;
            log.info("Operation: {} | Count: {} | Total Time: {}ms | Avg Time: {}ms", 
                    operation, count.get(), totalTime, avgTime);
        });
    }
}