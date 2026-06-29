<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import BindingSummaryPanel from '../components/BindingSummaryPanel.vue'
import PromptActionBar from '../components/PromptActionBar.vue'
import PromptEditDrawer from '../components/PromptEditDrawer.vue'
import PromptEditorPanel from '../components/PromptEditorPanel.vue'
import PromptListPanel from '../components/PromptListPanel.vue'
import PromptRenderPreview from '../components/PromptRenderPreview.vue'
import PromptResourceActivity from '../components/PromptResourceActivity.vue'
import PromptVariablePanel from '../components/PromptVariablePanel.vue'
import ResourceActionBar from '../components/ResourceActionBar.vue'
import ResourceDetailPanel from '../components/ResourceDetailPanel.vue'
import ResourceEditDrawer from '../components/ResourceEditDrawer.vue'
import ResourceListPanel from '../components/ResourceListPanel.vue'
import ResourcePreviewPanel from '../components/ResourcePreviewPanel.vue'
import SaveStatusBanner from '../components/SaveStatusBanner.vue'
import {
  ApiRequestError,
  archivePrompt,
  archiveResource,
  createPrompt,
  createResource,
  getPromptDetail,
  getPrompts,
  getResourceDetail,
  getResources,
  getTools,
  publishPrompt,
  publishResource,
  renderPrompt,
  updatePrompt,
  updateResource
} from '../services/api'
import type {
  PromptDetail,
  PromptRenderResponse,
  PromptTemplate,
  PromptUpsertInput,
  ResourceDetail,
  ResourceDocument,
  ResourceUpsertInput,
  ToolDefinition
} from '../types'

const props = defineProps<{ initialTab: 'prompt' | 'resource' }>()

const activeTab = ref<'prompt' | 'resource' | 'binding' | 'usage'>(props.initialTab)
const prompts = ref<PromptTemplate[]>([])
const resources = ref<ResourceDocument[]>([])
const tools = ref<ToolDefinition[]>([])
const selectedPrompt = ref<PromptTemplate | null>(null)
const selectedResource = ref<ResourceDocument | null>(null)
const promptDetail = ref<PromptDetail | null>(null)
const resourceDetail = ref<ResourceDetail | null>(null)
const variableJson = ref('{}')
const parseError = ref('')
const renderResult = ref<PromptRenderResponse | null>(null)
const source = ref<'api' | 'demo-fallback'>('demo-fallback')
const renderSource = ref<'api' | 'demo-fallback'>('demo-fallback')
const loading = ref(false)
const rendering = ref(false)
const saving = ref(false)
const saveMessage = ref('')
const saveTone = ref<'success' | 'error' | 'info'>('info')
const editErrors = ref<string[]>([])
const promptDrawerOpen = ref(false)
const promptDrawerMode = ref<'create' | 'edit'>('edit')
const resourceDrawerOpen = ref(false)
const resourceDrawerMode = ref<'create' | 'edit'>('edit')

const selectedPromptId = computed(() => selectedPrompt.value?.id ?? '')
const selectedResourceId = computed(() => selectedResource.value?.id ?? '')

watch(
  () => props.initialTab,
  (tab) => {
    activeTab.value = tab
  }
)

onMounted(loadWorkspace)

async function loadWorkspace() {
  loading.value = true
  try {
    const [promptResult, resourceResult, toolResult] = await Promise.all([getPrompts(), getResources(), getTools()])
    prompts.value = promptResult.data
    resources.value = resourceResult.data
    tools.value = toolResult.data
    source.value = [promptResult.source, resourceResult.source, toolResult.source].includes('demo-fallback') ? 'demo-fallback' : 'api'
    selectedPrompt.value = prompts.value[0] ?? null
    selectedResource.value = resources.value[0] ?? null
    await Promise.all([loadPromptDetail(), loadResourceDetail()])
  } finally {
    loading.value = false
  }
}

async function refreshPromptSelection(promptId: string) {
  const promptResult = await getPrompts()
  prompts.value = promptResult.data
  selectedPrompt.value = prompts.value.find((prompt) => prompt.id === promptId) ?? prompts.value[0] ?? null
  if (selectedPrompt.value) {
    const detailResult = await getPromptDetail(selectedPrompt.value.id)
    promptDetail.value = detailResult.data
    source.value = detailResult.source === 'demo-fallback' ? 'demo-fallback' : source.value
  }
}

async function refreshResourceSelection(resourceId: string) {
  const resourceResult = await getResources()
  resources.value = resourceResult.data
  selectedResource.value = resources.value.find((resource) => resource.id === resourceId) ?? resources.value[0] ?? null
  if (selectedResource.value) {
    const detailResult = await getResourceDetail(selectedResource.value.id)
    resourceDetail.value = detailResult.data
    source.value = detailResult.source === 'demo-fallback' ? 'demo-fallback' : source.value
  }
}

async function selectPrompt(prompt: PromptTemplate) {
  selectedPrompt.value = prompt
  await loadPromptDetail()
}

async function selectResource(resource: ResourceDocument) {
  selectedResource.value = resource
  await loadResourceDetail()
}

async function loadPromptDetail() {
  if (!selectedPrompt.value) {
    promptDetail.value = null
    return
  }
  const result = await getPromptDetail(selectedPrompt.value.id)
  promptDetail.value = result.data
  source.value = result.source === 'demo-fallback' ? 'demo-fallback' : source.value
  variableJson.value = JSON.stringify(sampleVariables(result.data.variables), null, 2)
  renderResult.value = null
  parseError.value = ''
}

async function loadResourceDetail() {
  if (!selectedResource.value) {
    resourceDetail.value = null
    return
  }
  const result = await getResourceDetail(selectedResource.value.id)
  resourceDetail.value = result.data
  source.value = result.source === 'demo-fallback' ? 'demo-fallback' : source.value
}

async function handleRender() {
  if (!selectedPrompt.value) return
  parseError.value = ''
  let variables: Record<string, unknown>
  try {
    variables = JSON.parse(variableJson.value) as Record<string, unknown>
  } catch {
    parseError.value = 'Variable JSON 格式无效，请检查引号、逗号和括号。'
    return
  }
  rendering.value = true
  try {
    const result = await renderPrompt(selectedPrompt.value.id, variables)
    renderResult.value = result.data
    renderSource.value = result.source
    if (result.source === 'api') {
      const refreshed = await getPromptDetail(selectedPrompt.value.id)
      promptDetail.value = refreshed.data
    }
  } finally {
    rendering.value = false
  }
}

function openNewPrompt() {
  promptDrawerMode.value = 'create'
  editErrors.value = []
  promptDrawerOpen.value = true
}

function openEditPrompt() {
  promptDrawerMode.value = 'edit'
  editErrors.value = []
  promptDrawerOpen.value = true
}

async function savePromptDraft(payload: PromptUpsertInput) {
  saving.value = true
  editErrors.value = []
  try {
    const result = promptDrawerMode.value === 'create'
      ? await createPrompt(payload)
      : await updatePrompt(selectedPrompt.value?.id ?? '', payload)
    promptDetail.value = result.data
    selectedPrompt.value = result.data.prompt
    source.value = result.source
    await refreshPromptSelection(result.data.prompt.id)
    promptDrawerOpen.value = false
    setSaveStatus('Prompt 草稿已保存。', 'success')
  } catch (error) {
    editErrors.value = [messageFromError(error)]
    setSaveStatus('Prompt 保存失败。', 'error')
  } finally {
    saving.value = false
  }
}

async function handlePublishPrompt() {
  if (!selectedPrompt.value) return
  saving.value = true
  try {
    const result = await publishPrompt(selectedPrompt.value.id)
    await refreshPromptSelection(result.data.prompt.id)
    setSaveStatus(result.data.warnings.length ? `Prompt 已发布，${result.data.warnings.length} 条 warning。` : 'Prompt 已发布。', 'success')
  } catch (error) {
    setSaveStatus(messageFromError(error), 'error')
  } finally {
    saving.value = false
  }
}

async function handleArchivePrompt() {
  if (!selectedPrompt.value) return
  saving.value = true
  try {
    const result = await archivePrompt(selectedPrompt.value.id)
    await refreshPromptSelection(result.data.prompt.id)
    setSaveStatus('Prompt 已归档。', 'success')
  } catch (error) {
    setSaveStatus(messageFromError(error), 'error')
  } finally {
    saving.value = false
  }
}

function openNewResource() {
  resourceDrawerMode.value = 'create'
  editErrors.value = []
  resourceDrawerOpen.value = true
}

function openEditResource() {
  resourceDrawerMode.value = 'edit'
  editErrors.value = []
  resourceDrawerOpen.value = true
}

async function saveResourceDraft(payload: ResourceUpsertInput) {
  saving.value = true
  editErrors.value = []
  try {
    const result = resourceDrawerMode.value === 'create'
      ? await createResource(payload)
      : await updateResource(selectedResource.value?.id ?? '', payload)
    resourceDetail.value = result.data
    selectedResource.value = result.data.resource
    source.value = result.source
    await refreshResourceSelection(result.data.resource.id)
    resourceDrawerOpen.value = false
    setSaveStatus('Resource 草稿已保存。', 'success')
  } catch (error) {
    editErrors.value = [messageFromError(error)]
    setSaveStatus('Resource 保存失败。', 'error')
  } finally {
    saving.value = false
  }
}

async function handlePublishResource() {
  if (!selectedResource.value) return
  saving.value = true
  try {
    const result = await publishResource(selectedResource.value.id)
    await refreshResourceSelection(result.data.resource.id)
    setSaveStatus('Resource 已发布。', 'success')
  } catch (error) {
    setSaveStatus(messageFromError(error), 'error')
  } finally {
    saving.value = false
  }
}

async function handleArchiveResource() {
  if (!selectedResource.value) return
  saving.value = true
  try {
    const result = await archiveResource(selectedResource.value.id)
    await refreshResourceSelection(result.data.resource.id)
    setSaveStatus('Resource 已归档。', 'success')
  } catch (error) {
    setSaveStatus(messageFromError(error), 'error')
  } finally {
    saving.value = false
  }
}

function setSaveStatus(message: string, tone: 'success' | 'error' | 'info') {
  saveMessage.value = message
  saveTone.value = tone
}

function messageFromError(error: unknown) {
  if (error instanceof ApiRequestError) {
    return error.message
  }
  return error instanceof Error ? error.message : '操作失败，请稍后重试。'
}

function sampleVariables(variables: string[]) {
  const samples: Record<string, unknown> = {
    customer_id: 'CUST-202405-000123',
    policy_doc: 'policy-docs',
    locale: 'zh-CN',
    invoice_id: 'INV-202406-1024',
    vendor: '杭州云启软件有限公司',
    amount: '128000 CNY'
  }
  return Object.fromEntries(variables.map((variable) => [variable, samples[variable] ?? `demo_${variable}`]))
}
</script>

<template>
  <div class="prompt-resource-page">
    <header class="page-title-row">
      <div>
        <p class="eyebrow">MCP Tool Gateway</p>
        <h1>Prompt Studio / Resource Library</h1>
        <p>统一管理 Prompt 模板、变量配置、Resource 资源与 Tool 绑定关系</p>
      </div>
      <div class="boundary-panel">
        <span>{{ source === 'api' ? '后端 API 已连接' : 'demo fallback 已启用' }}</span>
        <strong>{{ prompts.length }} Prompts · {{ resources.length }} Resources</strong>
      </div>
    </header>

    <nav class="workspace-tabs" aria-label="Prompt Resource tabs">
      <button type="button" :class="{ active: activeTab === 'prompt' }" @click="activeTab = 'prompt'">Prompt 模板</button>
      <button type="button" :class="{ active: activeTab === 'resource' }" @click="activeTab = 'resource'">Resource 资源</button>
      <button type="button" :class="{ active: activeTab === 'binding' }" @click="activeTab = 'binding'">Tool Binding</button>
      <button type="button" :class="{ active: activeTab === 'usage' }" @click="activeTab = 'usage'">Recent Usage</button>
    </nav>
    <SaveStatusBanner :message="saveMessage" :tone="saveTone" />

    <div v-if="loading" class="empty-state">正在同步 Prompt / Resource 治理数据。</div>

    <template v-else-if="activeTab === 'prompt'">
      <PromptActionBar :detail="promptDetail" :saving="saving" @new="openNewPrompt" @edit="openEditPrompt" @publish="handlePublishPrompt" @archive="handleArchivePrompt" />
      <section class="prompt-workspace-grid">
        <PromptListPanel :prompts="prompts" :selected-prompt-id="selectedPromptId" @select="selectPrompt" />
        <PromptEditorPanel :detail="promptDetail" />
        <aside class="prompt-render-stack">
          <PromptVariablePanel v-model="variableJson" :parse-error="parseError" :loading="rendering" @render="handleRender" />
          <PromptRenderPreview :result="renderResult" :source="renderSource" />
          <PromptResourceActivity :prompt-detail="promptDetail" :resource-detail="resourceDetail" mode="prompt" />
        </aside>
      </section>
    </template>

    <template v-else-if="activeTab === 'resource'">
      <ResourceActionBar :detail="resourceDetail" :saving="saving" @new="openNewResource" @edit="openEditResource" @publish="handlePublishResource" @archive="handleArchiveResource" />
      <section class="prompt-workspace-grid">
        <ResourceListPanel :resources="resources" :selected-resource-id="selectedResourceId" @select="selectResource" />
        <ResourceDetailPanel :detail="resourceDetail" />
        <aside class="prompt-render-stack">
          <ResourcePreviewPanel :detail="resourceDetail" />
          <PromptResourceActivity :prompt-detail="promptDetail" :resource-detail="resourceDetail" mode="resource" />
        </aside>
      </section>
    </template>

    <BindingSummaryPanel v-else-if="activeTab === 'binding'" :prompts="prompts" :resources="resources" :tools="tools" />

    <PromptResourceActivity v-else :prompt-detail="promptDetail" :resource-detail="resourceDetail" mode="all" />

    <PromptEditDrawer
      :open="promptDrawerOpen"
      :mode="promptDrawerMode"
      :detail="promptDetail"
      :saving="saving"
      :api-errors="editErrors"
      @close="promptDrawerOpen = false"
      @save="savePromptDraft"
    />
    <ResourceEditDrawer
      :open="resourceDrawerOpen"
      :mode="resourceDrawerMode"
      :detail="resourceDetail"
      :saving="saving"
      :api-errors="editErrors"
      @close="resourceDrawerOpen = false"
      @save="saveResourceDraft"
    />
  </div>
</template>
