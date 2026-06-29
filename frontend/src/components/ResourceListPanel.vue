<script setup lang="ts">
import type { ResourceDocument } from '../types'
import StatusBadge from './StatusBadge.vue'

defineProps<{ resources: ResourceDocument[]; selectedResourceId: string }>()
defineEmits<{ select: [resource: ResourceDocument] }>()
</script>

<template>
  <section class="prompt-resource-list-panel">
    <header>
      <p class="eyebrow">Resource 资源</p>
      <h2>Resource Library</h2>
    </header>
    <div class="prompt-resource-list">
      <button
        v-for="resource in resources"
        :key="resource.id"
        type="button"
        class="prompt-resource-row"
        :class="{ selected: selectedResourceId === resource.id }"
        @click="$emit('select', resource)"
      >
        <span>
          <strong>{{ resource.name }}</strong>
          <small>{{ resource.type }} · {{ resource.tags.join(', ') }}</small>
        </span>
        <span class="row-metrics">
          <StatusBadge :status="resource.status" />
          <strong>{{ resource.referenceCount }}</strong>
          <small>refs</small>
        </span>
      </button>
    </div>
  </section>
</template>
