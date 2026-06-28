import type { AuditLogEntry, DashboardStats, ToolCallRecord, ToolCallReview, ToolDefinition, TraceEvent } from '../types'

const DEMO_CALL_ID = 'call_demo_01HX6Z8K'
const now = () => new Date().toISOString()

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
