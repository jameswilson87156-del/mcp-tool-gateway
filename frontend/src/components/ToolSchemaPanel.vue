<script setup lang="ts">
import type { ToolDefinition } from '../types'

defineProps<{ tool: ToolDefinition | null }>()
</script>

<template>
  <section class="detail-panel">
    <header class="detail-panel-header">
      <div>
        <p class="eyebrow">Tool Schema</p>
        <h2>{{ tool?.name ?? '请选择 Tool' }}</h2>
      </div>
      <span v-if="tool" class="chip active">{{ tool.version }}</span>
    </header>
    <pre class="schema-code">{{ JSON.stringify(tool?.schema ?? { message: '等待选择 Tool' }, null, 2) }}</pre>
    <div v-if="tool" class="parameter-list">
      <article v-for="parameter in tool.parameters" :key="parameter.name" class="parameter-item">
        <div>
          <strong>{{ parameter.name }}</strong>
          <small>{{ parameter.description }}</small>
        </div>
        <span>{{ parameter.type }}{{ parameter.required ? ' · required' : '' }}</span>
      </article>
    </div>
  </section>
</template>
