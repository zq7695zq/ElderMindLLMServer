package com.zq.eldermindllmserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zq.eldermindllmserver.config.LLMConfig;
import com.zq.eldermindllmserver.model.LLMInferenceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 直接调用智谱AI API的服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZhipuAiDirectService {

    private final LLMConfig llmConfig;
    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    @Value("${spring.ai.zhipuai.api-key}")
    private String apiKey;
    
    private static final String ZHIPU_API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    
    /**
     * 执行视频推理
     */
    public LLMInferenceResult inference(String videoPath, String customPrompt) {
        if (!llmConfig.isEnabled()) {
            return LLMInferenceResult.failure("LLM推理已禁用", videoPath, llmConfig.getMode());
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取限流许可
            if (!rateLimiterService.acquirePermit()) {
                return LLMInferenceResult.failure("获取推理许可失败：限流", videoPath, llmConfig.getMode());
            }
            
            try {
                // 验证视频URL
                String videoUrl = validateVideoUrl(videoPath);
                if (videoUrl == null) {
                    return LLMInferenceResult.failure("无效的视频URL", videoPath, llmConfig.getMode());
                }
                
                // 准备提示词
                String prompt = createPromptWithEdgeDetectionResult(customPrompt);
                
                // 执行推理
                String response = performInference(videoUrl, prompt);
                
                // 解析响应
                Map<String, Object> parsedResult = parseJsonResponse(response);
                
                double inferenceTime = (System.currentTimeMillis() - startTime) / 1000.0;
                
                LLMInferenceResult result = LLMInferenceResult.success(
                    parsedResult, response, inferenceTime, videoPath, llmConfig.getMode());
                
                // 保存结果
                if (llmConfig.getResultProcessing().isSaveResults()) {
                    saveResult(result);
                }
                
                log.info("LLM推理完成，耗时: {}秒", String.format("%.2f", inferenceTime));
                return result;
                
            } finally {
                rateLimiterService.releasePermit();
            }
            
        } catch (Exception e) {
            log.error("LLM推理失败: {}", e.getMessage(), e);
            return LLMInferenceResult.failure("推理失败: " + e.getMessage(), videoPath, llmConfig.getMode());
        }
    }
    
    /**
     * 验证视频URL
     */
    private String validateVideoUrl(String videoPath) {
        try {
            // 如果是URL，直接返回
            if (videoPath.startsWith("http://") || videoPath.startsWith("https://")) {
                log.info("使用视频URL: {}", videoPath);
                return videoPath;
            }
            
            log.warn("本地文件路径需要转换为可访问的URL: {}", videoPath);
            return null; // 暂时不支持本地文件
            
        } catch (Exception e) {
            log.error("无效的视频路径: {}", videoPath, e);
            return null;
        }
    }
    
    /**
     * 执行推理 - 直接调用智谱AI HTTP API
     */
    private String performInference(String videoUrl, String prompt) {
        // 构建请求体，按照智谱AI API文档格式
        Map<String, Object> requestBody = Map.of(
            "model", llmConfig.getModel(),
            "messages", List.of(
                Map.of(
                    "role", "user",
                    "content", List.of(
                        Map.of(
                            "type", "video_url",
                            "video_url", Map.of("url", videoUrl)
                        ),
                        Map.of(
                            "type", "text",
                            "text", prompt
                        )
                    )
                )
            ),
            "temperature", 0.7,
            "max_tokens", 8192
        );
        
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        // 执行推理（带重试）
        Exception lastException = null;
        for (int attempt = 1; attempt <= llmConfig.getMaxRetries(); attempt++) {
            try {
                log.info("执行LLM推理，尝试次数: {}/{}", attempt, llmConfig.getMaxRetries());
                log.debug("请求体: {}", objectMapper.writeValueAsString(requestBody));
                
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    ZHIPU_API_URL,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> responseBody = response.getBody();

                    // 提取响应内容
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> choice = choices.get(0);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        String content = (String) message.get("content");
                        
                        log.info("LLM推理成功");
                        return content;
                    } else {
                        throw new RuntimeException("响应中没有找到choices字段");
                    }
                } else {
                    throw new RuntimeException("HTTP请求失败: " + response.getStatusCode());
                }
                
            } catch (Exception e) {
                lastException = e;
                if (attempt < llmConfig.getMaxRetries()) {
                    log.warn("LLM推理尝试 {}/{} 失败: {}, 重试中...", attempt, llmConfig.getMaxRetries(), e.getMessage());
                    try {
                        TimeUnit.SECONDS.sleep(1); // 重试延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("推理被中断", ie);
                    }
                } else {
                    log.error("LLM推理在 {} 次尝试后失败: {}", llmConfig.getMaxRetries(), e.getMessage());
                }
            }
        }
        
        throw new RuntimeException("推理失败: " + (lastException != null ? lastException.getMessage() : "未知错误"));
    }
    
    /**
     * 解析JSON响应
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String response) {
        try {
            // 尝试直接解析
            return objectMapper.readValue(response, Map.class);
        } catch (JsonProcessingException e) {
            try {
                // 尝试提取JSON部分
                int startIdx = response.indexOf('{');
                int endIdx = response.lastIndexOf('}') + 1;
                if (startIdx != -1 && endIdx > startIdx) {
                    String jsonStr = response.substring(startIdx, endIdx);
                    return objectMapper.readValue(jsonStr, Map.class);
                }
            } catch (JsonProcessingException ex) {
                log.warn("无法解析JSON响应，返回原始文本");
            }
        }
        
        // 如果无法解析为JSON，返回包含原始响应的Map
        return Map.of("raw_response", response, "parsed", false);
    }
    
    /**
     * 创建默认提示词
     */
    private String createDefaultPrompt() {
        String systemPrompt = llmConfig.getPromptConfig().getSystemPrompt();
        String userPrompt = llmConfig.getPromptConfig().getUserPrompt();

        // 使用模板替换动作列表
        if (StringUtils.hasText(systemPrompt)) {
            String enhancedPrompt = systemPrompt.replace("{ACTION_LIST}", getActionListDescription());
            return enhancedPrompt + "\n\n" + userPrompt;
        }
        return userPrompt;
    }

    /**
     * 获取动作列表描述
     */
    private String getActionListDescription() {
        String actionList;

        // 紧急情况
        actionList = "【紧急情况】（最高优先级）：\n" +
                "1. 摔倒 (falling) - ID:42\n" +
                "2. 恶心或呕吐 (nausea or vomiting) - ID:47\n" +

                // 健康相关
                "\n【健康状况】（高优先级）：\n" +
                "3. 蹒跚/摇摆 (staggering) - ID:41\n" +
                "4. 打喷嚏/咳嗽 (sneeze/cough) - ID:40\n" +
                "5. 摸头 (touch head) - ID:43\n" +
                "6. 摸胸部 (touch chest) - ID:44\n" +
                "7. 摸背部 (touch back) - ID:45\n" +
                "8. 摸脖子 (touch neck) - ID:46\n" +

                // 异常行为
                "\n【异常行为】（中等优先级）：\n" +
                "9. 打击/拍打 (punching/slapping) - ID:49\n" +
                "10. 踢他人 (kicking other person) - ID:50\n" +
                "11. 推他人 (pushing other person) - ID:51\n" +
                "12. 摸他人口袋 (touch other person's pocket) - ID:56\n" +
                "13. 掉落物品 (drop) - ID:4\n" +

                // 基本动作
                "\n【基本动作】（监控重点）：\n" +
                "14. 坐下 (sitting down) - ID:7\n" +
                "15. 起立 (standing up) - ID:8\n" +
                "16. 相向而行 (walking towards each other) - ID:58\n" +
                "17. 分开行走 (walking apart) - ID:59\n" +

                // 日常活动（常见的）
                "\n【日常活动】（正常行为）：\n" +
                "18. 喝水 (drink water) - ID:0\n" +
                "19. 吃饭/吃零食 (eat meal/snack) - ID:1\n" +
                "20. 刷牙 (brushing teeth) - ID:2\n" +
                "21. 阅读 (reading) - ID:10\n" +
                "22. 写字 (writing) - ID:11\n" +
                "23. 打电话 (make a phone call) - ID:27\n" +
                "24. 玩手机/平板 (playing with phone/tablet) - ID:28\n" +
                "25. 穿脱衣物鞋帽等\n" +

                // 手势和社交
                "\n【手势和社交】：\n" +
                "26. 挥手 (hand waving) - ID:22\n" +
                "27. 鼓掌 (clapping) - ID:9\n" +
                "28. 握手 (handshaking) - ID:57\n" +
                "29. 拥抱他人 (hugging other person) - ID:54\n" +
                "30. 其他手势动作\n" +

                "\n注意：如果检测到的动作不在上述列表中，请在返回结果中使用action_id: -1，并详细描述具体动作。";

        return actionList;
    }

    /**
     * 拼接边缘检测结果和LLM提示词
     */
    private String createPromptWithEdgeDetectionResult(String edgeDetectionResult) {
        return edgeDetectionResult + "\n\n" + createDefaultPrompt();
    }
    
    /**
     * 保存推理结果
     */
    private void saveResult(LLMInferenceResult result) {
        try {
            String resultsDir = llmConfig.getResultProcessing().getResultsDir();
            Path dirPath = Paths.get(resultsDir);
            Files.createDirectories(dirPath);
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String filename = String.format("llm_result_%s.json", timestamp);
            Path filePath = dirPath.resolve(filename);
            
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            Files.write(filePath, jsonContent.getBytes());
            
            log.debug("LLM结果已保存到: {}", filePath);
            
        } catch (Exception e) {
            log.error("保存LLM结果失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取服务状态
     */
    public Map<String, Object> getStatus() {
        return Map.of(
            "enabled", llmConfig.isEnabled(),
            "mode", llmConfig.getMode(),
            "model", llmConfig.getModel(),
            "apiUrl", ZHIPU_API_URL,
            "rateLimiter", rateLimiterService.getStatus(),
            "config", Map.of(
                "timeout", llmConfig.getTimeout(),
                "maxRetries", llmConfig.getMaxRetries(),
                "saveResults", llmConfig.getResultProcessing().isSaveResults()
            )
        );
    }
}
