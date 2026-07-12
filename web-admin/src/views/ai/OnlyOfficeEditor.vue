<template>
  <div class="onlyoffice-editor-page">
    <a-spin :spinning="loading" tip="正在加载文件...">
      <div :id="containerId" class="onlyoffice-editor-frame"></div>
    </a-spin>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { getKnowledgeDocumentPreviewConfig } from '@/api/ai'

const route = useRoute()
const loading = ref(false)
const containerId = 'onlyoffice-editor-container'
let onlyOfficeEditor = null

const loadEditor = async () => {
  const documentId = route.query.documentId
  if (!documentId) {
    message.error('缺少文档 ID')
    return
  }
  loading.value = true
  try {
    const response = await getKnowledgeDocumentPreviewConfig(documentId, 'edit')
    const previewData = response?.data || response
    await loadOnlyOfficeScript(previewData.documentServerApiUrl)
    destroyEditor()
    await nextTick()
    onlyOfficeEditor = new window.DocsAPI.DocEditor(containerId, previewData.config)
  } catch (error) {
    message.error(error.response?.data?.message || error.message || '打开文件编辑失败')
  } finally {
    loading.value = false
  }
}

const loadOnlyOfficeScript = (url) => {
  if (window.DocsAPI?.DocEditor) {
    return Promise.resolve()
  }
  const existingScript = document.querySelector(`script[src="${url}"]`)
  if (existingScript) {
    if (existingScript.dataset.loaded === 'true') {
      return Promise.resolve()
    }
    return new Promise((resolve, reject) => {
      existingScript.addEventListener('load', resolve, { once: true })
      existingScript.addEventListener('error', () => reject(new Error('OnlyOffice 脚本加载失败')), { once: true })
    })
  }
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = url
    script.async = true
    script.onload = () => {
      script.dataset.loaded = 'true'
      resolve()
    }
    script.onerror = () => reject(new Error('OnlyOffice 脚本加载失败'))
    document.body.appendChild(script)
  })
}

const destroyEditor = () => {
  if (onlyOfficeEditor?.destroyEditor) {
    onlyOfficeEditor.destroyEditor()
  }
  onlyOfficeEditor = null
}

onMounted(loadEditor)

onBeforeUnmount(destroyEditor)
</script>

<style scoped>
.onlyoffice-editor-page {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: #f8fafc;
}

.onlyoffice-editor-page :deep(.ant-spin-nested-loading),
.onlyoffice-editor-page :deep(.ant-spin-container) {
  width: 100%;
  height: 100%;
}

.onlyoffice-editor-frame {
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.onlyoffice-editor-frame :deep(> div),
.onlyoffice-editor-frame :deep(iframe) {
  width: 100% !important;
  height: 100% !important;
}
</style>
