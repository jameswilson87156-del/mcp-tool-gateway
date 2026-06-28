export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'BLOCKED'
export type CallStatus =
  | 'DRAFT'
  | 'PENDING_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'CHANGES_REQUESTED'
  | 'RUNNING'
  | 'SUCCESS'
  | 'FAILED'
  | 'BLOCKED'

export interface ToolParameterSchema {
  name: string
  type: string
  required: boolean
  description: string
  example: unknown
}

export interface ToolDefinition {
  id: string
  name: string
  description: string
  category: string
  provider: string
  version: string
  riskLevel: RiskLevel
  status: string
  approvalRequired: boolean
  parameters: ToolParameterSchema[]
  schema: Record<string, unknown>
  permissionScopes: string[]
  recentCallCount: number
  updatedAt: string
}

export interface ToolCallRecord {
  id: string
  toolId: string
  toolName: string
  requester: string
  provider: string
  environment: string
  riskLevel: RiskLevel
  status: CallStatus
  request: Record<string, unknown>
  response: Record<string, unknown>
  reviewId?: string | null
  latency: string
  createdAt: string
  updatedAt: string
}

export interface TraceEvent {
  id: string
  callId: string
  step: string
  status: CallStatus
  message: string
  latency: string
  evidence: Record<string, unknown>
  timestamp: string
}

export interface ToolCallReview {
  id: string
  callId: string
  toolId: string
  riskLevel: RiskLevel
  status: CallStatus
  reviewer?: string | null
  decision: string
  comment: string
  createdAt: string
  updatedAt: string
}

export interface AuditLogEntry {
  id: string
  actor: string
  action: string
  targetType: string
  targetId: string
  metadata: Record<string, unknown>
  timestamp: string
}

export interface DashboardStats {
  toolCount: number
  promptCount: number
  resourceCount: number
  pendingReviews: number
  blockedCalls: number
  providerStatus: string
  boundary: string
}
