<template>
  <div class="lease-management">
    <el-card class="filter-container">
      <div class="filter-item">
        <el-form :inline="true" :model="filterForm" class="filter-form">
          <el-form-item label="租赁状态">
            <el-select v-model="filterForm.status" placeholder="全部" clearable>
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="table-container">
      <div class="table-header">
        <span class="table-title">租赁记录列表</span>
      </div>
      
      <el-table
        v-loading="loading"
        :data="leaseRecords"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="订单信息" min-width="160">
          <template #default="scope">
            <div>订单号：{{ scope.row.order?.orderNo || '-' }}</div>
            <div>创建时间：{{ DateUtils.formatDate(scope.row.createTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="房屋信息" min-width="180">
          <template #default="scope">
            <div>{{ scope.row.house?.title || '-' }}</div>
            <div class="text-muted">{{ scope.row.house?.address || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="租户信息" min-width="120">
          <template #default="scope">
            <div>{{ scope.row.tenant?.name || '-' }}</div>
            <div class="text-muted">{{ scope.row.tenant?.phone || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="房东信息" min-width="120">
          <template #default="scope">
            <div>{{ scope.row.landlord?.name || '-' }}</div>
            <div class="text-muted">{{ scope.row.landlord?.phone || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="租期" min-width="200">
          <template #default="scope">
            <div>{{ DateUtils.formatDate(scope.row.startDate) }} 至 {{ DateUtils.formatDate(scope.row.endDate) }}</div>
            <div class="text-muted">租金：¥{{ scope.row.rentAmount }}/月</div>
          </template>
        </el-table-column>
        <el-table-column label="租赁状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评价" width="120">
          <template #default="scope">
            <template v-if="scope.row.evaluationScore">
              <el-rate
                v-model="scope.row.evaluationScore"
                disabled
                show-score
                text-color="#ff9900"
                score-template="{value}"
              />
            </template>
            <span v-else>暂无评价</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="220">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              @click="viewDetail(scope.row)"
            >
              查看详情
            </el-button>
            <el-button
              v-if="scope.row.contractUrl"
              type="info"
              size="small"
              @click="downloadContract(scope.row)"
            >
              下载合同
            </el-button>
            <el-button
              v-if="(isAdmin || isLandlord) && (!scope.row.contractUrl || scope.row.contractUrl === '')"
              type="success"
              size="small"
              @click="handleGenerateContract(scope.row)"
            >
              生成合同
            </el-button>
            <el-button
              v-if="(isAdmin || isLandlord) && scope.row.contractUrl"
              type="warning"
              size="small"
              @click="handleGenerateContract(scope.row)"
            >
              重新生成
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
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
    </el-card>
    
    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="租赁详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单编号">{{ detailForm.order?.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="租赁状态">
          <el-tag :type="getStatusType(detailForm.status)">
            {{ getStatusText(detailForm.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="房屋标题">{{ detailForm.house?.title }}</el-descriptions-item>
        <el-descriptions-item label="房屋地址">{{ detailForm.house?.address }}</el-descriptions-item>
        <el-descriptions-item label="租户姓名">{{ detailForm.tenant?.name }}</el-descriptions-item>
        <el-descriptions-item label="租户电话">{{ detailForm.tenant?.phone }}</el-descriptions-item>
        <el-descriptions-item label="房东姓名">{{ detailForm.landlord?.name }}</el-descriptions-item>
        <el-descriptions-item label="房东电话">{{ detailForm.landlord?.phone }}</el-descriptions-item>
        <el-descriptions-item label="月租金">¥ {{ detailForm.rentAmount }}</el-descriptions-item>
        <el-descriptions-item label="支付周期">{{ getPaymentCycleText(detailForm.paymentCycle) }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ DateUtils.formatDate(detailForm.startDate) }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ DateUtils.formatDate(detailForm.endDate) }}</el-descriptions-item>
        <el-descriptions-item label="实际结束日期" v-if="detailForm.actualEndDate">
          {{ DateUtils.formatDate(detailForm.actualEndDate) }}
        </el-descriptions-item>
        <el-descriptions-item label="合同链接" v-if="detailForm.contractUrl">
          <el-button type="info" size="small" @click="downloadContract(detailForm)">
            下载合同
          </el-button>
          <el-button 
            v-if="isAdmin || isLandlord" 
            type="warning" 
            size="small" 
            @click="handleGenerateContract(detailForm)"
          >
            重新生成
          </el-button>
        </el-descriptions-item>
        <el-descriptions-item label="合同操作" v-if="!detailForm.contractUrl && (isAdmin || isLandlord)">
          <el-button 
            type="success" 
            size="small" 
            @click="handleGenerateContract(detailForm)"
          >
            生成合同
          </el-button>
        </el-descriptions-item>
        <el-descriptions-item label="评价分数" v-if="detailForm.evaluationScore">
          <el-rate
            v-model="detailForm.evaluationScore"
            disabled
            show-score
            text-color="#ff9900"
            score-template="{value}"
          />
        </el-descriptions-item>
        <el-descriptions-item label="评价内容" v-if="detailForm.evaluationContent" :span="2">
          {{ detailForm.evaluationContent }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ DateUtils.formatDateTime(detailForm.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ DateUtils.formatDateTime(detailForm.updateTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import DateUtils from '@/utils/dateUtils'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const baseAPI = import.meta.env.VITE_BASE_API || '/api'
const router = useRouter()

// 状态选项
const statusOptions = [
  { value: 0, label: '已取消' },
  { value: 1, label: '租赁中' },
  { value: 2, label: '已结束' },
  { value: 3, label: '已退租' }
]

// 获取状态文本
const getStatusText = (status) => {
  const option = statusOptions.find(item => item.value === status)
  return option ? option.label : '未知状态'
}

// 获取状态类型
const getStatusType = (status) => {
  switch (status) {
    case 0: return 'danger'  // 已取消
    case 1: return 'success' // 租赁中
    case 2: return 'info'    // 已结束
    case 3: return 'warning' // 已退租
    default: return 'info'
  }
}

// 获取支付周期文本
const getPaymentCycleText = (cycle) => {
  switch (cycle) {
    case 'MONTHLY': return '月付';
    case 'QUARTERLY': return '季付';
    case 'YEARLY': return '年付';
    default: return '未知';
  }
}

// 筛选表单
const filterForm = reactive({
  status: ''
})

// 租赁记录列表
const loading = ref(false)
const leaseRecords = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 详情对话框
const detailDialogVisible = ref(false)
const detailForm = reactive({
  id: '',
  orderId: '',
  houseId: '',
  tenantId: '',
  landlordId: '',
  startDate: '',
  endDate: '',
  rentAmount: 0,
  paymentCycle: '',
  status: 0,
  actualEndDate: '',
  contractUrl: '',
  evaluationScore: null,
  evaluationContent: '',
  createTime: '',
  updateTime: '',
  house: {},
  tenant: {},
  landlord: {},
  order: {}
})

// 是否为管理员
const isAdmin = computed(() => userStore.userInfo?.roleCode === 'ADMIN')

// 是否为房东
const isLandlord = computed(() => userStore.userInfo?.roleCode === 'LANDLORD')

// 获取租赁记录列表
const fetchLeaseRecords = async () => {
  loading.value = true
  try {
    // 构建查询参数
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value,
      status: filterForm.status !== '' ? filterForm.status : undefined
    }
    
    // API路径根据用户角色确定
    let apiPath = '/lease-records'
    if (userStore.userInfo?.roleCode === 'LANDLORD') {
      apiPath = '/lease-records/landlord'
    } else if (userStore.userInfo?.roleCode === 'ADMIN') {
      apiPath = '/lease-records/admin'
    }
    
    await request.get(apiPath, params, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        leaseRecords.value = res.records||[]
        total.value = res.total||0
      }
    })
  } catch (error) {
    console.error('获取租赁记录失败:', error)
  } finally {
    loading.value = false
  }
}

// 查询
const handleSearch = () => {
  currentPage.value = 1
  fetchLeaseRecords()
}

// 重置筛选表单
const resetForm = () => {
  filterForm.status = ''
  handleSearch()
}

// 分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchLeaseRecords()
}

// 页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchLeaseRecords()
}

// 查看详情
const viewDetail = (row) => {
  Object.assign(detailForm, row)
  detailDialogVisible.value = true
}

// 下载合同
const downloadContract = (row) => {
  if (!row.contractUrl) {
    ElMessage.warning('合同文件不存在')
    return
  }
  
  // 获取token
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('您需要登录才能下载合同')
    router.push('/login')
    return
  }
  
  // 创建下载链接
  const downloadUrl = baseAPI + '/lease-records/' + row.id + '/download-contract?token=' + token
  
  // 创建一个隐藏的链接并点击它来下载
  const link = document.createElement('a')
  link.href = downloadUrl
  link.setAttribute('download', 'contract_' + row.id + '.pdf')
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 生成或重新生成合同
const handleGenerateContract = async (row) => {
  try {
    const isRegen = row.contractUrl && row.contractUrl !== '';
    const confirmMsg = isRegen ? '确定要重新生成合同吗？' : '确定要生成合同吗？';
    
    try {
      await ElMessageBox.confirm(confirmMsg, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      });
      
      loading.value = true;
      
      await request.post(`/lease-records/${row.id}/generate-contract`, null, {
        successMsg: isRegen ? '合同重新生成成功' : '合同生成成功',
        onSuccess: () => {
          // 刷新租赁记录列表
          fetchLeaseRecords();
        }
      });
    } catch (cancelError) {
      // 用户取消操作，不做处理
      console.log('用户取消操作');
    }
  } catch (error) {
    console.error('生成合同失败:', error);
    ElMessage.error(error.message || '生成合同失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

// 页面加载时获取租赁记录列表
onMounted(() => {
  fetchLeaseRecords()
})
</script>

<style scoped>
.lease-management {
  padding: 20px;
}

.filter-container {
  margin-bottom: 20px;
}

.table-container {
  margin-bottom: 20px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.table-title {
  font-size: 18px;
  font-weight: 500;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.text-muted {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
}
</style> 