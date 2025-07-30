package com.zq.eldermindllmserver.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试.env配置是否正确加载
 */
@SpringBootTest
@TestPropertySource(properties = {
    "ZHIPUAI_API_KEY=test-api-key-12345"
})
class DotenvConfigurationTest {

    @Value("${spring.ai.zhipuai.api-key:}")
    private String apiKey;

    @Test
    void testApiKeyConfiguration() {
        // 验证API Key是否被正确配置
        assertNotNull(apiKey, "API Key should not be null");
        assertFalse(apiKey.isEmpty(), "API Key should not be empty");
        
        // 如果使用测试属性，应该是测试值
        if (apiKey.equals("test-api-key-12345")) {
            assertEquals("test-api-key-12345", apiKey, "Test API Key should match");
        } else {
            // 如果使用.env文件，应该不是默认值
            assertNotEquals("your-api-key-here", apiKey, "API Key should not be the default placeholder");
        }
    }

    @Test
    void testEnvironmentVariableLoading() {
        // 测试环境变量是否可以被正确读取
        String envApiKey = System.getProperty("ZHIPUAI_API_KEY");
        if (envApiKey != null) {
            assertFalse(envApiKey.isEmpty(), "Environment API Key should not be empty if set");
        }
    }
}
