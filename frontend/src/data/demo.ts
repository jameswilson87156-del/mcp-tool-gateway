import type {
  AuditLogEntry,
  DashboardStats,
  DemoUserProfile,
  PromptDetail,
  PromptRenderResponse,
  PromptTemplate,
  ResourceDetail,
  ResourceDocument,
  ToolCallRecord,
  ToolCallReview,
  ToolDefinition,
  TraceDetail,
  TraceEvent,
  TraceSummary
} from '../types'

const DEMO_CALL_ID = 'call_demo_01HX6Z8K'
const now = () => new Date().toISOString()

export const demoCurrentUser: DemoUserProfile = {
  id: 'usr_admin',
  username: 'admin',
  displayName: '王震龙',
  role: 'ADMIN',
  permissionScopes: ['tool:*', 'prompt:*', 'resource:*', 'review:*', 'audit:read'],
  environment: 'Local Demo',
  modeLabel: 'RBAC demo',
  productLabel: 'MCP-style Gateway',
  signOutLabel: 'Sign out disabled in demo'
}

export const demoTools: ToolDefinition[] = [
  tool('crm.customer.search', '查询 CRM 客户 demo 数据', 'CRM', 'OpenAI-compatible', 'v1.2.0', 'MEDIUM', ['crm:customer:read'], [
    { name: 'customer_id', type: 'string', required: false, description: '客户 ID', example: 'CUST-202405-000123' },
    { name: 'region', type: 'string', required: false, description: '区域', example: 'APAC' },
    { name: 'limit', type: 'integer', required: false, description: '数量限制', example: 20 }
  ]),
  tool('db.query.readonly', '本地 demo SELECT，只允许只读查询', 'Database', 'Local DB Sandbox', 'v1.0.0', 'HIGH', ['db:query:readonly'], [
    { name: 'sql', type: 'string', required: true, description: '只读 SELECT SQL', example: 'select * from demo_customers limit 20' }
  ]),
  tool('resume.analyze', '分析简历文本并输出 demo 建议', 'HR', 'HR Sandbox', 'v0.9.0', 'MEDIUM', ['hr:resume:read'], [
    { name: 'resume_text', type: 'string', required: true, description: '简历文本', example: 'Java developer' }
  ]),
  tool('weather.lookup', '查询城市天气 demo 数据', 'External Data', 'Weather Sandbox', 'v1.0.0', 'LOW', ['weather:read'], [
    { name: 'city', type: 'string', required: true, description: '城市', example: '上海' }
  ]),
  tool('ticket.search', '查询工单列表', 'Operations', 'Ticket Service', 'v1.1.0', 'LOW', ['ticket:read'], [
    { name: 'keyword', type: 'string', required: false, description: '关键词', example: 'latency' }
  ]),
  tool('github.issue.search', '搜索 GitHub issue demo 数据', 'Developer', 'OpenAI-compatible', 'v1.0.4', 'MEDIUM', ['github:issue:read'], [
    { name: 'repository', type: 'string', required: true, description: '仓库', example: 'demo/mcp-gateway' },
    { name: 'query', type: 'string', required: false, description: '搜索条件', example: 'trace' }
  ])
]

export const demoStats: DashboardStats = {
  toolCount: 6,
  promptCount: 2,
  resourceCount: 2,
  pendingReviews: 1,
  blockedCalls: 0,
  providerStatus: '12/12 正常',
  boundary: 'MCP-style demo, not a complete official MCP implementation'
}

export const demoTrace: TraceEvent[] = [
  step('Request', 'SUCCESS', '接收 Tool invoke 请求', '4 ms'),
  step('Tool Select', 'SUCCESS', '匹配 ToolDefinition', '8 ms'),
  step('Schema Check', 'SUCCESS', '校验 Tool Schema 与 JSON 参数格式', '18 ms'),
  step('Permission Check', 'SUCCESS', '校验 RBAC demo 权限范围', '42 ms'),
  step('Human Review', 'PENDING_REVIEW', 'HIGH 风险进入人工审批', '15 ms'),
  step('Execute', 'PENDING_REVIEW', '等待审批，未执行 sandbox demo', '0 ms'),
  step('Audit Log', 'SUCCESS', '写入 Audit Log', '6 ms')
]

export const demoCall: ToolCallRecord = {
  id: DEMO_CALL_ID,
  toolId: 'db.query.readonly',
  toolName: 'db.query.readonly',
  requester: 'alice.zhang',
  provider: 'Local DB Sandbox',
  environment: 'production',
  riskLevel: 'HIGH',
  status: 'PENDING_REVIEW',
  request: {
    sql: 'select customer_id, name, status from demo_customers limit 20'
  },
  response: {
    pendingReview: true
  },
  reviewId: 'rev_demo_01',
  latency: '83 ms',
  createdAt: now(),
  updatedAt: now()
}

export const demoReviews: ToolCallReview[] = [
  {
    id: 'rev_demo_01',
    callId: DEMO_CALL_ID,
    toolId: 'db.query.readonly',
    riskLevel: 'HIGH',
    status: 'PENDING_REVIEW',
    reviewer: null,
    decision: 'PENDING',
    comment: '等待人工审批',
    createdAt: now(),
    updatedAt: now()
  }
]

export const demoAuditLogs: AuditLogEntry[] = [
  {
    id: 'aud_demo_01',
    actor: 'alice.zhang',
    action: 'tool.invoke.pending_review',
    targetType: 'ToolCallReview',
    targetId: 'rev_demo_01',
    metadata: { callId: DEMO_CALL_ID },
    timestamp: now()
  }
]

export const demoPrompts: PromptTemplate[] = [
  {
    id: 'prompt_customer_summary',
    name: 'customer-support.summary',
    description: '根据客户、政策文档和区域语言生成结构化客服摘要。',
    version: 'v1.2.0',
    category: 'Customer Support',
    status: 'ACTIVE',
    variables: ['customer_id', 'policy_doc', 'locale'],
    usageScope: '客服 Agent 可读，绑定 CRM 与 policy Resource',
    relatedTools: ['crm.customer.search', 'ticket.search'],
    updatedAt: now(),
    usageCount: 42,
    templateContent: `你是企业客服 Agent。请基于客户 {{customer_id}}、政策资料 {{policy_doc}}，使用 {{locale}} 输出：
1. 客户背景摘要
2. 可能适用的政策边界
3. 下一步 Tool 调用建议
保持审计友好，不要编造未提供事实。`
  },
  {
    id: 'prompt_invoice_review',
    name: 'finance.invoice.review',
    description: '检查发票字段并标注需要 Human Review 的审查原因。',
    version: 'v1.1.0',
    category: 'Finance Governance',
    status: 'DRAFT',
    variables: ['invoice_id', 'vendor', 'amount'],
    usageScope: '财务审查 demo，仅用于 sandbox 渲染',
    relatedTools: ['db.query.readonly'],
    updatedAt: now(),
    usageCount: 8,
    templateContent: `请审查发票 {{invoice_id}}，供应商 {{vendor}}，金额 {{amount}}。
输出字段完整性、风险说明、是否需要 Human Review。
仅基于输入变量判断，不访问真实财务系统。`
  }
]

export const demoResources: ResourceDocument[] = [
  {
    id: 'res_policy_docs',
    name: 'policy-docs',
    type: 'DOCUMENT',
    description: '客服政策文档摘要，用于回答客户服务边界和升级条件。',
    status: 'PUBLISHED',
    tags: ['policy', 'customer-support'],
    linkedTools: ['crm.customer.search', 'ticket.search'],
    updatedAt: now(),
    referenceCount: 27,
    contentSummary: '包含退款、升级、企业支持 SLA、区域差异等 demo 政策摘要。',
    schemaPreview: '',
    markdownPreview: `## 客服政策摘要
- 高价值客户问题优先进入 Human Review。
- 涉及退款、合同、隐私字段时需要审计记录。
- Agent 只能引用 Resource 摘要，不代表真实企业政策。`,
    relatedPrompts: ['prompt_customer_summary']
  },
  {
    id: 'res_customer_schema',
    name: 'customer-db-schema',
    type: 'DB_SCHEMA',
    description: 'CRM 客户查询的字段说明和只读访问边界。',
    status: 'SYNCED',
    tags: ['crm', 'schema', 'readonly'],
    linkedTools: ['crm.customer.search', 'db.query.readonly'],
    updatedAt: now(),
    referenceCount: 15,
    contentSummary: '描述 demo_customers 表的只读字段、脱敏约束和 local-rule fallback。',
    schemaPreview: `{
  "table": "demo_customers",
  "readonly": true,
  "fields": ["customer_id", "name", "region", "status"],
  "blocked": ["insert", "update", "delete", "drop"]
}`,
    markdownPreview: '',
    relatedPrompts: ['prompt_customer_summary', 'prompt_invoice_review']
  }
]

export const demoPromptDetail: PromptDetail = {
  prompt: demoPrompts[0],
  templateContent: demoPrompts[0].templateContent,
  variables: demoPrompts[0].variables,
  version: demoPrompts[0].version,
  status: demoPrompts[0].status,
  usageScope: demoPrompts[0].usageScope,
  relatedTools: demoPrompts[0].relatedTools,
  recentUsage: [
    { tool: 'crm.customer.search', actor: 'admin', result: 'demo/sandbox', timestamp: now() },
    { tool: 'ticket.search', actor: 'agent.builder', result: 'context attached', timestamp: now() }
  ],
  auditLogs: demoAuditLogs,
  warnings: []
}

export const demoResourceDetail: ResourceDetail = {
  resource: demoResources[0],
  contentSummary: demoResources[0].contentSummary,
  schemaPreview: demoResources[0].schemaPreview,
  markdownPreview: demoResources[0].markdownPreview,
  linkedTools: demoResources[0].linkedTools,
  relatedPrompts: demoResources[0].relatedPrompts,
  recentReferences: [
    { tool: 'crm.customer.search', traceId: 'trace_demo_reference', result: 'context attached', timestamp: now() },
    { tool: 'ticket.search', traceId: 'trace_demo_ticket', result: 'policy cited', timestamp: now() }
  ],
  auditLogs: demoAuditLogs
}

export function demoPromptRender(promptId: string, variables: Record<string, unknown>): PromptRenderResponse {
  const prompt = demoPrompts.find((item) => item.id === promptId) ?? demoPrompts[0]
  const missing = prompt.variables.filter((variable) => variables[variable] === undefined || variables[variable] === null || String(variables[variable]).trim() === '')
  if (missing.length > 0) {
    return {
      promptId: prompt.id,
      renderedPrompt: '',
      valid: false,
      validationErrors: missing.map((variable) => `缺少变量: ${variable}`),
      variables,
      renderedAt: now()
    }
  }
  const renderedPrompt = prompt.variables.reduce(
    (content, variable) => content.split(`{{${variable}}}`).join(String(variables[variable])),
    prompt.templateContent
  )
  return {
    promptId: prompt.id,
    renderedPrompt,
    valid: true,
    validationErrors: [],
    variables,
    renderedAt: now()
  }
}

export const demoTraceSummaries: TraceSummary[] = [
  {
    traceId: 'trace_demo_01HX6Z8K',
    callId: DEMO_CALL_ID,
    toolName: demoCall.toolName,
    requester: demoCall.requester,
    riskLevel: demoCall.riskLevel,
    status: demoCall.status,
    reviewStatus: 'PENDING_REVIEW',
    totalLatencyMs: 94,
    createdAt: demoCall.createdAt,
    provider: demoCall.provider,
    fallbackUsed: false,
    reviewRequired: true
  }
]

export const demoTraceDetail: TraceDetail = {
  traceId: 'trace_demo_01HX6Z8K',
  callId: DEMO_CALL_ID,
  toolCall: demoCall,
  toolSchemaSummary: demoTools.find((item) => item.id === demoCall.toolId)?.schema ?? {},
  inputJson: demoCall.request,
  outputJson: demoCall.response,
  status: demoCall.status,
  riskLevel: demoCall.riskLevel,
  permissionResult: 'RBAC demo permission check recorded',
  reviewRequired: true,
  reviewDecision: 'PENDING',
  reviewer: null,
  auditLogs: demoAuditLogs,
  traceEvents: demoTrace,
  totalLatencyMs: 94,
  errorMessage: null
}

export function parametersFor(toolDefinition: ToolDefinition): Record<string, unknown> {
  return Object.fromEntries(toolDefinition.parameters.map((item) => [item.name, item.example]))
}

function tool(
  id: string,
  description: string,
  category: string,
  provider: string,
  version: string,
  riskLevel: ToolDefinition['riskLevel'],
  permissionScopes: string[],
  parameters: ToolDefinition['parameters']
): ToolDefinition {
  const required = parameters.filter((item) => item.required).map((item) => item.name)
  const properties = Object.fromEntries(
    parameters.map((item) => [
      item.name,
      {
        type: item.type,
        description: item.description,
        example: item.example
      }
    ])
  )
  return {
    id,
    name: id,
    description,
    category,
    provider,
    version,
    riskLevel,
    status: 'ACTIVE',
    approvalRequired: riskLevel === 'HIGH' || riskLevel === 'BLOCKED',
    parameters,
    schema: {
      type: 'object',
      required,
      properties
    },
    permissionScopes,
    recentCallCount: id === 'db.query.readonly' ? 1 : 0,
    updatedAt: now()
  }
}

function step(stepName: string, status: TraceEvent['status'], message: string, latency: string): TraceEvent {
  return {
    id: `evt_${stepName.toLowerCase().replace(/\s+/g, '_')}`,
    callId: DEMO_CALL_ID,
    step: stepName,
    status,
    message,
    latency,
    evidence: { demoFallback: true },
    timestamp: now()
  }
}
