package com.mcp.gateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;

public record McpRpcResponse(
        String jsonrpc,
        Object id,
        @JsonInclude(JsonInclude.Include.NON_NULL) Object result,
        @JsonInclude(JsonInclude.Include.NON_NULL) McpRpcError error
) {
    public static McpRpcResponse success(Object id, Object result) {
        return new McpRpcResponse("2.0", id, result, null);
    }

    public static McpRpcResponse failure(Object id, int code, String message, java.util.Map<String, Object> data) {
        return new McpRpcResponse("2.0", id, null, new McpRpcError(code, message, data));
    }
}
