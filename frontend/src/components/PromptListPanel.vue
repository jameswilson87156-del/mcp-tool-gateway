<script setup lang="ts">
import type { PromptTemplate } from '../types'
import StatusBadge from './StatusBadge.vue'

defineProps<{ prompts: PromptTemplate[]; selectedPromptId: string }>()
defineEmits<{ select: [prompt: PromptTemplate] }>()
</script>

<template>
  <section class="prompt-resource-list-panel">
    <header>
      <p class="eyebrow">Prompt 模板</p>
      <h2>Prompt Registry</h2>
    </header>
    <div class="prompt-resource-list">
      <button
        v-for="prompt in prompts"
        :key="prompt.id"
        type="button"
        class="prompt-resource-row"
        :class="{ selected: selectedPromptId === prompt.id }"
        @click="$emit('select', prompt)"
      >
        <span>
          <strong>{{ prompt.name }}</strong>
          <small>{{ prompt.category }} · {{ prompt.version }}</small>
        </span>
        <span class="row-metrics">
          <StatusBadge :status="prompt.status" />
          <strong>{{ prompt.usageCount }}</strong>
          <small>usage</small>
        </span>
      </button>
      <div v-if="!prompts.length" class="empty-state">
        <strong>没有匹配的 Prompt</strong>
        <p>调整 Status、Category 或 keyword，或新建一个 Prompt 草稿。</p>
      </div>
    </div>
  </section>
</template>
