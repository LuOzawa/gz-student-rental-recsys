<template>
  <div class="transaction-management">
    <div class="search-container">
      <el-form :inline="true" :model="searchForm" class="demo-form-inline">
        <el-form-item label="交易类型">
          <el-select v-model="searchForm.type" placeholder="请选择类型" clearable>
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="用户名" v-if="isAdmin">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button type="success" @click="handleExport" :loading="exporting">
            <el-icon><Download /></el-icon>
            导出Excel
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table
      v-loading="loading"
      :data="tableData"
      stripe
      border
      style="width: 100%"
    >
      <el-table-column prop="id" label="交易ID" min-width="80" />
      <el-table-column label="订单编号" min-width="120">
        <template #default="scope">
          {{ scope.row.order?.orderNo || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="用户信息" min-width="120" v-if="isAdmin">
        <template #default="scope">
          {{ scope.row.user?.username || '-' }}<br>
          {{ scope.row.user?.name || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="交易类型" min-width="100">
        <template #default="scope">
          <el-tag :type="scope.row.type === 1 ? 'success' : 'danger'">
            {{ scope.row.type === 1 ? '收入' : '支出' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="交易金额" min-width="120">
        <template #default="scope">
          <span :class="scope.row.type === 1 ? 'income-amount' : 'expense-amount'">
            {{ scope.row.type === 1 ? '+' : '-' }} ¥ {{ scope.row.amount }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="交易描述" min-width="200" />
      <el-table-column label="交易时间" min-width="160">
        <template #default="scope">
          {{ DateUtils.formatDateTime(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="100" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="handleViewOrder(scope.row)">查看订单</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        @update:current-page="currentPage = $event"
        @update:page-size="pageSize = $event"
      />
    </div>

    <!-- 交易统计 -->
    <div class="statistics-container" v-if="showStatistics">
      <el-card class="statistics-card">
        <template #header>
          <div class="card-header">
            <span>交易统计</span>
            <el-radio-group v-model="periodType" size="small">
              <el-radio-button value="month">本月</el-radio-button>
              <el-radio-button value="year">本年</el-radio-button>
              <el-radio-button value="all">全部</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div class="statistics-content">
          <div class="stat-item income">
            <div class="stat-title">总收入</div>
            <div class="stat-value">¥ {{ statistics.totalIncome }}</div>
          </div>
          <div class="stat-item expense">
            <div class="stat-title">总支出</div>
            <div class="stat-value">¥ {{ statistics.totalExpense }}</div>
          </div>
          <div class="stat-item balance">
            <div class="stat-title">收支结余</div>
            <div class="stat-value" :class="statistics.balance >= 0 ? 'income-amount' : 'expense-amount'">
              ¥ {{ statistics.balance }}
            </div>
          </div>
          <div class="stat-item count">
            <div class="stat-title">交易笔数</div>
            <div class="stat-value">{{ statistics.totalCount }}</div>
          </div>
        </div>
      </el-card>
    </div>
    
    <!-- 添加订单详情对话框 -->
    <el-dialog v-model="orderDetailVisible" title="订单详情" width="700px" @closed="handleOrderDetailClosed">
      <el-descriptions v-if="currentOrder" :column="2" border>
        <el-descriptions-item label="订单编号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="getOrderStatusType(currentOrder.status)">
            {{ getOrderStatusText(currentOrder.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="房屋标题">{{ currentOrder.house?.title }}</el-descriptions-item>
        <el-descriptions-item label="房屋地址">{{ currentOrder.house?.address }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">¥ {{ currentOrder.amount }}</el-descriptions-item>
        <el-descriptions-item label="押金金额">¥ {{ currentOrder.deposit }}</el-descriptions-item>
        <el-descriptions-item label="租客姓名">{{ currentOrder.tenant?.name }}</el-descriptions-item>
        <el-descriptions-item label="租客电话">{{ currentOrder.tenant?.phone }}</el-descriptions-item>
        <el-descriptions-item label="房东姓名">{{ currentOrder.landlord?.name }}</el-descriptions-item>
        <el-descriptions-item label="房东电话">{{ currentOrder.landlord?.phone }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">
          {{ currentOrder.paymentTime ? DateUtils.formatDateTime(currentOrder.paymentTime) : '未支付' }}
        </el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ currentOrder.paymentMethod || '未支付' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ DateUtils.formatDateTime(currentOrder.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ DateUtils.formatDateTime(currentOrder.updateTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import request from '@/utils/request'
import DateUtils from '@/utils/dateUtils'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'

const router = useRouter()
const userStore = useUserStore()

// 用户权限判断
const isAdmin = computed(() => userStore.userInfo?.roleCode === 'ADMIN')

// 交易类型选项
const typeOptions = [
  { value: 1, label: '收入' },
  { value: 2, label: '支出' }
]

// 表格数据
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 搜索表单
const searchForm = reactive({
  type: '',
  username: ''
})

// 导出状态
const exporting = ref(false)

// 交易统计
const showStatistics = ref(true)
const periodType = ref('month')
const statistics = reactive({
  totalIncome: 0,
  totalExpense: 0,
  balance: 0,
  totalCount: 0,
  incomeCount: 0,
  expenseCount: 0
})

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const url = isAdmin.value ? '/transactions/admin/statistics' : '/transactions/statistics'

    // 构建查询参数 - 包含筛选条件
    const params = {
      period: periodType.value,
      type: searchForm.type || undefined
    }

    // 管理员可以按用户名查询
    if (isAdmin.value && searchForm.username) {
      params.username = searchForm.username
    }

    await request.get(url, params, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        statistics.totalIncome = res.totalIncome || 0
        statistics.totalExpense = res.totalExpense || 0
        statistics.balance = res.balance || 0
        statistics.totalCount = res.totalCount || 0
        statistics.incomeCount = res.incomeCount || 0
        statistics.expenseCount = res.expenseCount || 0
      }
    })
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取交易记录
const fetchTransactions = async () => {
  loading.value = true
  try {
    // 根据用户角色获取不同的交易记录
    const url = isAdmin.value ? '/transactions/admin' : '/transactions'
    
    // 构建查询参数
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value,
      type: searchForm.type || undefined
    }
    
    // 管理员可以按用户名查询
    if (isAdmin.value && searchForm.username) {
      params.username = searchForm.username
    }
    
    await request.get(url, params, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        tableData.value = res.records||[]
        total.value = res.total||0
      }
    })
  } catch (error) {
    console.error('获取交易记录失败:', error)
  } finally {
    loading.value = false
  }
}

// 查询
const handleSearch = () => {
  currentPage.value = 1
  fetchTransactions()
  fetchStatistics()  // 同时刷新统计数据
}

// 重置搜索
const resetSearch = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  handleSearch()
}

// 订单状态选项
const orderStatusOptions = [
  { value: 0, label: '待支付' },
  { value: 1, label: '已支付待确认' },
  { value: 2, label: '已确认' },
  { value: 3, label: '已取消' },
  { value: 4, label: '已退款' }
]

// 获取订单状态文本
const getOrderStatusText = (status) => {
  const option = orderStatusOptions.find(item => item.value === status)
  return option ? option.label : '未知状态'
}

// 获取订单状态类型
const getOrderStatusType = (status) => {
  switch (status) {
    case 0: return 'info'    // 待支付
    case 1: return 'warning' // 已支付待确认
    case 2: return 'success' // 已确认
    case 3: return 'danger'  // 已取消
    case 4: return 'info'    // 已退款
    default: return 'info'
  }
}

// 订单详情对话框
const orderDetailVisible = ref(false)
const currentOrder = ref(null)

// 查看关联订单
const handleViewOrder = async (row) => {
  if (!row.orderId) {
    ElMessage.warning('没有关联订单信息')
    return
  }
  
  try {
    await request.get(`/orders/${row.orderId}`, {}, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        currentOrder.value = res
        orderDetailVisible.value = true
      }
    })
  } catch (error) {
    console.error('获取订单详情失败:', error)
    ElMessage.error('获取订单详情失败')
  }
}

// 订单详情对话框关闭处理函数
const handleOrderDetailClosed = () => {
  currentOrder.value = null
}

// 分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchTransactions()
}

// 页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchTransactions()
}

// 监听统计周期变化
watch(periodType, () => {
  fetchStatistics()
})

// 导出Excel
const handleExport = async () => {
  exporting.value = true
  try {
    // 根据用户角色选择不同的导出接口
    const url = isAdmin.value ? '/api/transactions/admin/export/excel' : '/api/transactions/export/excel'

    // 构建查询参数
    const params = {
      period: periodType.value,
      type: searchForm.type || undefined
    }

    // 管理员可以按用户名查询
    if (isAdmin.value && searchForm.username) {
      params.username = searchForm.username
    }

    const token = localStorage.getItem('token')

    // 构建查询字符串
    const queryString = Object.keys(params)
      .filter(key => params[key] !== undefined)
      .map(key => `${key}=${encodeURIComponent(params[key])}`)
      .join('&')

    const fullUrl = `${url}?${queryString}`

    // 使用fetch下载文件（通过代理）
    const response = await fetch(fullUrl, {
      method: 'GET',
      headers: {
        'token': token
      }
    })

    if (!response.ok) {
      throw new Error('导出失败')
    }

    // 获取文件blob
    const blob = await response.blob()

    // 创建下载链接
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl

    // 从响应头获取文件名，如果没有则使用默认名称
    const contentDisposition = response.headers.get('Content-Disposition')
    let fileName = `交易记录_${new Date().getTime()}.xlsx`
    if (contentDisposition) {
      const fileNameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (fileNameMatch && fileNameMatch[1]) {
        fileName = decodeURIComponent(fileNameMatch[1].replace(/['"]/g, ''))
      }
    }

    link.download = fileName
    document.body.appendChild(link)
    link.click()

    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)

    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

// 页面加载时获取交易记录和统计数据
onMounted(() => {
  fetchTransactions()
  fetchStatistics()
})
</script>

<style scoped>
.transaction-management {
  padding: 20px;
}

.search-container {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.income-amount {
  color: #67c23a;
  font-weight: bold;
}

.expense-amount {
  color: #f56c6c;
  font-weight: bold;
}

.statistics-container {
  margin-top: 20px;
}

.statistics-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.statistics-content {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 20px;
}

.stat-item {
  flex: 1;
  min-width: 200px;
  padding: 20px;
  border-radius: 4px;
  background-color: #f8fafc;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.stat-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 22px;
  font-weight: bold;
  color: #303133;
}

.income .stat-value {
  color: #67c23a;
}

.expense .stat-value {
  color: #f56c6c;
}

.balance .stat-value {
  color: #409eff;
}
</style> 