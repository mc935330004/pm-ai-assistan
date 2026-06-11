package com.pm.ai.tools.router;

import com.pm.ai.tools.catalog.ApiOperationDefinition;
import com.pm.ai.tools.catalog.ApiParameterDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 查询参数构造测试：保证问题中的年份和显式过滤条件可以进入接口参数。
 */
class ApiQueryParameterBuilderTest {

    @Test
    void extractsYearAndKeepsExplicitFiltersForKnownQueryParameters() {
        // 接口声明了 beginYear、endYear、queryStr 三个查询参数。
        ApiOperationDefinition operation = ApiOperationDefinition.builder()
                .operationId("queryProjects")
                .method("GET")
                .path("/pmProject/projectList")
                .parameters(List.of(
                        new ApiParameterDefinition("beginYear", "query", false, "string"),
                        new ApiParameterDefinition("endYear", "query", false, "string"),
                        new ApiParameterDefinition("queryStr", "query", false, "string")
                ))
                .build();

        // 用户问题中包含年份，filters 中包含项目名称关键字。
        ApiQueryParameterBuilder builder = new ApiQueryParameterBuilder();
        Map<String, Object> params = builder.build(
                "查询2026年的项目",
                Map.of("queryStr", "智慧园区"),
                operation
        );

        // 年份自动填充为起止年份，显式过滤条件原样保留。
        assertThat(params).containsEntry("beginYear", "2026");
        assertThat(params).containsEntry("endYear", "2026");
        assertThat(params).containsEntry("queryStr", "智慧园区");
    }
}
