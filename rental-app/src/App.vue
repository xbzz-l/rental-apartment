<template>
  <!--
    手机视口布局：整页锁一屏，内容在 .app-scroll 内滚动，
    底部导航固定在手机框内（不随内容滚动）
  -->
  <div class="app-scroll" :class="{ 'with-tabbar': showTabbar }">
    <router-view />
  </div>
  <van-tabbar v-if="showTabbar" route fixed>
    <van-tabbar-item to="/" icon="home-o">首页</van-tabbar-item>
    <van-tabbar-item to="/profile" icon="user-o">我的</van-tabbar-item>
  </van-tabbar>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
// 首页/个人中心显示底部导航
const showTabbar = computed(() => ['/', '/profile'].includes(route.path))
</script>
