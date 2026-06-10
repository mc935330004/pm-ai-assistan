package com.pm.ai.assistan.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.ai.assistan.unit.ApiResponse;
import com.pm.ai.assistan.vo.ProjectVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiModelService {

    private final ChatClient chatClient;

    private final ObjectMapper objectMapper;

    private static final String SYSTEM = """
            你是 PM 项目管理系统智能助手，名字叫路小科。
            你只能基于系统查询返回的项目数据回答，不要编造项目、人员、金额、状态或时间。
            如果项目数据为空，才可以明确告诉用户没有查询到项目信息。
            如果项目数据非空，禁止回答“未查询到”“找不到”“没有相关项目信息”。
            回答要简洁、清晰，适合企业管理人员阅读。
            不要回答和PM系统无关的内容。
            """;

    public String summary(String question, ApiResponse<ProjectVO> result) {
        List<ProjectVO> projects = result.getData();
        String projectsJson = toJson(projects);
        String userContent = """
                用户问题：
                %s
                系统查询到的数据：
                %s
                请基于以上项目数据回答，不要编造，不要说未查询到，除非项目数据为空。
                """.formatted(question, projectsJson);

        return chatClient.prompt()
                .system(SYSTEM)
                .user(userContent)
                .call()
                .content();
    }

    private String toJson(List<ProjectVO> projects) {
        try {
            return objectMapper.writeValueAsString(projects);
        } catch (JsonProcessingException e) {
            return String.valueOf(projects);
        }
    }
}
