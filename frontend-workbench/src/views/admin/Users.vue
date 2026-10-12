<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { RefreshLeft, ArrowDown, Plus } from '@element-plus/icons-vue'
import { listUsers, grantRole, enable, disable, createUser, type UserItem } from '@/api/user'

const loading = ref(false)
const list = ref<UserItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const roleOptions: string[] = ['USER', 'OPERATOR', 'JUDGE', 'DEPT_HEAD', 'ADMIN']

const roleTagType: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
  USER: 'info',
  OPERATOR: 'primary',
  JUDGE: 'warning',
  DEPT_HEAD: 'success',
  ADMIN: 'danger'
}

async function load() {
  loading.value = true
  try {
    const res = await listUsers(page.value, size.value)
    list.value = res.list || []
    total.value = res.total || 0
  } catch { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

async function handleGrantRole(row: UserItem, role: string) {
  if (row.roles?.includes(role)) {
    ElMessage.info(`该用户已拥有 ${role} 角色`)
    return
  }
  try {
    await ElMessageBox.confirm(`确认向用户「${row.displayName || row.username}」授予 ${role} 角色？`, '授予角色', {
      type: 'warning', confirmButtonText: '授予', cancelButtonText: '取消'
    })
    await grantRole(row.id, role)
    ElMessage.success('角色已授予')
    load()
  } catch (e) {
    if (e !== 'cancel') { /* 拦截器已提示 */ }
  }
}

async function handleToggleEnabled(row: UserItem) {
  try {
    if (row.enabled) {
      await ElMessageBox.confirm(`确认停用用户「${row.displayName || row.username}」？`, '停用确认', {
        type: 'warning', confirmButtonText: '停用', cancelButtonText: '取消'
      })
      await disable(row.id)
      ElMessage.success('已停用')
    } else {
      await enable(row.id)
      ElMessage.success('已启用')
    }
    load()
  } catch (e) {
    if (e !== 'cancel') { /* 拦截器已提示 */ }
  }
}

function onPageChange(p: number) {
  page.value = p
  load()
}

const createDialogVisible = ref(false)
const createSubmitting = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive<{ email: string; account?: string; username?: string; roles: string[] }>({
  email: '', account: '', username: '', roles: []
})

const createRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不合法', trigger: 'blur' }
  ],
  roles: [{ required: true, type: 'array', message: '至少选择一个角色', trigger: 'change' }]
}

function openCreateDialog() {
  createForm.email = ''
  createForm.account = ''
  createForm.username = ''
  createForm.roles = []
  createDialogVisible.value = true
}

async function handleCreateUser() {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
  } catch { return }
  createSubmitting.value = true
  try {
    const res = await createUser({
      email: createForm.email.trim(),
      account: createForm.account?.trim() || undefined,
      username: createForm.username?.trim() || undefined,
      roles: createForm.roles
    })
    createDialogVisible.value = false
    createdAccount.value = res.user?.account || createForm.account?.trim() || ''
    createdPassword.value = res.initialPassword || ''
    createdDialogVisible.value = true
    load()
  } catch { /* 拦截器已提示 */ } finally {
    createSubmitting.value = false
  }
}

// 初始口令只在这一次响应里出现，关掉弹窗就再也拿不到，所以给个复制入口
const createdDialogVisible = ref(false)
const createdAccount = ref('')
const createdPassword = ref('')

async function copyInitialPassword() {
  try {
    await navigator.clipboard.writeText(createdPassword.value)
    ElMessage.success('初始口令已复制')
  } catch {
    ElMessage.warning('浏览器拒绝了剪贴板访问，请手动选中复制')
  }
}

onMounted(load)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <div class="toolbar">
      <h2 class="page-title">用户管理</h2>
      <div class="spacer" />
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建用户</el-button>
      <el-button :icon="RefreshLeft" @click="load">刷新</el-button>
    </div>
    <div class="card-block">
      <el-table :data="list" stripe style="width: 100%">
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="displayName" label="姓名" width="140" />
        <el-table-column prop="department" label="部门" width="160">
          <template #default="{ row }"><span>{{ row.department || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="角色" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="r in (row.roles || [])" :key="r" :type="roleTagType[r]" effect="light" size="small" style="margin-right: 6px">{{ r }}</el-tag>
            <span v-if="!row.roles?.length" class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain" size="small">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-dropdown trigger="click" @command="(c: any) => handleGrantRole(row as UserItem, c as string)">
              <el-button size="small" type="primary">授予角色<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="r in roleOptions" :key="r" :command="r" :disabled="(row as UserItem).roles?.includes(r)">{{ r }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button size="small" :type="(row as UserItem).enabled ? 'warning' : 'success'" @click="handleToggleEnabled(row as UserItem)">{{ (row as UserItem).enabled ? '停用' : '启用' }}</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无用户" /></template>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next, jumper" :total="total" :current-page="page" :page-size="size" @current-change="onPageChange" />
      </div>
    </div>

    <el-dialog v-model="createDialogVisible" title="新建用户" width="480px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="createForm.email" placeholder="作为账号身份" clearable />
        </el-form-item>
        <el-form-item label="账号（可选）" prop="account">
          <el-input v-model="createForm.account" placeholder="留空则取邮箱@前部分" clearable />
        </el-form-item>
        <el-form-item label="用户名（可选）" prop="username">
          <el-input v-model="createForm.username" placeholder="留空则取账号名" clearable />
        </el-form-item>
        <el-form-item label="角色" prop="roles">
          <el-select v-model="createForm.roles" multiple placeholder="至少选择一个角色" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <p class="muted" style="margin: 4px 0 0; font-size: 12px;">初始口令由服务端下发，创建成功后只显示一次，请及时转告用户；用户首次登录后需自行修改。</p>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="handleCreateUser">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createdDialogVisible" title="用户已创建" width="480px">
      <el-alert type="warning" :closable="false" show-icon
                title="初始口令只显示这一次，关闭后无法再查看" style="margin-bottom: 16px;" />
      <el-form label-position="top">
        <el-form-item label="登录账号">
          <el-input :model-value="createdAccount" readonly />
        </el-form-item>
        <el-form-item label="初始口令">
          <el-input :model-value="createdPassword" readonly type="text" />
        </el-form-item>
      </el-form>
      <p class="muted" style="margin: 0; font-size: 12px;">请把以上账号与口令转告用户，其首次登录后会被要求修改口令。</p>
      <template #footer>
        <el-button @click="copyInitialPassword">复制口令</el-button>
        <el-button type="primary" @click="createdDialogVisible = false">我已记录</el-button>
      </template>
    </el-dialog>
  </div>
</template>
