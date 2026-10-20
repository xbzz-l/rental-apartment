<template>
  <el-container class="layout">
    <el-aside width="224px" class="aside">
      <div class="logo">
        <span class="logo-badge">
          <svg viewBox="0 0 24 24" width="19" height="19" fill="none" aria-hidden="true">
            <path d="M3 11.4 12 4l9 7.4" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M6.2 10.6V20h11.6v-9.4" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </span>
        <div class="logo-text">
          <b>公寓租房</b>
          <i>管理后台</i>
        </div>
      </div>

      <el-menu :default-active="route.path" router class="menu">
        <el-menu-item index="/apartments">
          <el-icon><OfficeBuilding /></el-icon><span>房源管理</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><UserFilled /></el-icon><span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/stats">
          <el-icon><DataAnalysis /></el-icon><span>业务统计</span>
        </el-menu-item>
      </el-menu>

      <div class="aside-foot">Demo · v1.0</div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="page-title">{{ route.meta.title }}</span>
        <el-dropdown @command="handleCommand">
          <span class="admin-name">
            <span class="avatar">{{ initial }}</span>
            <span class="name">{{ userStore.userInfo?.realName || userStore.userInfo?.username || '管理员' }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 头像占位：真实姓名首字，未填则用账号首字
const initial = computed(() => (userStore.userInfo?.realName || userStore.userInfo?.username || '管').charAt(0))

// 头部下拉菜单命令：退出登录
function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

/* ---- 侧栏 ---- */
.aside {
  display: flex;
  flex-direction: column;
  background: var(--ink);
}
.logo {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 20px 18px 22px;
}
.logo-badge {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 11px;
  background: var(--brand);
  color: #fff;
  box-shadow: var(--sh-brand);
}
.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}
.logo-text b {
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.4px;
}
.logo-text i {
  font-style: normal;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.42);
  letter-spacing: 1.2px;
}

.menu {
  flex: 1;
  border-right: none;
  background: transparent;
  padding: 4px 12px;
}
.menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  margin-bottom: 4px;
  border-radius: 10px;
  color: rgba(255, 255, 255, 0.66);
  font-weight: 600;
}
.menu :deep(.el-menu-item .el-icon) {
  margin-right: 10px;
  font-size: 17px;
}
.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.07);
  color: #fff;
}
.menu :deep(.el-menu-item.is-active) {
  background: var(--brand);
  color: #fff;
  box-shadow: var(--sh-brand);
}
.aside-foot {
  padding: 16px 18px;
  font-size: 11px;
  letter-spacing: 0.6px;
  color: rgba(255, 255, 255, 0.28);
}

/* ---- 头部 ---- */
.header {
  height: 62px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 22px;
  background: var(--surface);
  border-bottom: 1px solid var(--line-2);
}
.page-title {
  font-size: 17px;
  font-weight: 800;
  color: var(--ink);
  letter-spacing: 0.3px;
}
.admin-name {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--ink-2);
  font-weight: 600;
  font-size: 13.5px;
}
.avatar {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--brand-100);
  color: var(--brand);
  font-weight: 700;
  font-size: 13px;
}

.main {
  background: var(--paper);
  padding: 18px;
}
</style>
