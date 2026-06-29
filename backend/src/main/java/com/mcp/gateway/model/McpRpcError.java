package com.mcp.gateway.model;

import java.util.Map;

public record McpRpcError(
        int code,
        String message,
        Map<String, Object> data
) {
}
