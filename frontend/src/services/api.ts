import {
  demoAuditLogs,
  demoCall,
  demoCurrentUser,
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
  AuditLogFilters,
  DashboardStats,
  DemoUserProfile,
  PageQuery,
  PageResponse,
  PromptDetail,
  PromptListFilters,
  PromptRenderResponse,
  PromptTemplate,
  PromptUpsertInput,
  ResourceDetail,
  ResourceDocument,
  ResourceListFilters,
  ResourceUpsertInput,
  ReviewFilters,
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

export class ApiRequestError extends Error {
  status: number
  detail: unknown

  constructor(status: number, detail: unknown) {
    super(typeof detail === 'object' && detail && 'error' in detail ? String((detail as { error: unknown }).error) : `HTTP ${status}`)
    this.status = status
    this.detail = detail
  }
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
      let detail: unknown = { error: `HTTP ${response.status}` }
      try {
        detail = await response.json()
      } catch {
        detail = { error: `HTTP ${response.status}` }
      }
      throw new ApiRequestError(response.status, detail)
    }
    return { data: (await response.json()) as T, source: 'api' }
  } catch (error) {
    if (error instanceof ApiRequestError) {
      throw error
    }
    return { data: fallback<T>(path, init), source: 'demo-fallback' }
  }
}

export function getTools() {
  return request<ToolDefinition[]>('/tools')
}

export function getStats() {
  return request<DashboardStats>('/dashboard/stats')
}

export async function getCurrentUser() {
  const result = await request<Partial<DemoUserProfile>>('/auth/me')
  return {
    source: result.source,
    data: {
      ...demoCurrentUser,
      ...result.data,
      displayName: result.data.displayName || demoCurrentUser.displayName,
      environment: demoCurrentUser.environment,
      modeLabel: demoCurrentUser.modeLabel,
      productLabel: demoCurrentUser.productLabel,
      signOutLabel: demoCurrentUser.signOutLabel
    }
  } satisfies ApiState<DemoUserProfile>
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
  return pagedRequest<ToolCallReview>('/reviews')
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
  return pagedRequest<AuditLogEntry>('/audit-logs')
}

export function getTraces(filters?: Partial<TraceFilters> & PageQuery) {
  const params = new URLSearchParams()
  appendPageParams(params, filters)
  if (filters?.keyword) params.set('keyword', filters.keyword)
  if (filters?.riskLevel && filters.riskLevel !== 'ALL') params.set('riskLevel', filters.riskLevel)
  if (filters?.status && filters.status !== 'ALL') params.set('status', filters.status)
  if (filters?.reviewRequired === 'YES') params.set('reviewRequired', 'true')
  if (filters?.reviewRequired === 'NO') params.set('reviewRequired', 'false')
  if (filters?.toolName) params.set('toolName', filters.toolName)
  const query = params.toString()
  return pagedRequest<TraceSummary>(`/traces${query ? `?${query}` : ''}`, filters)
}

export function getTraceDetail(traceId: string) {
  return request<TraceDetail>(`/traces/${traceId}`)
}

export function getReviewsPage(filters?: ReviewFilters) {
  const params = new URLSearchParams()
  appendPageParams(params, filters)
  if (filters?.status && filters.status !== 'ALL') params.set('status', filters.status)
  if (filters?.riskLevel && filters.riskLevel !== 'ALL') params.set('riskLevel', filters.riskLevel)
  if (filters?.toolName) params.set('toolName', filters.toolName)
  if (filters?.keyword) params.set('keyword', filters.keyword)
  const query = params.toString()
  return pagedRequest<ToolCallReview>(`/reviews${query ? `?${query}` : ''}`, filters)
}

export function getAuditLogsPage(filters?: AuditLogFilters) {
  const params = new URLSearchParams()
  appendPageParams(params, filters)
  if (filters?.action) params.set('action', filters.action)
  if (filters?.actor) params.set('actor', filters.actor)
  if (filters?.target) params.set('target', filters.target)
  if (filters?.keyword) params.set('keyword', filters.keyword)
  const query = params.toString()
  return pagedRequest<AuditLogEntry>(`/audit-logs${query ? `?${query}` : ''}`, filters)
}

export function getPrompts(filters?: PromptListFilters) {
  const params = new URLSearchParams()
  appendPageParams(params, filters)
  if (filters?.keyword) params.set('keyword', filters.keyword)
  if (filters?.status && filters.status !== 'ALL') params.set('status', filters.status)
  if (filters?.category) params.set('category', filters.category)
  const query = params.toString()
  return pagedRequest<PromptTemplate>(`/prompts${query ? `?${query}` : ''}`, filters)
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

export function createPrompt(payload: PromptUpsertInput) {
  return request<PromptDetail>('/prompts', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function updatePrompt(promptId: string, payload: PromptUpsertInput) {
  return request<PromptDetail>(`/prompts/${promptId}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export function publishPrompt(promptId: string) {
  return request<PromptDetail>(`/prompts/${promptId}/publish`, { method: 'POST' })
}

export function archivePrompt(promptId: string) {
  return request<PromptDetail>(`/prompts/${promptId}/archive`, { method: 'POST' })
}

export function getResources(filters?: ResourceListFilters) {
  const params = new URLSearchParams()
  appendPageParams(params, filters)
  if (filters?.keyword) params.set('keyword', filters.keyword)
  if (filters?.status && filters.status !== 'ALL') params.set('status', filters.status)
  if (filters?.type && filters.type !== 'ALL') params.set('type', filters.type)
  const query = params.toString()
  return pagedRequest<ResourceDocument>(`/resources${query ? `?${query}` : ''}`, filters)
}

export function getResourceDetail(resourceId: string) {
  return request<ResourceDetail>(`/resources/${resourceId}`)
}

export function createResource(payload: ResourceUpsertInput) {
  return request<ResourceDetail>('/resources', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function updateResource(resourceId: string, payload: ResourceUpsertInput) {
  return request<ResourceDetail>(`/resources/${resourceId}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export function publishResource(resourceId: string) {
  return request<ResourceDetail>(`/resources/${resourceId}/publish`, { method: 'POST' })
}

export function archiveResource(resourceId: string) {
  return request<ResourceDetail>(`/resources/${resourceId}/archive`, { method: 'POST' })
}

async function pagedRequest<T>(path: string, query?: PageQuery) {
  const result = await request<PageResponse<T> | T[]>(path)
  return {
    source: result.source,
    data: toPageResponse(result.data, query)
  } satisfies ApiState<PageResponse<T>>
}

function appendPageParams(params: URLSearchParams, query?: PageQuery) {
  if (query?.page !== undefined) params.set('page', String(query.page))
  if (query?.size !== undefined) params.set('size', String(query.size))
}

function toPageResponse<T>(value: PageResponse<T> | T[], query?: PageQuery): PageResponse<T> {
  const page = query?.page ?? 0
  const size = query?.size ?? 10
  if (!Array.isArray(value)) {
    return value
  }
  const start = page * size
  const items = value.slice(start, start + size)
  return {
    items,
    page,
    size,
    total: value.length,
    totalPages: value.length === 0 ? 0 : Math.ceil(value.length / size)
  }
}

function fallback<T>(path: string, init?: RequestInit): T {
  const cleanPath = path.split('?')[0]
  if (cleanPath === '/tools') {
    return demoTools as T
  }
  if (cleanPath === '/dashboard/stats') {
    return demoStats as T
  }
  if (cleanPath === '/auth/me') {
    return demoCurrentUser as T
  }
  if (cleanPath.startsWith('/traces/')) {
    const segments = cleanPath.split('/')
    const traceId = segments[segments.length - 1] ?? demoTraceDetail.traceId
    return { ...demoTraceDetail, traceId } as T
  }
  if (cleanPath.startsWith('/traces')) {
    return demoTraceSummaries as T
  }
  if (cleanPath.includes('/trace')) {
    return demoTrace as T
  }
  if (cleanPath === '/tool-calls') {
    return [demoCall] as T
  }
  if (cleanPath === '/reviews') {
    return demoReviews as T
  }
  if (cleanPath.includes('/reviews/')) {
    return { ...demoReviews[0], status: cleanPath.includes('approve') ? 'APPROVED' : cleanPath.includes('reject') ? 'REJECTED' : 'CHANGES_REQUESTED' } as T
  }
  if (cleanPath === '/audit-logs') {
    return demoAuditLogs as T
  }
  if (cleanPath === '/prompts' && init?.method === 'POST') {
    return demoPromptDetail as T
  }
  if (cleanPath === '/prompts') {
    return demoPrompts as T
  }
  if (cleanPath.includes('/prompts/') && cleanPath.includes('/render')) {
    const body = init?.body ? JSON.parse(String(init.body)) : { variables: {} }
    return demoPromptRender(cleanPath.split('/')[2] ?? demoPromptDetail.prompt.id, body.variables ?? {}) as T
  }
  if (cleanPath.includes('/prompts/') && (cleanPath.includes('/publish') || cleanPath.includes('/archive') || init?.method === 'PUT')) {
    const promptId = cleanPath.split('/')[2] ?? demoPromptDetail.prompt.id
    const prompt = demoPrompts.find((item) => item.id === promptId) ?? demoPromptDetail.prompt
    return { ...demoPromptDetail, prompt } as T
  }
  if (cleanPath.startsWith('/prompts/')) {
    const promptId = cleanPath.split('/')[2] ?? demoPromptDetail.prompt.id
    const prompt = demoPrompts.find((item) => item.id === promptId) ?? demoPromptDetail.prompt
    return { ...demoPromptDetail, prompt, templateContent: prompt.templateContent, variables: prompt.variables } as T
  }
  if (cleanPath === '/resources' && init?.method === 'POST') {
    return demoResourceDetail as T
  }
  if (cleanPath === '/resources') {
    return demoResources as T
  }
  if (cleanPath.includes('/resources/') && (cleanPath.includes('/publish') || cleanPath.includes('/archive') || init?.method === 'PUT')) {
    return demoResourceDetail as T
  }
  if (cleanPath.startsWith('/resources/')) {
    const resourceId = cleanPath.split('/')[2] ?? demoResourceDetail.resource.id
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
  if (cleanPath.includes('/invoke')) {
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
