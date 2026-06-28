<script setup lang="ts">
import type { TraceSummary } from '../types'
import RiskBadge from './RiskBadge.vue'
import StatusBadge from './StatusBadge.vue'

defineProps<{ traces: TraceSummary[]; selectedTraceId: string }>()
defineEmits<{ select: [trace: TraceSummary] }>()
</script>

<template>
  <section class="trace-list-panel">
    <header>
      <p class="eyebrow">Trace List</p>
      <h2>证据链列表</h2>
    </header>
    <div v-if="traces.length" class="trace-list">
      <button
        v-for="trace in traces"
        :key="trace.traceId"
        type="button"
        class="trace-list-row"
        :class="{ selected: selectedTraceId === trace.traceId }"
        @click="$emit('select', trace)"
      >
        <span class="trace-row-main">
          <strong>{{ trace.toolName }}</strong>
          <small>{{ trace.traceId }} · {{ trace.callId }}</small>
        </span>
        <span class="trace-row-meta">
          <small>{{ trace.requester }}</small>
          <strong>{{ trace.totalLatencyMs }} ms</strong>
        </span>
        <RiskBadge :risk="trace.riskLevel" />
        <StatusBadge :status="trace.status" />
        <span class="badge" :class="trace.reviewRequired ? 'status-pending-review' : 'status-success'">
          {{ trace.reviewRequired ? 'Human Review' : 'Auto Pass' }}
        </span>
      </button>
    </div>
    <div v-else class="empty-state">
      <strong>没有匹配的 Trace Evidence</strong>
      <p>请调整筛选条件，或先在 Tool Call 工作台执行一次 demo 调用。</p>
    </div>
  </section>
</template>
