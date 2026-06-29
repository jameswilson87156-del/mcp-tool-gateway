<script setup lang="ts">
defineProps<{ modelValue: string; parseError: string; loading: boolean }>()
defineEmits<{ 'update:modelValue': [value: string]; render: [] }>()
</script>

<template>
  <section class="prompt-variable-panel">
    <header class="detail-panel-header">
      <div>
        <p class="eyebrow">Variable JSON</p>
        <h2>变量配置</h2>
      </div>
      <button type="button" class="primary-button compact-button" :disabled="loading" @click="$emit('render')">
        {{ loading ? 'Rendering' : 'Render Prompt' }}
      </button>
    </header>
    <textarea
      class="json-editor prompt-variable-editor"
      :value="modelValue"
      spellcheck="false"
      @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
    />
    <p v-if="parseError" class="error-line">{{ parseError }}</p>
  </section>
</template>
