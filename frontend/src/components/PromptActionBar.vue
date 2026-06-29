<script setup lang="ts">
import type { PromptDetail } from '../types'

defineProps<{ detail: PromptDetail | null; saving: boolean }>()
defineEmits<{ new: []; edit: []; publish: []; archive: [] }>()
</script>

<template>
  <section class="workspace-action-bar">
    <div>
      <p class="eyebrow">Prompt Governance</p>
      <strong>编辑、保存草稿、发布和归档 Prompt 模板</strong>
    </div>
    <div class="workspace-actions">
      <button type="button" class="secondary-button compact-button" :disabled="saving" @click="$emit('new')">New Prompt</button>
      <button type="button" class="secondary-button compact-button" :disabled="!detail || saving" @click="$emit('edit')">Edit</button>
      <button type="button" class="primary-button compact-button" :disabled="!detail || saving" @click="$emit('publish')">Publish</button>
      <button type="button" class="danger-button compact-button" :disabled="!detail || saving" @click="$emit('archive')">Archive</button>
    </div>
  </section>
</template>
