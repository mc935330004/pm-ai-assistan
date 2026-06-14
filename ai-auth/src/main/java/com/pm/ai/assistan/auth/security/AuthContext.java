package com.pm.ai.assistan.auth.security;

import com.pm.ai.assistan.auth.exception.AuthException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthContext {

    private AuthContext() {
    }

    /**
     * 获取当前登录用户。
     * 如果没有通过 BearerAuthenticationFilter 建立登录态，会抛出业务异常。
     */
    public static AuthPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new AuthException("请先登录");
        }
        return principal;
    }
}
