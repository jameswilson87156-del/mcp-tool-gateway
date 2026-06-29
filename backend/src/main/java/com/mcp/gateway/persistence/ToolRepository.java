package com.mcp.gateway.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mcp.gateway.model.RiskLevel;
import com.mcp.gateway.model.ToolDefinition;
import com.mcp.gateway.model.ToolParameterSchema;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ToolRepository {
    private final JdbcTemplate jdbc;
    private final JsonCodec json;

    public ToolRepository(JdbcTemplate jdbc, JsonCodec json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long count() {
        Long count = jdbc.queryForObject("select count(*) from tools", Long.class);
        return count == null ? 0 : count;
    }

    public List<ToolDefinition> findAll() {
        return jdbc.query("select * from tools order by name", (rs, rowNum) -> new ToolDefinition(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getString("provider"),
                rs.getString("version"),
                RiskLevel.valueOf(rs.getString("risk_level")),
                rs.getString("status"),
                rs.getBoolean("approval_required"),
                json.list(rs.getString("parameters_json"), new TypeReference<>() {}),
                json.map(rs.getString("schema_json")),
                json.stringList(rs.getString("permission_scopes_json")),
                0,
                rs.getString("updated_at")
        ));
    }

    public Optional<ToolDefinition> findById(String id) {
        return findAll().stream().filter(tool -> tool.id().equals(id)).findFirst();
    }

    public void save(ToolDefinition tool) {
        jdbc.update("""
                merge into tools key(id) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                tool.id(),
                tool.name(),
                tool.description(),
                tool.category(),
                tool.provider(),
                tool.version(),
                tool.riskLevel().name(),
                tool.status(),
                tool.approvalRequired(),
                json.write(tool.parameters()),
                json.write(tool.schema()),
                json.write(tool.permissionScopes()),
                tool.updatedAt()
        );
    }
}
