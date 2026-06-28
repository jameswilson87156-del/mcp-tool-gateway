package com.mcp.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsDemoTools() throws Exception {
        mockMvc.perform(get("/api/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[?(@.id == 'crm.customer.search')]").exists())
                .andExpect(jsonPath("$[0].category").exists())
                .andExpect(jsonPath("$[0].approvalRequired").exists())
                .andExpect(jsonPath("$[0].schema.type").value("object"))
                .andExpect(jsonPath("$[0].recentCallCount").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists());
    }

    @Test
    void invokesMediumRiskToolDirectly() throws Exception {
        mockMvc.perform(post("/api/tools/crm.customer.search/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment": "production",
                                  "requester": "admin",
                                  "parameters": {
                                    "customer_id": "CUST-202405-000123",
                                    "region": "APAC",
                                    "limit": 20
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.response.demo").value(true));
    }

    @Test
    void sendsHighRiskReadonlyDbToolToReview() throws Exception {
        mockMvc.perform(post("/api/tools/db.query.readonly/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment": "production",
                                  "requester": "admin",
                                  "parameters": {
                                    "sql": "select * from demo_customers limit 20"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.reviewId").exists());
    }

    @Test
    void listsSeededHumanReviews() throws Exception {
        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.status == 'PENDING_REVIEW')]").exists())
                .andExpect(jsonPath("$[?(@.toolId == 'db.query.readonly')]").exists());
    }

    @Test
    void requestChangesUpdatesReviewState() throws Exception {
        var response = mockMvc.perform(post("/api/tools/db.query.readonly/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment": "production",
                                  "requester": "admin",
                                  "parameters": {
                                    "sql": "select * from demo_customers limit 20"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var reviewId = response.replaceAll("(?s).*\\\"reviewId\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/api/reviews/{id}/request-changes", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewer": "reviewer.li",
                                  "comment": "请缩小 SQL limit 并补充业务原因"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHANGES_REQUESTED"))
                .andExpect(jsonPath("$.decision").value("REQUEST_CHANGES"));
    }

    @Test
    void listsAuditLogs() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").exists());
    }

    @Test
    void listsTraceSummariesWithFilters() throws Exception {
        mockMvc.perform(get("/api/traces")
                        .param("riskLevel", "HIGH")
                        .param("reviewRequired", "true")
                        .param("keyword", "db.query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].traceId").exists())
                .andExpect(jsonPath("$[0].callId").exists())
                .andExpect(jsonPath("$[0].toolName").value("db.query.readonly"))
                .andExpect(jsonPath("$[0].reviewRequired").value(true))
                .andExpect(jsonPath("$[0].totalLatencyMs").exists());
    }

    @Test
    void returnsTraceDetailByTraceId() throws Exception {
        var response = mockMvc.perform(get("/api/traces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].traceId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var traceId = response.replaceAll("(?s).*?\\\"traceId\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(get("/api/traces/{traceId}", traceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId").value(traceId))
                .andExpect(jsonPath("$.toolCall").exists())
                .andExpect(jsonPath("$.toolSchemaSummary.type").value("object"))
                .andExpect(jsonPath("$.traceEvents[?(@.step == 'Schema Check')]").exists())
                .andExpect(jsonPath("$.auditLogs").exists());
    }

    @Test
    void blocksDangerousSqlEvenForReadonlyTool() throws Exception {
        mockMvc.perform(post("/api/tools/db.query.readonly/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment": "production",
                                  "requester": "admin",
                                  "parameters": {
                                    "sql": "delete from customers"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.response.blocked").value(true));
    }
}
