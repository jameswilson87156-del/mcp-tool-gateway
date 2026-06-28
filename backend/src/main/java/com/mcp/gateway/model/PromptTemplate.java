package com.mcp.gateway.model;

import java.util.List;

public record PromptTemplate(
        String id,
        String name,
        String version,
        String owner,
        RiskLevel riskLevel,
        List<String> variables,
        String systemInstruction,
        String status
) {
}
