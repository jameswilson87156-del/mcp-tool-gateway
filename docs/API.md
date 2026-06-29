# API

Base URL: `http://localhost:8080/api`

P5A keeps the existing response shapes unchanged while moving core data behind an H2 + JdbcTemplate repository layer. List endpoints still return arrays in this phase; pagination is intentionally deferred.

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
- `GET /prompts/{id}`
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
- `GET /resources/{id}`

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

Resource Library is context resource management for demo governance workflows, not an enterprise knowledge graph.

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
