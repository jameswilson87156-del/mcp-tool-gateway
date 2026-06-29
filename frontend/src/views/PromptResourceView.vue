<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import BindingSummaryPanel from '../components/BindingSummaryPanel.vue'
import PromptEditorPanel from '../components/PromptEditorPanel.vue'
import PromptListPanel from '../components/PromptListPanel.vue'
import PromptRenderPreview from '../components/PromptRenderPreview.vue'
import PromptResourceActivity from '../components/PromptResourceActivity.vue'
import PromptVariablePanel from '../components/PromptVariablePanel.vue'
import ResourceDetailPanel from '../components/ResourceDetailPanel.vue'
import ResourceListPanel from '../components/ResourceListPanel.vue'
import ResourcePreviewPanel from '../components/ResourcePreviewPanel.vue'
import { getPromptDetail, getPrompts, getResourceDetail, getResources, getTools, renderPrompt } from '../services/api'
import type {
  PromptDetail,
  PromptRenderResponse,
  PromptTemplate,
  ResourceDetail,
  ResourceDocument,
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

    <div v-if="loading" class="empty-state">正在同步 Prompt / Resource 治理数据。</div>

    <section v-else-if="activeTab === 'prompt'" class="prompt-workspace-grid">
      <PromptListPanel :prompts="prompts" :selected-prompt-id="selectedPromptId" @select="selectPrompt" />
      <PromptEditorPanel :detail="promptDetail" />
      <aside class="prompt-render-stack">
        <PromptVariablePanel v-model="variableJson" :parse-error="parseError" :loading="rendering" @render="handleRender" />
        <PromptRenderPreview :result="renderResult" :source="renderSource" />
        <PromptResourceActivity :prompt-detail="promptDetail" :resource-detail="resourceDetail" mode="prompt" />
      </aside>
    </section>

    <section v-else-if="activeTab === 'resource'" class="prompt-workspace-grid">
      <ResourceListPanel :resources="resources" :selected-resource-id="selectedResourceId" @select="selectResource" />
      <ResourceDetailPanel :detail="resourceDetail" />
      <aside class="prompt-render-stack">
        <ResourcePreviewPanel :detail="resourceDetail" />
        <PromptResourceActivity :prompt-detail="promptDetail" :resource-detail="resourceDetail" mode="resource" />
      </aside>
    </section>

    <BindingSummaryPanel v-else-if="activeTab === 'binding'" :prompts="prompts" :resources="resources" :tools="tools" />

    <PromptResourceActivity v-else :prompt-detail="promptDetail" :resource-detail="resourceDetail" mode="all" />
  </div>
</template>
