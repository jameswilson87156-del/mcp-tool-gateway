<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AuditTrailList from '../components/AuditTrailList.vue'
import ReviewDecisionPanel from '../components/ReviewDecisionPanel.vue'
import ReviewDetailPanel from '../components/ReviewDetailPanel.vue'
import ReviewQueue from '../components/ReviewQueue.vue'
import SectionCard from '../components/SectionCard.vue'
import { decideReview, getAuditLogs, getReviews, getToolCalls, getTools, getTrace } from '../services/api'
import type { AuditLogEntry, ToolCallRecord, ToolCallReview, ToolDefinition, TraceEvent } from '../types'

const reviews = ref<ToolCallReview[]>([])
const calls = ref<ToolCallRecord[]>([])
const tools = ref<ToolDefinition[]>([])
const auditLogs = ref<AuditLogEntry[]>([])
const trace = ref<TraceEvent[]>([])
const selectedReview = ref<ToolCallReview | null>(null)
const source = ref<'api' | 'demo-fallback'>('demo-fallback')
const busy = ref(false)

const selectedCall = computed(() => calls.value.find((call) => call.id === selectedReview.value?.callId) ?? null)
const selectedTool = computed(() => tools.value.find((tool) => tool.id === selectedReview.value?.toolId) ?? null)
const pendingCount = computed(() => reviews.value.filter((review) => review.status === 'PENDING_REVIEW').length)

onMounted(loadReviewData)

async function loadReviewData() {
  const [reviewState, callState, toolState, auditState] = await Promise.all([getReviews(), getToolCalls(), getTools(), getAuditLogs()])
  reviews.value = reviewState.data
  calls.value = callState.data
  tools.value = toolState.data
  auditLogs.value = auditState.data
  source.value = [reviewState.source, callState.source, toolState.source, auditState.source].every((item) => item === 'api') ? 'api' : 'demo-fallback'
  selectedReview.value = reviews.value.find((review) => review.status === 'PENDING_REVIEW') ?? reviews.value[0] ?? null
  await loadTrace()
}

async function selectReview(review: ToolCallReview) {
  selectedReview.value = review
  await loadTrace()
}

async function loadTrace() {
  if (!selectedReview.value) {
    trace.value = []
    return
  }
  const result = await getTrace(selectedReview.value.callId)
  trace.value = result.data
}

async function handleDecision(action: 'approve' | 'reject' | 'request-changes', comment: string) {
  if (!selectedReview.value) return
  busy.value = true
  try {
    await decideReview(selectedReview.value.id, action, comment)
    await loadReviewData()
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="review-page">
    <header class="page-title-row">
      <div>
        <p class="eyebrow">MCP Tool Gateway</p>
        <h1>Human Review Center</h1>
        <p>审核高风险 Tool Call，保证敏感调用经过人工确认</p>
      </div>
      <div class="boundary-panel">
        <span>{{ source === 'api' ? '后端 API 已连接' : 'demo fallback 已启用' }}</span>
        <strong>{{ pendingCount }} 待审核 · {{ reviews.length }} Review</strong>
      </div>
    </header>

    <div class="review-layout">
      <SectionCard title="待审核队列" eyebrow="Human Review">
        <ReviewQueue
          :reviews="reviews"
          :calls="calls"
          :tools="tools"
          :selected-id="selectedReview?.id ?? ''"
          @select="selectReview"
        />
      </SectionCard>
      <ReviewDetailPanel :review="selectedReview" :call="selectedCall" :tool="selectedTool" :trace="trace" />
      <ReviewDecisionPanel :review="selectedReview" :busy="busy" @decide="handleDecision" />
    </div>

    <AuditTrailList :logs="auditLogs" :reviews="reviews" />
  </div>
</template>
