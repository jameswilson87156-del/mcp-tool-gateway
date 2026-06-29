package com.mcp.gateway.persistence;

import com.mcp.gateway.model.CallStatus;
import com.mcp.gateway.model.TraceEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TraceRepository {
    private final JdbcTemplate jdbc;
    private final JsonCodec json;

    public TraceRepository(JdbcTemplate jdbc, JsonCodec json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long count() {
        Long count = jdbc.queryForObject("select count(*) from trace_events", Long.class);
        return count == null ? 0 : count;
    }

    public List<TraceEvent> findByCallId(String callId) {
        return jdbc.query("select * from trace_events where call_id = ? order by timestamp, id", (rs, rowNum) -> new TraceEvent(
                rs.getString("id"),
                rs.getString("call_id"),
                rs.getString("step"),
                CallStatus.valueOf(rs.getString("status")),
                rs.getString("message"),
                rs.getString("latency"),
                json.map(rs.getString("evidence_json")),
                rs.getTimestamp("timestamp").toInstant()
        ), callId);
    }

    public void save(TraceEvent event) {
        jdbc.update("""
                merge into trace_events key(id) values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                event.id(),
                event.callId(),
                event.step(),
                event.status().name(),
                event.message(),
                event.latency(),
                json.write(event.evidence()),
                event.timestamp()
        );
    }

    public void saveAll(List<TraceEvent> events) {
        events.forEach(this::save);
    }
}
