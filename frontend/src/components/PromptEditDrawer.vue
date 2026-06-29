<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { PromptDetail, PromptUpsertInput } from '../types'
import ValidationMessageList from './ValidationMessageList.vue'

const props = defineProps<{ open: boolean; mode: 'create' | 'edit'; detail: PromptDetail | null; saving: boolean; apiErrors: string[] }>()
const emit = defineEmits<{ close: []; save: [payload: PromptUpsertInput] }>()

const localErrors = ref<string[]>([])
const form = reactive({
  name: '',
  description: '',
  category: '',
  templateContent: '',
  variablesJson: '[]',
  usageScope: '',
  relatedToolsJson: '[]',
  status: 'DRAFT'
})

watch(
  () => [props.open, props.mode, props.detail?.prompt.id],
  resetForm,
  { immediate: true }
)

function resetForm() {
  const prompt = props.detail?.prompt
  form.name = props.mode === 'create' ? '' : prompt?.name ?? ''
  form.description = props.mode === 'create' ? '' : prompt?.description ?? ''
  form.category = props.mode === 'create' ? 'General' : prompt?.category ?? 'General'
  form.templateContent = props.mode === 'create' ? '请基于 {{input}} 输出结构化结果。' : props.detail?.templateContent ?? ''
  form.variablesJson = JSON.stringify(props.mode === 'create' ? ['input'] : props.detail?.variables ?? [], null, 2)
  form.usageScope = props.mode === 'create' ? 'Agent demo scope' : prompt?.usageScope ?? ''
  form.relatedToolsJson = JSON.stringify(props.mode === 'create' ? [] : prompt?.relatedTools ?? [], null, 2)
  form.status = props.mode === 'create' ? 'DRAFT' : prompt?.status ?? 'DRAFT'
  localErrors.value = []
}

function parseStringArray(value: string, label: string) {
  const parsed = JSON.parse(value)
  if (!Array.isArray(parsed) || parsed.some((item) => typeof item !== 'string')) {
    throw new Error(`${label} 必须是字符串数组 JSON`)
  }
  return parsed.map((item) => item.trim()).filter(Boolean)
}

function saveDraft() {
  localErrors.value = []
  try {
    emit('save', {
      name: form.name,
      description: form.description,
      category: form.category,
      templateContent: form.templateContent,
      variables: parseStringArray(form.variablesJson, 'variables'),
      usageScope: form.usageScope,
      relatedTools: parseStringArray(form.relatedToolsJson, 'relatedTools'),
      status: 'DRAFT'
    })
  } catch (error) {
    localErrors.value = [error instanceof Error ? error.message : 'Prompt 表单校验失败']
  }
}
</script>

<template>
  <aside v-if="open" class="edit-drawer">
    <header class="edit-drawer-header">
      <div>
        <p class="eyebrow">{{ mode === 'create' ? 'New Prompt' : 'Edit Prompt' }}</p>
        <h2>{{ mode === 'create' ? '创建 Prompt 草稿' : form.name }}</h2>
      </div>
      <button type="button" class="icon-button" @click="$emit('close')">×</button>
    </header>
    <ValidationMessageList :messages="[...localErrors, ...apiErrors]" />
    <div class="edit-form-grid">
      <label class="field-label">Name<input v-model="form.name" /></label>
      <label class="field-label">Category<input v-model="form.category" /></label>
      <label class="field-label wide">Description<input v-model="form.description" /></label>
      <label class="field-label wide">Usage Scope<input v-model="form.usageScope" /></label>
      <label class="field-label wide">Template Content<textarea v-model="form.templateContent" class="mono-textarea tall" /></label>
      <label class="field-label">Variables JSON<textarea v-model="form.variablesJson" class="mono-textarea" /></label>
      <label class="field-label">Related Tools JSON<textarea v-model="form.relatedToolsJson" class="mono-textarea" /></label>
    </div>
    <footer class="edit-drawer-footer">
      <button type="button" class="secondary-button compact-button" @click="$emit('close')">Cancel</button>
      <button type="button" class="primary-button compact-button" :disabled="saving" @click="saveDraft">
        {{ saving ? 'Saving' : 'Save Draft' }}
      </button>
    </footer>
  </aside>
</template>
