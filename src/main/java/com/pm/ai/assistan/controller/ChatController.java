package com.pm.ai.assistan.controller;

import com.pm.ai.assistan.dto.ChatRequest;
import com.pm.ai.assistan.service.ChatService;
import com.pm.ai.assistan.unit.AgentResult;
import com.pm.ai.assistan.vo.ChatResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * AI 聊天控制器：对外暴露普通聊天和流式聊天入口。
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat")
    public AgentResult<ChatResponseVO> chat(@RequestBody ChatRequest request,
                                            @RequestHeader("Authorization") String authorization) {
        // 普通聊天入口：交给服务层完成鉴权校验、工具调用和回答生成。
        return chatService.chat(request, authorization);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request,
                                   @RequestHeader("Authorization") String authorization) {
        // 流式聊天入口：返回 SSE 内容流，并复用服务层的 PM 工具调用链。
        return chatService.chatStream(request, authorization);
    }
}
