package com.zq.eldermindllmserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 提示词生成服务
 * 根据动作映射数据动态生成system-prompt
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptGeneratorService {
    
    private final ActionMappingService actionMappingService;
    
    /**
     * 生成智能视频监测系统的system-prompt
     */
    public String generateSystemPrompt() {
        String prompt = "你是一名专业的智能视频监测系统，专注于监测人类在室内的行为事件。\n" +
                "你的主要任务是分析视频内容，识别人体动作行为，并评估其风险等级。\n\n" +

                // 关键事件描述
                actionMappingService.generateCriticalEventsDescription() +

                // 分析要求
                "\n【分析要求】：\n" +
                "1. 仔细观察视频中人物的动作行为\n" +
                "2. 识别具体的动作类型和风险等级\n" +
                "3. 特别关注紧急情况和健康相关的异常行为\n" +
                "4. 评估是否需要立即干预或医疗援助\n\n" +

                // 输出格式要求
                "【输出格式】：\n" +
                "请以结构化JSON格式输出识别结果，示例如下：\n" +
                "{\n" +
                "  \"detected_action\": {\n" +
                "    \"action_id\": 42,\n" +
                "    \"chinese_name\": \"摔倒\",\n" +
                "    \"english_name\": \"falling\",\n" +
                "    \"category\": \"紧急情况\",\n" +
                "    \"risk_level\": \"紧急\"\n" +
                "  },\n" +
                "  \"analysis\": {\n" +
                "    \"confidence\": 0.95,\n" +
                "    \"description\": \"检测到人员摔倒，需要立即关注\",\n" +
                "    \"is_critical\": true,\n" +
                "    \"is_emergency\": true,\n" +
                "    \"requires_immediate_attention\": true\n" +
                "  },\n" +
                "  \"recommendations\": {\n" +
                "    \"alert_level\": \"紧急\",\n" +
                "    \"suggested_actions\": [\"立即派遣医护人员\", \"通知紧急联系人\"],\n" +
                "    \"monitoring_priority\": \"最高\"\n" +
                "  }\n" +
                "}\n\n" +

                // 特殊说明
                "【特殊说明】：\n" +
                "- 如果检测到的动作不在预定义列表中，请在action_id字段使用-1，并在chinese_name中描述具体动作\n" +
                "- 对于紧急情况（如摔倒、呕吐），请将is_emergency设置为true\n" +
                "- 对于健康相关动作（如摸头、摸胸等），请特别关注是否存在不适症状\n" +
                "- 置信度应基于动作的清晰度和识别准确性进行评估\n";

        return prompt;
    }
    
    /**
     * 生成简化版的system-prompt（用于快速识别）
     */
    public String generateSimpleSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一名智能视频监测系统，专注于监测人类在室内的行为事件。\n");
        prompt.append("请分析该视频并识别是否发生了以下关键事件：\n\n");
        
        // 获取关键动作列表
        List<String> emergencyActions = actionMappingService.getEmergencyActionNames();
        List<String> healthActions = actionMappingService.getHealthRelatedActionNames();
        List<String> criticalActions = actionMappingService.getCriticalActionNames();
        
        // 紧急事件
        prompt.append("【紧急事件】：\n");
        for (int i = 0; i < emergencyActions.size(); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, emergencyActions.get(i)));
        }
        
        // 健康相关事件
        prompt.append("\n【健康相关事件】：\n");
        int counter = emergencyActions.size() + 1;
        for (String action : healthActions) {
            if (!emergencyActions.contains(action)) {
                prompt.append(String.format("%d. %s\n", counter++, action));
            }
        }
        
        // 其他关键事件
        prompt.append("\n【其他关键事件】：\n");
        for (String action : criticalActions) {
            if (!emergencyActions.contains(action) && !healthActions.contains(action)) {
                prompt.append(String.format("%d. %s\n", counter++, action));
            }
        }
        
        prompt.append(String.format("\n%d. 其他\n", counter));
        
        prompt.append("\n请以结构化JSON格式输出识别结果，示例如下：\n");
        prompt.append("{\n");
        prompt.append("  \"type\": \"摔倒\",\n");
        prompt.append("  \"confidence\": 0.95,\n");
        prompt.append("  \"description\": \"详细描述\",\n");
        prompt.append("  \"risk_level\": \"紧急\",\n");
        prompt.append("  \"requires_attention\": true\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    /**
     * 根据配置生成适合的system-prompt
     */
    public String generatePromptByMode(String mode) {
        return switch (mode.toLowerCase()) {
            case "simple" -> generateSimpleSystemPrompt();
            case "detailed", "full" -> generateSystemPrompt();
            default -> {
                log.warn("未知的prompt模式: {}，使用默认模式", mode);
                yield generateSystemPrompt();
            }
        };
    }
}
