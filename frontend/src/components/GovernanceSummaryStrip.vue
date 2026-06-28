<script setup lang="ts">
import type { TraceSummary } from '../types'

const props = defineProps<{ traces: TraceSummary[]; source: 'api' | 'demo-fallback' }>()

function count(predicate: (trace: TraceSummary) => boolean) {
  return props.traces.filter(predicate).length
}
</script>

<template>
  <section class="governance-summary-strip">
    <div>
      <span>Trace Evidence</span>
      <strong>{{ traces.length }}</strong>
    </div>
    <div>
      <span>Human Review</span>
      <strong>{{ count((trace) => trace.reviewRequired) }}</strong>
    </div>
    <div>
      <span>Blocked / Failed</span>
      <strong>{{ count((trace) => trace.status === 'BLOCKED' || trace.status === 'FAILED') }}</strong>
    </div>
    <div>
      <span>Data Source</span>
      <strong>{{ source === 'api' ? '后端真实 API' : 'demo fallback' }}</strong>
    </div>
  </section>
</template>
