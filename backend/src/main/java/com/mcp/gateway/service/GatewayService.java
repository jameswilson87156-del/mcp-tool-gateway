package com.mcp.gateway.service;

import com.mcp.gateway.api.InvokeRequest;
import com.mcp.gateway.api.ReviewRequest;
import com.mcp.gateway.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GatewayService {
    private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();
    private final Map<String, ToolCallRecord> calls = new ConcurrentHashMap<>();
    private final Map<String, ToolCallReview> reviews = new ConcurrentHashMap<>();
    private final Map<String, List<TraceEvent>> traces = new ConcurrentHashMap<>();
    private final List<PromptTemplate> prompts = new ArrayList<>();
    private final List<ResourceDocument> resources = new ArrayList<>();
    private final List<AuditLogEntry> auditLogs = Collections.synchronizedList(new ArrayList<>());
    private final UserAccount demoAdmin = new UserAccount(
            "usr_admin",
            "admin",
            "平台管理员",
            UserRole.ADMIN,
            List.of("tool:*", "prompt:*", "resource:*", "review:*", "audit:read")
    );

    public GatewayService() {
        seedTools();
        seedPrompts();
        seedResources();
        seedPendingReview();
    }

    public UserAccount login(String username) {
        var account = username == null || username.isBlank() ? demoAdmin : new UserAccount(
                "usr_" + username.toLowerCase(Locale.ROOT),
                username,
                "Demo " + username,
                UserRole.DEVELOPER,
                List.of("tool:read", "tool:invoke", "trace:read")
        );
        audit("system", "auth.login", "UserAccount", account.id(), Map.of("demo", true));
        return account;
    }

    public UserAccount me() {
        return demoAdmin;
    }

    public List<ToolDefinition> listTools() {
        return tools.values().stream().map(this::withRecentCallCount).toList();
    }

    public ToolDefinition getTool(String id) {
        return withRecentCallCount(requireTool(id));
    }

    public ToolCallRecord invoke(String id, InvokeRequest request) {
        var tool = requireTool(id);
        var now = Instant.now();
        var callId = "call_" + shortId();
        var environment = request.environment() == null || request.environment().isBlank()
                ? "production"
                : request.environment();
        var requester = request.requester() == null || request.requester().isBlank()
                ? demoAdmin.username()
                : request.requester();
        var requestParams = request.parameters() == null ? Map.<String, Object>of() : request.parameters();
        var events = new ArrayList<TraceEvent>();

        events.add(trace(callId, "Request", CallStatus.RUNNING, "接收 Tool invoke 请求", "4 ms",
                Map.of("environment", environment, "requester", requester)));
        events.add(trace(callId, "Tool Select", CallStatus.SUCCESS, "匹配 ToolDefinition: " + tool.name(), "8 ms",
                Map.of("toolId", tool.id(), "provider", tool.provider())));
        events.add(trace(callId, "Schema Check", CallStatus.SUCCESS, "校验 Tool Schema 与 JSON 参数格式", "18 ms",
                Map.of("parameterCount", tool.parameters().size(), "required", requiredParameterNames(tool))));
        events.add(trace(callId, "Permission Check", CallStatus.SUCCESS, "校验 RBAC demo 权限范围", "42 ms",
                Map.of("scopes", tool.permissionScopes(), "role", demoAdmin.role())));

        if (tool.riskLevel() == RiskLevel.BLOCKED || containsBlockedSql(tool, requestParams)) {
            events.add(trace(callId, "Human Review", CallStatus.BLOCKED, "local-rule fallback 阻断危险操作", "12 ms",
                    Map.of("reason", "blocked risk or non-readonly SQL")));
            events.add(trace(callId, "Execute", CallStatus.BLOCKED, "已阻断，未执行 sandbox demo", "0 ms", Map.of()));
            events.add(trace(callId, "Audit Log", CallStatus.SUCCESS, "写入 Audit Log", "6 ms", Map.of("action", "tool.invoke.blocked")));
            var record = new ToolCallRecord(callId, tool.id(), tool.name(), requester, tool.provider(), environment,
                    RiskLevel.BLOCKED, CallStatus.BLOCKED, requestParams, Map.of("blocked", true, "message", "local-rule fallback blocked this request"),
                    null, "90 ms", now, now);
            calls.put(callId, record);
            traces.put(callId, events);
            audit(requester, "tool.invoke.blocked", "ToolCallRecord", callId, Map.of("tool", tool.id()));
            return record;
        }

        if (tool.riskLevel() == RiskLevel.HIGH) {
            var reviewId = "rev_" + shortId();
            events.add(trace(callId, "Human Review", CallStatus.PENDING_REVIEW, "高风险 Tool 调用进入 Human Review", "15 ms",
                    Map.of("reviewId", reviewId, "riskLevel", tool.riskLevel())));
            events.add(trace(callId, "Execute", CallStatus.PENDING_REVIEW, "等待人工审批，未执行 sandbox demo", "0 ms", Map.of()));
            events.add(trace(callId, "Audit Log", CallStatus.SUCCESS, "写入 Audit Log", "6 ms", Map.of("action", "tool.invoke.pending_review")));
            var record = new ToolCallRecord(callId, tool.id(), tool.name(), requester, tool.provider(), environment,
                    tool.riskLevel(), CallStatus.PENDING_REVIEW, requestParams, Map.of("pendingReview", true),
                    reviewId, "83 ms", now, now);
            var review = new ToolCallReview(reviewId, callId, tool.id(), tool.riskLevel(), CallStatus.PENDING_REVIEW,
                    null, "PENDING", "等待人工审批", now, now);
            calls.put(callId, record);
            reviews.put(reviewId, review);
            traces.put(callId, events);
            audit(requester, "tool.invoke.pending_review", "ToolCallReview", reviewId, Map.of("callId", callId));
            return record;
        }

        var response = executeDemoTool(tool, requestParams);
        events.add(trace(callId, "Human Review", CallStatus.SUCCESS, "风险等级允许直接执行，无需审批", "9 ms",
                Map.of("riskLevel", tool.riskLevel())));
        events.add(trace(callId, "Execute", CallStatus.SUCCESS, "执行 sandbox demo Tool 逻辑", "176 ms",
                Map.of("provider", tool.provider(), "sandbox", true)));
        events.add(trace(callId, "Audit Log", CallStatus.SUCCESS, "写入 Audit Log", "6 ms", Map.of("action", "tool.invoke.success")));
        var record = new ToolCallRecord(callId, tool.id(), tool.name(), requester, tool.provider(), environment,
                tool.riskLevel(), CallStatus.SUCCESS, requestParams, response, null, "286 ms", now, now);
        calls.put(callId, record);
        traces.put(callId, events);
        audit(requester, "tool.invoke.success", "ToolCallRecord", callId, Map.of("tool", tool.id()));
        return record;
    }

    public List<ToolCallRecord> listCalls() {
        return calls.values().stream()
                .sorted(Comparator.comparing(ToolCallRecord::createdAt).reversed())
                .toList();
    }

    public ToolCallRecord getCall(String id) {
        return Optional.ofNullable(calls.get(id)).orElseThrow(() -> new NoSuchElementException("Tool call not found: " + id));
    }

    public List<TraceEvent> getTrace(String callId) {
        return traces.getOrDefault(callId, List.of());
    }

    public List<ToolCallReview> listReviews() {
        return reviews.values().stream()
                .sorted(Comparator.comparing(ToolCallReview::createdAt).reversed())
                .toList();
    }

    public ToolCallReview approveReview(String id, ReviewRequest request) {
        var review = requireReview(id);
        var call = getCall(review.callId());
        var now = Instant.now();
        var updated = new ToolCallReview(id, review.callId(), review.toolId(), review.riskLevel(), CallStatus.APPROVED,
                reviewer(request), "APPROVED", comment(request, "人工审批通过，执行 sandbox demo"), review.createdAt(), now);
        var tool = requireTool(review.toolId());
        var response = executeDemoTool(tool, call.request());
        calls.put(call.id(), new ToolCallRecord(call.id(), call.toolId(), call.toolName(), call.requester(), call.provider(),
                call.environment(), call.riskLevel(), CallStatus.SUCCESS, call.request(), response, id, "344 ms", call.createdAt(), now));
        reviews.put(id, updated);
        appendTrace(call.id(), "Human Review", CallStatus.APPROVED, "审批通过: " + updated.comment(), "20 ms", Map.of("reviewer", updated.reviewer()));
        appendTrace(call.id(), "Execute", CallStatus.SUCCESS, "审批后执行 sandbox demo Tool", "190 ms", Map.of("sandbox", true));
        audit(updated.reviewer(), "review.approve", "ToolCallReview", id, Map.of("callId", call.id()));
        return updated;
    }

    public ToolCallReview rejectReview(String id, ReviewRequest request) {
        var review = requireReview(id);
        var call = getCall(review.callId());
        var now = Instant.now();
        var updated = new ToolCallReview(id, review.callId(), review.toolId(), review.riskLevel(), CallStatus.REJECTED,
                reviewer(request), "REJECTED", comment(request, "人工审批拒绝，未执行 Tool"), review.createdAt(), now);
        calls.put(call.id(), new ToolCallRecord(call.id(), call.toolId(), call.toolName(), call.requester(), call.provider(),
                call.environment(), call.riskLevel(), CallStatus.REJECTED, call.request(), Map.of("rejected", true), id, call.latency(), call.createdAt(), now));
        reviews.put(id, updated);
        appendTrace(call.id(), "Human Review", CallStatus.REJECTED, "审批拒绝: " + updated.comment(), "18 ms", Map.of("reviewer", updated.reviewer()));
        audit(updated.reviewer(), "review.reject", "ToolCallReview", id, Map.of("callId", call.id()));
        return updated;
    }

    public ToolCallReview requestChanges(String id, ReviewRequest request) {
        var review = requireReview(id);
        var call = getCall(review.callId());
        var now = Instant.now();
        var updated = new ToolCallReview(id, review.callId(), review.toolId(), review.riskLevel(), CallStatus.CHANGES_REQUESTED,
                reviewer(request), "REQUEST_CHANGES", comment(request, "需要补充上下文或缩小权限范围"), review.createdAt(), now);
        calls.put(call.id(), new ToolCallRecord(call.id(), call.toolId(), call.toolName(), call.requester(), call.provider(),
                call.environment(), call.riskLevel(), CallStatus.CHANGES_REQUESTED, call.request(), Map.of("changesRequested", true), id, call.latency(), call.createdAt(), now));
        reviews.put(id, updated);
        appendTrace(call.id(), "Human Review", CallStatus.CHANGES_REQUESTED, "要求补充信息: " + updated.comment(), "18 ms", Map.of("reviewer", updated.reviewer()));
        audit(updated.reviewer(), "review.request_changes", "ToolCallReview", id, Map.of("callId", call.id()));
        return updated;
    }

    public List<PromptTemplate> listPrompts() {
        return List.copyOf(prompts);
    }

    public List<ResourceDocument> listResources() {
        return List.copyOf(resources);
    }

    public List<AuditLogEntry> listAuditLogs() {
        return auditLogs.stream()
                .sorted(Comparator.comparing(AuditLogEntry::timestamp).reversed())
                .toList();
    }

    public Map<String, Object> dashboardStats() {
        var pending = calls.values().stream().filter(c -> c.status() == CallStatus.PENDING_REVIEW).count();
        var blocked = calls.values().stream().filter(c -> c.status() == CallStatus.BLOCKED).count();
        return Map.of(
                "toolCount", tools.size(),
                "promptCount", prompts.size(),
                "resourceCount", resources.size(),
                "pendingReviews", pending,
                "blockedCalls", blocked,
                "providerStatus", "12/12 正常",
                "boundary", "MCP-style demo, not a complete official MCP implementation"
        );
    }

    private ToolDefinition requireTool(String id) {
        return Optional.ofNullable(tools.get(id)).orElseThrow(() -> new NoSuchElementException("Tool not found: " + id));
    }

    private ToolCallReview requireReview(String id) {
        return Optional.ofNullable(reviews.get(id)).orElseThrow(() -> new NoSuchElementException("Review not found: " + id));
    }

    private void appendTrace(String callId, String step, CallStatus status, String message, String latency, Map<String, Object> evidence) {
        traces.computeIfAbsent(callId, ignored -> new ArrayList<>()).add(trace(callId, step, status, message, latency, evidence));
    }

    private TraceEvent trace(String callId, String step, CallStatus status, String message, String latency, Map<String, Object> evidence) {
        return new TraceEvent("evt_" + shortId(), callId, step, status, message, latency, evidence, Instant.now());
    }

    private void audit(String actor, String action, String targetType, String targetId, Map<String, Object> metadata) {
        auditLogs.add(new AuditLogEntry("aud_" + shortId(), actor, action, targetType, targetId, metadata, Instant.now()));
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String reviewer(ReviewRequest request) {
        return request.reviewer() == null || request.reviewer().isBlank() ? demoAdmin.username() : request.reviewer();
    }

    private String comment(ReviewRequest request, String fallback) {
        return request.comment() == null || request.comment().isBlank() ? fallback : request.comment();
    }

    private List<String> requiredParameterNames(ToolDefinition tool) {
        return tool.parameters().stream().filter(ToolParameterSchema::required).map(ToolParameterSchema::name).toList();
    }

    private ToolDefinition withRecentCallCount(ToolDefinition tool) {
        var count = (int) calls.values().stream().filter(call -> call.toolId().equals(tool.id())).count();
        return new ToolDefinition(
                tool.id(),
                tool.name(),
                tool.description(),
                tool.category(),
                tool.provider(),
                tool.version(),
                tool.riskLevel(),
                tool.status(),
                tool.approvalRequired(),
                tool.parameters(),
                tool.schema(),
                tool.permissionScopes(),
                count,
                tool.updatedAt()
        );
    }

    private boolean containsBlockedSql(ToolDefinition tool, Map<String, Object> params) {
        if (!tool.id().equals("db.query.readonly")) {
            return false;
        }
        var sql = String.valueOf(params.getOrDefault("sql", "")).toLowerCase(Locale.ROOT);
        return List.of("insert", "update", "delete", "drop", "alter", "truncate").stream().anyMatch(sql::contains)
                || !sql.trim().startsWith("select");
    }

    private Map<String, Object> executeDemoTool(ToolDefinition tool, Map<String, Object> params) {
        return switch (tool.id()) {
            case "weather.lookup" -> Map.of("city", params.getOrDefault("city", "上海"), "condition", "多云", "temperature", "24 C", "demo", true);
            case "ticket.search" -> Map.of("count", 2, "tickets", List.of(
                    Map.of("id", "TCK-1024", "status", "open", "title", "Provider latency spike"),
                    Map.of("id", "TCK-1042", "status", "review", "title", "Schema mismatch in CRM tool")
            ), "demo", true);
            case "resume.analyze" -> Map.of("score", 86, "matchedSkills", List.of("Java", "Vue", "Agent workflow"), "recommendation", "进入人工复核", "demo", true);
            case "github.issue.search" -> Map.of("repository", params.getOrDefault("repository", "demo/mcp-gateway"), "issues", List.of(
                    Map.of("id", 17, "title", "Add Trace Evidence filter", "state", "open")
            ), "demo", true);
            case "db.query.readonly" -> Map.of("rows", List.of(
                    Map.of("id", "ROW-001", "name", "demo_readonly_record", "status", "active")
            ), "readonly", true, "demo", true);
            case "crm.customer.search" -> Map.of("count", 2, "customers", List.of(
                    Map.of("customer_id", params.getOrDefault("customer_id", "CUST-202405-000123"), "name", "北京智创科技有限公司", "region", "APAC", "status", "active"),
                    Map.of("customer_id", "CUST-202405-000456", "name", "杭州云启软件有限公司", "region", "CN", "status", "review")
            ), "demo", true);
            default -> Map.of("message", "sandbox demo result", "demo", true);
        };
    }

    private void seedTools() {
        addTool("weather.lookup", "weather.lookup", "查询城市天气 demo 数据", "External Data", "Weather Sandbox", "v1.0.0", RiskLevel.LOW,
                List.of(param("city", "string", true, "城市", "上海")), List.of("weather:read"));
        addTool("ticket.search", "ticket.search", "查询工单列表", "Operations", "Ticket Service", "v1.1.0", RiskLevel.LOW,
                List.of(param("keyword", "string", false, "关键词", "latency")), List.of("ticket:read"));
        addTool("resume.analyze", "resume.analyze", "分析简历文本并输出 demo 建议", "HR", "HR Sandbox", "v0.9.0", RiskLevel.MEDIUM,
                List.of(param("resume_text", "string", true, "简历文本", "Java developer")), List.of("hr:resume:read"));
        addTool("github.issue.search", "github.issue.search", "搜索 GitHub issue demo 数据", "Developer", "OpenAI-compatible", "v1.0.4", RiskLevel.MEDIUM,
                List.of(param("repository", "string", true, "仓库", "demo/mcp-gateway"), param("query", "string", false, "搜索条件", "trace")), List.of("github:issue:read"));
        addTool("db.query.readonly", "db.query.readonly", "执行本地 demo SELECT，只允许只读查询", "Database", "Local DB Sandbox", "v1.0.0", RiskLevel.HIGH,
                List.of(param("sql", "string", true, "只读 SELECT SQL", "select * from demo_customers limit 20")), List.of("db:query:readonly"));
        addTool("crm.customer.search", "crm.customer.search", "查询 CRM 客户 demo 数据", "CRM", "OpenAI-compatible", "v1.2.0", RiskLevel.MEDIUM,
                List.of(param("customer_id", "string", false, "客户 ID", "CUST-202405-000123"), param("region", "string", false, "区域", "APAC"), param("limit", "integer", false, "数量限制", 20)), List.of("crm:customer:read"));
    }

    private void addTool(String id, String name, String description, String category, String provider, String version, RiskLevel riskLevel,
                         List<ToolParameterSchema> parameters, List<String> scopes) {
        var required = parameters.stream().filter(ToolParameterSchema::required).map(ToolParameterSchema::name).toList();
        var properties = new LinkedHashMap<String, Object>();
        for (ToolParameterSchema parameter : parameters) {
            properties.put(parameter.name(), Map.of(
                    "type", parameter.type(),
                    "description", parameter.description(),
                    "example", parameter.example()
            ));
        }
        var schema = Map.<String, Object>of(
                "type", "object",
                "required", required,
                "properties", properties
        );
        tools.put(id, new ToolDefinition(
                id,
                name,
                description,
                category,
                provider,
                version,
                riskLevel,
                "ACTIVE",
                riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.BLOCKED,
                parameters,
                schema,
                scopes,
                0,
                Instant.now().minusSeconds(3600).toString()
        ));
    }

    private ToolParameterSchema param(String name, String type, boolean required, String description, Object example) {
        return new ToolParameterSchema(name, type, required, description, example);
    }

    private void seedPrompts() {
        prompts.add(new PromptTemplate("prompt_customer_summary", "customer-support.summary", "v1.2.0", "客服平台组",
                RiskLevel.MEDIUM, List.of("customer_id", "policy_doc", "locale"), "根据客户上下文生成结构化摘要。", "PUBLISHED"));
        prompts.add(new PromptTemplate("prompt_invoice_review", "finance.invoice.review", "v1.1.0", "财务系统组",
                RiskLevel.HIGH, List.of("invoice_id", "vendor", "amount"), "检查发票字段并标注审查原因。", "PENDING_REVIEW"));
    }

    private void seedResources() {
        resources.add(new ResourceDocument("res_policy_docs", "policy-docs", "Knowledge Base", "production", "知识平台组",
                RiskLevel.MEDIUM, List.of("policy", "customer-support"), List.of("resource:policy:read")));
        resources.add(new ResourceDocument("res_customer_knowledge", "customer-knowledge", "Vector Index", "production", "CRM 平台组",
                RiskLevel.MEDIUM, List.of("crm", "customer"), List.of("resource:customer:read")));
    }

    private void seedPendingReview() {
        invoke("db.query.readonly", new InvokeRequest(
                "production",
                "alice.zhang",
                Map.of("sql", "select customer_id, name, status from demo_customers limit 20")
        ));
    }
}
