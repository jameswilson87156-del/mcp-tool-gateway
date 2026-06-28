<script setup lang="ts">
import type { AuditLogEntry } from '../types'

defineProps<{ logs: AuditLogEntry[] }>()
</script>

<template>
  <section class="audit-evidence-panel">
    <header>
      <p class="eyebrow">Audit Evidence</p>
      <h2>相关审计日志</h2>
    </header>
    <div class="audit-evidence-list">
      <article v-for="log in logs" :key="log.id" class="audit-evidence-item">
        <strong>{{ log.action }}</strong>
        <span>{{ log.actor }} · {{ log.targetType }} · {{ log.targetId }}</span>
        <small>{{ new Date(log.timestamp).toLocaleString('zh-CN') }}</small>
        <pre>{{ JSON.stringify(log.metadata, null, 2) }}</pre>
      </article>
    </div>
  </section>
</template>
