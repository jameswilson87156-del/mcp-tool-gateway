# API

Base URL: `http://localhost:8080/api`

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

## Prompts / Resources / Dashboard

- `GET /prompts`
- `GET /resources`
- `GET /dashboard/stats`
- `GET /audit-logs`

## Demo Tools

- `weather.lookup`
- `ticket.search`
- `resume.analyze`
- `github.issue.search`
- `db.query.readonly`
- `crm.customer.search`
