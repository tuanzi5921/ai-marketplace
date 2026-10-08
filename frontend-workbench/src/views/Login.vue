<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { localLogin, type LoginResult } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const submitting = ref(false)
const form = reactive({ account: '', password: '' })

async function handleLogin() {
  if (!form.account.trim() || !form.password) {
    ElMessage.warning('请输入账号与密码')
    return
  }
  submitting.value = true
  try {
    const res: LoginResult = await localLogin(form.account.trim(), form.password)
    auth.setToken(res.token)
    auth.setUser(res.user)
    ElMessage.success('登录成功')
    if (res.user.mustChangePassword) {
      router.replace('/change-password')
    } else {
      router.replace((route.query.redirect as string) || '/')
    }
  } catch {
    // request 拦截器已提示错误
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-blob bg-blob--1"></div>
      <div class="bg-blob bg-blob--2"></div>
      <div class="bg-grid"></div>
    </div>

    <div class="login-card">
      <div class="login-card__head">
        <div class="logo">AI</div>
        <h1>企业 AI 应用市场</h1>
        <p class="subtitle">上传作品 · 审核评分 · 排行榜</p>
      </div>

      <div class="login-card__body">
        <el-form class="login-form" label-position="top" @submit.prevent="handleLogin">
          <el-form-item label="账号">
            <el-input
              v-model="form.account"
              placeholder="请输入账号"
              size="large"
              autocomplete="username"
              clearable
            />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              autocomplete="current-password"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-button
            class="submit-btn"
            type="primary"
            size="large"
            round
            :loading="submitting"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form>

        <p class="tip">首次登录默认密码为 Welcome@2026，登录后请及时修改</p>
      </div>

      <footer class="login-card__foot">
        登录即代表同意《企业 AI 应用市场使用规范》
      </footer>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #0f1e4d;
}

.login-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}
.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.6;
}
.bg-blob--1 {
  width: 480px;
  height: 480px;
  top: -120px;
  left: -80px;
  background: radial-gradient(circle at 30% 30%, #4c6ef5, transparent 70%);
}
.bg-blob--2 {
  width: 520px;
  height: 520px;
  bottom: -160px;
  right: -120px;
  background: radial-gradient(circle at 70% 70%, #14b8a6, transparent 70%);
}
.bg-grid {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse at center, #000 30%, transparent 75%);
}

.login-card {
  position: relative;
  z-index: 1;
  width: 420px;
  max-width: calc(100vw - 32px);
  max-height: calc(100vh - 32px);
  overflow-y: auto;
  padding: 48px 40px 28px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 30px 70px rgba(8, 18, 48, 0.45);
  text-align: center;
}

.login-card__head .logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #3b5bdb, #14b8a6);
  color: #fff;
  font-weight: 700;
  font-size: 22px;
  margin-bottom: 18px;
}
.login-card__head h1 {
  margin: 0 0 6px;
  font-size: 24px;
  color: var(--brand-text);
  letter-spacing: 1px;
}
.subtitle {
  margin: 0 0 36px;
  color: var(--brand-muted);
  font-size: 14px;
}

.login-form {
  text-align: left;
}
.login-form :deep(.el-form-item__label) {
  font-size: 13px;
  color: var(--brand-muted);
  padding-bottom: 4px;
}
.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  margin-top: 4px;
}

.tip {
  margin: 16px 0 0;
  font-size: 12px;
  color: var(--brand-muted);
}

.login-card__foot {
  margin-top: 36px;
  font-size: 12px;
  color: var(--brand-muted);
  border-top: 1px solid var(--brand-border);
  padding-top: 16px;
}
</style>
