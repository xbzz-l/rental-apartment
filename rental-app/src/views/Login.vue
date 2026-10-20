<template>
  <div class="login-page">
    <!-- 品牌区 -->
    <header class="login-hero">
      <span class="logo-badge">
        <svg viewBox="0 0 24 24" width="26" height="26" fill="none" aria-hidden="true">
          <path d="M3 11.4 12 4l9 7.4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          <path d="M6.2 10.6V20h11.6v-9.4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </span>
      <h1 class="login-title">公寓租房</h1>
      <p class="login-sub">找到你的理想公寓</p>
    </header>

    <!-- 表单面板 -->
    <section class="login-panel">
      <van-tabs v-model:active="active" class="login-tabs">
        <!-- 登录 -->
        <van-tab title="登录">
          <van-form @submit="handleLogin">
            <van-cell-group inset>
              <van-field v-model="loginForm.username" placeholder="请输入账号"
                         :rules="[{ required: true, message: '请输入账号' }]" />
              <van-field v-model="loginForm.password" type="password" placeholder="请输入密码"
                         :rules="[{ required: true, message: '请输入密码' }]" />
            </van-cell-group>
            <div class="submit-btn">
              <van-button round block type="primary" native-type="submit" :loading="loading">登 录</van-button>
            </div>
            <div class="login-tip">默认账号：user / user123</div>
          </van-form>
        </van-tab>
        <!-- 注册 -->
        <van-tab title="注册">
          <van-form @submit="handleRegister">
            <van-cell-group inset>
              <van-field v-model="registerForm.username" placeholder="请输入账号"
                         :rules="[{ required: true, message: '请输入账号' }]" />
              <van-field v-model="registerForm.password" type="password" placeholder="请设置密码（6-20位）"
                         :rules="[{ required: true, message: '请输入密码' }, { pattern: /^.{6,20}$/, message: '密码长度需为6-20位' }]" />
              <van-field v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码"
                         :rules="[{ required: true, message: '请再次输入密码' }, { validator: sameAsPassword, message: '两次输入的密码不一致' }]" />
            </van-cell-group>
            <div class="register-tip">昵称将默认使用账号，真实信息可在「我的 - 编辑资料」中完善</div>
            <div class="submit-btn">
              <van-button round block type="primary" native-type="submit" :loading="loading">注 册</van-button>
            </div>
          </van-form>
        </van-tab>
      </van-tabs>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const active = ref(0)
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', confirmPassword: '' })

// 确认密码校验：与首次输入的密码一致
const sameAsPassword = (val) => val === registerForm.password

// 登录/注册成功后跳回来源页或首页
function redirectBack() {
  router.replace(route.query.redirect || '/')
}

// 登录：校验账号密码并保存登录态
async function handleLogin() {
  loading.value = true
  try {
    await userStore.login({ ...loginForm })
    showSuccessToast('登录成功')
    redirectBack()
  } catch {
    // 失败提示已由 store（管理员账号）或请求拦截器（网络/业务错误）统一弹出
  } finally {
    loading.value = false
  }
}

// 注册：成功后自动登录（昵称由后端统一取账号）
async function handleRegister() {
  loading.value = true
  try {
    await userStore.register({ username: registerForm.username, password: registerForm.password })
    await userStore.login({ username: registerForm.username, password: registerForm.password })
    showSuccessToast('注册成功')
    redirectBack()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100%;
  background: var(--paper);
  display: flex;
  flex-direction: column;
}

/* ---- 品牌区 ---- */
.login-hero {
  padding: 56px 24px 56px;
  background: var(--ink);
  text-align: center;
  color: #fff;
}
.logo-badge {
  width: 52px;
  height: 52px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.09);
  border: 1px solid rgba(255, 255, 255, 0.14);
  color: #fff;
}
.login-title {
  font-size: 25px;
  font-weight: 800;
  letter-spacing: 1.5px;
}
.login-sub {
  margin-top: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.58);
  letter-spacing: 0.6px;
}

/* ---- 表单面板：以浅色圆角面盖住品牌区底部 ---- */
.login-panel {
  flex: 1;
  margin-top: -24px;
  padding-top: 6px;
  background: var(--paper);
  border-radius: 24px 24px 0 0;
  position: relative;
  z-index: 1;
}
.login-tabs {
  --van-tabs-nav-background: transparent;
}
.login-tabs :deep(.van-tabs__wrap) {
  background: transparent;
}
.login-tabs :deep(.van-tab) {
  font-size: 15px;
  font-weight: 600;
  color: var(--muted);
}
.login-tabs :deep(.van-tab--active) {
  color: var(--ink);
  font-weight: 700;
}
.login-tabs :deep(.van-tabs__line) {
  height: 3px;
  border-radius: 3px;
}
.submit-btn {
  margin: 22px 12px 8px;
}
.submit-btn :deep(.van-button) {
  height: 46px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 2px;
  box-shadow: var(--sh-brand);
}
.register-tip {
  margin: 12px 20px 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--faint);
}
.login-tip {
  padding-bottom: 16px;
  text-align: center;
  color: var(--faint);
  font-size: 12px;
}
</style>
