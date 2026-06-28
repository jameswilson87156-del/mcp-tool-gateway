<script setup lang="ts">
const props = defineProps<{ activePage: string }>()
const emit = defineEmits<{ 'update:activePage': [page: string] }>()

const navItems = [
  ['总览', 'grid'],
  ['Tool Registry', 'box'],
  ['Tool Call 工作台', 'hammer'],
  ['提示词工作室', 'prompt'],
  ['资源中心', 'folder'],
  ['Human Review', 'review'],
  ['Trace Evidence', 'trace'],
  ['审计日志', 'audit'],
  ['RBAC / 权限', 'user'],
  ['设置', 'settings']
]
</script>

<template>
  <aside class="sidebar">
    <div class="brand">MCP Tool Gateway</div>
    <nav class="nav-list" aria-label="主导航">
      <button
        v-for="[label, icon] in navItems"
        :key="label"
        type="button"
        class="nav-item"
        :class="{ active: props.activePage === label }"
        @click="emit('update:activePage', label)"
      >
        <span class="nav-icon" aria-hidden="true">{{ icon.slice(0, 1).toUpperCase() }}</span>
        <span>{{ label }}</span>
      </button>
    </nav>
    <button type="button" class="collapse-button">收起</button>
  </aside>
</template>
