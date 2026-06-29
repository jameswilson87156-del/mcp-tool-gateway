package com.mcp.gateway.model;

import java.util.List;
import java.util.Map;

public record PromptDetail(
        PromptTemplate prompt,
        String templateContent,
        List<String> variables,
        String version,
        String status,
        String usageScope,
        List<String> relatedTools,
        List<Map<String, Object>> recentUsage,
        List<AuditLogEntry> auditLogs,
        List<String> warnings
) {
}
