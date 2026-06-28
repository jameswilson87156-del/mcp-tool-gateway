export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'BLOCKED'
export type CallStatus =
  | 'DRAFT'
  | 'PENDING_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
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
  provider: string
  version: string
  riskLevel: RiskLevel
  parameters: ToolParameterSchema[]
  permissionScopes: string[]
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

export interface DashboardStats {
  toolCount: number
  promptCount: number
  resourceCount: number
  pendingReviews: number
  blockedCalls: number
  providerStatus: string
  boundary: string
}
