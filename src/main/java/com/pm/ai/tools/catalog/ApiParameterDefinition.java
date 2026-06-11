package com.pm.ai.tools.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAPI 参数定义：描述一个接口参数的位置、名称、类型和必填规则。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiParameterDefinition {

    /**
     * 参数名称，例如 beginYear、projectCode。
     */
    private String name;

    /**
     * 参数位置，例如 query、path、header。
     */
    private String in;

    /**
     * 是否必填，用于工具调用前判断是否需要向用户追问。
     */
    private boolean required;

    /**
     * 参数类型，例如 string、integer。
     */
    private String type;
}
