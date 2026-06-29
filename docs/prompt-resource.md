# Prompt Studio / Resource Library

P4 adds a real Prompt / Resource governance workspace. The page is built with Vue DOM/CSS and follows the same light Linear / Stripe developer-console direction as the Workbench, Tool Registry, Human Review Center, and Trace Evidence pages.

## Pages

The `提示词工作室` navigation entry opens the Prompt tab. The `资源中心` navigation entry opens the same workspace on the Resource tab.

Workspace tabs:

- Prompt 模板
- Resource 资源
- Tool Binding
- Recent Usage

## Backend Endpoints

Prompt endpoints:

- `GET /api/prompts`
- `GET /api/prompts/{id}`
- `POST /api/prompts/{id}/render`

Resource endpoints:

- `GET /api/resources`
- `GET /api/resources/{id}`

## Prompt Render Boundary

Prompt render is demo/sandbox behavior. It performs local variable replacement, returns structured validation errors for missing variables, and records `AuditLogEntry` evidence.

It does not call a real LLM provider, does not validate real enterprise data, and does not imply production prompt governance.

## Resource Boundary

Resource Library manages context resource metadata, previews, Tool bindings, related Prompts, and recent reference summaries.

It is not an enterprise knowledge graph, vector database, production lineage system, or real document synchronization service.

## Screenshot Boundary

README may reference only Playwright-generated screenshots under `docs/images`.

AI-generated concept images under `docs/design/concepts` remain visual references only and are not runtime evidence.
