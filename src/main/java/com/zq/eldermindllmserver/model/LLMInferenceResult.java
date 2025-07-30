package com.zq.eldermindllmserver.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * LLM推理结果数据结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LLMInferenceResult {
    
    /**
     * 推理是否成功
     */
    private boolean success;
    
    /**
     * 解析后的结构化结果
     */
    private Map<String, Object> result;
    
    /**
     * 原始响应内容
     */
    private String rawResponse;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 推理耗时（秒）
     */
    private Double inferenceTime;
    
    /**
     * 视频路径或URL
     */
    private String videoPath;
    
    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 推理模式
     */
    private String mode;
    
    /**
     * 原始事件信息（如果有）
     */
    private OriginalEvent originalEvent;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OriginalEvent {
        /**
         * 动作ID
         */
        private Integer actionId;
        
        /**
         * 动作名称
         */
        private String actionName;
        
        /**
         * 置信度
         */
        private Double confidence;
        
        /**
         * 帧ID
         */
        private Long frameId;
        
        /**
         * 事件时间戳
         */
        private LocalDateTime eventTimestamp;
    }
    
    /**
     * 创建成功结果
     */
    public static LLMInferenceResult success(Map<String, Object> result, String rawResponse, 
                                           double inferenceTime, String videoPath, String mode) {
        return LLMInferenceResult.builder()
                .success(true)
                .result(result)
                .rawResponse(rawResponse)
                .inferenceTime(inferenceTime)
                .videoPath(videoPath)
                .mode(mode)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static LLMInferenceResult failure(String error, String videoPath, String mode) {
        return LLMInferenceResult.builder()
                .success(false)
                .error(error)
                .videoPath(videoPath)
                .mode(mode)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
