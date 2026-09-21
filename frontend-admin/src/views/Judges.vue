<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshLeft, Check, Close } from '@element-plus/icons-vue'
import {
  listRecommendations,
  approve,
  reject,
  type Recommendation,
  type RecommendationStatus
} from '@/api/judge'

const loading = ref(false)
const list = ref<Recommendation[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const statusTagType: Record<RecommendationStatus, '' | 'success' | 'info' | 'warning' | 'danger'> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger'
}

const statusText: Record<RecommendationStatus, string> = {
  PENDING: '待处理',
  APPROVED: '已入库',
  REJECTED: '已拒绝'
}

async function load() {
  loading.value = true
  try {
    const res = await listRecommendations(page.value, size.value)
    list.value = res.list || []
    total.value = res.total || 0
  } catch {
    /* 错误已提示 */
  } finally {
    loading.value = false
  }
}

async function handleApprove(row: Recommendation) {
  try {
    await ElMessageBox.confirm(
      `确认将「${row.recommendeeName}」备案入库为评委？`,
      '备案入库',
      { type: 'success', confirmButtonText: '入库', cancelButtonText: '取消' }
    )
    await approve(row.id)
    ElMessage.success('已备案入库')
    load()
  } catch (e) {
    if (e !== 'cancel') {
      /* 其他错误已提示 */
    }
  }
}

async function handleReject(row: Recommendation) {
  try {
    await ElMessageBox.confirm(
      `确认拒绝「${row.recommendeeName}」的评委推荐？`,
      '拒绝推荐',
      { type: 'error', confirmButtonText: '拒绝', cancelButtonText: '取消' }
    )
    await reject(row.id)
    ElMessage.success('已拒绝')
    load()
  } catch (e) {
    if (e !== 'cancel') {
      /* 其他错误已提示 */
    }
  }
}

function onPageChange(p: number) {
  page.value = p
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <div class="toolbar">
      <h2 class="page-title" style="margin: 0">评委推荐管理</h2>
      <div class="spacer" />
      <el-button :icon="RefreshLeft" @click="load">刷新</el-button>
    </div>

    <div class="card-block">
      <el-table :data="list" stripe style="width: 100%">
        <el-table-column prop="recommendeeName" label="被推荐人" width="150" />
        <el-table-column prop="recommendDepartment" label="推荐部门" min-width="180" show-overflow-tooltip />
        <el-table-column prop="recommenderName" label="推荐人" width="150" />
        <el-table-column prop="reason" label="推荐理由" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.reason || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag
              :type="statusTagType[row.status as RecommendationStatus]"
              effect="light"
              size="small"
            >
              {{ statusText[row.status as RecommendationStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="推荐时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              :icon="Check"
              :disabled="(row as Recommendation).status !== 'PENDING'"
              @click="handleApprove(row as Recommendation)"
            >
              备案入库
            </el-button>
            <el-button
              size="small"
              type="danger"
              :icon="Close"
              :disabled="(row as Recommendation).status !== 'PENDING'"
              @click="handleReject(row as Recommendation)"
            >
              拒绝
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无评委推荐" />
        </template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next, jumper"
          :total="total"
          :current-page="page"
          :page-size="size"
          @current-change="onPageChange"
        />
      </div>
    </div>
  </div>
</template>
