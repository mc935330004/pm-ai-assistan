package com.pm.ai.tools.intent;

import com.pm.ai.enums.IntentType;
import lombok.Data;

/**
 * @date: 2023/10/09 11:19
 * @description:
 */
@Data
public class IntentResult {

    /**
     * 意图类型
     */
    private IntentType type;

    /**
     * 用户问题
     */
    private String question;
}
