package com.mcp.gateway.service;

import com.mcp.gateway.api.InvokeRequest;
import com.mcp.gateway.api.PromptRenderRequest;
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

    public List<TraceSummary> listTraceSummaries(CallStatus status, RiskLevel riskLevel, String toolName, Boolean reviewRequired, String keyword) {
        var normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        var normalizedTool = toolName == null ? "" : toolName.trim().toLowerCase(Locale.ROOT);
        return calls.values().stream()
                .filter(call -> status == null || call.status() == status)
                .filter(call -> riskLevel == null || call.riskLevel() == riskLevel)
                .filter(call -> normalizedTool.isBlank() || call.toolName().toLowerCase(Locale.ROOT).contains(normalizedTool))
                .filter(call -> reviewRequired == null || isReviewRequired(call) == reviewRequired)
                .filter(call -> normalizedKeyword.isBlank() || traceKeyword(call).contains(normalizedKeyword))
                .sorted(Comparator.comparing(ToolCallRecord::createdAt).reversed())
                .map(this::toTraceSummary)
                .toList();
    }

    public TraceDetail getTraceDetail(String traceId) {
        var callId = callIdFromTraceId(traceId);
        var call = getCall(callId);
        var tool = requireTool(call.toolId());
        var review = call.reviewId() == null ? null : reviews.get(call.reviewId());
        var relatedAuditLogs = relatedAuditLogs(call).stream()
                .sorted(Comparator.comparing(AuditLogEntry::timestamp).reversed())
                .toList();
        var events = getTrace(call.id());
        var permissionResult = events.stream()
                .filter(event -> event.step().equals("Permission Check"))
                .findFirst()
                .map(TraceEvent::message)
                .orElse("RBAC demo permission check recorded");
        var errorMessage = call.status() == CallStatus.BLOCKED || call.status() == CallStatus.FAILED
                ? String.valueOf(call.response().getOrDefault("message", "Trace indicates blocked or failed execution"))
                : null;
        return new TraceDetail(
                traceIdFor(call.id()),
                call.id(),
                call,
                tool.schema(),
                call.request(),
                call.response(),
                call.status(),
                call.riskLevel(),
                permissionResult,
                isReviewRequired(call),
                review == null ? "NOT_REQUIRED" : review.decision(),
                review == null ? null : review.reviewer(),
                relatedAuditLogs,
                events,
                totalLatencyMs(call),
                errorMessage
        );
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

    public PromptDetail getPromptDetail(String id) {
        var prompt = requirePrompt(id);
        return new PromptDetail(
                prompt,
                prompt.templateContent(),
                prompt.variables(),
                prompt.version(),
                prompt.status(),
                prompt.usageScope(),
                prompt.relatedTools(),
                recentPromptUsage(prompt),
                relatedContentAuditLogs("PromptTemplate", prompt.id())
        );
    }

    public PromptRenderResponse renderPrompt(String id, PromptRenderRequest request) {
        var prompt = requirePrompt(id);
        var variables = request.variables() == null ? Map.<String, Object>of() : request.variables();
        var missingVariables = prompt.variables().stream()
                .filter(variable -> !variables.containsKey(variable) || variables.get(variable) == null || String.valueOf(variables.get(variable)).isBlank())
                .toList();
        var actor = request.requester() == null || request.requester().isBlank() ? demoAdmin.username() : request.requester();
        var now = Instant.now();

        if (!missingVariables.isEmpty()) {
            audit(actor, "prompt.render.validation_error", "PromptTemplate", prompt.id(), Map.of("missingVariables", missingVariables));
            return new PromptRenderResponse(
                    prompt.id(),
                    "",
                    false,
                    missingVariables.stream().map(variable -> "缺少变量: " + variable).toList(),
                    variables,
                    now.toString()
            );
        }

        var rendered = prompt.templateContent();
        for (String variable : prompt.variables()) {
            var value = String.valueOf(variables.get(variable));
            rendered = rendered.replace("{{" + variable + "}}", value);
        }
        var updatedPrompt = new PromptTemplate(
                prompt.id(),
                prompt.name(),
                prompt.description(),
                prompt.version(),
                prompt.category(),
                prompt.status(),
                prompt.variables(),
                prompt.usageScope(),
                prompt.relatedTools(),
                now.toString(),
                prompt.usageCount() + 1,
                prompt.templateContent()
        );
        prompts.set(promptIndex(prompt.id()), updatedPrompt);
        audit(actor, "prompt.render.success", "PromptTemplate", prompt.id(), Map.of("variables", variables.keySet(), "sandbox", true));
        return new PromptRenderResponse(prompt.id(), rendered, true, List.of(), variables, now.toString());
    }

    public List<ResourceDocument> listResources() {
        return List.copyOf(resources);
    }

    public ResourceDetail getResourceDetail(String id) {
        var resource = requireResource(id);
        return new ResourceDetail(
                resource,
                resource.contentSummary(),
                resource.schemaPreview(),
                resource.markdownPreview(),
                resource.linkedTools(),
                resource.relatedPrompts(),
                recentResourceReferences(resource),
                relatedContentAuditLogs("ResourceDocument", resource.id())
        );
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

    private PromptTemplate requirePrompt(String id) {
        return prompts.stream()
                .filter(prompt -> prompt.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Prompt not found: " + id));
    }

    private int promptIndex(String id) {
        for (int i = 0; i < prompts.size(); i++) {
            if (prompts.get(i).id().equals(id)) {
                return i;
            }
        }
        throw new NoSuchElementException("Prompt not found: " + id);
    }

    private ResourceDocument requireResource(String id) {
        return resources.stream()
                .filter(resource -> resource.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Resource not found: " + id));
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

    private TraceSummary toTraceSummary(ToolCallRecord call) {
        return new TraceSummary(
                traceIdFor(call.id()),
                call.id(),
                call.toolName(),
                call.requester(),
                call.riskLevel(),
                call.status(),
                reviewStatus(call),
                totalLatencyMs(call),
                call.createdAt(),
                call.provider(),
                fallbackUsed(call),
                isReviewRequired(call)
        );
    }

    private String traceIdFor(String callId) {
        return "trace_" + callId.replace("call_", "");
    }

    private String callIdFromTraceId(String traceId) {
        if (traceId.startsWith("trace_")) {
            var suffix = traceId.substring("trace_".length());
            var possibleCallId = "call_" + suffix;
            if (calls.containsKey(possibleCallId)) {
                return possibleCallId;
            }
        }
        if (calls.containsKey(traceId)) {
            return traceId;
        }
        throw new NoSuchElementException("Trace not found: " + traceId);
    }

    private CallStatus reviewStatus(ToolCallRecord call) {
        if (call.reviewId() == null) {
            return isReviewRequired(call) ? call.status() : CallStatus.SUCCESS;
        }
        var review = reviews.get(call.reviewId());
        return review == null ? call.status() : review.status();
    }

    private boolean isReviewRequired(ToolCallRecord call) {
        var tool = tools.get(call.toolId());
        return tool != null && tool.approvalRequired();
    }

    private boolean fallbackUsed(ToolCallRecord call) {
        return call.status() == CallStatus.BLOCKED
                || getTrace(call.id()).stream().anyMatch(event -> event.message().toLowerCase(Locale.ROOT).contains("fallback"));
    }

    private long totalLatencyMs(ToolCallRecord call) {
        var traceSum = getTrace(call.id()).stream()
                .mapToLong(event -> parseLatencyMs(event.latency()))
                .sum();
        return traceSum > 0 ? traceSum : parseLatencyMs(call.latency());
    }

    private long parseLatencyMs(String latency) {
        if (latency == null) {
            return 0;
        }
        var digits = latency.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return 0;
        }
        return Long.parseLong(digits);
    }

    private String traceKeyword(ToolCallRecord call) {
        return String.join(" ",
                traceIdFor(call.id()),
                call.id(),
                call.toolName(),
                call.requester(),
                call.provider(),
                call.status().name(),
                call.riskLevel().name()
        ).toLowerCase(Locale.ROOT);
    }

    private List<AuditLogEntry> relatedAuditLogs(ToolCallRecord call) {
        return auditLogs.stream()
                .filter(log -> log.targetId().equals(call.id())
                        || (call.reviewId() != null && log.targetId().equals(call.reviewId()))
                        || String.valueOf(log.metadata().getOrDefault("callId", "")).equals(call.id()))
                .toList();
    }

    private List<AuditLogEntry> relatedContentAuditLogs(String targetType, String targetId) {
        return auditLogs.stream()
                .filter(log -> log.targetType().equals(targetType) && log.targetId().equals(targetId))
                .sorted(Comparator.comparing(AuditLogEntry::timestamp).reversed())
                .toList();
    }

    private List<Map<String, Object>> recentPromptUsage(PromptTemplate prompt) {
        var usage = new ArrayList<Map<String, Object>>();
        usage.add(Map.of(
                "tool", prompt.relatedTools().isEmpty() ? "manual.render" : prompt.relatedTools().get(0),
                "actor", "admin",
                "result", "demo/sandbox",
                "timestamp", prompt.updatedAt()
        ));
        relatedContentAuditLogs("PromptTemplate", prompt.id()).stream()
                .filter(log -> log.action().startsWith("prompt.render"))
                .limit(4)
                .forEach(log -> usage.add(Map.of(
                        "tool", "Prompt render",
                        "actor", log.actor(),
                        "result", log.action(),
                        "timestamp", log.timestamp().toString()
                )));
        return usage;
    }

    private List<Map<String, Object>> recentResourceReferences(ResourceDocument resource) {
        var references = new ArrayList<Map<String, Object>>();
        for (String tool : resource.linkedTools()) {
            references.add(Map.of(
                    "tool", tool,
                    "traceId", "trace_demo_reference",
                    "result", "context attached",
                    "timestamp", resource.updatedAt()
            ));
        }
        return references;
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
        var now = Instant.now().minusSeconds(2800).toString();
        prompts.add(new PromptTemplate(
                "prompt_customer_summary",
                "customer-support.summary",
                "根据客户、政策文档和区域语言生成结构化客服摘要。",
                "v1.2.0",
                "Customer Support",
                "ACTIVE",
                List.of("customer_id", "policy_doc", "locale"),
                "客服 Agent 可读，绑定 CRM 与 policy Resource",
                List.of("crm.customer.search", "ticket.search"),
                now,
                42,
                """
                        你是企业客服 Agent。请基于客户 {{customer_id}}、政策资料 {{policy_doc}}，使用 {{locale}} 输出：
                        1. 客户背景摘要
                        2. 可能适用的政策边界
                        3. 下一步 Tool 调用建议
                        保持审计友好，不要编造未提供事实。
                        """
        ));
        prompts.add(new PromptTemplate(
                "prompt_invoice_review",
                "finance.invoice.review",
                "检查发票字段并标注需要 Human Review 的审查原因。",
                "v1.1.0",
                "Finance Governance",
                "DRAFT",
                List.of("invoice_id", "vendor", "amount"),
                "财务审查 demo，仅用于 sandbox 渲染",
                List.of("db.query.readonly"),
                Instant.now().minusSeconds(5400).toString(),
                8,
                """
                        请审查发票 {{invoice_id}}，供应商 {{vendor}}，金额 {{amount}}。
                        输出字段完整性、风险说明、是否需要 Human Review。
                        仅基于输入变量判断，不访问真实财务系统。
                        """
        ));
    }

    private void seedResources() {
        resources.add(new ResourceDocument(
                "res_policy_docs",
                "policy-docs",
                "DOCUMENT",
                "客服政策文档摘要，用于回答客户服务边界和升级条件。",
                "PUBLISHED",
                List.of("policy", "customer-support"),
                List.of("crm.customer.search", "ticket.search"),
                Instant.now().minusSeconds(3600).toString(),
                27,
                "包含退款、升级、企业支持 SLA、区域差异等 demo 政策摘要。",
                "",
                """
                        ## 客服政策摘要
                        - 高价值客户问题优先进入 Human Review。
                        - 涉及退款、合同、隐私字段时需要审计记录。
                        - Agent 只能引用 Resource 摘要，不代表真实企业政策。
                        """,
                List.of("prompt_customer_summary")
        ));
        resources.add(new ResourceDocument(
                "res_customer_schema",
                "customer-db-schema",
                "DB_SCHEMA",
                "CRM 客户查询的字段说明和只读访问边界。",
                "SYNCED",
                List.of("crm", "schema", "readonly"),
                List.of("crm.customer.search", "db.query.readonly"),
                Instant.now().minusSeconds(7200).toString(),
                15,
                "描述 demo_customers 表的只读字段、脱敏约束和 local-rule fallback。",
                """
                        {
                          "table": "demo_customers",
                          "readonly": true,
                          "fields": ["customer_id", "name", "region", "status"],
                          "blocked": ["insert", "update", "delete", "drop"]
                        }
                        """,
                "",
                List.of("prompt_customer_summary", "prompt_invoice_review")
        ));
    }

    private void seedPendingReview() {
        invoke("db.query.readonly", new InvokeRequest(
                "production",
                "alice.zhang",
                Map.of("sql", "select customer_id, name, status from demo_customers limit 20")
        ));
    }
}
