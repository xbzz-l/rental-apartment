<template>
  <div class="dashboard">
    <!-- 顶部栏 -->
    <header class="topbar">
      <div class="topbar-brand">
        <span class="logo-badge">
          <svg viewBox="0 0 24 24" width="19" height="19" fill="none" aria-hidden="true">
            <path d="M3 11.4 12 4l9 7.4" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M6.2 10.6V20h11.6v-9.4" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </span>
        <div class="topbar-text">
          <b>公寓租房 · BI 数据看板</b>
          <i>实时监控房源规模与营业额数据</i>
        </div>
      </div>
      <div class="topbar-actions">
        <label class="auto-refresh">
          <el-switch v-model="autoRefresh" />
          <span>定时刷新 · 30秒</span>
        </label>
        <el-button @click="handleLogout">退出登录</el-button>
      </div>
    </header>

    <!-- KPI 卡片 -->
    <el-row :gutter="16" class="kpi-row" v-loading="loading">
      <el-col :xs="24" :sm="8" v-for="item in kpiCards" :key="item.label">
        <el-card shadow="hover" class="kpi-card">
          <span class="rail" :style="{ background: item.color }"></span>
          <div class="kpi-blocks" :class="{ 'is-split': item.blocks.length > 1 }">
            <div v-for="b in item.blocks" :key="b.label" class="kpi-block">
              <div class="kpi-label">{{ b.label }}</div>
              <div class="kpi-value" :style="{ color: b.color }">{{ b.value }}</div>
            </div>
          </div>
          <div class="kpi-sub" v-html="item.sub"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 月度营业额对比 -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="chart-head">
          <span>月度营业额对比</span>
          <span class="chart-head-sub">近 6 个月 · 按成交房源的月租金合计（不含已取消）</span>
        </div>
      </template>
      <div ref="revenueRef" class="chart chart-lg"></div>
    </el-card>

    <!-- 占比图 + 每日营业额 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-head"><span>房源占比</span></div>
          </template>
          <div ref="pieRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-head">
              <span>每日营业额</span>
              <span class="chart-head-sub">近 7 天</span>
            </div>
          </template>
          <div ref="dailyRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { getBiStats } from '../api'
import { logout } from '../store/auth'

// ECharts 统一字体与配色（与三端设计令牌一致）
const FONT = '"Plus Jakarta Sans", -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif'
const C = { brand: '#2a4fd7', teal: '#12a594', amber: '#e8742c', ink: '#0f1b2d', muted: '#6b7787', line: '#eff1f5' }
const TOOLTIP = {
  backgroundColor: '#fff',
  borderColor: '#e5e8ee',
  borderWidth: 1,
  padding: [8, 12],
  textStyle: { color: '#2a3648', fontSize: 12.5, fontFamily: FONT },
  extraCssText: 'box-shadow:0 6px 20px -6px rgba(16,24,40,.18); border-radius:10px;'
}
const AXIS_LABEL = { color: C.muted, fontSize: 11.5, fontFamily: FONT }
const SPLIT_LINE = { lineStyle: { color: C.line } }

/** 金额格式化：¥12,345 */
const money = (v) => '¥' + Number(v || 0).toLocaleString('zh-CN')
/** 坐标轴金额缩写：>=1万显示「x.x万」 */
const axisMoney = (v) => (v >= 10000 ? (v / 10000).toFixed(v % 10000 === 0 ? 0 : 1) + '万' : v)

const loading = ref(false)
const stats = ref(null)
const autoRefresh = ref(false)

const revenueRef = ref()
const pieRef = ref()
const dailyRef = ref()
let revenueChart
let pieChart
let dailyChart
let timer = null

// 当月营业额副行：仅保留环比上月（当日营业额已提到主数值行）
const growthSub = computed(() => {
  const d = stats.value
  if (!d) {
    return ''
  }
  const g = d.monthGrowth
  if (g === null || g === undefined) {
    return `<span class="flat">${d.lastMonthRevenue > 0 ? '环比上月 —' : '上月无营业额'}</span>`
  }
  if (g > 0) {
    return `<span class="up">环比上月 ↑${g}%</span>`
  }
  if (g < 0) {
    return `<span class="down">环比上月 ↓${Math.abs(g)}%</span>`
  }
  return '<span class="flat">环比上月持平</span>'
})

// KPI 卡片数据：房源规模 + 营业额（当月/当日并列，结构一致的「标签 + 金额」区块）
const kpiCards = computed(() => [
  {
    color: C.brand,
    blocks: [{ label: '总房源数', value: stats.value?.totalApartments ?? 0, color: C.brand }],
    sub: '平台在册房源'
  },
  {
    color: C.amber,
    blocks: [
      { label: '当月营业额', value: money(stats.value?.monthRevenue), color: C.amber },
      { label: '当日营业额', value: money(stats.value?.dayRevenue), color: C.teal }
    ],
    sub: growthSub.value
  },
  {
    color: C.teal,
    blocks: [{ label: '待租房源数', value: stats.value?.vacantCount ?? 0, color: C.teal }],
    sub: '当前可预约'
  }
])

// 加载 BI 统计数据并渲染图表
async function load() {
  loading.value = true
  try {
    stats.value = await getBiStats()
    renderCharts()
  } finally {
    loading.value = false
  }
}

// 渲染三张图表
function renderCharts() {
  const data = stats.value
  if (!data) {
    return
  }
  const trend = data.revenueTrend || []
  const lastIndex = trend.length - 1

  // 月度营业额对比柱状图（当月高亮，便于与上月及其他月份对比）
  revenueChart.setOption({
    tooltip: {
      trigger: 'axis',
      ...TOOLTIP,
      formatter: (params) => `${params[0].axisValue}<br/>营业额 <b>${money(params[0].value)}</b>`
    },
    grid: { left: 62, right: 26, top: 42, bottom: 32 },
    xAxis: {
      type: 'category',
      data: trend.map((i) => i.month),
      axisLabel: AXIS_LABEL,
      axisLine: { lineStyle: { color: C.line } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: { ...AXIS_LABEL, formatter: axisMoney },
      splitLine: SPLIT_LINE
    },
    series: [{
      name: '营业额',
      type: 'bar',
      barMaxWidth: 58,
      data: trend.map((i, idx) => ({
        value: Number(i.amount) || 0,
        itemStyle: {
          color: idx === lastIndex ? C.amber : 'rgba(42, 79, 215, 0.26)',
          borderRadius: [6, 6, 0, 0]
        }
      })),
      label: {
        show: true,
        position: 'top',
        formatter: (p) => (p.value ? money(p.value) : ''),
        color: C.ink,
        fontSize: 11.5,
        fontWeight: 700,
        fontFamily: FONT
      }
    }]
  })

  // 房源占比饼图（中心显示总套数）
  const pieTotal = (data.statusPie || []).reduce((sum, i) => sum + Number(i.value || 0), 0)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}：{c} 套（{d}%）', ...TOOLTIP },
    legend: {
      bottom: 0,
      icon: 'circle',
      itemWidth: 9,
      itemHeight: 9,
      textStyle: { color: C.muted, fontSize: 12, fontFamily: FONT }
    },
    title: {
      text: String(pieTotal),
      subtext: '总房源',
      left: '50%',
      top: '36%',
      textAlign: 'center',
      textStyle: { fontSize: 26, fontWeight: 800, color: C.ink, fontFamily: FONT, lineHeight: 30 },
      subtextStyle: { fontSize: 11.5, color: C.muted, fontFamily: FONT }
    },
    color: [C.brand, C.amber],
    series: [{
      name: '房源占比',
      type: 'pie',
      radius: ['50%', '70%'],
      center: ['50%', '46%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 4 },
      label: {
        formatter: '{b}\n{c} 套',
        color: C.ink,
        fontSize: 12,
        fontWeight: 600,
        fontFamily: FONT,
        lineHeight: 17
      },
      labelLine: { length: 10, length2: 10, lineStyle: { color: C.line } },
      data: data.statusPie
    }]
  })

  // 每日营业额柱状图
  const daily = data.dailyRevenue || []
  dailyChart.setOption({
    tooltip: {
      trigger: 'axis',
      ...TOOLTIP,
      formatter: (params) => `${params[0].axisValue}<br/>营业额 <b>${money(params[0].value)}</b>`
    },
    grid: { left: 58, right: 26, top: 32, bottom: 32 },
    xAxis: {
      type: 'category',
      data: daily.map((i) => i.day),
      axisLabel: AXIS_LABEL,
      axisLine: { lineStyle: { color: C.line } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: { ...AXIS_LABEL, formatter: axisMoney },
      splitLine: SPLIT_LINE
    },
    series: [{
      name: '营业额',
      type: 'bar',
      barMaxWidth: 32,
      data: daily.map((i) => Number(i.amount) || 0),
      itemStyle: { color: C.teal, borderRadius: [5, 5, 0, 0] },
      label: {
        show: true,
        position: 'top',
        formatter: (p) => (p.value ? money(p.value) : ''),
        color: C.muted,
        fontSize: 11,
        fontFamily: FONT
      }
    }]
  })
}

// 自动刷新开关：开启后每 30 秒重新加载
watch(autoRefresh, (val) => {
  clearInterval(timer)
  if (val) {
    timer = setInterval(load, 30000)
  }
})

// 退出登录：清空登录态后 App 自动切回登录页
function handleLogout() {
  logout()
}

// 窗口缩放时自适应图表尺寸
function handleResize() {
  revenueChart?.resize()
  pieChart?.resize()
  dailyChart?.resize()
}

onMounted(() => {
  revenueChart = echarts.init(revenueRef.value)
  pieChart = echarts.init(pieRef.value)
  dailyChart = echarts.init(dailyRef.value)
  window.addEventListener('resize', handleResize)
  load()
})

onBeforeUnmount(() => {
  clearInterval(timer)
  window.removeEventListener('resize', handleResize)
  revenueChart?.dispose()
  pieChart?.dispose()
  dailyChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  padding: 20px 22px 28px;
  background: var(--paper);
}

/* ---- 顶部栏 ---- */
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding: 16px 20px;
  background: var(--ink);
  border-radius: var(--r-lg);
  box-shadow: var(--sh-2);
}
.topbar-brand {
  display: flex;
  align-items: center;
  gap: 13px;
  min-width: 0;
}
.logo-badge {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: var(--brand);
  color: #fff;
  box-shadow: var(--sh-brand);
}
.topbar-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
  min-width: 0;
}
.topbar-text b {
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.4px;
}
.topbar-text i {
  font-style: normal;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}
.topbar-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}
.auto-refresh {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.72);
}

/* ---- KPI ---- */
.kpi-row {
  row-gap: 16px;
  margin-bottom: 16px;
}
.kpi-card {
  position: relative;
  overflow: hidden;
}
.kpi-card :deep(.el-card__body) {
  padding: 18px 20px 20px;
}
.rail {
  position: absolute;
  top: 0;
  left: 20px;
  width: 40px;
  height: 3px;
  border-radius: 0 0 3px 3px;
}
.kpi-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
}
.kpi-blocks {
  display: flex;
  align-items: flex-start;
  gap: 34px;
  margin-top: 12px;
}
.kpi-block {
  flex: 1 1 0;
  min-width: 0;
}
.kpi-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
  white-space: nowrap;
}
.kpi-value {
  margin-top: 11px;
  font-size: 34px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.6px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.kpi-sub {
  margin-top: 11px;
  font-size: 12px;
  color: var(--faint);
}
.kpi-sub :deep(.up) {
  color: var(--teal);
  font-weight: 700;
}
.kpi-sub :deep(.down) {
  color: var(--danger);
  font-weight: 700;
}
.kpi-sub :deep(.flat) {
  color: var(--faint);
}

/* ---- 图表 ---- */
.chart-card {
  margin-bottom: 16px;
}
.chart-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.chart-head-sub {
  font-size: 12px;
  font-weight: 500;
  color: var(--faint);
}
.chart {
  height: 288px;
}
.chart-lg {
  height: 320px;
}
.chart-row {
  row-gap: 16px;
}

@media (max-width: 768px) {
  .dashboard {
    padding: 14px;
  }
  .topbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
