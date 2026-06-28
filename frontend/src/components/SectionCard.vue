<script setup lang="ts">
defineProps<{ title?: string; eyebrow?: string }>()
</script>

<template>
  <section class="section-card">
    <header v-if="title || eyebrow" class="section-card-header">
      <div>
        <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
        <h2 v-if="title">{{ title }}</h2>
      </div>
      <slot name="action" />
    </header>
    <slot />
  </section>
</template>
