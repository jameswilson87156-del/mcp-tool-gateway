# Handoff

## Current State

MCP Tool Gateway P1 is a runnable demo project with:

- Spring Boot 3 backend under `backend`.
- Vue 3 + Vite + TypeScript frontend under `frontend`.
- AI-generated design references under `docs/design/concepts`.
- Real browser screenshot targets under `docs/images`.
- P2 Tool Registry and Human Review Center pages.
- P3 Trace Evidence governance center.
- P4 Prompt Studio / Resource Library workspace.
- P5A H2 + JdbcTemplate persistence layer.

## Visual Direction

Default entry is B2 Tool Call Workbench, not a traditional KPI dashboard. Keep the light Linear / Stripe developer-console style:

- Near-white background.
- Graphite text.
- Cool gray borders.
- Cobalt action blue.
- Green success, orange review, red blocked.
- Chinese-first with English technical terms such as MCP, Tool, Provider, Trace, Human Review, RBAC, JSON, Schema, Latency.

## Boundaries To Preserve

- MCP-style only.
- RBAC demo only.
- Tool execution is demo/sandbox only.
- `db.query.readonly` is SELECT-only.
- H2 persistence is local demo persistence, not a production database architecture.
- Concept images are not runtime product screenshots.

## P2 Notes

- `Tool Registry` is a real page with search, filters, registry rows, Tool Schema, parameters, permission scopes, version, and recent call summary.
- `Human Review` is a real page with review queue, request detail, Trace Evidence summary, Audit Log, and backend-backed approve/reject/request-changes actions.

## P3 Notes

- `Trace Evidence` is a real page with backend-backed Trace summaries and detail drilldown.
- `GET /api/traces` supports keyword, status, risk level, review requirement, and Tool name filtering in memory.
- `GET /api/traces/{traceId}` returns Tool Call context, Tool Schema summary, JSON evidence, Permission Result, review decision, TraceEvent timeline, and related AuditLogEntry records.
- The frontend falls back to centralized demo Trace data only when the backend is unavailable and labels that state.

## P4 Notes

- `Prompt Studio / Resource Library` is a real page shared by the `提示词工作室` and `资源中心` navigation entries.
- `GET /api/prompts`, `GET /api/prompts/{id}`, and `POST /api/prompts/{id}/render` support Prompt list, detail, and demo/sandbox render.
- Missing Prompt variables return structured validation errors instead of silently rendering.
- `GET /api/resources` and `GET /api/resources/{id}` support Resource list/detail, previews, linked Tools, related Prompts, and recent reference summaries.
- Prompt render records `AuditLogEntry`; Resource Library remains context resource management, not an enterprise knowledge graph.

## P5A Notes

- Core backend state now reads/writes through JdbcTemplate repositories under `backend/src/main/java/com/mcp/gateway/persistence`.
- H2 tables are created from `backend/src/main/resources/schema.sql`.
- `GatewayService` still owns business orchestration for Tool invoke, Human Review, Trace aggregation, Prompt render, and Resource detail.
- Startup uses seed-on-empty for P1-P4-compatible demo data.
- Existing API response shapes are preserved; list endpoints are not paginated yet.
- `.data/`, `*.mv.db`, and `*.trace.db` are ignored and must not be committed.
- Remaining work is Prompt/Resource edit-save flows, pagination, and stricter RBAC policy modeling.
