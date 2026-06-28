<script setup lang="ts">
import type { RiskLevel } from '../types'

defineProps<{ risk: RiskLevel }>()

const label: Record<RiskLevel, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
  BLOCKED: '阻断'
}
</script>

<template>
  <span class="badge" :class="`risk-${risk.toLowerCase()}`">风险 {{ label[risk] }}</span>
</template>
