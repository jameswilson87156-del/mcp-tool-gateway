<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import JsonEditorPanel from '../components/JsonEditorPanel.vue'
import ResponsePreview from '../components/ResponsePreview.vue'
import RiskBadge from '../components/RiskBadge.vue'
import SectionCard from '../components/SectionCard.vue'
import StatusBadge from '../components/StatusBadge.vue'
import ToolSelector from '../components/ToolSelector.vue'
import TraceTimeline from '../components/TraceTimeline.vue'
import { demoTrace, parametersFor } from '../data/demo'
import { getStats, getTools, getTrace, invokeTool } from '../services/api'
import type { DashboardStats, ToolCallRecord, ToolDefinition, TraceEvent } from '../types'

const tools = ref<ToolDefinition[]>([])
const selectedTool = ref<ToolDefinition | null>(null)
const jsonText = ref('')
const jsonError = ref('')
const lastCall = ref<ToolCallRecord | null>(null)
const traceEvents = ref<TraceEvent[]>(demoTrace)
const source = ref<'api' | 'demo-fallback'>('demo-fallback')
const stats = ref<DashboardStats | null>(null)
const isInvoking = ref(false)

const reviewHint = computed(() => {
  if (!selectedTool.value) return '请选择 Tool'
  if (selectedTool.value.riskLevel === 'HIGH') return 'Human Review: 高风险调用需审批'
  if (selectedTool.value.riskLevel === 'BLOCKED') return 'Human Review: 已阻断'
  return 'Human Review: 无需审批'
})

onMounted(async () => {
  const [toolState, statState] = await Promise.all([getTools(), getStats()])
  tools.value = toolState.data
  stats.value = statState.data
  source.value = toolState.source === 'api' && statState.source === 'api' ? 'api' : 'demo-fallback'
  selectTool(tools.value.find((tool) => tool.id === 'crm.customer.search') ?? tools.value[0])
  await runInvoke()
})

function selectTool(tool: ToolDefinition) {
  selectedTool.value = tool
  jsonText.value = JSON.stringify(parametersFor(tool), null, 2)
  jsonError.value = ''
}

async function runInvoke() {
  if (!selectedTool.value) return
  try {
    jsonError.value = ''
    const parameters = JSON.parse(jsonText.value) as Record<string, unknown>
    isInvoking.value = true
    const result = await invokeTool(selectedTool.value.id, parameters)
    lastCall.value = result.data
    source.value = result.source
    const trace = await getTrace(result.data.id)
    traceEvents.value = trace.data.length ? trace.data : demoTrace
  } catch (error) {
    jsonError.value = error instanceof Error ? error.message : 'JSON 参数格式错误'
  } finally {
    isInvoking.value = false
  }
}
</script>

<template>
  <div class="workbench-page">
    <header class="page-title-row">
      <div>
        <p class="eyebrow">MCP Tool Gateway</p>
        <h1>MCP Tool Gateway</h1>
        <p>企业 Agent 工具接入、权限治理与调用审计平台</p>
        <p class="muted">统一管理 Tool / Prompt / Resource，建立权限边界、Human Review 审批链与 Trace Evidence。</p>
      </div>
      <div class="boundary-panel">
        <span>{{ stats?.providerStatus ?? '12/12 正常' }}</span>
        <strong>{{ source === 'api' ? '后端 API 已连接' : 'demo fallback 已启用' }}</strong>
      </div>
    </header>

    <div class="workbench-grid">
      <SectionCard title="选择 Tool">
        <ToolSelector :tools="tools" :selected-id="selectedTool?.id ?? ''" @select="selectTool" />
      </SectionCard>

      <SectionCard title="调用测试">
        <template #action>
          <div class="chip-row">
            <span class="chip active">MCP</span>
            <span class="chip">生产环境</span>
            <span class="chip">{{ selectedTool?.permissionScopes[0] ?? 'tool:read' }}</span>
          </div>
        </template>
        <div v-if="selectedTool" class="schema-strip">
          <div>
            <span>Provider</span>
            <strong>{{ selectedTool.provider }}</strong>
          </div>
          <div>
            <span>Schema</span>
            <strong>{{ selectedTool.parameters.length }} 参数</strong>
          </div>
          <RiskBadge :risk="selectedTool.riskLevel" />
        </div>
        <JsonEditorPanel v-model="jsonText" :error="jsonError" />
        <div class="invoke-row">
          <button type="button" class="primary-button" :disabled="isInvoking" @click="runInvoke">
            {{ isInvoking ? '执行中...' : '执行调用' }}
          </button>
          <button type="button" class="secondary-button" @click="selectedTool && selectTool(selectedTool)">重置</button>
        </div>
      </SectionCard>

      <SectionCard>
        <ResponsePreview :call="lastCall" :source="source" />
      </SectionCard>
    </div>

    <div class="status-strip">
      <div>
        <span>Human Review</span>
        <strong>{{ reviewHint }}</strong>
      </div>
      <div>
        <span>local-rule fallback</span>
        <strong>db.query.readonly 仅允许 SELECT</strong>
      </div>
      <div>
        <span>当前调用状态</span>
        <StatusBadge :status="lastCall?.status ?? 'DRAFT'" />
      </div>
    </div>

    <TraceTimeline :events="traceEvents" />
  </div>
</template>
