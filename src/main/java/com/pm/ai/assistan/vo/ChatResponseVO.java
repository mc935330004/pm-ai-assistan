package com.pm.ai.assistan.vo;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class ChatResponseVO {

    /**
     * Chat messages for a ChatGPT-like page.
     */
    private List<ChatMessageVO> messages;

    /**
     * Original project data used to generate the assistant message.
     */
    private List<ProjectVO> references;

    public static ChatResponseVO assistant(String content, List<ProjectVO> references) {
        ChatResponseVO response = new ChatResponseVO();
        response.setMessages(Collections.singletonList(ChatMessageVO.assistant(content)));
        response.setReferences(references);
        return response;
    }
}
