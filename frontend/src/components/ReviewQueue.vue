<script setup lang="ts">
import type { ToolCallRecord, ToolCallReview, ToolDefinition } from '../types'
import RiskBadge from './RiskBadge.vue'
import StatusBadge from './StatusBadge.vue'

defineProps<{
  reviews: ToolCallReview[]
  calls: ToolCallRecord[]
  tools: ToolDefinition[]
  selectedId: string
}>()
defineEmits<{ select: [review: ToolCallReview] }>()

function callFor(review: ToolCallReview, calls: ToolCallRecord[]) {
  return calls.find((call) => call.id === review.callId)
}

function toolName(review: ToolCallReview, tools: ToolDefinition[]) {
  return tools.find((tool) => tool.id === review.toolId)?.name ?? review.toolId
}
</script>

<template>
  <div class="review-queue">
    <button
      v-for="review in reviews"
      :key="review.id"
      type="button"
      class="review-row"
      :class="{ selected: selectedId === review.id }"
      @click="$emit('select', review)"
    >
      <span>
        <strong>{{ toolName(review, tools) }}</strong>
        <small>Requester: {{ callFor(review, calls)?.requester ?? 'unknown' }}</small>
      </span>
      <RiskBadge :risk="review.riskLevel" />
      <StatusBadge :status="review.status" />
      <small>{{ new Date(review.createdAt).toLocaleTimeString('zh-CN') }}</small>
    </button>
  </div>
</template>
