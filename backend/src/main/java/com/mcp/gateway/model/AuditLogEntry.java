package com.mcp.gateway.model;

import java.time.Instant;
import java.util.Map;

public record AuditLogEntry(
        String id,
        String actor,
        String action,
        String targetType,
        String targetId,
        Map<String, Object> metadata,
        Instant timestamp
) {
}
