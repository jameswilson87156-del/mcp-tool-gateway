# Handoff

## Current State

MCP Tool Gateway 的 P1–P5D 功能阶段和 P6 portfolio 文档收口均已完成。项目当前适合作为 GitHub 上的 Java backend / AI Agent platform learning project 展示：

- Spring Boot 3 + Java 17 backend，Vue 3 + Vite + TypeScript frontend。
- H2 + JdbcTemplate persistence，9 张 demo 表，seed-on-empty。
- Tool Registry、Schema、Workbench、Human Review、Trace Evidence、Audit Log。
- Prompt / Resource create、update、publish、archive 与 sandbox render。
- 五类治理列表统一 PageResponse pagination 与筛选。
- PolicyService demo RBAC、`X-Demo-Role` 测试 helper 和结构化 `403`。
- Playwright 真实本地浏览器截图位于 `docs/images`。
- Resume evidence、Interview Q&A、Project Boundary、Roadmap 已集中整理。

## Verification Baseline

2026-06-29 最近一次验收基线：

- Backend：`mvn test`，34 tests passed。
- Frontend：`npm run build` passed。
- Screenshots：`npm run screenshots` passed；本轮若截图路径和 UI 未变可不重复运行。
- Root：`git diff --check` passed。
- Security check：未跟踪 secret、`.env`、build output、日志或 H2 数据文件。

详细记录见 `docs/TEST_REPORT.md`。

## Boundaries To Preserve

- MCP-style only，不是完整官方 MCP 协议实现。
- PolicyService / `X-Demo-Role` 是 demo/testing，不是生产鉴权。
- Tool execution 和 Prompt render 是 sandbox，不连接真实 provider。
- H2、PageResponse 和本地筛选服务于 demo 数据，不是生产数据库或搜索架构。
- Resource Library 不是企业知识图谱。
- Human Review 是显式审批边界；不宣称无人值守高风险执行。
- `docs/images` 是运行截图；`docs/design/concepts` 仅是 AI concept references。

完整边界见 `docs/project-boundary.md`，未来方向见 `docs/roadmap.md`。

## Recommended Next Step

1. 创建 GitHub remote。
2. 推送当前 `main`。
3. 可选部署只读 demo。
4. 如果继续生产化，使用独立 roadmap 分支，不在当前 portfolio 主线无边界堆功能。

## Quick Commands

```bash
cd backend
mvn test

cd ../frontend
npm run build

cd ..
git diff --check
git status --short
```
