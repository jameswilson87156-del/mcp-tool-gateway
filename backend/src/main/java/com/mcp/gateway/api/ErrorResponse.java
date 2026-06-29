package com.mcp.gateway.api;

public record ErrorResponse(
        String code,
        String message,
        String action,
        String role,
        String requestId
) {
}
