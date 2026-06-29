# API

Base URL: `http://localhost:8080/api`

## MCP-style JSON-RPC Adapter Demo

- `POST /mcp/rpc`

This HTTP endpoint is an MCP-style JSON-RPC adapter demo, not a complete MCP official protocol implementation or official MCP server. It supports only:

- `tools/list`
- `tools/call`
- `prompts/list`
- `resources/list`

Request:

```json
{
  "jsonrpc": "2.0",
  "id": "req_001",
  "method": "tools/call",
  "params": {
    "toolName": "weather.lookup",
    "arguments": {
      "city": "上海"
    },
    "role": "DEVELOPER"
  }
}
```

Success responses contain `jsonrpc`, the echoed `id`, and `result`. Error responses contain `jsonrpc`, the echoed `id`, and `error`.

`tools/call` reuses the existing Tool invoke, RBAC PolicyService demo, Human Review, Trace Evidence, and Audit Log flow. HIGH risk calls return `PENDING_REVIEW`; dangerous requests can return `BLOCKED` without sandbox execution.

JSON-RPC demo error codes:

| Code | Message |
| --- | --- |
| `-32600` | `Invalid Request` |
| `-32601` | `Method not found` |
| `-32602` | `Invalid params` |
| `-32000` | `Server error` |
| `-32003` | `Forbidden` |

The adapter currently supports HTTP POST only. It does not support stdio, SSE, full capabilities negotiation, or complete MCP compatibility. See [mcp-json-rpc-adapter.md](mcp-json-rpc-adapter.md).

P5C adds paginated filtering on top of the H2 + JdbcTemplate repository layer. Frontend adapters still tolerate legacy array responses, but the paginated list endpoints now return `PageResponse`.

`PageResponse<T>` shape:

```json
{
  "items": [],
  "page": 0,
  "size": 10,
  "total": 100,
  "totalPages": 10
}
```

`page` starts at `0`, default `size` is `10`, and maximum `size` is `50`.

P5D adds a demo `PolicyService` for sensitive API checks. It is local RBAC demo behavior, not production authentication or authorization.

## RBAC Policy Demo

Sensitive endpoints accept an optional `X-Demo-Role` header for local tests and demos only. Missing header resolves to the seeded admin demo user behavior. Valid demo values are:

- `ADMIN`
- `DEVELOPER`
- `REVIEWER`
- `VIEWER`

Current seeded demo matrix:

| Role | Allowed actions |
| --- | --- |
| `ADMIN` | all demo actions |
| `DEVELOPER` | `TOOL_INVOKE`, `TRACE_VIEW` |
| `REVIEWER` | `REVIEW_DECIDE`, `TRACE_VIEW`, `AUDIT_VIEW` |
| `VIEWER` | `TRACE_VIEW` |

Protected endpoints:

| Endpoint | Action |
| --- | --- |
| `POST /tools/{id}/invoke` | `TOOL_INVOKE` |
| `POST /prompts`, `PUT /prompts/{id}` | `PROMPT_EDIT` |
| `POST /prompts/{id}/publish`, `POST /prompts/{id}/archive` | `PROMPT_PUBLISH` |
| `POST /resources`, `PUT /resources/{id}` | `RESOURCE_EDIT` |
| `POST /resources/{id}/publish`, `POST /resources/{id}/archive` | `RESOURCE_PUBLISH` |
| `POST /reviews/{id}/approve`, `POST /reviews/{id}/reject`, `POST /reviews/{id}/request-changes` | `REVIEW_DECIDE` |
| `GET /traces`, `GET /traces/{traceId}` | `TRACE_VIEW` |
| `GET /audit-logs` | `AUDIT_VIEW` |

## Auth

- `POST /auth/login`
- `GET /auth/me`

## Tools

- `GET /tools`
- `GET /tools/{id}`
- `POST /tools/{id}/invoke`

Tool fields include:

- `id`
- `name`
- `description`
- `category`
- `provider`
- `version`
- `riskLevel`
- `status`
- `approvalRequired`
- `parameters`
- `schema`
- `permissionScopes`
- `recentCallCount`
- `updatedAt`

Invoke request:

```json
{
  "environment": "production",
  "requester": "admin",
  "parameters": {
    "customer_id": "CUST-202405-000123",
    "region": "APAC",
    "limit": 20
  }
}
```

## Tool Calls

- `GET /tool-calls`
- `GET /tool-calls/{id}`
- `GET /tool-calls/{id}/trace`

## Trace Evidence

- `GET /traces`
- `GET /traces/{traceId}`

`GET /traces?page=&size=&status=&riskLevel=&toolName=&reviewRequired=&keyword=` supports local demo filtering:

- `status`
- `riskLevel`
- `toolName`
- `reviewRequired`
- `keyword`

Trace summary fields include:

- `traceId`
- `callId`
- `toolName`
- `requester`
- `riskLevel`
- `status`
- `reviewStatus`
- `totalLatencyMs`
- `createdAt`
- `provider`
- `fallbackUsed`

Trace detail fields include:

- `traceId`
- `callId`
- `toolCall`
- `toolSchema`
- `inputJson`
- `outputJson`
- `status`
- `riskLevel`
- `permissionResult`
- `reviewRequired`
- `reviewDecision`
- `reviewer`
- `auditLogs`
- `traceEvents`
- `totalLatencyMs`
- `errorMessage`

## Reviews

- `GET /reviews`
- `POST /reviews/{id}/approve`
- `POST /reviews/{id}/reject`
- `POST /reviews/{id}/request-changes`

Review state flow:

`PENDING_REVIEW -> APPROVED / REJECTED / CHANGES_REQUESTED`

`GET /reviews?page=&size=&status=&riskLevel=&toolName=&keyword=` returns `PageResponse<ToolCallReview>`.

## Prompts

- `GET /prompts`
- `POST /prompts`
- `GET /prompts/{id}`
- `PUT /prompts/{id}`
- `POST /prompts/{id}/publish`
- `POST /prompts/{id}/archive`
- `POST /prompts/{id}/render`

`GET /prompts?page=&size=&keyword=&status=&category=` returns `PageResponse<PromptTemplate>`.

Prompt fields include:

- `id`
- `name`
- `description`
- `version`
- `category`
- `status`
- `variables`
- `usageScope`
- `relatedTools`
- `updatedAt`
- `usageCount`

Prompt detail includes:

- `prompt`
- `templateContent`
- `variables`
- `version`
- `status`
- `usageScope`
- `relatedTools`
- `recentUsage`
- `auditLogs`
- `warnings`

Create/update request:

```json
{
  "name": "review.summary.prompt",
  "description": "Summarize a Tool Call for Human Review",
  "category": "Governance",
  "templateContent": "Review Tool {{tool_name}} with input {{input}}.",
  "variables": ["tool_name", "input"],
  "usageScope": "Human Review demo",
  "relatedTools": ["db.query.readonly"],
  "status": "DRAFT"
}
```

`POST /prompts` creates a `DRAFT` Prompt when no status is supplied. Publish requires `name` and `templateContent`, then sets status to `ACTIVE`. Render performs local demo variable replacement only.

## Resources

- `GET /resources`
- `POST /resources`
- `GET /resources/{id}`
- `PUT /resources/{id}`
- `POST /resources/{id}/publish`
- `POST /resources/{id}/archive`

`GET /resources?page=&size=&keyword=&status=&type=` returns `PageResponse<ResourceDocument>`.

Create/update request:

```json
{
  "name": "policy-boundary-note",
  "type": "MARKDOWN",
  "description": "Local demo permission scope note",
  "contentSummary": "Records Tool, Prompt, and Resource demo governance boundaries.",
  "schemaPreview": "",
  "markdownPreview": "## Permission Boundary\nDemo and sandbox only.",
  "tags": ["governance", "RBAC"],
  "linkedTools": ["db.query.readonly"],
  "relatedPrompts": ["review.summary.prompt"],
  "status": "DRAFT"
}
```

`POST /resources` creates a `DRAFT` Resource when no status is supplied. `PUT /resources/{id}` updates the existing record and returns structured `404` when the id is unknown.

`POST /resources/{id}/publish` validates that `name`, `type`, and `contentSummary` are present, then sets status to `PUBLISHED`.

`POST /resources/{id}/archive` sets status to `ARCHIVED`. Create, update, publish, and archive write `AuditLogEntry` records.

Resource Library is context resource management for demo governance workflows, not an enterprise knowledge graph.

## Error Shape

Unknown ids return:

```json
{
  "error": "Prompt not found: missing-id",
  "type": "not_found"
}
```

Validation failures return:

```json
{
  "error": "Prompt name is required before publish",
  "type": "validation_error"
}
```


Policy failures return HTTP `403`:

```json
{
  "code": "FORBIDDEN",
  "message": "当前角色无权执行该操作",
  "action": "REVIEW_DECIDE",
  "role": "DEVELOPER",
  "requestId": "req_12ab34cd56ef"
}
```

## Dashboard / Audit

- `GET /dashboard/stats`
- `GET /audit-logs`

`GET /audit-logs?page=&size=&action=&actor=&target=&keyword=` returns `PageResponse<AuditLogEntry>`.

P5C filtering is intended for the local demo H2 data set. It is not full-text search, not Elasticsearch, and not production-grade search infrastructure.

## Demo Tools

- `weather.lookup`
- `ticket.search`
- `resume.analyze`
- `github.issue.search`
- `db.query.readonly`
- `crm.customer.search`
