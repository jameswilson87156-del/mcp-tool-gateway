<script setup lang="ts">
import type { ToolDefinition } from '../types'
import RiskBadge from './RiskBadge.vue'

defineProps<{ tools: ToolDefinition[]; selectedId: string }>()
const emit = defineEmits<{ select: [tool: ToolDefinition] }>()
</script>

<template>
  <div class="tool-selector">
    <label class="small-search">
      <span>⌕</span>
      <input placeholder="搜索 Tool 名称或描述" />
    </label>
    <button
      v-for="tool in tools"
      :key="tool.id"
      type="button"
      class="tool-row"
      :class="{ selected: selectedId === tool.id }"
      @click="emit('select', tool)"
    >
      <span class="radio-dot" aria-hidden="true"></span>
      <span class="tool-copy">
        <strong>{{ tool.name }}</strong>
        <small>Provider: {{ tool.provider }} · {{ tool.version }}</small>
      </span>
      <RiskBadge :risk="tool.riskLevel" />
    </button>
  </div>
</template>
