<template>
  <div class="profile-page">
    <van-nav-bar title="我的" fixed placeholder />

    <template v-if="userStore.isLogin && userStore.userInfo">
      <!-- 用户信息卡片 -->
      <div class="user-card">
        <svg class="card-watermark" viewBox="0 0 24 24" width="104" height="104" fill="none" aria-hidden="true">
          <path d="M3 11.4 12 4l9 7.4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
          <path d="M6.2 10.6V20h11.6v-9.4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <van-image round width="62" height="62" :src="userStore.userInfo.avatar || defaultAvatar" class="user-avatar" />
        <div class="user-meta">
          <div class="user-nickname">{{ displayName }}</div>
          <div class="user-phone">
            <svg viewBox="0 0 24 24" width="12" height="12" fill="none" aria-hidden="true">
              <path d="M6.5 3.5h3l1.5 4-2 1.5a12 12 0 0 0 6 6l1.5-2 4 1.5v3a2 2 0 0 1-2.2 2A16.5 16.5 0 0 1 4.5 5.7 2 2 0 0 1 6.5 3.5Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
            </svg>
            <span>{{ userStore.userInfo.phone || '未填联系电话' }}</span>
            <span class="user-account">账号 @{{ userStore.userInfo.username }}</span>
          </div>
        </div>
      </div>

      <div class="menu-card">
        <van-cell title="我的租房记录" is-link @click="openRecords">
          <template #icon>
            <svg class="cell-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" aria-hidden="true">
              <path d="M4 7h16v13H4z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
              <path d="M4 7l8-4 8 4" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
              <path d="M9 20v-6h6v6" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
            </svg>
          </template>
        </van-cell>
        <van-cell title="编辑资料" is-link @click="openEdit">
          <template #icon>
            <svg class="cell-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" aria-hidden="true">
              <path d="M4 20h4l10-10-4-4L4 16v4Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
              <path d="M13.5 6.5l4 4" stroke="currentColor" stroke-width="1.8" />
            </svg>
          </template>
        </van-cell>
      </div>

      <div class="logout-btn">
        <van-button round block plain type="danger" @click="handleLogout">退出登录</van-button>
      </div>
    </template>

    <!-- 未登录状态 -->
    <van-empty v-else description="登录后查看个人信息">
      <van-button round type="primary" class="login-btn" @click="router.push('/login')">去登录</van-button>
    </van-empty>

    <!-- 我的租房记录弹层 -->
    <van-popup v-model:show="recordsVisible" position="bottom" round :style="{ height: '60%' }">
      <div class="popup-handle"></div>
      <div class="popup-title">我的租房记录</div>
      <div v-if="records.length" class="record-list">
        <div v-for="r in records" :key="r.id" class="record-item">
          <van-image :src="r.apartmentImage" fit="cover" class="record-img" />
          <div class="record-info">
            <div class="record-title van-ellipsis">{{ r.apartmentTitle }}</div>
            <div class="record-sub"><b>¥{{ r.rent }}</b>/月 · {{ r.createTime }}</div>
          </div>
          <div class="record-side">
            <van-tag :type="statusTagType(r.status)" :class="{ 'tag-canceled': r.status === '已取消' }">
              {{ r.status }}
            </van-tag>
            <van-button v-if="r.status === '已预约'" size="mini" type="primary" class="cancel-btn"
                        @click="handleCancel(r)">取消预约</van-button>
          </div>
        </div>
      </div>
      <van-empty v-else description="暂无租房记录" />
    </van-popup>

    <!-- 编辑资料弹层 -->
    <van-popup v-model:show="editVisible" position="bottom" round>
      <div class="popup-handle"></div>
      <div class="popup-title">编辑资料</div>
      <div class="popup-desc">请填写真实信息，便于办理租房登记</div>
      <van-form @submit="handleUpdate">
        <van-cell-group inset>
          <van-field v-model="editForm.realName" label="姓名" placeholder="请输入真实姓名"
                     :rules="[{ required: true, message: '请输入真实姓名' }]" />
          <van-field v-model="editForm.age" type="digit" label="年龄" placeholder="请输入年龄"
                     :rules="[{ validator: checkAge, message: '请输入 1-120 之间的年龄' }]" />
          <van-field v-model="editForm.phone" type="tel" label="电话" placeholder="请输入联系电话"
                     :rules="[{ required: true, message: '请输入联系电话' }]" />
          <van-field v-model="editForm.cohabitants" type="textarea" rows="2" autosize label="同住人"
                     placeholder="如：李小红（配偶，13800000003）；多人用「、」分隔" />
        </van-cell-group>
        <div class="popup-submit">
          <van-button round block type="primary" native-type="submit" :loading="updating">保存</van-button>
        </div>
      </van-form>
    </van-popup>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast } from 'vant'
import { cancelBooking, myBookings } from '../api'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://picsum.photos/seed/avatar/120/120'
const records = ref([])
const recordsVisible = ref(false)
const editVisible = ref(false)
const updating = ref(false)
const editForm = reactive({ realName: '', age: '', phone: '', cohabitants: '' })

// 卡片主标题：优先真实姓名，未填写则显示账号（昵称已统一等于账号，不再单独参与展示）
const displayName = computed(() => {
  const info = userStore.userInfo || {}
  return info.realName || info.username || ''
})

// 年龄校验：允许留空，填了则需为 1-120 的整数
const checkAge = (val) => {
  if (val === '' || val === null || val === undefined) return true
  const n = Number(val)
  return Number.isInteger(n) && n >= 1 && n <= 120
}

// 加载个人信息与租房记录
async function load() {
  await userStore.fetchInfo()
  records.value = await myBookings()
}

// 打开租房记录弹层（确保数据最新）
async function openRecords() {
  recordsVisible.value = true
  records.value = await myBookings()
}

// 状态标签配色：已成交为绿，其余（已预约/已取消）走基础色，已取消再由样式置灰
const statusTagType = (status) => (status === '已成交' ? 'success' : 'primary')

// 取消预约：二次确认后释放房源并刷新列表
async function handleCancel(r) {
  try {
    await showConfirmDialog({
      title: '取消预约',
      message: `确认取消「${r.apartmentTitle}」的预约吗？取消后该房源将重新开放。`
    })
  } catch {
    return
  }
  await cancelBooking(r.id)
  showSuccessToast('已取消预约')
  records.value = await myBookings()
}

// 打开编辑弹层时回填当前信息
function openEdit() {
  const info = userStore.userInfo || {}
  editForm.realName = info.realName || ''
  editForm.age = info.age === null || info.age === undefined ? '' : String(info.age)
  editForm.phone = info.phone || ''
  editForm.cohabitants = info.cohabitants || ''
  editVisible.value = true
}

// 保存资料修改
async function handleUpdate() {
  updating.value = true
  try {
    await userStore.updateInfo({
      realName: editForm.realName,
      age: editForm.age === '' ? null : Number(editForm.age),
      phone: editForm.phone,
      cohabitants: editForm.cohabitants
    })
    showSuccessToast('保存成功')
    editVisible.value = false
  } finally {
    updating.value = false
  }
}

// 退出登录
async function handleLogout() {
  try {
    await showConfirmDialog({ title: '提示', message: '确认退出登录吗？' })
  } catch {
    return
  }
  userStore.logout()
  records.value = []
  router.replace('/')
}

onMounted(load)
</script>

<style scoped>
.profile-page {
  min-height: 100%;
  background: var(--paper);
}

/* ---- 用户卡片 ---- */
.user-card {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 12px;
  padding: 22px 18px;
  background: var(--ink);
  border-radius: var(--r-lg);
  color: #fff;
  box-shadow: var(--sh-2);
}
.card-watermark {
  position: absolute;
  right: -18px;
  bottom: -26px;
  color: rgba(255, 255, 255, 0.06);
}
.user-avatar {
  border: 2px solid rgba(255, 255, 255, 0.22);
  flex-shrink: 0;
}
.user-meta {
  min-width: 0;
}
.user-nickname {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.3px;
}
.user-phone {
  margin-top: 7px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px;
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.6);
}
.user-account {
  margin-left: 3px;
  padding-left: 9px;
  border-left: 1px solid rgba(255, 255, 255, 0.18);
  color: rgba(255, 255, 255, 0.42);
}

/* ---- 菜单 ---- */
.menu-card {
  margin: 0 12px;
  background: var(--surface);
  border-radius: var(--r-lg);
  overflow: hidden;
  box-shadow: var(--sh-1);
}
.cell-icon {
  margin-right: 10px;
  color: var(--brand);
}
.menu-card :deep(.van-cell__title) {
  font-weight: 600;
  color: var(--ink-2);
}

.logout-btn {
  margin: 24px 12px;
}
.logout-btn :deep(.van-button) {
  height: 44px;
  font-weight: 600;
  border-color: rgba(217, 74, 74, 0.4);
}

.login-btn {
  width: 132px;
}

/* ---- 弹层 ---- */
.popup-handle {
  width: 36px;
  height: 4px;
  border-radius: 999px;
  background: var(--line);
  margin: 10px auto 0;
}
.popup-title {
  padding: 14px 16px 10px;
  text-align: center;
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
}
.popup-desc {
  padding: 0 16px 14px;
  text-align: center;
  font-size: 12px;
  color: var(--faint);
}
.popup-submit {
  margin: 20px 16px 30px;
}
.popup-submit :deep(.van-button) {
  height: 44px;
  font-weight: 700;
}

.record-list {
  padding: 0 16px 30px;
  overflow-y: auto;
  max-height: calc(60vh - 60px);
}
.record-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--line-2);
}
.record-img {
  width: 68px;
  height: 52px;
  border-radius: 10px;
  flex-shrink: 0;
}
.record-info {
  flex: 1;
  min-width: 0;
}
.record-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
}
.record-sub {
  margin-top: 5px;
  font-size: 12px;
  color: var(--faint);
}
.record-sub b {
  color: var(--amber);
  font-size: 13.5px;
  font-weight: 800;
}
.record-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 9px;
  flex-shrink: 0;
}
.tag-canceled {
  background: var(--line-2) !important;
  color: var(--faint) !important;
}
/* 取消预约按钮：尺寸与上方状态标签完全一致，仅用中性灰底色区分「操作」与「状态」 */
.cancel-btn {
  height: 18px;
  padding: 0 9px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.2px;
  border-radius: 999px;
  border: none;
  background: var(--muted);
  color: #fff;
}
.cancel-btn:active {
  opacity: 0.82;
}
</style>
