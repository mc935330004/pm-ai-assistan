package com.pm.ai.assistan.auth.config;

import com.pm.ai.assistan.auth.security.BearerAuthenticationFilter;
import com.pm.ai.assistan.auth.security.SecurityResponseHandler;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthProperties.class)
@MapperScan("com.pm.ai.assistan.auth.mapper")
/**
 * 认证权限模块的 Spring Security 主配置。
 * 主应用只需要引入 ai-auth 依赖，即可获得登录、鉴权、Mapper 扫描和 CORS 配置。
 */
public class SecurityConfig {

    /**
     * 自定义 Bearer token 过滤器，用 Redis 会话建立当前登录用户。
     */
    private final BearerAuthenticationFilter bearerAuthenticationFilter;

    /**
     * 统一输出 JSON 的安全异常处理器。
     */
    private final SecurityResponseHandler securityResponseHandler;

    /**
     * 认证模块配置，用来读取前端跨域白名单。
     */
    private final AuthProperties authProperties;

    /**
     * 配置安全过滤链。
     * 本项目采用前后端分离 + Redis token，所以禁用 CSRF，使用无状态 Session。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(securityResponseHandler)
                        .accessDeniedHandler(securityResponseHandler)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/auth/login", "/auth/register").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/ai/**").hasAuthority("AI_CHAT")
                        .requestMatchers(HttpMethod.GET, "/auth/me").hasAuthority("AUTH_ME")
                        .requestMatchers(HttpMethod.POST, "/auth/logout").hasAuthority("AUTH_LOGOUT")
                        .requestMatchers(HttpMethod.POST, "/auth/pm-token").hasAuthority("AUTH_PM_TOKEN")
                        .requestMatchers("/auth/admin/**").hasAuthority("AUTH_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(bearerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new EarlyExceptionHandlingFilter(), ExceptionTranslationFilter.class)
                .build();
    }

    /**
     * 密码加密器。
     * 用户密码写库前用 BCrypt，不保存明文。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 覆盖 Spring Security 默认内存用户。
     * 否则启动日志会生成一个临时密码，容易误导后续排查。
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }

    /**
     * 本地开发跨域配置。
     * 前端通过 localhost、127.0.0.1 或局域网 IP 访问 Vite 时，浏览器会携带 Origin。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(authProperties.getCors().getAllowedOriginPatterns());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 早期异常处理过滤器。
     * 在 ExceptionTranslationFilter 之前捕获授权异常，避免响应提交后再次处理。
     */
    static class EarlyExceptionHandlingFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            try {
                filterChain.doFilter(request, response);
            } catch (AuthorizationDeniedException e) {
                // 如果响应已提交，直接返回，不再处理
                if (response.isCommitted()) {
                    return;
                }
                // 重新抛出异常，让 ExceptionTranslationFilter 处理
                throw e;
            }
        }
    }
}
