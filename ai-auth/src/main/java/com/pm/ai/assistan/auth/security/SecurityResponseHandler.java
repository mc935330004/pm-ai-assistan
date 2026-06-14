package com.pm.ai.assistan.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.ai.assistan.auth.dto.AuthResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
/**
 * Spring Security 异常响应处理器。
 * 统一返回 AuthResult JSON，避免前端收到默认 HTML 错误页。
 */
public class SecurityResponseHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * 未登录或 token 失效时返回 401。
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        write(response, HttpStatus.UNAUTHORIZED, "登录已过期或未登录");
    }

    /**
     * 已登录但权限不足时返回 403。
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        write(response, HttpStatus.FORBIDDEN, "没有访问权限");
    }

    /**
     * 写出统一 JSON 响应。
     */
    private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), AuthResult.fail(message));
    }
}
