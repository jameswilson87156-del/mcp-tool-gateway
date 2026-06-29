import {
  demoAuditLogs,
  demoCall,
  demoPromptDetail,
  demoPromptRender,
  demoPrompts,
  demoResourceDetail,
  demoResources,
  demoReviews,
  demoStats,
  demoTools,
  demoTrace,
  demoTraceDetail,
  demoTraceSummaries
} from '../data/demo'
import type {
  AuditLogEntry,
  DashboardStats,
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
  TraceFilters,
  TraceSummary
} from '../types'

const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080/api'

export interface ApiState<T> {
  data: T
  source: 'api' | 'demo-fallback'
}

async function request<T>(path: string, init?: RequestInit): Promise<ApiState<T>> {
  try {
    const response = await fetch(`${API_BASE}${path}`, {
      headers: {
        'Content-Type': 'application/json',
        ...(init?.headers ?? {})
      },
      ...init
    })
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    return { data: (await response.json()) as T, source: 'api' }
  } catch {
    return { data: fallback<T>(path, init), source: 'demo-fallback' }
  }
}

export function getTools() {
  return request<ToolDefinition[]>('/tools')
}

export function getStats() {
  return request<DashboardStats>('/dashboard/stats')
}

export function getToolCalls() {
  return request<ToolCallRecord[]>('/tool-calls')
}

export function invokeTool(toolId: string, parameters: Record<string, unknown>) {
  return request<ToolCallRecord>(`/tools/${toolId}/invoke`, {
    method: 'POST',
    body: JSON.stringify({
      environment: 'production',
      requester: 'admin',
      parameters
    })
  })
}

export function getTrace(callId: string) {
  return request<TraceEvent[]>(`/tool-calls/${callId}/trace`)
}

export function getReviews() {
  return request<ToolCallReview[]>('/reviews')
}

export function decideReview(reviewId: string, action: 'approve' | 'reject' | 'request-changes', comment: string) {
  return request<ToolCallReview>(`/reviews/${reviewId}/${action}`, {
    method: 'POST',
    body: JSON.stringify({
      reviewer: 'reviewer.li',
      comment
    })
  })
}

export function getAuditLogs() {
  return request<AuditLogEntry[]>('/audit-logs')
}

export function getTraces(filters?: Partial<TraceFilters>) {
  const params = new URLSearchParams()
  if (filters?.keyword) params.set('keyword', filters.keyword)
  if (filters?.riskLevel && filters.riskLevel !== 'ALL') params.set('riskLevel', filters.riskLevel)
  if (filters?.status && filters.status !== 'ALL') params.set('status', filters.status)
  if (filters?.reviewRequired === 'YES') params.set('reviewRequired', 'true')
  if (filters?.reviewRequired === 'NO') params.set('reviewRequired', 'false')
  if (filters?.toolName) params.set('toolName', filters.toolName)
  const query = params.toString()
  return request<TraceSummary[]>(`/traces${query ? `?${query}` : ''}`)
}

export function getTraceDetail(traceId: string) {
  return request<TraceDetail>(`/traces/${traceId}`)
}

export function getPrompts() {
  return request<PromptTemplate[]>('/prompts')
}

export function getPromptDetail(promptId: string) {
  return request<PromptDetail>(`/prompts/${promptId}`)
}

export function renderPrompt(promptId: string, variables: Record<string, unknown>) {
  return request<PromptRenderResponse>(`/prompts/${promptId}/render`, {
    method: 'POST',
    body: JSON.stringify({
      requester: 'admin',
      variables
    })
  })
}

export function getResources() {
  return request<ResourceDocument[]>('/resources')
}

export function getResourceDetail(resourceId: string) {
  return request<ResourceDetail>(`/resources/${resourceId}`)
}

function fallback<T>(path: string, init?: RequestInit): T {
  if (path === '/tools') {
    return demoTools as T
  }
  if (path === '/dashboard/stats') {
    return demoStats as T
  }
  if (path.startsWith('/traces/')) {
    const segments = path.split('/')
    const traceId = segments[segments.length - 1] ?? demoTraceDetail.traceId
    return { ...demoTraceDetail, traceId } as T
  }
  if (path.startsWith('/traces')) {
    return demoTraceSummaries as T
  }
  if (path.includes('/trace')) {
    return demoTrace as T
  }
  if (path === '/tool-calls') {
    return [demoCall] as T
  }
  if (path === '/reviews') {
    return demoReviews as T
  }
  if (path.includes('/reviews/')) {
    return { ...demoReviews[0], status: path.includes('approve') ? 'APPROVED' : path.includes('reject') ? 'REJECTED' : 'CHANGES_REQUESTED' } as T
  }
  if (path === '/audit-logs') {
    return demoAuditLogs as T
  }
  if (path === '/prompts') {
    return demoPrompts as T
  }
  if (path.includes('/prompts/') && path.includes('/render')) {
    const body = init?.body ? JSON.parse(String(init.body)) : { variables: {} }
    return demoPromptRender(path.split('/')[2] ?? demoPromptDetail.prompt.id, body.variables ?? {}) as T
  }
  if (path.startsWith('/prompts/')) {
    const promptId = path.split('/')[2] ?? demoPromptDetail.prompt.id
    const prompt = demoPrompts.find((item) => item.id === promptId) ?? demoPromptDetail.prompt
    return { ...demoPromptDetail, prompt, templateContent: prompt.templateContent, variables: prompt.variables } as T
  }
  if (path === '/resources') {
    return demoResources as T
  }
  if (path.startsWith('/resources/')) {
    const resourceId = path.split('/')[2] ?? demoResourceDetail.resource.id
    const resource = demoResources.find((item) => item.id === resourceId) ?? demoResourceDetail.resource
    return {
      ...demoResourceDetail,
      resource,
      contentSummary: resource.contentSummary,
      schemaPreview: resource.schemaPreview,
      markdownPreview: resource.markdownPreview,
      linkedTools: resource.linkedTools,
      relatedPrompts: resource.relatedPrompts
    } as T
  }
  if (path.includes('/invoke')) {
    const body = init?.body ? JSON.parse(String(init.body)) : { parameters: {} }
    return {
      ...demoCall,
      id: `call_fallback_${Date.now()}`,
      request: body.parameters,
      updatedAt: new Date().toISOString()
    } as T
  }
  return demoCall as T
}
