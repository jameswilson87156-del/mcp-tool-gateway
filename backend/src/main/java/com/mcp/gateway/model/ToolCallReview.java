package com.mcp.gateway.model;

import java.time.Instant;

public record ToolCallReview(
        String id,
        String callId,
        String toolId,
        RiskLevel riskLevel,
        CallStatus status,
        String reviewer,
        String decision,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {
}
