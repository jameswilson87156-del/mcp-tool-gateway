<script setup lang="ts">
import type { PromptDetail, ResourceDetail } from '../types'

defineProps<{ promptDetail: PromptDetail | null; resourceDetail: ResourceDetail | null; mode: 'prompt' | 'resource' | 'all' }>()

function format(item: Record<string, unknown>) {
  return JSON.stringify(item, null, 2)
}
</script>

<template>
  <section class="prompt-resource-activity">
    <p class="eyebrow">Recent Usage</p>
    <h2>调用与审计证据</h2>
    <div class="activity-list">
      <template v-if="mode !== 'resource'">
        <article v-for="item in promptDetail?.recentUsage ?? []" :key="format(item)">
          <strong>{{ item.tool }}</strong>
          <small>{{ item.timestamp }}</small>
          <pre>{{ format(item) }}</pre>
        </article>
      </template>
      <template v-if="mode !== 'prompt'">
        <article v-for="item in resourceDetail?.recentReferences ?? []" :key="format(item)">
          <strong>{{ item.tool }}</strong>
          <small>{{ item.timestamp }}</small>
          <pre>{{ format(item) }}</pre>
        </article>
      </template>
      <article v-for="log in [...(promptDetail?.auditLogs ?? []), ...(resourceDetail?.auditLogs ?? [])]" :key="log.id">
        <strong>{{ log.action }}</strong>
        <small>{{ log.actor }} · {{ new Date(log.timestamp).toLocaleString('zh-CN') }}</small>
        <pre>{{ JSON.stringify(log.metadata, null, 2) }}</pre>
      </article>
    </div>
  </section>
</template>
