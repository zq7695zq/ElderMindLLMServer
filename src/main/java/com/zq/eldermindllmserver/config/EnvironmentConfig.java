package com.zq.eldermindllmserver.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 环境变量配置类
 * 负责加载.env文件中的环境变量
 */
@Configuration
public class EnvironmentConfig {

    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure()
                .directory(".")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();
    }
}
