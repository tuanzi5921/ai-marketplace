<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Trophy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { overall, byTrack, byCompetition, type RankingItem } from '@/api/ranking'

const router = useRouter()

// 赛道列表（可由当前大赛返回，这里给默认值兜底）
const trackTabs = ref<{ label: string; value: string }[]>([
  { label: '总榜', value: '__overall__' },
  { label: '赛道榜', value: '__tracks__' },
  { label: '大赛榜', value: '__competition__' }
])

const activeTab = ref('__overall__')

const overallList = ref<RankingItem[]>([])
const trackList = ref<RankingItem[]>([])
const competitionData = ref<{
  competitionName?: string
  tracks?: { track: string; items: RankingItem[] }[]
} | null>(null)

// 赛道选择
const availableTracks = ref<string[]>([])
const currentTrack = ref('')
const loading = ref(false)

// 排名样式
function rankClass(rank?: number) {
  if (rank === 1) return 'rank-1'
  if (rank === 2) return 'rank-2'
  if (rank === 3) return 'rank-3'
  return ''
}

function goDetail(id: string | number) {
  router.push(`/submissions/${id}`)
}

// 加载总榜
async function loadOverall() {
  loading.value = true
  try {
    overallList.value = (await overall(10)) || []
  } catch {
    // request 拦截器已提示
  } finally {
    loading.value = false
  }
}

// 加载赛道榜
async function loadTrack(track: string) {
  loading.value = true
  try {
    trackList.value = (await byTrack(track, 10)) || []
  } catch {
    // request 拦截器已提示
  } finally {
    loading.value = false
  }
}

// 加载大赛聚合榜
async function loadCompetition() {
  loading.value = true
  try {
    const data = await byCompetition()
    competitionData.value = data || null
    availableTracks.value = data?.tracks?.map((t) => t.track) || []
    if (availableTracks.value.length && !currentTrack.value) {
      currentTrack.value = availableTracks.value[0]
    }
  } catch {
    // request 拦截器已提示
  } finally {
    loading.value = false
  }
}

function handleTabChange(tab: string) {
  if (tab === '__overall__') loadOverall()
  else if (tab === '__tracks__' && !availableTracks.value.length) loadCompetition()
  else if (tab === '__competition__') loadCompetition()
}

function handleTrackSelect(track: string) {
  currentTrack.value = track
  loadTrack(track)
}

onMounted(() => {
  loadOverall()
})
</script>

<template>
  <div class="page-container ranking">
    <div class="ranking__head">
      <h2>排行榜</h2>
      <p>看看哪些 AI 作品最受同事欢迎</p>
    </div>

    <el-tabs v-model="activeTab" class="ranking-tabs" @tab-change="handleTabChange">
      <!-- 总榜 -->
      <el-tab-pane label="总榜" name="__overall__">
        <div v-loading="loading" class="rank-list">
          <div
            v-for="item in overallList"
            :key="item.id"
            class="rank-item"
            @click="goDetail(item.id)"
          >
            <div class="rank-no" :class="rankClass(item.rank)">
              {{ item.rank || '—' }}
            </div>
            <div class="rank-cover">
              <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" />
              <div v-else class="rank-cover__placeholder">{{ item.title.charAt(0) }}</div>
            </div>
            <div class="rank-info">
              <div class="rank-info__title">{{ item.title }}</div>
              <div class="rank-info__author">{{ item.author?.displayName || '—' }}</div>
            </div>
            <div class="rank-score">
              <el-icon><Trophy /></el-icon>
              <span>{{ item.score != null ? Number(item.score).toFixed(2) : (item.avgScore ? Number(item.avgScore).toFixed(1) : '—') }}</span>
            </div>
          </div>
          <el-empty v-if="!loading && !overallList.length" description="暂无排行数据" />
        </div>
      </el-tab-pane>

      <!-- 各赛道 -->
      <el-tab-pane label="各赛道" name="__tracks__">
        <div class="track-select">
          <el-radio-group v-model="currentTrack" @change="handleTrackSelect">
            <el-radio-button v-for="t in availableTracks" :key="t" :value="t">
              {{ t }}
            </el-radio-button>
          </el-radio-group>
          <span v-if="!availableTracks.length" class="track-empty">
            暂无赛道，请稍后再试
          </span>
        </div>
        <div v-loading="loading" class="rank-list">
          <div
            v-for="item in trackList"
            :key="item.id"
            class="rank-item"
            @click="goDetail(item.id)"
          >
            <div class="rank-no" :class="rankClass(item.rank)">{{ item.rank || '—' }}</div>
            <div class="rank-cover">
              <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" />
              <div v-else class="rank-cover__placeholder">{{ item.title.charAt(0) }}</div>
            </div>
            <div class="rank-info">
              <div class="rank-info__title">{{ item.title }}</div>
              <div class="rank-info__author">{{ item.author?.displayName || '—' }}</div>
            </div>
            <div class="rank-score">
              <el-icon><Trophy /></el-icon>
              <span>{{ item.score != null ? Number(item.score).toFixed(2) : '—' }}</span>
            </div>
          </div>
          <el-empty v-if="!loading && !trackList.length" description="请选择赛道或暂无数据" />
        </div>
      </el-tab-pane>

      <!-- 大赛聚合榜 -->
      <el-tab-pane label="大赛榜" name="__competition__">
        <div v-loading="loading" class="competition">
          <template v-if="competitionData">
            <h3 v-if="competitionData.competitionName" class="comp-name">
              {{ competitionData.competitionName }}
            </h3>
            <div v-if="competitionData.tracks?.length" class="comp-tracks">
              <div v-for="t in competitionData.tracks" :key="t.track" class="comp-track">
                <div class="comp-track__title">{{ t.track }}</div>
                <div class="rank-list">
                  <div
                    v-for="item in t.items"
                    :key="item.id"
                    class="rank-item"
                    @click="goDetail(item.id)"
                  >
                    <div class="rank-no" :class="rankClass(item.rank)">{{ item.rank || '—' }}</div>
                    <div class="rank-info">
                      <div class="rank-info__title">{{ item.title }}</div>
                      <div class="rank-info__author">{{ item.author?.displayName || '—' }}</div>
                    </div>
                    <div class="rank-score">
                      <el-icon><Trophy /></el-icon>
                      <span>{{ item.score != null ? Number(item.score).toFixed(2) : '—' }}</span>
                    </div>
                  </div>
                  <el-empty v-if="!t.items?.length" description="该赛道暂无作品" />
                </div>
              </div>
            </div>
          </template>
          <el-empty v-else-if="!loading" description="暂无大赛排行" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.ranking__head {
  margin-bottom: 20px;
}
.ranking__head h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--brand-text);
}
.ranking__head p {
  margin: 0;
  color: var(--brand-muted);
  font-size: 14px;
}

.track-select {
  margin-bottom: 18px;
}
.track-empty {
  margin-left: 12px;
  color: var(--brand-muted);
  font-size: 13px;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 120px;
}
.rank-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 18px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid var(--brand-border);
  cursor: pointer;
  transition: all 0.18s ease;
}
.rank-item:hover {
  border-color: var(--brand-primary);
  box-shadow: 0 6px 18px rgba(59, 91, 219, 0.1);
}
.rank-no {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--el-color-primary-light-7);
  color: var(--brand-primary);
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.rank-1 {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  color: #fff;
}
.rank-2 {
  background: linear-gradient(135deg, #cbd5e1, #94a3b8);
  color: #fff;
}
.rank-3 {
  background: linear-gradient(135deg, #fb923c, #ea580c);
  color: #fff;
}
.rank-cover {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, #eaf0fe, #f0fdfa);
}
.rank-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.rank-cover__placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: #3b5bdb;
  opacity: 0.5;
}
.rank-info {
  flex: 1;
  min-width: 0;
}
.rank-info__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--brand-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.rank-info__author {
  font-size: 13px;
  color: var(--brand-muted);
  margin-top: 4px;
}
.rank-score {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #f59e0b;
  font-weight: 700;
  font-size: 16px;
}

.competition {
  min-height: 120px;
}
.comp-name {
  margin: 0 0 18px;
  color: var(--brand-primary);
  font-size: 18px;
}
.comp-tracks {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.comp-track__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--brand-text);
  margin-bottom: 12px;
  padding-left: 10px;
  border-left: 3px solid var(--brand-accent);
}
</style>
