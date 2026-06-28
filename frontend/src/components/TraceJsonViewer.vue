<script setup lang="ts">
defineProps<{ title: string; value: unknown }>()
</script>

<template>
  <section class="trace-json-viewer">
    <h3>{{ title }}</h3>
    <pre>{{ JSON.stringify(value ?? {}, null, 2) }}</pre>
  </section>
</template>
