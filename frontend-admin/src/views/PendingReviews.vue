<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormRules, type FormInstance } from 'element-plus'
import { Check, Close, RefreshLeft } from '@element-plus/icons-vue'
import { listPending, approve, reject, type ReviewItem } from '@/api/review'

const loading = ref(false)
const list = ref<ReviewItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

// 审核弹窗
const dialogVisible = ref(false)
const dialogMode = ref<'approve' | 'reject'>('approve')
const dialogRow = ref<ReviewItem | null>(null)
const reasonForm = reactive({ reason: '' })
const reasonRef = ref<FormInstance>()
const submitting = ref(false)

const reasonRules: FormRules = {
  reason: [
    { required: true, message: '请填写审核说明', trigger: 'blur' },
    { min: 2, max: 200, message: '长度 2-200 字符', trigger: 'blur' }
  ]
}

async function load() {
  loading.value = true
  try {
    const res = await listPending(page.value, size.value)
    list.value = res.list || []
    total.value = res.total || 0
  } catch {
    /* 错误已提示 */
  } finally {
    loading.value = false
  }
}

function openDialog(row: ReviewItem, mode: 'approve' | 'reject') {
  dialogRow.value = row
  dialogMode.value = mode
  reasonForm.reason = ''
  dialogVisible.value = true
}

async function submitReview() {
  if (!reasonRef.value || !dialogRow.value) return
  await reasonRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const id = dialogRow.value!.id
      if (dialogMode.value === 'approve') {
        await approve(id, reasonForm.reason)
        ElMessage.success('已通过审核')
      } else {
        await reject(id, reasonForm.reason)
        ElMessage.success('已退回作品')
      }
      dialogVisible.value = false
      load()
    } catch {
      /* 错误已提示 */
    } finally {
      submitting.value = false
    }
  })
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
      <h2 class="page-title" style="margin: 0">待审作品</h2>
      <div class="spacer" />
      <el-button :icon="RefreshLeft" @click="load">刷新</el-button>
    </div>

    <div class="card-block">
      <el-table :data="list" stripe style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="authorName" label="作者" width="130" />
        <el-table-column prop="authorDepartment" label="部门" width="140">
          <template #default="{ row }">
            <span>{{ row.authorDepartment || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="track" label="赛道" width="120">
          <template #default="{ row }">
            <span>{{ row.track || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="提交时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" :icon="Check" @click="openDialog(row, 'approve')">
              通过
            </el-button>
            <el-button type="danger" size="small" :icon="Close" @click="openDialog(row, 'reject')">
              退回
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无待审作品" />
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

    <!-- 审核弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'approve' ? '审核通过' : '审核退回'"
      width="480px"
      destroy-on-close
    >
      <div class="dialog-row-info" v-if="dialogRow">
        <span class="muted">作品：</span>{{ dialogRow.title }}
      </div>
      <el-form
        ref="reasonRef"
        :model="reasonForm"
        :rules="reasonRules"
        label-position="top"
        style="margin-top: 12px"
      >
        <el-form-item label="审核说明" prop="reason">
          <el-input
            v-model="reasonForm.reason"
            type="textarea"
            :rows="4"
            :placeholder="dialogMode === 'approve' ? '可填写通过说明' : '请填写退回原因'"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          :type="dialogMode === 'approve' ? 'primary' : 'danger'"
          :loading="submitting"
          @click="submitReview"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dialog-row-info {
  font-size: 14px;
  color: var(--text-primary);
}
</style>
