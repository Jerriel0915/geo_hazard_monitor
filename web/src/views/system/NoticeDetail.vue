<template>
  <div class="notice-detail-page">
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
      <h2 class="page-title">公告详情</h2>
    </div>
    <el-card v-loading="loading" class="notice-card">
      <template v-if="detail.noticeId">
        <h1 class="notice-title">{{ detail.noticeTitle }}</h1>
        <div class="notice-meta">
          <el-tag :type="detail.noticeType === '1' ? 'warning' : 'success'" size="small">
            {{ detail.noticeType === '1' ? '通知' : '公告' }}
          </el-tag>
          <span class="meta-item">发布人：{{ detail.createBy || '-' }}</span>
          <span class="meta-item">发布时间：{{ detail.createTime || '-' }}</span>
        </div>
        <el-divider />
        <div class="notice-content" v-html="sanitizedContent" />
      </template>
      <el-empty v-else-if="!loading" description="公告不存在或已被删除" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNoticeById, markRead, type SysNotice } from '@/api/notice'

const route = useRoute()
const router = useRouter()
const detail = ref<Partial<SysNotice>>({})
const loading = ref(false)

/** 简单 XSS 缓解：移除 <script>/<iframe>/<object>/<embed> 标签。
 *  公告仅 system:notice:add 权限的管理员可发布，信任端输入；
 *  此处做一层兜底过滤，避免意外粘贴恶意脚本。 */
const sanitizedContent = computed(() => {
  const html = detail.value.noticeContent ?? ''
  return html
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    .replace(/<(iframe|object|embed)\b[^>]*>.*?<\/\1>/gis, '')
})

async function loadDetail() {
  const id = Number(route.params.id)
  if (Number.isNaN(id) || id <= 0) {
    ElMessage.error('公告 ID 无效')
    return
  }
  loading.value = true
  try {
    const res = await getNoticeById(id)
    detail.value = res.data ?? {}
    // 异步标记已读，不阻塞渲染（失败静默，下次进入会再次尝试）
    if (res.data?.noticeId) {
      markRead(id).catch(() => { /* ignore */ })
    }
  } catch {
    ElMessage.error('加载公告详情失败')
  } finally {
    loading.value = false
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/system/notice')
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.notice-detail-page {
  padding: 16px 24px;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}
.notice-card {
  max-width: 900px;
}
.notice-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 12px;
}
.notice-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: #909399;
}
.meta-item {
  font-size: 13px;
}
.notice-content {
  line-height: 1.8;
  font-size: 14px;
  color: #303133;
  word-break: break-word;
}
.notice-content :deep(img) {
  max-width: 100%;
}
</style>
