<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { ResourceDetail, ResourceType, ResourceUpsertInput } from '../types'
import ValidationMessageList from './ValidationMessageList.vue'

const props = defineProps<{ open: boolean; mode: 'create' | 'edit'; detail: ResourceDetail | null; saving: boolean; apiErrors: string[] }>()
const emit = defineEmits<{ close: []; save: [payload: ResourceUpsertInput] }>()

const localErrors = ref<string[]>([])
const form = reactive({
  name: '',
  type: 'DOCUMENT' as ResourceType,
  description: '',
  contentSummary: '',
  schemaPreview: '',
  markdownPreview: '',
  tagsJson: '[]',
  linkedToolsJson: '[]',
  relatedPromptsJson: '[]'
})

watch(
  () => [props.open, props.mode, props.detail?.resource.id],
  resetForm,
  { immediate: true }
)

function resetForm() {
  const resource = props.detail?.resource
  form.name = props.mode === 'create' ? '' : resource?.name ?? ''
  form.type = (props.mode === 'create' ? 'DOCUMENT' : resource?.type ?? 'DOCUMENT') as ResourceType
  form.description = props.mode === 'create' ? '' : resource?.description ?? ''
  form.contentSummary = props.mode === 'create' ? '' : resource?.contentSummary ?? ''
  form.schemaPreview = props.mode === 'create' ? '' : resource?.schemaPreview ?? ''
  form.markdownPreview = props.mode === 'create' ? '## Resource Draft' : resource?.markdownPreview ?? ''
  form.tagsJson = JSON.stringify(props.mode === 'create' ? [] : resource?.tags ?? [], null, 2)
  form.linkedToolsJson = JSON.stringify(props.mode === 'create' ? [] : resource?.linkedTools ?? [], null, 2)
  form.relatedPromptsJson = JSON.stringify(props.mode === 'create' ? [] : resource?.relatedPrompts ?? [], null, 2)
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
      type: form.type,
      description: form.description,
      contentSummary: form.contentSummary,
      schemaPreview: form.schemaPreview,
      markdownPreview: form.markdownPreview,
      tags: parseStringArray(form.tagsJson, 'tags'),
      linkedTools: parseStringArray(form.linkedToolsJson, 'linkedTools'),
      relatedPrompts: parseStringArray(form.relatedPromptsJson, 'relatedPrompts'),
      status: 'DRAFT'
    })
  } catch (error) {
    localErrors.value = [error instanceof Error ? error.message : 'Resource 表单校验失败']
  }
}
</script>

<template>
  <aside v-if="open" class="edit-drawer">
    <header class="edit-drawer-header">
      <div>
        <p class="eyebrow">{{ mode === 'create' ? 'New Resource' : 'Edit Resource' }}</p>
        <h2>{{ mode === 'create' ? '创建 Resource 草稿' : form.name }}</h2>
      </div>
      <button type="button" class="icon-button" @click="$emit('close')">×</button>
    </header>
    <ValidationMessageList :messages="[...localErrors, ...apiErrors]" />
    <div class="edit-form-grid">
      <label class="field-label">Name<input v-model="form.name" /></label>
      <label class="field-label">Type
        <select v-model="form.type">
          <option>DOCUMENT</option>
          <option>API_SPEC</option>
          <option>DB_SCHEMA</option>
          <option>BUSINESS_RULE</option>
          <option>POLICY</option>
        </select>
      </label>
      <label class="field-label wide">Description<input v-model="form.description" /></label>
      <label class="field-label wide">Content Summary<textarea v-model="form.contentSummary" class="mono-textarea" /></label>
      <label class="field-label wide">Schema Preview<textarea v-model="form.schemaPreview" class="mono-textarea" /></label>
      <label class="field-label wide">Markdown Preview<textarea v-model="form.markdownPreview" class="mono-textarea" /></label>
      <label class="field-label">Tags JSON<textarea v-model="form.tagsJson" class="mono-textarea" /></label>
      <label class="field-label">Linked Tools JSON<textarea v-model="form.linkedToolsJson" class="mono-textarea" /></label>
      <label class="field-label wide">Related Prompts JSON<textarea v-model="form.relatedPromptsJson" class="mono-textarea" /></label>
    </div>
    <footer class="edit-drawer-footer">
      <button type="button" class="secondary-button compact-button" @click="$emit('close')">Cancel</button>
      <button type="button" class="primary-button compact-button" :disabled="saving" @click="saveDraft">
        {{ saving ? 'Saving' : 'Save Draft' }}
      </button>
    </footer>
  </aside>
</template>
