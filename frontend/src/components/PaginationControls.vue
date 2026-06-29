<script setup lang="ts">
import type { PageResponse } from '../types'

const props = defineProps<{ page: PageResponse<unknown> }>()
const emit = defineEmits<{ change: [page: number] }>()

function previous() {
  if (props.page.page > 0) {
    emit('change', props.page.page - 1)
  }
}

function next() {
  if (props.page.page + 1 < props.page.totalPages) {
    emit('change', props.page.page + 1)
  }
}
</script>

<template>
  <nav class="pagination-controls" aria-label="pagination">
    <span>共 {{ page.total }} 条</span>
    <button type="button" :disabled="page.page <= 0" @click="previous">Previous</button>
    <strong>{{ page.totalPages === 0 ? 0 : page.page + 1 }} / {{ page.totalPages }}</strong>
    <button type="button" :disabled="page.page + 1 >= page.totalPages" @click="next">Next</button>
  </nav>
</template>
