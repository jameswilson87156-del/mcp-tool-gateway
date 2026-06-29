package com.mcp.gateway.api;

import com.mcp.gateway.model.AuditLogEntry;
import com.mcp.gateway.model.PageResponse;
import com.mcp.gateway.service.GatewayService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/audit-logs")
public class AuditLogController {
    private final GatewayService gateway;

    public AuditLogController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @GetMapping
    public PageResponse<AuditLogEntry> auditLogs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String target,
            @RequestParam(required = false) String keyword
    ) {
        return gateway.listAuditLogs(page, size, action, actor, target, keyword);
    }
}
