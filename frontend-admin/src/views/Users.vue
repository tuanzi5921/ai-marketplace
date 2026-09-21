<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshLeft, UserFilled, ArrowDown } from '@element-plus/icons-vue'
import { listUsers, grantRole, enable, disable, type UserItem } from '@/api/user'
import type { UserRole } from '@/stores/auth'

const loading = ref(false)
const list = ref<UserItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const roleOptions: UserRole[] = ['OPERATOR', 'JUDGE', 'DEPT_HEAD', 'ADMIN']

const roleTagType: Record<UserRole, '' | 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
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
  } catch {
    /* 错误已提示 */
  } finally {
    loading.value = false
  }
}

// 授予角色（下拉选择）
async function handleGrantRole(row: UserItem, role: UserRole) {
  if (row.roles?.includes(role)) {
    ElMessage.info(`该用户已拥有 ${role} 角色`)
    return
  }
  try {
    await ElMessageBox.confirm(`确认向用户「${row.displayName}」授予 ${role} 角色？`, '授予角色', {
      type: 'warning',
      confirmButtonText: '授予',
      cancelButtonText: '取消'
    })
    await grantRole(row.id, role)
    ElMessage.success('角色已授予')
    load()
  } catch (e) {
    if (e !== 'cancel') {
      /* 其他错误已提示 */
    }
  }
}

// 启停
async function handleToggleEnabled(row: UserItem) {
  try {
    if (row.enabled) {
      await ElMessageBox.confirm(`确认停用用户「${row.displayName}」？`, '停用确认', {
        type: 'warning',
        confirmButtonText: '停用',
        cancelButtonText: '取消'
      })
      await disable(row.id)
      ElMessage.success('已停用')
    } else {
      await enable(row.id)
      ElMessage.success('已启用')
    }
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
      <h2 class="page-title" style="margin: 0">用户管理</h2>
      <div class="spacer" />
      <el-button :icon="RefreshLeft" @click="load">刷新</el-button>
    </div>

    <div class="card-block">
      <el-table :data="list" stripe style="width: 100%">
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="displayName" label="姓名" width="140" />
        <el-table-column prop="department" label="部门" width="160">
          <template #default="{ row }">
            <span>{{ row.department || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="220">
          <template #default="{ row }">
            <el-tag
              v-for="r in (row.roles || [])"
              :key="r"
              :type="roleTagType[r as UserRole]"
              effect="light"
              size="small"
              style="margin-right: 6px"
            >
              {{ r }}
            </el-tag>
            <span v-if="!row.roles?.length" class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="100" />
        <el-table-column label="启用状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain" size="small">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-dropdown trigger="click" @command="(c: any) => handleGrantRole(row as UserItem, c)">
              <el-button size="small" type="primary">
                授予角色<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="r in roleOptions"
                    :key="r"
                    :command="r"
                    :disabled="(row as UserItem).roles?.includes(r)"
                  >
                    {{ r }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button
              size="small"
              :type="(row as UserItem).enabled ? 'warning' : 'success'"
              @click="handleToggleEnabled(row as UserItem)"
            >
              {{ (row as UserItem).enabled ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无用户" />
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
.el-icon--right {
  margin-left: 4px;
}
</style>
