<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Files, DocumentChecked, Download, User, Trophy } from '@element-plus/icons-vue'
import { stats, type DashboardStats } from '@/api/dashboard'
import { current, type Competition, type CompetitionPhase } from '@/api/competition'

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
    const [s, c] = await Promise.allSettled([stats(), current()])
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

    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div v-for="c in cards" :key="c.label" class="stat-card" :class="`tone-${c.tone}`">
        <div class="stat-icon">
          <el-icon><component :is="c.icon" /></el-icon>
        </div>
        <div class="stat-meta">
          <div class="stat-value">{{ c.value.toLocaleString() }}</div>
          <div class="stat-label">{{ c.label }}</div>
        </div>
      </div>
    </div>

    <!-- 当前大赛状态卡 -->
    <div class="comp-card">
      <div class="comp-head">
        <div class="comp-title">
          <el-icon class="comp-icon"><Trophy /></el-icon>
          <span>当前大赛</span>
        </div>
      </div>

      <div v-if="activeComp" class="comp-body">
        <div class="comp-name">{{ activeComp.name }}</div>
        <div class="comp-tags">
          <el-tag type="success" effect="dark" round>已激活</el-tag>
          <el-tag v-if="activeComp.phase" type="warning" effect="plain" round>
            {{ phaseText[activeComp.phase] }}
          </el-tag>
          <el-tag type="info" effect="plain" round>
            入围阈值 {{ activeComp.shortlistThreshold }}
          </el-tag>
          <el-tag type="info" effect="plain" round>
            每赛道 Top {{ activeComp.topNPerTrack }}
          </el-tag>
        </div>
        <div class="comp-dates">
          <span>征集：{{ activeComp.submitStartAt }} → {{ activeComp.submitEndAt }}</span>
          <span>评审：{{ activeComp.reviewAt }}</span>
        </div>
        <div v-if="activeComp.tracksConfig?.length" class="comp-tracks">
          <span class="muted">赛道：</span>
          <el-tag
            v-for="t in activeComp.tracksConfig"
            :key="t.key"
            size="small"
            effect="plain"
          >{{ t.name }}</el-tag>
        </div>
      </div>

      <div v-else class="comp-empty muted">
        暂无活跃大赛，前往「大赛配置」创建并激活。
      </div>
    </div>
  </div>
</template>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 22px;
}

.stat-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  padding: 20px 22px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.tone-indigo .stat-icon { background: #eef2ff; color: #4f46e5; }
.tone-amber .stat-icon { background: #fef3c7; color: #d97706; }
.tone-emerald .stat-icon { background: #d1fae5; color: #059669; }
.tone-sky .stat-icon { background: #e0f2fe; color: #0284c7; }

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.1;
}

.stat-label {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.comp-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.comp-head {
  padding: 16px 22px;
  border-bottom: 1px solid var(--border-color);
  background: #f8fafc;
}

.comp-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--text-primary);
}

.comp-icon {
  color: var(--brand-primary);
}

.comp-body {
  padding: 22px;
}

.comp-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 14px;
}

.comp-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.comp-dates {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  color: var(--text-regular);
  font-size: 13px;
  margin-bottom: 12px;
}

.comp-tracks {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.comp-empty {
  padding: 36px 22px;
  text-align: center;
  font-size: 14px;
}
</style>
