<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import AuditEvidencePanel from '../components/AuditEvidencePanel.vue'
import GovernanceSummaryStrip from '../components/GovernanceSummaryStrip.vue'
import TraceFilterBar from '../components/TraceFilterBar.vue'
import TraceListPanel from '../components/TraceListPanel.vue'
import TraceStepDetailPanel from '../components/TraceStepDetailPanel.vue'
import TraceTimelineDetail from '../components/TraceTimelineDetail.vue'
import { getTraceDetail, getTraces } from '../services/api'
import type { TraceDetail, TraceEvent, TraceFilters, TraceSummary } from '../types'

const filters = ref<TraceFilters>({
  keyword: '',
  riskLevel: 'ALL',
  status: 'ALL',
  reviewRequired: 'ALL',
  toolName: ''
})
const traces = ref<TraceSummary[]>([])
const toolOptions = ref<string[]>([])
const selectedTrace = ref<TraceSummary | null>(null)
const detail = ref<TraceDetail | null>(null)
const selectedStep = ref<TraceEvent | null>(null)
const source = ref<'api' | 'demo-fallback'>('demo-fallback')
const loading = ref(false)

const selectedTraceId = computed(() => selectedTrace.value?.traceId ?? '')

onMounted(loadTraces)

watch(filters, loadTraces, { deep: true })

async function loadTraces() {
  loading.value = true
  try {
    const result = await getTraces(filters.value)
    traces.value = result.data
    const nextTools = new Set(toolOptions.value)
    traces.value.forEach((trace) => nextTools.add(trace.toolName))
    toolOptions.value = [...nextTools].sort()
    source.value = result.source
    if (!selectedTrace.value || !traces.value.some((item) => item.traceId === selectedTrace.value?.traceId)) {
      selectedTrace.value = traces.value[0] ?? null
    }
    await loadDetail()
  } finally {
    loading.value = false
  }
}

async function selectTrace(trace: TraceSummary) {
  selectedTrace.value = trace
  await loadDetail()
}

async function loadDetail() {
  if (!selectedTrace.value) {
    detail.value = null
    selectedStep.value = null
    return
  }
  const result = await getTraceDetail(selectedTrace.value.traceId)
  detail.value = result.data
  source.value = result.source
  selectedStep.value = result.data.traceEvents[0] ?? null
}
</script>

<template>
  <div class="trace-evidence-page">
    <header class="page-title-row">
      <div>
        <p class="eyebrow">MCP Tool Gateway</p>
        <h1>Trace Evidence</h1>
        <p>追踪 Tool Call 从请求、权限校验、Human Review 到 Audit Log 的完整证据链</p>
      </div>
      <div class="boundary-panel">
        <span>{{ source === 'api' ? '后端 API 已连接' : 'demo fallback 已启用' }}</span>
        <strong>{{ traces.length }} Traces · {{ loading ? '加载中' : '已同步' }}</strong>
      </div>
    </header>

    <GovernanceSummaryStrip :traces="traces" :source="source" />
    <TraceFilterBar v-model:filters="filters" :tool-options="toolOptions" />

    <div class="trace-governance-layout">
      <TraceListPanel :traces="traces" :selected-trace-id="selectedTraceId" @select="selectTrace" />
      <main class="trace-center-column">
        <TraceTimelineDetail :events="detail?.traceEvents ?? []" :selected-step-id="selectedStep?.id ?? ''" @select="selectedStep = $event" />
        <AuditEvidencePanel :logs="detail?.auditLogs ?? []" />
      </main>
      <TraceStepDetailPanel :detail="detail" :step="selectedStep" />
    </div>
  </div>
</template>
