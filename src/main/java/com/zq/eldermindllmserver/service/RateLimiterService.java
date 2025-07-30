package com.zq.eldermindllmserver.service;

import com.zq.eldermindllmserver.config.LLMConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LLM推理限流服务
 */
@Slf4j
@Service
public class RateLimiterService {
    
    private final LLMConfig.RateLimiter config;
    private final Semaphore concurrentSemaphore;
    private final AtomicInteger currentConcurrent = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<LocalDateTime> minuteRequests = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<LocalDateTime> hourRequests = new ConcurrentLinkedQueue<>();
    
    public RateLimiterService(LLMConfig llmConfig) {
        this.config = llmConfig.getRateLimiter();
        this.concurrentSemaphore = new Semaphore(config.getMaxConcurrentRequests());
        
        log.info("LLM限流器初始化: 并发={}, 分钟限制={}, 小时限制={}", 
                config.getMaxConcurrentRequests(), 
                config.getMaxRequestsPerMinute(), 
                config.getMaxRequestsPerHour());
    }
    
    /**
     * 获取请求许可
     */
    public boolean acquirePermit() throws InterruptedException {
        if (!config.isEnabled()) {
            return true;
        }
        
        // 尝试获取并发许可
        if (!concurrentSemaphore.tryAcquire(config.getQueueTimeout(), TimeUnit.SECONDS)) {
            log.warn("等待并发许可超时 ({}秒)", config.getQueueTimeout());
            return false;
        }
        
        try {
            // 检查频率限制
            long startTime = System.currentTimeMillis();
            while (!canMakeRequest()) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= (long) config.getQueueTimeout() * 1000L) {
                    log.warn("等待频率限制解除超时");
                    return false;
                }

                log.debug("频率限制中，等待 {}秒", config.getRetryDelay());
                try {
                    TimeUnit.MILLISECONDS.sleep((long) (config.getRetryDelay() * 1000));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedException("等待被中断");
                }
            }
            
            // 记录请求
            recordRequest();
            currentConcurrent.incrementAndGet();
            
            log.debug("获取LLM请求许可成功，当前并发: {}/{}", 
                    currentConcurrent.get(), config.getMaxConcurrentRequests());
            
            return true;
            
        } catch (Exception e) {
            // 如果出现异常，释放并发许可
            concurrentSemaphore.release();
            throw e;
        }
    }
    
    /**
     * 释放请求许可
     */
    public void releasePermit() {
        if (!config.isEnabled()) {
            return;
        }
        
        concurrentSemaphore.release();
        currentConcurrent.decrementAndGet();
        
        log.debug("释放LLM请求许可，当前并发: {}/{}", 
                currentConcurrent.get(), config.getMaxConcurrentRequests());
    }
    
    /**
     * 检查是否可以发起请求
     */
    private boolean canMakeRequest() {
        cleanupOldRequests();
        
        // 检查每分钟限制
        if (minuteRequests.size() >= config.getMaxRequestsPerMinute()) {
            log.debug("每分钟请求数已达上限 ({})", config.getMaxRequestsPerMinute());
            return false;
        }
        
        // 检查每小时限制
        if (hourRequests.size() >= config.getMaxRequestsPerHour()) {
            log.debug("每小时请求数已达上限 ({})", config.getMaxRequestsPerHour());
            return false;
        }
        
        return true;
    }
    
    /**
     * 记录一次请求
     */
    private void recordRequest() {
        LocalDateTime now = LocalDateTime.now();
        minuteRequests.offer(now);
        hourRequests.offer(now);
        cleanupOldRequests();
    }
    
    /**
     * 清理过期的请求记录
     */
    private void cleanupOldRequests() {
        LocalDateTime now = LocalDateTime.now();
        
        // 清理一分钟前的记录
        minuteRequests.removeIf(time -> ChronoUnit.SECONDS.between(time, now) > 60);
        
        // 清理一小时前的记录
        hourRequests.removeIf(time -> ChronoUnit.SECONDS.between(time, now) > 3600);
    }
    
    /**
     * 获取限流器状态
     */
    public Map<String, Object> getStatus() {
        cleanupOldRequests();
        
        return Map.of(
            "enabled", config.isEnabled(),
            "concurrent", Map.of(
                "current", currentConcurrent.get(),
                "max", config.getMaxConcurrentRequests(),
                "available", config.getMaxConcurrentRequests() - currentConcurrent.get()
            ),
            "rateLimits", Map.of(
                "perMinute", Map.of(
                    "current", minuteRequests.size(),
                    "max", config.getMaxRequestsPerMinute(),
                    "remaining", Math.max(0, config.getMaxRequestsPerMinute() - minuteRequests.size())
                ),
                "perHour", Map.of(
                    "current", hourRequests.size(),
                    "max", config.getMaxRequestsPerHour(),
                    "remaining", Math.max(0, config.getMaxRequestsPerHour() - hourRequests.size())
                )
            ),
            "config", Map.of(
                "queueTimeout", config.getQueueTimeout(),
                "retryDelay", config.getRetryDelay()
            )
        );
    }
    

}
