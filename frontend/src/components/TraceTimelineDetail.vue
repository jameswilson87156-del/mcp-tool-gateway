<script setup lang="ts">
import type { TraceEvent } from '../types'
import StatusBadge from './StatusBadge.vue'

defineProps<{ events: TraceEvent[]; selectedStepId: string }>()
defineEmits<{ select: [event: TraceEvent] }>()
</script>

<template>
  <section class="trace-timeline-detail">
    <header class="trace-header">
      <h2>Trace Timeline</h2>
      <span>Request -> Tool Select -> Schema Check -> Permission Check -> Human Review -> Execute -> Audit Log</span>
    </header>
    <div class="trace-step-grid">
      <button
        v-for="(event, index) in events"
        :key="event.id"
        type="button"
        class="trace-step-card"
        :class="{ selected: selectedStepId === event.id }"
        @click="$emit('select', event)"
      >
        <span class="timeline-index">{{ index + 1 }}</span>
        <strong>{{ event.step }}</strong>
        <StatusBadge :status="event.status" />
        <p>{{ event.message }}</p>
        <small>{{ event.latency }} · {{ new Date(event.timestamp).toLocaleTimeString('zh-CN') }}</small>
      </button>
    </div>
  </section>
</template>
