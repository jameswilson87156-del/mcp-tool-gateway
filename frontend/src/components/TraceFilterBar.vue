<script setup lang="ts">
import type { CallStatus, RiskLevel, TraceFilters } from '../types'

defineProps<{ filters: TraceFilters; toolOptions: string[] }>()
const emit = defineEmits<{ 'update:filters': [filters: TraceFilters] }>()

const riskOptions: Array<'ALL' | RiskLevel> = ['ALL', 'LOW', 'MEDIUM', 'HIGH', 'BLOCKED']
const statusOptions: Array<'ALL' | CallStatus> = ['ALL', 'PENDING_REVIEW', 'SUCCESS', 'BLOCKED', 'FAILED', 'CHANGES_REQUESTED', 'REJECTED']

function update(filters: TraceFilters, patch: Partial<TraceFilters>) {
  emit('update:filters', { ...filters, ...patch })
}
</script>

<template>
  <section class="trace-filter-bar">
    <label class="search-box">
      <span>⌕</span>
      <input
        :value="filters.keyword"
        placeholder="搜索 Trace ID / Tool Name / Call ID"
        @input="update(filters, { keyword: ($event.target as HTMLInputElement).value })"
      />
    </label>
    <select :value="filters.riskLevel" @change="update(filters, { riskLevel: ($event.target as HTMLSelectElement).value as TraceFilters['riskLevel'] })">
      <option v-for="option in riskOptions" :key="option" :value="option">Risk Level: {{ option === 'ALL' ? '全部' : option }}</option>
    </select>
    <select :value="filters.status" @change="update(filters, { status: ($event.target as HTMLSelectElement).value as TraceFilters['status'] })">
      <option v-for="option in statusOptions" :key="option" :value="option">Status: {{ option === 'ALL' ? '全部' : option }}</option>
    </select>
    <select :value="filters.reviewRequired" @change="update(filters, { reviewRequired: ($event.target as HTMLSelectElement).value as TraceFilters['reviewRequired'] })">
      <option value="ALL">Review Required: 全部</option>
      <option value="YES">需要 Human Review</option>
      <option value="NO">无需 Human Review</option>
    </select>
    <select :value="filters.toolName" @change="update(filters, { toolName: ($event.target as HTMLSelectElement).value })">
      <option value="">Tool Name: 全部</option>
      <option v-for="toolName in toolOptions" :key="toolName" :value="toolName">{{ toolName }}</option>
    </select>
  </section>
</template>
