package com.mcp.gateway.persistence;

import com.mcp.gateway.model.UserRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class RolePolicyRepository {
    private final JdbcTemplate jdbc;

    public RolePolicyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long count() {
        Long count = jdbc.queryForObject("select count(*) from role_policies", Long.class);
        return count == null ? 0 : count;
    }

    public Set<String> allowedActions(UserRole role) {
        return jdbc.queryForList(
                        "select action from role_policies where role = ? and allowed = true",
                        String.class,
                        role.name()
                ).stream()
                .collect(Collectors.toSet());
    }

    public void save(UserRole role, String action, boolean allowed) {
        jdbc.update("""
                merge into role_policies key(id) values (?, ?, ?, ?)
                """,
                role.name() + ":" + action,
                role.name(),
                action,
                allowed
        );
    }
}
