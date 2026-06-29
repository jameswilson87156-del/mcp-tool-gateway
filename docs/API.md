# API

Base URL: `http://localhost:8080/api`

P5B keeps the existing read response shapes compatible and adds Prompt / Resource write operations on top of the H2 + JdbcTemplate repository layer. List endpoints still return arrays in this phase; pagination is intentionally deferred.

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

`GET /traces` supports in-memory filtering:

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

## Prompts

- `GET /prompts`
- `POST /prompts`
- `GET /prompts/{id}`
- `PUT /prompts/{id}`
- `POST /prompts/{id}/publish`
- `POST /prompts/{id}/archive`
- `POST /prompts/{id}/render`

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
  "name": "高风险工单摘要 Prompt",
  "description": "用于 Human Review 前整理 Tool Call 证据",
  "category": "review",
  "templateContent": "请根据 {{trace_id}} 总结风险原因。",
  "variables": ["trace_id"],
  "usageScope": "Human Review",
  "relatedTools": ["ticket.search"],
  "status": "DRAFT"
}
```

`POST /prompts` creates a `DRAFT` Prompt when no status is supplied. `PUT /prompts/{id}` updates the existing record and returns structured `404` when the id is unknown.

`POST /prompts/{id}/publish` validates that `name` and `templateContent` are present. Declared variables that are not found as `{{variable}}` placeholders are returned as `warnings`; publish still returns `PromptDetail` and sets status to `ACTIVE`.

`POST /prompts/{id}/archive` sets status to `ARCHIVED`. Create, update, publish, archive, and render write `AuditLogEntry` records.

Render request:

```json
{
  "requester": "admin",
  "variables": {
    "customer_id": "CUST-202405-000123",
    "policy_doc": "policy-docs",
    "locale": "zh-CN"
  }
}
```

Render response returns `valid`, `validationErrors`, `renderedPrompt`, and `renderedAt`. Missing variables return a structured validation result. Render is demo/sandbox behavior and records an `AuditLogEntry`.

## Resources

- `GET /resources`
- `POST /resources`
- `GET /resources/{id}`
- `PUT /resources/{id}`
- `POST /resources/{id}/publish`
- `POST /resources/{id}/archive`

Resource fields include:

- `id`
- `name`
- `type`
- `description`
- `status`
- `tags`
- `linkedTools`
- `updatedAt`
- `referenceCount`

Resource detail includes:

- `resource`
- `contentSummary`
- `schemaPreview`
- `markdownPreview`
- `linkedTools`
- `relatedPrompts`
- `recentReferences`
- `auditLogs`

Create/update request:

```json
{
  "name": "权限策略说明",
  "type": "Markdown",
  "description": "本地 demo 权限范围说明",
  "contentSummary": "记录 Tool、Prompt、Resource 的演示权限边界。",
  "schemaPreview": "",
  "markdownPreview": "## 权限边界\n仅用于 demo/sandbox。",
  "tags": ["governance", "RBAC"],
  "linkedTools": ["db.query.readonly"],
  "relatedPrompts": ["prompt-review-summary"],
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

## Dashboard / Audit

- `GET /dashboard/stats`
- `GET /audit-logs`

## Demo Tools

- `weather.lookup`
- `ticket.search`
- `resume.analyze`
- `github.issue.search`
- `db.query.readonly`
- `crm.customer.search`
