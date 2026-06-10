package com.pm.ai.assistan.dto;

import lombok.Data;

@Data
public class ProjectQueryDTO {

    /**
     * 项目查询字段
     */
    private String queryStr;

    /**
     * 开始年份
     */
    private String beginYear;
    /**
     * 结束年份
     */
    private String endYear;
}
