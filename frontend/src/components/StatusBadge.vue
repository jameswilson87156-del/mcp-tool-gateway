<script setup lang="ts">
import type { CallStatus } from '../types'

defineProps<{ status: CallStatus | string }>()

const label: Record<string, string> = {
  SUCCESS: '成功',
  PENDING_REVIEW: '待审批',
  BLOCKED: '已阻断',
  FAILED: '失败',
  RUNNING: '运行中',
  APPROVED: '已批准',
  REJECTED: '已拒绝',
  CHANGES_REQUESTED: '需补充',
  DRAFT: '草稿',
  ACTIVE: '启用',
  PUBLISHED: '已发布',
  SYNCED: '已同步',
  ARCHIVED: '已归档'
}
</script>

<template>
  <span class="badge" :class="`status-${String(status).toLowerCase().replace(/_/g, '-')}`">
    {{ label[status] ?? status }}
  </span>
</template>
