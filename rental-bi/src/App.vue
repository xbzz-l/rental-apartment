<template>
  <!-- 未登录显示登录页，登录后显示数据看板 -->
  <Dashboard v-if="authState.token" />
  <Login v-else />
</template>

<script setup>
import { authState } from './store/auth'
import Dashboard from './views/Dashboard.vue'
import Login from './views/Login.vue'
</script>
