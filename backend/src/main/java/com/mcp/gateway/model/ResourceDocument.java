package com.mcp.gateway.model;

import java.util.List;

public record ResourceDocument(
        String id,
        String name,
        String type,
        String description,
        String status,
        List<String> tags,
        List<String> linkedTools,
        String updatedAt,
        int referenceCount,
        String contentSummary,
        String schemaPreview,
        String markdownPreview,
        List<String> relatedPrompts
) {
}
