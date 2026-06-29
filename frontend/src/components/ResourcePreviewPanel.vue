<script setup lang="ts">
import type { ResourceDetail } from '../types'

defineProps<{ detail: ResourceDetail | null }>()
</script>

<template>
  <section class="resource-preview-panel">
    <p class="eyebrow">Resource Preview</p>
    <h2>Schema / Markdown</h2>
    <pre class="schema-code resource-preview-code">{{
      detail?.schemaPreview || detail?.markdownPreview || '等待选择 Resource。'
    }}</pre>
  </section>
</template>
