# MCP-style JSON-RPC Adapter Demo

P7B adds a small JSON-RPC 2.0 envelope adapter over the existing MCP Tool Gateway demo workflows.

This is an **MCP-style JSON-RPC adapter demo**. It is not a complete MCP official protocol implementation, not a fully MCP-compatible server, and not an official MCP server.

## Endpoint

Current transport support is limited to:

`POST /api/mcp/rpc`

Example:

```json
{
  "jsonrpc": "2.0",
  "id": "req_001",
  "method": "tools/list",
  "params": {}
}
```

Success envelope:

```json
{
  "jsonrpc": "2.0",
  "id": "req_001",
  "result": {}
}
```

Error envelope:

```json
{
  "jsonrpc": "2.0",
  "id": "req_001",
  "error": {
    "code": -32601,
    "message": "Method not found",
    "data": {}
  }
}
```

## Demo Methods

### `tools/list`

Returns summaries from the current Tool Registry:

- `name`
- `description`
- `schema`
- `riskLevel`
- `provider`
- `approvalRequired`

### `tools/call`

Required params:

```json
{
  "toolName": "weather.lookup",
  "arguments": {
    "city": "上海"
  },
  "role": "DEVELOPER"
}
```

`role` is optional and only feeds the existing RBAC PolicyService demo. When omitted, it keeps the existing local demo default behavior. It is not authentication and can be forged by a caller.

The adapter resolves the current ToolDefinition, performs the existing `TOOL_INVOKE` PolicyService check, then calls `GatewayService.invoke`. The result includes:

- `callId`
- `status`
- `reviewRequired`
- `traceId`
- `result` for SUCCESS or BLOCKED calls
- `pendingReview` for calls waiting on Human Review

LOW / MEDIUM risk calls follow the existing sandbox execution. HIGH risk calls return `PENDING_REVIEW` and are not executed until the existing review workflow approves them. BLOCKED calls do not execute.

Because `GatewayService.invoke` remains the single orchestration path, JSON-RPC calls continue to create the same Tool Call, Human Review, Trace Evidence, and Audit Log records as the REST invoke endpoint.

### `prompts/list`

Returns Prompt summaries with `name`, `description`, `version`, `status`, and `variables`.

### `resources/list`

Returns Resource summaries with `name`, `type`, `status`, `description`, and `linkedTools`.

## Error Mapping

| Code | Message | Meaning |
| --- | --- | --- |
| `-32600` | `Invalid Request` | `jsonrpc` is not `2.0`, or method is missing |
| `-32601` | `Method not found` | method is not one of the four demo methods |
| `-32602` | `Invalid params` | required `tools/call` fields are missing or invalid |
| `-32000` | `Server error` | existing demo business processing raised an unexpected runtime error |
| `-32003` | `Forbidden` | PolicyService denied `TOOL_INVOKE` for the supplied demo role |

The adapter does not return Java exception stack traces. Policy errors include demo action, role, and requestId metadata in `error.data`.

## Explicit Non-Goals

The current adapter does not support:

- stdio transport.
- SSE transport.
- full MCP capabilities negotiation.
- MCP lifecycle or session management.
- complete official MCP method and schema compatibility.
- real external Tool execution.
- production authentication, authorization, tenant isolation, or trusted role claims.

Swagger UI documents this endpoint as a demo API. OpenAPI generation does not make it a complete MCP protocol implementation.

## Roadmap Boundary

A future, separate productionization track could replace this adapter with a real MCP JSON-RPC compatibility layer, standard transports, capability negotiation, official interoperability tests, and isolated external Tool adapters. Those items are roadmap only and are not implemented in P7B.
