<script setup lang="ts">
import { onMounted, reactive, ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Download, Star, Timer } from '@element-plus/icons-vue'
import {
  listPublished,
  type SubmissionSummary,
  type SubmissionType
} from '@/api/submission'
import { getCurrent, type Competition } from '@/api/competition'

const router = useRouter()

// 作品类型选项
const typeOptions: { label: string; value: SubmissionType | '' }[] = [
  { label: '全部', value: '' },
  { label: '代码作品', value: 'CODE' },
  { label: '可执行程序', value: 'EXECUTABLE' },
  { label: 'SaaS 服务', value: 'SAAS' },
  { label: '文档方案', value: 'DOC' }
]

const typeTagMap: Record<string, string> = {
  CODE: 'primary',
  EXECUTABLE: 'success',
  SAAS: 'warning',
  DOC: 'info'
}
function typeTagType(t?: string) {
  return typeTagMap[t || ''] || 'info'
}
function typeLabel(t?: string) {
  return typeOptions.find((o) => o.value === t)?.label || t
}

// 查询条件
const filters = reactive<{ type: SubmissionType | ''; domain: string }>({
  type: '',
  domain: ''
})

const domainOptions = [
  '通用办公',
  '研发效能',
  '市场营销',
  '客户服务',
  '数据分析',
  '人力资源',
  '财务管理'
]

// 列表数据
const list = ref<SubmissionSummary[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 12, total: 0 })

// 大赛倒计时
const competition = ref<Competition | null>(null)
const countdown = ref('')
let timer: number | undefined

function computeCountdown(end?: string) {
  if (!end) return ''
  const diff = new Date(end).getTime() - Date.now()
  if (diff <= 0) return '已结束'
  const d = Math.floor(diff / 86400000)
  const h = Math.floor((diff % 86400000) / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  return `${d}天 ${h}时 ${m}分 ${s}秒`
}
function startTimer() {
  if (timer) window.clearInterval(timer)
  timer = window.setInterval(() => {
    countdown.value = computeCountdown(competition.value?.endTime)
  }, 1000)
}

async function loadCompetition() {
  try {
    const data = await getCurrent()
    competition.value = data || null
    if (competition.value?.endTime) {
      countdown.value = computeCountdown(competition.value.endTime)
      startTimer()
    }
  } catch {
    // 当前无大赛属正常情况
  }
}

// 加载作品列表
async function loadList() {
  loading.value = true
  try {
    const res = await listPublished({
      page: page.current,
      size: page.size,
      type: filters.type,
      domain: filters.domain || undefined
    })
    list.value = res.records || []
    page.total = res.total || 0
  } catch {
    // request 拦截器已提示
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.current = 1
  loadList()
}

function handleReset() {
  filters.type = ''
  filters.domain = ''
  page.current = 1
  loadList()
}

function handlePageChange(p: number) {
  page.current = p
  loadList()
}

function goDetail(id: string | number) {
  router.push(`/submissions/${id}`)
}

const isEmpty = computed(() => !loading.value && list.value.length === 0)

onMounted(() => {
  loadCompetition()
  loadList()
})
onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <div class="page-container home">
    <!-- 当前大赛倒计时 -->
    <el-card v-if="competition" class="banner-card" shadow="never">
      <div class="banner">
        <div class="banner__left">
          <el-icon class="banner__icon"><Timer /></el-icon>
          <div>
            <div class="banner__title">{{ competition.name || 'AI 创作大赛' }}</div>
            <div class="banner__desc">赛道：{{ competition.tracks?.join('、') || '—' }}</div>
          </div>
        </div>
        <div class="banner__right">
          <div class="banner__label">距结束</div>
          <div class="banner__countdown">{{ countdown }}</div>
        </div>
      </div>
    </el-card>

    <!-- 筛选区 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-bar">
        <el-radio-group v-model="filters.type" @change="handleSearch">
          <el-radio-button
            v-for="o in typeOptions"
            :key="o.value || 'all'"
            :value="o.value"
          >
            {{ o.label }}
          </el-radio-button>
        </el-radio-group>

        <el-select
          v-model="filters.domain"
          placeholder="业务领域"
          clearable
          style="width: 180px"
          @change="handleSearch"
        >
          <el-option v-for="d in domainOptions" :key="d" :label="d" :value="d" />
        </el-select>

        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <!-- 作品卡片网格 -->
    <div v-loading="loading" class="grid">
      <el-card
        v-for="item in list"
        :key="item.id"
        class="submission-card"
        shadow="hover"
        @click="goDetail(item.id)"
      >
        <div class="cover">
          <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" />
          <div v-else class="cover__placeholder">{{ item.title.charAt(0) }}</div>
          <el-tag class="cover__type" :type="typeTagType(item.type)" effect="dark" size="small">
            {{ typeLabel(item.type) }}
          </el-tag>
        </div>
        <div class="info">
          <div class="info__title" :title="item.title">{{ item.title }}</div>
          <div class="info__desc">{{ item.shortDesc || '暂无简介' }}</div>
          <div class="info__tags">
            <el-tag
              v-for="t in (item.techStack || []).slice(0, 3)"
              :key="t"
              size="small"
              effect="plain"
            >
              {{ t }}
            </el-tag>
          </div>
          <div class="info__meta">
            <span class="meta-item">
              <el-icon><Download /></el-icon>
              {{ item.downloadCount || 0 }}
            </span>
            <span class="meta-item">
              <el-icon><Star /></el-icon>
              {{ item.avgScore ? Number(item.avgScore).toFixed(1) : '—' }}
              <span class="meta-sub">({{ item.raterCount || 0 }}人)</span>
            </span>
            <span class="meta-author">{{ item.author?.displayName || '—' }}</span>
          </div>
        </div>
      </el-card>

      <el-empty v-if="isEmpty" description="暂无作品，快来上传第一个吧" />
    </div>

    <!-- 分页 -->
    <div v-if="page.total > 0" class="pager">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :current-page="page.current"
        :page-size="page.size"
        :total="page.total"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.banner-card {
  margin-bottom: 16px;
  border: none;
  background: linear-gradient(120deg, #1e3a8a 0%, #3b5bdb 70%, #4c6ef5 100%);
}
.banner-card :deep(.el-card__body) {
  padding: 20px 24px;
}
.banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
}
.banner__left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.banner__icon {
  font-size: 28px;
}
.banner__title {
  font-size: 18px;
  font-weight: 600;
}
.banner__desc {
  font-size: 13px;
  opacity: 0.85;
  margin-top: 4px;
}
.banner__label {
  font-size: 12px;
  opacity: 0.85;
}
.banner__countdown {
  font-size: 22px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
  margin-top: 2px;
}

.filter-card {
  margin-bottom: 20px;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
  min-height: 200px;
}
.submission-card {
  cursor: pointer;
  overflow: hidden;
}
.submission-card :deep(.el-card__body) {
  padding: 0;
}
.cover {
  position: relative;
  height: 160px;
  background: linear-gradient(135deg, #eaf0fe, #f0fdfa);
  overflow: hidden;
}
.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover__placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56px;
  font-weight: 700;
  color: #3b5bdb;
  opacity: 0.5;
}
.cover__type {
  position: absolute;
  top: 10px;
  right: 10px;
}
.info {
  padding: 14px 16px 16px;
}
.info__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--brand-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.info__desc {
  margin-top: 6px;
  font-size: 13px;
  color: var(--brand-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 38px;
}
.info__tags {
  margin-top: 10px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  min-height: 24px;
}
.info__meta {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 13px;
  color: var(--brand-muted);
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.meta-sub {
  font-size: 12px;
  opacity: 0.8;
}
.meta-author {
  margin-left: auto;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pager {
  margin-top: 28px;
  display: flex;
  justify-content: center;
}
</style>
