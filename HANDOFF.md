# Handoff

## Current State

MCP Tool Gateway P1 is a runnable demo project with:

- Spring Boot 3 backend under `backend`.
- Vue 3 + Vite + TypeScript frontend under `frontend`.
- AI-generated design references under `docs/design/concepts`.
- Real browser screenshot targets under `docs/images`.

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
- Concept images are not runtime product screenshots.
