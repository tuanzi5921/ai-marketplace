<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { loginByCode, redirectToWecomAuth } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// 企微 SSO 回调携带 code 参数，自动完成登录
onMounted(async () => {
  const code = route.query.code as string | undefined
  if (code) {
    try {
      const result = await loginByCode(code)
      auth.setToken(result.token)
      auth.setUser(result.user)
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/'
      router.replace(redirect)
    } catch {
      // request 拦截器已提示错误
    }
  }
})

// 点击「企微 SSO 登录」跳转授权页
function handleWecomLogin() {
  redirectToWecomAuth()
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
        <p class="subtitle">员工端工作台 · 用 AI 重塑工作方式</p>
      </div>

      <div class="login-card__body">
        <el-button
          type="primary"
          size="large"
          round
          class="sso-btn"
          @click="handleWecomLogin"
        >
          <el-icon class="sso-icon"><ChatDotRound /></el-icon>
          企微 SSO 登录
        </el-button>
        <p class="tip">使用企业微信账号一键登录，开启你的 AI 创作之旅</p>
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

.sso-btn {
  width: 100%;
  font-size: 16px;
  height: 48px;
}
.sso-icon {
  margin-right: 6px;
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
