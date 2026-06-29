# Roadmap

## 当前阶段

MCP Tool Gateway 当前是 **portfolio demo / learning project**。P1–P5D、P6、P7A 和 P7B 已完成；当前仍保持 demo/sandbox 边界。

## 已完成

- **P1**：MCP-style Tool Gateway 基础 API、Tool Call Workbench 与 sandbox 调用。
- **P2**：Tool Registry、Human Review Center。
- **P3**：Trace Evidence 治理中心。
- **P4**：Prompt Studio / Resource Library。
- **P5A**：H2 + JdbcTemplate persistence layer。
- **P5B**：Prompt / Resource create、update、publish、archive workflow。
- **P5C**：PageResponse pagination、筛选稳定性与 Audit Log 页面。
- **P5D**：RBAC PolicyService demo 与结构化 `403`。
- **P6**：GitHub README、resume evidence、interview Q&A、project boundary 和 roadmap 文档收口。
- **P7A**：GitHub Actions CI、Docker Compose、OpenAPI / Swagger 与一键启动文档。
- **P7B**：HTTP `POST /api/mcp/rpc` MCP-style JSON-RPC adapter demo，支持四个 demo methods 并复用现有 Tool governance flow。

## 未来生产化路线

以下条目均为 **roadmap / 未实现规划**，不代表当前仓库已经具备对应能力。

### Phase 1：身份、数据与基础安全

- Replace `X-Demo-Role` with real server-side identity context。
- Add JWT / OAuth / SSO，并明确用户、角色和 tenant 的可信来源。
- Replace H2 with MySQL / PostgreSQL。
- Add Flyway / Liquibase migration、约束、索引、备份与恢复策略。
- 将 PageResponse 的筛选、排序和分页下推数据库。

### Phase 2：协议与真实 Tool 执行

- Replace the limited HTTP JSON-RPC adapter demo with a real MCP compatibility layer、capability negotiation、stdio/SSE transport 和 interoperability tests。
- Add real external tool adapter contract。
- 建立隔离 sandbox、secret manager、egress allowlist、超时、重试、幂等和熔断。
- 为 SQL Tool 使用只读账号、AST allowlist、资源限制和独立执行环境。

### Phase 3：策略与租户治理

- Upgrade to production-grade policy engine / policy-as-code。
- 增加资源级 RBAC / ABAC、审批策略版本、策略测试与变更审计。
- Add multi-tenant workspace、tenant isolation、配额和数据归属。
- 强化 Human Review 的并发控制、重新提交、委派和升级流程。

### Phase 4：可观测性与部署加固

- Add OpenTelemetry traces、metrics、structured logs 和 alerting。
- Add rate limit、audit retention、归档、检索与 SIEM integration。
- Add TLS/mTLS、CORS/CSRF 策略、dependency scanning、SBOM、镜像签名和 secret rotation。
- Add container / Kubernetes deployment hardening、健康检查、容量规划、灾备和回滚演练。

## 路线图原则

- 先替换可信边界，再接真实外部工具。
- 每个生产化能力都需要测试、威胁建模、迁移与回滚方案。
- 不为扩大功能数量而继续无边界堆叠；优先补齐身份、隔离、可观测性和运维质量。
- README 与简历只描述已合并、可复核的能力；roadmap 必须持续标记为未来规划。
