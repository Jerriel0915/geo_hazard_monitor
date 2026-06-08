// src/utils/hazard.ts

export interface Hazard {
  id: number
  name: string
  level: string
  location: string
  status: string
  deviceCount: number
  description?: string
  createTime: string
}

export interface HazardWithDevices extends Hazard {
  devices: any[]
}

const mockHazards: Hazard[] = [
  { id: 1, name: 'K15+200 右侧边坡', level: '高风险', location: 'G65包茂高速K15+200右侧', status: '监测中', deviceCount: 3, description: '该边坡高度大于30m，岩体破碎，存在滑坡风险', createTime: '2025-03-15' },
  { id: 2, name: 'K23+500 左侧边坡', level: '中风险', location: 'G65包茂高速K23+500左侧', status: '监测中', deviceCount: 4, description: '该边坡坡度较陡，雨季存在溜坍风险', createTime: '2025-04-20' },
  { id: 3, name: 'K31+100 右侧边坡', level: '低风险', location: 'G65包茂高速K31+100右侧', status: '监测中', deviceCount: 2, description: '该边坡表层风化，需要持续关注', createTime: '2025-05-10' },
  { id: 4, name: 'K42+800 左侧边坡', level: '高风险', location: 'G65包茂高速K42+800左侧', status: '监测中', deviceCount: 3, description: '该边坡曾发生小型塌方，加固后持续监测', createTime: '2025-02-28' },
  { id: 5, name: 'K56+300 右侧边坡', level: '中风险', location: 'G65包茂高速K56+300右侧', status: '已处置', deviceCount: 2, description: '已完成锚固施工，持续监测稳定性', createTime: '2025-01-15' },
]

export const hazardApi = {
  getAll(): Hazard[] {
    return mockHazards
  },

  getById(id: number): HazardWithDevices | undefined {
    const hazard = mockHazards.find(h => h.id === id)
    if (!hazard) return undefined
    return { ...hazard, devices: [] }
  },

  getStats(): { total: number; high: number; medium: number; low: number } {
    return {
      total: mockHazards.length,
      high: mockHazards.filter(h => h.level === '高风险').length,
      medium: mockHazards.filter(h => h.level === '中风险').length,
      low: mockHazards.filter(h => h.level === '低风险').length,
    }
  }
}

export default hazardApi
