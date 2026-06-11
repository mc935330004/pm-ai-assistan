package com.pm.ai.tools.router;

import com.pm.ai.tools.catalog.ApiOperationDefinition;
import com.pm.ai.tools.catalog.ApiParameterDefinition;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查询参数构造器：把用户问题和显式 filters 转换成接口查询参数。
 */
@Component
public class ApiQueryParameterBuilder {

    /**
     * 年份识别规则，匹配 2026 这种四位年份。
     */
    private static final Pattern YEAR_PATTERN = Pattern.compile("(20\\d{2})");

    /**
     * 构造接口查询参数，只输出 OpenAPI 中声明过的参数。
     */
    public Map<String, Object> build(String question, Map<String, Object> filters,
                                     ApiOperationDefinition operation) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (operation == null || operation.getParameters() == null) {
            return params;
        }

        // 先写入模型或调用方显式传入的过滤条件。
        if (filters != null) {
            for (ApiParameterDefinition parameter : operation.getParameters()) {
                if (filters.containsKey(parameter.getName())) {
                    params.put(parameter.getName(), filters.get(parameter.getName()));
                }
            }
        }

        // 再从自然语言问题中提取年份，填充常见 beginYear/endYear 参数。
        String year = extractYear(question);
        if (year != null) {
            putIfDeclared(params, operation, "beginYear", year);
            putIfDeclared(params, operation, "endYear", year);
        }

        return params;
    }

    /**
     * 从用户问题中提取第一个四位年份。
     */
    private String extractYear(String question) {
        if (question == null) {
            return null;
        }
        Matcher matcher = YEAR_PATTERN.matcher(question);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 只有接口声明了该参数时才写入，避免向后端传递未知参数。
     */
    private void putIfDeclared(Map<String, Object> params, ApiOperationDefinition operation,
                               String name, Object value) {
        boolean declared = operation.getParameters().stream()
                .anyMatch(parameter -> name.equals(parameter.getName()));
        if (declared) {
            params.putIfAbsent(name, value);
        }
    }
}
