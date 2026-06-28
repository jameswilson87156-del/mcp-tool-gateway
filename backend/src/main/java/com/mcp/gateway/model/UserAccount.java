package com.mcp.gateway.model;

import java.util.List;

public record UserAccount(
        String id,
        String username,
        String displayName,
        UserRole role,
        List<String> permissionScopes
) {
}
