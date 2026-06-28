package com.mcp.gateway.model;

import java.util.List;

public record ToolDefinition(
        String id,
        String name,
        String description,
        String provider,
        String version,
        RiskLevel riskLevel,
        List<ToolParameterSchema> parameters,
        List<String> permissionScopes
) {
}
