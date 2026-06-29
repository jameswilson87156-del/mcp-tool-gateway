<script setup lang="ts">
import type { PromptRenderResponse } from '../types'

defineProps<{ result: PromptRenderResponse | null; source: 'api' | 'demo-fallback' }>()
</script>

<template>
  <section class="prompt-render-preview">
    <header>
      <p class="eyebrow">Render Preview</p>
      <h2>Rendered Prompt</h2>
      <p class="source-note" :class="{ fallback: source === 'demo-fallback' }">
        {{ result ? (source === 'api' ? '后端 API render' : 'demo fallback render') : '等待 render 调用' }}
      </p>
    </header>
    <div v-if="result?.validationErrors.length" class="validation-box">
      <strong>Validation Error</strong>
      <p v-for="error in result.validationErrors" :key="error">{{ error }}</p>
    </div>
    <pre class="schema-code prompt-render-code">{{ result?.renderedPrompt || '等待输入变量并调用 render 接口。' }}</pre>
  </section>
</template>
