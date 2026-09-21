<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormRules, type FormInstance } from 'element-plus'
import { Plus, Edit, Check, Minus, RefreshLeft } from '@element-plus/icons-vue'
import {
  list,
  create,
  update,
  activate,
  switchPhase,
  type Competition,
  type CompetitionDTO,
  type CompetitionPhase
} from '@/api/competition'

const loading = ref(false)
const listData = ref<Competition[]>([])

const phases: CompetitionPhase[] = ['SUBMIT', 'REVIEW', 'AWARD']
const phaseText: Record<CompetitionPhase, string> = {
  SUBMIT: '征集',
  REVIEW: '评审',
  AWARD: '颁奖'
}

async function load() {
  loading.value = true
  try {
    const res = await list()
    listData.value = Array.isArray(res) ? res : []
  } catch {
    /* 错误已提示 */
  } finally {
    loading.value = false
  }
}

// —— 新建 / 编辑弹窗 ——
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInstance>()
const submitting = ref(false)

function emptyForm(): CompetitionDTO & { awardsConfig: any[]; tracksConfig: any[] } {
  return {
    name: '',
    submitStartAt: '',
    submitEndAt: '',
    reviewAt: '',
    shortlistThreshold: 80,
    topNPerTrack: 3,
    awardsConfig: [],
    tracksConfig: []
  }
}

const form = reactive(emptyForm())

const rules: FormRules = {
  name: [{ required: true, message: '请输入大赛名称', trigger: 'blur' }],
  submitStartAt: [{ required: true, message: '请选择征集开始时间', trigger: 'change' }],
  submitEndAt: [{ required: true, message: '请选择征集结束时间', trigger: 'change' }],
  reviewAt: [{ required: true, message: '请选择评审时间', trigger: 'change' }],
  shortlistThreshold: [{ required: true, message: '请输入入围阈值', trigger: 'blur' }],
  topNPerTrack: [{ required: true, message: '请输入每赛道 Top 数', trigger: 'blur' }]
}

function openCreate() {
  dialogMode.value = 'create'
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row: Competition) {
  dialogMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    name: row.name,
    submitStartAt: row.submitStartAt,
    submitEndAt: row.submitEndAt,
    reviewAt: row.reviewAt,
    shortlistThreshold: row.shortlistThreshold,
    topNPerTrack: row.topNPerTrack,
    awardsConfig: row.awardsConfig ? [...row.awardsConfig] : [],
    tracksConfig: row.tracksConfig ? [...row.tracksConfig] : []
  })
  dialogVisible.value = true
}

function addAward() {
  form.awardsConfig.push({ name: '', count: 1, reward: '' })
}
function removeAward(i: number) {
  form.awardsConfig.splice(i, 1)
}
function addTrack() {
  form.tracksConfig.push({ key: '', name: '', description: '' })
}
function removeTrack(i: number) {
  form.tracksConfig.splice(i, 1)
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload: CompetitionDTO = { ...form }
      if (dialogMode.value === 'create') {
        await create(payload)
        ElMessage.success('大赛已创建')
      } else {
        await update(payload)
        ElMessage.success('大赛已更新')
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

// —— 行操作 ——
async function handleActivate(row: Competition) {
  try {
    await ElMessageBox.confirm(`确认激活大赛「${row.name}」？激活后将成为当前大赛。`, '激活确认', {
      type: 'warning',
      confirmButtonText: '激活',
      cancelButtonText: '取消'
    })
    await activate(row.id)
    ElMessage.success('已激活')
    load()
  } catch (e) {
    if (e !== 'cancel') {
      /* 其他错误已提示 */
    }
  }
}

async function handleSwitchPhase(row: Competition, phase: CompetitionPhase) {
  if (row.phase === phase) return
  try {
    await ElMessageBox.confirm(
      `确认将大赛「${row.name}」切换到「${phaseText[phase]}」阶段？`,
      '阶段切换',
      { type: 'warning', confirmButtonText: '切换', cancelButtonText: '取消' }
    )
    await switchPhase(row.id, phase)
    ElMessage.success('阶段已切换')
    load()
  } catch (e) {
    if (e !== 'cancel') {
      /* 其他错误已提示 */
    }
  }
}

onMounted(load)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <div class="toolbar">
      <h2 class="page-title" style="margin: 0">大赛配置</h2>
      <div class="spacer" />
      <el-button :icon="RefreshLeft" @click="load">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建大赛</el-button>
    </div>

    <div class="card-block">
      <el-table :data="listData" stripe style="width: 100%">
        <el-table-column prop="name" label="大赛名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.active" type="success" effect="dark" size="small">已激活</el-tag>
            <el-tag v-else type="info" effect="plain" size="small">未激活</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前阶段" width="110">
          <template #default="{ row }">
            <span>{{ row.phase ? phaseText[row.phase as CompetitionPhase] : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitStartAt" label="征集开始" width="170" />
        <el-table-column prop="submitEndAt" label="征集结束" width="170" />
        <el-table-column prop="reviewAt" label="评审时间" width="170" />
        <el-table-column prop="shortlistThreshold" label="入围阈值" width="100" />
        <el-table-column prop="topNPerTrack" label="每赛道Top" width="100" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :icon="Edit" @click="openEdit(row as Competition)">编辑</el-button>
            <el-button
              size="small"
              type="success"
              :icon="Check"
              :disabled="(row as Competition).active"
              @click="handleActivate(row as Competition)"
            >
              激活
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无大赛，点击「新建大赛」创建" />
        </template>
      </el-table>

      <!-- 阶段切换行（在表格下方独立展示，便于操作） -->
      <div class="phase-block" v-if="listData.length">
        <div class="phase-block-title muted">阶段切换（灰色为当前阶段）</div>
        <div v-for="row in listData" :key="row.id" class="phase-row">
          <span class="phase-row-name">{{ row.name }}</span>
          <el-button-group>
            <el-button
              v-for="p in phases"
              :key="p"
              size="small"
              :type="row.phase === p ? 'primary' : ''"
              :disabled="row.phase === p"
              @click="handleSwitchPhase(row, p)"
            >
              {{ phaseText[p] }}
            </el-button>
          </el-button-group>
        </div>
      </div>
    </div>

    <!-- 新建 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建大赛' : '编辑大赛'"
      width="720px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="大赛名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入大赛名称" maxlength="60" />
        </el-form-item>
        <el-form-item label="征集开始时间" prop="submitStartAt">
          <el-date-picker
            v-model="form.submitStartAt"
            type="datetime"
            placeholder="选择征集开始时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="征集结束时间" prop="submitEndAt">
          <el-date-picker
            v-model="form.submitEndAt"
            type="datetime"
            placeholder="选择征集结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="评审时间" prop="reviewAt">
          <el-date-picker
            v-model="form.reviewAt"
            type="datetime"
            placeholder="选择评审时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="入围阈值" prop="shortlistThreshold">
          <el-input-number v-model="form.shortlistThreshold" :min="0" :max="100" />
          <span class="muted form-hint">百分制分数</span>
        </el-form-item>
        <el-form-item label="每赛道Top" prop="topNPerTrack">
          <el-input-number v-model="form.topNPerTrack" :min="1" :max="50" />
        </el-form-item>

        <!-- 赛道配置 -->
        <el-form-item label="赛道配置">
          <div class="dyn-list">
            <div v-for="(t, i) in form.tracksConfig" :key="i" class="dyn-row">
              <el-input v-model="t.key" placeholder="赛道Key" style="width: 140px" />
              <el-input v-model="t.name" placeholder="赛道名称" style="width: 180px" />
              <el-input v-model="t.description" placeholder="描述（选填）" style="flex: 1" />
              <el-button type="danger" :icon="Minus" link @click="removeTrack(i)">移除</el-button>
            </div>
            <el-button :icon="Plus" link type="primary" @click="addTrack">添加赛道</el-button>
          </div>
        </el-form-item>

        <!-- 奖项配置 -->
        <el-form-item label="奖项配置">
          <div class="dyn-list">
            <div v-for="(a, i) in form.awardsConfig" :key="i" class="dyn-row">
              <el-input v-model="a.name" placeholder="奖项名称" style="width: 160px" />
              <el-input-number v-model="a.count" :min="1" :max="99" placeholder="数量" />
              <el-input v-model="a.reward" placeholder="奖励说明" style="flex: 1" />
              <el-button type="danger" :icon="Minus" link @click="removeAward(i)">移除</el-button>
            </div>
            <el-button :icon="Plus" link type="primary" @click="addAward">添加奖项</el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ dialogMode === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.phase-block {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed var(--border-color);
}

.phase-block-title {
  font-size: 13px;
  margin-bottom: 12px;
}

.phase-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 6px 0;
}

.phase-row-name {
  width: 200px;
  font-size: 13px;
  color: var(--text-regular);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dyn-list {
  width: 100%;
}

.dyn-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.form-hint {
  margin-left: 10px;
  font-size: 12px;
}
</style>
