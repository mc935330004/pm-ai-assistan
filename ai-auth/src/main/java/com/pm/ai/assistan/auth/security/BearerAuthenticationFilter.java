package com.pm.ai.assistan.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
/**
 * Bearer token 认证过滤器。
 * 每个请求进来时解析本地登录 token，查 Redis 会话并建立 SecurityContext。
 */
public class BearerAuthenticationFilter extends OncePerRequestFilter {

    private final AuthSessionService authSessionService;

    /**
     * 请求过滤入口。
     * 找到有效会话就设置 Authentication，找不到则继续交给后续鉴权规则返回 401。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        authSessionService.extractBearerToken(header)
                .flatMap(authSessionService::findByRawToken)
                .ifPresent(session -> {
                    AuthPrincipal principal = authSessionService.toPrincipal(session);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            authorities(principal)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
        filterChain.doFilter(request, response);
    }

    /**
     * 把角色和接口权限转换成 Spring Security authority。
     * 角色统一加 ROLE_ 前缀，接口权限直接使用编码。
     */
    private List<SimpleGrantedAuthority> authorities(AuthPrincipal principal) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        principal.getRoles().stream()
                .filter(StringUtils::hasText)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        principal.getPermissions().stream()
                .map(PermissionGrant::getCode)
                .filter(StringUtils::hasText)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        return authorities;
    }
}
