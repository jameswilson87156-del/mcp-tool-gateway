package com.mcp.gateway.service;

import com.mcp.gateway.api.InvokeRequest;
import com.mcp.gateway.api.PromptRenderRequest;
import com.mcp.gateway.api.PromptUpsertRequest;
import com.mcp.gateway.api.ResourceUpsertRequest;
import com.mcp.gateway.api.ReviewRequest;
import com.mcp.gateway.model.*;
import com.mcp.gateway.persistence.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class GatewayService {
    private final ToolRepository toolRepository;
    private final PromptRepository promptRepository;
    private final ResourceRepository resourceRepository;
    private final ToolCallRepository toolCallRepository;
    private final ReviewRepository reviewRepository;
    private final TraceRepository traceRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final RolePolicyRepository rolePolicyRepository;
    private final UserAccount demoAdmin = new UserAccount(
            "usr_admin",
            "admin",
            "王震龙",
            UserRole.ADMIN,
            List.of("tool:*", "prompt:*", "resource:*", "review:*", "audit:read")
    );

    public GatewayService(
            ToolRepository toolRepository,
            PromptRepository promptRepository,
            ResourceRepository resourceRepository,
            ToolCallRepository toolCallRepository,
            ReviewRepository reviewRepository,
            TraceRepository traceRepository,
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            RolePolicyRepository rolePolicyRepository
    ) {
        this.toolRepository = toolRepository;
        this.promptRepository = promptRepository;
        this.resourceRepository = resourceRepository;
        this.toolCallRepository = toolCallRepository;
        this.reviewRepository = reviewRepository;
        this.traceRepository = traceRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.rolePolicyRepository = rolePolicyRepository;
        seedIfEmpty();
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
        return toolRepository.findAll().stream().map(this::withRecentCallCount).toList();
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
            toolCallRepository.save(record);
            traceRepository.saveAll(events);
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
            toolCallRepository.save(record);
            reviewRepository.save(review);
            traceRepository.saveAll(events);
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
        toolCallRepository.save(record);
        traceRepository.saveAll(events);
        audit(requester, "tool.invoke.success", "ToolCallRecord", callId, Map.of("tool", tool.id()));
        return record;
    }

    public List<ToolCallRecord> listCalls() {
        return toolCallRepository.findAll().stream()
                .sorted(Comparator.comparing(ToolCallRecord::createdAt).reversed())
                .toList();
    }

    public ToolCallRecord getCall(String id) {
        return toolCallRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Tool call not found: " + id));
    }

    public List<TraceEvent> getTrace(String callId) {
        return traceRepository.findByCallId(callId);
    }

    public PageResponse<TraceSummary> listTraceSummaries(Integer page, Integer size, CallStatus status, RiskLevel riskLevel, String toolName, Boolean reviewRequired, String keyword) {
        var normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        var normalizedTool = toolName == null ? "" : toolName.trim().toLowerCase(Locale.ROOT);
        var items = toolCallRepository.findAll().stream()
                .filter(call -> status == null || call.status() == status)
                .filter(call -> riskLevel == null || call.riskLevel() == riskLevel)
                .filter(call -> normalizedTool.isBlank() || call.toolName().toLowerCase(Locale.ROOT).contains(normalizedTool))
                .filter(call -> reviewRequired == null || isReviewRequired(call) == reviewRequired)
                .filter(call -> normalizedKeyword.isBlank() || traceKeyword(call).contains(normalizedKeyword))
                .sorted(Comparator.comparing(ToolCallRecord::createdAt).reversed())
                .map(this::toTraceSummary)
                .toList();
        return PageResponse.of(items, page, size);
    }

    public TraceDetail getTraceDetail(String traceId) {
        var callId = callIdFromTraceId(traceId);
        var call = getCall(callId);
        var tool = requireTool(call.toolId());
        var review = call.reviewId() == null ? null : reviewRepository.findById(call.reviewId()).orElse(null);
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

    public PageResponse<ToolCallReview> listReviews(Integer page, Integer size, CallStatus status, RiskLevel riskLevel, String toolName, String keyword) {
        var normalizedKeyword = normalize(keyword);
        var normalizedTool = normalize(toolName);
        var items = reviewRepository.findAll().stream()
                .filter(review -> status == null || review.status() == status)
                .filter(review -> riskLevel == null || review.riskLevel() == riskLevel)
                .filter(review -> normalizedTool.isBlank() || normalize(review.toolId()).contains(normalizedTool))
                .filter(review -> normalizedKeyword.isBlank() || reviewKeyword(review).contains(normalizedKeyword))
                .sorted(Comparator.comparing(ToolCallReview::createdAt).reversed())
                .toList();
        return PageResponse.of(items, page, size);
    }

    public ToolCallReview approveReview(String id, ReviewRequest request) {
        var review = requireReview(id);
        var call = getCall(review.callId());
        var now = Instant.now();
        var updated = new ToolCallReview(id, review.callId(), review.toolId(), review.riskLevel(), CallStatus.APPROVED,
                reviewer(request), "APPROVED", comment(request, "人工审批通过，执行 sandbox demo"), review.createdAt(), now);
        var tool = requireTool(review.toolId());
        var response = executeDemoTool(tool, call.request());
        toolCallRepository.save(new ToolCallRecord(call.id(), call.toolId(), call.toolName(), call.requester(), call.provider(),
                call.environment(), call.riskLevel(), CallStatus.SUCCESS, call.request(), response, id, "344 ms", call.createdAt(), now));
        reviewRepository.save(updated);
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
        toolCallRepository.save(new ToolCallRecord(call.id(), call.toolId(), call.toolName(), call.requester(), call.provider(),
                call.environment(), call.riskLevel(), CallStatus.REJECTED, call.request(), Map.of("rejected", true), id, call.latency(), call.createdAt(), now));
        reviewRepository.save(updated);
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
        toolCallRepository.save(new ToolCallRecord(call.id(), call.toolId(), call.toolName(), call.requester(), call.provider(),
                call.environment(), call.riskLevel(), CallStatus.CHANGES_REQUESTED, call.request(), Map.of("changesRequested", true), id, call.latency(), call.createdAt(), now));
        reviewRepository.save(updated);
        appendTrace(call.id(), "Human Review", CallStatus.CHANGES_REQUESTED, "要求补充信息: " + updated.comment(), "18 ms", Map.of("reviewer", updated.reviewer()));
        audit(updated.reviewer(), "review.request_changes", "ToolCallReview", id, Map.of("callId", call.id()));
        return updated;
    }

    public PageResponse<PromptTemplate> listPrompts(Integer page, Integer size, String keyword, String status, String category) {
        var normalizedKeyword = normalize(keyword);
        var normalizedStatus = normalize(status);
        var normalizedCategory = normalize(category);
        var items = promptRepository.findAll().stream()
                .filter(prompt -> normalizedStatus.isBlank() || normalize(prompt.status()).equals(normalizedStatus))
                .filter(prompt -> normalizedCategory.isBlank() || normalize(prompt.category()).contains(normalizedCategory))
                .filter(prompt -> normalizedKeyword.isBlank() || promptKeyword(prompt).contains(normalizedKeyword))
                .toList();
        return PageResponse.of(items, page, size);
    }

    public PromptDetail getPromptDetail(String id) {
        var prompt = requirePrompt(id);
        return promptDetail(prompt, List.of());
    }

    public PromptDetail createPrompt(PromptUpsertRequest request) {
        var now = Instant.now().toString();
        var prompt = new PromptTemplate(
                "prompt_" + shortId(),
                requireText(request.name(), "Prompt name is required"),
                text(request.description()),
                "v1.0.0",
                textOr(request.category(), "General"),
                textOr(request.status(), "DRAFT"),
                listOrEmpty(request.variables()),
                text(request.usageScope()),
                listOrEmpty(request.relatedTools()),
                now,
                0,
                text(request.templateContent())
        );
        promptRepository.save(prompt);
        audit(demoAdmin.username(), "prompt.create", "PromptTemplate", prompt.id(), Map.of("status", prompt.status()));
        return promptDetail(prompt, List.of());
    }

    public PromptDetail updatePrompt(String id, PromptUpsertRequest request) {
        var existing = requirePrompt(id);
        var updated = new PromptTemplate(
                existing.id(),
                requireText(request.name(), "Prompt name is required"),
                text(request.description()),
                existing.version(),
                textOr(request.category(), existing.category()),
                textOr(request.status(), "DRAFT"),
                listOrEmpty(request.variables()),
                text(request.usageScope()),
                listOrEmpty(request.relatedTools()),
                Instant.now().toString(),
                existing.usageCount(),
                text(request.templateContent())
        );
        promptRepository.save(updated);
        audit(demoAdmin.username(), "prompt.update", "PromptTemplate", updated.id(), Map.of("status", updated.status()));
        return promptDetail(updated, List.of());
    }

    public PromptDetail publishPrompt(String id) {
        var existing = requirePrompt(id);
        requireText(existing.name(), "Prompt name is required before publish");
        requireText(existing.templateContent(), "Prompt templateContent is required before publish");
        var warnings = existing.variables().stream()
                .filter(variable -> !existing.templateContent().contains("{{" + variable + "}}"))
                .map(variable -> "变量未在模板中找到: " + variable)
                .toList();
        var published = new PromptTemplate(
                existing.id(),
                existing.name(),
                existing.description(),
                existing.version(),
                existing.category(),
                "ACTIVE",
                existing.variables(),
                existing.usageScope(),
                existing.relatedTools(),
                Instant.now().toString(),
                existing.usageCount(),
                existing.templateContent()
        );
        promptRepository.save(published);
        audit(demoAdmin.username(), "prompt.publish", "PromptTemplate", published.id(), Map.of("warnings", warnings));
        return promptDetail(published, warnings);
    }

    public PromptDetail archivePrompt(String id) {
        var existing = requirePrompt(id);
        var archived = new PromptTemplate(
                existing.id(),
                existing.name(),
                existing.description(),
                existing.version(),
                existing.category(),
                "ARCHIVED",
                existing.variables(),
                existing.usageScope(),
                existing.relatedTools(),
                Instant.now().toString(),
                existing.usageCount(),
                existing.templateContent()
        );
        promptRepository.save(archived);
        audit(demoAdmin.username(), "prompt.archive", "PromptTemplate", archived.id(), Map.of("previousStatus", existing.status()));
        return promptDetail(archived, List.of());
    }

    private PromptDetail promptDetail(PromptTemplate prompt, List<String> warnings) {
        return new PromptDetail(
                prompt,
                prompt.templateContent(),
                prompt.variables(),
                prompt.version(),
                prompt.status(),
                prompt.usageScope(),
                prompt.relatedTools(),
                recentPromptUsage(prompt),
                relatedContentAuditLogs("PromptTemplate", prompt.id()),
                warnings
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
        promptRepository.save(updatedPrompt);
        audit(actor, "prompt.render.success", "PromptTemplate", prompt.id(), Map.of("variables", variables.keySet(), "sandbox", true));
        return new PromptRenderResponse(prompt.id(), rendered, true, List.of(), variables, now.toString());
    }

    public PageResponse<ResourceDocument> listResources(Integer page, Integer size, String keyword, String status, String type) {
        var normalizedKeyword = normalize(keyword);
        var normalizedStatus = normalize(status);
        var normalizedType = normalize(type);
        var items = resourceRepository.findAll().stream()
                .filter(resource -> normalizedStatus.isBlank() || normalize(resource.status()).equals(normalizedStatus))
                .filter(resource -> normalizedType.isBlank() || normalize(resource.type()).equals(normalizedType))
                .filter(resource -> normalizedKeyword.isBlank() || resourceKeyword(resource).contains(normalizedKeyword))
                .toList();
        return PageResponse.of(items, page, size);
    }

    public ResourceDetail getResourceDetail(String id) {
        var resource = requireResource(id);
        return resourceDetail(resource);
    }

    public ResourceDetail createResource(ResourceUpsertRequest request) {
        var now = Instant.now().toString();
        var resource = new ResourceDocument(
                "res_" + shortId(),
                requireText(request.name(), "Resource name is required"),
                textOr(request.type(), "DOCUMENT"),
                text(request.description()),
                textOr(request.status(), "DRAFT"),
                listOrEmpty(request.tags()),
                listOrEmpty(request.linkedTools()),
                now,
                0,
                text(request.contentSummary()),
                text(request.schemaPreview()),
                text(request.markdownPreview()),
                listOrEmpty(request.relatedPrompts())
        );
        resourceRepository.save(resource);
        audit(demoAdmin.username(), "resource.create", "ResourceDocument", resource.id(), Map.of("status", resource.status()));
        return resourceDetail(resource);
    }

    public ResourceDetail updateResource(String id, ResourceUpsertRequest request) {
        var existing = requireResource(id);
        var updated = new ResourceDocument(
                existing.id(),
                requireText(request.name(), "Resource name is required"),
                textOr(request.type(), existing.type()),
                text(request.description()),
                textOr(request.status(), "DRAFT"),
                listOrEmpty(request.tags()),
                listOrEmpty(request.linkedTools()),
                Instant.now().toString(),
                existing.referenceCount(),
                text(request.contentSummary()),
                text(request.schemaPreview()),
                text(request.markdownPreview()),
                listOrEmpty(request.relatedPrompts())
        );
        resourceRepository.save(updated);
        audit(demoAdmin.username(), "resource.update", "ResourceDocument", updated.id(), Map.of("status", updated.status()));
        return resourceDetail(updated);
    }

    public ResourceDetail publishResource(String id) {
        var existing = requireResource(id);
        requireText(existing.name(), "Resource name is required before publish");
        requireText(existing.type(), "Resource type is required before publish");
        requireText(existing.contentSummary(), "Resource contentSummary is required before publish");
        var published = new ResourceDocument(
                existing.id(),
                existing.name(),
                existing.type(),
                existing.description(),
                "PUBLISHED",
                existing.tags(),
                existing.linkedTools(),
                Instant.now().toString(),
                existing.referenceCount(),
                existing.contentSummary(),
                existing.schemaPreview(),
                existing.markdownPreview(),
                existing.relatedPrompts()
        );
        resourceRepository.save(published);
        audit(demoAdmin.username(), "resource.publish", "ResourceDocument", published.id(), Map.of("type", published.type()));
        return resourceDetail(published);
    }

    public ResourceDetail archiveResource(String id) {
        var existing = requireResource(id);
        var archived = new ResourceDocument(
                existing.id(),
                existing.name(),
                existing.type(),
                existing.description(),
                "ARCHIVED",
                existing.tags(),
                existing.linkedTools(),
                Instant.now().toString(),
                existing.referenceCount(),
                existing.contentSummary(),
                existing.schemaPreview(),
                existing.markdownPreview(),
                existing.relatedPrompts()
        );
        resourceRepository.save(archived);
        audit(demoAdmin.username(), "resource.archive", "ResourceDocument", archived.id(), Map.of("previousStatus", existing.status()));
        return resourceDetail(archived);
    }

    private ResourceDetail resourceDetail(ResourceDocument resource) {
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

    public PageResponse<AuditLogEntry> listAuditLogs(Integer page, Integer size, String action, String actor, String target, String keyword) {
        var normalizedAction = normalize(action);
        var normalizedActor = normalize(actor);
        var normalizedTarget = normalize(target);
        var normalizedKeyword = normalize(keyword);
        var items = auditLogRepository.findAll().stream()
                .filter(entry -> normalizedAction.isBlank() || normalize(entry.action()).contains(normalizedAction))
                .filter(entry -> normalizedActor.isBlank() || normalize(entry.actor()).contains(normalizedActor))
                .filter(entry -> normalizedTarget.isBlank() || normalize(entry.targetType()).contains(normalizedTarget) || normalize(entry.targetId()).contains(normalizedTarget))
                .filter(entry -> normalizedKeyword.isBlank() || auditKeyword(entry).contains(normalizedKeyword))
                .sorted(Comparator.comparing(AuditLogEntry::timestamp).reversed())
                .toList();
        return PageResponse.of(items, page, size);
    }

    public Map<String, Object> dashboardStats() {
        var calls = toolCallRepository.findAll();
        var pending = calls.stream().filter(c -> c.status() == CallStatus.PENDING_REVIEW).count();
        var blocked = calls.stream().filter(c -> c.status() == CallStatus.BLOCKED).count();
        return Map.of(
                "toolCount", toolRepository.count(),
                "promptCount", promptRepository.count(),
                "resourceCount", resourceRepository.count(),
                "pendingReviews", pending,
                "blockedCalls", blocked,
                "providerStatus", "12/12 正常",
                "boundary", "MCP-style demo, not a complete official MCP implementation"
        );
    }

    private ToolDefinition requireTool(String id) {
        return toolRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Tool not found: " + id));
    }

    private ToolCallReview requireReview(String id) {
        return reviewRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Review not found: " + id));
    }

    private PromptTemplate requirePrompt(String id) {
        return promptRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Prompt not found: " + id));
    }

    private ResourceDocument requireResource(String id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Resource not found: " + id));
    }

    private void appendTrace(String callId, String step, CallStatus status, String message, String latency, Map<String, Object> evidence) {
        traceRepository.save(trace(callId, step, status, message, latency, evidence));
    }

    private TraceEvent trace(String callId, String step, CallStatus status, String message, String latency, Map<String, Object> evidence) {
        return new TraceEvent("evt_" + shortId(), callId, step, status, message, latency, evidence, Instant.now());
    }

    private void audit(String actor, String action, String targetType, String targetId, Map<String, Object> metadata) {
        auditLogRepository.save(new AuditLogEntry("aud_" + shortId(), actor, action, targetType, targetId, metadata, Instant.now()));
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

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String textOr(String value, String fallback) {
        var normalized = text(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private String requireText(String value, String message) {
        var normalized = text(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private List<String> listOrEmpty(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
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
            if (toolCallRepository.findById(possibleCallId).isPresent()) {
                return possibleCallId;
            }
        }
        if (toolCallRepository.findById(traceId).isPresent()) {
            return traceId;
        }
        throw new NoSuchElementException("Trace not found: " + traceId);
    }

    private CallStatus reviewStatus(ToolCallRecord call) {
        if (call.reviewId() == null) {
            return isReviewRequired(call) ? call.status() : CallStatus.SUCCESS;
        }
        return reviewRepository.findById(call.reviewId()).map(ToolCallReview::status).orElse(call.status());
    }

    private boolean isReviewRequired(ToolCallRecord call) {
        return toolRepository.findById(call.toolId()).map(ToolDefinition::approvalRequired).orElse(false);
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

    private String reviewKeyword(ToolCallReview review) {
        return normalize(String.join(" ",
                review.id(),
                review.callId(),
                review.toolId(),
                review.riskLevel().name(),
                review.status().name(),
                review.decision(),
                review.comment() == null ? "" : review.comment(),
                review.reviewer() == null ? "" : review.reviewer()
        ));
    }

    private String promptKeyword(PromptTemplate prompt) {
        return normalize(String.join(" ",
                prompt.id(),
                prompt.name(),
                prompt.description(),
                prompt.category(),
                prompt.status(),
                prompt.usageScope(),
                String.join(" ", prompt.variables()),
                String.join(" ", prompt.relatedTools())
        ));
    }

    private String resourceKeyword(ResourceDocument resource) {
        return normalize(String.join(" ",
                resource.id(),
                resource.name(),
                resource.type(),
                resource.description(),
                resource.status(),
                resource.contentSummary(),
                String.join(" ", resource.tags()),
                String.join(" ", resource.linkedTools()),
                String.join(" ", resource.relatedPrompts())
        ));
    }

    private String auditKeyword(AuditLogEntry entry) {
        return normalize(String.join(" ",
                entry.id(),
                entry.actor(),
                entry.action(),
                entry.targetType(),
                entry.targetId(),
                String.valueOf(entry.metadata())
        ));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private List<AuditLogEntry> relatedAuditLogs(ToolCallRecord call) {
        return auditLogRepository.findAll().stream()
                .filter(log -> log.targetId().equals(call.id())
                        || (call.reviewId() != null && log.targetId().equals(call.reviewId()))
                        || String.valueOf(log.metadata().getOrDefault("callId", "")).equals(call.id()))
                .toList();
    }

    private List<AuditLogEntry> relatedContentAuditLogs(String targetType, String targetId) {
        return auditLogRepository.findAll().stream()
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
        var count = toolCallRepository.countByToolId(tool.id());
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
        toolRepository.save(new ToolDefinition(
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

    public void seedIfEmpty() {
        if (userRepository.count() == 0) {
            seedDemoUsers();
        }
        if (rolePolicyRepository.count() == 0) {
            seedRolePolicies();
        }
        if (toolRepository.count() > 0) {
            return;
        }
        seedTools();
        seedPrompts();
        seedResources();
        seedPendingReview();
    }

    private void seedDemoUsers() {
        userRepository.save(demoAdmin);
        userRepository.save(new UserAccount("usr_developer", "developer", "Demo Developer", UserRole.DEVELOPER,
                List.of("tool:read", "tool:invoke", "prompt:read", "resource:read", "trace:read")));
        userRepository.save(new UserAccount("usr_reviewer", "reviewer.li", "审核员李", UserRole.REVIEWER,
                List.of("tool:read", "review:*", "trace:read", "audit:read")));
        userRepository.save(new UserAccount("usr_viewer", "viewer", "只读观察员", UserRole.VIEWER,
                List.of("tool:read", "prompt:read", "resource:read", "trace:read")));
    }

    private void seedRolePolicies() {
        for (String action : List.of("TOOL_INVOKE", "TOOL_MANAGE", "PROMPT_EDIT", "PROMPT_PUBLISH", "RESOURCE_EDIT",
                "RESOURCE_PUBLISH", "REVIEW_DECIDE", "TRACE_VIEW", "AUDIT_VIEW", "SETTINGS_MANAGE")) {
            rolePolicyRepository.save(UserRole.ADMIN, action, true);
        }
        for (String action : List.of("TOOL_INVOKE", "TRACE_VIEW")) {
            rolePolicyRepository.save(UserRole.DEVELOPER, action, true);
        }
        for (String action : List.of("REVIEW_DECIDE", "TRACE_VIEW", "AUDIT_VIEW")) {
            rolePolicyRepository.save(UserRole.REVIEWER, action, true);
        }
        rolePolicyRepository.save(UserRole.VIEWER, "TRACE_VIEW", true);
    }

    private void seedPrompts() {
        var now = Instant.now().minusSeconds(2800).toString();
        promptRepository.save(new PromptTemplate(
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
        promptRepository.save(new PromptTemplate(
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
        resourceRepository.save(new ResourceDocument(
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
        resourceRepository.save(new ResourceDocument(
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
