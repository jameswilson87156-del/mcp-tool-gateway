<script setup lang="ts">
import type { ResourceDetail } from '../types'
import StatusBadge from './StatusBadge.vue'

defineProps<{ detail: ResourceDetail | null }>()
</script>

<template>
  <section class="resource-detail-panel">
    <template v-if="detail">
      <header class="detail-panel-header">
        <div>
          <p class="eyebrow">Resource Detail</p>
          <h2>{{ detail.resource.name }}</h2>
        </div>
        <StatusBadge :status="detail.resource.status" />
      </header>
      <p class="muted">{{ detail.resource.description }}</p>
      <div class="info-grid compact-grid">
        <div>
          <span>Type</span>
          <strong>{{ detail.resource.type }}</strong>
        </div>
        <div>
          <span>Updated At</span>
          <strong>{{ new Date(detail.resource.updatedAt).toLocaleString('zh-CN') }}</strong>
        </div>
        <div>
          <span>Linked Tools</span>
          <strong>{{ detail.linkedTools.join(', ') }}</strong>
        </div>
        <div>
          <span>Related Prompts</span>
          <strong>{{ detail.relatedPrompts.join(', ') }}</strong>
        </div>
      </div>
      <h3>Content Summary</h3>
      <p class="resource-summary">{{ detail.contentSummary }}</p>
      <div class="scope-list">
        <span v-for="tag in detail.resource.tags" :key="tag" class="chip">{{ tag }}</span>
      </div>
    </template>
    <div v-else class="empty-state">请选择一个 Resource。</div>
  </section>
</template>
