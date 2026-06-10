package com.pm.ai.enums;

public enum IntentType {
    PROJECT_QUERY("项目查询"),
    CONTRACT_QUERY("合同查询"),
    COLLECTION_QUERY("回款查询"),
    AUDIT_QUERY("审批查询"),
    UNKNOWN("未知");

    private final String desc;

    IntentType(String desc) {
        this.desc = desc;
    }
}
