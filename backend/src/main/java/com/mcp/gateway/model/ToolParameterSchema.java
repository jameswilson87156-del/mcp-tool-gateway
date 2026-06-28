package com.mcp.gateway.model;

public record ToolParameterSchema(
        String name,
        String type,
        boolean required,
        String description,
        Object example
) {
}
