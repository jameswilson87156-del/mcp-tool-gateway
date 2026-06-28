package com.mcp.gateway.model;

import java.util.List;
import java.util.Map;

public record TraceDetail(
        String traceId,
        String callId,
        ToolCallRecord toolCall,
        Map<String, Object> toolSchemaSummary,
        Map<String, Object> inputJson,
        Map<String, Object> outputJson,
        CallStatus status,
        RiskLevel riskLevel,
        String permissionResult,
        boolean reviewRequired,
        String reviewDecision,
        String reviewer,
        List<AuditLogEntry> auditLogs,
        List<TraceEvent> traceEvents,
        long totalLatencyMs,
        String errorMessage
) {
}
