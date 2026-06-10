package com.pm.ai.assistan.vo;

import lombok.Data;

@Data
public class ProjectVO {

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 项目编码
     */
    private String projectCode;

    /**
     * 项目类型
     */
    private String projectType;

    /**
     * 立项金额
     */
    private String estimatedUndertateMoney;

    /**
     * 立项时间
     */
    private String approvalTime;

    /**
     * 签订公司
     */
    private String companyName;

    /**
     * 签订部门
     */
    private String deptName;

    /**
     * 项目经理
     */
    private String projectManagerName;

    /**
     * 项目状态
     */
    private Integer processProgress;

    /**
     * 审批状态
     */
    private Integer auditStatus;
}
