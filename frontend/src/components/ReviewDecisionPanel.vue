<script setup lang="ts">
import { ref, watch } from 'vue'
import type { ToolCallReview } from '../types'

const props = defineProps<{ review: ToolCallReview | null; busy: boolean }>()
const emit = defineEmits<{ decide: [action: 'approve' | 'reject' | 'request-changes', comment: string] }>()

const comment = ref('已核对权限范围、风险说明与 Trace Evidence。')

watch(
  () => props.review?.id,
  () => {
    comment.value = '已核对权限范围、风险说明与 Trace Evidence。'
  }
)
</script>

<template>
  <section class="decision-panel">
    <p class="eyebrow">Decision</p>
    <h2>审批决策</h2>
    <label class="field-label">
      Reviewer
      <input value="reviewer.li" readonly />
    </label>
    <label class="field-label">
      审批意见
      <textarea v-model="comment" :disabled="!review || busy" />
    </label>
    <div class="decision-time">
      <span>Decision Time</span>
      <strong>{{ new Date().toLocaleString('zh-CN') }}</strong>
    </div>
    <div class="decision-actions">
      <button type="button" class="primary-button" :disabled="!review || busy" @click="emit('decide', 'approve', comment)">Approve</button>
      <button type="button" class="danger-button" :disabled="!review || busy" @click="emit('decide', 'reject', comment)">Reject</button>
      <button type="button" class="secondary-button" :disabled="!review || busy" @click="emit('decide', 'request-changes', comment)">Request Changes</button>
    </div>
  </section>
</template>
