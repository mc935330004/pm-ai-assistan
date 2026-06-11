package com.pm.ai.tools.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OpenAPI 目录解析器：把 Swagger/OpenAPI JSON 转换成工具路由可用的接口目录。
 */
@Component
public class OpenApiCatalogParser {

    /**
     * JSON 解析器，复用 Jackson 处理 OpenAPI 文档。
     */
    private final ObjectMapper objectMapper;

    /**
     * 默认构造方法，方便单元测试直接 new。
     */
    public OpenApiCatalogParser() {
        this(new ObjectMapper());
    }

    /**
     * 注入构造方法，方便 Spring 复用统一 ObjectMapper。
     */
    public OpenApiCatalogParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析 OpenAPI JSON；readOnlyOnly 为 true 时只保留 GET 查询接口。
     */
    public List<ApiOperationDefinition> parse(String openApiJson, boolean readOnlyOnly) {
        try {
            JsonNode root = objectMapper.readTree(openApiJson);
            JsonNode paths = root.path("paths");
            List<ApiOperationDefinition> operations = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> pathIterator = paths.fields();

            // 遍历 paths 下的每一个路径和 HTTP 方法。
            while (pathIterator.hasNext()) {
                Map.Entry<String, JsonNode> pathEntry = pathIterator.next();
                parsePath(pathEntry.getKey(), pathEntry.getValue(), readOnlyOnly, operations);
            }

            return operations;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid OpenAPI JSON", e);
        }
    }

    /**
     * 解析单个 path 节点下的多个 HTTP method。
     */
    private void parsePath(String path, JsonNode pathNode, boolean readOnlyOnly,
                           List<ApiOperationDefinition> operations) {
        Iterator<Map.Entry<String, JsonNode>> methodIterator = pathNode.fields();
        while (methodIterator.hasNext()) {
            Map.Entry<String, JsonNode> methodEntry = methodIterator.next();
            String method = methodEntry.getKey().toUpperCase(Locale.ROOT);

            // OpenAPI 中可能出现 parameters 等非 HTTP 方法节点，需要跳过。
            if (!isHttpMethod(method)) {
                continue;
            }

            boolean readOnly = "GET".equals(method);
            if (readOnlyOnly && !readOnly) {
                continue;
            }

            JsonNode operationNode = methodEntry.getValue();
            operations.add(ApiOperationDefinition.builder()
                    .operationId(resolveOperationId(method, path, operationNode))
                    .method(method)
                    .path(path)
                    .domain(resolveDomain(operationNode))
                    .summary(resolveSummary(operationNode))
                    .readOnly(readOnly)
                    .enabled(true)
                    .parameters(parseParameters(operationNode))
                    .build());
        }
    }

    /**
     * 判断节点名是否为 OpenAPI 支持的 HTTP 方法。
     */
    private boolean isHttpMethod(String method) {
        return List.of("GET", "POST", "PUT", "PATCH", "DELETE").contains(method);
    }

    /**
     * 解析接口唯一标识；缺少 operationId 时使用 method + path 生成兜底值。
     */
    private String resolveOperationId(String method, String path, JsonNode operationNode) {
        String operationId = textValue(operationNode.path("operationId"));
        if (StringUtils.hasText(operationId)) {
            return operationId;
        }
        return method.toLowerCase(Locale.ROOT) + path.replace("/", "_").replace("{", "").replace("}", "");
    }

    /**
     * 解析业务领域；优先取第一个 tag。
     */
    private String resolveDomain(JsonNode operationNode) {
        JsonNode tags = operationNode.path("tags");
        if (tags.isArray() && tags.size() > 0) {
            return textValue(tags.get(0));
        }
        return "";
    }

    /**
     * 解析接口摘要；summary 为空时降级到 description。
     */
    private String resolveSummary(JsonNode operationNode) {
        String summary = textValue(operationNode.path("summary"));
        if (StringUtils.hasText(summary)) {
            return summary;
        }
        return textValue(operationNode.path("description"));
    }

    /**
     * 解析接口参数列表，目前主要支持 query/path/header 参数。
     */
    private List<ApiParameterDefinition> parseParameters(JsonNode operationNode) {
        List<ApiParameterDefinition> parameters = new ArrayList<>();
        JsonNode parameterNodes = operationNode.path("parameters");
        if (!parameterNodes.isArray()) {
            return parameters;
        }

        for (JsonNode parameterNode : parameterNodes) {
            parameters.add(ApiParameterDefinition.builder()
                    .name(textValue(parameterNode.path("name")))
                    .in(defaultValue(textValue(parameterNode.path("in")), "query"))
                    .required(parameterNode.path("required").asBoolean(false))
                    .type(defaultValue(textValue(parameterNode.path("schema").path("type")), "string"))
                    .build());
        }
        return parameters;
    }

    /**
     * 安全读取 JSON 文本值，避免使用当前 Jackson 版本中已过时的 asText(defaultValue)。
     */
    private String textValue(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? "" : node.asText();
    }

    /**
     * 文本为空时返回默认值，用于 OpenAPI 中可缺省的字段。
     */
    private String defaultValue(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
