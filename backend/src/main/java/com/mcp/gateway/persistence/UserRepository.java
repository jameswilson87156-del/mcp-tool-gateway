package com.mcp.gateway.persistence;

import com.mcp.gateway.model.UserAccount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;
    private final JsonCodec json;

    public UserRepository(JdbcTemplate jdbc, JsonCodec json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long count() {
        Long count = jdbc.queryForObject("select count(*) from demo_users", Long.class);
        return count == null ? 0 : count;
    }

    public void save(UserAccount user) {
        jdbc.update("""
                merge into demo_users key(id) values (?, ?, ?, ?, ?)
                """,
                user.id(),
                user.username(),
                user.displayName(),
                user.role().name(),
                json.write(user.permissionScopes())
        );
    }
}
