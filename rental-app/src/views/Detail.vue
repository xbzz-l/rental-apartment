<template>
  <div v-if="apartment" class="detail-page">
    <!-- 房源图片轮播 -->
    <div class="media">
      <van-swipe class="img-swipe" :autoplay="3000" indicator-color="#fff">
        <van-swipe-item v-for="(img, i) in imageList" :key="i">
          <van-image :src="img" fit="cover" class="swipe-img" />
        </van-swipe-item>
      </van-swipe>
      <button class="back-btn" aria-label="返回" @click="router.back()">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none">
          <path d="M15 5l-7 7 7 7" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
    </div>

    <!-- 基本信息 -->
    <section class="head-card">
      <div class="head-top">
        <h1 class="head-title">{{ apartment.title }}</h1>
        <span class="status-chip" :class="apartment.status === '可租' ? 'is-free' : 'is-taken'">
          {{ apartment.status }}
        </span>
      </div>
      <div class="head-rent"><i>¥</i>{{ apartment.rent }}<em>/月</em></div>
      <div class="head-tags">
        <span class="chip">{{ apartment.layout }}</span>
        <span class="chip">{{ apartment.area }}㎡</span>
      </div>
    </section>

    <!-- 详情信息 -->
    <section class="card">
      <h2 class="card-title">房源信息</h2>
      <div class="rows">
        <div class="row"><span>户型</span><b>{{ apartment.layout }}</b></div>
        <div class="row"><span>面积</span><b>{{ apartment.area }} ㎡</b></div>
        <div class="row"><span>月租金</span><b class="is-price">¥{{ apartment.rent }}</b></div>
        <div class="row"><span>位置</span><b class="is-wrap">{{ apartment.address }}</b></div>
        <div class="row"><span>发布时间</span><b class="is-muted">{{ apartment.createTime }}</b></div>
      </div>
    </section>

    <!-- 配套设施 -->
    <section v-if="facilities.length" class="card">
      <h2 class="card-title">配套设施</h2>
      <div class="facilities">
        <span v-for="f in facilities" :key="f" class="fac">{{ f }}</span>
      </div>
    </section>

    <!-- 底部预约按钮 -->
    <div class="bottom-bar">
      <div class="bar-price">
        <i>¥</i>{{ apartment.rent }}<em>/月</em>
      </div>
      <van-button
        v-if="apartment.status === '可租'"
        type="primary"
        round
        class="bar-btn"
        :loading="booking"
        @click="handleBooking"
      >
        立即预约
      </van-button>
      <van-button v-else round disabled class="bar-btn">该房源已租出</van-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showDialog, showSuccessToast, showToast } from 'vant'
import { createBooking, getApartment } from '../api'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const apartment = ref(null)
const booking = ref(false)

// 图片列表：逗号分隔字符串转数组
const imageList = computed(() => (apartment.value?.images ? apartment.value.images.split(',') : []))
// 配套设施：逗号分隔字符串转数组
const facilities = computed(() =>
  apartment.value?.facilities ? apartment.value.facilities.split(',').filter(Boolean) : []
)

// 加载房源详情
async function load() {
  apartment.value = await getApartment(route.params.id)
}

// 提交租房预约：未登录先跳登录，已登录确认后提交
async function handleBooking() {
  if (!userStore.isLogin) {
    showToast('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  try {
    await showDialog({
      title: '预约租房',
      message: `确认预约「${apartment.value.title}」吗？预约后该房源将标记为已租。`
    })
  } catch {
    return // 用户取消
  }
  booking.value = true
  try {
    await createBooking(apartment.value.id)
    showSuccessToast('预约成功')
    await load()
  } finally {
    booking.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.detail-page {
  min-height: 100%;
  padding-bottom: 92px;
  background: var(--paper);
}

/* ---- 图片 ---- */
.media {
  position: relative;
}
.img-swipe,
.swipe-img {
  height: 272px;
}
.img-swipe {
  border-radius: 0 0 var(--r-xl) var(--r-xl);
  overflow: hidden;
}
.back-btn {
  position: absolute;
  top: 14px;
  left: 14px;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: rgba(15, 27, 45, 0.42);
  color: #fff;
  cursor: pointer;
  backdrop-filter: blur(6px);
}

/* ---- 头部信息卡 ---- */
.head-card {
  margin: -20px 12px 12px;
  padding: 16px;
  background: var(--surface);
  border-radius: var(--r-lg);
  box-shadow: var(--sh-2);
  position: relative;
  z-index: 2;
}
.head-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}
.head-title {
  font-size: 19px;
  font-weight: 800;
  color: var(--ink);
  letter-spacing: 0.2px;
  line-height: 1.35;
}
.status-chip {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
}
.status-chip.is-free {
  color: var(--teal);
  background: var(--teal-100);
}
.status-chip.is-taken {
  color: var(--muted);
  background: var(--line-2);
}
.head-rent {
  margin-top: 12px;
  color: var(--amber);
  font-size: 27px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.4px;
}
.head-rent i {
  font-style: normal;
  font-size: 15px;
  font-weight: 700;
  margin-right: 2px;
}
.head-rent em {
  font-style: normal;
  font-size: 12px;
  font-weight: 500;
  color: var(--faint);
  margin-left: 3px;
}
.head-tags {
  margin-top: 14px;
  display: flex;
  gap: 8px;
}
.chip {
  font-size: 12px;
  font-weight: 600;
  color: var(--brand);
  background: var(--brand-050);
  padding: 4px 12px;
  border-radius: 999px;
}

/* ---- 信息卡 ---- */
.card {
  margin: 0 12px 12px;
  padding: 16px;
  background: var(--surface);
  border-radius: var(--r-lg);
  box-shadow: var(--sh-1);
}
.card-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0.3px;
  margin-bottom: 12px;
}
.rows {
  display: flex;
  flex-direction: column;
}
.row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-top: 1px solid var(--line-2);
  font-size: 13.5px;
}
.row:first-child {
  border-top: none;
  padding-top: 0;
}
.row span {
  color: var(--muted);
  flex-shrink: 0;
}
.row b {
  font-weight: 600;
  color: var(--ink-2);
  text-align: right;
}
.row b.is-price {
  color: var(--amber);
  font-weight: 800;
}
.row b.is-wrap {
  line-height: 1.45;
}
.row b.is-muted {
  color: var(--faint);
  font-weight: 500;
}

/* ---- 配套设施 ---- */
.facilities {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.fac {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink-2);
  background: var(--paper);
  border: 1px solid var(--line);
  padding: 6px 12px;
  border-radius: 999px;
}

/* ---- 底部操作栏 ---- */
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-top: 1px solid var(--line);
}
.bar-price {
  color: var(--amber);
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.3px;
}
.bar-price i {
  font-style: normal;
  font-size: 13px;
  font-weight: 700;
  margin-right: 1px;
}
.bar-price em {
  font-style: normal;
  font-size: 11px;
  font-weight: 500;
  color: var(--faint);
  margin-left: 2px;
}
.bar-btn {
  flex: 1;
  height: 46px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 1px;
  box-shadow: var(--sh-brand);
}

/* 桌面浏览器打开时预约按钮栏同步限定到 App 宽度内 */
@media (min-width: 431px) {
  .bottom-bar {
    left: 50%;
    right: auto;
    transform: translateX(-50%);
    width: 430px;
  }
}
</style>
