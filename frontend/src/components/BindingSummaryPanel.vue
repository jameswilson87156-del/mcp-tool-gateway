<script setup lang="ts">
import type { PromptTemplate, ResourceDocument, ToolDefinition } from '../types'
import RiskBadge from './RiskBadge.vue'

defineProps<{ prompts: PromptTemplate[]; resources: ResourceDocument[]; tools: ToolDefinition[] }>()
</script>

<template>
  <section class="binding-summary-panel">
    <header class="page-title-row compact-title-row">
      <div>
        <p class="eyebrow">Tool Binding</p>
        <h2>Prompt / Resource 绑定关系</h2>
      </div>
    </header>
    <div class="binding-table">
      <article v-for="tool in tools" :key="tool.id" class="binding-row">
        <strong>{{ tool.name }}</strong>
        <span>{{ prompts.find((prompt) => prompt.relatedTools.includes(tool.id))?.name ?? '未绑定 Prompt' }}</span>
        <span>{{ resources.find((resource) => resource.linkedTools.includes(tool.id))?.name ?? '未绑定 Resource' }}</span>
        <RiskBadge :risk="tool.riskLevel" />
        <span>{{ tool.approvalRequired ? 'Human Review' : 'Auto Pass' }}</span>
        <small>{{ new Date(tool.updatedAt).toLocaleString('zh-CN') }}</small>
      </article>
    </div>
  </section>
</template>
