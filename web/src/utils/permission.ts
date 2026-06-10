import {type AuthInfo, getAuthInfo} from './userApi'
import {ref} from 'vue'

export const permissions = ref<string[]>([])
let loaded = false

/** 检查当前用户是否拥有指定权限（含 *:*:* 超级通配） */
export function hasPermission(perm: string): boolean {
  return permissions.value.includes('*:*:*') || permissions.value.includes(perm)
}

/** 加载用户权限集合（幂等，仅首次调用发起请求） */
export async function loadPermissions(): Promise<void> {
  if (loaded) return
  try {
    const info: AuthInfo = await getAuthInfo()
    permissions.value = info.permissions || []
  } catch {
    permissions.value = []
  }
  loaded = true
}
