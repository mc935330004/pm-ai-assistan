package com.pm.ai.tools.http;

import com.pm.ai.tools.catalog.ApiOperationDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * PM 系统动态调用器：根据接口目录定义发起真实 HTTP 请求。
 */
@Component
public class DynamicPmApiCaller {

    /**
     * HTTP 客户端，便于测试中替换为 MockRestServiceServer。
     */
    private final RestTemplate restTemplate;

    /**
     * PM 系统基础地址，例如 http://localhost:9999/pm。
     */
    private final String baseUrl;

    /**
     * Spring 注入构造方法。
     */
    public DynamicPmApiCaller(RestTemplate restTemplate,
                              @Value("${pm.system.base-url:http://localhost:9999/pm}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = trimTrailingSlash(baseUrl);
    }

    /**
     * 按接口定义调用 PM 系统，第一阶段只允许 GET 查询接口。
     */
    public String call(ApiOperationDefinition operation, Map<String, Object> queryParams, String authorization) {
        if (operation == null || !"GET".equalsIgnoreCase(operation.getMethod())) {
            throw new IllegalArgumentException("Only GET operations are allowed");
        }

        // 拼接 URL 和查询参数。
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl + operation.getPath());
        if (queryParams != null) {
            queryParams.forEach(uriBuilder::queryParam);
        }

        // 透传用户 Authorization，确保沿用 PM 系统原有权限控制。
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                uriBuilder.build(true).toUri(),
                HttpMethod.GET,
                entity,
                String.class
        );
        return response.getBody();
    }

    /**
     * 去掉基础地址末尾斜杠，避免和接口 path 拼接时出现双斜杠。
     */
    private String trimTrailingSlash(String value) {
        if (value == null || !value.endsWith("/")) {
            return value;
        }
        return value.substring(0, value.length() - 1);
    }
}
