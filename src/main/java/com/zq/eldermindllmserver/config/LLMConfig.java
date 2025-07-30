package com.zq.eldermindllmserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LLM推理配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "llm.inference")
public class LLMConfig {
    
    /**
     * 是否启用LLM推理
     */
    private boolean enabled = true;
    
    /**
     * 推理模式：api 或 custom
     */
    private String mode = "api";
    
    /**
     * 推理超时时间（秒）
     */
    private int timeout = 60;
    
    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 模型名称
     */
    private String model = "glm-4v-plus";

    /**
     * 触发条件配置
     */
    private TriggerConditions triggerConditions = new TriggerConditions();
    
    /**
     * 提示词配置
     */
    private PromptConfig promptConfig = new PromptConfig();
    
    /**
     * 结果处理配置
     */
    private ResultProcessing resultProcessing = new ResultProcessing();
    
    /**
     * 限流配置
     */
    private RateLimiter rateLimiter = new RateLimiter();
    
    @Data
    public static class TriggerConditions {
        /**
         * 是否对所有事件进行推理
         */
        private boolean allEvents = false;
        
        /**
         * 最大置信度阈值
         */
        private double maxConfidence = 0.8;
        
        /**
         * 最小置信度阈值
         */
        private double minConfidence = 0.3;
        
        /**
         * 仅对目标动作推理
         */
        private boolean targetActionsOnly = true;
        
        /**
         * 仅对关键动作推理
         */
        private boolean criticalActionsOnly = false;
    }
    
    @Data
    public static class PromptConfig {
        /**
         * 系统提示词
         */
        private String systemPrompt;

        /**
         * 用户提示词
         */
        private String userPrompt = "请仔细分析这个视频中的人体动作行为";
    }
    
    @Data
    public static class ResultProcessing {
        /**
         * 是否保存结果
         */
        private boolean saveResults = true;
        
        /**
         * 结果保存目录
         */
        private String resultsDir = "llm_results";
        
        /**
         * 是否包含视频路径
         */
        private boolean includeVideoPath = true;
    }
    
    @Data
    public static class RateLimiter {
        /**
         * 是否启用限流
         */
        private boolean enabled = true;
        
        /**
         * 最大并发请求数
         */
        private int maxConcurrentRequests = 3;
        
        /**
         * 每分钟最大请求数
         */
        private int maxRequestsPerMinute = 30;
        
        /**
         * 每小时最大请求数
         */
        private int maxRequestsPerHour = 500;
        
        /**
         * 队列等待超时时间（秒）
         */
        private int queueTimeout = 30;
        
        /**
         * 重试延迟时间（秒）
         */
        private double retryDelay = 1.0;
    }
}
