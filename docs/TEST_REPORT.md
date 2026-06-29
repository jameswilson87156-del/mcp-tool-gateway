# Test Report

This file is updated after local verification.

## Commands

- Backend: `cd backend && mvn test`
- Frontend build: `cd frontend && npm run build`
- Compose validation: `docker compose config`
- Docker images: `docker compose build` when a Docker CLI and daemon are available
- Screenshots: `cd frontend && npm run screenshots`
- Root: `git diff --check`
- Root: `git status --short`

## Latest Result

Date: 2026-06-29

- `mvn test`: passed. 60 tests, 0 failures, 0 errors. P7C strengthened boundary coverage across JSON-RPC adapter errors and id echoing, PolicyService role/action decisions, Repository/JsonCodec round trips, Trace pagination, Audit keyword filtering, and existing Prompt/Resource state flows. These tests still use local H2 and do not call real external tools.
- `mvn -B -DskipTests package`: passed. The executable `mcp-tool-gateway-0.1.0.jar` path used by the backend Dockerfile was generated successfully.
- `npm run build`: passed. `vue-tsc --noEmit` and `vite build` completed.
- GitHub Actions P7A run `28358408399`: passed. Backend tests and frontend build jobs both completed successfully before P7B work started.
- `docker compose config`: not executed because the local machine does not have the `docker` command installed or available on `PATH` (`CommandNotFoundException`). The Compose file received a manual structure review; this is not reported as a successful Docker validation.
- `docker compose build`: not executed for the same reason. No Docker build result is claimed.
- `npm run screenshots`: not rerun for P7C because frontend UI and README screenshot paths did not change. The previous run passed: the script started the Spring Boot backend and Vite frontend, then captured real browser screenshots listed below.
- `git diff --check`: passed. Git reported Windows line-ending normalization warnings only.
- Security scan: passed. No API keys, `sk-` tokens, secrets, private keys, `.env`, tracked build outputs, H2 database files, `node_modules`, `dist`, `target`, or log files were found in the index.
- Screenshots:
  - `docs/images/mcp-tool-workbench.png`
  - `docs/images/tool-registry.png`
  - `docs/images/human-review-center.png`
  - `docs/images/trace-evidence.png`
  - `docs/images/audit-log.png`
  - `docs/images/prompt-resource.png`
  - `docs/images/large/mcp-tool-workbench.png`
  - `docs/images/large/tool-registry.png`
  - `docs/images/large/human-review-center.png`
  - `docs/images/large/trace-evidence.png`
  - `docs/images/large/audit-log.png`
  - `docs/images/large/prompt-resource.png`

## Visual Check

The captured page follows the B2 Tool Call Workbench direction:

- Light Linear / Stripe style developer console.
- Chinese-first copy with MCP, Tool, Provider, Human Review, Trace, JSON, Schema, Latency technical terms.
- No dark blue/black dashboard.
- No KPI-card stack as the primary interface.
- The README screenshot path uses `docs/images`, not `docs/design/concepts`.
- P2 Tool Registry and Human Review Center are implemented as real Vue/CSS pages, not concept-image backgrounds.
- P3 Trace Evidence is implemented as a real Vue/CSS governance evidence center with filters, timeline drilldown, step evidence, JSON evidence, and Audit Evidence.
- P4 Prompt Studio / Resource Library is implemented as a real Vue/CSS developer configuration workspace with backend-backed Prompt render, Resource detail, Tool Binding, and usage/audit evidence.
- P5A keeps the P1-P4 frontend shape unchanged while moving backend state to H2 + JdbcTemplate repositories.
- P5B adds Prompt / Resource create, update, draft, publish, archive, validation feedback, and Audit Log evidence while preserving the existing developer-workbench visual system.
- P5C adds PageResponse pagination and local demo filtering for Trace, Review, Audit Log, Prompt, and Resource lists, plus a lightweight real Audit Log page.
- P5D adds PolicyService-backed demo RBAC checks and structured `403` responses for sensitive actions without changing the visual direction.
- P7A adds CI, local demo containers, and OpenAPI documentation without changing frontend pages or the MCP-style/demo boundaries.
- P7B adds an HTTP-only MCP-style JSON-RPC adapter demo without changing frontend pages. It does not claim complete MCP compatibility, official transports, or production Tool execution.
- P7C adds boundary-focused tests only. It does not add business features, real external execution, production authorization, or complete MCP official protocol compatibility tests.
