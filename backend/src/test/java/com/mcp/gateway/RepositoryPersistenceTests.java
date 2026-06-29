package com.mcp.gateway;

import com.mcp.gateway.model.AuditLogEntry;
import com.mcp.gateway.model.RiskLevel;
import com.mcp.gateway.model.ToolDefinition;
import com.mcp.gateway.model.ToolParameterSchema;
import com.mcp.gateway.persistence.AuditLogRepository;
import com.mcp.gateway.persistence.JsonCodec;
import com.mcp.gateway.persistence.ToolRepository;
import com.mcp.gateway.service.GatewayService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RepositoryPersistenceTests {
    @Autowired
    private JsonCodec json;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private GatewayService gateway;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void cleanRepositoryTestData() {
        jdbc.update("delete from tools where id = ?", "test.echo.readonly");
        jdbc.update("delete from audit_logs where id = ?", "audit_test_json");
    }

    @Test
    void jsonCodecRoundTripsMapAndListValues() {
        var mapJson = json.write(Map.of("nested", Map.of("enabled", true), "count", 3));
        var listJson = json.write(List.of("tool:read", "trace:read"));

        assertThat(json.map(mapJson))
                .containsEntry("count", 3)
                .containsKey("nested");
        assertThat(json.stringList(listJson)).containsExactly("tool:read", "trace:read");
    }

    @Test
    void toolRepositorySavesAndReadsById() {
        var tool = new ToolDefinition(
                "test.echo.readonly",
                "test.echo.readonly",
                "Repository persistence test Tool",
                "Testing",
                "Local Test",
                "v0.0.1",
                RiskLevel.LOW,
                "ACTIVE",
                false,
                List.of(new ToolParameterSchema("message", "string", true, "Echo message", "hello")),
                Map.of("type", "object", "required", List.of("message")),
                List.of("tool:test"),
                0,
                Instant.now().toString()
        );

        toolRepository.save(tool);

        assertThat(toolRepository.findById("test.echo.readonly")).isPresent();
        assertThat(toolRepository.findById("test.echo.readonly").orElseThrow().schema())
                .containsEntry("type", "object");
    }

    @Test
    void seedIfEmptyDoesNotDuplicateMainDemoData() {
        var toolCount = toolRepository.count();

        gateway.seedIfEmpty();

        assertThat(toolRepository.count()).isEqualTo(toolCount);
    }

    @Test
    void auditLogMetadataJsonRoundTripsThroughRepository() {
        var entry = new AuditLogEntry(
                "audit_test_json",
                "tester",
                "test.metadata",
                "ToolDefinition",
                "test.echo.readonly",
                Map.of("flags", List.of("json", "audit"), "nested", Map.of("ok", true)),
                Instant.now()
        );

        auditLogRepository.save(entry);

        var saved = auditLogRepository.findAll().stream()
                .filter(log -> log.id().equals("audit_test_json"))
                .findFirst()
                .orElseThrow();
        var flags = ((List<?>) saved.metadata().get("flags")).stream().map(String::valueOf).toList();

        assertThat(saved.metadata()).containsKey("nested");
        assertThat(flags).contains("json", "audit");
    }
}
