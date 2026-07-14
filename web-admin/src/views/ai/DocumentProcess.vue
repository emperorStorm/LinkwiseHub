<template>
  <div class="document-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">文档分片</h2>
        <p class="page-subtitle">{{ DOCUMENT_ANALYSIS_TEXT }}</p>
      </div>
      <div class="header-actions">
        <a-segmented v-model:value="parseStrategy" :options="parseStrategyOptions" />
        <a-upload
          :show-upload-list="false"
          :before-upload="beforeUpload"
          :custom-request="handleUpload"
          :accept="DOCUMENT_ACCEPT"
        >
          <a-button type="primary" :loading="uploading">
            <template #icon><upload-outlined /></template>
            上传文档
          </a-button>
        </a-upload>
        <a-button :loading="splitConfigLoading" @click="openSplitConfigModal">
          <template #icon><setting-outlined /></template>
          配置
        </a-button>
      </div>
    </div>

    <section class="document-panel">
      <div class="panel-toolbar">
        <span class="panel-title">文档列表</span>
        <div class="panel-actions">
          <a-input-search
            v-model:value="query.keyword"
            placeholder="搜索标题或文件名"
            allow-clear
            size="small"
            class="document-search"
            @search="applyFilters"
          />
          <a-range-picker
            v-model:value="query.dateRange"
            size="small"
            class="date-range"
            @change="applyFilters"
          />
          <a-button size="small" @click="loadDocuments">
            <template #icon><reload-outlined /></template>
            刷新
          </a-button>
        </div>
      </div>

      <a-table
        :columns="documentColumns"
        :data-source="filteredDocuments"
        :loading="documentLoading"
        :pagination="{ pageSize: 10, showSizeChanger: true }"
        :scroll="{ x: 1360 }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <div class="document-title-cell">
              <a-button
                type="link"
                class="file-link"
                :disabled="record.parseStatus !== 'SUCCESS'"
                @click.stop="openChunkModal(record)"
              >
                {{ getDocumentTitle(record) }}
              </a-button>
              <span>{{ record.fileName }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'sourceType'">
            <a-tag :color="getSourceColor(record.sourceType)">{{ getSourceText(record.sourceType) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'fileSize'">
            {{ formatFileSize(record.fileSize) }}
          </template>
          <template v-else-if="column.key === 'parseStatus'">
            <div class="parse-status-cell">
              <a-tag :color="getStatusColor(record.parseStatus)">
                {{ getStatusText(record.parseStatus) }}
              </a-tag>
              <a-progress
                v-if="record.parseStatus === 'PROCESSING'"
                :percent="getJobProgress(record.id)"
                :show-info="false"
                size="small"
              />
            </div>
          </template>
          <template v-else-if="column.key === 'publishStatus'">
            <a-tag :color="record.publishStatus === 'PUBLISHED' ? 'green' : 'default'">
              {{ getPublishStatusText(record.publishStatus) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'errorMessage'">
            <a-tooltip :title="record.errorMessage">
              <span class="error-message">{{ record.errorMessage || '-' }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'createTime' || column.key === 'updateTime'">
            {{ formatTime(record[column.key]) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space @click.stop>
              <a-button
                size="small"
                type="link"
                :disabled="record.parseStatus !== 'SUCCESS'"
                @click.stop="openChunkModal(record)"
              >
                <template #icon><unordered-list-outlined /></template>
                Chunk查看
              </a-button>
              <a-button
                v-if="record.parseStatus === 'FAILED'"
                size="small"
                type="link"
                :loading="retryingDocumentId === record.id"
                @click.stop="handleRetry(record)"
              >
                <template #icon><redo-outlined /></template>
                重试
              </a-button>
              <a-popconfirm
                title="确定删除该文档和全部 Chunk 吗？"
                ok-text="删除"
                cancel-text="取消"
                @confirm="handleDelete(record)"
              >
                <a-button size="small" type="link" danger @click.stop>
                  <template #icon><delete-outlined /></template>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </section>

    <a-modal
      v-model:open="chunkModalVisible"
      :title="chunkModalTitle"
      width="980px"
      :footer="null"
      destroy-on-close
    >
      <a-table
        :columns="chunkColumns"
        :data-source="chunks"
        :loading="chunkLoading"
        :pagination="{ pageSize: 8, showSizeChanger: true }"
        :scroll="{ x: 960 }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'content'">
            <a-tooltip :title="record.content">
              <span class="chunk-content">{{ record.content }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'sourceTitle'">
            <span>{{ record.sourceTitle || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'vectorStatus'">
            <a-tag :color="record.vectorStatus === 'SUCCESS' ? 'green' : 'default'">
              {{ record.vectorStatus || '-' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>

    <a-modal
      v-model:open="splitConfigVisible"
      title="RAG 分片参数"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="splitConfigSaving"
      @ok="saveSplitConfigForm"
    >
      <a-alert
        message="配置仅影响后续上传文档，已有文档和 Chunk 不会自动重切；段落和 Markdown 开关仅作兼容保留。"
        type="info"
        show-icon
        class="config-alert"
      />
      <a-form layout="vertical" class="config-form">
        <a-form-item label="目标 Chunk 长度" extra="映射到 TokenTextSplitter 的 chunk size，建议 200-2000，默认 800。">
          <a-input-number
            v-model:value="splitConfigForm.targetChunkLength"
            :min="200"
            :max="2000"
            :step="100"
            style="width: 100%"
            placeholder="请输入目标长度"
          />
        </a-form-item>
        <a-form-item label="优先按段落切分" extra="当前 RAG 流程由 TokenTextSplitter 接管，保留该项兼容旧配置。">
          <a-switch v-model:checked="splitConfigForm.splitByBlankLine" checked-children="开启" un-checked-children="关闭" />
        </a-form-item>
        <a-form-item label="保留 Markdown 标题" extra="当前 RAG 流程统一使用文档标题作为来源标题，保留该项兼容旧配置。">
          <a-switch v-model:checked="splitConfigForm.preserveMarkdownTitle" checked-children="开启" un-checked-children="关闭" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  DeleteOutlined,
  RedoOutlined,
  ReloadOutlined,
  SettingOutlined,
  UnorderedListOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'
import {
  deleteDocument,
  getDocumentChunks,
  getDocumentParseJob,
  getDocuments,
  getSplitConfig,
  retryDocumentParse,
  saveSplitConfig,
  uploadDocument
} from '@/api/ai'
import {
  DOCUMENT_ACCEPT,
  DOCUMENT_ANALYSIS_TEXT,
  DOCUMENT_ALLOWED_TYPES,
  DOCUMENT_IMAGE_TYPES,
  DOCUMENT_SUPPORTED_TEXT
} from '@/utils/documentFileTypes'

const MAX_FILE_SIZE = 10 * 1024 * 1024
const route = useRoute()

const documents = ref([])
const chunks = ref([])
const currentDocument = ref(null)
const chunkModalVisible = ref(false)
const uploading = ref(false)
const retryingDocumentId = ref(null)
const parseStrategy = ref('AUTO')
const jobProgress = ref({})
const documentLoading = ref(false)
const chunkLoading = ref(false)
const splitConfigLoading = ref(false)
const splitConfigSaving = ref(false)
const splitConfigVisible = ref(false)
const splitConfigForm = ref({
  targetChunkLength: 800,
  splitByBlankLine: true,
  preserveMarkdownTitle: true
})
const query = reactive({
  keyword: '',
  dateRange: []
})
const parseStrategyOptions = [
  { label: '兼容解析', value: 'LEGACY' },
  { label: '自动选择', value: 'AUTO' },
  { label: 'MinerU', value: 'MINERU' }
]
let pollingTimer = null

const documentColumns = [
  { title: '标题', dataIndex: 'title', key: 'title', width: 260, ellipsis: true },
  { title: '来源', dataIndex: 'sourceType', key: 'sourceType', width: 90 },
  { title: '类型', dataIndex: 'fileType', key: 'fileType', width: 80 },
  { title: '大小', dataIndex: 'fileSize', key: 'fileSize', width: 100 },
  { title: '状态', dataIndex: 'parseStatus', key: 'parseStatus', width: 110 },
  { title: '发布', dataIndex: 'publishStatus', key: 'publishStatus', width: 90 },
  { title: 'Chunk', dataIndex: 'chunkCount', key: 'chunkCount', width: 90 },
  { title: '错误信息', dataIndex: 'errorMessage', key: 'errorMessage', width: 180, ellipsis: true },
  { title: '上传时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' }
]

const chunkColumns = [
  { title: '序号', dataIndex: 'chunkIndex', key: 'chunkIndex', width: 70 },
  { title: '文档名', dataIndex: 'documentName', key: 'documentName', width: 180, ellipsis: true },
  { title: '来源标题', dataIndex: 'sourceTitle', key: 'sourceTitle', width: 180, ellipsis: true },
  { title: '内容', dataIndex: 'content', key: 'content', width: 360, ellipsis: true },
  { title: '字数', dataIndex: 'contentLength', key: 'contentLength', width: 80 },
  { title: '向量状态', dataIndex: 'vectorStatus', key: 'vectorStatus', width: 100 }
]

const chunkModalTitle = computed(() => {
  if (!currentDocument.value) {
    return 'Chunk查看'
  }
  return `${getDocumentTitle(currentDocument.value)} - Chunk查看`
})

const filteredDocuments = computed(() => {
  const keyword = query.keyword.trim().toLowerCase()
  const [start, end] = query.dateRange || []
  return documents.value.filter(item => {
    const title = getDocumentTitle(item).toLowerCase()
    const fileName = (item.fileName || '').toLowerCase()
    const keywordMatched = !keyword || title.includes(keyword) || fileName.includes(keyword)
    if (!keywordMatched) {
      return false
    }
    if (!start || !end || !item.createTime) {
      return true
    }
    const createTime = new Date(item.createTime.replace('T', ' '))
    const startTime = start.startOf ? start.startOf('day').toDate() : new Date(start)
    const endTime = end.endOf ? end.endOf('day').toDate() : new Date(end)
    return createTime >= startTime && createTime <= endTime
  })
})

/**
 * 上传前校验，避免无效文件进入后端。
 */
const beforeUpload = (file) => {
  const suffix = getFileSuffix(file.name)
  if (!DOCUMENT_ALLOWED_TYPES.includes(suffix)) {
    message.error(`仅支持 ${DOCUMENT_SUPPORTED_TEXT} 文件`)
    return false
  }
  if (parseStrategy.value === 'LEGACY' && DOCUMENT_IMAGE_TYPES.includes(suffix)) {
    message.error('图片文件需要选择“自动选择”或“MinerU”解析')
    return false
  }
  if (file.size > MAX_FILE_SIZE) {
    message.error('文件大小不能超过 10MB')
    return false
  }
  if (file.size === 0) {
    message.error('文件不能为空')
    return false
  }
  return true
}

/**
 * 自定义上传流程，上传成功后刷新列表并选中新文档。
 */
const handleUpload = async ({ file, onSuccess, onError }) => {
  uploading.value = true
  try {
    const response = await uploadDocument(file, parseStrategy.value)
    const result = response?.data || response
    const processing = result.parseStatus === 'PROCESSING'
    message.success(processing ? '文档已进入解析队列' : `解析成功，生成 ${result.chunkCount || 0} 个 Chunk`)
    await loadDocuments()
    const uploaded = documents.value.find(item => item.id === result.documentId)
    if (uploaded && !processing) {
      await openChunkModal(uploaded)
    }
    onSuccess?.(response)
  } catch (error) {
    message.error(error.response?.data?.message || error.message || '上传解析失败')
    onError?.(error)
  } finally {
    uploading.value = false
  }
}

/**
 * 加载文档列表。
 */
const loadDocuments = async () => {
  documentLoading.value = true
  try {
    const response = await getDocuments()
    documents.value = response?.data || response || []
    await loadProcessingJobs()
    await selectRouteDocument()
  } catch (error) {
    message.error('加载文档列表失败')
  } finally {
    documentLoading.value = false
  }
}

const loadProcessingJobs = async () => {
  const processingDocuments = documents.value.filter(item => item.parseStatus === 'PROCESSING')
  const entries = await Promise.all(processingDocuments.map(async item => {
    try {
      const response = await getDocumentParseJob(item.id)
      return [item.id, response?.data || response]
    } catch (error) {
      return [item.id, null]
    }
  }))
  jobProgress.value = Object.fromEntries(entries)
}

const getJobProgress = (documentId) => jobProgress.value[documentId]?.progress || 0

const handleRetry = async (record) => {
  retryingDocumentId.value = record.id
  try {
    await retryDocumentParse(record.id, parseStrategy.value)
    message.success('文档已重新提交解析')
    await loadDocuments()
  } catch (error) {
    message.error(error.response?.data?.message || error.message || '重新解析失败')
  } finally {
    retryingDocumentId.value = null
  }
}

const applyFilters = () => {
  if (currentDocument.value && !filteredDocuments.value.some(item => item.id === currentDocument.value.id)) {
    currentDocument.value = null
    chunks.value = []
    chunkModalVisible.value = false
  }
}

/**
 * 打开 Chunk 查看弹窗并加载分片。
 */
const openChunkModal = async (record) => {
  currentDocument.value = record
  chunkModalVisible.value = true
  chunkLoading.value = true
  try {
    const response = await getDocumentChunks(record.id)
    chunks.value = response?.data || response || []
    if (chunks.value.length === 0) {
      message.warning('该文档暂无 Chunk')
    }
  } catch (error) {
    message.error(error.response?.data?.message || '加载 Chunk 失败')
    chunks.value = []
  } finally {
    chunkLoading.value = false
  }
}

const selectRouteDocument = async () => {
  const documentId = Number(route.query.documentId)
  if (!documentId) {
    return
  }
  const target = documents.value.find(item => item.id === documentId)
  if (target?.parseStatus === 'SUCCESS') {
    await openChunkModal(target)
  }
}

/**
 * 删除文档后同步清空当前 Chunk 预览。
 */
const handleDelete = async (record) => {
  try {
    await deleteDocument(record.id)
    message.success('删除成功')
    if (currentDocument.value?.id === record.id) {
      currentDocument.value = null
      chunks.value = []
      chunkModalVisible.value = false
    }
    await loadDocuments()
  } catch (error) {
    message.error(error.response?.data?.message || '删除失败')
  }
}

/**
 * 打开配置弹窗前刷新后端配置，避免展示过期规则。
 */
const openSplitConfigModal = async () => {
  await loadSplitConfig()
  splitConfigVisible.value = true
}

/**
 * 加载全局分片配置。
 */
const loadSplitConfig = async () => {
  splitConfigLoading.value = true
  try {
    const response = await getSplitConfig()
    const config = response?.data || response || {}
    splitConfigForm.value = {
      targetChunkLength: config.targetChunkLength || 800,
      splitByBlankLine: config.splitByBlankLine !== false,
      preserveMarkdownTitle: config.preserveMarkdownTitle !== false
    }
  } catch (error) {
    message.error(error.response?.data?.message || '加载分片配置失败')
  } finally {
    splitConfigLoading.value = false
  }
}

/**
 * 保存全局分片配置，后续上传文档会按新规则分片。
 */
const saveSplitConfigForm = async () => {
  const targetLength = Number(splitConfigForm.value.targetChunkLength)
  if (!targetLength || targetLength < 200 || targetLength > 2000) {
    message.error('目标 Chunk 长度必须在 200 到 2000 之间')
    return
  }
  splitConfigSaving.value = true
  try {
    const response = await saveSplitConfig({
      targetChunkLength: targetLength,
      splitByBlankLine: splitConfigForm.value.splitByBlankLine,
      preserveMarkdownTitle: splitConfigForm.value.preserveMarkdownTitle
    })
    const config = response?.data || response || {}
    splitConfigForm.value = {
      targetChunkLength: config.targetChunkLength || targetLength,
      splitByBlankLine: config.splitByBlankLine !== false,
      preserveMarkdownTitle: config.preserveMarkdownTitle !== false
    }
    splitConfigVisible.value = false
    message.success(response?.message || '配置已保存，仅影响后续上传文档')
  } catch (error) {
    message.error(error.response?.data?.message || '保存分片配置失败')
  } finally {
    splitConfigSaving.value = false
  }
}

const getFileSuffix = (fileName) => {
  const index = fileName.lastIndexOf('.')
  return index >= 0 ? fileName.slice(index + 1).toLowerCase() : ''
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  return `${(bytes / Math.pow(1024, index)).toFixed(index === 0 ? 0 : 2)} ${units[index]}`
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 19)
}

const getStatusColor = (status) => {
  const colorMap = {
    SUCCESS: 'green',
    PROCESSING: 'blue',
    FAILED: 'red'
  }
  return colorMap[status] || 'default'
}

const getStatusText = (status) => {
  const textMap = {
    SUCCESS: '解析成功',
    PROCESSING: '解析中',
    FAILED: '解析失败'
  }
  return textMap[status] || status || '-'
}

const getDocumentTitle = (record) => {
  if (record.title && record.title.trim()) {
    return record.title
  }
  if (!record.fileName) {
    return '未命名文档'
  }
  const index = record.fileName.lastIndexOf('.')
  return index > 0 ? record.fileName.slice(0, index) : record.fileName
}

const getSourceText = (sourceType) => {
  const map = {
    CONTENT: '内容',
    FILE: '文件',
    MIXED: '混合'
  }
  return map[sourceType] || '上传'
}

const getSourceColor = (sourceType) => {
  const map = {
    CONTENT: 'cyan',
    FILE: 'geekblue',
    MIXED: 'gold'
  }
  return map[sourceType] || 'default'
}

const getPublishStatusText = (publishStatus) => {
  const map = {
    DRAFT: '草稿',
    PUBLISHED: '已发布'
  }
  return map[publishStatus] || publishStatus || '-'
}

watch(
  () => route.query.documentId,
  () => {
    selectRouteDocument()
  }
)

onMounted(() => {
  loadDocuments()
  loadSplitConfig()
  pollingTimer = window.setInterval(() => {
    if (documents.value.some(item => item.parseStatus === 'PROCESSING') && !documentLoading.value) {
      loadDocuments()
    }
  }, 3000)
})

onBeforeUnmount(() => {
  if (pollingTimer) {
    window.clearInterval(pollingTimer)
  }
})
</script>

<style scoped>
.document-page {
  animation: fadeIn 0.3s ease-out;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.parse-status-cell {
  width: 88px;
}

.parse-status-cell :deep(.ant-progress) {
  margin-top: 4px;
}

.page-title {
  margin: 0;
  color: #1e293b;
  font-size: 20px;
  font-weight: 600;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.document-panel {
  min-width: 0;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.panel-toolbar {
  min-height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 16px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}

.panel-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  min-width: 0;
}

.panel-title {
  color: #1e293b;
  font-size: 15px;
  font-weight: 600;
}

.document-search {
  width: 220px;
}

.date-range {
  width: 220px;
}

.document-title-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  min-width: 0;
}

.document-title-cell span {
  max-width: 260px;
  overflow: hidden;
  color: #7a8798;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-link {
  max-width: 260px;
  padding: 0;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chunk-content {
  display: inline-block;
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.empty-state {
  padding: 96px 0;
}

.config-alert {
  margin-bottom: 16px;
}

.config-form {
  margin-top: 4px;
}

.document-panel :deep(.ant-table-thead > tr > th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .panel-toolbar,
  .panel-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .document-search,
  .date-range {
    width: 100%;
  }
}
</style>
