<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshLeft, Hide, View, Delete } from '@element-plus/icons-vue'
import { listReported, hide, restore, deleteComment, type ReportedComment } from '@/api/comment'

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
  } catch {
    /* 错误已提示 */
  } finally {
    loading.value = false
  }
}

async function handleHide(row: ReportedComment) {
  try {
    await ElMessageBox.confirm('确认隐藏该评论？', '隐藏确认', {
      type: 'warning',
      confirmButtonText: '隐藏',
      cancelButtonText: '取消'
    })
    await hide(row.id)
    ElMessage.success('已隐藏')
    load()
  } catch (e) {
    if (e !== 'cancel') {
      /* 其他错误已提示 */
    }
  }
}

async function handleRestore(row: ReportedComment) {
  try {
    await ElMessageBox.confirm('确认恢复该评论？', '恢复确认', {
      type: 'info',
      confirmButtonText: '恢复',
      cancelButtonText: '取消'
    })
    await restore(row.id)
    ElMessage.success('已恢复')
    load()
  } catch (e) {
    if (e !== 'cancel') {
      /* 其他错误已提示 */
    }
  }
}

async function handleDelete(row: ReportedComment) {
  try {
    await ElMessageBox.confirm('确认删除该评论？该操作不可恢复。', '删除确认', {
      type: 'error',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteComment(row.id)
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
      <h2 class="page-title" style="margin: 0">评论管理</h2>
      <div class="spacer" />
      <el-button :icon="RefreshLeft" @click="load">刷新</el-button>
    </div>

    <div class="card-block">
      <el-table :data="list" stripe style="width: 100%">
        <el-table-column prop="content" label="评论内容" min-width="280" show-overflow-tooltip />
        <el-table-column prop="authorName" label="作者" width="140" />
        <el-table-column prop="targetTitle" label="所属作品" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.targetTitle || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.hidden ? 'info' : 'warning'" effect="plain" size="small">
              {{ row.hidden ? '已隐藏' : '可见' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reportCount" label="举报次数" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="danger" effect="dark" size="small">{{ row.reportCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reportedAt" label="举报时间" width="170" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="warning" :icon="Hide" @click="handleHide(row)">隐藏</el-button>
            <el-button size="small" type="success" :icon="View" @click="handleRestore(row)">恢复</el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无被举报评论" />
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
