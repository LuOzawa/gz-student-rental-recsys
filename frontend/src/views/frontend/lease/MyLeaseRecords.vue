<template>
  <div class="my-lease-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <span class="page-icon">🏠</span>
      <h1 class="page-title">我的租赁记录</h1>
    </div>

    <!-- 筛选工具栏 -->
    <div class="filter-section card-styled">
      <div class="filter-row">
        <span class="filter-icon">🔍</span>
        <select
          v-model="filterForm.status"
          class="filter-select"
          @change="handleSearch"
        >
          <option value="">全部状态</option>
          <option v-for="item in statusOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
        <button v-if="filterForm.status !== ''" class="clear-btn" @click="filterForm.status = ''; handleSearch()">
          ✕
        </button>
      </div>
    </div>

    <!-- 租赁记录列表 -->
    <div class="lease-list" v-loading="loading">
      <div v-if="leaseRecords.length === 0 && !loading" class="empty-state">
        <span class="empty-icon">📭</span>
        <h3 class="empty-title">暂无租赁记录</h3>
        <p class="empty-desc">去看看心仪的房源吧</p>
        <button class="btn-browse" @click="$router.push('/houses')">
          🏘️ 浏览房源
        </button>
      </div>

      <div
        v-for="record in leaseRecords"
        :key="record.id"
        class="lease-card card-creative"
      >
        <div class="lease-header">
          <span class="order-no">📝 {{ record.order?.orderNo }}</span>
          <span class="lease-date">{{ record.createTime }}</span>
        </div>

        <div class="lease-content">
          <div class="house-info" @click="goToHouseDetail(record.house?.id)">
            <div class="house-image-wrapper">
              <img v-if="getHouseImage(record.house)" :src="getHouseImage(record.house)" class="house-image" />
              <div v-else class="house-image-placeholder">🏠</div>
            </div>

            <div class="house-details">
              <h4 class="house-title">{{ record.house?.title || '未知房源' }}</h4>
              <p class="house-address">📍 {{ record.house?.address || '未知地址' }}</p>
              <div class="house-meta">
                <span v-if="record.house?.area" class="meta-tag">📐 {{ record.house.area }}㎡</span>
                <span v-if="record.house?.typeName" class="meta-tag">{{ record.house.typeName }}</span>
              </div>
            </div>
          </div>

          <div class="lease-info">
            <div class="info-row">
              <span class="info-icon">👤</span>
              <span class="info-label">房东：</span>
              <span class="info-value">{{ record.landlord?.name || '未知' }}</span>
            </div>
            <div class="info-row">
              <span class="info-icon">📞</span>
              <span class="info-label">联系方式：</span>
              <span class="info-value">{{ record.landlord?.phone || '未知' }}</span>
            </div>
            <div class="lease-period-box">
              <div class="period-row">
                <span class="period-icon">📅</span>
                <span class="period-label">租期</span>
                <span class="period-value">{{ DateUtils.formatDate(record.startDate) }} 至 {{ DateUtils.formatDate(record.endDate) }}</span>
              </div>
              <div class="period-row">
                <span class="period-icon">💰</span>
                <span class="period-label">租金</span>
                <span class="period-value">¥ {{ record.rentAmount }}</span>
              </div>
              <div class="period-row">
                <span class="period-icon">🔄</span>
                <span class="period-label">支付周期</span>
                <span class="period-value">{{ getPaymentCycleText(record.paymentCycle) }}</span>
              </div>
            </div>
            <div class="status-tags">
              <span class="status-badge" :class="getStatusClass(record.status)">
                {{ getStatusIcon(record.status) }} {{ getStatusText(record.status) }}
              </span>
              <span v-if="canRenew(record)" class="tag-renewable">✨ 可续签</span>
              <span v-if="record.hasRenewal" class="tag-renewed">🔄 已续签</span>
              <span v-if="record.isRenewal === 1" class="tag-renewal-order">🔄 续签租约</span>
            </div>
          </div>
        </div>

        <div class="lease-footer">
          <div class="evaluation-info" v-if="record.evaluationScore">
            <span class="eval-item">
              <span class="eval-icon">⭐</span>
              <span>房屋：{{ record.evaluationScore }}/5</span>
            </span>
            <span v-if="record.landlordCreditScore" class="eval-item">
              <span class="eval-icon">👍</span>
              <span>房东信用：{{ record.landlordCreditScore }}/5</span>
            </span>
          </div>

          <div class="action-buttons">
            <button
              v-if="record.contractUrl"
              class="btn-action btn-download"
              @click="downloadContract(record)"
            >
              📄 下载合同
            </button>
            <button
              class="btn-action btn-detail"
              @click="viewLeaseDetail(record)"
            >
              📋 详情
            </button>
            <button
              v-if="record.status === 1 && !record.evaluationScore"
              class="btn-action btn-evaluate"
              @click="handleEvaluate(record)"
            >
              ✨ 评价
            </button>
            <button
              v-if="canRenew(record) && !record.hasRenewal"
              class="btn-action btn-renew"
              @click="handleRenew(record)"
            >
              🔄 申请续签
            </button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="leaseRecords.length > 0" class="pagination-section">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 30]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 租赁详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="租赁详情" width="700px" class="custom-dialog">
      <div class="detail-grid">
        <div class="detail-item full">
          <span class="label">订单编号</span>
          <span class="value">{{ detailForm.order?.orderNo }}</span>
        </div>
        <div class="detail-item">
          <span class="label">租赁状态</span>
          <span class="value status-badge" :class="getStatusClass(detailForm.status)">
            {{ getStatusText(detailForm.status) }}
          </span>
        </div>
        <div class="detail-item">
          <span class="label">房源标题</span>
          <span class="value">{{ detailForm.house?.title }}</span>
        </div>
        <div class="detail-item">
          <span class="label">房源地址</span>
          <span class="value">{{ detailForm.house?.address }}</span>
        </div>
        <div class="detail-item">
          <span class="label">月租金</span>
          <span class="value price">¥ {{ detailForm.rentAmount }}</span>
        </div>
        <div class="detail-item">
          <span class="label">支付周期</span>
          <span class="value">{{ getPaymentCycleText(detailForm.paymentCycle) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">开始日期</span>
          <span class="value">{{ DateUtils.formatDate(detailForm.startDate) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">结束日期</span>
          <span class="value">{{ DateUtils.formatDate(detailForm.endDate) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">房东姓名</span>
          <span class="value">{{ detailForm.landlord?.name }}</span>
        </div>
        <div class="detail-item">
          <span class="label">房东电话</span>
          <span class="value">{{ detailForm.landlord?.phone }}</span>
        </div>
        <div v-if="detailForm.evaluationScore" class="detail-item">
          <span class="label">房屋评分</span>
          <div class="rating-display">
            <el-rate v-model="detailForm.evaluationScore" disabled show-score />
          </div>
        </div>
        <div v-if="detailForm.evaluationContent" class="detail-item full">
          <span class="label">评价内容</span>
          <span class="value review-text">{{ detailForm.evaluationContent }}</span>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <button class="btn-primary" @click="detailDialogVisible = false">关闭</button>
        </div>
      </template>
    </el-dialog>

    <!-- 评价对话框 -->
    <el-dialog v-model="evaluateDialogVisible" title="租赁评价" width="550px" class="custom-dialog">
      <div class="evaluate-content">
        <div class="evaluate-house">
          <span class="house-icon">🏠</span>
          <span>{{ evaluateForm.houseTitle }}</span>
        </div>
        <div class="evaluate-landlord">
          <span class="landlord-icon">👤</span>
          <span>{{ evaluateForm.landlordName }}</span>
        </div>

        <div class="rating-section">
          <span class="section-title">⭐ 房屋评分</span>
          <el-rate
            v-model="evaluateForm.houseScore"
            :texts="['很差', '较差', '一般', '不错', '很好']"
            show-text
          />
          <span class="section-hint">对房屋的整体满意度评分</span>
        </div>

        <div class="rating-section">
          <span class="section-title">👍 房东信用评分</span>
          <el-rate
            v-model="evaluateForm.landlordCreditScore"
            :texts="['很差', '较差', '一般', '良好', '优秀']"
            show-text
          />
          <span class="section-hint">对房东服务态度、诚信度的评分</span>
        </div>

        <div class="input-section">
          <span class="section-title">💬 评价内容</span>
          <textarea
            v-model="evaluateForm.content"
            class="evaluate-textarea"
            placeholder="请输入您对此次租赁体验的评价（选填）"
            maxlength="500"
          ></textarea>
          <span class="char-count">{{ evaluateForm.content.length }}/500</span>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <button class="btn-secondary" @click="evaluateDialogVisible = false">取消</button>
          <button class="btn-primary" :disabled="submitting" @click="submitEvaluation">
            {{ submitting ? '提交中...' : '🚀 提交评价' }}
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- 续签对话框 -->
    <el-dialog v-model="renewDialogVisible" title="申请续签" width="600px" class="custom-dialog">
      <div class="renew-content">
        <div class="renew-info">
          <span class="info-icon">🏠</span>
          <span class="info-text">{{ renewForm.houseTitle }}</span>
        </div>
        <div class="renew-info">
          <span class="info-icon">📅</span>
          <span class="info-text">当前租期：{{ DateUtils.formatDate(renewForm.currentStartDate) }} 至 {{ DateUtils.formatDate(renewForm.currentEndDate) }}</span>
        </div>
        <div class="renew-info">
          <span class="info-icon">🔄</span>
          <span class="info-text">续签租期：{{ DateUtils.formatDate(renewForm.renewalStartDate) }} 至 {{ DateUtils.formatDate(renewForm.renewalEndDate) }}</span>
        </div>
        <div class="renew-info">
          <span class="info-icon">💳</span>
          <span class="info-text">支付周期：{{ getPaymentCycleText(renewForm.paymentCycle) }}</span>
        </div>
        <div class="renew-price">
          <span class="price-label">续签租金</span>
          <div class="price-input">
            <span class="currency">¥</span>
            <input
              v-model="renewForm.amount"
              type="number"
              min="0"
              step="100"
              class="price-input-field"
            />
            <span class="period">/{{ getPaymentCycleUnit(renewForm.paymentCycle) }}</span>
          </div>
          <span class="price-hint">原租金：¥{{ renewForm.originalAmount }}</span>
        </div>

        <div class="renew-tips">
          <span class="tips-icon">ℹ️</span>
          <div class="tips-content">
            <p>1. 续签无需再次支付押金</p>
            <p>2. 续签租约将在当前租约到期后的第二天开始</p>
            <p>3. 续签申请提交后需要等待房东确认</p>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <button class="btn-secondary" @click="renewDialogVisible = false">取消</button>
          <button class="btn-primary" :disabled="renewing" @click="submitRenewal">
            {{ renewing ? '提交中...' : '🚀 提交申请' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import DateUtils from '@/utils/dateUtils'

const router = useRouter()
const baseAPI = import.meta.env.VITE_BASE_API || '/api'

// 跳转到房源详情页
const goToHouseDetail = (houseId) => {
  if (houseId) {
    router.push('/houses/' + houseId)
  }
}

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

// 获取状态图标
const getStatusIcon = (status) => {
  const icons = {
    0: '✗',
    1: '🏠',
    2: '✓',
    3: '🚪'
  }
  return icons[status] || '•'
}

// 获取状态样式类
const getStatusClass = (status) => {
  const classes = {
    0: 'status-cancelled',
    1: 'status-active',
    2: 'status-ended',
    3: 'status-terminated'
  }
  return classes[status] || ''
}

// 获取支付周期文本
const getPaymentCycleText = (cycle) => {
  const map = {
    'MONTHLY': '月付',
    'QUARTERLY': '季付',
    'YEARLY': '年付'
  }
  return map[cycle] || '未知'
}

// 获取支付周期单位
const getPaymentCycleUnit = (cycle) => {
  const map = {
    'MONTHLY': '月',
    'QUARTERLY': '季',
    'YEARLY': '年'
  }
  return map[cycle] || '月'
}

// 检查租约是否可以续签
const canRenew = (lease) => {
  if (lease.status !== 1) return false
  if (!lease.endDate) return false

  const today = new Date()
  const endDate = new Date(lease.endDate)
  const daysUntilEnd = Math.ceil((endDate - today) / (1000 * 60 * 60 * 24))

  return daysUntilEnd >= 0 && daysUntilEnd <= 7
}

// 计算续签结束日期
const calculateRenewalEndDate = (startDate, paymentCycle) => {
  const start = new Date(startDate)
  let endDate = new Date(start)

  const months = {
    'MONTHLY': 1,
    'QUARTERLY': 3,
    'YEARLY': 12
  }
  const monthsToAdd = months[paymentCycle] || 1

  endDate.setMonth(endDate.getMonth() + monthsToAdd)
  endDate.setDate(endDate.getDate() - 1)

  return endDate
}

// 数据加载与分页
const loading = ref(false)
const leaseRecords = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 筛选表单
const filterForm = reactive({
  status: ''
})

// 详情对话框
const detailDialogVisible = ref(false)
const detailForm = reactive({
  order: {},
  house: {},
  landlord: {},
  rentAmount: 0,
  paymentCycle: '',
  status: 0,
  startDate: '',
  endDate: '',
  evaluationScore: null,
  landlordCreditScore: null,
  evaluationContent: ''
})

// 评价对话框
const evaluateDialogVisible = ref(false)
const submitting = ref(false)
const evaluateForm = reactive({
  id: '',
  houseTitle: '',
  landlordName: '',
  houseScore: 5,
  landlordCreditScore: 5,
  content: ''
})

// 续签对话框
const renewDialogVisible = ref(false)
const renewing = ref(false)
const renewForm = reactive({
  leaseId: '',
  houseTitle: '',
  currentStartDate: '',
  currentEndDate: '',
  renewalStartDate: '',
  renewalEndDate: '',
  paymentCycle: '',
  amount: 0,
  originalAmount: 0
})

// 获取房屋图片
const getHouseImage = (house) => {
  if (!house || !house.images) return null
  try {
    const images = JSON.parse(house.images)
    if (images && images.length > 0) {
      const imgUrl = images[0]
      return imgUrl.startsWith('http') ? imgUrl : baseAPI + imgUrl
    }
    return null
  } catch (e) {
    return null
  }
}

// 获取租赁记录列表
const fetchLeaseRecords = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value,
      status: filterForm.status !== '' ? filterForm.status : undefined
    }

    await request.get('/lease-records/tenant', params, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        leaseRecords.value = res.records || []
        total.value = res.total || 0
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

// 重置搜索
const resetSearch = () => {
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

// 查看租赁详情
const viewLeaseDetail = (record) => {
  Object.assign(detailForm, record)
  detailDialogVisible.value = true
}

// 下载合同
const downloadContract = (record) => {
  if (!record.contractUrl) {
    ElMessage.warning('合同文件不存在')
    return
  }

  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('您需要登录才能下载合同')
    router.push('/login')
    return
  }

  const downloadUrl = baseAPI + '/lease-records/' + record.id + '/download-contract?token=' + token
  const link = document.createElement('a')
  link.href = downloadUrl
  link.download = '租赁合同_' + record.id + '.pdf'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 评价租赁
const handleEvaluate = (record) => {
  evaluateForm.id = record.id
  evaluateForm.houseTitle = record.house?.title || '未知房屋'
  evaluateForm.landlordName = record.landlord?.name || '未知房东'
  evaluateForm.houseScore = 5
  evaluateForm.landlordCreditScore = 5
  evaluateForm.content = ''
  evaluateDialogVisible.value = true
}

// 提交评价
const submitEvaluation = async () => {
  if (evaluateForm.houseScore < 1) {
    ElMessage.warning('请选择房屋评分')
    return
  }

  if (evaluateForm.landlordCreditScore < 1) {
    ElMessage.warning('请选择房东信用评分')
    return
  }

  submitting.value = true
  try {
    await request.post(`/lease-records/${evaluateForm.id}/evaluation`, null, {
      params: {
        houseScore: evaluateForm.houseScore,
        landlordCreditScore: evaluateForm.landlordCreditScore,
        content: evaluateForm.content || ''
      },
      successMsg: '评价提交成功',
      onSuccess: () => {
        evaluateDialogVisible.value = false
        fetchLeaseRecords()
      }
    })
  } catch (error) {
    console.error('提交评价失败:', error)
  } finally {
    submitting.value = false
  }
}

// 处理续签
const handleRenew = async (lease) => {
  try {
    const canRenewRes = await request.get(`/orders/check-renewal/${lease.id}`, {}, {
      showDefaultMsg: false
    })

    if (!canRenewRes) {
      ElMessage.warning('当前租约不符合续签条件')
      return
    }

    renewForm.leaseId = lease.id
    renewForm.houseTitle = lease.house?.title || '未知房屋'
    renewForm.currentStartDate = lease.startDate
    renewForm.currentEndDate = lease.endDate
    renewForm.paymentCycle = lease.paymentCycle
    renewForm.originalAmount = lease.rentAmount
    renewForm.amount = lease.rentAmount

    const endDate = new Date(lease.endDate)
    const renewalStartDate = new Date(endDate)
    renewalStartDate.setDate(renewalStartDate.getDate() + 1)
    renewForm.renewalStartDate = renewalStartDate

    const renewalEndDate = calculateRenewalEndDate(renewalStartDate, lease.paymentCycle)
    renewForm.renewalEndDate = renewalEndDate

    renewDialogVisible.value = true
  } catch (error) {
    console.error('获取续签信息失败:', error)
    ElMessage.error('获取续签信息失败，请稍后重试')
  }
}

// 提交续签申请
const submitRenewal = async () => {
  if (!renewForm.amount || renewForm.amount <= 0) {
    ElMessage.warning('请输入有效的续签租金')
    return
  }

  renewing.value = true
  try {
    await request.post(`/orders/renewal/${renewForm.leaseId}`, {
      amount: renewForm.amount,
      paymentCycle: renewForm.paymentCycle
    }, {
      successMsg: '续签订单创建成功，请前往订单页面支付',
      onSuccess: () => {
        renewDialogVisible.value = false
        fetchLeaseRecords()
        setTimeout(() => {
          router.push('/my-orders')
        }, 1500)
      }
    })
  } catch (error) {
    console.error('提交续签申请失败:', error)
  } finally {
    renewing.value = false
  }
}

// 检查租约是否有续签订单
const checkRenewalStatus = async () => {
  try {
    for (const lease of leaseRecords.value) {
      if (lease.status === 1) {
        const orderRes = await request.get('/orders/tenant', {
          currentPage: 1,
          size: 100
        }, {
          showDefaultMsg: false
        })

        const orders = orderRes?.records || []
        const renewalOrder = orders.find(order =>
          order.isRenewal === 1 &&
          order.originalOrderId === lease.orderId
        )

        if (renewalOrder) {
          lease.hasRenewal = true
        }
      }
    }
  } catch (error) {
    console.error('检查续签状态失败:', error)
  }
}

onMounted(async () => {
  await fetchLeaseRecords()
  await checkRenewalStatus()
})
</script>

<style lang="scss" scoped>
.my-lease-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
}

// 页面头部
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;

  .page-icon {
    font-size: 32px;
    animation: float 3s ease-in-out infinite;
  }

  .page-title {
    font-family: 'Outfit', sans-serif;
    font-size: 28px;
    font-weight: 800;
    color: var(--color-text-primary);
    margin: 0;
  }
}

// 筛选区域
.filter-section {
  padding: 16px 20px;
  margin-bottom: 20px;

  .filter-row {
    display: flex;
    align-items: center;
    gap: 12px;

    .filter-icon {
      font-size: 18px;
    }

    .filter-select {
      flex: 1;
      padding: 10px 16px;
      border: 2px solid var(--color-border-light);
      border-radius: var(--radius-full);
      font-size: 14px;
      background: white;
      cursor: pointer;
      outline: none;
      transition: all var(--transition-fast);

      &:focus {
        border-color: var(--color-primary);
      }
    }

    .clear-btn {
      width: 28px;
      height: 28px;
      border: none;
      background: var(--color-bg-tertiary);
      border-radius: 50%;
      cursor: pointer;
      font-size: 12px;
      color: var(--color-text-tertiary);
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all var(--transition-fast);

      &:hover {
        background: var(--color-error);
        color: white;
      }
    }
  }
}

// 租赁列表
.lease-list {
  min-height: 300px;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;

  .empty-icon {
    font-size: 80px;
    display: block;
    margin-bottom: 16px;
    opacity: 0.5;
  }

  .empty-title {
    font-size: 20px;
    font-weight: 700;
    color: var(--color-text-primary);
    margin: 0 0 8px 0;
  }

  .empty-desc {
    color: var(--color-text-tertiary);
    margin: 0 0 24px 0;
  }
}

.btn-browse {
  padding: 12px 28px;
  background: var(--color-primary-gradient);
  color: white;
  border: none;
  border-radius: var(--radius-full);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(255, 107, 107, 0.4);
  }
}

// 租赁卡片
.lease-card {
  padding: 20px;
  margin-bottom: 16px;
  animation: fadeInUp 0.4s ease-out backwards;

  .lease-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 16px;
    border-bottom: 1px solid var(--color-border-light);
    margin-bottom: 16px;

    .order-no {
      font-weight: 700;
      font-size: 14px;
      color: var(--color-text-primary);
    }

    .lease-date {
      font-size: 13px;
      color: var(--color-text-tertiary);
    }
  }

  .lease-content {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
    margin-bottom: 16px;
  }

  .house-info {
    display: flex;
    flex: 3;
    min-width: 300px;
    gap: 14px;
    cursor: pointer;
    padding: 10px;
    margin: -10px;
    border-radius: var(--radius-lg);
    transition: all var(--transition-fast);

    &:hover {
      background: var(--color-bg-tertiary);

      .house-title {
        color: var(--color-primary);
      }
    }

    .house-image-wrapper {
      width: 120px;
      height: 90px;
      flex-shrink: 0;
      border-radius: var(--radius-md);
      overflow: hidden;
      background: var(--color-bg-tertiary);

      .house-image {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .house-image-placeholder {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 32px;
        opacity: 0.5;
      }
    }

    .house-details {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: space-between;

      .house-title {
        font-weight: 700;
        font-size: 16px;
        color: var(--color-text-primary);
        margin: 0 0 6px 0;
      }

      .house-address {
        color: var(--color-text-tertiary);
        font-size: 13px;
        margin: 0 0 8px 0;
      }

      .house-meta {
        display: flex;
        gap: 8px;

        .meta-tag {
          padding: 4px 10px;
          background: var(--color-bg-tertiary);
          border-radius: var(--radius-sm);
          font-size: 12px;
          color: var(--color-text-secondary);
        }
      }
    }
  }

  .lease-info {
    flex: 2;
    min-width: 250px;
    display: flex;
    flex-direction: column;
    gap: 10px;

    .info-row {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      color: var(--color-text-secondary);

      .info-icon {
        font-size: 14px;
      }

      .info-label {
        color: var(--color-text-tertiary);
      }

      .info-value {
        color: var(--color-text-primary);
        font-weight: 500;
      }
    }

    .lease-period-box {
      padding: 12px;
      background: var(--color-bg-tertiary);
      border-radius: var(--radius-md);
      margin: 4px 0;

      .period-row {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;

        &:last-child {
          margin-bottom: 0;
        }

        .period-icon {
          font-size: 14px;
        }

        .period-label {
          color: var(--color-text-tertiary);
          font-size: 13px;
          min-width: 60px;
        }

        .period-value {
          color: var(--color-text-primary);
          font-weight: 500;
          font-size: 13px;
        }
      }
    }

    .status-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .status-badge {
      padding: 6px 14px;
      border-radius: var(--radius-full);
      font-size: 13px;
      font-weight: 600;

      &.status-cancelled {
        background: var(--color-bg-tertiary);
        color: var(--color-text-tertiary);
      }

      &.status-active {
        background: rgba(78, 205, 196, 0.2);
        color: var(--color-secondary-dark);
      }

      &.status-ended {
        background: var(--color-bg-tertiary);
        color: var(--color-text-tertiary);
      }

      &.status-terminated {
        background: rgba(255, 107, 107, 0.15);
        color: var(--color-error);
      }
    }

    .tag-renewable {
      padding: 4px 10px;
      background: rgba(78, 205, 196, 0.2);
      color: var(--color-secondary-dark);
      border-radius: var(--radius-full);
      font-size: 12px;
      font-weight: 600;
    }

    .tag-renewed {
      padding: 4px 10px;
      background: var(--color-bg-tertiary);
      color: var(--color-text-tertiary);
      border-radius: var(--radius-full);
      font-size: 12px;
      font-weight: 600;
    }

    .tag-renewal-order {
      padding: 4px 10px;
      background: rgba(255, 230, 109, 0.2);
      color: #B8860B;
      border-radius: var(--radius-full);
      font-size: 12px;
      font-weight: 600;
    }
  }

  .lease-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 16px;
    border-top: 1px solid var(--color-border-light);

    .evaluation-info {
      display: flex;
      flex-direction: column;
      gap: 6px;

      .eval-item {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        color: var(--color-text-secondary);

        .eval-icon {
          font-size: 14px;
        }
      }
    }

    .action-buttons {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
    }
  }
}

.btn-action {
  padding: 8px 16px;
  border: 2px solid transparent;
  border-radius: var(--radius-full);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);

  &.btn-download {
    background: transparent;
    border-color: var(--color-border);
    color: var(--color-text-secondary);

    &:hover {
      border-color: var(--color-secondary);
      color: var(--color-secondary);
    }
  }

  &.btn-detail {
    background: transparent;
    border-color: var(--color-border);
    color: var(--color-text-secondary);

    &:hover {
      border-color: var(--color-text-secondary);
      background: var(--color-bg-tertiary);
    }
  }

  &.btn-evaluate {
    background: transparent;
    border-color: var(--color-accent);
    color: #B8860B;

    &:hover {
      background: var(--color-accent);
      color: var(--color-text-primary);
    }
  }

  &.btn-renew {
    background: var(--color-secondary-gradient);
    color: white;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(78, 205, 196, 0.3);
    }
  }
}

// 分页
.pagination-section {
  margin-top: 32px;
  display: flex;
  justify-content: center;

  :deep(.el-pagination) {
    .btn-prev,
    .btn-next,
    .el-pager li {
      border-radius: var(--radius-md);
      font-weight: 600;

      &.is-active {
        background: var(--color-primary-gradient);
        color: white;
      }
    }
  }
}

// 对话框样式
:deep(.custom-dialog) {
  .el-dialog__header {
    padding: 20px 24px;
    border-bottom: 1px solid var(--color-border-light);

    .el-dialog__title {
      font-weight: 700;
      font-size: 18px;
    }
  }

  .el-dialog__body {
    padding: 24px;
  }

  .el-dialog__footer {
    padding: 16px 24px;
    border-top: 1px solid var(--color-border-light);
  }
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;

  .detail-item {
    display: flex;
    justify-content: space-between;
    padding: 12px;
    background: var(--color-bg-tertiary);
    border-radius: var(--radius-md);
    align-items: center;

    &.full {
      grid-column: 1 / -1;
    }

    .label {
      color: var(--color-text-tertiary);
      font-weight: 500;
    }

    .value {
      color: var(--color-text-primary);
      font-weight: 500;

      &.price {
        color: var(--color-primary);
        font-weight: 700;
      }

      &.review-text {
        white-space: pre-line;
        line-height: 1.6;
        font-size: 14px;
      }

      &.status-badge {
        padding: 4px 10px;
        border-radius: var(--radius-full);
        font-size: 12px;
      }
    }

    .rating-display {
      display: flex;
      align-items: center;
    }
  }
}

.evaluate-content {
  .evaluate-house,
  .evaluate-landlord {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;
    background: var(--color-bg-tertiary);
    border-radius: var(--radius-md);
    margin-bottom: 16px;

    .house-icon,
    .landlord-icon {
      font-size: 20px;
    }
  }

  .rating-section {
    padding: 16px;
    background: var(--color-bg-tertiary);
    border-radius: var(--radius-md);
    margin-bottom: 16px;

    .section-title {
      display: block;
      font-weight: 600;
      color: var(--color-text-primary);
      margin-bottom: 12px;
    }

    .section-hint {
      display: block;
      font-size: 12px;
      color: var(--color-text-tertiary);
      margin-top: 8px;
    }
  }

  .input-section {
    position: relative;
    margin-bottom: 8px;

    .section-title {
      display: block;
      font-weight: 600;
      color: var(--color-text-primary);
      margin-bottom: 12px;
    }

    .evaluate-textarea {
      width: 100%;
      padding: 16px;
      border: 2px solid var(--color-border);
      border-radius: var(--radius-lg);
      font-size: 14px;
      font-family: inherit;
      resize: none;
      min-height: 100px;
      outline: none;
      transition: all var(--transition-fast);

      &:focus {
        border-color: var(--color-primary);
      }
    }

    .char-count {
      position: absolute;
      bottom: 12px;
      right: 12px;
      font-size: 12px;
      color: var(--color-text-tertiary);
    }
  }
}

.renew-content {
  .renew-info {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid var(--color-border-light);

    &:last-of-type {
      border-bottom: none;
    }

    .info-icon {
      font-size: 18px;
    }

    .info-text {
      color: var(--color-text-secondary);
      font-size: 14px;
    }
  }

  .renew-price {
    padding: 16px;
    background: var(--color-bg-tertiary);
    border-radius: var(--radius-md);
    margin: 16px 0;

    .price-label {
      display: block;
      font-weight: 600;
      color: var(--color-text-primary);
      margin-bottom: 12px;
    }

    .price-input {
      display: flex;
      align-items: center;
      gap: 8px;

      .currency {
        font-size: 18px;
        font-weight: 700;
        color: var(--color-primary);
      }

      .price-input-field {
        flex: 1;
        padding: 10px 12px;
        border: 2px solid var(--color-border);
        border-radius: var(--radius-md);
        font-size: 16px;
        font-weight: 600;
        color: var(--color-primary);
        outline: none;

        &:focus {
          border-color: var(--color-primary);
        }
      }

      .period {
        color: var(--color-text-tertiary);
      }
    }

    .price-hint {
      display: block;
      font-size: 12px;
      color: var(--color-text-tertiary);
      margin-top: 8px;
    }
  }

  .renew-tips {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 12px;
    background: rgba(78, 205, 196, 0.1);
    border-radius: var(--radius-md);

    .tips-icon {
      font-size: 18px;
    }

    .tips-content {
      flex: 1;

      p {
        margin: 4px 0;
        font-size: 13px;
        line-height: 1.5;
        color: var(--color-text-secondary);
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-primary {
  padding: 12px 24px;
  background: var(--color-primary-gradient);
  color: white;
  border: none;
  border-radius: var(--radius-full);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(255, 107, 107, 0.4);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.btn-secondary {
  padding: 12px 24px;
  background: transparent;
  color: var(--color-text-secondary);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-full);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-base);

  &:hover {
    border-color: var(--color-text-secondary);
    background: var(--color-bg-tertiary);
  }
}

// 响应式
@media (max-width: 768px) {
  .my-lease-page {
    padding: 15px;
  }

  .lease-content {
    flex-direction: column;
  }

  .lease-footer {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;

    .action-buttons {
      justify-content: stretch;

      .btn-action {
        flex: 1;
        text-align: center;
      }
    }
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
