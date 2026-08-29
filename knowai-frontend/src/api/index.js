import request from './request'

// ===== 认证 =====
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)

// ===== 知识库 =====
export const createKnowledge = (data) => request.post('/knowledge', data)
export const listKnowledge = () => request.get('/knowledge/list', {
  params: { pageNum: 1, pageSize: 20 }
})

// ===== 文档 =====
export const uploadDocument = (knowledgeId, file) => {
  const formData = new FormData()
  formData.append('knowledgeId', knowledgeId)
  formData.append('file', file)
  return request.post('/knowledge/document', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export const listDocuments = (knowledgeId) =>
  request.get('/knowledge/document', { params: { knowledgeId } })
// 触发解析（解析内部会自动向量化）
export const parseDocument = (documentId) =>
  request.post(`/document/parse/${documentId}`)

// ===== 问答 =====
export const chat = (data) => request.post('/chat', data)
