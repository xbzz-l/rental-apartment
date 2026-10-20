<template>
  <div class="users-page">
    <!-- 工具栏：搜索 + 用户总数 -->
    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <el-input v-model="query.keyword" placeholder="搜索账号 / 姓名" clearable style="width: 260px"
                  @keyup.enter="search" @clear="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="count-pill">
          <span class="count-label">注册用户总数</span>
          <span class="count-value">{{ userCount }}</span>
        </div>
      </div>
    </el-card>

    <!-- 用户表格 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-head">
          <span>用户列表</span>
          <span class="card-count">共 {{ total }} 位用户</span>
        </div>
      </template>
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="账号" min-width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="mini-avatar" :class="row.role === 'admin' ? 'is-admin' : ''">
                {{ (row.realName || row.username || '?').charAt(0) }}
              </span>
              <span class="cell-name">{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="姓名" width="120">
          <template #default="{ row }">
            <span v-if="row.realName">{{ row.realName }}</span>
            <span v-else class="cell-empty">未填写</span>
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="80">
          <template #default="{ row }">
            <span v-if="row.age !== null && row.age !== undefined">{{ row.age }}</span>
            <span v-else class="cell-empty">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="150">
          <template #default="{ row }">
            <span v-if="row.phone">{{ row.phone }}</span>
            <span v-else class="cell-empty">未填写</span>
          </template>
        </el-table-column>
        <el-table-column prop="cohabitants" label="同住人" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.cohabitants">{{ row.cohabitants }}</span>
            <span v-else class="cell-empty">无</span>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'primary'">{{ row.role === 'admin' ? '管理员' : '用户' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" min-width="150" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRecords(row)">租房记录</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" background v-model:current-page="query.page" :page-size="query.size"
                     :total="total" layout="total, prev, pager, next" @current-change="load" />
    </el-card>

    <!-- 租房记录抽屉 -->
    <el-drawer v-model="drawerVisible" :title="`${currentUser?.realName || currentUser?.username || ''} 的租房记录`" size="60%">
      <el-table :data="records" v-loading="recordsLoading">
        <el-table-column prop="apartmentTitle" label="房源" min-width="180" show-overflow-tooltip />
        <el-table-column label="月租金" width="110">
          <template #default="{ row }"><span class="price">¥{{ row.rent }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '已成交' ? 'success' : row.status === '已预约' ? 'primary' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="预约时间" min-width="170" />
      </el-table>
      <el-empty v-if="!recordsLoading && !records.length" description="该用户暂无租房记录" />
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteUser, getUserBookings, getUserCount, getUsers } from '../api'

const list = ref([])
const total = ref(0)
const userCount = ref(0)
const loading = ref(false)
const records = ref([])
const recordsLoading = ref(false)
const drawerVisible = ref(false)
const currentUser = ref(null)

const query = reactive({ page: 1, size: 10, keyword: '' })

// 加载用户列表与总数
async function load() {
  loading.value = true
  try {
    const data = await getUsers({ page: query.page, size: query.size, keyword: query.keyword || undefined })
    list.value = data.records || []
    total.value = Number(data.total) || 0
    userCount.value = await getUserCount()
  } finally {
    loading.value = false
  }
}

// 关键词搜索（重置页码）
function search() {
  query.page = 1
  load()
}

// 打开用户租房记录抽屉
async function openRecords(row) {
  currentUser.value = row
  drawerVisible.value = true
  recordsLoading.value = true
  records.value = []
  try {
    const data = await getUserBookings(row.id, { page: 1, size: 100 })
    records.value = data.records || []
  } finally {
    recordsLoading.value = false
  }
}

// 删除用户：二次确认后连同其租房记录一并清理，并释放不再被占用的房源
async function handleDelete(row) {
  const name = row.realName || row.username
  try {
    await ElMessageBox.confirm(
      `确认删除用户「${name}」（账号 ${row.username}）吗？该用户名下的租房记录将一并删除，占用的房源会被释放，操作不可恢复。`,
      '删除用户',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await deleteUser(row.id)
  ElMessage.success(`已删除用户「${name}」`)
  // 若当前页被删空，回退一页避免停留在空白页
  if (list.value.length === 1 && query.page > 1) {
    query.page -= 1
  }
  load()
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
  gap: 16px;
}
.count-pill {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 7px 16px;
  border-radius: 999px;
  background: var(--brand-050);
}
.count-label {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--muted);
}
.count-value {
  font-size: 20px;
  font-weight: 800;
  color: var(--brand);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-count {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--muted);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}
.mini-avatar {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--brand-100);
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
}
.mini-avatar.is-admin {
  background: var(--amber-100);
  color: var(--amber);
}
.cell-name {
  font-weight: 600;
  color: var(--ink-2);
}
.cell-empty {
  color: var(--muted);
  font-size: 12.5px;
}

.price {
  color: var(--amber);
  font-weight: 800;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
