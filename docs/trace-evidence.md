# Trace Evidence

Every P1 Tool invocation creates a `ToolCallRecord`, multiple `TraceEvent` entries, and an `AuditLogEntry`.

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
