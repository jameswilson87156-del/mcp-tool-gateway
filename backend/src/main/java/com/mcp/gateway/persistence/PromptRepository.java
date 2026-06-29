package com.mcp.gateway.persistence;

import com.mcp.gateway.model.PromptTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PromptRepository {
    private final JdbcTemplate jdbc;
    private final JsonCodec json;

    public PromptRepository(JdbcTemplate jdbc, JsonCodec json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long count() {
        Long count = jdbc.queryForObject("select count(*) from prompts", Long.class);
        return count == null ? 0 : count;
    }

    public List<PromptTemplate> findAll() {
        return jdbc.query("select * from prompts order by updated_at desc", (rs, rowNum) -> new PromptTemplate(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("version"),
                rs.getString("category"),
                rs.getString("status"),
                json.stringList(rs.getString("variables_json")),
                rs.getString("usage_scope"),
                json.stringList(rs.getString("related_tools_json")),
                rs.getString("updated_at"),
                rs.getInt("usage_count"),
                rs.getString("template_content")
        ));
    }

    public Optional<PromptTemplate> findById(String id) {
        return findAll().stream().filter(prompt -> prompt.id().equals(id)).findFirst();
    }

    public void save(PromptTemplate prompt) {
        jdbc.update("""
                merge into prompts key(id) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                prompt.id(),
                prompt.name(),
                prompt.description(),
                prompt.version(),
                prompt.category(),
                prompt.status(),
                json.write(prompt.variables()),
                prompt.usageScope(),
                json.write(prompt.relatedTools()),
                prompt.updatedAt(),
                prompt.usageCount(),
                prompt.templateContent()
        );
    }
}
