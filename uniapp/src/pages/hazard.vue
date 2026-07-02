<!-- src/pages/hazard.vue -->
<script setup lang="ts">
import type { Hazard } from '@/utils/hazard'
import PageHeader from '@/components/PageHeader.vue'
import { computed, nextTick, onMounted, ref } from 'vue'
import { hazardApi } from '@/utils/hazard'

const loading = ref(true)
const isRefreshing = ref(false)
const hazards = ref<Hazard[]>([])
const keyword = ref('')

// 状态定义
const STATUS_LIST = [
  { label: '监测中', value: '监测中' },
  { label: '停测中', value: '停测中' },
  { label: '已完结', value: '已完结' },
] as const
const activeStatus = ref('监测中')

// 分组筛选
const activeGroup = ref('')
const showGroupDropdown = ref(false)
const groupOptions = computed(() => {
  const groups = new Set<string>()
  for (const h of hazards.value) {
    if (h.groupName) groups.add(h.groupName)
  }
  return [...groups]
})
const activeGroupLabel = computed(() => activeGroup.value || '全部分组')

// 各状态数量统计
const statusCounts = computed(() => {
  const counts: Record<string, number> = {}
  for (const s of STATUS_LIST) counts[s.value] = 0
  for (const h of hazards.value) {
    if (counts[h.status] !== undefined) counts[h.status]++
  }
  return counts
})

// 回到顶部
const showBackTop = ref(false)
const scrollTop = ref(0)

const filteredHazards = computed(() => {
  let list = hazards.value
  if (activeStatus.value) {
    list = list.filter(h => h.status === activeStatus.value)
  }
  if (activeGroup.value) {
    list = list.filter(h => h.groupName === activeGroup.value)
  }
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(h =>
      h.name.toLowerCase().includes(kw)
      || (h.code && h.code.toLowerCase().includes(kw)),
    )
  }
  return list
})

async function loadData() {
  try {
    hazards.value = await hazardApi.getAll()
  }
  catch (error) {
    console.error('加载隐患点数据失败:', error)
  }
  finally {
    loading.value = false
    isRefreshing.value = false
  }
}

function onRefresh() {
  isRefreshing.value = true
  loadData()
}

function goToDetail(hazard: Hazard) {
  uni.navigateTo({ url: `/pages/hazard-detail?id=${hazard.id}` })
}

function onSearch(e: any) {
  keyword.value = e.detail.value || ''
}

function clearSearch() {
  keyword.value = ''
}

function selectStatus(status: string) {
  activeStatus.value = status
}

function selectGroup(group: string) {
  activeGroup.value = group
  showGroupDropdown.value = false
}

function toggleGroupDropdown() {
  showGroupDropdown.value = !showGroupDropdown.value
}

function getStatusClass(status: string): string {
  switch (status) {
    case '监测中': return 'monitoring'
    case '停测中': return 'paused'
    case '已完结': return 'resolved'
    default: return 'resolved'
  }
}

function onScroll(e: { detail: { scrollTop: number } }) {
  showBackTop.value = e.detail.scrollTop > 400
}

function scrollToTop() {
  scrollTop.value = 1
  nextTick(() => { scrollTop.value = 0 })
  showBackTop.value = false
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <view class="page-container">
    <!-- 渐变头部 -->
    <PageHeader title="隐患点" subtitle="风险管控 · 安全监测" />

    <!-- 隐患点列表 -->
    <view class="page-body">
      <!-- 筛选胶囊卡片（固定在顶部） -->
      <view class="filter-capsule-card">
        <!-- 状态胶囊 -->
        <view class="status-capsule-row">
          <view
            v-for="s in STATUS_LIST"
            :key="s.value"
            class="capsule-item"
            :class="{ active: activeStatus === s.value }"
            @click="selectStatus(s.value)"
          >
            <text class="capsule-text">{{ s.label }}</text>
            <text v-if="statusCounts[s.value] > 0" class="capsule-count">{{ statusCounts[s.value] }}</text>
          </view>
        </view>
        <!-- 分组 + 搜索框 -->
        <view class="filter-search-row">
          <view class="group-filter-btn" @click="toggleGroupDropdown">
            <text class="group-filter-text">{{ activeGroupLabel }}</text>
            <text class="group-filter-arrow" :class="{ open: showGroupDropdown }">&#x25BC;</text>
          </view>
          <view class="filter-search-bar">
            <view class="search-icon-wrapper">
              <zui-svg-icon icon="search" :width="16" color="#9ca3af" />
            </view>
            <input
              class="search-input"
              type="text"
              placeholder="搜索隐患点名称 / 编号"
              placeholder-class="search-placeholder"
              :value="keyword"
              confirm-type="search"
              @input="onSearch"
            >
            <view v-if="keyword" class="search-clear" @click="clearSearch">
              <text class="clear-icon">x</text>
            </view>
          </view>
        </view>
        <!-- 分组下拉列表 -->
        <view v-if="showGroupDropdown" class="group-dropdown">
          <view
            class="group-option"
            :class="{ active: activeGroup === '' }"
            @click="selectGroup('')"
          >
            全部分组
          </view>
          <view
            v-for="g in groupOptions"
            :key="g"
            class="group-option"
            :class="{ active: activeGroup === g }"
            @click="selectGroup(g)"
          >
            {{ g }}
          </view>
        </view>
      </view>

      <scroll-view
        class="list-scroll"
        scroll-y
        :scroll-top="scrollTop"
        refresher-enabled
        :refresher-triggered="isRefreshing"
        @refresherrefresh="onRefresh"
        @scroll="onScroll"
      >
      <view class="list-inner">
        <!-- 骨架屏 -->
        <view v-if="loading" class="skeleton-wrapper">
          <Skeleton :rows="4" />
          <Skeleton :rows="4" />
        </view>

        <!-- 列表内容 -->
        <view v-else>
          <DataCard
            v-for="item in filteredHazards"
            :key="item.id"
            :title="item.name"
            :subtitle="item.code"
            @click="goToDetail(item)"
          >
            <template #tag>
              <view class="status-tag" :class="getStatusClass(item.status)">
                {{ item.status }}
              </view>
            </template>

            <text v-if="item.description" class="card-desc-text">{{ item.description }}</text>
            <text v-else class="card-desc-text placeholder">暂无描述</text>

            <template #footer>
              <text class="card-device-count">📡 {{ item.deviceCount }}台设备</text>
              <text class="card-time">{{ item.createTime }}</text>
            </template>
          </DataCard>

          <EmptyState
            v-if="filteredHazards.length === 0"
            :use-image="true"
            title="暂无隐患点"
            :description="keyword || activeStatus ? '未找到匹配的隐患点' : '当前没有任何隐患点数据'"
          />
        </view>
      </view>
      </scroll-view>
    </view>

    <!-- 回到顶部 -->
    <view v-if="showBackTop" class="back-top-btn" @click="scrollToTop">
      <zui-svg-icon icon="up" :width="20" color="#ffffff" />
    </view>
  </view>
</template>

<style lang="scss" scoped>
.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #eef1f8 0%, #e8ecf4 100%);
}

.header {
  position: relative;
  flex-shrink: 0;
}

/* 页面主体 */
.page-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 筛选胶囊卡片 */
.filter-capsule-card {
  flex-shrink: 0;
  margin: 0 32rpx;
  padding: 16rpx;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(102, 126, 234, 0.1);
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.status-capsule-row {
  display: flex;
  gap: 16rpx;
  background: #f7f8fc;
  border-radius: 20rpx;
  padding: 6rpx;
}

.capsule-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16rpx 0;
  border-radius: 16rpx;
  transition: all 0.2s;

  &.active {
    background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
    box-shadow: 0 4rpx 16rpx rgba(48, 104, 228, 0.3);

    .capsule-text {
      color: #ffffff;
      font-weight: 600;
    }
  }
}

.capsule-text {
  font-size: 26rpx;
  color: #6b7280;
}

.capsule-count {
  font-size: 20rpx;
  background: rgba(107, 114, 128, 0.12);
  color: #6b7280;
  padding: 2rpx 10rpx;
  border-radius: 14rpx;
  min-width: 28rpx;
  text-align: center;
  margin-left: 6rpx;

  .capsule-item.active & {
    background: rgba(255, 255, 255, 0.3);
    color: #ffffff;
  }
}

/* 分组 + 搜索行 */
.filter-search-row {
  display: flex;
  gap: 12rpx;
  align-items: center;
}

.group-filter-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  height: 72rpx;
  padding: 0 20rpx;
  background: #f0f5ff;
  border-radius: 36rpx;
  flex-shrink: 0;
  border: 1rpx solid #d6e4ff;
}

.group-filter-text {
  font-size: 24rpx;
  color: #3068e4;
  max-width: 160rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-filter-arrow {
  font-size: 16rpx;
  color: #3068e4;
  transition: transform 0.2s;

  &.open { transform: rotate(180deg); }
}

.filter-search-bar {
  flex: 1;
  display: flex;
  align-items: center;
  height: 72rpx;
  background: #f7f8fc;
  border-radius: 36rpx;
  padding: 0 24rpx;
}

/* 分组下拉列表 */
.group-dropdown {
  background: #f7f8fc;
  border-radius: 16rpx;
  padding: 8rpx 0;
  border: 1rpx solid #e5e7eb;
}

.group-option {
  padding: 20rpx 24rpx;
  font-size: 26rpx;
  color: #4b5563;

  &.active {
    color: #3068e4;
    font-weight: 600;
  }
}

.search-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  height: 72rpx;
  font-size: 26rpx;
  color: #1a1a2e;
}

.search-placeholder {
  color: #9ca3af;
  font-size: 26rpx;
}

.search-clear {
  width: 40rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e5e7eb;
  border-radius: 50%;
}

.clear-icon {
  font-size: 22rpx;
  color: #6b7280;
}

/* 列表滚动区 */
.list-scroll {
  flex: 1;
  height: 0;
}

.list-inner {
  padding: 20rpx 32rpx;
}

.skeleton-wrapper {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 20rpx rgba(102, 126, 234, 0.1);
}

.status-tag {
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  flex-shrink: 0;

  &.monitoring {
    background: rgba(82, 196, 26, 0.1);
    color: #52c41a;
  }

  &.paused {
    background: rgba(250, 140, 22, 0.1);
    color: #fa8c16;
  }

  &.resolved {
    background: rgba(156, 163, 175, 0.1);
    color: #9ca3af;
  }
}

.card-desc-text {
  font-size: 26rpx;
  color: #6b7280;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;

  &.placeholder {
    color: #d1d5db;
    font-style: italic;
  }
}

.card-device-count {
  font-size: 24rpx;
  color: #4b5563;
}

.card-time {
  font-size: 22rpx;
  color: #9ca3af;
  flex-shrink: 0;
}

/* 回到顶部 */
.back-top-btn {
  position: fixed;
  right: 32rpx;
  bottom: 200rpx;
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #3068e4 0%, #1e5acc 100%);
  box-shadow: 0 8rpx 24rpx rgba(48, 104, 228, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;

  &:active {
    transform: scale(0.9);
    opacity: 0.85;
  }
}
</style>
