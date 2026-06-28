<script setup lang="ts">
import SidebarNav from './SidebarNav.vue'
import TopBar from './TopBar.vue'

defineProps<{ activePage: string }>()
defineEmits<{ 'update:activePage': [page: string] }>()
</script>

<template>
  <div class="app-shell">
    <SidebarNav :active-page="activePage" @update:active-page="$emit('update:activePage', $event)" />
    <main class="main-surface">
      <TopBar />
      <section class="page-surface">
        <slot />
      </section>
    </main>
  </div>
</template>
