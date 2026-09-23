<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Trophy, Medal, Files, Edit } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { me as fetchMe } from '@/api/auth'
import { listPublished, type SubmissionSummary } from '@/api/submission'
import { request } from '@/api/request'

const router = useRouter()
const auth = useAuthStore()

const typeLabel: Record<string, string> = {
  CODE: '代码作品',
  EXECUTABLE: '可执行程序',
  SAAS: 'SaaS 服务',
  DOC: '文档方案'
}

// 用户档案
const profile = ref(auth.user)

// 我的提交列表
const mySubmissions = ref<SubmissionSummary[]>([])
const loading = ref(false)

async function loadProfile() {
  try {
    const user = await fetchMe()
    auth.setUser(user)
    profile.value = user
  } catch {
    // request 拦截器已提示
  }
}

// 我的提交（后端约定：GET /api/submissions/mine）
async function loadMySubmissions() {
  loading.value = true
  try {
    const res = await request.get<{ records: SubmissionSummary[] }>('/submissions/mine')
    mySubmissions.value = res?.records || []
  } catch {
    // 兜底：若后端暂无 /mine 接口，则用已发布列表按作者过滤
    try {
      const all = await listPublished({ page: 1, size: 100 })
      const name = profile.value?.username
      mySubmissions.value = (all.records || []).filter(
        (s) => s.author?.username === name || s.author?.id === (profile.value?.id as unknown)
      )
    } catch {
      // 静默
    }
  } finally {
    loading.value = false
  }
}

function goDetail(id: string | number) {
  router.push(`/submissions/${id}`)
}

function goSubmit() {
  router.push('/submit')
}

onMounted(() => {
  if (!auth.user) loadProfile()
  loadMySubmissions()
})

// 头像文字
const avatarText = (name?: string) => (name ? name.charAt(0).toUpperCase() : 'U')
</script>

<template>
  <div class="page-container me">
    <!-- 用户信息卡 -->
    <el-card class="profile-card" shadow="never">
      <div class="profile">
        <el-avatar :size="72" class="profile__avatar">
          {{ avatarText(profile?.displayName || profile?.username) }}
        </el-avatar>
        <div class="profile__main">
          <div class="profile__name">{{ profile?.displayName || profile?.username || '—' }}</div>
          <div class="profile__sub">
            <span v-if="profile?.department">{{ profile.department }}</span>
            <span v-if="profile?.roles?.length"> · {{ profile.roles.join(' / ') }}</span>
          </div>
        </div>
        <div class="profile__stats">
          <div class="stat">
            <el-icon class="stat__icon"><Trophy /></el-icon>
            <div>
              <div class="stat__num">{{ profile?.points ?? 0 }}</div>
              <div class="stat__label">积分</div>
            </div>
          </div>
          <div class="stat">
            <el-icon class="stat__icon"><Files /></el-icon>
            <div>
              <div class="stat__num">{{ mySubmissions.length }}</div>
              <div class="stat__label">我的作品</div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 我的提交列表 -->
    <el-card class="list-card" shadow="never">
      <template #header>
        <div class="list-head">
          <span><el-icon><Medal /></el-icon> 我的提交</span>
          <el-button type="primary" size="small" :icon="Edit" @click="goSubmit">
            上传新作品
          </el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="mySubmissions"
        style="width: 100%"
        empty-text="还没有提交过作品，去上传第一个吧"
      >
        <el-table-column label="作品" min-width="220">
          <template #default="{ row }">
            <div class="cell-title" @click="goDetail(row.id)">{{ row.title }}</div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ typeLabel[row.type] || row.type }}</template>
        </el-table-column>
        <el-table-column label="赛道" width="120">
          <template #default="{ row }">{{ row.track || '—' }}</template>
        </el-table-column>
        <el-table-column label="版本" width="100">
          <template #default="{ row }">{{ row.version || '—' }}</template>
        </el-table-column>
        <el-table-column label="下载量" width="100" align="center">
          <template #default="{ row }">{{ row.downloadCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="评分" width="120" align="center">
          <template #default="{ row }">
            {{ row.avgScore ? Number(row.avgScore).toFixed(1) : '—' }}
            <span class="cell-sub">({{ row.raterCount || 0 }})</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button text type="primary" @click="goDetail(row.id)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.me {
  padding-top: 20px;
}

.profile-card {
  margin-bottom: 20px;
  background: linear-gradient(120deg, #1e3a8a 0%, #3b5bdb 70%, #4c6ef5 100%);
}
.profile-card :deep(.el-card__body) {
  padding: 28px 32px;
}
.profile {
  display: flex;
  align-items: center;
  gap: 22px;
  color: #fff;
}
.profile__avatar {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  flex-shrink: 0;
}
.profile__main {
  flex: 1;
}
.profile__name {
  font-size: 22px;
  font-weight: 600;
}
.profile__sub {
  margin-top: 6px;
  font-size: 14px;
  opacity: 0.88;
}
.profile__stats {
  display: flex;
  gap: 32px;
}
.stat {
  display: flex;
  align-items: center;
  gap: 10px;
}
.stat__icon {
  font-size: 26px;
  opacity: 0.9;
}
.stat__num {
  font-size: 22px;
  font-weight: 700;
}
.stat__label {
  font-size: 12px;
  opacity: 0.85;
  margin-top: 2px;
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.list-head span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.cell-title {
  color: var(--brand-primary);
  cursor: pointer;
  font-weight: 500;
}
.cell-title:hover {
  text-decoration: underline;
}
.cell-sub {
  color: var(--brand-muted);
  font-size: 12px;
}
</style>
