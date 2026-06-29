package com.mcp.gateway.persistence;

import com.mcp.gateway.model.CallStatus;
import com.mcp.gateway.model.RiskLevel;
import com.mcp.gateway.model.ToolCallReview;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ReviewRepository {
    private final JdbcTemplate jdbc;

    public ReviewRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long count() {
        Long count = jdbc.queryForObject("select count(*) from tool_call_reviews", Long.class);
        return count == null ? 0 : count;
    }

    public List<ToolCallReview> findAll() {
        return jdbc.query("select * from tool_call_reviews order by created_at desc", (rs, rowNum) -> new ToolCallReview(
                rs.getString("id"),
                rs.getString("call_id"),
                rs.getString("tool_id"),
                RiskLevel.valueOf(rs.getString("risk_level")),
                CallStatus.valueOf(rs.getString("status")),
                rs.getString("reviewer"),
                rs.getString("decision"),
                rs.getString("comment"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ));
    }

    public Optional<ToolCallReview> findById(String id) {
        return findAll().stream().filter(review -> review.id().equals(id)).findFirst();
    }

    public void save(ToolCallReview review) {
        jdbc.update("""
                merge into tool_call_reviews key(id) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                review.id(),
                review.callId(),
                review.toolId(),
                review.riskLevel().name(),
                review.status().name(),
                review.reviewer(),
                review.decision(),
                review.comment(),
                review.createdAt(),
                review.updatedAt()
        );
    }
}
