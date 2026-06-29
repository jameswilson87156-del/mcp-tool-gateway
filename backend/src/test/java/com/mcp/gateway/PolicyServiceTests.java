package com.mcp.gateway;

import com.mcp.gateway.model.UserRole;
import com.mcp.gateway.persistence.RolePolicyRepository;
import com.mcp.gateway.security.PolicyAccessDeniedException;
import com.mcp.gateway.security.PolicyAction;
import com.mcp.gateway.security.PolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PolicyServiceTests {
    @Autowired
    private PolicyService policy;

    @Autowired
    private RolePolicyRepository rolePolicyRepository;

    @Test
    void adminAllowsEveryDemoAction() {
        for (var action : PolicyAction.values()) {
            assertThat(policy.isAllowed(UserRole.ADMIN, action)).isTrue();
        }
    }

    @Test
    void developerCannotDecideReviews() {
        assertThat(policy.isAllowed(UserRole.DEVELOPER, PolicyAction.REVIEW_DECIDE)).isFalse();
        assertThatThrownBy(() -> policy.require("DEVELOPER", PolicyAction.REVIEW_DECIDE))
                .isInstanceOf(PolicyAccessDeniedException.class)
                .extracting("role")
                .isEqualTo(UserRole.DEVELOPER);
    }

    @Test
    void reviewerCanViewAuditLogs() {
        assertThat(policy.isAllowed(UserRole.REVIEWER, PolicyAction.AUDIT_VIEW)).isTrue();
    }

    @Test
    void viewerOnlyAllowsTraceView() {
        assertThat(policy.isAllowed(UserRole.VIEWER, PolicyAction.TRACE_VIEW)).isTrue();
        for (var action : PolicyAction.values()) {
            if (action != PolicyAction.TRACE_VIEW) {
                assertThat(policy.isAllowed(UserRole.VIEWER, action)).isFalse();
            }
        }
    }

    @Test
    void unknownRoleHeaderResolvesToViewerAndDeniesToolInvoke() {
        assertThat(policy.resolveRole("unknown-role")).isEqualTo(UserRole.VIEWER);
        assertThatThrownBy(() -> policy.require("unknown-role", PolicyAction.TOOL_INVOKE))
                .isInstanceOf(PolicyAccessDeniedException.class)
                .extracting("role")
                .isEqualTo(UserRole.VIEWER);
    }

    @Test
    void unknownStoredActionDoesNotGrantPermission() {
        rolePolicyRepository.save(UserRole.DEVELOPER, "UNKNOWN_ACTION", true);

        assertThat(policy.isAllowed(UserRole.DEVELOPER, PolicyAction.SETTINGS_MANAGE)).isFalse();
    }
}
