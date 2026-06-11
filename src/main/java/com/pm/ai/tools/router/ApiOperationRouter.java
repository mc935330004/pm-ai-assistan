package com.pm.ai.tools.router;

import com.pm.ai.tools.catalog.ApiOperationDefinition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 接口路由器：根据用户问题和可选领域，从接口目录中选择最合适的接口。
 */
@Component
public class ApiOperationRouter {

    /**
     * 路由入口；后续接入 RAG 时可以把候选接口列表替换成向量召回结果。
     */
    public Optional<ApiOperationDefinition> route(String question, String domain,
                                                  List<ApiOperationDefinition> operations) {
        if (operations == null || operations.isEmpty()) {
            return Optional.empty();
        }

        // 只从启用且只读的接口中选择，避免模型误触写操作。
        Optional<ApiOperationDefinition> bestMatch = operations.stream()
                .filter(ApiOperationDefinition::isEnabled)
                .filter(ApiOperationDefinition::isReadOnly)
                .max(Comparator.comparingInt(operation -> score(question, domain, operation)));
        return bestMatch.filter(operation -> score(question, domain, operation) > 0);
    }

    /**
     * 简单关键词评分；当前足够支撑 v1，后续可以替换为向量相似度评分。
     */
    private int score(String question, String domain, ApiOperationDefinition operation) {
        String normalizedQuestion = normalize(question);
        int score = 0;

        // 显式 domain 权重最高。
        if (StringUtils.hasText(domain) && domain.equalsIgnoreCase(operation.getDomain())) {
            score += 100;
        }

        // 中文业务关键词映射到领域，提升常见问法命中率。
        score += scoreDomainKeyword(normalizedQuestion, operation);

        // 接口摘要、operationId、path 命中也可加分。
        score += containsScore(normalizedQuestion, operation.getSummary(), 20);
        score += containsScore(normalizedQuestion, operation.getOperationId(), 10);
        score += containsScore(normalizedQuestion, operation.getPath(), 5);
        score += preferredListScore(normalizedQuestion, operation);
        score -= pathVariablePenalty(operation);
        score -= mutatingGetPenalty(operation);

        return score;
    }

    /**
     * 将空值安全地转成小写字符串，便于做简单匹配。
     */
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    /**
     * 根据中文关键词给常见 PM 领域加分。
     */
    private int scoreDomainKeyword(String question, ApiOperationDefinition operation) {
        String operationText = operationText(operation);
        if (question.contains("项目") && containsAny(operationText, "项目", "project", "pmproject")) {
            return 80;
        }
        if (question.contains("合同") && containsAny(operationText, "合同", "contract")) {
            return 80;
        }
        if ((question.contains("回款") || question.contains("收款"))
                && containsAny(operationText, "回款", "收款", "collection")) {
            return 80;
        }
        if ((question.contains("审批") || question.contains("待办"))
                && containsAny(operationText, "审批", "待办", "audit", "task")) {
            return 80;
        }
        return 0;
    }

    /**
     * 对常见列表查询入口额外加权，避免“查询”这类泛词命中详情接口。
     */
    private int preferredListScore(String question, ApiOperationDefinition operation) {
        String path = normalize(operation.getPath());
        String operationId = normalize(operation.getOperationId());
        int score = 0;
        if (question.contains("项目") && "/pmproject/projectlist".equals(path)) {
            score += 120;
        }
        if (question.contains("项目") && (operationId.contains("projectlist") || path.endsWith("/projectlist"))) {
            score += 50;
        }
        if ((question.contains("我的") || question.contains("列表"))
                && containsAny(operationText(operation), "列表", "list", "page")) {
            score += 20;
        }
        return score;
    }

    /**
     * 没有明确参数时，详情类 path 参数接口不应抢占列表查询。
     */
    private int pathVariablePenalty(ApiOperationDefinition operation) {
        String path = normalize(operation.getPath());
        return path.contains("{") || path.contains("}") ? 40 : 0;
    }

    /**
     * OpenAPI 中存在一些用 GET 暴露的写操作，动态只读路由需要降权。
     */
    private int mutatingGetPenalty(ApiOperationDefinition operation) {
        String text = operationText(operation);
        return containsAny(text,
                "delete", "del", "submit", "revoke", "update", "save", "add", "complete", "push",
                "删除", "提交", "撤回", "更新", "修改", "新增", "审批", "推送", "终止") ? 80 : 0;
    }

    /**
     * 判断问题是否包含接口描述文本，命中则返回指定分数。
     */
    private int containsScore(String question, String candidate, int score) {
        String normalizedCandidate = normalize(candidate);
        if (!StringUtils.hasText(normalizedCandidate)) {
            return 0;
        }
        if (isGenericSummary(normalizedCandidate)) {
            return 0;
        }
        return question.contains(normalizedCandidate) ? score : 0;
    }

    /**
     * “查询/详情/列表”等摘要太泛，不能单独决定路由。
     */
    private boolean isGenericSummary(String value) {
        return List.of("查询", "详情", "列表", "分页查询", "导出").contains(value);
    }

    private String operationText(ApiOperationDefinition operation) {
        return normalize(operation.getDomain()) + " "
                + normalize(operation.getSummary()) + " "
                + normalize(operation.getOperationId()) + " "
                + normalize(operation.getPath());
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }
}
