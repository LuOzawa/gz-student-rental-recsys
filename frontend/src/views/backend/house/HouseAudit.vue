<template>
  <div class="house-audit">
    <!-- 搜索区域 -->
    <div class="search-container">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="房源标题">
          <el-input v-model="searchForm.title" placeholder="请输入房源标题" clearable />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="searchForm.auditStatus" placeholder="请选择审核状态" clearable>
            <el-option label="待审核" :value="0" />
            <el-option label="审核通过" :value="1" />
            <el-option label="审核拒绝" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 统计信息 -->
    <el-row :gutter="20" class="stats-container">
      <el-col :span="8">
        <el-card>
          <div class="stat-item">
            <div class="stat-label">待审核</div>
            <div class="stat-value pending">{{ pendingCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-item">
            <div class="stat-label">审核通过</div>
            <div class="stat-value success">{{ approvedCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-item">
            <div class="stat-label">审核拒绝</div>
            <div class="stat-value danger">{{ rejectedCount }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="房源标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="landlordName" label="房东" width="120" />
      <el-table-column prop="typeName" label="类型" width="120" />
      <el-table-column prop="area" label="面积(㎡)" width="100" />
      <el-table-column prop="price" label="租金(元/月)" width="120" />
      <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
      <el-table-column label="审核状态" width="120">
        <template #default="scope">
          <el-tag :type="getAuditStatusType(scope.row.auditStatus)">
            {{ getAuditStatusText(scope.row.auditStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditReason" label="拒绝原因" min-width="150" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="scope">
          <el-button
            type="success"
            link
            @click="handleApprove(scope.row)"
            :disabled="scope.row.auditStatus !== 0"
          >
            通过
          </el-button>
          <el-button
            type="danger"
            link
            @click="handleReject(scope.row)"
            :disabled="scope.row.auditStatus !== 0"
          >
            拒绝
          </el-button>
          <el-button type="primary" link @click="handleViewDetail(scope.row)">详情</el-button>
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

    <!-- 拒绝审核对话框 -->
    <el-dialog
      title="审核拒绝"
      v-model="rejectDialogVisible"
      width="500px"
    >
      <el-form
        ref="rejectFormRef"
        :model="rejectForm"
        :rules="rejectRules"
        label-width="100px"
      >
        <el-form-item label="拒绝原因" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="rejectDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitReject">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      title="房源详情"
      v-model="detailDialogVisible"
      width="900px"
    >
      <el-descriptions :column="2" border v-if="currentHouse">
        <el-descriptions-item label="房源ID">{{ currentHouse.id }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="getAuditStatusType(currentHouse.auditStatus)">
            {{ getAuditStatusText(currentHouse.auditStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="房源标题" :span="2">{{ currentHouse.title }}</el-descriptions-item>
        <el-descriptions-item label="房东">{{ currentHouse.landlordName }}</el-descriptions-item>
        <el-descriptions-item label="房屋类型">{{ currentHouse.typeName }}</el-descriptions-item>
        <el-descriptions-item label="面积">{{ currentHouse.area }} ㎡</el-descriptions-item>
        <el-descriptions-item label="租金">{{ currentHouse.price }} 元/月</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ currentHouse.address }}</el-descriptions-item>
        <el-descriptions-item label="房源描述" :span="2">
          <div style="white-space: pre-line;">{{ currentHouse.description }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="拒绝原因" :span="2" v-if="currentHouse.auditReason">
          {{ currentHouse.auditReason }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentHouse.createTime }}</el-descriptions-item>
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

// 统计数据
const pendingCount = ref(0)
const approvedCount = ref(0)
const rejectedCount = ref(0)

// 搜索表单
const searchForm = reactive({
  title: '',
  auditStatus: 0 // 默认显示待审核
})

// 拒绝对话框
const rejectDialogVisible = ref(false)
const rejectFormRef = ref(null)
const rejectForm = reactive({
  houseId: null,
  reason: ''
})
const rejectRules = {
  reason: [
    { required: true, message: '请输入拒绝原因', trigger: 'blur' },
    { min: 5, message: '拒绝原因至少5个字符', trigger: 'blur' }
  ]
}

// 详情对话框
const detailDialogVisible = ref(false)
const currentHouse = ref(null)

// 初始化
onMounted(() => {
  fetchHouses()
  fetchStatistics()
})

// 获取房源列表
const fetchHouses = async () => {
  loading.value = true
  try {
    const params = {
      title: searchForm.title,
      currentPage: currentPage.value,
      size: pageSize.value
    }

    if (searchForm.auditStatus !== null && searchForm.auditStatus !== undefined && searchForm.auditStatus !== '') {
      params.auditStatus = searchForm.auditStatus
    }

    const res = await request.get('/houses/admin/audit', params)
    tableData.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取房源列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取统计数据
const fetchStatistics = async () => {
  try {
    // 获取待审核数量
    const pendingRes = await request.get('/houses/admin/audit', {
      auditStatus: 0,
      currentPage: 1,
      size: 1
    })
    pendingCount.value = pendingRes.total || 0

    // 获取审核通过数量
    const approvedRes = await request.get('/houses/admin/audit', {
      auditStatus: 1,
      currentPage: 1,
      size: 1
    })
    approvedCount.value = approvedRes.total || 0

    // 获取审核拒绝数量
    const rejectedRes = await request.get('/houses/admin/audit', {
      auditStatus: 2,
      currentPage: 1,
      size: 1
    })
    rejectedCount.value = rejectedRes.total || 0
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchHouses()
}

// 重置搜索
const resetSearch = () => {
  searchForm.title = ''
  searchForm.auditStatus = 0
  handleSearch()
}

// 审核通过
const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定审核通过房源"${row.title}"吗？`,
      '审核通过',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success'
      }
    )

    await request.put(`/houses/${row.id}/approve`)
    ElMessage.success('审核通过成功')
    fetchHouses()
    fetchStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('审核通过失败:', error)
    }
  }
}

// 审核拒绝
const handleReject = (row) => {
  rejectForm.houseId = row.id
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

// 提交拒绝
const submitReject = () => {
  rejectFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await request.put(`/houses/${rejectForm.houseId}/reject`, null, {
          params: { reason: rejectForm.reason }
        })
        ElMessage.success('审核拒绝成功')
        rejectDialogVisible.value = false
        fetchHouses()
        fetchStatistics()
      } catch (error) {
        console.error('审核拒绝失败:', error)
      }
    }
  })
}

// 查看详情
const handleViewDetail = (row) => {
  currentHouse.value = row
  detailDialogVisible.value = true
}

// 获取审核状态类型
const getAuditStatusType = (status) => {
  const typeMap = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return typeMap[status] || 'info'
}

// 获取审核状态文本
const getAuditStatusText = (status) => {
  const textMap = {
    0: '待审核',
    1: '审核通过',
    2: '审核拒绝'
  }
  return textMap[status] || '未知'
}

// 分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchHouses()
}

// 页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchHouses()
}
</script>

<style lang="scss" scoped>
.house-audit {
  .search-container {
    margin-bottom: 20px;
    padding: 18px;
    background-color: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  }

  .stats-container {
    margin-bottom: 20px;

    .stat-item {
      text-align: center;
      padding: 10px 0;

      .stat-label {
        font-size: 14px;
        color: #999;
        margin-bottom: 10px;
      }

      .stat-value {
        font-size: 28px;
        font-weight: bold;

        &.pending {
          color: #E6A23C;
        }

        &.success {
          color: #67C23A;
        }

        &.danger {
          color: #F56C6C;
        }
      }
    }
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
