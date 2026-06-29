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

export interface TraceSummary {
  traceId: string
  callId: string
  toolName: string
  requester: string
  riskLevel: RiskLevel
  status: CallStatus
  reviewStatus: CallStatus
  totalLatencyMs: number
  createdAt: string
  provider: string
  fallbackUsed: boolean
  reviewRequired: boolean
}

export interface TraceDetail {
  traceId: string
  callId: string
  toolCall: ToolCallRecord
  toolSchemaSummary: Record<string, unknown>
  inputJson: Record<string, unknown>
  outputJson: Record<string, unknown>
  status: CallStatus
  riskLevel: RiskLevel
  permissionResult: string
  reviewRequired: boolean
  reviewDecision: string
  reviewer?: string | null
  auditLogs: AuditLogEntry[]
  traceEvents: TraceEvent[]
  totalLatencyMs: number
  errorMessage?: string | null
}

export interface TraceFilters {
  keyword: string
  riskLevel: 'ALL' | RiskLevel
  status: 'ALL' | CallStatus
  reviewRequired: 'ALL' | 'YES' | 'NO'
  toolName: string
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

export type PromptStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED'
export type ResourceStatus = 'DRAFT' | 'PUBLISHED' | 'SYNCED' | 'ARCHIVED'
export type ResourceType = 'DOCUMENT' | 'API_SPEC' | 'DB_SCHEMA' | 'BUSINESS_RULE' | 'POLICY'

export interface PromptTemplate {
  id: string
  name: string
  description: string
  version: string
  category: string
  status: PromptStatus
  variables: string[]
  usageScope: string
  relatedTools: string[]
  updatedAt: string
  usageCount: number
  templateContent: string
}

export interface PromptDetail {
  prompt: PromptTemplate
  templateContent: string
  variables: string[]
  version: string
  status: PromptStatus
  usageScope: string
  relatedTools: string[]
  recentUsage: Array<Record<string, unknown>>
  auditLogs: AuditLogEntry[]
  warnings: string[]
}

export interface PromptRenderResponse {
  promptId: string
  renderedPrompt: string
  valid: boolean
  validationErrors: string[]
  variables: Record<string, unknown>
  renderedAt: string
}

export interface ResourceDocument {
  id: string
  name: string
  type: ResourceType
  description: string
  status: ResourceStatus
  tags: string[]
  linkedTools: string[]
  updatedAt: string
  referenceCount: number
  contentSummary: string
  schemaPreview: string
  markdownPreview: string
  relatedPrompts: string[]
}

export interface ResourceDetail {
  resource: ResourceDocument
  contentSummary: string
  schemaPreview: string
  markdownPreview: string
  linkedTools: string[]
  relatedPrompts: string[]
  recentReferences: Array<Record<string, unknown>>
  auditLogs: AuditLogEntry[]
}

export interface PromptUpsertInput {
  name: string
  description: string
  category: string
  templateContent: string
  variables: string[]
  usageScope: string
  relatedTools: string[]
  status?: PromptStatus
}

export interface ResourceUpsertInput {
  name: string
  type: ResourceType
  description: string
  contentSummary: string
  schemaPreview: string
  markdownPreview: string
  tags: string[]
  linkedTools: string[]
  relatedPrompts: string[]
  status?: ResourceStatus
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

export interface UserAccount {
  id: string
  username: string
  displayName: string
  role: 'ADMIN' | 'DEVELOPER' | 'REVIEWER' | 'VIEWER'
  permissionScopes: string[]
}

export interface DemoUserProfile extends UserAccount {
  environment: 'Local Demo'
  modeLabel: 'RBAC demo'
  productLabel: 'MCP-style Gateway'
  signOutLabel: 'Sign out disabled in demo'
}
