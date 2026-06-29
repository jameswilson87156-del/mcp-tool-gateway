package com.mcp.gateway.api;

import java.util.List;

public record ResourceUpsertRequest(
        String name,
        String type,
        String description,
        String contentSummary,
        String schemaPreview,
        String markdownPreview,
        List<String> tags,
        List<String> linkedTools,
        List<String> relatedPrompts,
        String status
) {
}
