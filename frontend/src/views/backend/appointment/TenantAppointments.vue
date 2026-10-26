<template>
  <div class="tenant-appointments">
    <!-- 搜索区域 -->
    <div class="search-container">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="房源名称">
          <el-input v-model="searchForm.houseName" placeholder="请输入房源名称" clearable />
        </el-form-item>
        <el-form-item label="预约状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="待确认" :value="0" />
            <el-option label="已确认" :value="1" />
            <el-option label="已拒绝" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
    >
      <el-table-column prop="id" label="预约ID" width="80" />
      <el-table-column label="房源信息" min-width="200">
        <template #default="scope">
          <div v-if="scope.row.house">
            <div style="font-weight: bold;">{{ scope.row.house.name }}</div>
            <div style="font-size: 12px; color: #999;">{{ scope.row.house.address }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="房东" width="120">
        <template #default="scope">
          <div v-if="scope.row.landlord">{{ scope.row.landlord.name || scope.row.landlord.username }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="appointmentDate" label="预约日期" width="120" />
      <el-table-column prop="appointmentTime" label="预约时间" width="150" />
      <el-table-column prop="contactPhone" label="联系电话" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="rejectReason" label="拒绝原因" min-width="150" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <el-button type="primary" link @click="handleViewDetail(scope.row)">详情</el-button>
          <el-popconfirm
            title="确定取消该预约吗？"
            @confirm="handleCancel(scope.row.id)"
            v-if="scope.row.status === 0 || scope.row.status === 1"
          >
            <template #reference>
              <el-button type="danger" link>取消预约</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-container">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 详情对话框 -->
    <el-dialog
      title="预约详情"
      v-model="detailDialogVisible"
      width="700px"
    >
      <el-descriptions :column="2" border v-if="currentAppointment">
        <el-descriptions-item label="预约ID">{{ currentAppointment.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentAppointment.status)">
            {{ getStatusText(currentAppointment.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="房源名称" :span="2" v-if="currentAppointment.house">
          {{ currentAppointment.house.name }}
        </el-descriptions-item>
        <el-descriptions-item label="房源地址" :span="2" v-if="currentAppointment.house">
          {{ currentAppointment.house.address }}
        </el-descriptions-item>
        <el-descriptions-item label="房东" v-if="currentAppointment.landlord">
          {{ currentAppointment.landlord.name || currentAppointment.landlord.username }}
        </el-descriptions-item>
        <el-descriptions-item label="房东电话" v-if="currentAppointment.landlord">
          {{ currentAppointment.landlord.phone }}
        </el-descriptions-item>
        <el-descriptions-item label="预约日期">{{ currentAppointment.appointmentDate }}</el-descriptions-item>
        <el-descriptions-item label="预约时间">{{ currentAppointment.appointmentTime }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentAppointment.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="留言信息" :span="2">
          {{ currentAppointment.message || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="拒绝原因" :span="2" v-if="currentAppointment.rejectReason">
          {{ currentAppointment.rejectReason }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentAppointment.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

// 表格数据
const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 搜索表单
const searchForm = reactive({
  houseName: '',
  status: null
})

// 详情对话框
const detailDialogVisible = ref(false)
const currentAppointment = ref(null)

// 初始化
onMounted(() => {
  fetchAppointments()
})

// 获取预约列表
const fetchAppointments = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value,
      status: searchForm.status !== null && searchForm.status !== undefined && searchForm.status !== '' ? searchForm.status : undefined,
      houseName: searchForm.houseName || undefined
    }

    const res = await request.get('/appointments/tenant', params)

    tableData.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取预约列表失败:', error)
    ElMessage.error('获取预约列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchAppointments()
}

// 重置搜索
const resetSearch = () => {
  searchForm.houseName = ''
  searchForm.status = null
  handleSearch()
}

// 取消预约
const handleCancel = async (id) => {
  try {
    await request.put(`/appointments/${id}/cancel`)
    ElMessage.success('取消预约成功')
    fetchAppointments()
  } catch (error) {
    console.error('取消预约失败:', error)
  }
}

// 查看详情
const handleViewDetail = (row) => {
  currentAppointment.value = row
  detailDialogVisible.value = true
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    0: 'warning',
    1: 'success',
    2: 'danger',
    3: 'info',
    4: 'info'
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const textMap = {
    0: '待确认',
    1: '已确认',
    2: '已拒绝',
    3: '已完成',
    4: '已取消'
  }
  return textMap[status] || '未知'
}

// 分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchAppointments()
}

// 页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchAppointments()
}
</script>

<style lang="scss" scoped>
.tenant-appointments {
  .search-container {
    margin-bottom: 20px;
    padding: 18px;
    background-color: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
