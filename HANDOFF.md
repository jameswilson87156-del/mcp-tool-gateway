# Handoff

## Current State

MCP Tool Gateway 的 P1–P5D 功能阶段、P6 portfolio 文档收口、P7A 工程化收口和 P7B JSON-RPC adapter demo 均已完成：

- Spring Boot 3 + Java 17 backend，Vue 3 + Vite + TypeScript frontend。
- H2 + JdbcTemplate persistence，9 张 demo 表，seed-on-empty。
- Tool Registry、Schema、Workbench、Human Review、Trace Evidence、Audit Log。
- Prompt / Resource editing、PageResponse pagination、PolicyService demo RBAC。
- GitHub Actions 在 push / pull request 到 `main` 时运行 Maven tests 和 frontend build。
- Docker Compose 在本地启动 backend `8080` 与 Nginx frontend `8088`。
- springdoc 提供 Swagger UI 和 OpenAPI JSON，只扫描 `/api/**` demo endpoints。
- `POST /api/mcp/rpc` 支持四个 MCP-style JSON-RPC demo methods；`tools/call` 复用 Policy、Invoke、Human Review、Trace 与 Audit。
- Playwright 真实页面截图、Resume Evidence、Interview Q&A、Project Boundary、Roadmap 已归档。

## Engineering Entry Points

- CI：`.github/workflows/ci.yml`
- Compose：`docker-compose.yml`
- Backend image：`backend/Dockerfile`
- Frontend image：`frontend/Dockerfile`
- Deployment guide：`docs/deployment.md`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`
- JSON-RPC adapter：`POST http://localhost:8080/api/mcp/rpc`
- Adapter guide：`docs/mcp-json-rpc-adapter.md`

## Verification Baseline

2026-06-29 P7B 验收基线：

- Backend：`mvn test`，45 tests passed；其中 10 tests 覆盖 JSON-RPC success、Human Review、BLOCKED、参数、method 和 Policy error。
- Frontend：`npm run build` passed。
- GitHub Actions：P7A run `28358408399` 的 backend / frontend jobs passed。
- Compose：本机没有可用的 `docker` 命令，因此 `docker compose config` 未执行；文件已人工复核，但不宣称 Docker 验证通过。
- Docker build：同因未执行；backend jar 路径已通过本机 Maven package 验证。
- Screenshots：P7B 未修改前端 UI 或截图路径，因此不重复生成。
- Root：`git diff --check` passed。
- Security：无真实凭据、`.env`、被跟踪的 build output 或 H2 数据文件。

## Boundaries To Preserve

- MCP-style only，不是完整官方 MCP 协议实现。
- JSON-RPC endpoint 是 HTTP adapter demo，不支持 stdio、SSE 或完整 capabilities negotiation。
- PolicyService / `X-Demo-Role` 是 demo/testing，不是生产鉴权。
- Tool execution 和 Prompt render 是 sandbox，不连接真实 provider。
- H2、PageResponse 和本地筛选服务于 demo 数据，不是生产数据库或搜索架构。
- Docker Compose 是本地 demo 启动方式，不是生产部署架构。
- Swagger / OpenAPI 描述当前 REST demo API，不代表生产级 contract。
- Resource Library 不是企业知识图谱；Human Review 不代表无人值守执行。
- `docs/images` 是运行截图；`docs/design/concepts` 仅是 AI concept references。

## Recommended Next Step

1. 推送 P7B commit 并确认 GitHub Actions 两个 jobs 通过。
2. 可选执行 Compose smoke test，检查 frontend、backend、Swagger UI。
3. 如果继续生产化，使用独立 roadmap 分支，不在当前 portfolio 主线无边界堆功能。

## Quick Commands

```bash
cd backend
mvn test

cd ../frontend
npm ci
npm run build

cd ..
docker compose config
docker compose up --build
git diff --check
git status --short
```
