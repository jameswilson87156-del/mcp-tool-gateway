<script setup lang="ts">
defineProps<{ title: string }>()
</script>

<template>
  <div class="placeholder-page">
    <p class="eyebrow">MCP Tool Gateway</p>
    <h1>{{ title }}</h1>
    <p>该模块将在 P2 实现，当前已完成核心调用链路。</p>
    <div class="placeholder-grid">
      <div>权限边界</div>
      <div>Trace Evidence</div>
      <div>Audit Log</div>
    </div>
  </div>
</template>
