<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { demoCurrentUser } from '../data/demo'
import { getCurrentUser } from '../services/api'
import type { DemoUserProfile } from '../types'
import UserMenu from './UserMenu.vue'

const user = ref<DemoUserProfile>(demoCurrentUser)
const userSource = ref<'api' | 'demo-fallback'>('demo-fallback')

onMounted(async () => {
  const result = await getCurrentUser()
  user.value = result.data
  userSource.value = result.source
})
</script>

<template>
  <header class="topbar">
    <label class="search-box">
      <span aria-hidden="true">⌕</span>
      <input type="search" placeholder="搜索 Tool、Provider、Trace" />
    </label>
    <div class="topbar-actions">
      <span class="environment-pill">Local Demo</span>
      <span class="provider-status">Provider 状态 <strong>12/12 正常</strong></span>
      <button type="button" class="icon-button" aria-label="通知">!</button>
      <UserMenu :user="user" :source="userSource" />
    </div>
  </header>
</template>
