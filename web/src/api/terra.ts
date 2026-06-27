import request from '@/utils/request'
import type {
  TerraPersonality,
  TerraModelConfig,
  TerraSkill,
  TerraTool,
  TerraConversation,
  TerraMessageData,
} from '@/components/terra/types'

interface AjaxResult<T = any> {
  code: number
  msg: string
  data: T
}

const unwrap = async <T>(promise: Promise<AjaxResult<T>>): Promise<T> => {
  const res = await promise
  if (res && res.code !== 200) {
    throw new Error(res.msg || '操作失败')
  }
  return res.data
}

// ============ 人格配置 ============
export const getPersonalities = () => unwrap<TerraPersonality[]>(request.get('/terra/personality'))
export const updatePersonality = (data: Partial<TerraPersonality>) => unwrap<void>(request.put('/terra/personality', data))
export const togglePersonality = (id: number) => unwrap<void>(request.put(`/terra/personality/${id}/toggle`))

// ============ 模型配置 ============
export const getModelConfigs = () => unwrap<TerraModelConfig[]>(request.get('/terra/model-configs'))
export const getModelConfig = (id: number) => unwrap<TerraModelConfig>(request.get(`/terra/model-configs/${id}`))
export const createModelConfig = (data: Partial<TerraModelConfig>) => unwrap<void>(request.post('/terra/model-configs', data))
export const updateModelConfig = (data: Partial<TerraModelConfig>) => unwrap<void>(request.put('/terra/model-configs', data))
export const deleteModelConfig = (id: number) => unwrap<void>(request.delete(`/terra/model-configs/${id}`))
export const activateModelConfig = (id: number) => unwrap<void>(request.put(`/terra/model-configs/${id}/activate`))

// ============ 技能管理 ============
export const getSkills = () => unwrap<TerraSkill[]>(request.get('/terra/skills'))
export const getSkillDetail = (id: number) => unwrap<TerraSkill>(request.get(`/terra/skills/${id}`))
export const deleteSkill = (id: number) => unwrap<void>(request.delete(`/terra/skills/${id}`))
export const toggleSkill = (id: number) => unwrap<void>(request.put(`/terra/skills/${id}/toggle`))

// ============ 工具管理 ============
export const getTools = () => unwrap<TerraTool[]>(request.get('/terra/tools'))
export const createTool = (data: Partial<TerraTool>) => unwrap<void>(request.post('/terra/tools', data))
export const updateTool = (data: Partial<TerraTool>) => unwrap<void>(request.put('/terra/tools', data))
export const deleteTool = (id: number) => unwrap<void>(request.delete(`/terra/tools/${id}`))
export const toggleTool = (id: number) => unwrap<void>(request.put(`/terra/tools/${id}/toggle`))

// ============ 对话 & 会话 ============
export const getConversations = () => unwrap<TerraConversation[]>(request.get('/terra/conversations'))
export const getConversationMessages = (id: number) => unwrap<TerraMessageData[]>(request.get(`/terra/conversations/${id}/messages`))
export const createConversation = (title: string) => unwrap<TerraConversation>(request.post('/terra/conversations', { title }))
export const deleteConversation = (id: number) => unwrap<void>(request.delete(`/terra/conversations/${id}`))
export const postToolResult = (callId: string, success: boolean, result: unknown) => unwrap<void>(request.post('/terra/chat/tool-result', { callId, success, result }))
