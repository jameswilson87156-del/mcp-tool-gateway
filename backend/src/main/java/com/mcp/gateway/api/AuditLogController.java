package com.mcp.gateway.api;

import com.mcp.gateway.model.AuditLogEntry;
import com.mcp.gateway.service.GatewayService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/audit-logs")
public class AuditLogController {
    private final GatewayService gateway;

    public AuditLogController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @GetMapping
    public List<AuditLogEntry> auditLogs() {
        return gateway.listAuditLogs();
    }
}
