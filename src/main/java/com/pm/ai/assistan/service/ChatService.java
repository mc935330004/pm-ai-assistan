package com.pm.ai.assistan.service;

import com.pm.ai.assistan.dto.ChatRequest;
import com.pm.ai.assistan.unit.AgentResult;
import com.pm.ai.assistan.vo.ChatResponseVO;
import com.pm.ai.tools.tool.PmApiQueryRequest;
import com.pm.ai.tools.tool.PmApiQueryResponse;
import com.pm.ai.tools.tool.PmApiQueryTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

/**
 * 聊天服务入口：负责校验请求、注册 PM 查询工具并调用大模型生成回答。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    /**
     * Spring AI 聊天客户端。
     */
    private final ChatClient chatClient;

    /**
     * 单一 PM 查询工具，内部会根据 OpenAPI 目录路由到具体接口。
     */
    private final PmApiQueryTool pmApiQueryTool;

    /**
     * 系统提示词：明确要求模型需要 PM 数据时调用 pm_api_query。
     */
    private static final String SYSTEM = "You are Lu Xiaoke, the PM project management assistant.\n"
            + "When the user asks for PM system data, call the pm_api_query tool first.\n"
            + "Use only tool results or provided backend data to answer PM data questions.\n"
            + "Do not invent projects, contracts, amounts, dates, people, or statuses.\n"
            + "Only read-only PM queries are allowed. Refuse dangerous write operations.\n"
            + "Keep answers concise and professional.";

    /**
     * 普通聊天入口：为每次请求创建带当前 Authorization 的工具回调。
     */
    public AgentResult<ChatResponseVO> chat(ChatRequest request, String authorization) {
        try {
            // 校验用户身份头，避免未授权请求进入工具调用链。
            if (!StringUtils.hasText(authorization)) {
                throw new RuntimeException("Missing Authorization request header");
            }

            // 校验用户问题，避免空问题浪费模型调用。
            String question = request == null ? null : request.getQuestion();
            if (!StringUtils.hasText(question)) {
                throw new RuntimeException("Question must not be empty");
            }

            // 注册单一目录路由工具，模型只看到 pm_api_query，不直接看到 100 个接口。
            String answer = chatClient.prompt()
                    .system(SYSTEM)
                    .user(question).tools(ToolCallbackProvider.from(pmApiQueryTool.toolCallback(authorization)))
                    .call()
                    .content();

            // 返回前端聊天消息结构，引用数据由工具结果进入模型上下文。
            return AgentResult.success("success", ChatResponseVO.assistant(answer, null));
        } catch (Exception e) {
            // 捕获聊天链路异常，统一返回 AgentResult 失败结构。
            log.error("Chat request failed", e);
            return AgentResult.fail("Chat request failed", null);
        }
    }

    /**
     * 流式聊天入口：返回模型内容流，前端可以逐段渲染大模型输出。
     */
    public Flux<String> chatStream(ChatRequest request, String authorization) {
        try {
            // 流式接口同样校验 Authorization，确保工具调用可以透传用户身份。
            if (!StringUtils.hasText(authorization)) {
                return Flux.just("Missing Authorization request header");
            }

            // 流式接口同样校验问题内容，避免空请求进入模型调用。
            String question = request == null ? null : request.getQuestion();
            if (!StringUtils.hasText(question)) {
                return Flux.just("Question must not be empty");
            }

            PmApiQueryResponse pmApiResult = pmApiQueryTool.execute(
                    PmApiQueryRequest.builder().question(question).build(),
                    authorization
            );

            String system = SYSTEM;
            if (pmApiResult.isSuccess()) {
                // Spring AI 2.0.0-RC1 的 OpenAI 兼容流式 tool calling 在部分模型上会中断。
                // 流式入口先完成 PM 查询，再把真实后端数据作为上下文交给模型生成 SSE 内容。
                system = SYSTEM + "\n\nPM backend query result is already available.\n"
                        + "OperationId: " + pmApiResult.getOperationId() + "\n"
                        + "Data:\n" + pmApiResult.getData();
            } else {
                system = SYSTEM + "\n\nPM backend query was not executed successfully: "
                        + pmApiResult.getMessage();
            }

            // 使用 Spring AI stream API 返回 Flux<String>，前端可逐段渲染最终回答。
            return chatClient.prompt()
                    .system(system)
                    .user(question)
                    .stream()
                    .content()
                    .filter(content -> !content.isEmpty())
                    .onErrorResume(error -> {
                        log.error("流式响应处理失败", error);
                        return Flux.just("\n\n[响应处理出错，请重试]");
                    });
        } catch (Exception e) {
            // 构建流之前发生异常时，返回一个错误文本片段，避免连接直接无响应。
            log.error("流式聊天请求失败", e);
            return Flux.just("聊天请求失败");
        }
    }
}
