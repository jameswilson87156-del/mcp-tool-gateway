<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import RiskBadge from '../components/RiskBadge.vue'
import SectionCard from '../components/SectionCard.vue'
import ToolPermissionPanel from '../components/ToolPermissionPanel.vue'
import ToolRegistryTable from '../components/ToolRegistryTable.vue'
import ToolSchemaPanel from '../components/ToolSchemaPanel.vue'
import { getTools } from '../services/api'
import type { RiskLevel, ToolDefinition } from '../types'

const tools = ref<ToolDefinition[]>([])
const selectedTool = ref<ToolDefinition | null>(null)
const query = ref('')
const risk = ref<'ALL' | RiskLevel>('ALL')
const provider = ref('ALL')
const approvalRequired = ref<'ALL' | 'YES' | 'NO'>('ALL')
const source = ref<'api' | 'demo-fallback'>('demo-fallback')

const providers = computed(() => ['ALL', ...new Set(tools.value.map((tool) => tool.provider))])
const filteredTools = computed(() => {
  const q = query.value.trim().toLowerCase()
  return tools.value.filter((tool) => {
    const matchesQuery = !q || `${tool.name} ${tool.description} ${tool.category}`.toLowerCase().includes(q)
    const matchesRisk = risk.value === 'ALL' || tool.riskLevel === risk.value
    const matchesProvider = provider.value === 'ALL' || tool.provider === provider.value
    const matchesApproval =
      approvalRequired.value === 'ALL' ||
      (approvalRequired.value === 'YES' && tool.approvalRequired) ||
      (approvalRequired.value === 'NO' && !tool.approvalRequired)
    return matchesQuery && matchesRisk && matchesProvider && matchesApproval
  })
})

onMounted(async () => {
  const result = await getTools()
  tools.value = result.data
  source.value = result.source
  selectedTool.value = result.data.find((tool) => tool.id === 'crm.customer.search') ?? result.data[0] ?? null
})
</script>

<template>
  <div class="registry-page">
    <header class="page-title-row">
      <div>
        <p class="eyebrow">MCP Tool Gateway</p>
        <h1>Tool Registry</h1>
        <p>统一管理 Agent 可调用的 Tool、Schema、Provider 与权限范围</p>
      </div>
      <div class="boundary-panel">
        <span>{{ source === 'api' ? '后端 API 已连接' : 'demo fallback 已启用' }}</span>
        <strong>{{ tools.length }} Tools · {{ tools.filter((tool) => tool.approvalRequired).length }} 需审批</strong>
      </div>
    </header>

    <section class="registry-filters">
      <label class="search-box">
        <span>⌕</span>
        <input v-model="query" placeholder="搜索 Tool 名称、分类或描述" />
      </label>
      <select v-model="risk">
        <option value="ALL">Risk Level: 全部</option>
        <option value="LOW">LOW</option>
        <option value="MEDIUM">MEDIUM</option>
        <option value="HIGH">HIGH</option>
        <option value="BLOCKED">BLOCKED</option>
      </select>
      <select v-model="provider">
        <option v-for="item in providers" :key="item" :value="item">Provider: {{ item === 'ALL' ? '全部' : item }}</option>
      </select>
      <select v-model="approvalRequired">
        <option value="ALL">Approval Required: 全部</option>
        <option value="YES">需要审批</option>
        <option value="NO">无需审批</option>
      </select>
    </section>

    <div class="registry-layout">
      <SectionCard title="Tool 列表" eyebrow="Registry">
        <ToolRegistryTable :tools="filteredTools" :selected-id="selectedTool?.id ?? ''" @select="selectedTool = $event" />
      </SectionCard>
      <div class="registry-detail-stack">
        <section class="selected-tool-hero">
          <div>
            <p class="eyebrow">Selected Tool</p>
            <h2>{{ selectedTool?.name ?? '请选择 Tool' }}</h2>
            <p>{{ selectedTool?.description }}</p>
          </div>
          <RiskBadge v-if="selectedTool" :risk="selectedTool.riskLevel" />
        </section>
        <ToolSchemaPanel :tool="selectedTool" />
        <ToolPermissionPanel :tool="selectedTool" />
      </div>
    </div>
  </div>
</template>
