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
        return operations.stream()
                .filter(ApiOperationDefinition::isEnabled)
                .filter(ApiOperationDefinition::isReadOnly)
                .max(Comparator.comparingInt(operation -> score(question, domain, operation)));
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
        score += scoreDomainKeyword(normalizedQuestion, operation.getDomain());

        // 接口摘要、operationId、path 命中也可加分。
        score += containsScore(normalizedQuestion, operation.getSummary(), 20);
        score += containsScore(normalizedQuestion, operation.getOperationId(), 10);
        score += containsScore(normalizedQuestion, operation.getPath(), 5);

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
    private int scoreDomainKeyword(String question, String domain) {
        String normalizedDomain = normalize(domain);
        if ("project".equals(normalizedDomain) && question.contains("项目")) {
            return 80;
        }
        if ("contract".equals(normalizedDomain) && question.contains("合同")) {
            return 80;
        }
        if ("collection".equals(normalizedDomain) && (question.contains("回款") || question.contains("收款"))) {
            return 80;
        }
        if ("audit".equals(normalizedDomain) && (question.contains("审批") || question.contains("待办"))) {
            return 80;
        }
        return 0;
    }

    /**
     * 判断问题是否包含接口描述文本，命中则返回指定分数。
     */
    private int containsScore(String question, String candidate, int score) {
        String normalizedCandidate = normalize(candidate);
        if (!StringUtils.hasText(normalizedCandidate)) {
            return 0;
        }
        return question.contains(normalizedCandidate) ? score : 0;
    }
}
