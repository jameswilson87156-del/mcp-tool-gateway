package com.mcp.gateway.model;

import java.util.List;
import java.util.Map;

public record PromptRenderResponse(
        String promptId,
        String renderedPrompt,
        boolean valid,
        List<String> validationErrors,
        Map<String, Object> variables,
        String renderedAt
) {
}
