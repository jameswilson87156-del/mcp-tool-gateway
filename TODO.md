# TODO

## Completed

- [x] P1：Tool Gateway 基础 API、Tool Call Workbench、sandbox execution。
- [x] P2：Tool Registry、Human Review Center。
- [x] P3：Trace Evidence governance center。
- [x] P4：Prompt Studio / Resource Library。
- [x] P5A：H2 + JdbcTemplate persistence layer。
- [x] P5B：Prompt / Resource create、update、publish、archive workflow。
- [x] P5C：PageResponse pagination、治理列表筛选、Audit Log 页面。
- [x] P5D：RBAC PolicyService demo、敏感 API 检查、结构化 `403`。
- [x] P6：GitHub README、简历证据、面试问答、项目边界和 roadmap 收口。

当前项目已具备 GitHub 简历展示条件：核心链路可运行，真实页面截图、测试结果、能力边界和面试证据均已归档。

## Recommended Next Steps

- [ ] 创建 GitHub remote，并推送 `main`。
- [ ] 可选：部署只读演示环境，并保持 demo/sandbox 标签可见。
- [ ] 可选：仅在明确决定生产化后，新建独立 roadmap 分支逐项替换 demo 边界。

不建议继续无边界堆叠功能。生产化工作应优先处理真实身份、数据库迁移、执行隔离、协议兼容和可观测性，详见 `docs/roadmap.md`。

## Intentionally Out of Current Scope

- Complete official MCP protocol compatibility。
- OAuth / SSO / JWT 与 production-grade authorization。
- Real external provider execution and credentials。
- MySQL / PostgreSQL、migration 与 production deployment。
- Multi-tenancy、enterprise knowledge graph、Elasticsearch。
- Unattended automatic execution for high-risk Tools。
