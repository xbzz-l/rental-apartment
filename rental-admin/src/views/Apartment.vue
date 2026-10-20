<template>
  <div class="apartment-page">
    <!-- 工具栏：筛选 + 新增 -->
    <el-card shadow="never" class="toolbar">
      <div class="toolbar-row">
        <div class="filters">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="search">
            <el-option label="可租" value="可租" />
            <el-option label="已租" value="已租" />
          </el-select>
          <el-select v-model="query.layout" placeholder="全部户型" clearable style="width: 150px" @change="search">
            <el-option v-for="l in layouts" :key="l" :label="l" :value="l" />
          </el-select>
        </div>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>新增房源
        </el-button>
      </div>
    </el-card>

    <!-- 房源表格 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-head">
          <span>房源列表</span>
          <span class="card-count">共 {{ total }} 条</span>
        </div>
      </template>
      <el-table :data="list" v-loading="loading">
        <el-table-column label="图片" width="104">
          <template #default="{ row }">
            <el-image :src="firstImage(row.images)" fit="cover" class="row-img"
                      :preview-src-list="imageList(row)" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="layout" label="户型" width="110" />
        <el-table-column label="面积" width="90">
          <template #default="{ row }">{{ row.area }}㎡</template>
        </el-table-column>
        <el-table-column label="月租金" width="110">
          <template #default="{ row }"><span class="price">¥{{ row.rent }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="row.status === '可租' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="位置" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" background v-model:current-page="query.page" :page-size="query.size"
                     :total="total" layout="total, prev, pager, next" @current-change="load" />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑房源' : '新增房源'" width="560px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="如：阳光花园·温馨一居" />
        </el-form-item>
        <el-form-item label="户型">
          <el-select v-model="form.layout" placeholder="请选择户型" style="width: 100%">
            <el-option v-for="l in layouts" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="面积(㎡)" prop="area">
          <el-input-number v-model="form.area" :min="1" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="月租金(元)" prop="rent">
          <el-input-number v-model="form.rent" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="可租">可租</el-radio>
            <el-radio value="已租">已租</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="图片URL">
          <el-input v-model="form.images" type="textarea" :rows="2" placeholder="多个图片URL用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="配套设施">
          <el-input v-model="form.facilities" placeholder="如：空调,冰箱,洗衣机" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.address" placeholder="如：幸福市阳光区花园路1号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createApartment, deleteApartment, getApartments, updateApartment } from '../api'

const route = useRoute()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 10, status: '', layout: '' })
const layouts = ['开间', '一室一厅', '两室一厅', '两室两厅', '三室两厅', 'Loft']

const emptyForm = { id: null, title: '', layout: '', area: 50, rent: 2000, status: '可租', images: '', facilities: '', address: '' }
const form = reactive({ ...emptyForm })

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  area: [{ required: true, message: '请输入面积', trigger: 'blur' }],
  rent: [{ required: true, message: '请输入租金', trigger: 'blur' }]
}

// 加载房源列表
async function load() {
  loading.value = true
  try {
    const data = await getApartments({
      page: query.page,
      size: query.size,
      status: query.status || undefined,
      layout: query.layout || undefined
    })
    list.value = data.records || []
    total.value = Number(data.total) || 0
  } finally {
    loading.value = false
  }
}

// 筛选变化：重置页码后重新加载
function search() {
  query.page = 1
  load()
}

// 取逗号分隔图片串的第一张
function firstImage(images) {
  return images ? images.split(',')[0] : ''
}

// 图片预览列表
function imageList(row) {
  return row.images ? row.images.split(',') : []
}

// 打开新增弹窗
function openCreate() {
  Object.assign(form, emptyForm)
  dialogVisible.value = true
}

// 打开编辑弹窗（回填数据）
function openEdit(row) {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

// 保存新增/编辑
async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateApartment(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await createApartment(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

// 删除房源（二次确认）
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除房源「${row.title}」吗？关联租房记录将一并删除。`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  await deleteApartment(row.id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(() => {
  // 支持从业务统计卡片带状态筛选跳转（如 /apartments?status=已租）
  if (route.query.status) query.status = String(route.query.status)
  if (route.query.layout) query.layout = String(route.query.layout)
  load()
})

// 已在房源页时再次从统计卡片跳转：同步筛选条件
watch(() => route.query, (q) => {
  query.status = q.status ? String(q.status) : ''
  query.layout = q.layout ? String(q.layout) : ''
  query.page = 1
  load()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.filters {
  display: flex;
  gap: 12px;
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
.row-img {
  width: 84px;
  height: 58px;
  border-radius: 10px;
  display: block;
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
