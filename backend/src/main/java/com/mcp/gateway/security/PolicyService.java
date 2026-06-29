package com.mcp.gateway.security;

import com.mcp.gateway.model.UserRole;
import com.mcp.gateway.persistence.RolePolicyRepository;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PolicyService {
    private final RolePolicyRepository rolePolicyRepository;
    private final Map<UserRole, Set<PolicyAction>> fallbackPolicies = new EnumMap<>(UserRole.class);

    public PolicyService(RolePolicyRepository rolePolicyRepository) {
        this.rolePolicyRepository = rolePolicyRepository;
        fallbackPolicies.put(UserRole.ADMIN, EnumSet.allOf(PolicyAction.class));
        fallbackPolicies.put(UserRole.DEVELOPER, EnumSet.of(
                PolicyAction.TOOL_INVOKE,
                PolicyAction.TRACE_VIEW
        ));
        fallbackPolicies.put(UserRole.REVIEWER, EnumSet.of(
                PolicyAction.REVIEW_DECIDE,
                PolicyAction.TRACE_VIEW,
                PolicyAction.AUDIT_VIEW
        ));
        fallbackPolicies.put(UserRole.VIEWER, EnumSet.of(PolicyAction.TRACE_VIEW));
    }

    public UserRole resolveRole(String demoRoleHeader) {
        if (demoRoleHeader == null || demoRoleHeader.isBlank()) {
            return UserRole.ADMIN;
        }
        try {
            return UserRole.valueOf(demoRoleHeader.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return UserRole.VIEWER;
        }
    }

    public void require(String demoRoleHeader, PolicyAction action) {
        var role = resolveRole(demoRoleHeader);
        if (!isAllowed(role, action)) {
            throw new PolicyAccessDeniedException(action, role, "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        }
    }

    public boolean isAllowed(UserRole role, PolicyAction action) {
        var storedActions = rolePolicyRepository.allowedActions(role).stream()
                .map(this::parseAction)
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toSet());
        var allowedActions = storedActions.isEmpty() ? fallbackPolicies.getOrDefault(role, Set.of()) : storedActions;
        return allowedActions.contains(action);
    }

    private java.util.Optional<PolicyAction> parseAction(String action) {
        try {
            return java.util.Optional.of(PolicyAction.valueOf(action));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }
}
