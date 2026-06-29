package com.mcp.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class McpRpcControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsToolsWithJsonRpcSuccessEnvelope() throws Exception {
        rpc("""
                {"jsonrpc":"2.0","id":"req_tools","method":"tools/list","params":{}}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("req_tools"))
                .andExpect(jsonPath("$.result.tools[0].name").exists())
                .andExpect(jsonPath("$.result.tools[0].schema").exists())
                .andExpect(jsonPath("$.result.tools[0].riskLevel").exists());
    }

    @Test
    void listsPromptsWithJsonRpcSuccessEnvelope() throws Exception {
        rpc("""
                {"jsonrpc":"2.0","id":"req_prompts","method":"prompts/list","params":{}}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.prompts[0].name").exists())
                .andExpect(jsonPath("$.result.prompts[0].variables").isArray());
    }

    @Test
    void listsResourcesWithJsonRpcSuccessEnvelope() throws Exception {
        rpc("""
                {"jsonrpc":"2.0","id":"req_resources","method":"resources/list","params":{}}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.resources[0].name").exists())
                .andExpect(jsonPath("$.result.resources[0].linkedTools").isArray());
    }

    @Test
    void callsLowRiskToolThroughExistingInvokeFlow() throws Exception {
        rpc("""
                {
                  "jsonrpc":"2.0",
                  "id":"req_low",
                  "method":"tools/call",
                  "params":{"toolName":"weather.lookup","arguments":{"city":"上海"},"role":"DEVELOPER"}
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("SUCCESS"))
                .andExpect(jsonPath("$.result.reviewRequired").value(false))
                .andExpect(jsonPath("$.result.callId").exists())
                .andExpect(jsonPath("$.result.traceId").exists())
                .andExpect(jsonPath("$.result.result.demo").value(true));
    }

    @Test
    void sendsHighRiskToolToHumanReview() throws Exception {
        rpc("""
                {
                  "jsonrpc":"2.0",
                  "id":"req_high",
                  "method":"tools/call",
                  "params":{"toolName":"db.query.readonly","arguments":{"sql":"select * from demo_customers limit 5"},"role":"ADMIN"}
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.result.reviewRequired").value(true))
                .andExpect(jsonPath("$.result.pendingReview.reviewId").exists());
    }

    @Test
    void blocksDangerousSqlWithoutExecution() throws Exception {
        rpc("""
                {
                  "jsonrpc":"2.0",
                  "id":"req_blocked",
                  "method":"tools/call",
                  "params":{"toolName":"db.query.readonly","arguments":{"sql":"delete from demo_customers"},"role":"ADMIN"}
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("BLOCKED"))
                .andExpect(jsonPath("$.result.reviewRequired").value(false))
                .andExpect(jsonPath("$.result.result.blocked").value(true));
    }

    @Test
    void returnsMethodNotFoundForUnsupportedMethod() throws Exception {
        rpc("""
                {"jsonrpc":"2.0","id":"req_method","method":"capabilities/list","params":{}}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32601))
                .andExpect(jsonPath("$.error.message").value("Method not found"));
    }

    @Test
    void returnsInvalidRequestForNonJsonRpcTwo() throws Exception {
        rpc("""
                {"jsonrpc":"1.0","id":"req_version","method":"tools/list","params":{}}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32600))
                .andExpect(jsonPath("$.error.message").value("Invalid Request"));
    }

    @Test
    void returnsInvalidParamsWhenToolArgumentsAreMissing() throws Exception {
        rpc("""
                {"jsonrpc":"2.0","id":"req_params","method":"tools/call","params":{"toolName":"weather.lookup"}}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.message").value("Invalid params"));
    }

    @Test
    void mapsViewerPolicyDenialToForbiddenError() throws Exception {
        rpc("""
                {
                  "jsonrpc":"2.0",
                  "id":"req_forbidden",
                  "method":"tools/call",
                  "params":{"toolName":"weather.lookup","arguments":{"city":"上海"},"role":"VIEWER"}
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32003))
                .andExpect(jsonPath("$.error.message").value("Forbidden"))
                .andExpect(jsonPath("$.error.data.action").value("TOOL_INVOKE"))
                .andExpect(jsonPath("$.error.data.role").value("VIEWER"));
    }

    private org.springframework.test.web.servlet.ResultActions rpc(String body) throws Exception {
        return mockMvc.perform(post("/api/mcp/rpc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
