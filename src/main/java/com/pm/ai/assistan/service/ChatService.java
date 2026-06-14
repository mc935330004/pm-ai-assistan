package com.pm.ai.assistan.service;

import com.pm.ai.assistan.auth.exception.PmAuthorizationMissingException;
import com.pm.ai.assistan.auth.service.AuthService;
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

@Service
@Slf4j
@RequiredArgsConstructor
/**
 * AI 聊天服务。
 * 这里把“本地登录 token”和“PM 系统 token”拆开：用户先登录本系统，再单独绑定 PM token。
 */
public class ChatService {

    /**
     * 系统提示词。
     * 要求模型在需要 PM 数据时先使用 pm_api_query 工具，避免凭空编造项目数据。
     */
    private static final String SYSTEM = """
            你是路小科，PM项目管理助理
            当用户请求PM系统数据时，请先调用pm_api_query工具。
            仅使用工具结果或提供的后端数据来回答PM数据问题
            不要虚构项目、合同、金额、日期、人员或状态
            只允许进行只读的PM查询。拒绝危险的写入操作。
            保持回答简洁且专业。
            """;

    /**
     * Spring AI 聊天客户端。
     */
    private final ChatClient chatClient;

    /**
     * PM 查询工具，负责根据 OpenAPI 目录路由到真实 PM 查询接口。
     */
    private final PmApiQueryTool pmApiQueryTool;

    /**
     * 认证服务，用于读取当前会话绑定的 PM token。
     */
    private final AuthService authService;

    /**
     * 普通聊天。
     * 会先校验问题内容，再读取 PM token 并把它绑定到工具回调中。
     */
    public AgentResult<ChatResponseVO> chat(ChatRequest request) {
        try {
            String question = request == null ? null : request.getQuestion();
            if (!StringUtils.hasText(question)) {
                throw new IllegalArgumentException("Question must not be empty");
            }

            String pmAuthorization = authService.currentPmAuthorization();
            String answer = chatClient.prompt()
                    .system(SYSTEM)
                    .user(question)
                    .tools(ToolCallbackProvider.from(pmApiQueryTool.toolCallback(pmAuthorization)))
                    .call()
                    .content();

            return AgentResult.success("success", ChatResponseVO.assistant(answer, null));
        } catch (PmAuthorizationMissingException e) {
            return AgentResult.fail(e.getMessage(), null);
        } catch (Exception e) {
            log.error("Chat request failed", e);
            return AgentResult.fail("Chat request failed", null);
        }
    }

    /**
     * 流式聊天。
     * 先同步查询 PM 数据，再把真实后端数据放进模型上下文中生成 SSE 内容。
     */
    public Flux<String> chatStream(ChatRequest request) {
        try {
            String question = request == null ? null : request.getQuestion();
            if (!StringUtils.hasText(question)) {
                return Flux.just("Question must not be empty");
            }

            String pmAuthorization = authService.currentPmAuthorization();
            PmApiQueryResponse pmApiResult = pmApiQueryTool.execute(
                    PmApiQueryRequest.builder().question(question).build(),
                    pmAuthorization
            );

            // Spring AI 当前流式 tool calling 在部分模型上不稳定，所以流式接口先查 PM，再交给模型总结。
            String system = SYSTEM;
            if (pmApiResult.isSuccess()) {
                system = SYSTEM + "\n\nPM backend query result is already available.\n"
                        + "OperationId: " + pmApiResult.getOperationId() + "\n"
                        + "Data:\n" + pmApiResult.getData();
            } else {
                system = SYSTEM + "\n "
                        + pmApiResult.getMessage();
            }

            return chatClient.prompt()
                    .system(system)
                    .user(question)
                    .stream()
                    .content()
                    .filter(content -> !content.isEmpty())
                    .onErrorResume(error -> {
                        log.error("Streaming chat response failed", error);
                        return Flux.just("\n\n[响应处理出错，请重试]");
                    });
        } catch (PmAuthorizationMissingException e) {
            return Flux.just(e.getMessage());
        } catch (Exception e) {
            log.error("Streaming chat request failed", e);
            return Flux.just("聊天请求失败");
        }
    }
}
