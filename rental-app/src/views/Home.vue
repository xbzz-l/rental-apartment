<template>
  <div class="home-page">
    <!-- 品牌头图：深墨底 + 屋顶标志 -->
    <header class="hero">
      <div class="hero-bar">
        <div class="brand">
          <svg class="roof-mark" viewBox="0 0 24 24" width="18" height="18" fill="none" aria-hidden="true">
            <path d="M3 11.4 12 4l9 7.4" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M6.2 10.6V20h11.6v-9.4" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span class="brand-name">公寓租房</span>
        </div>
        <span class="hero-count"><b>{{ list.length }}</b> 套房源</span>
      </div>
      <h1 class="hero-title">找一处，住得安心</h1>
      <p class="hero-sub">幸福市 · 精选公寓直租</p>
    </header>

    <!-- 筛选：户型 + 租金区间 -->
    <div class="filter-wrap">
      <van-dropdown-menu active-color="var(--brand)">
        <van-dropdown-item v-model="filters.layout" :options="layoutOptions" @change="reload" />
        <van-dropdown-item v-model="filters.rentRange" :options="rentOptions" @change="reload" />
        <van-dropdown-item v-model="filters.status" :options="statusOptions" @change="reload" />
      </van-dropdown-menu>
    </div>

    <!-- 房源列表 + 下拉刷新 -->
    <van-pull-refresh v-model="refreshing" @refresh="reload">
      <div v-if="list.length" class="apt-list">
        <div v-for="apt in list" :key="apt.id" class="apt-card" @click="goDetail(apt.id)">
          <div class="apt-media">
            <van-image :src="firstImage(apt.images)" fit="cover" class="apt-img" />
            <span class="apt-badge" :class="apt.status === '可租' ? 'is-free' : 'is-taken'">
              {{ apt.status }}
            </span>
          </div>
          <div class="apt-body">
            <div class="apt-title van-ellipsis">{{ apt.title }}</div>
            <div class="apt-meta">{{ apt.layout }} · {{ apt.area }}㎡</div>
            <div class="apt-addr van-ellipsis">
              <svg viewBox="0 0 24 24" width="12" height="12" fill="none" aria-hidden="true">
                <path d="M12 21s7-5.3 7-11a7 7 0 1 0-14 0c0 5.7 7 11 7 11Z" stroke="currentColor" stroke-width="2" />
                <circle cx="12" cy="10" r="2.4" stroke="currentColor" stroke-width="2" />
              </svg>
              {{ apt.address }}
            </div>
            <div class="apt-foot">
              <span class="apt-rent"><i>¥</i>{{ apt.rent }}<em>/月</em></span>
              <span class="apt-go">查看</span>
            </div>
          </div>
        </div>
      </div>
      <van-empty v-else description="暂无符合条件的房源" />
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getApartments } from '../api'

const router = useRouter()
const list = ref([])
const refreshing = ref(false)

// 筛选条件（含"全部"选项）
const filters = reactive({ layout: '', rentRange: '', status: '' })
const layoutOptions = [
  { text: '全部户型', value: '' },
  { text: '开间', value: '开间' },
  { text: '一室一厅', value: '一室一厅' },
  { text: '两室一厅', value: '两室一厅' },
  { text: 'Loft', value: 'Loft' }
]
const rentOptions = [
  { text: '全部租金', value: '' },
  { text: '2000元以下', value: '0-2000' },
  { text: '2000-3000元', value: '2000-3000' },
  { text: '3000元以上', value: '3000-' }
]
// 房源状态筛选：选「仅看可租」即过滤掉已租出的房源
const statusOptions = [
  { text: '全部状态', value: '' },
  { text: '仅看可租', value: '可租' },
  { text: '仅看已租', value: '已租' }
]

// 解析租金区间筛选条件（0-2000 / 2000-3000 / 3000-）
function rentParams() {
  if (!filters.rentRange) {
    return {}
  }
  const [min, max] = filters.rentRange.split('-')
  return max ? { minRent: Number(min), maxRent: Number(max) } : { minRent: Number(min) }
}

// 加载房源列表
async function load() {
  const data = await getApartments({
    page: 1,
    size: 100,
    layout: filters.layout || undefined,
    status: filters.status || undefined,
    ...rentParams()
  })
  list.value = data.records || []
}

// 下拉刷新 / 筛选变化后重新加载
async function reload() {
  try {
    await load()
  } finally {
    refreshing.value = false
  }
}

// 跳转房源详情
function goDetail(id) {
  router.push(`/detail/${id}`)
}

// 取逗号分隔图片串的第一张
function firstImage(images) {
  return images ? images.split(',')[0] : ''
}

onMounted(load)
</script>

<style scoped>
.home-page {
  min-height: 100%;
  background: var(--paper);
}

/* ---- 品牌头图 ---- */
.hero {
  padding: 18px 16px 22px;
  background: var(--ink);
  color: #fff;
  border-radius: 0 0 var(--r-xl) var(--r-xl);
}
.hero-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand {
  display: flex;
  align-items: center;
  gap: 7px;
}
.roof-mark {
  color: #fff;
}
.brand-name {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.4px;
}
.hero-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.62);
}
.hero-count b {
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  margin-right: 2px;
}
.hero-title {
  margin-top: 20px;
  font-size: 23px;
  font-weight: 800;
  letter-spacing: 0.4px;
  line-height: 1.25;
}
.hero-sub {
  margin-top: 6px;
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 0.3px;
}

/* ---- 筛选 ---- */
.filter-wrap {
  margin: -12px 12px 0;
  border-radius: var(--r);
  overflow: hidden;
  box-shadow: var(--sh-1);
  position: relative;
  z-index: 2;
}
.filter-wrap :deep(.van-dropdown-menu__bar) {
  box-shadow: none;
  height: 46px;
  background: var(--surface);
}
.filter-wrap :deep(.van-dropdown-menu__title) {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink-2);
}

/* ---- 房源列表 ---- */
.apt-list {
  padding: 14px 12px 4px;
}
.apt-card {
  display: flex;
  background: var(--surface);
  border: 1px solid var(--line-2);
  border-radius: var(--r);
  overflow: hidden;
  margin-bottom: 12px;
  box-shadow: var(--sh-1);
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}
.apt-card:active {
  transform: scale(0.985);
  box-shadow: var(--sh-2);
}
.apt-media {
  position: relative;
  flex-shrink: 0;
}
.apt-img {
  width: 128px;
  height: 104px;
  display: block;
}
.apt-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 999px;
  color: #fff;
}
.apt-badge.is-free {
  background: rgba(18, 165, 148, 0.92);
}
.apt-badge.is-taken {
  background: rgba(15, 27, 45, 0.72);
}
.apt-body {
  flex: 1;
  min-width: 0;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
}
.apt-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0.2px;
}
.apt-meta {
  margin-top: 3px;
  font-size: 12px;
  color: var(--muted);
}
.apt-addr {
  margin-top: 5px;
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11.5px;
  color: var(--faint);
}
.apt-addr svg {
  flex-shrink: 0;
  color: var(--faint);
}
.apt-foot {
  margin-top: auto;
  padding-top: 8px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}
.apt-rent {
  color: var(--amber);
  font-size: 19px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.2px;
}
.apt-rent i {
  font-style: normal;
  font-size: 12px;
  font-weight: 700;
  margin-right: 1px;
}
.apt-rent em {
  font-style: normal;
  font-size: 11px;
  font-weight: 500;
  color: var(--faint);
  margin-left: 2px;
}
.apt-go {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--brand);
  background: var(--brand-050);
  padding: 3px 10px;
  border-radius: 999px;
}
</style>
