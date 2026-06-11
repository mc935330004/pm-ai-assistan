package com.pm.ai.tools.tool;

import com.pm.ai.tools.catalog.ApiOperationDefinition;
import com.pm.ai.tools.catalog.OpenApiCatalogService;
import com.pm.ai.tools.http.DynamicPmApiCaller;
import com.pm.ai.tools.router.ApiOperationRouter;
import com.pm.ai.tools.router.ApiQueryParameterBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * PM 查询工具：对模型只暴露一个 pm_api_query，再由后端目录路由到具体接口。
 */
@Component
@RequiredArgsConstructor
public class PmApiQueryTool {

    /**
     * OpenAPI 接口目录服务。
     */
    private final OpenApiCatalogService catalogService;

    /**
     * 接口路由器。
     */
    private final ApiOperationRouter router;

    /**
     * 查询参数构造器。
     */
    private final ApiQueryParameterBuilder parameterBuilder;

    /**
     * PM 系统动态调用器。
     */
    private final DynamicPmApiCaller apiCaller;

    /**
     * 为单次用户请求创建工具回调，把 Authorization 绑定到闭包里透传给 PM 系统。
     */
    public ToolCallback toolCallback(String authorization) {
        return FunctionToolCallback
                .builder("pm_api_query", (PmApiQueryRequest request) -> execute(request, authorization))
                .description("Query read-only PM system APIs through the OpenAPI catalog router.")
                .inputType(PmApiQueryRequest.class)
                .build();
    }

    /**
     * 执行工具调用：目录召回、接口路由、参数构造、HTTP 调用都在这里串起来。
     */
    public PmApiQueryResponse execute(PmApiQueryRequest request, String authorization) {
        List<ApiOperationDefinition> operations = catalogService.getOperations();
        if (operations.isEmpty()) {
            return PmApiQueryResponse.builder()
                    .success(false)
                    .message("PM API catalog is empty or unavailable.")
                    .build();
        }

        ApiOperationDefinition operation = router.route(request.getQuestion(), request.getDomain(), operations)
                .orElse(null);
        if (operation == null) {
            return PmApiQueryResponse.builder()
                    .success(false)
                    .message("No suitable read-only PM API was found.")
                    .build();
        }

        Map<String, Object> queryParams = parameterBuilder.build(request.getQuestion(), request.getFilters(), operation);
        String data = apiCaller.call(operation, queryParams, authorization);
        return PmApiQueryResponse.builder()
                .success(true)
                .operationId(operation.getOperationId())
                .message("PM API query completed.")
                .data(data)
                .build();
    }
}
