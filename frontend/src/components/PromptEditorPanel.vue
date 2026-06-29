<script setup lang="ts">
import type { PromptDetail } from '../types'
import StatusBadge from './StatusBadge.vue'

defineProps<{ detail: PromptDetail | null }>()
</script>

<template>
  <section class="prompt-editor-panel">
    <template v-if="detail">
      <header class="detail-panel-header">
        <div>
          <p class="eyebrow">Prompt Detail</p>
          <h2>{{ detail.prompt.name }}</h2>
        </div>
        <StatusBadge :status="detail.status" />
      </header>
      <p class="muted">{{ detail.prompt.description }}</p>
      <div class="info-grid compact-grid">
        <div>
          <span>Version</span>
          <strong>{{ detail.version }}</strong>
        </div>
        <div>
          <span>Category</span>
          <strong>{{ detail.prompt.category }}</strong>
        </div>
        <div>
          <span>Usage Scope</span>
          <strong>{{ detail.usageScope }}</strong>
        </div>
        <div>
          <span>Related Tools</span>
          <strong>{{ detail.relatedTools.join(', ') }}</strong>
        </div>
      </div>
      <h3>Template Content</h3>
      <pre class="schema-code prompt-template-code">{{ detail.templateContent }}</pre>
      <h3>Variables</h3>
      <div class="scope-list">
        <span v-for="variable in detail.variables" :key="variable" class="chip">{{ variable }}</span>
      </div>
    </template>
    <div v-else class="empty-state">请选择一个 Prompt 模板。</div>
  </section>
</template>
