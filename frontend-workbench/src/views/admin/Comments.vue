<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshLeft } from '@element-plus/icons-vue'
import { listReported, hideComment, restoreComment, deleteComment, type ReportedComment } from '@/api/admin-comment'

const loading = ref(false)
const list = ref<ReportedComment[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

async function load() {
  loading.value = true
  try {
    const res = await listReported(page.value, size.value)
    list.value = res.list || []
    total.value = res.total || 0
  } catch { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

async function handleHide(row: any) {
  try {
    await ElMessageBox.confirm('确认隐藏该评论？', '隐藏确认', { type: 'warning' })
    await hideComment(row.id)
    ElMessage.success('已隐藏')
    load()
  } catch (e) { if (e !== 'cancel') { /* 拦截器已提示 */ } }
}

async function handleRestore(row: any) {
  await restoreComment(row.id)
  ElMessage.success('已恢复')
  load()
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确认删除该评论？不可恢复。', '删除确认', { type: 'error' })
    await deleteComment(row.id)
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
      <h2 class="page-title">评论管理</h2>
      <div class="spacer" />
      <el-button :icon="RefreshLeft" @click="load">刷新</el-button>
    </div>
    <div class="card-block">
      <el-table :data="list" stripe style="width: 100%">
        <el-table-column prop="content" label="评论内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="reportCount" label="举报次数" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.hidden ? 'info' : 'warning'" size="small">{{ row.hidden ? '已隐藏' : '可见' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.hidden" size="small" type="warning" @click="handleHide(row)">隐藏</el-button>
            <el-button v-else size="small" type="success" @click="handleRestore(row)">恢复</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无被举报评论" /></template>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next, jumper" :total="total" :current-page="page" :page-size="size" @current-change="onPageChange" />
      </div>
    </div>
  </div>
</template>
