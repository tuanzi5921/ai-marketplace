<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { changePassword } from '@/api/auth'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const submitting = ref(false)

async function handleSubmit() {
  if (!form.oldPassword || !form.newPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  if (form.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  submitting.value = true
  try {
    await changePassword(form.oldPassword, form.newPassword)
    if (auth.user) {
      auth.setUser({ ...auth.user, mustChangePassword: false })
    }
    ElMessage.success('密码修改成功')
    router.replace('/')
  } catch { /* 拦截器已提示 */ } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="change-pwd-page">
    <div class="change-pwd-card">
      <h2>修改密码</h2>
      <p class="hint" v-if="auth.needChangePassword">首次登录请修改初始密码</p>
      <el-form class="pwd-form" label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="当前密码">
          <el-input v-model="form.oldPassword" type="password" show-password size="large" autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="form.newPassword" type="password" show-password size="large" autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="form.confirmPassword" type="password" show-password size="large" autocomplete="new-password" @keyup.enter="handleSubmit" />
        </el-form-item>
        <el-button type="primary" size="large" round class="submit-btn" :loading="submitting" @click="handleSubmit">确认修改</el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.change-pwd-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}
.change-pwd-card {
  width: 420px;
  max-width: calc(100vw - 32px);
  padding: 40px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
  text-align: center;
}
.change-pwd-card h2 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #1a1a2e;
}
.hint {
  margin: 0 0 28px;
  color: #e6a23c;
  font-size: 14px;
}
.pwd-form {
  text-align: left;
}
.pwd-form :deep(.el-form-item__label) {
  font-size: 13px;
  color: #606266;
}
.submit-btn {
  width: 100%;
  margin-top: 8px;
}
</style>
