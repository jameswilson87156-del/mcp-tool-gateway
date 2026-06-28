package com.mcp.gateway.model;

import java.util.List;

public record ResourceDocument(
        String id,
        String title,
        String type,
        String environment,
        String owner,
        RiskLevel riskLevel,
        List<String> tags,
        List<String> permissionScopes
) {
}
