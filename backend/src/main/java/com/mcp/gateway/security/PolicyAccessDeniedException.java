package com.mcp.gateway.security;

import com.mcp.gateway.model.UserRole;

public class PolicyAccessDeniedException extends RuntimeException {
    private final PolicyAction action;
    private final UserRole role;
    private final String requestId;

    public PolicyAccessDeniedException(PolicyAction action, UserRole role, String requestId) {
        super("\u5f53\u524d\u89d2\u8272\u65e0\u6743\u6267\u884c\u8be5\u64cd\u4f5c");
        this.action = action;
        this.role = role;
        this.requestId = requestId;
    }

    public PolicyAction action() {
        return action;
    }

    public UserRole role() {
        return role;
    }

    public String requestId() {
        return requestId;
    }
}
