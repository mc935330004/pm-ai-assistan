package com.pm.ai.assistan.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * AI 模型服务：只负责把后端已经查询到的数据整理成自然语言回答。
 */
@Service
@RequiredArgsConstructor
public class AiModelService {

    private final ChatClient chatClient;

    private final ObjectMapper objectMapper;

    /**
     * 系统提示词：使用普通字符串，避免当前 Maven 编译链路对 text block 解析不稳定。
     */
    private static final String SYSTEM = "You are Lu Xiaoke, the PM project management assistant.\n"
            + "Answer only from backend system data supplied by the application.\n"
            + "Do not invent projects, people, amounts, status, or dates.\n"
            + "If the data is empty, clearly say no related data was found.\n"
            + "Keep answers concise and suitable for enterprise project managers.";

    /**
     * 根据用户问题和后端数据生成总结回答，禁止模型脱离真实数据发挥。
     */
    public String summary(String question, Object result) {
        String resultJson = toJson(result);
        // 用户提示词：把问题和后端数据放在一起交给模型总结。
        String userContent = "User question:\n"
                + question
                + "\nBackend data:\n"
                + resultJson
                + "\nAnswer based only on the backend data above.";

        return chatClient.prompt()
                .system(SYSTEM)
                .user(userContent)
                .call()
                .content();
    }

    private String toJson(Object result) {
        try {
            // 优先序列化成 JSON，方便模型读取结构化数据。
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            // 序列化失败时降级为普通字符串，避免聊天链路直接中断。
            return String.valueOf(result);
        }
    }
}
