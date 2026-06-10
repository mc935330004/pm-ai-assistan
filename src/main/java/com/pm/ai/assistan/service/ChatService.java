package com.pm.ai.assistan.service;

import com.pm.ai.assistan.agent.AiModelService;
import com.pm.ai.assistan.dto.ChatRequest;
import com.pm.ai.assistan.dto.ProjectQueryDTO;
import com.pm.ai.assistan.fiegn.PmProjectClient;
import com.pm.ai.assistan.unit.AgentResult;
import com.pm.ai.assistan.unit.ApiResponse;
import com.pm.ai.assistan.vo.ProjectVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final AiModelService aiModelService;
    private final PmProjectClient pmProjectClient;

    public AgentResult chat(ChatRequest request, String authorization) {
        try {
            if (!StringUtils.hasText(authorization)) {
                throw new RuntimeException("缺少 Authorization 请求头");
            }

            String question = request.getQuestion();
            if (!StringUtils.hasText(question)) {
                throw new RuntimeException("问题不能为空");
            }

            if (question.contains("项目")) {
                ProjectQueryDTO queryDTO = new ProjectQueryDTO();
                queryDTO.setBeginYear("2026");
                queryDTO.setEndYear("2026");

                ApiResponse<ProjectVO> response = pmProjectClient.getProjectList(authorization, queryDTO);
                List<ProjectVO> projectList = response == null ? null : response.getData();
                int projectCount = projectList == null ? 0 : projectList.size();
                log.info("pm-system projectList response, code={}, msg={}, dataSize={}",
                        response == null ? null : response.getCode(),
                        response == null ? null : response.getMsg(),
                        projectCount);

                if (projectCount == 0) {
                    return AgentResult.success("未查询到项目信息", projectList);
                }

                String answer = aiModelService.summary(question, response);
                return AgentResult.success(answer, projectList);
            }
        } catch (Exception e) {
            log.error("聊天失败", e);
            return AgentResult.fail("聊天失败", null);
        }

        return AgentResult.success("当前第一版只支持项目查询，例如：查询我的项目", null);
    }
}
