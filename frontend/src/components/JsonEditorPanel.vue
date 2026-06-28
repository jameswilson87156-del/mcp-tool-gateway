<script setup lang="ts">
defineProps<{ modelValue: string; error?: string }>()
defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <div class="json-editor-wrap">
    <div class="editor-tabs">
      <button type="button" class="active">参数</button>
      <button type="button">Headers</button>
      <button type="button">权限上下文</button>
      <button type="button" class="ghost">格式化</button>
    </div>
    <textarea
      class="json-editor"
      spellcheck="false"
      :value="modelValue"
      @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
    />
    <p v-if="error" class="error-line">{{ error }}</p>
  </div>
</template>
