<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { changePassword } from '@/api/auth'

const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧口令', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新口令', trigger: 'blur' },
    { min: 8, max: 128, message: '新口令长度需在 8-128 之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新口令', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的新口令不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await changePassword(form.oldPassword, form.newPassword)
    // 同步清除 mustChangePassword 标志
    if (auth.user) {
      auth.setUser({ ...auth.user, mustChangePassword: false })
    }
    ElMessage.success('口令修改成功')
    router.replace('/dashboard')
  } catch {
    // 错误已由响应拦截器提示
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="cp-page">
    <div class="ambient ambient-a" />
    <div class="ambient ambient-b" />

    <div class="cp-card">
      <div class="brand">
        <div class="brand-mark">AI</div>
        <div class="brand-text">
          <h1>企业 AI 应用市场</h1>
          <p>首次登录 · 修改口令</p>
        </div>
      </div>

      <div class="cp-body">
        <p class="hint">检测到首次登录，请修改初始口令后继续。</p>

        <el-form
          ref="formRef"
          class="cp-form"
          label-position="top"
          :model="form"
          :rules="rules"
          @submit.prevent="handleSubmit"
        >
          <el-form-item label="旧口令" prop="oldPassword">
            <el-input
              v-model="form.oldPassword"
              type="password"
              placeholder="请输入当前口令"
              size="large"
              autocomplete="current-password"
              show-password
            />
          </el-form-item>
          <el-form-item label="新口令" prop="newPassword">
            <el-input
              v-model="form.newPassword"
              type="password"
              placeholder="至少 8 位"
              size="large"
              autocomplete="new-password"
              show-password
            />
          </el-form-item>
          <el-form-item label="确认新口令" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="再次输入新口令"
              size="large"
              autocomplete="new-password"
              show-password
              @keyup.enter="handleSubmit"
            />
          </el-form-item>
          <el-button
            class="submit-btn"
            size="large"
            :loading="submitting"
            @click="handleSubmit"
          >
            提交修改
          </el-button>
        </el-form>

        <p class="footnote muted">修改成功后即可进入运营后台</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cp-page {
  position: relative;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #0f172a;
}

.ambient {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.55;
  pointer-events: none;
}

.ambient-a {
  width: 420px;
  height: 420px;
  background: #4f46e5;
  top: -120px;
  left: -80px;
}

.ambient-b {
  width: 360px;
  height: 360px;
  background: #818cf8;
  bottom: -120px;
  right: -60px;
  opacity: 0.4;
}

.cp-card {
  position: relative;
  width: 400px;
  max-width: calc(100vw - 32px);
  max-height: calc(100vh - 32px);
  overflow-y: auto;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 18px;
  box-shadow: 0 24px 60px rgba(2, 6, 23, 0.4);
  padding: 36px 32px 28px;
  backdrop-filter: blur(6px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
}

.brand-mark {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4f46e5 0%, #818cf8 100%);
  color: #fff;
  font-weight: 700;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 0.5px;
}

.brand-text h1 {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.3px;
}

.brand-text p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #94a3b8;
  letter-spacing: 1px;
}

.cp-body {
  text-align: center;
}

.hint {
  margin: 0 0 22px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}

.cp-form {
  text-align: left;
}

.cp-form :deep(.el-form-item__label) {
  font-size: 13px;
  color: #475569;
  padding-bottom: 4px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  margin-top: 4px;
}

.footnote {
  margin: 18px 0 0;
  font-size: 12px;
}
</style>
