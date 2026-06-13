/** Tag type helpers for log display — extracted from Log.vue */

export function getRequestMethodTag(method?: string): '' | 'success' | 'warning' | 'danger' | 'info' {
    const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
        GET: 'success', POST: 'warning', PUT: 'info', DELETE: 'danger',
    }
    return map[String(method || '').toUpperCase()] || 'info'
}

export function getBusinessTypeTag(type?: string): '' | 'success' | 'warning' | 'danger' | 'info' {
    const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
        INSERT: 'success', UPDATE: 'warning', DELETE: 'danger', EXPORT: 'info', OTHER: 'info',
    }
    return map[String(type || '').toUpperCase()] || 'info'
}

export function getAuthEventTag(type?: string): '' | 'success' | 'warning' | 'danger' | 'info' {
    const key = String(type || '').toUpperCase()
    if (key.includes('SUCCESS') || key === 'LOGOUT') return 'success'
    if (key.includes('UNAUTHORIZED') || key.includes('INVALID')) return 'warning'
    return 'danger'
}

export function getLevelTag(level?: string): '' | 'success' | 'warning' | 'danger' | 'info' {
    const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
        INFO: 'info', WARN: 'warning', ERROR: 'danger',
    }
    return map[String(level || '').toUpperCase()] || 'info'
}

export function getStreamEventTag(event: string): '' | 'success' | 'warning' | 'danger' | 'info' {
    const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
        ready: 'success', replay: 'warning', operation: 'info', auth: 'danger', runtime: 'warning',
    }
    return map[event] || 'info'
}

export function getLiveSubtypeTag(logType: string): '' | 'success' | 'warning' | 'danger' | 'info' {
    if (logType === 'OPERATION') return 'warning'
    if (logType === 'AUTH') return 'danger'
    if (logType === 'RUNTIME') return 'info'
    return ''
}
