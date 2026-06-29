package com.mcp.gateway.persistence;

import com.mcp.gateway.model.CallStatus;
import com.mcp.gateway.model.RiskLevel;
import com.mcp.gateway.model.ToolCallRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ToolCallRepository {
    private final JdbcTemplate jdbc;
    private final JsonCodec json;

    public ToolCallRepository(JdbcTemplate jdbc, JsonCodec json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long count() {
        Long count = jdbc.queryForObject("select count(*) from tool_calls", Long.class);
        return count == null ? 0 : count;
    }

    public int countByToolId(String toolId) {
        Integer count = jdbc.queryForObject("select count(*) from tool_calls where tool_id = ?", Integer.class, toolId);
        return count == null ? 0 : count;
    }

    public List<ToolCallRecord> findAll() {
        return jdbc.query("select * from tool_calls order by created_at desc", (rs, rowNum) -> new ToolCallRecord(
                rs.getString("id"),
                rs.getString("tool_id"),
                rs.getString("tool_name"),
                rs.getString("requester"),
                rs.getString("provider"),
                rs.getString("environment"),
                RiskLevel.valueOf(rs.getString("risk_level")),
                CallStatus.valueOf(rs.getString("status")),
                json.map(rs.getString("request_json")),
                json.map(rs.getString("response_json")),
                rs.getString("review_id"),
                rs.getString("latency"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ));
    }

    public Optional<ToolCallRecord> findById(String id) {
        return findAll().stream().filter(call -> call.id().equals(id)).findFirst();
    }

    public void save(ToolCallRecord call) {
        jdbc.update("""
                merge into tool_calls key(id) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                call.id(),
                call.toolId(),
                call.toolName(),
                call.requester(),
                call.provider(),
                call.environment(),
                call.riskLevel().name(),
                call.status().name(),
                json.write(call.request()),
                json.write(call.response()),
                call.reviewId(),
                call.latency(),
                call.createdAt(),
                call.updatedAt()
        );
    }
}
