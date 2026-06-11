package com.pm.ai.tools.router;

import com.pm.ai.tools.catalog.ApiOperationDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 接口路由测试：保证用户问题可以从接口目录中命中最合适的查询接口。
 */
class ApiOperationRouterTest {

    @Test
    void routesProjectAndContractQuestionsToMatchingOperations() {
        // 构造两个不同领域的接口定义，模拟从 OpenAPI 目录解析后的结果。
        ApiOperationDefinition projectOperation = ApiOperationDefinition.builder()
                .operationId("queryProjects")
                .method("GET")
                .path("/pmProject/projectList")
                .domain("project")
                .summary("查询项目列表")
                .enabled(true)
                .readOnly(true)
                .build();
        ApiOperationDefinition contractOperation = ApiOperationDefinition.builder()
                .operationId("queryContracts")
                .method("GET")
                .path("/pmContract/list")
                .domain("contract")
                .summary("查询合同列表")
                .enabled(true)
                .readOnly(true)
                .build();

        // 路由器应该优先根据领域和问题关键词选择对应接口。
        ApiOperationRouter router = new ApiOperationRouter();

        assertThat(router.route("帮我查询项目列表", null, List.of(projectOperation, contractOperation)))
                .get()
                .extracting(ApiOperationDefinition::getOperationId)
                .isEqualTo("queryProjects");
        assertThat(router.route("帮我查一下合同信息", null, List.of(projectOperation, contractOperation)))
                .get()
                .extracting(ApiOperationDefinition::getOperationId)
                .isEqualTo("queryContracts");
    }
}
