/**
 * 智能问答模块工具函数
 * 包含错误处理、时间格式化等通用方法
 */
import { Modal } from 'ant-design-vue'
import { Marked } from 'marked'
import DOMPurify from 'dompurify'

// ==================== Markdown渲染 ====================

// 创建Marked实例，marked 18.x版本使用Marked类替代setOptions
const markedInstance = new Marked({
  breaks: true,
  gfm: true
})

/**
 * 将Markdown内容转换为安全的HTML
 * @param {string} content - 原始内容
 * @returns {string} 安全的HTML字符串
 */
export const renderMarkdown = (content) => {
  if (!content) return ''
  const html = markedInstance.parse(content)
  // marked.parse在v18可能返回Promise，确保同步获取结果
  if (html instanceof Promise) {
    console.warn('marked.parse返回了Promise，这不应该发生')
    return content
  }
  return DOMPurify.sanitize(html)
}

// ==================== 错误码定义 ====================

export const ErrorCodes = {
  // AI服务错误 (4000-4999)
  AI_SERVICE_ERROR: '4000',
  AI_SERVICE_TIMEOUT: '4001',
  AI_SERVICE_UNAVAILABLE: '4002',
  AI_CONFIG_MISSING: '4003',
  AI_API_KEY_INVALID: '4004',
  AI_API_KEY_EXPIRED: '4005',
  AI_QUOTA_EXCEEDED: '4006',
  AI_RATE_LIMITED: '4007',
  AI_MODEL_UNAVAILABLE: '4008',
  AI_PARSE_ERROR: '4009',
  AI_NETWORK_ERROR: '4010',
  AI_RETRY_EXHAUSTED: '4011',
  
  // 网络错误 (5000-5999)
  NETWORK_ERROR: '5000',
  NETWORK_TIMEOUT: '5001',
  CONNECTION_REFUSED: '5002',
  
  // 业务错误 (2000-2999)
  MESSAGE_EMPTY: '2003',
  MESSAGE_TOO_LONG: '2004',
  CONVERSATION_NOT_FOUND: '3001'
}

// ==================== 错误消息映射 ====================

const ERROR_MESSAGES = {
  [ErrorCodes.AI_SERVICE_TIMEOUT]: 'AI响应较慢，请检查网络后重试',
  [ErrorCodes.AI_SERVICE_UNAVAILABLE]: 'AI服务暂时不可用，请稍后重试',
  [ErrorCodes.AI_API_KEY_INVALID]: 'AI服务配置异常，请联系管理员',
  [ErrorCodes.AI_API_KEY_EXPIRED]: 'AI服务授权已过期，请联系管理员',
  [ErrorCodes.AI_QUOTA_EXCEEDED]: 'AI服务配额已用完，请明天再试',
  [ErrorCodes.AI_RATE_LIMITED]: '请求过于频繁，请稍后再试',
  [ErrorCodes.AI_NETWORK_ERROR]: '网络连接失败，请检查网络后重试',
  [ErrorCodes.NETWORK_TIMEOUT]: '网络请求超时，请重试',
  [ErrorCodes.CONNECTION_REFUSED]: '无法连接到服务器，请检查网络',
  [ErrorCodes.MESSAGE_EMPTY]: '请输入您的问题',
  [ErrorCodes.MESSAGE_TOO_LONG]: '输入内容过长，请精简后重试',
  [ErrorCodes.CONVERSATION_NOT_FOUND]: '会话不存在，将为您创建新会话'
}

/**
 * 获取友好的错误提示
 * @param {string} errorCode - 错误码
 * @param {string} defaultMessage - 默认消息
 * @returns {string} 友好的错误提示
 */
export const getFriendlyErrorMessage = (errorCode, defaultMessage) => {
  return ERROR_MESSAGES[errorCode] || defaultMessage || '操作失败，请稍后重试'
}

/**
 * 判断是否为AI服务错误
 * @param {string} errorCode - 错误码
 * @returns {boolean}
 */
export const isAiServiceError = (errorCode) => {
  return errorCode && errorCode.startsWith('4')
}

/**
 * 判断是否为网络错误
 * @param {Error} error - 错误对象
 * @returns {boolean}
 */
export const isNetworkError = (error) => {
  if (!error) return false
  if (error.code === 'ECONNREFUSED' || error.code === 'ETIMEDOUT') return true
  const msg = (error.message || '').toLowerCase()
  return msg.includes('network') || 
         msg.includes('timeout') || 
         msg.includes('connect') ||
         msg.includes('refused')
}

// ==================== 错误处理 ====================

/**
 * 处理API错误
 * @param {Error} error - 错误对象
 * @param {string} fallbackMessage - 默认错误消息
 * @returns {Object} 包含errorCode和errorMessage的对象
 */
export const handleApiError = (error, fallbackMessage) => {
  console.error('API请求失败:', error)
  
  let errorMessage = fallbackMessage || '操作失败'
  let errorCode = ''
  let showRetryOption = false
  
  if (error.response) {
    const { data } = error.response
    errorCode = data?.errorCode || ''
    errorMessage = data?.message || errorMessage
    
    if (isAiServiceError(errorCode) || isNetworkError(error)) {
      showRetryOption = true
    }
  } else if (error.request) {
    errorCode = ErrorCodes.NETWORK_ERROR
    errorMessage = getFriendlyErrorMessage(ErrorCodes.NETWORK_ERROR, '网络连接失败')
    showRetryOption = true
  } else {
    errorMessage = '请求配置错误'
  }
  
  const friendlyMessage = getFriendlyErrorMessage(errorCode, errorMessage)
  
  if (showRetryOption) {
    Modal.warning({
      title: '提示',
      content: friendlyMessage,
      okText: '重试',
      cancelText: '确定'
    })
  } else {
    const { message } = require('ant-design-vue')
    message.error(friendlyMessage)
  }
  
  return { errorCode, errorMessage: friendlyMessage, showRetryOption }
}

// ==================== 时间格式化 ====================

/**
 * 格式化时间为 yyyy-MM-dd HH:mm:ss 格式
 * @param {string|Date} time - 时间字符串或Date对象
 * @returns {string} 格式化后的时间字符串
 */
export const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// ==================== 消息处理 ====================

/**
 * 创建用户消息对象
 * @param {string} content - 消息内容
 * @returns {Object} 用户消息对象
 */
export const createUserMessage = (content) => ({
  id: `temp-${Date.now()}`,
  role: 'user',
  content,
  createTime: new Date().toISOString()
})

/**
 * 创建AI消息对象
 * @param {boolean} isStreaming - 是否为流式消息
 * @param {Object} extra - 需要固化到消息上的模型等附加信息
 * @returns {Object} AI消息对象
 */
export const createAssistantMessage = (isStreaming = false, extra = {}) => ({
  id: `ai-${Date.now()}`,
  role: 'assistant',
  content: '',
  createTime: new Date().toISOString(),
  isStreaming,
  ...extra
})

/**
 * 创建错误消息对象
 * @param {string} content - 错误内容
 * @param {Object} extra - 需要固化到消息上的模型等附加信息
 * @returns {Object} 错误消息对象
 */
export const createErrorMessage = (content = '抱歉，消息发送失败。请稍后重试。', extra = {}) => ({
  id: `error-${Date.now()}`,
  role: 'assistant',
  content: `⚠️ ${content}`,
  createTime: new Date().toISOString(),
  isError: true,
  ...extra
})

// ==================== 导出 ====================

export default {
  renderMarkdown,
  ErrorCodes,
  getFriendlyErrorMessage,
  isAiServiceError,
  isNetworkError,
  handleApiError,
  formatTime,
  createUserMessage,
  createAssistantMessage,
  createErrorMessage
}
