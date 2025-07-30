package com.zq.eldermindllmserver.controller;

import com.zq.eldermindllmserver.service.ActionMappingService;
import com.zq.eldermindllmserver.service.PromptGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 动作映射管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/actions")
@RequiredArgsConstructor
public class ActionMappingController {
    
    private final ActionMappingService actionMappingService;
    private final PromptGeneratorService promptGeneratorService;
    
    /**
     * 获取所有动作映射
     */
    @GetMapping("/mappings")
    public ResponseEntity<Map<String, Object>> getAllMappings() {
        try {
            Map<String, Object> response = Map.of(
                "actions", actionMappingService.getActionInfo(0) != null ? "loaded" : "not loaded",
                "categories", actionMappingService.getCategories(),
                "critical_actions", actionMappingService.getCriticalActionNames(),
                "emergency_actions", actionMappingService.getEmergencyActionNames(),
                "health_related_actions", actionMappingService.getHealthRelatedActionNames()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取动作映射失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "获取动作映射失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 根据动作ID获取动作信息
     */
    @GetMapping("/action/{actionId}")
    public ResponseEntity<Object> getActionInfo(@PathVariable int actionId) {
        try {
            ActionMappingService.ActionInfo actionInfo = actionMappingService.getActionInfo(actionId);
            if (actionInfo != null) {
                return ResponseEntity.ok(actionInfo);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("获取动作信息失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "获取动作信息失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 获取关键动作列表
     */
    @GetMapping("/critical")
    public ResponseEntity<List<String>> getCriticalActions() {
        try {
            List<String> criticalActions = actionMappingService.getCriticalActionNames();
            return ResponseEntity.ok(criticalActions);
        } catch (Exception e) {
            log.error("获取关键动作列表失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取紧急动作列表
     */
    @GetMapping("/emergency")
    public ResponseEntity<List<String>> getEmergencyActions() {
        try {
            List<String> emergencyActions = actionMappingService.getEmergencyActionNames();
            return ResponseEntity.ok(emergencyActions);
        } catch (Exception e) {
            log.error("获取紧急动作列表失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取健康相关动作列表
     */
    @GetMapping("/health-related")
    public ResponseEntity<List<String>> getHealthRelatedActions() {
        try {
            List<String> healthActions = actionMappingService.getHealthRelatedActionNames();
            return ResponseEntity.ok(healthActions);
        } catch (Exception e) {
            log.error("获取健康相关动作列表失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 根据分类获取动作列表
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ActionMappingService.ActionInfo>> getActionsByCategory(@PathVariable String category) {
        try {
            List<ActionMappingService.ActionInfo> actions = actionMappingService.getActionsByCategory(category);
            return ResponseEntity.ok(actions);
        } catch (Exception e) {
            log.error("根据分类获取动作列表失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取动态生成的system-prompt
     */
    @GetMapping("/prompt/system")
    public ResponseEntity<Map<String, String>> getSystemPrompt(@RequestParam(defaultValue = "detailed") String mode) {
        try {
            String prompt = promptGeneratorService.generatePromptByMode(mode);
            return ResponseEntity.ok(Map.of(
                "mode", mode,
                "prompt", prompt
            ));
        } catch (Exception e) {
            log.error("生成system-prompt失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "生成system-prompt失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 获取简化版system-prompt
     */
    @GetMapping("/prompt/simple")
    public ResponseEntity<Map<String, String>> getSimpleSystemPrompt() {
        try {
            String prompt = promptGeneratorService.generateSimpleSystemPrompt();
            return ResponseEntity.ok(Map.of(
                "mode", "simple",
                "prompt", prompt
            ));
        } catch (Exception e) {
            log.error("生成简化版system-prompt失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "生成简化版system-prompt失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 获取详细版system-prompt
     */
    @GetMapping("/prompt/detailed")
    public ResponseEntity<Map<String, String>> getDetailedSystemPrompt() {
        try {
            String prompt = promptGeneratorService.generateSystemPrompt();
            return ResponseEntity.ok(Map.of(
                "mode", "detailed",
                "prompt", prompt
            ));
        } catch (Exception e) {
            log.error("生成详细版system-prompt失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "生成详细版system-prompt失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 检查动作是否为关键动作
     */
    @GetMapping("/check/critical/{actionId}")
    public ResponseEntity<Map<String, Object>> checkCriticalAction(@PathVariable int actionId) {
        try {
            boolean isCritical = actionMappingService.isCriticalAction(actionId);
            boolean isEmergency = actionMappingService.isEmergencyAction(actionId);
            boolean isHealthRelated = actionMappingService.isHealthRelatedAction(actionId);
            
            ActionMappingService.ActionInfo actionInfo = actionMappingService.getActionInfo(actionId);
            
            return ResponseEntity.ok(Map.of(
                "action_id", actionId,
                "action_name", actionInfo != null ? actionInfo.getChinese() : "未知动作",
                "is_critical", isCritical,
                "is_emergency", isEmergency,
                "is_health_related", isHealthRelated
            ));
        } catch (Exception e) {
            log.error("检查关键动作失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "检查关键动作失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 获取所有分类信息
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, ActionMappingService.CategoryInfo>> getCategories() {
        try {
            Map<String, ActionMappingService.CategoryInfo> categories = actionMappingService.getCategories();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("获取分类信息失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
