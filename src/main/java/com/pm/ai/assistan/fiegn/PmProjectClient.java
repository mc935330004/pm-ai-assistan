package com.pm.ai.assistan.fiegn;

import com.pm.ai.assistan.unit.ApiResponse;
import com.pm.ai.assistan.dto.ProjectQueryDTO;


import com.pm.ai.assistan.vo.ProjectVO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Component
@FeignClient(name = "pm-system",url = "${pm.system.base-url}")
public interface  PmProjectClient {

    /**
     * 查询pm项目接口
     */
    @Tool(description = "查询pm项目接口", name = "query_pmProject")
    @GetMapping ("/pmProject/projectList")
    ApiResponse<ProjectVO> getProjectList(@RequestHeader("Authorization") String authorization,
                                          @SpringQueryMap ProjectQueryDTO dto);

    /**
     * 查询合同信息接口
     */
    @Tool(description = "查询合同信息接口", name = "query_contract")
    @GetMapping("/pmProject/infoByCode")
    ApiResponse<ProjectVO> getContractList(@RequestHeader("Authorization") String authorization,
                                           @SpringQueryMap ProjectQueryDTO dto);
}
