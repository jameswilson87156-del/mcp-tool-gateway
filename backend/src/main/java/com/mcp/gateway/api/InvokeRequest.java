package com.mcp.gateway.api;

import java.util.Map;

public record InvokeRequest(
        String environment,
        String requester,
        Map<String, Object> parameters
) {
}
