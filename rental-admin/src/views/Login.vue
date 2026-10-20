<template>
  <div class="login-page">
    <div class="login-box">
      <!-- 品牌侧 -->
      <aside class="brand-side">
        <div class="brand-mark">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" aria-hidden="true">
            <path d="M3 11.4 12 4l9 7.4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M6.2 10.6V20h11.6v-9.4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
        <h1 class="brand-title">公寓租房</h1>
        <p class="brand-sub">运营管理后台</p>
        <ul class="brand-points">
          <li>房源增删改查</li>
          <li>用户与租赁记录</li>
          <li>业务数据统计</li>
        </ul>
      </aside>

      <!-- 表单侧 -->
      <section class="form-side">
        <h2 class="form-title">登录</h2>
        <p class="form-sub">请使用管理员账号登录</p>
        <el-form :model="form" @submit.prevent="handleLogin">
          <el-form-item>
            <el-input v-model="form.username" placeholder="请输入账号" size="large">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password>
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form>
        <div class="login-tip">默认管理员账号：admin / admin123</div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)

// 管理员登录
async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login({ ...form })
    ElMessage.success('登录成功')
    router.replace('/')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--ink);
}
.login-box {
  display: flex;
  width: 100%;
  max-width: 860px;
  background: var(--surface);
  border-radius: 22px;
  overflow: hidden;
  box-shadow: 0 30px 70px -24px rgba(0, 0, 0, 0.55);
}

/* ---- 品牌侧 ---- */
.brand-side {
  position: relative;
  width: 340px;
  flex-shrink: 0;
  padding: 44px 36px;
  background: linear-gradient(160deg, var(--brand) 0%, var(--brand-700) 100%);
  color: #fff;
}
.brand-mark {
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.2);
}
.brand-title {
  margin-top: 26px;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 1.5px;
}
.brand-sub {
  margin-top: 8px;
  font-size: 13px;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.66);
}
.brand-points {
  margin-top: 34px;
  padding: 0;
  list-style: none;
}
.brand-points li {
  position: relative;
  padding-left: 18px;
  margin-bottom: 13px;
  font-size: 13.5px;
  color: rgba(255, 255, 255, 0.86);
}
.brand-points li::before {
  content: "";
  position: absolute;
  left: 0;
  top: 7px;
  width: 7px;
  height: 7px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.6);
  transform: rotate(45deg);
}

/* ---- 表单侧 ---- */
.form-side {
  flex: 1;
  padding: 52px 46px;
  min-width: 0;
}
.form-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--ink);
  letter-spacing: 0.5px;
}
.form-sub {
  margin: 8px 0 28px;
  font-size: 13px;
  color: var(--muted);
}
.login-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 3px;
}
.login-tip {
  margin-top: 20px;
  text-align: center;
  color: var(--faint);
  font-size: 12px;
}

@media (max-width: 720px) {
  .brand-side {
    display: none;
  }
  .form-side {
    padding: 40px 28px;
  }
}
</style>
