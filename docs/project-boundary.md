# Project Boundary / 项目边界

本文集中说明 MCP Tool Gateway 当前“做到了什么、只演示了什么、没有做什么”，避免把 portfolio demo 描述为生产系统。

## 已实现能力

- Spring Boot REST API：Auth、Tools、Tool Calls、Reviews、Traces、Prompts、Resources、Dashboard、Audit Logs。
- Tool Registry、Tool 参数 / Schema 摘要、Provider、版本、风险等级、审批要求和 permission scopes 数据模型。
- Tool Call Workbench 与本地 sandbox response。
- 高风险 Tool Call 的 Human Review：approve、reject、request changes。
- Trace Evidence：按调用聚合步骤事件、输入输出、Review 和 Audit evidence。
- Prompt / Resource create、update、draft、publish、archive；Prompt 本地 render 与校验反馈。
- H2 + JdbcTemplate repositories、`schema.sql` 建表和 seed-on-empty demo 数据。
- Trace、Review、Audit、Prompt、Resource 的统一 `PageResponse` 分页和本地筛选。
- PolicyService 角色—动作检查与结构化 `403`。
- Vue 3 + TypeScript 开发者控制台，以及 Playwright 对真实本地页面的截图脚本。
- 35 个 JUnit / MockMvc 后端测试，覆盖主要 API、OpenAPI、持久化、状态流转、分页和 Policy 边界。

## Demo / Sandbox 能力

| 能力 | 当前含义 | 不代表 |
| --- | --- | --- |
| Demo auth | 返回本地 demo user | 真实登录、账号生命周期或可信身份 |
| `X-Demo-Role` | 测试角色策略的 header | OAuth、SSO、JWT 或不可伪造的授权上下文 |
| PolicyService | 本地角色到动作 allowlist | 生产 IAM、资源级 ABAC 或多租户策略引擎 |
| Tool execution | 返回固定/参数化 sandbox 数据 | 调用真实 CRM、数据库、GitHub、工单或天气系统 |
| `db.query.readonly` | 演示 SELECT-only 字符串规则 | SQL parser、数据库隔离或生产 SQL 安全网关 |
| Prompt render | 本地 `{{variable}}` 替换与缺参反馈 | 真实 LLM 调用或生产 Prompt runtime |
| Resource Library | 上下文资源元数据、预览和绑定 | 企业知识图谱、向量库、RAG 或同步平台 |
| H2 persistence | 本机内存 demo 存储 | 高可用生产数据库方案 |
| PageResponse | 对少量本地数据筛选切片 | 数据库级海量分页、全文检索或 Elasticsearch |
| Trace / Audit | 可查询的 demo 证据 | OpenTelemetry、不可篡改审计或 SIEM |

## 未实现能力

- 完整 MCP 官方协议、JSON-RPC compatibility、capability negotiation 和标准 transport。
- 真实 external tool adapter、凭据托管、网络隔离和执行容器。
- OAuth、SSO、JWT、MFA、session、用户生命周期和可信角色分配。
- 生产级 RBAC/ABAC、policy-as-code、资源级条件与 tenant isolation。
- MySQL / PostgreSQL、数据库 migration、备份恢复、HA 和生产索引策略。
- Multi-tenant workspace、配额、计费或 workspace 管理。
- OpenTelemetry、metrics、alerting、集中日志和生产 audit retention。
- Rate limit、幂等键、并发审批、分布式锁和工作流恢复。
- Secret manager、TLS/mTLS、供应链安全、镜像和部署加固。
- 真实企业数据、企业知识图谱或无人值守高风险自动执行。

## 不夸大的表述规则

- 使用“**MCP-style 工具网关 demo**”，不使用“完整 MCP server / 完整 MCP 协议实现”。
- 使用“**RBAC PolicyService demo**”，不使用“生产级权限平台”。
- 使用“**sandbox Tool execution**”，不使用“已接入真实企业系统”。
- 使用“**本地 H2 demo persistence**”，不使用“生产数据库架构”。
- 使用“**context Resource Library**”，不使用“企业级知识图谱”。
- 使用“**demo 分页与筛选**”，不使用“海量检索 / Elasticsearch”。
- 使用“**显式 Human Review**”，不使用“无人值守高风险自动执行”。
- `docs/images` 是真实本地页面截图；`docs/design/concepts` 是 AI concept references，不能混用。

## 后续可扩展方向

以下全部属于未来工作，不是当前能力：

- 将 H2 迁移到 MySQL / PostgreSQL，并增加 schema migration。
- 增加真实 MCP JSON-RPC compatibility layer。
- 为外部 Tool 建立 adapter contract、sandbox、secret injection 和网络策略。
- 用 OAuth / SSO / JWT 和服务端 identity context 替换 demo header。
- 引入 production-grade policy engine 和 policy-as-code。
- 增加 multi-tenant workspace 与资源隔离。
- 接入 OpenTelemetry、metrics、告警和审计 retention。
- 完成 rate limit、依赖与镜像治理、TLS、部署和灾备加固。

具体阶段建议见 [roadmap.md](roadmap.md)。
