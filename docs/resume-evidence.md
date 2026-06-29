# Resume Evidence / 简历证据

## 一句话定位

MCP Tool Gateway 是一个面向企业 AI Agent 的 MCP-style 工具接入与治理 demo，围绕 Tool / Prompt / Resource 管理、风险审批、调用证据、审计记录和本地持久化构建可运行闭环。

## 为什么适合 Java 后端 / AI Agent / AI 平台开发实习简历

这个项目不只展示页面或单表 CRUD，而是把 Java 后端常见工程能力放进 Agent 工具调用场景：API 与数据模型设计、状态流转、权限策略边界、持久化、统一分页、结构化错误、集成测试以及前后端联调。同时，它对“已实现的 demo”和“尚未实现的生产能力”有明确区分，便于面试时用代码和测试回答，而不是依赖概念描述。

## 可复核的技术亮点

- **Spring Boot API 设计**：按 Auth、Tools、Tool Calls、Reviews、Traces、Prompts、Resources、Audit Logs 拆分接口，提供结构化 `404`、validation error 和 `403` 响应。
- **JdbcTemplate + H2 persistence**：以 repository 层持久化 Tool、Prompt、Resource、Tool Call、Review、Trace Event、Audit Log、demo user 和 role policy；空库时 seed demo 数据。
- **Tool / Prompt / Resource 数据建模**：描述 Provider、版本、风险、Schema、Prompt 变量、Resource 预览和跨对象绑定关系。
- **Tool Schema 与风险等级**：为 Tool 定义参数、必填项、JSON Schema 摘要、permission scopes 和 `LOW / MEDIUM / HIGH / BLOCKED` 风险等级。
- **Human Review 状态流转**：高风险调用先进入 `PENDING_REVIEW`，支持 approve、reject、request changes；批准后才执行 sandbox Tool。
- **Trace Evidence 聚合**：按 Tool Call 汇总 Request、Tool Select、Schema Check、Permission Check、Human Review、Execute、Audit Log 事件及关联输入输出。
- **Audit Log**：记录登录、Tool 调用、审批、Prompt / Resource create/update/publish/archive 和 Prompt render 等行为。
- **RBAC PolicyService demo**：角色到动作策略由 H2 demo 表提供，并在敏感 controller 入口统一检查；拒绝时返回可解释的结构化 `403`。
- **PageResponse pagination**：为五类治理列表统一 `items / page / size / total / totalPages`，限制最大 page size，并覆盖筛选和空结果测试。
- **Vue 高保真开发者控制台**：实现 Workbench、Registry、Review、Trace、Prompt / Resource 和 Audit 页面，而不是把概念图当成产品页面。
- **Playwright 真实截图**：截图脚本启动本地前后端并捕获实际浏览器页面，形成可在 GitHub 复核的 UI 证据。

## 可写进简历的 bullet

以下表述可按岗位要求选用 5–6 条：

- 设计并实现 MCP-style 企业 Agent 工具网关 demo，使用 Spring Boot 3 提供 Tool、Prompt、Resource、Review、Trace 和 Audit 等分组 API。
- 基于 JdbcTemplate + H2 构建 repository 持久化层，覆盖 9 张 demo 表、JSON/CLOB 字段映射和空库 seed 策略。
- 建模 Tool Schema、permission scope 与四级风险策略，为高风险 Tool Call 设计 `PENDING_REVIEW -> APPROVED / REJECTED / CHANGES_REQUESTED` 人工审批流程。
- 实现 Trace Evidence 聚合，将 Tool 选择、Schema、权限、审批、sandbox 执行和 Audit Log 串成可查询证据链。
- 实现 Prompt / Resource create、update、publish、archive 及审计记录，并用统一 `PageResponse` 支持五类治理列表的分页筛选。
- 使用 Vue 3 + TypeScript 构建开发者控制台，并通过 34 个后端测试、类型检查、Vite build 和 Playwright 真实截图完成本地验收。

## 不建议写进简历的夸大说法

- “实现了完整 MCP 官方协议”——当前是 MCP-style，未实现完整 JSON-RPC compatibility layer。
- “搭建生产级 IAM / RBAC”——PolicyService 和 `X-Demo-Role` 只用于本地 demo/testing。
- “接入真实企业系统并安全执行工具”——Tool response 来自 sandbox demo，没有真实外部凭据或 adapter。
- “实现生产级 SQL 安全网关”——`db.query.readonly` 只是演示性字符串规则，不是 SQL parser、数据库只读账号或隔离执行环境。
- “建设企业知识图谱 / RAG 平台”——Resource Library 只管理上下文资源元数据与预览。
- “实现 Elasticsearch 或海量数据分页”——当前列表从本地 H2 demo 数据读取后在服务层筛选分页。
- “支持无人值守高风险自动化”——高风险调用刻意要求 Human Review。
- “项目已生产部署”——当前阶段是 portfolio demo / learning project。

## 面试官可能追问

1. 这个项目和普通 CRUD 项目的核心差异是什么？
2. 为什么使用 MCP-style，而不是宣称完整 MCP？
3. Tool Schema 当前真正校验了哪些内容，还有哪些缺口？
4. PolicyService、Tool permission scope 与 Human Review 分别解决什么问题？
5. 高风险调用从请求到批准执行的状态如何保持一致？
6. Trace Evidence 如何关联 Tool Call、Review、Trace Event 和 Audit Log？
7. 为什么选择 JdbcTemplate + H2，而不是 JPA 或 MySQL？
8. JSON 字段放在 CLOB 中有什么取舍？
9. PageResponse 为什么还不是生产级数据库分页？
10. `X-Demo-Role` 为什么不能作为真实鉴权？
11. `db.query.readonly` 的字符串拦截规则有哪些绕过风险？
12. Prompt / Resource publish 为什么需要状态和审计证据？
13. 后端不可用时 frontend fallback 如何避免误导用户？
14. 34 个后端测试重点覆盖了哪些业务边界？
15. 如果生产化，第一阶段应先替换哪些 demo 边界？

对应参考回答见 [interview-qa.md](interview-qa.md)。
