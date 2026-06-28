<script setup lang="ts">
import type { TraceEvent } from '../types'
import StatusBadge from './StatusBadge.vue'

defineProps<{ events: TraceEvent[] }>()
</script>

<template>
  <section class="trace-panel">
    <header class="trace-header">
      <h2>Trace 时间线</h2>
      <span>Schema Check → Permission Check → Human Review → Execute → Audit Log</span>
    </header>
    <div class="timeline">
      <article v-for="(event, index) in events" :key="event.id" class="timeline-card">
        <div class="timeline-index">{{ index + 1 }}</div>
        <div>
          <strong>{{ event.step }}</strong>
          <StatusBadge :status="event.status" />
        </div>
        <p>{{ event.message }}</p>
        <small>耗时 {{ event.latency }}</small>
      </article>
    </div>
  </section>
</template>
