package com.mcp.gateway.persistence;

import com.mcp.gateway.model.AuditLogEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AuditLogRepository {
    private final JdbcTemplate jdbc;
    private final JsonCodec json;

    public AuditLogRepository(JdbcTemplate jdbc, JsonCodec json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long count() {
        Long count = jdbc.queryForObject("select count(*) from audit_logs", Long.class);
        return count == null ? 0 : count;
    }

    public List<AuditLogEntry> findAll() {
        return jdbc.query("select * from audit_logs order by timestamp desc", (rs, rowNum) -> new AuditLogEntry(
                rs.getString("id"),
                rs.getString("actor"),
                rs.getString("action"),
                rs.getString("target_type"),
                rs.getString("target_id"),
                json.map(rs.getString("metadata_json")),
                rs.getTimestamp("timestamp").toInstant()
        ));
    }

    public void save(AuditLogEntry entry) {
        jdbc.update("""
                merge into audit_logs key(id) values (?, ?, ?, ?, ?, ?, ?)
                """,
                entry.id(),
                entry.actor(),
                entry.action(),
                entry.targetType(),
                entry.targetId(),
                json.write(entry.metadata()),
                entry.timestamp()
        );
    }
}
