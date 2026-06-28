<script setup lang="ts">
import type { ToolCallRecord } from '../types'
import StatusBadge from './StatusBadge.vue'

defineProps<{ call: ToolCallRecord | null; source: 'api' | 'demo-fallback' }>()
</script>

<template>
  <div class="response-preview">
    <div class="response-header">
      <h2>响应预览</h2>
      <StatusBadge v-if="call" :status="call.status" />
    </div>
    <pre class="response-code">{{ JSON.stringify(call?.response ?? { message: '等待执行 Tool call' }, null, 2) }}</pre>
    <div class="response-meta">
      <div>
        <span>request_id</span>
        <strong>{{ call?.id ?? '尚未生成' }}</strong>
      </div>
      <div>
        <span>Provider</span>
        <strong>{{ call?.provider ?? 'OpenAI-compatible' }}</strong>
      </div>
      <div>
        <span>Latency</span>
        <strong class="success">{{ call?.latency ?? '待执行' }}</strong>
      </div>
    </div>
    <p class="source-note" :class="{ fallback: source === 'demo-fallback' }">
      {{ source === 'api' ? '数据来源：后端真实 API' : '数据来源：集中式 demo fallback，后端未连接' }}
    </p>
  </div>
</template>
