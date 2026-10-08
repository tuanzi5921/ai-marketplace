<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Files, DocumentChecked, Download, User, Trophy } from '@element-plus/icons-vue'
import { stats, type DashboardStats } from '@/api/dashboard'
import { currentCompetition, type Competition, type CompetitionPhase } from '@/api/admin-competition'

const statsData = ref<DashboardStats>({
  totalSubmissions: 0,
  pendingReviews: 0,
  totalDownloads: 0,
  totalUsers: 0,
  activeCompetition: null
})
const activeComp = ref<Competition | null>(null)
const loading = ref(false)

const cards = computed(() => [
  { label: '总作品数', value: statsData.value.totalSubmissions, icon: Files, tone: 'indigo' },
  { label: '待审数', value: statsData.value.pendingReviews, icon: DocumentChecked, tone: 'amber' },
  { label: '累计下载', value: statsData.value.totalDownloads, icon: Download, tone: 'emerald' },
  { label: '活跃用户', value: statsData.value.totalUsers, icon: User, tone: 'sky' }
])

const phaseText: Record<CompetitionPhase, string> = {
  SUBMIT: '征集阶段',
  REVIEW: '评审阶段',
  AWARD: '颁奖阶段'
}

async function load() {
  loading.value = true
  try {
    const [s, c] = await Promise.allSettled([stats(), currentCompetition()])
    if (s.status === 'fulfilled') statsData.value = s.value
    if (c.status === 'fulfilled') activeComp.value = c.value
  } catch {
    ElMessage.error('看板数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <h2 class="page-title">数据看板</h2>
    <div class="stat-grid">
      <div v-for="c in cards" :key="c.label" class="stat-card" :class="`tone-${c.tone}`">
        <div class="stat-icon">
          <el-icon><component :is="c.icon" /></el-icon>
        </div>
        <div>
          <div class="stat-value">{{ c.value.toLocaleString() }}</div>
          <div class="stat-label">{{ c.label }}</div>
        </div>
      </div>
    </div>
    <div class="comp-card">
      <div class="comp-head">
        <div class="comp-title">
          <el-icon><Trophy /></el-icon>
          <span>当前大赛</span>
        </div>
      </div>
      <div v-if="activeComp" class="comp-body">
        <div class="comp-name">{{ activeComp.name }}</div>
        <div class="comp-tags">
          <el-tag type="success" effect="dark" round>已激活</el-tag>
          <el-tag v-if="activeComp.phase" type="warning" effect="plain" round>{{ phaseText[activeComp.phase] }}</el-tag>
        </div>
      </div>
      <div v-else class="comp-empty muted">暂无活跃大赛，前往「大赛配置」创建并激活。</div>
    </div>
  </div>
</template>
