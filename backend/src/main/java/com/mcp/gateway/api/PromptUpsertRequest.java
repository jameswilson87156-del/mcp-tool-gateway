package com.mcp.gateway.api;

import java.util.List;

public record PromptUpsertRequest(
        String name,
        String description,
        String category,
        String templateContent,
        List<String> variables,
        String usageScope,
        List<String> relatedTools,
        String status
) {
}
