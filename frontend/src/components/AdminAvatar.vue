<script setup lang="ts">
defineProps<{ initials?: string }>()
</script>

<template>
  <span class="admin-avatar" aria-hidden="true">
    <svg viewBox="0 0 48 48" role="img" focusable="false">
      <circle class="avatar-node avatar-node-primary" cx="15" cy="15" r="3.5" />
      <circle class="avatar-node" cx="33" cy="14" r="2.8" />
      <circle class="avatar-node" cx="35" cy="33" r="3.2" />
      <path class="avatar-link" d="M18 16 L30 15 M33 17 L35 30 M18 18 L33 31" />
    </svg>
    <span>{{ initials ?? 'WZL' }}</span>
  </span>
</template>
