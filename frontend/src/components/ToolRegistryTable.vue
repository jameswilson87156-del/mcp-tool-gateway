<script setup lang="ts">
import type { ToolDefinition } from '../types'
import RiskBadge from './RiskBadge.vue'

defineProps<{ tools: ToolDefinition[]; selectedId: string }>()
defineEmits<{ select: [tool: ToolDefinition] }>()
</script>

<template>
  <div class="registry-table">
    <button
      v-for="tool in tools"
      :key="tool.id"
      type="button"
      class="registry-row"
      :class="{ selected: selectedId === tool.id }"
      @click="$emit('select', tool)"
    >
      <span class="registry-main">
        <strong>{{ tool.name }}</strong>
        <small>{{ tool.description }}</small>
      </span>
      <span class="registry-provider">{{ tool.provider }}</span>
      <span class="registry-version">{{ tool.version }}</span>
      <RiskBadge :risk="tool.riskLevel" />
      <span class="badge" :class="tool.approvalRequired ? 'status-pending-review' : 'status-success'">
        {{ tool.approvalRequired ? '需审批' : '可直连' }}
      </span>
    </button>
  </div>
</template>
