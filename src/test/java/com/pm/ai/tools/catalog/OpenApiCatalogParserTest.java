package com.pm.ai.tools.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenAPI 目录解析测试：保证接口目录只暴露安全的只读查询接口。
 */
class OpenApiCatalogParserTest {

    @Test
    void parsesOnlyEnabledReadOperationsWhenReadOnlyModeIsEnabled() {
        // 构造一个最小 OpenAPI 文档，包含一个 GET 查询接口和一个 POST 写接口。
        String openApiJson = "{"
                + "\"paths\":{"
                + "\"/pmProject/projectList\":{"
                + "\"get\":{"
                + "\"operationId\":\"queryProjects\","
                + "\"summary\":\"查询项目列表\","
                + "\"tags\":[\"project\"],"
                + "\"parameters\":["
                + "{\"name\":\"beginYear\",\"in\":\"query\",\"required\":false,\"schema\":{\"type\":\"string\"}},"
                + "{\"name\":\"endYear\",\"in\":\"query\",\"required\":false,\"schema\":{\"type\":\"string\"}}"
                + "]"
                + "}"
                + "},"
                + "\"/pmProject/delete\":{"
                + "\"post\":{"
                + "\"operationId\":\"deleteProject\","
                + "\"summary\":\"删除项目\","
                + "\"tags\":[\"project\"]"
                + "}"
                + "}"
                + "}"
                + "}";

        // 解析目录时开启只读模式，POST 接口必须被过滤。
        OpenApiCatalogParser parser = new OpenApiCatalogParser();
        List<ApiOperationDefinition> operations = parser.parse(openApiJson, true);

        // 断言只剩下 GET 查询接口，并且参数元数据被完整保留。
        assertThat(operations).hasSize(1);
        assertThat(operations.get(0).getOperationId()).isEqualTo("queryProjects");
        assertThat(operations.get(0).getMethod()).isEqualTo("GET");
        assertThat(operations.get(0).getPath()).isEqualTo("/pmProject/projectList");
        assertThat(operations.get(0).getDomain()).isEqualTo("project");
        assertThat(operations.get(0).getParameters()).extracting(ApiParameterDefinition::getName)
                .containsExactly("beginYear", "endYear");
    }
}
