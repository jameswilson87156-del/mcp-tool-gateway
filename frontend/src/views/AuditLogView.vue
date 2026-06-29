<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PaginationControls from '../components/PaginationControls.vue'
import { getAuditLogsPage } from '../services/api'
import type { AuditLogEntry, PageResponse } from '../types'

const auditPage = ref<PageResponse<AuditLogEntry>>({ items: [], page: 0, size: 10, total: 0, totalPages: 0 })
const source = ref<'api' | 'demo-fallback'>('demo-fallback')
const loading = ref(false)
const filters = ref({
  keyword: '',
  action: '',
  actor: '',
  target: ''
})

onMounted(loadAuditLogs)

async function loadAuditLogs(page = auditPage.value.page) {
  loading.value = true
  try {
    const result = await getAuditLogsPage({ ...filters.value, page, size: 10 })
    auditPage.value = result.data
    source.value = result.source
  } finally {
    loading.value = false
  }
}

async function applyFilters() {
  auditPage.value = { ...auditPage.value, page: 0 }
  await loadAuditLogs(0)
}

async function changePage(page: number) {
  auditPage.value = { ...auditPage.value, page }
  await loadAuditLogs(page)
}
</script>

<template>
  <div class="audit-log-page">
    <header class="page-title-row">
      <div>
        <p class="eyebrow">MCP Tool Gateway</p>
        <h1>Audit Log</h1>
        <p>按 action、actor、target 与 keyword 筛选 demo H2 审计证据，不提供生产级搜索能力。</p>
      </div>
      <div class="boundary-panel">
        <span>{{ source === 'api' ? '后端 API 已连接' : 'demo fallback 已启用' }}</span>
        <strong>{{ auditPage.total }} Audit Events · {{ loading ? '加载中' : '已同步' }}</strong>
      </div>
    </header>

    <section class="audit-filter-bar">
      <label class="search-box">
        <span>⌕</span>
        <input v-model="filters.keyword" placeholder="搜索 action / actor / target / metadata" @input="applyFilters" />
      </label>
      <input v-model="filters.action" placeholder="Action" @input="applyFilters" />
      <input v-model="filters.actor" placeholder="Actor" @input="applyFilters" />
      <input v-model="filters.target" placeholder="Target" @input="applyFilters" />
    </section>

    <section class="audit-evidence-list">
      <article v-for="entry in auditPage.items" :key="entry.id" class="audit-evidence-card">
        <div>
          <p class="eyebrow">{{ entry.action }}</p>
          <h2>{{ entry.targetType }} · {{ entry.targetId }}</h2>
          <span>{{ entry.actor }} · {{ new Date(entry.timestamp).toLocaleString('zh-CN') }}</span>
        </div>
        <pre>{{ JSON.stringify(entry.metadata, null, 2) }}</pre>
      </article>
      <div v-if="!auditPage.items.length" class="empty-state">
        <strong>没有匹配的 Audit Log</strong>
        <p>调整筛选条件，或先执行 Tool Call / Human Review / Prompt Resource 写操作生成审计证据。</p>
      </div>
    </section>

    <PaginationControls :page="auditPage" @change="changePage" />
  </div>
</template>
