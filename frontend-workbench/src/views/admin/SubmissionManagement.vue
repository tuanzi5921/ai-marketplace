<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshLeft, Search, Delete } from '@element-plus/icons-vue'
import { listAllSubmissions, offlineSubmission, deleteSubmission, type AdminSubmission } from '@/api/admin-submission'

const loading = ref(false)
const list = ref<AdminSubmission[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const filters = reactive<{ status: string; type: string }>({ status: '', type: '' })

const statusOptions = [
  { label: '待审核', value: 'PENDING' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已退回', value: 'REJECTED' },
  { label: '已下架', value: 'UNLISTED' }
]

const typeOptions = [
  { label: '源码', value: 'SOURCE' },
  { label: '可执行包', value: 'EXECUTABLE' },
  { label: 'SaaS', value: 'SAAS' },
  { label: '文档', value: 'DOCUMENT' }
]

const statusTagType: Record<string, 'primary' | 'success' | 'info' | 'warning' | 'danger'> = {
  PENDING: 'warning', PUBLISHED: 'success', REJECTED: 'danger', UNLISTED: 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await listAllSubmissions(page.value, size.value, {
      status: filters.status || undefined, type: filters.type || undefined
    })
    list.value = res.list || []
    total.value = res.total || 0
  } catch { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

function onFilter() { page.value = 1; load() }
function onReset() { filters.status = ''; filters.type = ''; page.value = 1; load() }

async function handleOffline(row: any) {
  try {
    await ElMessageBox.confirm(`确认下架作品「${row.title}」？`, '下架确认', { type: 'warning', confirmButtonText: '下架', cancelButtonText: '取消' })
    await offlineSubmission(row.id)
    ElMessage.success('已下架')
    load()
  } catch (e) { if (e !== 'cancel') { /* 拦截器已提示 */ } }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除作品「${row.title}」？该操作不可恢复。`, '删除确认', { type: 'error', confirmButtonText: '删除', cancelButtonText: '取消' })
    await deleteSubmission(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { if (e !== 'cancel') { /* 拦截器已提示 */ } }
}

function onPageChange(p: number) { page.value = p; load() }

onMounted(load)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <div class="toolbar">
      <h2 class="page-title">作品管理</h2>
      <div class="spacer" />
      <el-button :icon="RefreshLeft" @click="load">刷新</el-button>
    </div>
    <div class="card-block">
      <div class="filter-bar">
        <el-select v-model="filters.status" placeholder="按状态筛选" clearable style="width: 150px" @change="onFilter">
          <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="filters.type" placeholder="按类型筛选" clearable style="width: 150px" @change="onFilter">
          <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-button :icon="Search" type="primary" @click="onFilter">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </div>
      <el-table :data="list" stripe style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status]" effect="light" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="downloadCount" label="下载量" width="100" />
        <el-table-column prop="ratingAvg" label="评分" width="100" />
        <el-table-column prop="createdAt" label="提交时间" width="170" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button type="warning" size="small" :disabled="row.status !== 'PUBLISHED'" @click="handleOffline(row)">下架</el-button>
            <el-button type="danger" size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无作品" /></template>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next, jumper" :total="total" :current-page="page" :page-size="size" @current-change="onPageChange" />
      </div>
    </div>
  </div>
</template>
