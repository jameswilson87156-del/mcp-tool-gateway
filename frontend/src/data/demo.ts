import type { DashboardStats, ToolCallRecord, ToolDefinition, TraceEvent } from '../types'

const DEMO_CALL_ID = 'call_demo_01HX6Z8K'

export const demoTools: ToolDefinition[] = [
  {
    id: 'crm.customer.search',
    name: 'crm.customer.search',
    description: '查询 CRM 客户 demo 数据',
    provider: 'OpenAI-compatible',
    version: 'v1.2.0',
    riskLevel: 'MEDIUM',
    permissionScopes: ['crm:customer:read'],
    parameters: [
      { name: 'customer_id', type: 'string', required: false, description: '客户 ID', example: 'CUST-202405-000123' },
      { name: 'region', type: 'string', required: false, description: '区域', example: 'APAC' },
      { name: 'limit', type: 'integer', required: false, description: '数量限制', example: 20 }
    ]
  },
  {
    id: 'db.query.readonly',
    name: 'db.query.readonly',
    description: '本地 demo SELECT，只允许只读查询',
    provider: 'Local DB Sandbox',
    version: 'v1.0.0',
    riskLevel: 'HIGH',
    permissionScopes: ['db:query:readonly'],
    parameters: [{ name: 'sql', type: 'string', required: true, description: '只读 SELECT SQL', example: 'select * from demo_customers limit 20' }]
  },
  {
    id: 'resume.analyze',
    name: 'resume.analyze',
    description: '分析简历文本并输出 demo 建议',
    provider: 'HR Sandbox',
    version: 'v0.9.0',
    riskLevel: 'MEDIUM',
    permissionScopes: ['hr:resume:read'],
    parameters: [{ name: 'resume_text', type: 'string', required: true, description: '简历文本', example: 'Java developer' }]
  },
  {
    id: 'weather.lookup',
    name: 'weather.lookup',
    description: '查询城市天气 demo 数据',
    provider: 'Weather Sandbox',
    version: 'v1.0.0',
    riskLevel: 'LOW',
    permissionScopes: ['weather:read'],
    parameters: [{ name: 'city', type: 'string', required: true, description: '城市', example: '上海' }]
  },
  {
    id: 'ticket.search',
    name: 'ticket.search',
    description: '查询工单列表',
    provider: 'Ticket Service',
    version: 'v1.1.0',
    riskLevel: 'LOW',
    permissionScopes: ['ticket:read'],
    parameters: [{ name: 'keyword', type: 'string', required: false, description: '关键词', example: 'latency' }]
  },
  {
    id: 'github.issue.search',
    name: 'github.issue.search',
    description: '搜索 GitHub issue demo 数据',
    provider: 'OpenAI-compatible',
    version: 'v1.0.4',
    riskLevel: 'MEDIUM',
    permissionScopes: ['github:issue:read'],
    parameters: [
      { name: 'repository', type: 'string', required: true, description: '仓库', example: 'demo/mcp-gateway' },
      { name: 'query', type: 'string', required: false, description: '搜索条件', example: 'trace' }
    ]
  }
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
  step('Human Review', 'SUCCESS', 'MEDIUM 风险无需审批', '9 ms'),
  step('Execute', 'SUCCESS', '执行 sandbox demo Tool 逻辑', '176 ms'),
  step('Audit Log', 'SUCCESS', '写入 Audit Log', '6 ms')
]

export const demoCall: ToolCallRecord = {
  id: DEMO_CALL_ID,
  toolId: 'crm.customer.search',
  toolName: 'crm.customer.search',
  requester: 'admin',
  provider: 'OpenAI-compatible',
  environment: 'production',
  riskLevel: 'MEDIUM',
  status: 'SUCCESS',
  request: {
    customer_id: 'CUST-202405-000123',
    region: 'APAC',
    limit: 20
  },
  response: {
    count: 2,
    customers: [
      {
        customer_id: 'CUST-202405-000123',
        name: '北京智创科技有限公司',
        region: 'APAC',
        status: 'active'
      }
    ],
    demo: true
  },
  reviewId: null,
  latency: '286 ms',
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString()
}

export function parametersFor(tool: ToolDefinition): Record<string, unknown> {
  return Object.fromEntries(tool.parameters.map((item) => [item.name, item.example]))
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
    timestamp: new Date().toISOString()
  }
}
