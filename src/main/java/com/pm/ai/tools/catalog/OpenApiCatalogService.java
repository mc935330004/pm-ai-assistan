package com.pm.ai.tools.catalog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAPI 接口目录服务：负责拉取、解析和缓存 PM 系统接口目录。
 */
@Service
public class OpenApiCatalogService {

    /**
     * OpenAPI 文档 HTTP 客户端。
     */
    private final RestTemplate restTemplate;

    /**
     * OpenAPI JSON 解析器。
     */
    private final OpenApiCatalogParser parser;

    /**
     * PM 系统 OpenAPI 文档地址。
     */
    private final String openApiUrl;

    /**
     * 是否只保留只读接口。
     */
    private final boolean readOnlyOnly;

    /**
     * 内存缓存，避免每次工具调用都重新拉取 OpenAPI 文档。
     */
    private volatile List<ApiOperationDefinition> cachedOperations = new ArrayList<>();

    /**
     * Spring 注入构造方法。
     */
    public OpenApiCatalogService(RestTemplate restTemplate,
                                 OpenApiCatalogParser parser,
                                 @Value("${pm.system.openapi-url:http://pm.s-ic.cn/pm/v3/api-docs}") String openApiUrl,
                                 @Value("${pm.tools.read-only-only:true}") boolean readOnlyOnly) {
        this.restTemplate = restTemplate;
        this.parser = parser;
        this.openApiUrl = openApiUrl;
        this.readOnlyOnly = readOnlyOnly;
    }

    /**
     * 获取接口目录；缓存为空时同步拉取一次。
     */
    public List<ApiOperationDefinition> getOperations() {
        if (cachedOperations.isEmpty()) {
            refresh();
        }
        return cachedOperations;
    }

    /**
     * 刷新接口目录；拉取失败时保留旧缓存，避免一次文档服务异常导致聊天不可用。
     */
    public synchronized List<ApiOperationDefinition> refresh() {
        try {
            String openApiJson = restTemplate.getForObject(openApiUrl, String.class);
            if (!StringUtils.hasText(openApiJson)) {
                return cachedOperations;
            }
            cachedOperations = parser.parse(openApiJson, readOnlyOnly);
            return cachedOperations;
        } catch (RestClientException e) {
            return cachedOperations;
        }
    }
}
