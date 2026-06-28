<script setup lang="ts">
import type { AuditLogEntry, ToolCallReview } from '../types'

defineProps<{ logs: AuditLogEntry[]; reviews: ToolCallReview[] }>()
</script>

<template>
  <section class="audit-trail-panel">
    <div>
      <h2>Review History</h2>
      <article v-for="review in reviews.slice(0, 4)" :key="review.id" class="audit-item">
        <strong>{{ review.decision }}</strong>
        <span>{{ review.comment }}</span>
        <small>{{ new Date(review.updatedAt).toLocaleString('zh-CN') }}</small>
      </article>
    </div>
    <div>
      <h2>Audit Log</h2>
      <article v-for="log in logs.slice(0, 5)" :key="log.id" class="audit-item">
        <strong>{{ log.action }}</strong>
        <span>{{ log.actor }} · {{ log.targetType }}</span>
        <small>{{ new Date(log.timestamp).toLocaleString('zh-CN') }}</small>
      </article>
    </div>
  </section>
</template>
