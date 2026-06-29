package com.mcp.gateway.persistence;

import com.mcp.gateway.model.ResourceDocument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ResourceRepository {
    private final JdbcTemplate jdbc;
    private final JsonCodec json;

    public ResourceRepository(JdbcTemplate jdbc, JsonCodec json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long count() {
        Long count = jdbc.queryForObject("select count(*) from resources", Long.class);
        return count == null ? 0 : count;
    }

    public List<ResourceDocument> findAll() {
        return jdbc.query("select * from resources order by updated_at desc", (rs, rowNum) -> new ResourceDocument(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getString("description"),
                rs.getString("status"),
                json.stringList(rs.getString("tags_json")),
                json.stringList(rs.getString("linked_tools_json")),
                rs.getString("updated_at"),
                rs.getInt("reference_count"),
                rs.getString("content_summary"),
                rs.getString("schema_preview"),
                rs.getString("markdown_preview"),
                json.stringList(rs.getString("related_prompts_json"))
        ));
    }

    public Optional<ResourceDocument> findById(String id) {
        return findAll().stream().filter(resource -> resource.id().equals(id)).findFirst();
    }

    public void save(ResourceDocument resource) {
        jdbc.update("""
                merge into resources key(id) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                resource.id(),
                resource.name(),
                resource.type(),
                resource.description(),
                resource.status(),
                json.write(resource.tags()),
                json.write(resource.linkedTools()),
                resource.updatedAt(),
                resource.referenceCount(),
                resource.contentSummary(),
                resource.schemaPreview(),
                resource.markdownPreview(),
                json.write(resource.relatedPrompts())
        );
    }
}
