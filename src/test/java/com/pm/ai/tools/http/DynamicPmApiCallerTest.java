package com.pm.ai.tools.http;

import com.pm.ai.tools.catalog.ApiOperationDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 动态 HTTP 调用测试：保证通用调用器会拼接查询参数并透传用户 Authorization。
 */
class DynamicPmApiCallerTest {

    @Test
    void callsGetOperationWithAuthorizationAndQueryParameters() {
        // 使用 MockRestServiceServer 避免真实请求 PM 系统。
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://pm.test/pmProject/projectList?beginYear=2026"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token"))
                .andRespond(withSuccess("{\"code\":0,\"msg\":\"ok\",\"data\":[]}", MediaType.APPLICATION_JSON));

        // 构造一个只读 GET 接口定义。
        ApiOperationDefinition operation = ApiOperationDefinition.builder()
                .operationId("queryProjects")
                .method("GET")
                .path("/pmProject/projectList")
                .enabled(true)
                .readOnly(true)
                .build();

        // 调用器应该返回 PM 系统原始 JSON，供工具结果继续包装。
        DynamicPmApiCaller caller = new DynamicPmApiCaller(restTemplate, "http://pm.test");
        String response = caller.call(operation, Map.of("beginYear", "2026"), "Bearer token");

        assertThat(response).contains("\"code\":0");
        server.verify();
    }
}
