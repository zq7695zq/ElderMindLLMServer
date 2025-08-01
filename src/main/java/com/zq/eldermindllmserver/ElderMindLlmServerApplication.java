package com.zq.eldermindllmserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class ElderMindLlmServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ElderMindLlmServerApplication.class, args);
	}

}
