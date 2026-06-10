package com.pm.ai.assistan.vo;

import lombok.Data;

@Data
public class ChatMessageVO {

    /**
     * Message role: user, assistant, or system.
     */
    private String role;

    /**
     * Message content type, currently text.
     */
    private String contentType;

    /**
     * Message text rendered by the chat page.
     */
    private String content;

    /**
     * Milliseconds since epoch.
     */
    private Long createdAt;

    public static ChatMessageVO assistant(String content) {
        ChatMessageVO message = new ChatMessageVO();
        message.setRole("assistant");
        message.setContentType("text");
        message.setContent(content);
        message.setCreatedAt(System.currentTimeMillis());
        return message;
    }
}
