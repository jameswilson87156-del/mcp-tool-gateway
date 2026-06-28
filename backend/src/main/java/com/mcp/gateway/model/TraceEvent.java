package com.mcp.gateway.model;

import java.time.Instant;
import java.util.Map;

public record TraceEvent(
        String id,
        String callId,
        String step,
        CallStatus status,
        String message,
        String latency,
        Map<String, Object> evidence,
        Instant timestamp
) {
}
