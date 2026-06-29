package com.mcp.gateway.api;

import com.mcp.gateway.model.McpRpcRequest;
import com.mcp.gateway.model.McpRpcResponse;
import com.mcp.gateway.service.McpRpcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/mcp")
@Tag(
        name = "MCP-style JSON-RPC Adapter",
        description = "MCP-style JSON-RPC adapter demo. This is not a full MCP protocol implementation."
)
public class McpRpcController {
    private final McpRpcService rpcService;

    public McpRpcController(McpRpcService rpcService) {
        this.rpcService = rpcService;
    }

    @PostMapping("/rpc")
    @Operation(
            summary = "Invoke an MCP-style JSON-RPC demo method",
            description = "Supports tools/list, tools/call, prompts/list, and resources/list over HTTP POST only."
    )
    public McpRpcResponse rpc(@RequestBody(required = false) McpRpcRequest request) {
        return rpcService.handle(request);
    }
}
