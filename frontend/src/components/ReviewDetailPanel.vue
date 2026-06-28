<script setup lang="ts">
import type { ToolCallRecord, ToolCallReview, ToolDefinition, TraceEvent } from '../types'
import StatusBadge from './StatusBadge.vue'

defineProps<{
  review: ToolCallReview | null
  call: ToolCallRecord | null
  tool: ToolDefinition | null
  trace: TraceEvent[]
}>()
</script>

<template>
  <section class="review-detail-panel">
    <header class="detail-panel-header">
      <div>
        <p class="eyebrow">Request Summary</p>
        <h2>{{ tool?.name ?? '暂无审核请求' }}</h2>
      </div>
      <StatusBadge v-if="review" :status="review.status" />
    </header>

    <div class="review-summary-grid">
      <div>
        <span>Requester</span>
        <strong>{{ call?.requester ?? '-' }}</strong>
      </div>
      <div>
        <span>Provider</span>
        <strong>{{ call?.provider ?? '-' }}</strong>
      </div>
      <div>
        <span>Permission Result</span>
        <strong>RBAC demo 通过</strong>
      </div>
      <div>
        <span>Related Resource</span>
        <strong>policy-docs / customer-knowledge</strong>
      </div>
    </div>

    <div class="split-code-grid">
      <div>
        <h3>Tool Input JSON</h3>
        <pre class="schema-code compact">{{ JSON.stringify(call?.request ?? {}, null, 2) }}</pre>
      </div>
      <div>
        <h3>Output Preview</h3>
        <pre class="schema-code compact">{{ JSON.stringify(call?.response ?? {}, null, 2) }}</pre>
      </div>
    </div>

    <div class="risk-explanation">
      <strong>Risk Explanation</strong>
      <p>该请求访问 `{{ tool?.permissionScopes?.[0] ?? 'db:query:readonly' }}`，风险等级为 {{ review?.riskLevel ?? 'HIGH' }}。高风险 Tool Call 必须经过 Human Review，审批前不会执行 sandbox demo。</p>
    </div>

    <div class="trace-evidence-list">
      <article v-for="event in trace" :key="event.id">
        <strong>{{ event.step }}</strong>
        <StatusBadge :status="event.status" />
        <small>{{ event.message }}</small>
      </article>
    </div>
  </section>
</template>
