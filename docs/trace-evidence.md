# Trace Evidence

Every Tool invocation creates a `ToolCallRecord`, multiple `TraceEvent` entries, and an `AuditLogEntry`.

P3 adds a real Trace Evidence governance center. The page is built with Vue DOM/CSS, supports filtering and drilldown, and is not rendered from the AI-generated concept image.

Trace steps:

1. Request
2. Tool Select
3. Schema Check
4. Permission Check
5. Human Review
6. Execute
7. Audit Log

For `LOW` and `MEDIUM` risk tools, Human Review records `无需审批` and the sandbox demo executes immediately.

For `HIGH` risk tools, the call enters `PENDING_REVIEW`; execution waits for an explicit approve/reject/request-changes action.

For blocked SQL on `db.query.readonly`, `local-rule fallback` blocks the request and no sandbox execution runs.

## P3 Governance Center

The Trace Evidence page supports:

- Filtering by keyword, risk level, status, review requirement, and Tool name.
- Selecting a Trace from the evidence list.
- Drilling into timeline steps from Request through Audit Log.
- Inspecting step metadata, Tool Schema summary, Input JSON, Output JSON, Permission Result, Risk Explanation, reviewer decision, and error messages.
- Viewing related Audit Evidence for the selected Tool Call.

Trace center endpoints:

- `GET /api/traces`
- `GET /api/traces/{traceId}`

`GET /api/traces` accepts these optional query parameters:

- `status`
- `riskLevel`
- `toolName`
- `reviewRequired`
- `keyword`

The frontend uses backend data first. If the backend is unavailable, the page falls back to the centralized demo data in `frontend/src/data/demo.ts` and clearly marks the fallback source.

## P2 Review Actions

Human Review Center calls the backend review endpoints directly:

- `POST /api/reviews/{id}/approve`
- `POST /api/reviews/{id}/reject`
- `POST /api/reviews/{id}/request-changes`

Each decision updates:

- `ToolCallReview`
- `ToolCallRecord`
- `TraceEvent`
- `AuditLogEntry`

`CHANGES_REQUESTED` means a reviewer asked for narrower scope, extra context, or safer parameters before approving a high-risk Tool Call.
