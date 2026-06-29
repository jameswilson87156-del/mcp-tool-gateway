# RBAC Policy Demo

P5D introduces `PolicyService` as a small backend policy boundary for sensitive MCP Tool Gateway demo actions.

This is demo RBAC. It is not a production-grade permission system, OAuth integration, SSO integration, JWT implementation, tenant model, account system, or external identity provider integration.

## Roles

- `ADMIN`
- `DEVELOPER`
- `REVIEWER`
- `VIEWER`

## Actions

- `TOOL_INVOKE`
- `TOOL_MANAGE`
- `PROMPT_EDIT`
- `PROMPT_PUBLISH`
- `RESOURCE_EDIT`
- `RESOURCE_PUBLISH`
- `REVIEW_DECIDE`
- `TRACE_VIEW`
- `AUDIT_VIEW`
- `SETTINGS_MANAGE`

## Current Demo Matrix

| Role | Allowed actions |
| --- | --- |
| `ADMIN` | all demo actions |
| `DEVELOPER` | `TOOL_INVOKE`, `TRACE_VIEW` |
| `REVIEWER` | `REVIEW_DECIDE`, `TRACE_VIEW`, `AUDIT_VIEW` |
| `VIEWER` | `TRACE_VIEW` |

The source of truth is the seeded local H2 `role_policies` table. `PolicyService` keeps a fallback matrix with the same shape for empty local demo data.

## Protected Endpoints

| Endpoint | Required action |
| --- | --- |
| `POST /api/tools/{id}/invoke` | `TOOL_INVOKE` |
| `POST /api/prompts` | `PROMPT_EDIT` |
| `PUT /api/prompts/{id}` | `PROMPT_EDIT` |
| `POST /api/prompts/{id}/publish` | `PROMPT_PUBLISH` |
| `POST /api/prompts/{id}/archive` | `PROMPT_PUBLISH` |
| `POST /api/resources` | `RESOURCE_EDIT` |
| `PUT /api/resources/{id}` | `RESOURCE_EDIT` |
| `POST /api/resources/{id}/publish` | `RESOURCE_PUBLISH` |
| `POST /api/resources/{id}/archive` | `RESOURCE_PUBLISH` |
| `POST /api/reviews/{id}/approve` | `REVIEW_DECIDE` |
| `POST /api/reviews/{id}/reject` | `REVIEW_DECIDE` |
| `POST /api/reviews/{id}/request-changes` | `REVIEW_DECIDE` |
| `GET /api/traces` | `TRACE_VIEW` |
| `GET /api/traces/{traceId}` | `TRACE_VIEW` |
| `GET /api/audit-logs` | `AUDIT_VIEW` |

## Structured 403

Unauthorized policy checks return HTTP `403` without an exception stack:

```json
{
  "code": "FORBIDDEN",
  "message": "当前角色无权执行该操作",
  "action": "REVIEW_DECIDE",
  "role": "DEVELOPER",
  "requestId": "req_12ab34cd56ef"
}
```

## Demo Role Header

Tests and local demos may send `X-Demo-Role: ADMIN`, `DEVELOPER`, `REVIEWER`, or `VIEWER`.

`X-Demo-Role` is only a demo/testing helper. It is not a production authentication header and must not be used as a real trust boundary.

## Why No OAuth / SSO / JWT

P5D deliberately keeps scope small. The project is still an MCP-style demo gateway with local H2 data and sandbox Tool execution. Adding OAuth, SSO, JWT, user lifecycle, tenant isolation, and production policy administration would be a separate security architecture project and would change the current demo boundary.

## Production Upgrade Path

A production path would add a real identity provider, signed sessions or tokens, tenant-aware policy storage, server-side role assignment, audit-grade actor identity, defense-in-depth checks, and integration tests that do not rely on a demo header.