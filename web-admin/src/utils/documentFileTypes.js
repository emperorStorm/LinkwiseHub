export const DOCUMENT_ALLOWED_TYPES = [
  'txt', 'md', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'pdf',
  'png', 'jpg', 'jpeg', 'webp', 'tiff'
]

export const DOCUMENT_IMAGE_TYPES = ['png', 'jpg', 'jpeg', 'webp', 'tiff']

export const DOCUMENT_ACCEPT = DOCUMENT_ALLOWED_TYPES.map(type => `.${type}`).join(',')

export const DOCUMENT_SUPPORTED_TEXT = 'Word、PPT、Excel、PDF、图片、txt、md'
