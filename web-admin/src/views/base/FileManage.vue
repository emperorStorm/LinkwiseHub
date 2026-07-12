<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">文件管理</h2>
    </div>

    <div class="table-wrapper">
      <a-table 
        :columns="columns" 
        :data-source="files" 
        row-key="id"
        :pagination="{ pageSize: 10, showSizeChanger: true }"
        class="data-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'originalName'">
            <a-tooltip :title="record.originalName">
              <span class="file-name">{{ record.originalName }}</span>
            </a-tooltip>
          </template>
          <template v-if="column.key === 'fileSize'">
            {{ formatFileSize(record.fileSize) }}
          </template>
          <template v-if="column.key === 'module'">
            <a-tag :color="getModuleColor(record.module)">{{ record.module }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <span class="status-badge" :class="record.status === 1 ? 'status-active' : 'status-inactive'">
              {{ record.status === 1 ? '正常' : '已删除' }}
            </span>
          </template>
          <template v-if="column.key === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="previewFile(record)">
                <eye-outlined /> 预览
              </a-button>
              <a-popconfirm
                title="确定要删除该文件吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="deleteFile(record.id)"
              >
                <a-button type="link" size="small" danger>
                  <delete-outlined /> 删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:open="previewVisible"
      title="文件预览"
      :footer="null"
      width="800px"
    >
      <div class="preview-container">
        <a-image :src="previewUrl" style="max-width: 100%;" />
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { EyeOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import axios from 'axios'
import { message } from 'ant-design-vue'

const files = ref([])
const previewVisible = ref(false)
const previewUrl = ref('')

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '文件名', dataIndex: 'originalName', key: 'originalName', ellipsis: true },
  { title: '文件类型', dataIndex: 'fileSuffix', key: 'fileSuffix', width: 100 },
  { title: '文件大小', dataIndex: 'fileSize', key: 'fileSize', width: 100 },
  { title: '所属模块', dataIndex: 'module', key: 'module', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '上传时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 150 }
]

const loadFiles = async () => {
  try {
    const response = await axios.get('/api/base/file/list')
    files.value = response.data
  } catch (error) {
    message.error('加载文件列表失败')
  }
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 19)
}

const getModuleColor = (module) => {
  const colors = {
    'base': 'blue',
    'default': 'gray'
  }
  return colors[module] || 'gray'
}

const previewFile = (record) => {
  previewUrl.value = `/uploads/${record.fileName}`
  previewVisible.value = true
}

const deleteFile = async (id) => {
  try {
    await axios.delete(`/api/base/file/${id}`)
    message.success('删除成功')
    loadFiles()
  } catch (error) {
    message.error('删除失败')
  }
}

onMounted(() => {
  loadFiles()
})
</script>

<style scoped>
.page-container {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.table-wrapper {
  background: white;
  border-radius: 12px;
  overflow: hidden;
}

.data-table :deep(.ant-table-thead > tr > th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  border-bottom: 2px solid #e2e8f0;
}

.data-table :deep(.ant-table-tbody > tr:hover > td) {
  background: #f8fafc;
}

.file-name {
  max-width: 200px;
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-active {
  background: #dcfce7;
  color: #16a34a;
}

.status-inactive {
  background: #fee2e2;
  color: #dc2626;
}

.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}
</style>
