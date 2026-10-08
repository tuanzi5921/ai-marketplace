<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshLeft, Plus } from '@element-plus/icons-vue'
import {
  listCompetitions, activateCompetition, switchPhase,
  type Competition, type CompetitionPhase
} from '@/api/admin-competition'

const loading = ref(false)
const list = ref<Competition[]>([])

const phaseText: Record<CompetitionPhase, string> = {
  SUBMIT: '征集阶段', REVIEW: '评审阶段', AWARD: '颁奖阶段'
}

async function load() {
  loading.value = true
  try {
    list.value = await listCompetitions() || []
  } catch { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

async function handleActivate(row: any) {
  try {
    await ElMessageBox.confirm(`确认激活大赛「${row.name}」？`, '激活确认', { type: 'success' })
    await activateCompetition(row.id)
    ElMessage.success('已激活')
    load()
  } catch (e) { if (e !== 'cancel') { /* 拦截器已提示 */ } }
}

async function handlePhase(row: any, phase: CompetitionPhase) {
  try {
    await ElMessageBox.confirm(`确认切换到「${phaseText[phase]}」？`, '阶段切换', { type: 'warning' })
    await switchPhase(row.id, phase)
    ElMessage.success('已切换')
    load()
  } catch (e) { if (e !== 'cancel') { /* 拦截器已提示 */ } }
}

onMounted(load)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <div class="toolbar">
      <h2 class="page-title">大赛配置</h2>
      <div class="spacer" />
      <el-button :icon="RefreshLeft" @click="load">刷新</el-button>
    </div>
    <div class="card-block">
      <el-table :data="list" stripe style="width: 100%">
        <el-table-column prop="name" label="大赛名称" min-width="180" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phase" label="阶段" width="120">
          <template #default="{ row }">
            <span v-if="row.phase">{{ phaseText[row.phase as CompetitionPhase] || row.phase }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitStartAt" label="征集开始" width="170" />
        <el-table-column prop="submitEndAt" label="征集结束" width="170" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'ACTIVE'" type="success" size="small" @click="handleActivate(row)">激活</el-button>
            <el-button size="small" @click="handlePhase(row, 'SUBMIT')">征集</el-button>
            <el-button size="small" @click="handlePhase(row, 'REVIEW')">评审</el-button>
            <el-button size="small" @click="handlePhase(row, 'AWARD')">颁奖</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无大赛配置" /></template>
      </el-table>
    </div>
  </div>
</template>
