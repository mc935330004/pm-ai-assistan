package com.pm.ai.tools.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * pm_api_query 工具入参：模型只需要提交问题、可选领域和过滤条件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PmApiQueryRequest {

    /**
     * 用户原始问题，用于接口路由和参数抽取。
     */
    private String question;

    /**
     * 可选业务领域，例如 project、contract、collection、audit。
     */
    private String domain;

    /**
     * 可选过滤条件，例如 beginYear、endYear、queryStr。
     */
    @Builder.Default
    private Map<String, Object> filters = new HashMap<>();
}
