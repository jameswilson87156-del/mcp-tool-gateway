<script setup lang="ts">
import type { ToolDefinition } from '../types'
import RiskBadge from './RiskBadge.vue'

defineProps<{ tool: ToolDefinition | null }>()
</script>

<template>
  <section class="detail-panel">
    <header class="detail-panel-header">
      <div>
        <p class="eyebrow">Permission Scope</p>
        <h2>权限范围与风险</h2>
      </div>
      <RiskBadge v-if="tool" :risk="tool.riskLevel" />
    </header>
    <div v-if="tool" class="info-grid">
      <div>
        <span>Category</span>
        <strong>{{ tool.category }}</strong>
      </div>
      <div>
        <span>Status</span>
        <strong>{{ tool.status }}</strong>
      </div>
      <div>
        <span>Approval Required</span>
        <strong>{{ tool.approvalRequired ? '是' : '否' }}</strong>
      </div>
      <div>
        <span>Recent Calls</span>
        <strong>{{ tool.recentCallCount }}</strong>
      </div>
    </div>
    <div v-if="tool" class="scope-list">
      <span v-for="scope in tool.permissionScopes" :key="scope" class="chip">{{ scope }}</span>
    </div>
    <p v-if="tool" class="muted">更新时间：{{ new Date(tool.updatedAt).toLocaleString('zh-CN') }}</p>
  </section>
</template>
