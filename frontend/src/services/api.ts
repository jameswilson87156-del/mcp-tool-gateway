import { demoCall, demoStats, demoTools, demoTrace } from '../data/demo'
import { demoAuditLogs, demoReviews } from '../data/demo'
import type { AuditLogEntry, DashboardStats, ToolCallRecord, ToolCallReview, ToolDefinition, TraceEvent } from '../types'

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

function fallback<T>(path: string, init?: RequestInit): T {
  if (path === '/tools') {
    return demoTools as T
  }
  if (path === '/dashboard/stats') {
    return demoStats as T
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
