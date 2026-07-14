/**
 * 智能问答模块API服务
 * 封装与后端AI服务的交互逻辑
 */
import axios from 'axios'

// ==================== API基础配置 ====================
const BASE_URL = '/api/ai'

// 创建axios实例
const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 60000
})

// 响应拦截器
apiClient.interceptors.response.use(
  response => {
    const result = response.data
    if (result && typeof result.code === 'number' && result.code !== 200) {
      const error = new Error(result.message || '请求失败')
      error.response = { data: result }
      return Promise.reject(error)
    }
    return result
  },
  error => {
    console.error('AI API请求失败:', error)
    return Promise.reject(error)
  }
)

// ==================== 会话管理API ====================

/**
 * 获取会话列表
 * @returns {Promise<Array>} 会话列表
 */
export const getConversations = () => {
  return apiClient.get('/conversations')
}

/**
 * 创建新会话
 * @param {string} title - 会话标题
 * @returns {Promise<Object>} 新会话信息
 */
export const createConversation = (title = '新对话') => {
  return apiClient.post('/conversations', { title })
}

/**
 * 删除会话
 * @param {number} id - 会话ID
 * @returns {Promise<void>}
 */
export const deleteConversation = (id) => {
  return apiClient.delete(`/conversations/${id}`)
}

/**
 * 获取会话消息列表
 * @param {number} conversationId - 会话ID
 * @returns {Promise<Array>} 消息列表
 */
export const getMessages = (conversationId) => {
  return apiClient.get(`/conversations/${conversationId}/messages`)
}

// ==================== 文档分片API ====================

/**
 * 上传文档并按指定策略触发解析
 * @param {File} file - 可分片解析的文档文件
 * @returns {Promise<Object>} 上传解析结果
 */
export const uploadDocument = (file, strategy = 'AUTO') => {
  const formData = new FormData()
  formData.append('file', file)
  return apiClient.post('/documents/upload', formData, {
    params: { strategy },
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

/**
 * 获取文档列表
 * @returns {Promise<Object>} 文档列表响应
 */
export const getDocuments = () => {
  return apiClient.get('/documents')
}

/**
 * 获取文档 Chunk 列表
 * @param {number} documentId - 文档ID
 * @returns {Promise<Object>} Chunk 列表响应
 */
export const getDocumentChunks = (documentId) => {
  return apiClient.get(`/documents/${documentId}/chunks`)
}

/**
 * 删除文档及其 Chunk
 * @param {number} documentId - 文档ID
 * @returns {Promise<Object>} 删除响应
 */
export const deleteDocument = (documentId) => {
  return apiClient.delete(`/documents/${documentId}`)
}

export const getDocumentParseJob = (documentId) => {
  return apiClient.get(`/documents/${documentId}/parse-job`)
}

export const retryDocumentParse = (documentId, strategy) => {
  return apiClient.post(`/documents/${documentId}/parse/retry`, { strategy })
}

/**
 * 获取文档分片配置
 * @returns {Promise<Object>} 分片配置响应
 */
export const getSplitConfig = () => {
  return apiClient.get('/documents/split-config')
}

/**
 * 保存文档分片配置，仅影响后续上传文档
 * @param {Object} config - 分片配置
 * @returns {Promise<Object>} 保存后的配置响应
 */
export const saveSplitConfig = (config) => {
  return apiClient.put('/documents/split-config', config)
}

// ==================== 知识库API ====================

export const getKnowledgeCategories = () => {
  return apiClient.get('/knowledge/categories')
}

export const createKnowledgeCategory = (data) => {
  return apiClient.post('/knowledge/categories', data)
}

export const updateKnowledgeCategory = (id, data) => {
  return apiClient.put(`/knowledge/categories/${id}`, data)
}

export const deleteKnowledgeCategory = (id) => {
  return apiClient.delete(`/knowledge/categories/${id}`)
}

export const getKnowledgeDocuments = (params = {}) => {
  return apiClient.get('/knowledge/documents', { params })
}

export const getKnowledgeDocumentDetail = (id) => {
  return apiClient.get(`/knowledge/documents/${id}`)
}

export const createKnowledgeDocument = (data) => {
  const formData = buildKnowledgeDocumentFormData(data)
  return apiClient.post('/knowledge/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export const updateKnowledgeDocument = (id, data) => {
  const formData = buildKnowledgeDocumentFormData(data)
  return apiClient.put(`/knowledge/documents/${id}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export const updateKnowledgePublishStatus = (id, publishStatus) => {
  return apiClient.put(`/knowledge/documents/${id}/publish-status`, { publishStatus })
}

export const getKnowledgeDocumentChunks = (id) => {
  return apiClient.get(`/knowledge/documents/${id}/chunks`)
}

export const getKnowledgeDocumentPreviewConfig = (id, mode = 'view') => {
  return apiClient.get(`/knowledge/documents/${id}/preview-config`, { params: { mode } })
}

export const deleteKnowledgeDocumentAttachment = (id) => {
  return apiClient.delete(`/knowledge/documents/${id}/attachment`)
}

export const deleteKnowledgeDocument = (id) => {
  return apiClient.delete(`/knowledge/documents/${id}`)
}

const buildKnowledgeDocumentFormData = (data) => {
  const formData = new FormData()
  formData.append('categoryId', data.categoryId)
  formData.append('title', data.title)
  formData.append('contentHtml', data.contentHtml || '')
  formData.append('publishStatus', data.publishStatus || 'DRAFT')
  if (data.file) {
    formData.append('file', data.file)
  }
  return formData
}

// ==================== SSE流式聊天API ====================

/**
 * 发送流式聊天消息（SSE）
 * @param {Object} params - 请求参数
 * @param {number} params.conversationId - 会话ID
 * @param {string} params.message - 用户消息
 * @param {string} params.model - AI模型
 * @param {string} params.scope - 问答范围
 * @param {Function} params.onMessage - 消息回调
 * @param {Function} params.onDone - 完成回调
 * @param {Function} params.onError - 错误回调
 * @returns {AbortController} 中断控制器
 */
export const sendStreamMessage = ({ conversationId, message, model, scope = 'local_knowledge', onMessage, onDone, onError }) => {
  const controller = new AbortController()
  
  fetch(`${BASE_URL}/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ conversationId, message, model, scope }),
    signal: controller.signal
  })
    .then(response => {
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      return response.body.getReader()
    })
    .then(reader => {
      const decoder = new TextDecoder()
      let buffer = ''
      let currentEvent = ''
      let currentData = ''
      
      const processStream = ({ done, value }) => {
        if (done) {
          buffer += decoder.decode()
          if (buffer.trim()) {
            if (buffer.startsWith('event:')) {
              currentEvent = buffer.slice(6).trim()
            } else if (buffer.startsWith('data:')) {
              currentData = buffer.slice(5).trim()
            }
          }
          if (currentData) {
            try {
              const jsonData = JSON.parse(currentData)
              if (currentEvent === 'content' && jsonData.content && onMessage) {
                onMessage(jsonData.content)
              } else if (currentEvent === 'done') {
                onDone && onDone(jsonData)
              } else if (currentEvent === 'error' || jsonData.errorCode) {
                onError && onError(new Error(jsonData.message || 'AI服务错误'))
              }
            } catch (e) {
              // 解析失败，忽略
            }
          }
          return
        }
        
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        
        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            currentData = line.slice(5).trim()
          } else if (line.trim() === '') {
            if (currentData) {
              try {
                const jsonData = JSON.parse(currentData)
                
                if (currentEvent === 'content' && jsonData.content && onMessage) {
                  onMessage(jsonData.content)
                } else if (currentEvent === 'done') {
                  onDone && onDone(jsonData)
                } else if (currentEvent === 'error') {
                  onError && onError(new Error(jsonData.message || 'AI服务错误'))
                } else if (jsonData.errorCode) {
                  onError && onError(new Error(jsonData.message || 'AI服务错误'))
                }
              } catch (e) {
                // JSON解析失败，忽略
              }
              currentData = ''
            }
            currentEvent = ''
          }
        }
        
        reader.read().then(processStream)
      }
      
      reader.read().then(processStream)
    })
    .catch(error => {
      if (error.name !== 'AbortError') {
        onError && onError(error)
      }
    })
  
  return controller
}

// ==================== 导出API对象 ====================
export default {
  getConversations,
  createConversation,
  deleteConversation,
  getMessages,
  uploadDocument,
  getDocuments,
  getDocumentChunks,
  deleteDocument,
  getSplitConfig,
  saveSplitConfig,
  sendStreamMessage
}
