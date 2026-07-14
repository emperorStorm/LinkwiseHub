<template>
  <div class="knowledge-page">
    <aside class="category-panel">
      <div class="category-head">
        <div>
          <h2>知识库</h2>
          <p>{{ categoryStats }}</p>
        </div>
        <a-button type="primary" size="small" @click="openCategoryModal('create-root')">
          <template #icon><plus-outlined /></template>
          分类
        </a-button>
      </div>

      <a-input-search
        v-model:value="categoryKeyword"
        placeholder="搜索分类"
        class="category-search"
        allow-clear
      />

      <div class="tree-shell">
        <a-empty v-if="!categoryLoading && filteredCategoryTree.length === 0" description="暂无分类" class="tree-empty" />
        <a-tree
          v-else
          :tree-data="filteredCategoryTree"
          :selected-keys="selectedCategoryKeys"
          :expanded-keys="expandedKeys"
          :loading="categoryLoading"
          block-node
          @select="handleCategorySelect"
          @expand="keys => expandedKeys = keys"
        >
          <template #title="{ dataRef }">
            <div class="category-node">
              <span class="category-label">
                <folder-open-outlined class="category-icon" />
                <span class="category-name">{{ dataRef.name }}</span>
              </span>
              <a-dropdown trigger="click">
                <a-button type="text" size="small" class="node-action" @click.stop>
                  <more-outlined />
                </a-button>
                <template #overlay>
                  <a-menu @click="({ key }) => handleCategoryAction(key, dataRef)">
                    <a-menu-item key="create-sibling">
                      <plus-circle-outlined /> 新增同级
                    </a-menu-item>
                    <a-menu-item key="create-child">
                      <folder-add-outlined /> 新增子级
                    </a-menu-item>
                    <a-menu-item key="edit">
                      <edit-outlined /> 重命名
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="delete" danger>
                      <delete-outlined /> 删除
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </div>
          </template>
        </a-tree>
      </div>
    </aside>

    <main class="document-workbench">
      <section class="workbench-header">
        <div>
          <h1>{{ currentCategoryName }}</h1>
          <p class="header-copy">按分类维护可分片的知识文档，发布状态当前用于管理展示和筛选。</p>
        </div>
        <div class="header-metrics">
          <div>
            <strong>{{ documents.length }}</strong>
            <span>文档</span>
          </div>
          <div>
            <strong>{{ publishedCount }}</strong>
            <span>已发布</span>
          </div>
        </div>
      </section>

      <section class="toolbar">
        <a-input-search
          v-model:value="query.keyword"
          placeholder="搜索标题或文件名"
          allow-clear
          class="keyword-input"
          @search="loadDocuments"
        />
        <a-select v-model:value="query.publishStatus" class="status-select" @change="loadDocuments">
          <a-select-option value="">全部状态</a-select-option>
          <a-select-option value="PUBLISHED">已发布</a-select-option>
          <a-select-option value="DRAFT">草稿</a-select-option>
        </a-select>
        <a-button @click="loadDocuments">
          <template #icon><reload-outlined /></template>
          刷新
        </a-button>
        <a-button type="primary" :disabled="!selectedCategoryId" @click="openDocumentModal()">
          <template #icon><file-add-outlined /></template>
          新增文档
        </a-button>
      </section>

      <section class="document-table-panel">
        <a-table
          :columns="documentColumns"
          :data-source="documents"
          :loading="documentLoading"
          :pagination="{ pageSize: 8, showSizeChanger: true }"
          :scroll="{ x: 980 }"
          row-key="id"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'title'">
              <div class="doc-title-cell">
                <a-button type="link" class="doc-title-link" @click="openDocumentDetail(record)">
                  {{ record.title || record.fileName || '未命名文档' }}
                </a-button>
                <span>{{ record.fileName || '纯内容文档' }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'publishStatus'">
              <a-switch
                :checked="record.publishStatus === 'PUBLISHED'"
                checked-children="发布"
                un-checked-children="草稿"
                @change="checked => changePublishStatus(record, checked)"
              />
            </template>
            <template v-else-if="column.key === 'sourceType'">
              <a-tag :color="getSourceColor(record.sourceType)">{{ getSourceText(record.sourceType) }}</a-tag>
            </template>
            <template v-else-if="column.key === 'parseStatus'">
              <div class="parse-status-cell">
                <a-tag :color="record.parseStatus === 'SUCCESS' ? 'green' : record.parseStatus === 'FAILED' ? 'red' : 'blue'">
                  {{ getParseText(record.parseStatus) }}
                </a-tag>
                <span class="parse-engine">{{ getParserText(record) }}</span>
                <a-progress
                  v-if="record.parseStatus === 'PROCESSING'"
                  :percent="record.parseProgress || 0"
                  :show-info="false"
                  size="small"
                />
              </div>
            </template>
            <template v-else-if="column.key === 'parseError'">
              <a-tooltip :title="getParseError(record)">
                <span class="parse-error">{{ getParseError(record) || '-' }}</span>
              </a-tooltip>
            </template>
            <template v-else-if="column.key === 'fileSize'">
              {{ formatFileSize(record.fileSize) }}
            </template>
            <template v-else-if="column.key === 'updateTime'">
              {{ formatTime(record.updateTime || record.createTime) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space class="action-buttons" :size="4">
                <a-tooltip title="查看切片">
                  <a-button size="small" type="text" class="action-icon-btn" @click="goDocumentChunks(record)">
                    <unordered-list-outlined />
                  </a-button>
                </a-tooltip>
                <a-tooltip title="编辑">
                  <a-button size="small" type="text" class="action-icon-btn" @click="openDocumentModal(record)">
                    <edit-outlined />
                  </a-button>
                </a-tooltip>
                <a-dropdown v-if="record.parseStatus === 'FAILED'" trigger="click">
                  <a-tooltip title="重新分析">
                    <a-button size="small" type="text" class="action-icon-btn" :loading="retryingDocumentId === record.id">
                      <redo-outlined />
                    </a-button>
                  </a-tooltip>
                  <template #overlay>
                    <a-menu @click="({ key }) => retryKnowledgeDocument(record, key)">
                      <a-menu-item key="AUTO">自动识别</a-menu-item>
                      <a-menu-item key="MINERU" :disabled="!isMineruAnalysisFile(record.fileType)">MinerU</a-menu-item>
                      <a-menu-item key="LEGACY" :disabled="!isLegacyAnalysisFile(record.fileType)">兼容解析</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
                <a-popconfirm title="确定删除该知识文档吗？" ok-text="删除" cancel-text="取消" @confirm="removeDocument(record)">
                  <a-button size="small" type="text" danger class="action-icon-btn">
                    <delete-outlined />
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </section>
    </main>

    <a-modal
      v-model:open="categoryModal.visible"
      :title="categoryModalTitle"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="categorySaving"
      @ok="submitCategory"
    >
      <a-form layout="vertical">
        <a-form-item label="分类名称" required>
          <a-input v-model:value="categoryForm.name" placeholder="请输入分类名称" maxlength="50" show-count />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="categoryForm.sort" :min="0" :max="9999" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="documentModalVisible"
      :title="documentForm.id ? '编辑知识文档' : '新增知识文档'"
      width="780px"
      ok-text="确认"
      cancel-text="取消"
      :confirm-loading="documentSaving"
      @ok="submitDocument"
      @cancel="resetDocumentForm"
    >
      <a-form layout="vertical" class="document-form">
        <a-form-item label="所属分类" required>
          <a-tree-select
            v-model:value="documentForm.categoryId"
            :tree-data="categoryTreeSelectData"
            tree-default-expand-all
            placeholder="请选择分类"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="标题" required>
          <a-input v-model:value="documentForm.title" placeholder="请输入知识文档标题" maxlength="120" show-count />
        </a-form-item>
        <a-form-item label="内容">
          <div class="editor-toolbar">
            <a-button size="small" @click="execEditorCommand('bold')"><bold-outlined /></a-button>
            <a-button size="small" @click="execEditorCommand('insertUnorderedList')"><unordered-list-outlined /></a-button>
            <a-button size="small" @click="execEditorCommand('removeFormat')"><clear-outlined /></a-button>
          </div>
          <div
            ref="editorRef"
            class="rich-editor"
            contenteditable="true"
            data-placeholder="输入知识正文，可与文件一起分片"
            @input="syncEditorContent"
          />
        </a-form-item>
        <a-form-item label="文件" extra="编辑时不选新文件则沿用原文件。">
          <a-upload
            :before-upload="beforeFileSelect"
            :accept="DOCUMENT_ACCEPT"
            :max-count="1"
            :show-upload-list="false"
          >
            <a-button>
              <template #icon><upload-outlined /></template>
              选择文件
            </a-button>
          </a-upload>
          <div class="file-analysis-hint">{{ DOCUMENT_ANALYSIS_TEXT }}</div>
          <div v-if="currentAttachment" class="form-attachment-row">
            <button type="button" class="form-attachment-title" @click="previewFormAttachment">
              <paper-clip-outlined />
              <span>{{ currentAttachment.name }}</span>
            </button>
            <div class="form-attachment-actions">
              <a-tooltip title="下载">
                <a-button size="small" type="text" class="attachment-icon-btn" :disabled="!!documentForm.file" @click="downloadFormAttachment">
                  <download-outlined />
                </a-button>
              </a-tooltip>
              <a-tooltip title="修改">
                <a-button size="small" type="text" class="attachment-icon-btn" :disabled="!!documentForm.file || !canEditCurrentAttachment" @click="editFormAttachment">
                  <edit-outlined />
                </a-button>
              </a-tooltip>
              <a-popconfirm
                title="确定删除该附件吗？"
                ok-text="删除"
                cancel-text="取消"
                @confirm="deleteFormAttachment"
              >
                <a-tooltip title="删除">
                  <a-button size="small" type="text" danger class="attachment-icon-btn attachment-delete-btn">
                    <delete-outlined />
                  </a-button>
                </a-tooltip>
              </a-popconfirm>
            </div>
          </div>
        </a-form-item>
        <a-form-item label="发布状态">
          <a-switch
            v-model:checked="documentForm.published"
            checked-children="发布"
            un-checked-children="草稿"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer
      v-model:open="detailDrawer.visible"
      title="文档详情"
      width="720"
      class="document-detail-drawer"
      :destroy-on-close="true"
    >
      <a-spin :spinning="detailDrawer.loading">
        <template v-if="detailDocument">
          <section class="detail-head">
            <div>
              <h2>{{ detailDocument.title || detailDocument.fileName || '未命名文档' }}</h2>
              <p>{{ detailDocument.categoryName || '未分类' }}</p>
            </div>
            <a-tag :color="detailDocument.publishStatus === 'PUBLISHED' ? 'green' : 'default'">
              {{ detailDocument.publishStatus === 'PUBLISHED' ? '已发布' : '草稿' }}
            </a-tag>
          </section>

          <section class="detail-grid">
            <div>
              <span>来源</span>
              <strong>{{ getSourceText(detailDocument.sourceType) }}</strong>
            </div>
            <div>
              <span>解析状态</span>
              <strong>{{ getParseText(detailDocument.parseStatus) }}</strong>
            </div>
            <div>
              <span>切片数量</span>
              <strong>{{ detailDocument.chunkCount || 0 }}</strong>
            </div>
            <div>
              <span>更新时间</span>
              <strong>{{ formatTime(detailDocument.updateTime || detailDocument.createTime) || '-' }}</strong>
            </div>
          </section>

          <section v-if="detailDocument.parseStatus !== 'SUCCESS' || getParseError(detailDocument)" class="detail-parse-alert">
            <a-alert
              :type="detailDocument.parseStatus === 'FAILED' ? 'error' : 'info'"
              :message="getParserText(detailDocument)"
              :description="getDetailParseDescription(detailDocument)"
              show-icon
            />
          </section>

          <section class="detail-section">
            <div class="section-title">附件</div>
            <div v-if="detailDocument.fileName" class="attachment-box">
              <button type="button" class="attachment-meta attachment-title-button" @click="openAttachmentPreview(detailDocument)">
                <file-text-outlined />
                <div>
                  <strong>{{ detailDocument.fileName }}</strong>
                  <span>{{ (detailDocument.fileType || '').toUpperCase() }} · {{ formatFileSize(detailDocument.fileSize) }}</span>
                </div>
              </button>
              <a-tooltip title="下载">
                <a-button size="small" type="text" class="attachment-icon-btn" @click="downloadDetailAttachment(detailDocument)">
                  <download-outlined />
                </a-button>
              </a-tooltip>
            </div>
            <a-empty v-else description="暂无附件" />
          </section>

          <section class="detail-section">
            <div class="section-title">正文内容</div>
            <div v-if="detailDocument.contentHtml" class="content-preview" v-html="sanitizeContent(detailDocument.contentHtml)" />
            <a-empty v-else description="暂无正文内容" />
          </section>
        </template>
      </a-spin>
    </a-drawer>

    <a-modal
      v-model:open="previewModal.visible"
      :title="previewModal.title"
      :width="previewModalWidth"
      wrap-class-name="onlyoffice-preview-modal"
      :footer="null"
      :destroy-on-close="true"
      centered
      @after-open-change="handlePreviewOpenChange"
      @cancel="closePreview"
    >
      <div class="onlyoffice-preview-shell">
        <a-spin :spinning="previewLoading">
          <div :id="onlyOfficeContainerId" class="onlyoffice-frame"></div>
        </a-spin>
      </div>
    </a-modal>

  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import DOMPurify from 'dompurify'
import {
  BoldOutlined,
  ClearOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  FileAddOutlined,
  FileTextOutlined,
  FolderAddOutlined,
  FolderOpenOutlined,
  MoreOutlined,
  PaperClipOutlined,
  PlusCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  RedoOutlined,
  UnorderedListOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'
import {
  createKnowledgeCategory,
  createKnowledgeDocument,
  deleteKnowledgeCategory,
  deleteKnowledgeDocument,
  deleteKnowledgeDocumentAttachment,
  getKnowledgeCategories,
  getKnowledgeDocumentDetail,
  getKnowledgeDocumentPreviewConfig,
  getKnowledgeDocuments,
  retryDocumentParse,
  updateKnowledgeCategory,
  updateKnowledgeDocument,
  updateKnowledgePublishStatus
} from '@/api/ai'
import {
  DOCUMENT_ACCEPT,
  DOCUMENT_ALLOWED_TYPES,
  DOCUMENT_ANALYSIS_TEXT,
  DOCUMENT_SUPPORTED_TEXT,
  MINERU_ANALYSIS_TYPES
} from '@/utils/documentFileTypes'

const MAX_FILE_SIZE = 10 * 1024 * 1024
const router = useRouter()

const categories = ref([])
const documents = ref([])
const selectedCategoryId = ref(null)
const selectedCategoryKeys = ref([])
const expandedKeys = ref([])
const categoryKeyword = ref('')
const categoryLoading = ref(false)
const categorySaving = ref(false)
const documentLoading = ref(false)
const documentSaving = ref(false)
const retryingDocumentId = ref(null)
const documentModalVisible = ref(false)
const editorRef = ref(null)
const fileList = ref([])
const detailDocument = ref(null)
const previewLoading = ref(false)
const onlyOfficeContainerId = 'onlyoffice-preview-container'
const previewModalWidth = '75vw'
let onlyOfficeEditor = null
let documentPollingTimer = null

const query = reactive({
  keyword: '',
  publishStatus: ''
})

const categoryModal = reactive({
  visible: false,
  mode: 'create-root',
  target: null
})

const categoryForm = reactive({
  name: '',
  sort: 0
})

const documentForm = reactive({
  id: null,
  categoryId: null,
  title: '',
  contentHtml: '',
  published: false,
  file: null,
  oldFileName: '',
  oldFileSize: 0,
  hasOldFile: false
})

const detailDrawer = reactive({
  visible: false,
  loading: false
})

const previewModal = reactive({
  visible: false,
  title: '附件预览'
})

const documentColumns = [
  { title: '文档', dataIndex: 'title', key: 'title', width: 210, ellipsis: true },
  { title: '分类', dataIndex: 'categoryName', key: 'categoryName', width: 120 },
  { title: '来源', dataIndex: 'sourceType', key: 'sourceType', width: 90 },
  { title: '状态', dataIndex: 'publishStatus', key: 'publishStatus', width: 120 },
  { title: '解析', dataIndex: 'parseStatus', key: 'parseStatus', width: 150 },
  { title: '识别说明', key: 'parseError', width: 200, ellipsis: true },
  { title: '大小', dataIndex: 'fileSize', key: 'fileSize', width: 100 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 146, fixed: 'right' }
]

const categoryModalTitle = computed(() => {
  if (categoryModal.mode === 'edit') return '重命名分类'
  if (categoryModal.mode === 'create-child') return '新增子级分类'
  if (categoryModal.mode === 'create-sibling') return '新增同级分类'
  return '新增根分类'
})

const categoryStats = computed(() => `${flattenCategories(categories.value).length} 个分类`)
const currentCategoryName = computed(() => findCategoryName(selectedCategoryId.value) || '全部知识文档')
const publishedCount = computed(() => documents.value.filter(item => item.publishStatus === 'PUBLISHED').length)

const filteredCategoryTree = computed(() => {
  const tree = toTreeData(categories.value)
  const keyword = categoryKeyword.value.trim()
  if (!keyword) return tree
  return filterTree(tree, keyword)
})

const categoryTreeSelectData = computed(() => toTreeData(categories.value))
const currentAttachment = computed(() => fileList.value[0] || null)
const canEditCurrentAttachment = computed(() => documentForm.oldFileName && getFileSuffix(documentForm.oldFileName) !== 'pdf')

const loadCategories = async () => {
  categoryLoading.value = true
  try {
    const response = await getKnowledgeCategories()
    categories.value = response?.data || response || []
    const flat = flattenCategories(categories.value)
    expandedKeys.value = flat.map(item => item.id)
    if (!selectedCategoryId.value && flat.length > 0) {
      selectedCategoryId.value = flat[0].id
      selectedCategoryKeys.value = [flat[0].id]
    }
  } catch (error) {
    message.error(error.response?.data?.message || '加载分类失败')
  } finally {
    categoryLoading.value = false
  }
}

const loadDocuments = async () => {
  documentLoading.value = true
  try {
    const params = {
      categoryId: selectedCategoryId.value || undefined,
      keyword: query.keyword || undefined,
      publishStatus: query.publishStatus || undefined
    }
    const response = await getKnowledgeDocuments(params)
    documents.value = response?.data || response || []
  } catch (error) {
    message.error(error.response?.data?.message || '加载知识文档失败')
  } finally {
    documentLoading.value = false
  }
}

const handleCategorySelect = (keys) => {
  selectedCategoryKeys.value = keys
  selectedCategoryId.value = keys[0] || null
  loadDocuments()
}

const handleCategoryAction = (key, category) => {
  if (key === 'delete') {
    removeCategory(category)
    return
  }
  openCategoryModal(key, category)
}

const openCategoryModal = (mode, target = null) => {
  categoryModal.visible = true
  categoryModal.mode = mode
  categoryModal.target = target
  categoryForm.name = mode === 'edit' ? target.name : ''
  categoryForm.sort = mode === 'edit' ? target.sort || 0 : 0
}

const submitCategory = async () => {
  if (!categoryForm.name.trim()) {
    message.error('请输入分类名称')
    return
  }
  categorySaving.value = true
  try {
    const target = categoryModal.target
    if (categoryModal.mode === 'edit') {
      await updateKnowledgeCategory(target.id, {
        parentId: target.parentId || 0,
        name: categoryForm.name.trim(),
        sort: categoryForm.sort || 0
      })
    } else {
      await createKnowledgeCategory({
        parentId: resolveNewCategoryParentId(),
        name: categoryForm.name.trim(),
        sort: categoryForm.sort || 0
      })
    }
    categoryModal.visible = false
    await loadCategories()
    message.success('分类已保存')
  } catch (error) {
    message.error(error.response?.data?.message || '保存分类失败')
  } finally {
    categorySaving.value = false
  }
}

const resolveNewCategoryParentId = () => {
  const target = categoryModal.target
  if (categoryModal.mode === 'create-child') return target?.id || 0
  if (categoryModal.mode === 'create-sibling') return target?.parentId || 0
  return 0
}

const removeCategory = async (category) => {
  try {
    await deleteKnowledgeCategory(category.id)
    message.success('分类已删除')
    if (selectedCategoryId.value === category.id) {
      selectedCategoryId.value = null
      selectedCategoryKeys.value = []
    }
    await loadCategories()
    await loadDocuments()
  } catch (error) {
    message.error(error.response?.data?.message || '删除分类失败')
  }
}

const openDocumentModal = async (record = null) => {
  resetDocumentForm()
  if (record) {
    documentForm.id = record.id
    documentForm.categoryId = record.categoryId
    documentForm.title = record.title
    documentForm.contentHtml = record.contentHtml || ''
    documentForm.published = record.publishStatus === 'PUBLISHED'
    documentForm.hasOldFile = !!record.fileName
    documentForm.oldFileName = record.fileName || ''
    documentForm.oldFileSize = record.fileSize || 0
    if (record.fileName) {
      fileList.value = [{
        uid: `old-${record.id}`,
        name: record.fileName,
        status: 'done',
        size: record.fileSize || 0
      }]
    }
  } else {
    documentForm.categoryId = selectedCategoryId.value
  }
  documentModalVisible.value = true
  await nextTick()
  if (editorRef.value) {
    editorRef.value.innerHTML = documentForm.contentHtml || ''
  }
}

const resetDocumentForm = () => {
  documentForm.id = null
  documentForm.categoryId = selectedCategoryId.value
  documentForm.title = ''
  documentForm.contentHtml = ''
  documentForm.published = false
  documentForm.file = null
  documentForm.oldFileName = ''
  documentForm.oldFileSize = 0
  documentForm.hasOldFile = false
  fileList.value = []
  if (editorRef.value) {
    editorRef.value.innerHTML = ''
  }
}

const submitDocument = async () => {
  syncEditorContent()
  if (!documentForm.categoryId) {
    message.error('请选择分类')
    return
  }
  if (!documentForm.title.trim()) {
    message.error('请输入标题')
    return
  }
  const contentText = stripHtml(documentForm.contentHtml)
  if (!contentText && !documentForm.file && !documentForm.hasOldFile) {
    message.error('内容和文件至少填写一个')
    return
  }
  documentSaving.value = true
  try {
    const payload = {
      categoryId: documentForm.categoryId,
      title: documentForm.title.trim(),
      contentHtml: documentForm.contentHtml,
      publishStatus: documentForm.published ? 'PUBLISHED' : 'DRAFT',
      file: documentForm.file
    }
    if (documentForm.id) {
      await updateKnowledgeDocument(documentForm.id, payload)
    } else {
      await createKnowledgeDocument(payload)
    }
    documentModalVisible.value = false
    resetDocumentForm()
    query.keyword = ''
    query.publishStatus = ''
    await loadDocuments()
    message.success('知识文档已保存')
  } catch (error) {
    message.error(error.response?.data?.message || '保存知识文档失败')
  } finally {
    documentSaving.value = false
  }
}

const beforeFileSelect = (file) => {
  const suffix = getFileSuffix(file.name)
  if (!DOCUMENT_ALLOWED_TYPES.includes(suffix)) {
    message.error(`仅支持 ${DOCUMENT_SUPPORTED_TEXT} 文件`)
    return false
  }
  if (file.size > MAX_FILE_SIZE) {
    message.error('文件大小不能超过 10MB')
    return false
  }
  documentForm.file = file
  fileList.value = [file]
  return false
}

const previewFormAttachment = () => {
  if (documentForm.file) {
    message.info('新选择的文件保存后才可预览')
    return
  }
  if (!documentForm.id || !documentForm.hasOldFile) {
    message.info('附件保存后才可预览')
    return
  }
  openAttachmentPreview({
    id: documentForm.id,
    fileName: currentAttachment.value?.name
  })
}

const downloadFormAttachment = () => {
  if (!documentForm.id || documentForm.file || !documentForm.hasOldFile) {
    message.info('原附件保存后才可下载')
    return
  }
  const link = document.createElement('a')
  link.href = `/api/ai/knowledge/documents/${documentForm.id}/attachment`
  link.download = currentAttachment.value?.name || ''
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const editFormAttachment = () => {
  if (!documentForm.id || documentForm.file || !documentForm.hasOldFile) {
    message.info('原附件保存后才可修改')
    return
  }
  if (!canEditCurrentAttachment.value) {
    message.info('PDF 文件仅支持查看')
    return
  }
  const routeData = router.resolve({
    path: '/ai/onlyoffice-editor',
    query: { documentId: documentForm.id }
  })
  window.open(routeData.href, '_blank', 'noopener,noreferrer')
}

const deleteFormAttachment = async () => {
  if (documentForm.file) {
    removeSelectedFile()
    return
  }
  if (!documentForm.id || !documentForm.hasOldFile) {
    removeSelectedFile()
    return
  }
  try {
    const response = await deleteKnowledgeDocumentAttachment(documentForm.id)
    const updatedDocument = response?.data || response
    documentForm.hasOldFile = false
    documentForm.file = null
    fileList.value = []
    await loadDocuments()
    if (updatedDocument?.id) {
      message.success('附件已删除')
    }
  } catch (error) {
    message.error(error.response?.data?.message || '删除附件失败')
  }
}

const removeSelectedFile = () => {
  if (documentForm.file && documentForm.hasOldFile) {
    documentForm.file = null
    fileList.value = [{
      uid: `old-${documentForm.id}`,
      name: documentForm.oldFileName,
      status: 'done',
      size: documentForm.oldFileSize || 0
    }]
    return true
  }
  documentForm.file = null
  documentForm.hasOldFile = false
  fileList.value = []
  return true
}

const changePublishStatus = async (record, checked) => {
  const nextStatus = checked ? 'PUBLISHED' : 'DRAFT'
  const previous = record.publishStatus
  record.publishStatus = nextStatus
  try {
    await updateKnowledgePublishStatus(record.id, nextStatus)
    message.success('发布状态已更新')
  } catch (error) {
    record.publishStatus = previous
    message.error(error.response?.data?.message || '状态更新失败')
  }
}

const retryKnowledgeDocument = async (record, strategy) => {
  retryingDocumentId.value = record.id
  try {
    await retryDocumentParse(record.id, strategy)
    message.success('文档已重新提交分析')
    await loadDocuments()
    if (detailDocument.value?.id === record.id) {
      await openDocumentDetail(record)
    }
  } catch (error) {
    message.error(error.response?.data?.message || error.message || '重新分析失败')
  } finally {
    retryingDocumentId.value = null
  }
}

const openDocumentDetail = async (record) => {
  detailDrawer.visible = true
  detailDrawer.loading = true
  detailDocument.value = null
  try {
    const response = await getKnowledgeDocumentDetail(record.id)
    detailDocument.value = response?.data || response || record
  } catch (error) {
    message.error(error.response?.data?.message || '加载文档详情失败')
  } finally {
    detailDrawer.loading = false
  }
}

const openAttachmentPreview = async (record) => {
  previewLoading.value = true
  previewModal.visible = true
  previewModal.title = record.fileName || '附件预览'
  await nextTick()
  try {
    const response = await getKnowledgeDocumentPreviewConfig(record.id, 'view')
    const previewData = response?.data || response
    await loadOnlyOfficeScript(previewData.documentServerApiUrl)
    destroyOnlyOfficeEditor()
    await nextTick()
    resizeOnlyOfficePreview()
    onlyOfficeEditor = new window.DocsAPI.DocEditor(onlyOfficeContainerId, previewData.config)
    setTimeout(resizeOnlyOfficePreview, 100)
    setTimeout(resizeOnlyOfficePreview, 500)
  } catch (error) {
    previewModal.visible = false
    message.error(error.response?.data?.message || error.message || '打开附件预览失败')
  } finally {
    previewLoading.value = false
  }
}

const downloadDetailAttachment = (record) => {
  if (!record?.id) {
    message.info('附件不存在')
    return
  }
  const link = document.createElement('a')
  link.href = `/api/ai/knowledge/documents/${record.id}/attachment`
  link.download = record.fileName || ''
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const closePreview = () => {
  destroyOnlyOfficeEditor()
  previewModal.visible = false
}

const handlePreviewOpenChange = (open) => {
  if (open) {
    nextTick(resizeOnlyOfficePreview)
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

const destroyOnlyOfficeEditor = () => {
  if (onlyOfficeEditor?.destroyEditor) {
    onlyOfficeEditor.destroyEditor()
  }
  onlyOfficeEditor = null
}

const resizeOnlyOfficePreview = () => {
  const root = document.querySelector('.onlyoffice-preview-modal')
  const modal = root?.querySelector('.ant-modal')
  const content = root?.querySelector('.ant-modal-content')
  const header = root?.querySelector('.ant-modal-header')
  const body = root?.querySelector('.ant-modal-body')
  const shell = root?.querySelector('.onlyoffice-preview-shell')
  const spin = root?.querySelector('.ant-spin-nested-loading')
  const spinContainer = root?.querySelector('.ant-spin-container')
  const frame = document.getElementById(onlyOfficeContainerId)
  if (!root || !content || !body || !frame) return
  const contentHeight = Math.max(window.innerHeight - 32, 620)
  const headerHeight = header?.getBoundingClientRect().height || 56
  const bodyHeight = Math.max(contentHeight - headerHeight, 540)
  if (modal) {
    modal.style.width = '75vw'
    modal.style.maxWidth = '75vw'
  }
  content.style.height = `${contentHeight}px`
  content.style.display = 'flex'
  content.style.flexDirection = 'column'
  body.style.flex = '1'
  body.style.minHeight = '0'
  body.style.overflow = 'hidden'
  body.style.padding = '12px'
  body.style.height = `${bodyHeight}px`
  if (shell) shell.style.height = '100%'
  if (spin) spin.style.height = '100%'
  if (spinContainer) spinContainer.style.height = '100%'
  frame.style.height = `${Math.max(bodyHeight - 24, 516)}px`
  frame.style.minHeight = '0'
}

const sanitizeContent = (html) => DOMPurify.sanitize(html || '')

const goDocumentChunks = (record) => {
  router.push({
    path: '/ai/documents',
    query: { documentId: record.id }
  })
}

const removeDocument = async (record) => {
  try {
    await deleteKnowledgeDocument(record.id)
    message.success('知识文档已删除')
    await loadDocuments()
  } catch (error) {
    message.error(error.response?.data?.message || '删除失败')
  }
}

const execEditorCommand = (command) => {
  editorRef.value?.focus()
  document.execCommand(command, false, null)
  syncEditorContent()
}

const syncEditorContent = () => {
  documentForm.contentHtml = editorRef.value?.innerHTML || ''
}

const toTreeData = (tree) => tree.map(item => ({
  ...item,
  key: item.id,
  value: item.id,
  title: item.name,
  children: toTreeData(item.children || [])
}))

const filterTree = (tree, keyword) => {
  const result = []
  for (const node of tree) {
    const children = filterTree(node.children || [], keyword)
    if (node.name.includes(keyword) || children.length > 0) {
      result.push({ ...node, children })
    }
  }
  return result
}

const flattenCategories = (tree) => {
  const list = []
  const walk = (nodes) => {
    nodes.forEach(node => {
      list.push(node)
      walk(node.children || [])
    })
  }
  walk(tree || [])
  return list
}

const findCategoryName = (id) => flattenCategories(categories.value).find(item => item.id === id)?.name

const stripHtml = (html) => (html || '')
  .replace(/<br\s*\/?>/gi, '\n')
  .replace(/<[^>]+>/g, '')
  .replace(/&nbsp;/g, ' ')
  .trim()

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

const getSourceText = (sourceType) => {
  const map = { CONTENT: '内容', FILE: '文件', MIXED: '混合' }
  return map[sourceType] || '-'
}

const getSourceColor = (sourceType) => {
  const map = { CONTENT: 'cyan', FILE: 'geekblue', MIXED: 'gold' }
  return map[sourceType] || 'default'
}

const getParseText = (status) => {
  const map = { SUCCESS: '成功', PROCESSING: '处理中', FAILED: '失败' }
  return map[status] || status || '-'
}

const isMineruAnalysisFile = (fileType) => MINERU_ANALYSIS_TYPES.includes((fileType || '').toLowerCase())

const isLegacyAnalysisFile = (fileType) => ['txt', 'md', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'pdf'].includes((fileType || '').toLowerCase())

const getParserText = (record) => {
  if (!record?.fileName) return '正文索引'
  if (record.parseEngine === 'MINERU') return 'MinerU 智能识别'
  if (record.parseEngine === 'LEGACY') return '兼容解析'
  if (record.parseEngine === 'AUTO') {
    return isMineruAnalysisFile(record.fileType) ? 'MinerU 智能识别' : '兼容解析'
  }
  return isMineruAnalysisFile(record.fileType) ? '自动识别' : '兼容解析'
}

const getParseError = (record) => record?.errorMessage || record?.parseJobErrorMessage || ''

const getDetailParseDescription = (record) => {
  const errorMessage = getParseError(record)
  if (errorMessage) return errorMessage
  if (record?.parseStatus === 'PROCESSING') {
    return `当前进度 ${record.parseProgress || 0}%${record.parseRetryCount ? `，已重试 ${record.parseRetryCount} 次` : ''}`
  }
  return '附件内容正在按文件类型建立检索索引。'
}

onMounted(async () => {
  await loadCategories()
  await loadDocuments()
  window.addEventListener('resize', resizeOnlyOfficePreview)
  documentPollingTimer = window.setInterval(() => {
    if (documents.value.some(item => item.parseStatus === 'PROCESSING') && !documentLoading.value) {
      loadDocuments()
    }
  }, 3000)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeOnlyOfficePreview)
  if (documentPollingTimer) {
    window.clearInterval(documentPollingTimer)
  }
  destroyOnlyOfficeEditor()
})
</script>

<style scoped>
.knowledge-page {
  min-height: calc(100vh - 112px);
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
  color: #172033;
}

.category-panel,
.document-table-panel {
  background: #fff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.category-panel {
  min-height: 640px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.category-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px;
  background: #fbfdff;
  border-bottom: 1px solid #e3eaf4;
  color: #172033;
}

.category-head h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.category-head p {
  margin: 4px 0 0;
  color: #7a8798;
  font-size: 12px;
}

.category-search {
  padding: 14px;
}

.tree-shell {
  flex: 1;
  padding: 0 10px 16px;
  overflow: auto;
}

.tree-empty {
  padding-top: 80px;
}

.category-node {
  min-height: 36px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding-right: 4px;
}

.category-label {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 32px;
}

.category-icon {
  color: #24706e;
  font-size: 16px;
  flex: 0 0 auto;
}

.category-name {
  min-width: 0;
  overflow: hidden;
  color: #26364d;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-action {
  opacity: 0;
  color: #64748b;
}

.category-node:hover .node-action {
  opacity: 1;
}

.document-workbench {
  min-width: 0;
}

.workbench-header {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
  padding: 20px 22px;
  background: #f7fbfa;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
}

.workbench-header h1 {
  margin: 0;
  color: #172033;
  font-size: 26px;
  font-weight: 750;
}

.header-copy {
  margin: 8px 0 0;
  color: #637083;
}

.header-metrics {
  display: grid;
  grid-template-columns: repeat(2, 110px);
  gap: 10px;
}

.header-metrics div {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 84px;
  padding: 12px;
  background: #fff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
}

.header-metrics strong {
  color: #12343b;
  font-size: 24px;
  line-height: 1;
}

.header-metrics span {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.keyword-input {
  max-width: 320px;
}

.status-select {
  width: 140px;
}

.document-table-panel {
  overflow: hidden;
}

.action-buttons {
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
}

.action-icon-btn {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.parse-status-cell {
  display: grid;
  gap: 3px;
  min-width: 122px;
}

.parse-engine {
  overflow: hidden;
  color: #65758a;
  font-size: 12px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.parse-status-cell :deep(.ant-progress) {
  margin: 0;
}

.parse-error {
  display: inline-block;
  max-width: 180px;
  overflow: hidden;
  color: #bf5b2b;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.doc-title-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.doc-title-link {
  height: auto;
  max-width: 280px;
  padding: 0;
  color: #172033;
  font-size: 14px;
  font-weight: 650;
  line-height: 1.35;
  text-align: left;
  white-space: normal;
}

.doc-title-cell span {
  color: #7a8798;
  font-size: 12px;
}

.document-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.file-analysis-hint {
  margin-top: 8px;
  color: #637083;
  font-size: 12px;
  line-height: 1.65;
}

.detail-parse-alert {
  margin-top: 14px;
}

.editor-toolbar {
  display: flex;
  gap: 8px;
  padding: 8px;
  background: #f8fafc;
  border: 1px solid #dbe3ee;
  border-bottom: none;
  border-radius: 8px 8px 0 0;
}

.rich-editor {
  min-height: 180px;
  max-height: 280px;
  padding: 12px 14px;
  overflow: auto;
  color: #172033;
  line-height: 1.7;
  border: 1px solid #dbe3ee;
  border-radius: 0 0 8px 8px;
  outline: none;
}

.rich-editor:empty::before {
  color: #94a3b8;
  content: attr(data-placeholder);
}

.form-attachment-row {
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  padding: 0 6px 0 10px;
  background: #f8fafc;
  border: 1px solid #e3eaf4;
  border-radius: 6px;
}

.form-attachment-title {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0;
  overflow: hidden;
  color: #172033;
  font: inherit;
  line-height: 32px;
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.form-attachment-title:hover {
  color: #1d4ed8;
}

.form-attachment-title span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-attachment-actions {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  flex: 0 0 auto;
}

.attachment-icon-btn {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.attachment-delete-btn {
  color: #cf1322;
}

.attachment-delete-btn:hover {
  color: #ff4d4f;
  background: #fff1f0;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 1px solid #e3eaf4;
}

.detail-head h2 {
  margin: 0;
  color: #172033;
  font-size: 22px;
  font-weight: 750;
  line-height: 1.35;
}

.detail-head p {
  margin: 6px 0 0;
  color: #64748b;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 18px 0;
}

.detail-grid div {
  min-height: 72px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e3eaf4;
  border-radius: 8px;
}

.detail-grid span,
.attachment-meta span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.detail-grid strong {
  display: block;
  margin-top: 8px;
  color: #172033;
  font-size: 14px;
  font-weight: 650;
}

.detail-section {
  margin-top: 20px;
}

.section-title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 15px;
  font-weight: 700;
}

.attachment-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  background: #fbfdff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
}

.attachment-meta {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.attachment-title-button {
  flex: 1;
  padding: 0;
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.attachment-title-button:hover strong {
  color: #1d4ed8;
}

.attachment-meta .anticon {
  color: #24706e;
  font-size: 22px;
}

.attachment-meta div {
  min-width: 0;
}

.attachment-meta strong {
  display: block;
  overflow: hidden;
  color: #172033;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content-preview {
  min-height: 160px;
  max-height: 360px;
  padding: 14px;
  overflow: auto;
  color: #172033;
  line-height: 1.7;
  background: #fff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
}

.content-preview :deep(p) {
  margin: 0 0 10px;
}

.onlyoffice-frame {
  height: 100%;
  min-height: 520px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
}

.onlyoffice-frame :deep(> div) {
  height: 100% !important;
}

.onlyoffice-frame :deep(iframe) {
  width: 100% !important;
  height: 100% !important;
}

.document-table-panel :deep(.ant-table-thead > tr > th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 650;
}

.tree-shell :deep(.ant-tree-treenode) {
  width: 100%;
  align-items: center;
  padding: 2px 0;
}

.tree-shell :deep(.ant-tree-switcher) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 36px;
  line-height: 36px;
}

.tree-shell :deep(.ant-tree-node-content-wrapper) {
  flex: 1;
  min-width: 0;
  height: 36px;
  display: inline-flex;
  align-items: center;
  padding: 0 6px;
  border-radius: 7px;
  line-height: 36px;
}

.tree-shell :deep(.ant-tree-title) {
  width: 100%;
}

.tree-shell :deep(.ant-tree-node-selected) {
  background: #e8f6f3 !important;
}

@media (max-width: 1180px) {
  .knowledge-page {
    grid-template-columns: 1fr;
  }

  .category-panel {
    min-height: auto;
  }
}

@media (max-width: 760px) {
  .workbench-header,
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-metrics {
    width: 100%;
    grid-template-columns: repeat(2, 1fr);
  }

  .keyword-input,
  .status-select {
    width: 100%;
    max-width: none;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .attachment-box {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>

<style>
.onlyoffice-preview-modal .ant-modal {
  width: 75vw !important;
  max-width: 75vw;
  top: 0;
  padding-bottom: 0;
}

.onlyoffice-preview-modal .ant-modal-content {
  height: calc(100vh - 32px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.onlyoffice-preview-modal .ant-modal-header {
  flex: 0 0 auto;
}

.onlyoffice-preview-modal .ant-modal-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 12px;
}

.onlyoffice-preview-modal .onlyoffice-preview-shell {
  height: 100%;
  min-height: 0;
}

.onlyoffice-preview-modal .ant-spin-nested-loading,
.onlyoffice-preview-modal .ant-spin-container {
  height: 100%;
}

.onlyoffice-preview-modal .onlyoffice-frame {
  height: 100%;
  min-height: 0;
}

.onlyoffice-preview-modal .onlyoffice-frame > div,
.onlyoffice-preview-modal .onlyoffice-frame iframe {
  width: 100% !important;
  height: 100% !important;
}

@media (max-width: 900px) {
  .onlyoffice-preview-modal .ant-modal {
    width: calc(100vw - 24px) !important;
    max-width: calc(100vw - 24px);
  }
}
</style>
