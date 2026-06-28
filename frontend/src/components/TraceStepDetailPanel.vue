<script setup lang="ts">
import type { TraceDetail, TraceEvent } from '../types'
import StatusBadge from './StatusBadge.vue'
import TraceJsonViewer from './TraceJsonViewer.vue'

defineProps<{ detail: TraceDetail | null; step: TraceEvent | null }>()
</script>

<template>
  <aside class="trace-step-detail-panel">
    <header class="detail-panel-header">
      <div>
        <p class="eyebrow">Step Evidence</p>
        <h2>{{ step?.step ?? '选择步骤' }}</h2>
      </div>
      <StatusBadge v-if="step" :status="step.status" />
    </header>

    <div class="trace-detail-facts">
      <div>
        <span>Permission Result</span>
        <strong>{{ detail?.permissionResult ?? '-' }}</strong>
      </div>
      <div>
        <span>Risk Level</span>
        <strong>{{ detail?.riskLevel ?? '-' }}</strong>
      </div>
      <div>
        <span>Review Decision</span>
        <strong>{{ detail?.reviewDecision ?? '-' }}</strong>
      </div>
      <div>
        <span>Reviewer</span>
        <strong>{{ detail?.reviewer ?? '未指定' }}</strong>
      </div>
    </div>

    <div v-if="detail?.errorMessage" class="risk-explanation">
      <strong>Error Message</strong>
      <p>{{ detail.errorMessage }}</p>
    </div>

    <TraceJsonViewer title="Step Metadata" :value="step?.evidence ?? {}" />
    <TraceJsonViewer title="Input JSON" :value="detail?.inputJson ?? {}" />
    <TraceJsonViewer title="Output JSON" :value="detail?.outputJson ?? {}" />
    <TraceJsonViewer title="Tool Schema 摘要" :value="detail?.toolSchemaSummary ?? {}" />
  </aside>
</template>
