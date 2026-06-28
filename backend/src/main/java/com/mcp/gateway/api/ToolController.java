package com.mcp.gateway.api;

import com.mcp.gateway.model.ToolCallRecord;
import com.mcp.gateway.model.ToolDefinition;
import com.mcp.gateway.service.GatewayService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/tools")
public class ToolController {
    private final GatewayService gateway;

    public ToolController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @GetMapping
    public List<ToolDefinition> listTools() {
        return gateway.listTools();
    }

    @GetMapping("/{id}")
    public ToolDefinition getTool(@PathVariable String id) {
        return gateway.getTool(id);
    }

    @PostMapping("/{id}/invoke")
    public ToolCallRecord invoke(@PathVariable String id, @RequestBody InvokeRequest request) {
        return gateway.invoke(id, request);
    }
}
