package com.pm.ai.tools.intent;

import com.pm.ai.enums.IntentType;
import org.springframework.stereotype.Service;

@Service
public class IntentService {

    public IntentResult parse(String question) {
        IntentResult result = new IntentResult();
        result.setQuestion(question);

        if (question.contains("项目")) {
            result.setType(IntentType.PROJECT_QUERY);
        } else if (question.contains("合同")) {
            result.setType(IntentType.CONTRACT_QUERY);
        } else if (question.contains("回款") || question.contains("收款")) {
            result.setType(IntentType.COLLECTION_QUERY);
        } else if (question.contains("审批") || question.contains("待办")) {
            result.setType(IntentType.AUDIT_QUERY);
        } else {
            result.setType(IntentType.UNKNOWN);
        }

        return result;
    }
}