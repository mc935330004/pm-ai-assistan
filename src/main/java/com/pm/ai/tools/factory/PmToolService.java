package com.pm.ai.tools.factory;

import com.pm.ai.enums.IntentType;
import com.pm.ai.tools.intent.IntentResult;

public interface PmToolService {
    /**
     * 支持的类型
     * @return
     */
    IntentType support();

    /**
     * 执行
     * @param intent
     * @param authorization
     * @return
     */
    Object execute(IntentResult intent, String authorization);
}
