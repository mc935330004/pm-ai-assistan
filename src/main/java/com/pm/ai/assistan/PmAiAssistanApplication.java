package com.pm.ai.assistan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class PmAiAssistanApplication {

    public static void main(String[] args) {
        SpringApplication.run(PmAiAssistanApplication.class, args);
    }
}
