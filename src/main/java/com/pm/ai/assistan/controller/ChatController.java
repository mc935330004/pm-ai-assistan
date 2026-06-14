package com.pm.ai.assistan.controller;

import com.pm.ai.assistan.dto.ChatRequest;
import com.pm.ai.assistan.service.ChatService;
import com.pm.ai.assistan.unit.AgentResult;
import com.pm.ai.assistan.vo.ChatResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
/**
 * AI 聊天控制器。
 * 鉴权由 Spring Security 完成，所以这里不再手动接收 Authorization 请求头。
 */
public class ChatController {

    private final ChatService chatService;

    /**
     * 普通聊天接口。
     * 如果问题需要 PM 数据，服务层会用当前会话绑定的 PM token 调用 PM 系统。
     */
    @PostMapping("/chat")
    public AgentResult<ChatResponseVO> chat(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    /**
     * 流式聊天接口。
     * 仍然要求本地登录 token；PM 查询授权从 Redis 会话中读取。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        // Spring MVC 下直接返回 Flux<String> 时，开发代理和浏览器容易遇到 chunked 连接未正常结束的问题。
        // 使用 SseEmitter 可以明确控制每个 SSE 片段的发送、完成和异常收尾。
        SseEmitter emitter = new SseEmitter(0L);
        AtomicReference<Disposable> subscriptionRef = new AtomicReference<>();

        emitter.onCompletion(() -> disposeSubscription(subscriptionRef));
        emitter.onTimeout(() -> {
            disposeSubscription(subscriptionRef);
            emitter.complete();
        });
        emitter.onError(error -> disposeSubscription(subscriptionRef));

        SecurityContext securityContext = SecurityContextHolder.getContext();
        CompletableFuture.runAsync(() -> {
            try {
                // SSE 推送放到异步线程后，需要把当前登录上下文带过去，服务层才能读取当前用户和 PM token 绑定关系。
                SecurityContextHolder.setContext(securityContext);
                Disposable subscription = chatService.chatStream(request)
                        .subscribe(
                                chunk -> sendSseChunk(emitter, chunk, subscriptionRef),
                                emitter::completeWithError,
                                () -> completeSseEmitter(emitter, subscriptionRef)
                        );
                subscriptionRef.set(subscription);
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
        return emitter;
    }

    private void sendSseChunk(SseEmitter emitter, String chunk, AtomicReference<Disposable> subscriptionRef) {
        try {
            emitter.send(SseEmitter.event().data(chunk));
        } catch (IOException | IllegalStateException e) {
            // 客户端断开或代理关闭时停止继续推送，避免后台线程继续写已经关闭的响应。
            disposeSubscription(subscriptionRef);
            emitter.completeWithError(e);
        }
    }

    private void completeSseEmitter(SseEmitter emitter, AtomicReference<Disposable> subscriptionRef) {
        try {
            // 给前端一个明确的结束标记，避免开发代理没有及时关闭 chunked 连接时页面一直等待。
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
        } catch (IOException | IllegalStateException e) {
            emitter.completeWithError(e);
        } finally {
            disposeSubscription(subscriptionRef);
        }
    }

    private void disposeSubscription(AtomicReference<Disposable> subscriptionRef) {
        Disposable subscription = subscriptionRef.getAndSet(null);
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }
}
