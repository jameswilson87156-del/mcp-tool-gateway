<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import type { DemoUserProfile } from '../types'
import AdminAvatar from './AdminAvatar.vue'

defineProps<{ user: DemoUserProfile; source: 'api' | 'demo-fallback' }>()

const open = ref(false)
const menuRoot = ref<HTMLElement | null>(null)

function toggleMenu() {
  open.value = !open.value
}

function closeMenu() {
  open.value = false
}

function handleDocumentClick(event: MouseEvent) {
  if (!menuRoot.value?.contains(event.target as Node)) {
    closeMenu()
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeMenu()
  }
}

document.addEventListener('click', handleDocumentClick)
document.addEventListener('keydown', handleKeydown)

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div ref="menuRoot" class="user-menu" :data-source="source">
    <button type="button" class="user-trigger" :aria-expanded="open" aria-haspopup="menu" @click.stop="toggleMenu">
      <AdminAvatar />
      <span class="user-trigger-copy">
        <strong>{{ user.displayName }}</strong>
        <span>{{ user.role }} · {{ user.environment }}</span>
      </span>
      <span class="chevron" aria-hidden="true">⌄</span>
    </button>

    <div v-if="open" class="user-menu-panel" role="menu">
      <div class="user-menu-header">
        <AdminAvatar />
        <div>
          <strong>{{ user.displayName }}</strong>
          <span>{{ user.username }}</span>
        </div>
      </div>
      <dl class="user-menu-list">
        <div>
          <dt>Role</dt>
          <dd>{{ user.role }}</dd>
        </div>
        <div>
          <dt>Environment</dt>
          <dd>{{ user.environment }}</dd>
        </div>
        <div>
          <dt>Mode</dt>
          <dd>{{ user.modeLabel }}</dd>
        </div>
        <div>
          <dt>Gateway</dt>
          <dd>{{ user.productLabel }}</dd>
        </div>
      </dl>
      <button type="button" class="disabled-menu-action" disabled>{{ user.signOutLabel }}</button>
    </div>
  </div>
</template>
