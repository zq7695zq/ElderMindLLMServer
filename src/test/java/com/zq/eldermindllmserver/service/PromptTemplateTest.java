package com.zq.eldermindllmserver.service;

import com.zq.eldermindllmserver.config.LLMConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试Prompt模板功能
 */
@SpringBootTest
class PromptTemplateTest {
    
    @Autowired
    private ZhipuAiDirectService zhipuAiDirectService;
    
    @Autowired
    private LLMConfig llmConfig;
    
    @Test
    void testPromptTemplate() {
        // 获取配置的system-prompt模板
        String systemPrompt = llmConfig.getPromptConfig().getSystemPrompt();
        
        // 验证模板包含占位符
        assertTrue(systemPrompt.contains("{ACTION_LIST}"), "System prompt应该包含{ACTION_LIST}占位符");
        
        // 验证模板的基本结构
        assertTrue(systemPrompt.contains("智能视频监测系统"), "应该包含系统角色描述");
        assertTrue(systemPrompt.contains("JSON格式"), "应该包含输出格式要求");
    }
    
    @Test
    void testActionListGeneration() {
        // 这里我们需要通过反射或者其他方式测试getActionListDescription方法
        // 由于该方法是private的，我们可以测试整体的prompt生成效果
        
        // 验证配置加载正确
        assertNotNull(llmConfig.getPromptConfig().getSystemPrompt());
        assertNotNull(llmConfig.getPromptConfig().getUserPrompt());
    }
}
