package com.mcp.gateway.model;

import java.time.Instant;

public record TraceSummary(
        String traceId,
        String callId,
        String toolName,
        String requester,
        RiskLevel riskLevel,
        CallStatus status,
        CallStatus reviewStatus,
        long totalLatencyMs,
        Instant createdAt,
        String provider,
        boolean fallbackUsed,
        boolean reviewRequired
) {
}
