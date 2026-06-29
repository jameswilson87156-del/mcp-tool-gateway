package com.mcp.gateway.model;

import java.util.List;

public record PromptTemplate(
        String id,
        String name,
        String description,
        String version,
        String category,
        String status,
        List<String> variables,
        String usageScope,
        List<String> relatedTools,
        String updatedAt,
        int usageCount,
        String templateContent
) {
}
