<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshLeft } from '@element-plus/icons-vue'
import { listRecommendations, approveRecommendation, rejectRecommendation, type Recommendation } from '@/api/judge'

const loading = ref(false)
const list = ref<Recommendation[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

async function load() {
  loading.value = true
  try {
    const res = await listRecommendations(page.value, size.value)
    list.value = res.list || []
    total.value = res.total || 0
  } catch { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

async function handleApprove(row: any) {
  try {
    await ElMessageBox.confirm('确认通过该评委推荐？', '审核确认', { type: 'success' })
    await approveRecommendation(row.id)
    ElMessage.success('已通过')
    load()
  } catch (e) { if (e !== 'cancel') { /* 拦截器已提示 */ } }
}

async function handleReject(row: any) {
  try {
    await ElMessageBox.confirm('确认拒绝该评委推荐？', '拒绝确认', { type: 'warning' })
    await rejectRecommendation(row.id)
    ElMessage.success('已拒绝')
    load()
  } catch (e) { if (e !== 'cancel') { /* 拦截器已提示 */ } }
}

function onPageChange(p: number) { page.value = p; load() }
onMounted(load)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <div class="toolbar">
      <h2 class="page-title">评委管理</h2>
      <div class="spacer" />
      <el-button :icon="RefreshLeft" @click="load">刷新</el-button>
    </div>
    <div class="card-block">
      <el-table :data="list" stripe style="width: 100%">
        <el-table-column prop="department" label="推荐赛道" width="160">
          <template #default="{ row }"><span>{{ row.department || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="reason" label="推荐理由" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'APPROVED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" type="success" size="small" @click="handleApprove(row)">通过</el-button>
            <el-button v-if="row.status === 'PENDING'" type="danger" size="small" @click="handleReject(row)">拒绝</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无评委推荐" /></template>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next, jumper" :total="total" :current-page="page" :page-size="size" @current-change="onPageChange" />
      </div>
    </div>
  </div>
</template>
