<script setup lang="ts">
import type { ResourceDetail } from '../types'

defineProps<{ detail: ResourceDetail | null; saving: boolean }>()
defineEmits<{ new: []; edit: []; publish: []; archive: [] }>()
</script>

<template>
  <section class="workspace-action-bar">
    <div>
      <p class="eyebrow">Resource Governance</p>
      <strong>编辑、保存草稿、发布和归档上下文资源</strong>
    </div>
    <div class="workspace-actions">
      <button type="button" class="secondary-button compact-button" :disabled="saving" @click="$emit('new')">New Resource</button>
      <button type="button" class="secondary-button compact-button" :disabled="!detail || saving" @click="$emit('edit')">Edit</button>
      <button type="button" class="primary-button compact-button" :disabled="!detail || saving" @click="$emit('publish')">Publish</button>
      <button type="button" class="danger-button compact-button" :disabled="!detail || saving" @click="$emit('archive')">Archive</button>
    </div>
  </section>
</template>
