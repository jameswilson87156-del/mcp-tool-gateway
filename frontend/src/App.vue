<script setup lang="ts">
import { ref } from 'vue'
import AppShell from './components/AppShell.vue'
import ToolCallWorkbenchView from './views/ToolCallWorkbenchView.vue'
import PlaceholderView from './views/PlaceholderView.vue'

const activePage = ref('Tool Call 工作台')
</script>

<template>
  <AppShell v-model:active-page="activePage">
    <ToolCallWorkbenchView v-if="activePage === 'Tool Call 工作台'" />
    <PlaceholderView v-else :title="activePage" />
  </AppShell>
</template>
