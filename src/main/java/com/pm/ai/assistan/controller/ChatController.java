package com.pm.ai.assistan.controller;

import com.pm.ai.assistan.dto.ChatRequest;
import com.pm.ai.assistan.service.ChatService;
import com.pm.ai.assistan.unit.AgentResult;
import com.pm.ai.assistan.vo.ChatResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatClient chatClient;

    private static final String SYSTEM = """
            你是 PM 项目管理系统智能助手，名字叫路小科。
            回答要简洁、清晰，适合企业管理人员阅读。
            """;

    @PostMapping("/chat")
    public AgentResult<ChatResponseVO> chat(@RequestBody ChatRequest request,
                                            @RequestHeader("Authorization") String authorization) {
        return chatService.chat(request, authorization);
    }

    @PostMapping("/chat/stream")
    public AgentResult<ChatResponseVO> chatStream(@RequestBody ChatRequest request) {
        String answer = chatClient.prompt()
                .system(SYSTEM)
                .user(request.getQuestion())
                .call()
                .content();
        return AgentResult.success("success", ChatResponseVO.assistant(answer, null));
    }
}
