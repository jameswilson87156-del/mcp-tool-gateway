<script setup lang="ts">
defineProps<{ message: string; tone: 'success' | 'error' | 'info' }>()
</script>

<template>
  <div v-if="message" class="save-status-banner" :class="tone">
    {{ message }}
  </div>
</template>
