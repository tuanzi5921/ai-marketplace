<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshLeft, Search, Delete, Download } from '@element-plus/icons-vue'
import {
  listAll,
  offline,
  deleteSubmission,
  type Submission,
  type SubmissionStatus,
  type SubmissionType
} from '@/api/submission'

const loading = ref(false)
const list = ref<Submission[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

// 过滤条件
const filters = reactive<{ status: SubmissionStatus | ''; type: SubmissionType | '' }>({
  status: '',
  type: ''
})

const statusOptions: { label: string; value: SubmissionStatus }[] = [
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已退回', value: 'REJECTED' },
  { label: '已上架', value: 'ONLINE' },
  { label: '已下架', value: 'OFFLINE' }
]

const typeOptions: { label: string; value: SubmissionType }[] = [
  { label: '应用', value: 'APP' },
  { label: '智能体', value: 'AGENT' },
  { label: '插件', value: 'PLUGIN' },
  { label: '解决方案', value: 'SOLUTION' }
]

const statusTagType: Record<SubmissionStatus, '' | 'success' | 'info' | 'warning' | 'danger'> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  ONLINE: 'success',
  OFFLINE: 'info'
}

const statusText: Record<SubmissionStatus, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已退回',
  ONLINE: '已上架',
  OFFLINE: '已下架'
}

async function load() {
  loading.value = true
  try {
    const res = await listAll(page.value, size.value, {
      status: filters.status || undefined,
      type: filters.type || undefined
    })
    list.value = res.list || []
    total.value = res.total || 0
  } catch {
    /* 错误已提示 */
  } finally {
    loading.value = false
  }
}

function onFilter() {
  page.value = 1
  load()
}

function onReset() {
  filters.status = ''
  filters.type = ''
  page.value = 1
  load()
}

async function handleOffline(row: Submission) {
  try {
    await ElMessageBox.confirm(`确认下架作品「${row.title}」？下架后将不再展示在前台。`, '下架确认', {
      type: 'warning',
      confirmButtonText: '下架',
      cancelButtonText: '取消'
    })
    await offline(row.id)
    ElMessage.success('已下架')
    load()
  } catch (e) {
    if (e !== 'cancel') {
      /* 其他错误已提示 */
    }
  }
}

async function handleDelete(row: Submission) {
  try {
    await ElMessageBox.confirm(`确认删除作品「${row.title}」？该操作不可恢复。`, '删除确认', {
      type: 'error',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteSubmission(row.id)
    ElMessage.success('已删除')
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
      <h2 class="page-title" style="margin: 0">作品管理</h2>
      <div class="spacer" />
      <el-button :icon="RefreshLeft" @click="load">刷新</el-button>
    </div>

    <div class="card-block">
      <div class="filter-bar">
        <el-select
          v-model="filters.status"
          placeholder="按状态筛选"
          clearable
          style="width: 150px"
          @change="onFilter"
        >
          <el-option
            v-for="o in statusOptions"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
        <el-select
          v-model="filters.type"
          placeholder="按类型筛选"
          clearable
          style="width: 150px"
          @change="onFilter"
        >
          <el-option
            v-for="o in typeOptions"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
        <el-button :icon="Search" type="primary" @click="onFilter">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </div>

      <el-table :data="list" stripe style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="authorName" label="作者" width="130" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            {{ typeOptions.find((o) => o.value === row.type)?.label || row.type }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status as SubmissionStatus]" effect="light" size="small">
              {{ statusText[row.status as SubmissionStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="track" label="赛道" width="120">
          <template #default="{ row }">
            <span>{{ row.track || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="downloads" label="下载量" width="100" />
        <el-table-column prop="submittedAt" label="提交时间" width="170" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button
              type="warning"
              size="small"
              :icon="Download"
              :disabled="row.status !== 'ONLINE'"
              @click="handleOffline(row)"
            >
              下架
            </el-button>
            <el-button type="danger" size="small" :icon="Delete" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无作品" />
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

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
</style>
