package com.mcp.gateway.api;

import java.util.Map;

public record PromptRenderRequest(
        String requester,
        Map<String, Object> variables
) {
}
