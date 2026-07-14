export const DOCUMENT_ALLOWED_TYPES = [
  'txt', 'md', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'pdf',
  'png', 'jpg', 'jpeg', 'webp', 'tiff'
]

export const DOCUMENT_IMAGE_TYPES = ['png', 'jpg', 'jpeg', 'webp', 'tiff']

export const MINERU_ANALYSIS_TYPES = [
  'pdf', 'docx', 'pptx', 'xlsx',
  ...DOCUMENT_IMAGE_TYPES
]

export const DOCUMENT_ANALYSIS_TEXT = '支持 TXT、Markdown、DOC、DOCX、PPT、PPTX、XLS、XLSX、PDF、PNG、JPG、JPEG、WEBP、TIFF，最大 10MB。PDF、现代 Office 文档和图片自动使用 MinerU 智能识别；TXT、Markdown、旧版 Office 文档使用兼容解析。'

export const DOCUMENT_ACCEPT = DOCUMENT_ALLOWED_TYPES.map(type => `.${type}`).join(',')

export const DOCUMENT_SUPPORTED_TEXT = 'Word、PPT、Excel、PDF、图片、txt、md'
