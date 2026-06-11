package com.pm.ai.assistan.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 聊天流式接口测试：保证控制器以 SSE 流的形式返回大模型内容。
 */
class ChatControllerStreamTest {

    @Test
    void chatStreamReturnsFluxWithEventStreamMediaType() throws NoSuchMethodException {
        // 通过反射检查接口签名，避免测试依赖真实大模型调用。
        Method method = ChatController.class.getMethod(
                "chatStream",
                com.pm.ai.assistan.dto.ChatRequest.class,
                String.class
        );

        // 流式接口必须返回 Flux，前端才能逐段接收模型输出。
        assertThat(method.getReturnType()).isEqualTo(Flux.class);

        // 流式接口必须声明 text/event-stream，便于浏览器和前端框架按 SSE 处理。
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertThat(postMapping.produces()).contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
