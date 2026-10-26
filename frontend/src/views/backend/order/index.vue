<template>
  <div class="order-management">
    <div class="search-container">
      <el-form :inline="true" :model="searchForm" class="demo-form-inline">
        <el-form-item label="订单编号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入订单编号" clearable />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="租客用户名" v-if="isAdmin">
          <el-input v-model="searchForm.tenantUsername" placeholder="请输入租客用户名" clearable />
        </el-form-item>
        <el-form-item label="房东用户名" v-if="isAdmin">
          <el-input v-model="searchForm.landlordUsername" placeholder="请输入房东用户名" clearable />
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
      <el-table-column prop="orderNo" label="订单编号" min-width="120" />
      <el-table-column label="房屋标题" min-width="150">
        <template #default="scope">
          {{ scope.row.house?.title || '未知房屋' }}
        </template>
      </el-table-column>
      <el-table-column label="租客信息" min-width="120">
        <template #default="scope">
          {{ scope.row.tenant?.username || '未知租客' }}<br>
          {{ scope.row.tenant?.name || '未知租客' }}<br>
          {{ scope.row.tenant?.phone || '未知电话' }}
        </template>
      </el-table-column>
      <el-table-column v-if="isAdmin" label="房东信息" min-width="120">
        <template #default="scope">
          {{ scope.row.landlord?.username || '未知房东' }}<br>
          {{ scope.row.landlord?.name || '未知房东' }}<br>
          {{ scope.row.landlord?.phone || '未知电话' }}
        </template>
      </el-table-column>
      <el-table-column prop="amount" label="订单金额" min-width="100" />
      <el-table-column prop="deposit" label="押金金额" min-width="100" />
      <el-table-column label="订单状态" min-width="120">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付时间" min-width="160">
        <template #default="scope">
          {{ scope.row.paymentTime ? DateUtils.formatDateTime(scope.row.paymentTime) : '未支付' }}
        </template>
      </el-table-column>
      <el-table-column label="支付方式" min-width="120">
        <template #default="scope">
          {{ scope.row.paymentMethod || '未支付' }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="160">
        <template #default="scope">
          {{ DateUtils.formatDateTime(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" min-width="200">
        <template #default="scope">
          <el-button size="small" @click="handleDetail(scope.row)">详情</el-button>
          <el-button 
            v-if="scope.row.status === 0" 
            size="small" 
            type="primary"
            @click="handlePay(scope.row)"
          >支付</el-button>
          <el-button
            v-if="scope.row.status === 1 && isLandlord"
            size="small"
            type="success"
            @click="handleConfirm(scope.row)"
          >确认</el-button>
          <el-button 
            v-if="scope.row.status === 0" 
            size="small" 
            type="danger"
            @click="handleCancel(scope.row)"
          >取消</el-button>
          <el-button 
            v-if="scope.row.status === 1 && isTenant" 
            size="small" 
            type="warning"
            @click="handleRefund(scope.row)"
          >退款</el-button>
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

    <!-- 支付对话框 -->
    <el-dialog v-model="payDialogVisible" title="订单支付" width="500px" @closed="handlePayDialogClosed">
      <el-form :model="payForm" label-width="120px">
        <el-form-item label="订单编号">
          <span>{{ payForm.orderNo }}</span>
        </el-form-item>
        <el-form-item label="房屋">
          <span>{{ payForm.houseTitle }}</span>
        </el-form-item>
        <el-form-item label="订单金额">
          <span class="price">¥ {{ payForm.amount }}</span>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-radio-group v-model="payForm.paymentMethod">
            <el-radio value="支付宝">支付宝</el-radio>
            <el-radio value="微信支付">微信支付</el-radio>
            <el-radio value="银行卡">银行卡</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="payDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmPay">确认支付</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="订单详情" width="700px" @closed="handleDetailDialogClosed">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单编号">{{ detailForm.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="getStatusType(detailForm.status)">
            {{ getStatusText(detailForm.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="房屋标题">{{ detailForm.house?.title }}</el-descriptions-item>
        <el-descriptions-item label="房屋地址">{{ detailForm.house?.address }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">¥ {{ detailForm.amount }}</el-descriptions-item>
        <el-descriptions-item label="押金金额">¥ {{ detailForm.deposit }}</el-descriptions-item>
        <el-descriptions-item label="租客姓名">{{ detailForm.tenant?.name }}</el-descriptions-item>
        <el-descriptions-item label="租客电话">{{ detailForm.tenant?.phone }}</el-descriptions-item>
        <el-descriptions-item label="房东姓名">{{ detailForm.landlord?.name }}</el-descriptions-item>
        <el-descriptions-item label="房东电话">{{ detailForm.landlord?.phone }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">
          {{ detailForm.paymentTime ? DateUtils.formatDateTime(detailForm.paymentTime) : '未支付' }}
        </el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ detailForm.paymentMethod || '未支付' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ DateUtils.formatDateTime(detailForm.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ DateUtils.formatDateTime(detailForm.updateTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import request from '@/utils/request'
import DateUtils from '@/utils/dateUtils'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

// 用户权限判断
const isAdmin = computed(() => userStore.userInfo?.roleCode === 'ADMIN')
const isLandlord = computed(() => userStore.userInfo?.roleCode === 'LANDLORD')
const isTenant = computed(() => userStore.userInfo?.roleCode === 'TENANT')

// 状态选项
const statusOptions = [
  { value: 0, label: '待支付' },
  { value: 1, label: '已支付待确认' },
  { value: 2, label: '已确认' },
  { value: 3, label: '已取消' },
  { value: 4, label: '已退款' }
]

// 获取状态文本
const getStatusText = (status) => {
  const option = statusOptions.find(item => item.value === status)
  return option ? option.label : '未知状态'
}

// 获取状态类型
const getStatusType = (status) => {
  switch (status) {
    case 0: return 'info'    // 待支付
    case 1: return 'warning' // 已支付待确认
    case 2: return 'success' // 已确认
    case 3: return 'danger'  // 已取消
    case 4: return 'info'    // 已退款
    default: return 'info'
  }
}

// 表格数据
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const exporting = ref(false)

// 搜索表单
const searchForm = reactive({
  orderNo: '',
  status: '',
  tenantUsername: '',
  landlordUsername: ''
})

// 支付对话框
const payDialogVisible = ref(false)
const payForm = reactive({
  id: '',
  orderNo: '',
  houseTitle: '',
  amount: 0,
  paymentMethod: '支付宝'
})

// 详情对话框
const detailDialogVisible = ref(false)
const detailForm = reactive({
  id: '',
  orderNo: '',
  status: 0,
  amount: 0,
  deposit: 0,
  house: {},
  tenant: {},
  landlord: {},
  paymentTime: '',
  paymentMethod: '',
  createTime: '',
  updateTime: ''
})

// 添加清理函数
const handleDetailDialogClosed = () => {
  // 清空详情表单
  Object.assign(detailForm, {
    id: '',
    orderNo: '',
    status: 0,
    amount: 0,
    deposit: 0,
    house: {},
    tenant: {},
    landlord: {},
    paymentTime: '',
    paymentMethod: '',
    createTime: '',
    updateTime: ''
  })
}

// 添加支付对话框关闭处理函数
const handlePayDialogClosed = () => {
  // 清空支付表单
  Object.assign(payForm, {
    id: '',
    orderNo: '',
    houseTitle: '',
    amount: 0,
    paymentMethod: '支付宝'
  })
}

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    // 根据用户角色获取不同的订单列表
    let url = '/orders'
    if (isAdmin.value) {
      url = '/orders/admin'
    } else if (isLandlord.value) {
      url = '/orders/landlord'
    } else if (isTenant.value) {
      url = '/orders/tenant'
    }

    // 构建查询参数
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value,
      orderNo: searchForm.orderNo || undefined,
      status: searchForm.status !== '' ? searchForm.status : undefined
    }

    // 管理员可以按租客用户名和房东用户名查询
    if (isAdmin.value) {
      params.tenantUsername = searchForm.tenantUsername || undefined
      params.landlordUsername = searchForm.landlordUsername || undefined
    }

    await request.get(url, params, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        tableData.value = res.records
        total.value = res.total
      }
    })
  } catch (error) {
    console.error('获取订单列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 查询
const handleSearch = () => {
  currentPage.value = 1
  fetchOrders()
}

// 重置搜索
const resetSearch = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = ''
  })
  handleSearch()
}

// 分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchOrders()
}

// 页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchOrders()
}

// 查看详情
const handleDetail = async (row) => {
  try {
    await request.get(`/orders/${row.id}`, {}, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        Object.assign(detailForm, res)
        detailDialogVisible.value = true
      }
    })
  } catch (error) {
    console.error('获取订单详情失败:', error)
  }
}

// 支付订单
const handlePay = (row) => {
  payForm.id = row.id
  payForm.orderNo = row.orderNo
  payForm.houseTitle = row.house?.title || '未知房屋'
  payForm.amount = row.amount
  payForm.paymentMethod = '支付宝'
  payDialogVisible.value = true
}

// 确认支付
const confirmPay = async () => {
  if (!payForm.paymentMethod) {
    ElMessage.warning('请选择支付方式')
    return
  }

  try {
    await request.put(`/orders/${payForm.id}/pay`, null, {
      params: {
        paymentMethod: payForm.paymentMethod
      },
      successMsg: '支付成功',
      onSuccess: () => {
        payDialogVisible.value = false
        fetchOrders()
      }
    })
  } catch (error) {
    console.error('支付订单失败:', error)
  }
}

// 确认订单
const handleConfirm = (row) => {
  ElMessageBox.confirm(
    '确认接受此订单吗？确认后房屋将变为已出租状态',
    '订单确认',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await request.put(`/orders/${row.id}/confirm`, null, {
        successMsg: '订单确认成功',
        onSuccess: () => {
          fetchOrders()
        }
      })
    } catch (error) {
      console.error('确认订单失败:', error)
    }
  }).catch(() => {})
}

// 取消订单
const handleCancel = (row) => {
  ElMessageBox.confirm(
    '确认取消此订单吗？取消后不可恢复',
    '订单取消',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await request.put(`/orders/${row.id}/cancel`, null, {
        successMsg: '订单取消成功',
        onSuccess: () => {
          fetchOrders()
        }
      })
    } catch (error) {
      console.error('取消订单失败:', error)
    }
  }).catch(() => {})
}

// 申请退款
const handleRefund = (row) => {
  ElMessageBox.confirm(
    '确认申请退款吗？申请后将立即处理退款',
    '退款申请',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await request.post(`/orders/${row.id}/refund`, null, {
        successMsg: '退款申请成功',
        onSuccess: () => {
          fetchOrders()
        }
      })
    } catch (error) {
      console.error('申请退款失败:', error)
    }
  }).catch(() => {})
}

// 导出Excel
const handleExport = async () => {
  exporting.value = true
  try {
    // 根据用户角色选择不同的导出接口
    let url = '/api/orders/tenant/export/excel'
    if (isAdmin.value) {
      url = '/api/orders/admin/export/excel'
    } else if (isLandlord.value) {
      url = '/api/orders/landlord/export/excel'
    }

    // 构建查询参数
    const params = {
      orderNo: searchForm.orderNo || undefined,
      status: searchForm.status !== '' ? searchForm.status : undefined
    }

    // 管理员可以按租客用户名和房东用户名筛选
    if (isAdmin.value) {
      if (searchForm.tenantUsername) params.tenantUsername = searchForm.tenantUsername
      if (searchForm.landlordUsername) params.landlordUsername = searchForm.landlordUsername
    }

    const token = localStorage.getItem('token')

    // 构建查询字符串
    const queryString = Object.keys(params)
      .filter(key => params[key] !== undefined)
      .map(key => `${key}=${encodeURIComponent(params[key])}`)
      .join('&')

    const fullUrl = queryString ? `${url}?${queryString}` : url

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
    let fileName = `订单列表_${new Date().getTime()}.xlsx`
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

// 页面加载时获取订单列表
onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.order-management {
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

.price {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

.dialog-footer button {
  margin-left: 10px;
}
</style> 