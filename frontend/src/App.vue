<script setup lang="ts">
import { ref } from 'vue'
import AppShell from './components/AppShell.vue'
import ToolCallWorkbenchView from './views/ToolCallWorkbenchView.vue'
import ToolRegistryView from './views/ToolRegistryView.vue'
import HumanReviewCenterView from './views/HumanReviewCenterView.vue'
import TraceEvidenceView from './views/TraceEvidenceView.vue'
import PromptResourceView from './views/PromptResourceView.vue'
import PlaceholderView from './views/PlaceholderView.vue'

const activePage = ref('Tool Call 工作台')
</script>

<template>
  <AppShell v-model:active-page="activePage">
    <ToolCallWorkbenchView v-if="activePage === 'Tool Call 工作台'" />
    <ToolRegistryView v-else-if="activePage === 'Tool Registry'" />
    <HumanReviewCenterView v-else-if="activePage === 'Human Review'" />
    <TraceEvidenceView v-else-if="activePage === 'Trace Evidence'" />
    <PromptResourceView v-else-if="activePage === '提示词工作室' || activePage === '资源中心'" :initial-tab="activePage === '资源中心' ? 'resource' : 'prompt'" />
    <PlaceholderView v-else :title="activePage" />
  </AppShell>
</template>
