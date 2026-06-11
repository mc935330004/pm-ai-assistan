package com.pm.ai.tools.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * pm_api_query 工具出参：向模型返回真实接口调用结果和路由信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PmApiQueryResponse {

    /**
     * 工具调用是否成功。
     */
    private boolean success;

    /**
     * 实际命中的 OpenAPI operationId。
     */
    private String operationId;

    /**
     * 提示消息，用于说明错误、空结果或需要补充信息。
     */
    private String message;

    /**
     * PM 系统返回的原始 JSON 数据。
     */
    private String data;
}
