# Prompt Studio / Resource Library

P4 added a real Prompt / Resource governance workspace. P5B upgrades it from read + render into an editable governance workbench with create, update, draft, publish, archive, validation feedback, and Audit Log evidence.

The page is built with Vue DOM/CSS and follows the same light Linear / Stripe developer-console direction as the Workbench, Tool Registry, Human Review Center, and Trace Evidence pages.

## Pages

The `提示词工作室` navigation entry opens the Prompt tab. The `资源中心` navigation entry opens the same workspace on the Resource tab.

Workspace tabs:

- Prompt 模板
- Resource 资源
- Tool Binding
- Recent Usage

P5B keeps editing inside right-side drawers and action bars so the page still feels like a developer workbench, not a plain CMS form page.

## Backend Endpoints

Prompt endpoints:

- `GET /api/prompts`
- `POST /api/prompts`
- `GET /api/prompts/{id}`
- `PUT /api/prompts/{id}`
- `POST /api/prompts/{id}/publish`
- `POST /api/prompts/{id}/archive`
- `POST /api/prompts/{id}/render`

Resource endpoints:

- `GET /api/resources`
- `POST /api/resources`
- `GET /api/resources/{id}`
- `PUT /api/resources/{id}`
- `POST /api/resources/{id}/publish`
- `POST /api/resources/{id}/archive`

Write operations persist through the H2 repository layer and record `AuditLogEntry` rows. Unknown ids return structured `404` responses, and publish validation returns structured validation errors.

## Editing Flow

Prompt Studio supports:

- New Prompt.
- Edit current Prompt.
- Save as `DRAFT`.
- Publish to `ACTIVE`.
- Archive to `ARCHIVED`.
- Render with structured missing-variable validation.

Resource Library supports:

- New Resource.
- Edit current Resource.
- Save as `DRAFT`.
- Publish to `PUBLISHED`.
- Archive to `ARCHIVED`.

The frontend refreshes list and detail data after successful saves. If the backend is unavailable, the centralized demo fallback still lives in `frontend/src/data/demo.ts` and is labeled in the UI.

## Prompt Render Boundary

Prompt render is demo/sandbox behavior. It performs local variable replacement, returns structured validation errors for missing variables, and records `AuditLogEntry` evidence.

It does not call a real LLM provider, does not validate real enterprise data, and does not imply production prompt governance.

## Resource Boundary

Resource Library manages context resource metadata, previews, Tool bindings, related Prompts, and recent reference summaries.

It is not a real enterprise configuration center, enterprise knowledge graph, vector database, production lineage system, or real document synchronization service.

## Screenshot Boundary

README may reference only Playwright-generated screenshots under `docs/images`.

AI-generated concept images under `docs/design/concepts` remain visual references only and are not runtime evidence.
