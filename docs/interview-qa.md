# Interview Q&A

## 1. 这个项目和普通 CRUD 有什么区别？

普通 CRUD 主要解决对象的增删改查；这个项目的主线是 Agent Tool Call 的治理闭环。一次调用会经过 Tool 选择、Schema 记录、Policy 检查、风险判断、可选 Human Review、sandbox 执行、Trace Evidence 和 Audit Log，并让 Tool Call 与 Review 状态联动。

Prompt / Resource 的编辑确实包含 CRUD，但又加入 draft、publish、archive、validation 和 audit evidence。项目价值在于状态、策略和证据链的组合，而不是 CRUD 本身。

## 2. 为什么叫 MCP-style，而不说完整 MCP？

项目借用了 MCP 的 Tool、Prompt、Resource 三类能力抽象和工具接入思想，但当前对外 API 是普通 Spring MVC REST API，没有实现完整 MCP JSON-RPC 消息格式、能力协商、标准 transport、协议版本兼容和官方 SDK interoperability。

因此称为 MCP-style 更准确：它证明了网关治理模型和产品形态，不把尚未实现的协议兼容包装成现有能力。

## 3. Tool Schema 怎么设计？

`ToolDefinition` 保存 Tool 的基础信息、Provider、版本、风险等级、审批要求、permission scopes、参数列表和 Schema 摘要；每个参数描述名称、类型、是否必填、说明和示例。前端据此展示参数与 JSON Schema，Trace 中也记录 Schema Check 步骤。

当前 Schema Check 仍是 demo：实现会提取并记录必填参数，但没有接入完整 JSON Schema validator 对所有请求做严格运行时校验。生产化时应引入标准 validator、schema version、兼容性检查和可解释的字段级错误。

## 4. 高风险 Tool Call 怎么处理？

敏感调用先在 controller 入口经过 `PolicyService` 的 `TOOL_INVOKE` 检查。业务层发现 Tool 风险为 `HIGH` 后创建 `ToolCallRecord` 和 `ToolCallReview`，状态设为 `PENDING_REVIEW`，写入 Trace 与 Audit，且不会执行 Tool。

只有 reviewer 批准后，服务才执行 sandbox Tool 并更新 Call、Review、Trace 和 Audit；拒绝或要求修改都不会执行。`BLOCKED` 风险或命中危险 SQL 规则时会直接阻断。

## 5. Human Review 状态如何流转？

入口状态是 `PENDING_REVIEW`，决策分为：

- approve：Review 变为 `APPROVED`，Tool Call 执行后变为 `SUCCESS`；
- reject：Review 和 Tool Call 进入拒绝状态，不执行 Tool；
- request changes：进入 `CHANGES_REQUESTED`，要求补充上下文或缩小参数范围。

每个决策同步保存 Review、Tool Call、Trace Event 和 Audit Log。当前 demo 未实现复杂的重新提交、乐观锁和并发审批，生产环境需补充状态机约束与幂等控制。

## 6. Trace Evidence 是怎么聚合的？

每次调用生成 `call_*` 标识，相关 `TraceEvent` 都以 call id 存储；展示层把它映射为 `trace_*` 标识。详情查询再聚合 Tool Call、Tool Schema、input/output JSON、Review 决策、Trace Events 和关联 Audit Logs。

这种设计让页面既能看摘要列表，也能下钻到一次调用的步骤证据。当前 latency 是 demo 数据，生产环境应替换为真实时间测量、标准 trace/span id 和 OpenTelemetry。

## 7. Audit Log 记录什么？

Audit Log 记录 actor、action、target type、target id、timestamp 和 metadata。现有动作覆盖 demo 登录、Tool 调用成功/待审/阻断、Review 决策、Prompt / Resource create/update/publish/archive，以及 Prompt render 成功或校验失败。

它的目标是回答“谁在什么时候对哪个对象做了什么”。当前本地 H2 记录不是不可篡改审计存储，也没有 retention、归档、签名和 SIEM 对接，因此不能宣称生产审计平台。

## 8. Prompt / Resource 在这个系统里有什么作用？

Prompt 用于管理 Agent 可复用的模板、变量、适用范围和关联 Tools，并提供本地变量替换的 sandbox render。Resource 用于管理可被 Prompt 或 Tool 引用的上下文资源摘要、预览、标签和绑定关系。

二者都有 draft/publish/archive 工作流和 Audit Log，体现“配置也需要治理”。Resource 不是知识图谱或向量数据库，Prompt render 也不调用真实 LLM。

## 9. 为什么使用 JdbcTemplate + H2，而不是 JPA？

本阶段目标是用最小依赖展示清晰的数据边界。H2 便于 clone 后直接运行测试；JdbcTemplate 让 SQL、RowMapper、JSON/CLOB 映射和 repository 行为保持显式，适合当前规模，也避免为了 demo 引入复杂 ORM 映射。

代价是手工映射较多，关系约束和查询组合能力有限。生产化时可继续使用 JDBC 技术栈迁移到 PostgreSQL/MySQL，也可以在模型复杂度明显上升后评估 jOOQ、MyBatis 或 JPA；选择应由查询与领域模型决定。

## 10. PageResponse 分页怎么设计？

统一响应包含 `items`、`page`、`size`、`total`、`totalPages`；页码从 0 开始，默认 size 为 10，最大 50，负页码和异常 size 会被归一化。Trace、Review、Audit、Prompt、Resource 共用该结构，前端也能兼容早期数组响应。

当前实现是 repository 读取本地 demo 数据后，在服务层筛选、排序并切片，适合少量 seed 数据，但不是生产级分页。数据量增大后应将 filter、sort、limit/offset 或 keyset pagination 下推数据库并建立索引。

## 11. RBAC PolicyService 怎么设计？

`PolicyService` 将 `UserRole` 映射到 `PolicyAction`。优先读取 H2 `role_policies` 中的允许动作；本地表为空时使用同形 fallback matrix。敏感 controller 在业务方法执行前调用 `require`，不允许时抛出策略异常，由统一 handler 返回含 action、role、requestId 的结构化 `403`。

这个小边界使授权逻辑不散落在业务服务中，也便于测试不同角色。但它没有真实身份、tenant、资源级条件、策略管理和防篡改能力，只是 PolicyService demo。

## 12. 为什么 X-Demo-Role 不是生产鉴权？

因为它是客户端可直接填写的普通 header，没有签名、session、token 校验或服务端角色绑定。任何调用方都可以声称自己是 `ADMIN`；缺省 header 还会解析成 demo admin，这明显只适合测试和演示。

生产环境应由可信身份层完成认证，再从服务端 identity/claims 映射角色和 tenant，并确保客户端不能覆盖授权上下文。

## 13. db.query.readonly 为什么必须限制只读？

Agent 生成的 SQL 可能误写、越权或被 prompt injection 操纵。demo 规则要求 SQL 以 `select` 开头，并拦截 `insert/update/delete/drop/alter/truncate` 等关键词，命中后记录 blocked Trace 且不执行 sandbox Tool。

字符串检查只是风险意识演示，可能被注释、编码、嵌套语法或关键词误判绕过。真实方案应使用独立只读数据库账号、SQL parser/AST allowlist、表列级授权、行数和超时限制、事务只读、网络隔离及完整审计，不能只依赖正则或字符串包含判断。

## 14. 前端为什么选择 Tool Call Workbench 作为默认首页？

Workbench 能在一个页面展示项目最重要的价值链：选 Tool、查看 Schema/权限、编辑 JSON、发起调用、看到响应、Review 状态与 Trace。相比 KPI dashboard，它更快证明项目确实可操作，并能自然引导到 Registry、Human Review、Trace、Prompt / Resource 和 Audit 页面。

这也是开发者工具产品更合适的信息架构：默认页围绕主要任务，而不是先展示与任务脱节的统计卡片。

## 15. 如果继续升级到生产环境，你会怎么做？

我会分阶段替换 demo 边界：先建立真实身份与 tenant boundary，用 JWT/OAuth/SSO 替换 `X-Demo-Role`；迁移 PostgreSQL/MySQL 并加入 Flyway/Liquibase、约束、索引和数据库级分页；再实现 MCP JSON-RPC compatibility layer 和隔离的 external tool adapter。

随后把 PolicyService 升级为资源级 policy-as-code，补充幂等、并发审批、rate limit、secret management、审计 retention、OpenTelemetry、指标告警和部署加固。每一步都应带威胁建模、迁移测试和回滚方案；这些属于 roadmap，不是当前已实现能力。
