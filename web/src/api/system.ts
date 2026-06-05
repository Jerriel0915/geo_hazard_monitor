import request from '@/utils/request'

export interface AjaxResult<T = any> {
  code: number
  msg: string
  data: T
  timestamp?: number
  [key: string]: any
}

export interface PageResult<T> {
  rows: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface OrganizationItem {
  id: number
  code: string
  name: string
  parentId: number
  parentIds?: string
  level?: number
  leader?: string
  phone?: string
  email?: string
  region?: string
  address?: string
  status?: number
  sortOrder?: number
  createTime?: string
  updateTime?: string
  children?: OrganizationItem[]
}

export interface OrganizationPayload {
  code: string
  name: string
  parentId: number
  leader?: string
  phone?: string
  email?: string
  region?: string
  address?: string
  status?: number
  sortOrder?: number
}

export interface UserItem {
  id: number
  username: string
  realName: string
  avatar?: string
  phone?: string
  email?: string
  orgId?: number
  orgName?: string
  status?: number
  lastLoginTime?: string
  createTime?: string
  createBy?: string
  updateTime?: string
  updateBy?: string
  remark?: string
  roleIds?: number[]
  postIds?: number[]
}

export interface UserPayload {
  username?: string
  password?: string
  realName?: string
  phone?: string
  email?: string
  orgId?: number
  status?: number
  roleIds?: number[]
  remark?: string
}

export interface RoleItem {
  id: number
  code: string
  name: string
  description?: string
  dataScope?: number
  sortOrder?: number
  status?: number
  menuIds?: number[]
  createTime?: string
}

export interface RolePayload {
  code?: string
  name?: string
  description?: string
  dataScope?: number
  sortOrder?: number
  status?: number
  menuIds?: number[]
  deptIds?: number[]
}

export interface MenuItem {
  id: number
  parentId: number
  name: string
  code?: string
  path?: string
  component?: string
  icon?: string
  type?: number
  visible?: number
  isCache?: number
  sortOrder?: number
  perms?: string
  status?: number
  createTime?: string
  children?: MenuItem[]
}

export interface MenuPayload {
  parentId: number
  name: string
  code?: string
  path?: string
  component?: string
  icon?: string
  type: number
  visible?: number
  isCache?: number
  sortOrder?: number
  perms?: string
  status?: number
}

export interface TreeOption {
  id: number
  label: string
  children?: TreeOption[]
}

export interface RoleDeptTreeResult {
  checkedKeys: number[]
  depts: TreeOption[]
}

const unwrap = async <T>(promise: Promise<AjaxResult<T>>): Promise<T> => {
  const response = await promise
  return response.data
}

const mapMenuTypeToBackend = (type: number) => {
  const map: Record<number, string> = {
    0: 'M',
    1: 'C',
    2: 'F'
  }
  return map[type] ?? 'C'
}

const mapMenuPayload = (payload: MenuPayload) => ({
  parentId: payload.parentId,
  menuName: payload.name,
  routeName: payload.code,
  path: payload.path,
  component: payload.component,
  icon: payload.icon,
  menuType: mapMenuTypeToBackend(payload.type),
  visible: payload.visible === undefined ? undefined : String(payload.visible),
  isCache: payload.isCache === undefined ? undefined : String(payload.isCache),
  orderNum: payload.sortOrder,
  perms: payload.perms,
  status: payload.status === undefined ? undefined : String(payload.status),
  isFrame: '1'
})

export const getOrganizationTree = (params?: Record<string, any>) =>
  unwrap<OrganizationItem[]>(request.get('/organizations/tree', { params }))

export const getOrganizationPage = (params?: Record<string, any>) =>
  unwrap<PageResult<OrganizationItem>>(request.get('/organizations/page', { params }))

export const getOrganizationDetail = (id: number) =>
  unwrap<OrganizationItem>(request.get(`/organizations/${id}`))

export const createOrganization = async (payload: OrganizationPayload) => {
  const response = await request.post<AjaxResult<{ id: number }>>('/organizations', payload)
  return response.data
}

export const updateOrganization = (id: number, payload: OrganizationPayload) =>
  unwrap<null>(request.put(`/organizations/${id}`, payload))

export const deleteOrganization = (id: number) =>
  unwrap<null>(request.delete(`/organizations/${id}`))

export const getUserPage = (params?: Record<string, any>) =>
  unwrap<PageResult<UserItem>>(request.get('/users/page', { params }))

export const getUserDetail = (id: number) =>
  unwrap<UserItem>(request.get(`/users/${id}`))

export const createUser = async (payload: UserPayload) => {
  const response = await request.post<AjaxResult<{ id: number }>>('/users', payload)
  return response.data
}

export const updateUser = (id: number, payload: UserPayload) =>
  unwrap<null>(request.put(`/users/${id}`, payload))

export const deleteUser = (id: number) =>
  unwrap<null>(request.delete(`/users/${id}`))

export const batchDeleteUsers = (ids: number[]) =>
  unwrap<null>(request.delete('/users/batch', { data: { ids } }))

export const changeUserPassword = (id: number, payload: { oldPassword: string; newPassword: string }) =>
  unwrap<null>(request.put(`/users/${id}/password`, payload))

export const getRolePage = (params?: Record<string, any>) =>
  unwrap<PageResult<RoleItem>>(request.get('/roles/page', { params }))

export const getRoleDetail = (id: number) =>
  unwrap<RoleItem>(request.get(`/roles/${id}`))

export const createRole = async (payload: RolePayload) => {
  const response = await request.post<AjaxResult<{ id: number }>>('/roles', payload)
  return response.data
}

export const updateRole = (id: number, payload: RolePayload) =>
  unwrap<null>(request.put(`/roles/${id}`, payload))

export const deleteRole = (id: number) =>
  unwrap<null>(request.delete(`/roles/${id}`))

export const batchDeleteRoles = (ids: number[]) =>
  unwrap<null>(request.delete('/roles/batch', { data: { ids } }))

export const getRoleOptions = () =>
  unwrap<any[]>(request.get('/roles/optionselect')).then((list) =>
    list.map((item) => ({
      id: item.roleId,
      code: item.roleKey,
      name: item.roleName,
      status: item.status === undefined ? undefined : Number(item.status)
    }) as RoleItem)
  )

export const getRoleDeptTree = (id: number) =>
  request
    .get<(AjaxResult<null> & { checkedKeys?: number[]; depts?: TreeOption[] })>(`/roles/${id}/deptTree`)
    .then((response) => ({
      checkedKeys: response.checkedKeys || [],
      depts: response.depts || []
    }))

export const saveRoleDataScope = (id: number, payload: { dataScope: number; deptIds?: number[] }) =>
  unwrap<null>(request.put(`/roles/${id}/dataScope`, payload))

export const toggleRoleStatus = (id: number, payload: { status: string }) =>
  unwrap<null>(request.put(`/roles/${id}/status`, payload))

export const getMenuTree = () =>
  unwrap<MenuItem[]>(request.get('/menus/tree'))

export const getMenuDetail = (id: number) =>
  unwrap<MenuItem>(request.get(`/menus/${id}`))

export const createMenu = async (payload: MenuPayload) => {
  const response = await request.post<AjaxResult<{ id: number }>>('/menus', mapMenuPayload(payload))
  return response.data
}

export const updateMenu = (id: number, payload: MenuPayload) =>
  unwrap<null>(request.put(`/menus/${id}`, mapMenuPayload(payload)))

export const deleteMenu = (id: number) =>
  unwrap<null>(request.delete(`/menus/${id}`))

export interface PermissionCoverage {
  codePerms: string[]
  dbPerms: string[]
  missingInDb: string[]
}

export const getPermissionCoverage = () =>
  unwrap<PermissionCoverage>(request.get('/menus/permission-coverage'))

export const batchRegisterPermissions = (perms: string[]) =>
  request.post<AjaxResult<string>>('/menus/batch-register', perms)

// ===================== 日志清理配置 =====================

export interface LogCleanupConfig {
    enabled: boolean
    retentionDays: number
    cron: string
}

export const getLogCleanupConfig = () =>
    unwrap<LogCleanupConfig>(request.get('/logs/cleanup-config'))

export const updateLogCleanupConfig = (data: Partial<LogCleanupConfig>) =>
    unwrap<null>(request.put('/logs/cleanup-config', data))
