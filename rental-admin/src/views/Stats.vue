<template>
  <div class="stats-page">
    <!-- 工具栏 -->
    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <div>
          <div class="page-head-title">业务数据总览</div>
          <div class="page-head-sub">实时统计平台用户、房源与预约情况，点击卡片可查看明细</div>
        </div>
        <el-button @click="load"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>
    </el-card>

    <!-- 统计卡片（全部可点击下钻） -->
    <el-row :gutter="16" v-loading="loading" class="stat-grid">
      <el-col :xs="24" :sm="12" :md="8" v-for="item in cards" :key="item.label">
        <el-card shadow="hover" class="stat-card" @click="handleCard(item)">
          <span class="rail" :style="{ background: item.color }"></span>
          <div class="stat-top">
            <span class="stat-icon" :style="{ background: item.bg, color: item.color }">
              <el-icon :size="19"><component :is="item.icon" /></el-icon>
            </span>
            <span class="stat-label">{{ item.label }}</span>
            <span class="stat-go">
              <el-icon :size="12"><ArrowRight /></el-icon>
            </span>
          </div>
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-hint">{{ item.hint }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 预约明细抽屉（预约量卡片下钻） -->
    <el-drawer v-model="detailVisible" :title="detailTitle" size="640px">
      <div class="detail-tip">
        <el-icon :size="13"><InfoFilled /></el-icon>
        统计口径与卡片一致：仅统计未取消的预约记录
      </div>
      <el-table v-if="detailList.length" :data="detailList" v-loading="detailLoading">
        <el-table-column prop="apartmentTitle" label="房源" min-width="170" show-overflow-tooltip />
        <el-table-column prop="username" label="预约用户" width="110" />
        <el-table-column label="月租金" width="100">
          <template #default="{ row }"><span class="price">¥{{ row.rent }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <el-tag :type="row.status === '已成交' ? 'success' : 'primary'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="预约时间" min-width="160" />
      </el-table>
      <el-empty v-else-if="!detailLoading" description="该时间段暂无预约记录" />
      <el-pagination v-if="detailTotal > detailQuery.size" class="detail-pager" background
                     v-model:current-page="detailQuery.page" :page-size="detailQuery.size"
                     :total="detailTotal" layout="total, prev, pager, next" @current-change="loadDetail" />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getStatsBookings, getStatsOverview } from '../api'

const router = useRouter()
const loading = ref(false)
const stats = ref(null)

// 统计卡片配置：值取自后端统计接口；action 决定点击后的下钻方式
const cards = computed(() => [
  {
    label: '注册用户总数', value: stats.value?.userCount ?? 0, icon: 'User',
    color: 'var(--brand)', bg: 'var(--brand-050)', hint: '查看注册用户列表',
    action: { type: 'route', path: '/users' }
  },
  {
    label: '房源总数', value: stats.value?.apartmentCount ?? 0, icon: 'OfficeBuilding',
    color: '#6d5ae6', bg: '#edebfc', hint: '查看全部房源',
    action: { type: 'route', path: '/apartments' }
  },
  {
    label: '已租房源', value: stats.value?.rentedCount ?? 0, icon: 'House',
    color: 'var(--teal)', bg: 'var(--teal-100)', hint: '查看已租房源',
    action: { type: 'route', path: '/apartments', query: { status: '已租' } }
  },
  {
    label: '空置房源', value: stats.value?.vacantCount ?? 0, icon: 'HomeFilled',
    color: 'var(--amber)', bg: 'var(--amber-100)', hint: '查看可租房源',
    action: { type: 'route', path: '/apartments', query: { status: '可租' } }
  },
  {
    label: '今日预约量', value: stats.value?.todayBookings ?? 0, icon: 'Calendar',
    color: 'var(--brand)', bg: 'var(--brand-050)', hint: '查看今日预约明细',
    action: { type: 'detail', range: 'today' }
  },
  {
    label: '本周预约量', value: stats.value?.weekBookings ?? 0, icon: 'TrendCharts',
    color: 'var(--teal)', bg: 'var(--teal-100)', hint: '查看本周预约明细',
    action: { type: 'detail', range: 'week' }
  }
])

// ---- 预约明细抽屉 ----
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailList = ref([])
const detailTotal = ref(0)
const detailRange = ref('today')
const detailQuery = reactive({ page: 1, size: 10 })

const detailTitle = computed(() => (detailRange.value === 'week' ? '本周预约明细' : '今日预约明细'))

// 加载统计总览
async function load() {
  loading.value = true
  try {
    stats.value = await getStatsOverview()
  } finally {
    loading.value = false
  }
}

// 卡片点击：跳转对应页面，或打开明细抽屉
function handleCard(item) {
  const action = item.action
  if (action.type === 'route') {
    router.push({ path: action.path, query: action.query })
  } else {
    openDetail(action.range)
  }
}

// 打开预约明细（每次重置到第一页）
async function openDetail(range) {
  detailRange.value = range
  detailQuery.page = 1
  detailVisible.value = true
  await loadDetail()
}

async function loadDetail() {
  detailLoading.value = true
  try {
    const data = await getStatsBookings({
      range: detailRange.value,
      page: detailQuery.page,
      size: detailQuery.size
    })
    detailList.value = data.records || []
    detailTotal.value = Number(data.total) || 0
  } finally {
    detailLoading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-head-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--ink);
  letter-spacing: 0.3px;
}
.page-head-sub {
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--muted);
}

.stat-grid {
  row-gap: 16px;
}
.stat-card {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.stat-card :deep(.el-card__body) {
  padding: 20px;
}
.stat-card:hover {
  transform: translateY(-2px);
}
.stat-card:hover .stat-go {
  opacity: 1;
  background: var(--brand);
  color: #fff;
}
.rail {
  position: absolute;
  top: 0;
  left: 20px;
  width: 34px;
  height: 3px;
  border-radius: 0 0 3px 3px;
}
.stat-top {
  display: flex;
  align-items: center;
  gap: 10px;
}
.stat-icon {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 11px;
  flex-shrink: 0;
}
.stat-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
}
.stat-go {
  margin-left: auto;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--brand-050);
  color: var(--brand);
  opacity: 0.6;
  transition: all 0.18s ease;
}
.stat-value {
  margin-top: 16px;
  font-size: 32px;
  font-weight: 800;
  color: var(--ink);
  line-height: 1;
  letter-spacing: -0.5px;
}
.stat-hint {
  margin-top: 10px;
  font-size: 12px;
  color: var(--faint);
}

/* ---- 明细抽屉 ---- */
.detail-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
  padding: 9px 12px;
  border-radius: var(--r-sm);
  background: var(--brand-050);
  color: var(--muted);
  font-size: 12.5px;
}
.detail-pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.price {
  color: var(--amber);
  font-weight: 800;
}
</style>
