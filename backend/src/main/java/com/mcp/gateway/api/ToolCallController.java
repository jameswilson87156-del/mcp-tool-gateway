package com.mcp.gateway.api;

import com.mcp.gateway.model.ToolCallRecord;
import com.mcp.gateway.model.TraceEvent;
import com.mcp.gateway.service.GatewayService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/tool-calls")
public class ToolCallController {
    private final GatewayService gateway;

    public ToolCallController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @GetMapping
    public List<ToolCallRecord> listCalls() {
        return gateway.listCalls();
    }

    @GetMapping("/{id}")
    public ToolCallRecord getCall(@PathVariable String id) {
        return gateway.getCall(id);
    }

    @GetMapping("/{id}/trace")
    public List<TraceEvent> trace(@PathVariable String id) {
        return gateway.getTrace(id);
    }
}
