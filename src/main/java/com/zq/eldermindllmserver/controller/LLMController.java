package com.zq.eldermindllmserver.controller;

import com.zq.eldermindllmserver.model.LLMInferenceResult;
import com.zq.eldermindllmserver.service.ZhipuAiDirectService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * LLM推理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
public class LLMController {
    
    private final ZhipuAiDirectService zhipuAiDirectService;
    
    /**
     * 视频推理接口
     */
    @PostMapping("/inference")
    public ResponseEntity<LLMInferenceResult> inference(@RequestBody InferenceRequest request) {
        log.info("收到视频推理请求: {}", request.getVideoUrl());
        
        try {
            LLMInferenceResult result = zhipuAiDirectService.inference(
                request.getVideoUrl(),
                request.getCustomPrompt()
            );
            
            if (result.isSuccess()) {
                log.info("视频推理成功完成");
                return ResponseEntity.ok(result);
            } else {
                log.warn("视频推理失败: {}", result.getError());
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            log.error("视频推理异常: {}", e.getMessage(), e);
            LLMInferenceResult errorResult = LLMInferenceResult.failure(
                "服务器内部错误: " + e.getMessage(), 
                request.getVideoUrl(), 
                "api"
            );
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 获取服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> status = zhipuAiDirectService.getStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("获取状态失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "获取状态失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "LLM Inference Service",
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * 推理请求数据结构
     */
    @Setter
    @Getter
    public static class InferenceRequest {
        @NotBlank(message = "视频URL不能为空")
        private String videoUrl;
        
        private String customPrompt;

    }
}
