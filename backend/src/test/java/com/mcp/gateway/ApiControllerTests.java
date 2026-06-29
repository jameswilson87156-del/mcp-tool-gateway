package com.mcp.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import com.mcp.gateway.service.GatewayService;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private GatewayService gateway;

    @Test
    void seedsPersistentDemoDataOnStartup() {
        org.assertj.core.api.Assertions.assertThat(countRows("tools")).isGreaterThanOrEqualTo(6);
        org.assertj.core.api.Assertions.assertThat(countRows("prompts")).isGreaterThanOrEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(countRows("resources")).isGreaterThanOrEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(countRows("demo_users")).isGreaterThanOrEqualTo(4);
        org.assertj.core.api.Assertions.assertThat(countRows("role_policies")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void seedIfEmptyDoesNotDuplicateMainDemoData() {
        var toolCount = countRows("tools");
        var promptCount = countRows("prompts");
        var resourceCount = countRows("resources");

        gateway.seedIfEmpty();

        org.assertj.core.api.Assertions.assertThat(countRows("tools")).isEqualTo(toolCount);
        org.assertj.core.api.Assertions.assertThat(countRows("prompts")).isEqualTo(promptCount);
        org.assertj.core.api.Assertions.assertThat(countRows("resources")).isEqualTo(resourceCount);
    }

    @Test
    void lowRiskInvokePersistsCallTraceAndAuditRows() throws Exception {
        var callCount = countRows("tool_calls");
        var traceCount = countRows("trace_events");
        var auditCount = countRows("audit_logs");

        mockMvc.perform(post("/api/tools/weather.lookup/invoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment": "production",
                                  "requester": "developer",
                                  "parameters": {
                                    "city": "上海"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        org.assertj.core.api.Assertions.assertThat(countRows("tool_calls")).isGreaterThan(callCount);
        org.assertj.core.api.Assertions.assertThat(countRows("trace_events")).isGreaterThan(traceCount);
        org.assertj.core.api.Assertions.assertThat(countRows("audit_logs")).isGreaterThan(auditCount);
    }

    @Test
    void listsDemoTools() throws Exception {
        mockMvc.perform(get("/api/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'weather.lookup')]").exists())
                .andExpect(jsonPath("$[?(@.id == 'ticket.search')]").exists())
                .andExpect(jsonPath("$[?(@.id == 'crm.customer.search')]").exists())
                .andExpect(jsonPath("$[?(@.id == 'db.query.readonly')]").exists())
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
                .andExpect(jsonPath("$.items[?(@.status == 'PENDING_REVIEW')]").exists())
                .andExpect(jsonPath("$.items[?(@.toolId == 'db.query.readonly')]").exists());
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
                .andExpect(jsonPath("$.items[0].action").exists());
    }

    @Test
    void returnsPromptListDetailAndRenderResult() throws Exception {
        mockMvc.perform(get("/api/prompts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == 'prompt_customer_summary')]").exists())
                .andExpect(jsonPath("$.items[0].description").exists())
                .andExpect(jsonPath("$.items[0].usageScope").exists())
                .andExpect(jsonPath("$.items[0].relatedTools").exists())
                .andExpect(jsonPath("$.items[0].usageCount").exists());

        mockMvc.perform(get("/api/prompts/prompt_customer_summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt.id").value("prompt_customer_summary"))
                .andExpect(jsonPath("$.templateContent").exists())
                .andExpect(jsonPath("$.recentUsage").exists())
                .andExpect(jsonPath("$.auditLogs").exists());

        mockMvc.perform(post("/api/prompts/prompt_customer_summary/render")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requester": "admin",
                                  "variables": {
                                    "customer_id": "CUST-202405-000123",
                                    "policy_doc": "policy-docs",
                                    "locale": "zh-CN"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.renderedPrompt").value(org.hamcrest.Matchers.containsString("CUST-202405-000123")));
    }

    @Test
    void promptRenderReturnsValidationErrorsForMissingVariables() throws Exception {
        mockMvc.perform(post("/api/prompts/prompt_customer_summary/render")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requester": "admin",
                                  "variables": {
                                    "customer_id": "CUST-202405-000123"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.validationErrors").isArray())
                .andExpect(jsonPath("$.validationErrors[0]").exists());
    }

    @Test
    void createsUpdatesPublishesAndArchivesPromptWithAuditLogs() throws Exception {
        var auditCount = countRows("audit_logs");
        var created = mockMvc.perform(post("/api/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "ops.incident.brief",
                                  "description": "生成事件响应摘要",
                                  "category": "Operations",
                                  "templateContent": "请基于 {{incident_id}} 输出 {{locale}} 事件摘要。",
                                  "variables": ["incident_id", "locale"],
                                  "usageScope": "运维 Agent demo",
                                  "relatedTools": ["ticket.search"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt.status").value("DRAFT"))
                .andExpect(jsonPath("$.prompt.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var promptId = extractJsonString(created, "id");

        mockMvc.perform(put("/api/prompts/{id}", promptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "ops.incident.brief.v2",
                                  "description": "更新事件响应摘要",
                                  "category": "Operations",
                                  "templateContent": "请基于 {{incident_id}} 输出 {{locale}} 事件摘要和下一步 Tool。",
                                  "variables": ["incident_id", "locale"],
                                  "usageScope": "运维 Agent demo",
                                  "relatedTools": ["ticket.search"],
                                  "status": "DRAFT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt.name").value("ops.incident.brief.v2"));

        mockMvc.perform(post("/api/prompts/{id}/publish", promptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt.status").value("ACTIVE"));

        mockMvc.perform(post("/api/prompts/{id}/archive", promptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt.status").value("ARCHIVED"));

        org.assertj.core.api.Assertions.assertThat(countRows("audit_logs")).isGreaterThanOrEqualTo(auditCount + 4);
    }

    @Test
    void publishPromptReturnsValidationErrorForInvalidPrompt() throws Exception {
        var created = mockMvc.perform(post("/api/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "invalid.prompt",
                                  "description": "缺少模板",
                                  "category": "Operations",
                                  "variables": ["incident_id"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var promptId = extractJsonString(created, "id");

        mockMvc.perform(post("/api/prompts/{id}/publish", promptId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation_error"));
    }

    @Test
    void returnsResourceListAndDetail() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == 'res_policy_docs')]").exists())
                .andExpect(jsonPath("$.items[0].type").exists())
                .andExpect(jsonPath("$.items[0].linkedTools").exists())
                .andExpect(jsonPath("$.items[0].referenceCount").exists());

        mockMvc.perform(get("/api/resources/res_customer_schema"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.id").value("res_customer_schema"))
                .andExpect(jsonPath("$.contentSummary").exists())
                .andExpect(jsonPath("$.schemaPreview").exists())
                .andExpect(jsonPath("$.recentReferences").exists());
    }

    @Test
    void createsUpdatesPublishesAndArchivesResourceWithAuditLogs() throws Exception {
        var auditCount = countRows("audit_logs");
        var created = mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "incident-runbook",
                                  "type": "DOCUMENT",
                                  "description": "事件响应 runbook",
                                  "contentSummary": "包含升级、审批和 Trace Evidence 要点。",
                                  "markdownPreview": "## Runbook",
                                  "tags": ["ops", "runbook"],
                                  "linkedTools": ["ticket.search"],
                                  "relatedPrompts": ["ops.incident.brief"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var resourceId = extractJsonString(created, "id");

        mockMvc.perform(put("/api/resources/{id}", resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "incident-runbook-v2",
                                  "type": "DOCUMENT",
                                  "description": "更新事件响应 runbook",
                                  "contentSummary": "包含升级、审批、Trace Evidence 和 Audit Log 要点。",
                                  "markdownPreview": "## Runbook v2",
                                  "tags": ["ops", "runbook"],
                                  "linkedTools": ["ticket.search"],
                                  "relatedPrompts": ["ops.incident.brief"],
                                  "status": "DRAFT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.name").value("incident-runbook-v2"));

        mockMvc.perform(post("/api/resources/{id}/publish", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.status").value("PUBLISHED"));

        mockMvc.perform(post("/api/resources/{id}/archive", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.status").value("ARCHIVED"));

        org.assertj.core.api.Assertions.assertThat(countRows("audit_logs")).isGreaterThanOrEqualTo(auditCount + 4);
    }

    @Test
    void missingPromptAndResourceReturnStructured404() throws Exception {
        mockMvc.perform(put("/api/prompts/not_exists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "missing",
                                  "templateContent": "{{x}}"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(post("/api/resources/not_exists/archive"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void listsTraceSummariesWithFilters() throws Exception {
        mockMvc.perform(get("/api/traces")
                        .param("riskLevel", "HIGH")
                        .param("reviewRequired", "true")
                        .param("keyword", "db.query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].traceId").exists())
                .andExpect(jsonPath("$.items[0].callId").exists())
                .andExpect(jsonPath("$.items[0].toolName").value("db.query.readonly"))
                .andExpect(jsonPath("$.items[0].reviewRequired").value(true))
                .andExpect(jsonPath("$.items[0].totalLatencyMs").exists());
    }

    @Test
    void returnsTraceDetailByTraceId() throws Exception {
        var response = mockMvc.perform(get("/api/traces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].traceId").exists())
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
    void tracesReturnPaginatedPageResponse() throws Exception {
        mockMvc.perform(get("/api/traces")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.total").exists())
                .andExpect(jsonPath("$.totalPages").exists());
    }

    @Test
    void traceKeywordFilterIsCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/traces")
                        .param("keyword", "DB.QUERY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].toolName").value("db.query.readonly"));
    }

    @Test
    void reviewStatusFilterReturnsMatchingPage() throws Exception {
        mockMvc.perform(get("/api/reviews")
                        .param("status", "PENDING_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void auditActionAndKeywordFiltersReturnMatchingPage() throws Exception {
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
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/audit-logs")
                        .param("action", "tool.invoke")
                        .param("keyword", "pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].action").value("tool.invoke.pending_review"));
    }

    @Test
    void traceMaxPageSizeAndAuditKeywordAreStable() throws Exception {
        mockMvc.perform(get("/api/traces")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));

        mockMvc.perform(get("/api/audit-logs")
                        .param("keyword", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].action").value("tool.invoke.pending_review"));
    }

    @Test
    void promptStatusAndCategoryFiltersReturnMatchingPage() throws Exception {
        mockMvc.perform(get("/api/prompts")
                        .param("status", "ACTIVE")
                        .param("category", "Customer")
                        .param("keyword", "support"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].category").value("Customer Support"));
    }

    @Test
    void resourceStatusAndTypeFiltersReturnMatchingPage() throws Exception {
        mockMvc.perform(get("/api/resources")
                        .param("status", "PUBLISHED")
                        .param("type", "DOCUMENT")
                        .param("keyword", "policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.items[0].type").value("DOCUMENT"));
    }

    @Test
    void pageSizeIsLimitedAndEmptyResultsReturnEmptyPage() throws Exception {
        mockMvc.perform(get("/api/prompts")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));

        mockMvc.perform(get("/api/resources")
                        .param("keyword", "no-match-p5c"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void adminCanCreatePromptWithPolicy() throws Exception {
        mockMvc.perform(post("/api/prompts")
                        .header("X-Demo-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "policy.admin.prompt",
                                  "description": "RBAC demo prompt",
                                  "category": "Security",
                                  "templateContent": "请基于 {{input}} 输出 RBAC demo 说明。",
                                  "variables": ["input"],
                                  "usageScope": "RBAC demo"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt.status").value("DRAFT"));
    }

    @Test
    void adminCanPublishResourceWithPolicy() throws Exception {
        var created = mockMvc.perform(post("/api/resources")
                        .header("X-Demo-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "policy-resource",
                                  "type": "POLICY",
                                  "description": "RBAC demo resource",
                                  "contentSummary": "记录 RBAC demo policy 边界。",
                                  "markdownPreview": "## RBAC demo",
                                  "tags": ["rbac"],
                                  "linkedTools": ["db.query.readonly"],
                                  "relatedPrompts": []
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var resourceId = extractJsonString(created, "id");

        mockMvc.perform(post("/api/resources/{id}/publish", resourceId)
                        .header("X-Demo-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.status").value("PUBLISHED"));
    }

    @Test
    void reviewerCanApproveReviewWithPolicy() throws Exception {
        var response = createPendingReviewCall();
        var reviewId = extractJsonString(response, "reviewId");

        mockMvc.perform(post("/api/reviews/{id}/approve", reviewId)
                        .header("X-Demo-Role", "REVIEWER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewer": "reviewer.li",
                                  "comment": "RBAC demo approve"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void developerCannotApproveReviewAndGetsStructured403() throws Exception {
        var response = createPendingReviewCall();
        var reviewId = extractJsonString(response, "reviewId");
        var auditCount = countRows("audit_logs");

        mockMvc.perform(post("/api/reviews/{id}/approve", reviewId)
                        .header("X-Demo-Role", "DEVELOPER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewer": "developer",
                                  "comment": "should fail"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.action").value("REVIEW_DECIDE"))
                .andExpect(jsonPath("$.role").value("DEVELOPER"))
                .andExpect(jsonPath("$.requestId").exists());

        org.assertj.core.api.Assertions.assertThat(countRows("audit_logs")).isEqualTo(auditCount);
    }

    @Test
    void viewerCannotInvokeHighRiskToolWithPolicy() throws Exception {
        mockMvc.perform(post("/api/tools/db.query.readonly/invoke")
                        .header("X-Demo-Role", "VIEWER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment": "production",
                                  "requester": "viewer",
                                  "parameters": {
                                    "sql": "select * from demo_customers limit 20"
                                  }
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.action").value("TOOL_INVOKE"))
                .andExpect(jsonPath("$.role").value("VIEWER"));
    }

    @Test
    void viewerCanViewTraceWhenPolicyAllowsTraceView() throws Exception {
        mockMvc.perform(get("/api/traces")
                        .header("X-Demo-Role", "VIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void authorizedSensitiveOperationStillWritesAuditLog() throws Exception {
        var auditCount = countRows("audit_logs");
        var response = createPendingReviewCall();
        var reviewId = extractJsonString(response, "reviewId");

        mockMvc.perform(post("/api/reviews/{id}/reject", reviewId)
                        .header("X-Demo-Role", "REVIEWER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewer": "reviewer.li",
                                  "comment": "RBAC demo reject"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        org.assertj.core.api.Assertions.assertThat(countRows("audit_logs")).isGreaterThan(auditCount);
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

    private String createPendingReviewCall() throws Exception {
        return mockMvc.perform(post("/api/tools/db.query.readonly/invoke")
                        .header("X-Demo-Role", "ADMIN")
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
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
    private long countRows(String table) {
        Long count = jdbc.queryForObject("select count(*) from " + table, Long.class);
        return count == null ? 0 : count;
    }

    private String extractJsonString(String json, String field) {
        return json.replaceAll("(?s).*?\\\"" + field + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");
    }
}
