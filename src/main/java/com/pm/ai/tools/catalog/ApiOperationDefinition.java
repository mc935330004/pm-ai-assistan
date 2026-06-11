package com.pm.ai.tools.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAPI 接口定义：这是 AI 工具路由的核心目录项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiOperationDefinition {

    /**
     * 接口唯一标识，优先来自 OpenAPI operationId。
     */
    private String operationId;

    /**
     * HTTP 方法，例如 GET、POST。
     */
    private String method;

    /**
     * 接口路径，例如 /pmProject/projectList。
     */
    private String path;

    /**
     * 业务领域，例如 project、contract、collection、audit。
     */
    private String domain;

    /**
     * 接口摘要，来自 OpenAPI summary 或 description。
     */
    private String summary;

    /**
     * 是否只读；第一阶段只允许只读接口被工具调用。
     */
    private boolean readOnly;

    /**
     * 是否启用；后续可通过本地配置或管理后台关闭高风险接口。
     */
    private boolean enabled;

    /**
     * 接口参数定义，主要用于查询参数构造和必填校验。
     */
    @Builder.Default
    private List<ApiParameterDefinition> parameters = new ArrayList<>();
}
