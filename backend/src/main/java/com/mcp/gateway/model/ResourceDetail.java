package com.mcp.gateway.model;

import java.util.List;
import java.util.Map;

public record ResourceDetail(
        ResourceDocument resource,
        String contentSummary,
        String schemaPreview,
        String markdownPreview,
        List<String> linkedTools,
        List<String> relatedPrompts,
        List<Map<String, Object>> recentReferences,
        List<AuditLogEntry> auditLogs
) {
}
