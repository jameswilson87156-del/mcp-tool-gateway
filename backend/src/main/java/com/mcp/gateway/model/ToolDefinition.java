package com.mcp.gateway.model;

import java.util.List;
import java.util.Map;

public record ToolDefinition(
        String id,
        String name,
        String description,
        String category,
        String provider,
        String version,
        RiskLevel riskLevel,
        String status,
        boolean approvalRequired,
        List<ToolParameterSchema> parameters,
        Map<String, Object> schema,
        List<String> permissionScopes,
        int recentCallCount,
        String updatedAt
) {
}
