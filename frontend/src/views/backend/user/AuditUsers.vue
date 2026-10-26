<template>
  <div class="audit-users">
    <!-- 搜索区域 -->
    <div class="search-container">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.roleCode" placeholder="请选择角色" clearable>
            <el-option label="租户" value="TENANT" />
            <el-option label="房东" value="LANDLORD" />
          </el-select>
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
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="name" label="姓名" min-width="100" />
      <el-table-column prop="roleCode" label="角色" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.roleCode === 'LANDLORD' ? 'success' : 'info'">
            {{ scope.row.roleCode === 'LANDLORD' ? '房东' : scope.row.roleCode === 'TENANT' ? '租户' : '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sex" label="性别" width="80">
        <template #default="scope">
          {{ scope.row.sex === 'male' ? '男' : scope.row.sex === 'female' ? '女' : '未知' }}
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="phone" label="手机号" width="120" />
      <el-table-column prop="idCard" label="身份证号" min-width="180" show-overflow-tooltip />
      <el-table-column label="审核状态" width="120">
        <template #default="scope">
          <el-tag
            :type="scope.row.auditStatus === 0 ? 'warning' : scope.row.auditStatus === 1 ? 'success' : 'danger'"
          >
            {{ scope.row.auditStatus === 0 ? '待审核' : scope.row.auditStatus === 1 ? '审核通过' : '审核拒绝' }}
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
        :page-sizes="[10, 20, 50, 100]"
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
      title="用户详情"
      v-model="detailDialogVisible"
      width="600px"
    >
      <el-descriptions :column="2" border v-if="currentUser">
        <el-descriptions-item label="用户ID">{{ currentUser.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ currentUser.name }}</el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ currentUser.sex === 'male' ? '男' : currentUser.sex === 'female' ? '女' : '未知' }}
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="currentUser.roleCode === 'LANDLORD' ? 'success' : 'info'">
            {{ currentUser.roleCode === 'LANDLORD' ? '房东' : '租户' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentUser.email }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentUser.phone }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ currentUser.idCard }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag
            :type="currentUser.auditStatus === 0 ? 'warning' : currentUser.auditStatus === 1 ? 'success' : 'danger'"
          >
            {{ currentUser.auditStatus === 0 ? '待审核' : currentUser.auditStatus === 1 ? '审核通过' : '审核拒绝' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="信用分" v-if="currentUser.roleCode === 'LANDLORD'">
          {{ currentUser.creditScore }}
        </el-descriptions-item>
        <el-descriptions-item label="拒绝原因" :span="2" v-if="currentUser.auditReason">
          {{ currentUser.auditReason }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentUser.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="2">{{ currentUser.updateTime }}</el-descriptions-item>
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
  username: '',
  name: '',
  roleCode: '',
  auditStatus: 0 // 默认显示待审核
})

// 拒绝对话框
const rejectDialogVisible = ref(false)
const rejectFormRef = ref(null)
const rejectForm = reactive({
  userId: null,
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
const currentUser = ref(null)

// 初始化
onMounted(() => {
  fetchUsers()
  fetchStatistics()
})

// 获取用户列表
const fetchUsers = async () => {
  loading.value = true
  try {
    const params = {
      username: searchForm.username,
      name: searchForm.name,
      roleCode: searchForm.roleCode,
      currentPage: currentPage.value,
      size: pageSize.value
    }

    // 如果选择了审核状态,添加到参数中(注意:auditStatus可以是0,所以不能简单用if判断)
    if (searchForm.auditStatus !== null && searchForm.auditStatus !== undefined && searchForm.auditStatus !== '') {
      params.auditStatus = searchForm.auditStatus
    }

    const res = await request.get('/user/page', params)
    tableData.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取统计数据
const fetchStatistics = async () => {
  try {
    // 获取待审核数量
    const pendingRes = await request.get('/user/page', {
      auditStatus: 0,
      currentPage: 1,
      size: 1
    })
    pendingCount.value = pendingRes.total || 0

    // 获取审核通过数量
    const approvedRes = await request.get('/user/page', {
      auditStatus: 1,
      currentPage: 1,
      size: 1
    })
    approvedCount.value = approvedRes.total || 0

    // 获取审核拒绝数量
    const rejectedRes = await request.get('/user/page', {
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
  fetchUsers()
}

// 重置搜索
const resetSearch = () => {
  searchForm.username = ''
  searchForm.name = ''
  searchForm.roleCode = ''
  searchForm.auditStatus = 0
  handleSearch()
}

// 审核通过
const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定审核通过用户"${row.username}"吗？`,
      '审核通过',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success'
      }
    )

    await request.put(`/user/approve/${row.id}`)
    ElMessage.success('审核通过成功')
    fetchUsers()
    fetchStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('审核通过失败:', error)
    }
  }
}

// 审核拒绝
const handleReject = (row) => {
  rejectForm.userId = row.id
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

// 提交拒绝
const submitReject = () => {
  rejectFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await request.put(`/user/reject/${rejectForm.userId}`, null, {
          params: { reason: rejectForm.reason }
        })
        ElMessage.success('审核拒绝成功')
        rejectDialogVisible.value = false
        fetchUsers()
        fetchStatistics()
      } catch (error) {
        console.error('审核拒绝失败:', error)
      }
    }
  })
}

// 查看详情
const handleViewDetail = (row) => {
  currentUser.value = row
  detailDialogVisible.value = true
}

// 分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchUsers()
}

// 页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchUsers()
}
</script>

<style lang="scss" scoped>
.audit-users {
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
