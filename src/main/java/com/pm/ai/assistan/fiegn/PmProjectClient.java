package com.pm.ai.assistan.fiegn;

import com.pm.ai.assistan.dto.ProjectQueryDTO;
import com.pm.ai.assistan.unit.ApiResponse;
import com.pm.ai.assistan.vo.ProjectVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * PM 项目系统 Feign 客户端：仅作为后端实现细节，不直接暴露给大模型作为 Tool。
 */
@Component
@FeignClient(name = "pm-system", url = "${pm.system.base-url}")
public interface PmProjectClient {

    /**
     * 查询 PM 项目列表。
     */
    @GetMapping("/pmProject/projectList")
    ApiResponse<ProjectVO> getProjectList(@RequestHeader("Authorization") String authorization,
                                          @SpringQueryMap ProjectQueryDTO dto);

    /**
     * 查询合同信息。
     */
    @GetMapping("/pmProject/infoByCode")
    ApiResponse<ProjectVO> getContractList(@RequestHeader("Authorization") String authorization,
                                           @SpringQueryMap ProjectQueryDTO dto);
}
