package com.zq.eldermindllmserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动作映射服务类
 * 负责读取和管理动作映射数据
 */
@Slf4j
@Service
public class ActionMappingService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, ActionInfo> actionMappings = new HashMap<>();
    private final Map<String, CategoryInfo> categories = new HashMap<>();
    private final Set<Integer> criticalActions = new HashSet<>();
    private final Set<Integer> emergencyActions = new HashSet<>();
    private final Set<Integer> healthRelatedActions = new HashSet<>();
    
    /**
     * 初始化动作映射数据
     */
    @PostConstruct
    public void init() {
        try {
            loadActionMappings();
            log.info("动作映射数据加载成功，共加载 {} 个动作", actionMappings.size());
        } catch (Exception e) {
            log.error("加载动作映射数据失败", e);
            throw new RuntimeException("初始化动作映射服务失败", e);
        }
    }
    
    /**
     * 从静态文件加载动作映射数据
     */
    private void loadActionMappings() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/action-mappings.json");
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode rootNode = objectMapper.readTree(inputStream);
            
            // 加载动作映射
            JsonNode actionsNode = rootNode.get("actions");
            if (actionsNode != null) {
                actionsNode.fieldNames().forEachRemaining(actionId -> {
                    JsonNode actionNode = actionsNode.get(actionId);

                    ActionInfo actionInfo = new ActionInfo();
                    actionInfo.setId(Integer.parseInt(actionId));
                    actionInfo.setEnglish(actionNode.get("english").asText());
                    actionInfo.setChinese(actionNode.get("chinese").asText());
                    actionInfo.setCategory(actionNode.get("category").asText());
                    actionInfo.setRiskLevel(actionNode.get("risk_level").asText());
                    actionInfo.setCritical(actionNode.get("is_critical").asBoolean());

                    actionMappings.put(actionId, actionInfo);
                });
            }
            
            // 加载分类信息
            JsonNode categoriesNode = rootNode.get("categories");
            if (categoriesNode != null) {
                categoriesNode.fieldNames().forEachRemaining(categoryName -> {
                    JsonNode categoryNode = categoriesNode.get(categoryName);

                    CategoryInfo categoryInfo = new CategoryInfo();
                    categoryInfo.setName(categoryName);
                    categoryInfo.setDescription(categoryNode.get("description").asText());
                    categoryInfo.setMonitoringLevel(categoryNode.get("monitoring_level").asText());

                    categories.put(categoryName, categoryInfo);
                });
            }
            
            // 加载关键动作列表
            JsonNode criticalActionsNode = rootNode.get("critical_actions");
            if (criticalActionsNode != null && criticalActionsNode.isArray()) {
                criticalActionsNode.forEach(node -> criticalActions.add(node.asInt()));
            }
            
            // 加载紧急动作列表
            JsonNode emergencyActionsNode = rootNode.get("emergency_actions");
            if (emergencyActionsNode != null && emergencyActionsNode.isArray()) {
                emergencyActionsNode.forEach(node -> emergencyActions.add(node.asInt()));
            }
            
            // 加载健康相关动作列表
            JsonNode healthRelatedActionsNode = rootNode.get("health_related_actions");
            if (healthRelatedActionsNode != null && healthRelatedActionsNode.isArray()) {
                healthRelatedActionsNode.forEach(node -> healthRelatedActions.add(node.asInt()));
            }
        }
    }
    
    /**
     * 根据动作ID获取动作信息
     */
    public ActionInfo getActionInfo(int actionId) {
        return actionMappings.get(String.valueOf(actionId));
    }
    
    /**
     * 根据动作ID获取中文名称
     */
    public String getChineseName(int actionId) {
        ActionInfo actionInfo = getActionInfo(actionId);
        return actionInfo != null ? actionInfo.getChinese() : "未知动作";
    }
    

    
    /**
     * 判断是否为关键动作
     */
    public boolean isCriticalAction(int actionId) {
        return criticalActions.contains(actionId);
    }
    
    /**
     * 判断是否为紧急动作
     */
    public boolean isEmergencyAction(int actionId) {
        return emergencyActions.contains(actionId);
    }
    
    /**
     * 判断是否为健康相关动作
     */
    public boolean isHealthRelatedAction(int actionId) {
        return healthRelatedActions.contains(actionId);
    }
    
    /**
     * 获取所有关键动作的中文名称列表
     */
    public List<String> getCriticalActionNames() {
        return criticalActions.stream()
                .map(this::getChineseName)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有紧急动作的中文名称列表
     */
    public List<String> getEmergencyActionNames() {
        return emergencyActions.stream()
                .map(this::getChineseName)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有健康相关动作的中文名称列表
     */
    public List<String> getHealthRelatedActionNames() {
        return healthRelatedActions.stream()
                .map(this::getChineseName)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据分类获取动作列表
     */
    public List<ActionInfo> getActionsByCategory(String category) {
        return actionMappings.values().stream()
                .filter(action -> category.equals(action.getCategory()))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有分类信息
     */
    public Map<String, CategoryInfo> getCategories() {
        return new HashMap<>(categories);
    }
    
    /**
     * 生成用于system-prompt的关键事件描述
     */
    public String generateCriticalEventsDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("请重点关注以下关键事件：\n");
        
        // 紧急情况
        sb.append("\n【紧急情况】（需要立即响应）：\n");
        emergencyActions.forEach(actionId -> {
            ActionInfo action = getActionInfo(actionId);
            if (action != null) {
                sb.append(String.format("- %s (%s)\n", action.getChinese(), action.getEnglish()));
            }
        });
        
        // 健康相关
        sb.append("\n【健康状况】（需要密切关注）：\n");
        healthRelatedActions.forEach(actionId -> {
            ActionInfo action = getActionInfo(actionId);
            if (action != null && !emergencyActions.contains(actionId)) {
                sb.append(String.format("- %s (%s)\n", action.getChinese(), action.getEnglish()));
            }
        });
        
        // 其他关键动作
        sb.append("\n【其他关键动作】：\n");
        criticalActions.forEach(actionId -> {
            ActionInfo action = getActionInfo(actionId);
            if (action != null && !emergencyActions.contains(actionId) && !healthRelatedActions.contains(actionId)) {
                sb.append(String.format("- %s (%s)\n", action.getChinese(), action.getEnglish()));
            }
        });
        
        return sb.toString();
    }
    
    /**
     * 动作信息数据类
     */
    @Data
    public static class ActionInfo {
        private int id;
        private String english;
        private String chinese;
        private String category;
        private String riskLevel;
        private boolean critical;
    }
    
    /**
     * 分类信息数据类
     */
    @Data
    public static class CategoryInfo {
        private String name;
        private String description;
        private String monitoringLevel;
    }
}
