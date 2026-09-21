<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElLoading } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { loginByCode, redirectToWecomAuth, me } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const redirecting = ref(false)

/** 点击企微 SSO 登录 → 跳转授权页 */
async function handleSsoLogin() {
  redirecting.value = true
  try {
    await redirectToWecomAuth()
  } catch {
    // 错误已由响应拦截器提示
  } finally {
    redirecting.value = false
  }
}

/** 处理企微回调的 ?code= */
async function handleCallback(code: string) {
  const loading = ElLoading.service({
    text: '正在登录…',
    background: 'rgba(255,255,255,0.7)'
  })
  try {
    const res = await loginByCode(code)
    auth.setToken(res.token)
    if (res.user) {
      auth.setUser(res.user)
    } else {
      auth.setUser(await me())
    }
    // 角色守卫
    if (!auth.canAccessAdmin) {
      ElMessage.error('当前账号无运营后台访问权限（需 OPERATOR / ADMIN）')
      auth.logout()
      return
    }
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.replace(redirect)
  } catch {
    // 失败回到登录页，保留无 code 的干净 URL
    router.replace({ name: 'Login' })
  } finally {
    loading.close()
  }
}

onMounted(() => {
  const code = route.query.code as string | undefined
  if (code) {
    handleCallback(code)
  }
})
</script>

<template>
  <div class="login-page">
    <div class="ambient ambient-a" />
    <div class="ambient ambient-b" />

    <div class="login-card">
      <div class="brand">
        <div class="brand-mark">AI</div>
        <div class="brand-text">
          <h1>企业 AI 应用市场</h1>
          <p>运营后台 · Operations Console</p>
        </div>
      </div>

      <div class="login-body">
        <p class="hint">使用企业微信账号扫码登录，仅限运营 / 超管 / 评委角色访问。</p>
        <el-button
          type="primary"
          size="large"
          class="sso-btn"
          :loading="redirecting"
          @click="handleSsoLogin"
        >
          <span v-if="!redirecting">企业微信 SSO 登录</span>
        </el-button>
        <p class="footnote muted">登录即表示同意企业 AI 应用市场使用规范</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
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

.login-card {
  position: relative;
  width: 400px;
  max-width: calc(100vw - 32px);
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

.login-body {
  text-align: center;
}

.hint {
  margin: 0 0 22px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}

.sso-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.5px;
  border-radius: 10px;
}

.footnote {
  margin: 18px 0 0;
  font-size: 12px;
}
</style>
