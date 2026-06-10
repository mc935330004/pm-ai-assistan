package com.pm.ai.tools.service;

import com.pm.ai.assistan.fiegn.PmProjectClient;
import com.pm.ai.enums.IntentType;
import com.pm.ai.tools.factory.PmToolService;
import com.pm.ai.tools.intent.IntentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class ProjectTool implements PmToolService {

    private final PmProjectClient pmProjectClient;

    public ProjectTool(PmProjectClient pmProjectClient) {
        this.pmProjectClient = pmProjectClient;
    }

    @Override
    public IntentType support() {
        return IntentType.PROJECT_QUERY;
    }

    @Override
    public Object execute(IntentResult intent, String authorization) {
        return null;
    }
}
