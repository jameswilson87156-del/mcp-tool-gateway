package com.mcp.gateway.model;

import java.time.Instant;
import java.util.Map;

public record ToolCallRecord(
        String id,
        String toolId,
        String toolName,
        String requester,
        String provider,
        String environment,
        RiskLevel riskLevel,
        CallStatus status,
        Map<String, Object> request,
        Map<String, Object> response,
        String reviewId,
        String latency,
        Instant createdAt,
        Instant updatedAt
) {
}
