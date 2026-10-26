<template>
  <div class="my-orders-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <span class="page-icon">📋</span>
      <h1 class="page-title">我的订单</h1>
    </div>

    <!-- Tab分组 -->
    <div class="tab-section">
      <div class="tab-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          :class="['tab-tab', { active: activeTab === tab.value }]"
          @click="activeTab = tab.value; handleTabClick()"
        >
          {{ tab.label }}
        </button>
      </div>
    </div>

    <!-- 搜索筛选 -->
    <div class="filter-section card-styled">
      <div class="filter-row">
        <span class="filter-icon">🔍</span>
        <input
          v-model="filterForm.orderNo"
          type="text"
          class="filter-input"
          placeholder="搜索订单编号..."
          @keyup.enter="handleSearch"
        />
        <button v-if="filterForm.orderNo" class="clear-btn" @click="filterForm.orderNo = ''; handleSearch()">
          ✕
        </button>
        <button class="btn-search" @click="handleSearch">
          搜索
        </button>
      </div>
    </div>

    <!-- 订单列表 -->
    <div class="orders-list" v-loading="loading">
      <div v-if="orders.length === 0 && !loading" class="empty-state">
        <span class="empty-icon">📭</span>
        <h3 class="empty-title">暂无订单</h3>
        <p class="empty-desc">去浏览心仪的房源吧</p>
        <button class="btn-browse" @click="$router.push('/houses')">
          🏘️ 浏览房源
        </button>
      </div>

      <div
        v-for="order in orders"
        :key="order.id"
        class="order-card card-creative"
      >
        <div class="order-header">
          <div class="order-header-left">
            <span class="order-no">📝 {{ order.orderNo }}</span>
            <span v-if="order.isRenewal === 1" class="renewal-badge">🔄 续签订单</span>
          </div>
          <span class="order-date">{{ order.createTime }}</span>
        </div>

        <!-- 续签订单说明 -->
        <div v-if="order.isRenewal === 1" class="renewal-info">
          <span class="info-icon">ℹ️</span>
          <span>本订单为续签订单，无需支付押金</span>
        </div>

        <div class="order-content">
          <div class="house-info" @click="goToHouseDetail(order.house?.id)">
            <div class="house-image-wrapper">
              <img v-if="getHouseImage(order.house)" :src="getHouseImage(order.house)" class="house-image" />
              <div v-else class="house-image-placeholder">🏠</div>
            </div>

            <div class="house-details">
              <h4 class="house-title">{{ order.house?.title || '未知房源' }}</h4>
              <p class="house-address">📍 {{ order.house?.address || '未知地址' }}</p>
              <div class="house-meta">
                <span v-if="order.house?.area" class="meta-tag">📐 {{ order.house.area }}㎡</span>
                <span v-if="order.house?.typeName" class="meta-tag">{{ order.house.typeName }}</span>
              </div>
            </div>
          </div>

          <div class="order-info">
            <div class="info-row">
              <span class="info-icon">👤</span>
              <span class="info-label">房东：</span>
              <span class="info-value">{{ order.landlord?.name || '未知' }}</span>
            </div>
            <div class="info-row" v-if="order.status === 2">
              <span class="info-icon">📞</span>
              <span class="info-label">联系方式：</span>
              <span class="info-value">{{ order.landlord?.phone || '未知' }}</span>
            </div>
            <div class="price-section">
              <div class="price-row">
                <span class="price-label">订单金额</span>
                <span class="price-value primary">¥ {{ order.amount }}</span>
              </div>
              <div class="price-row">
                <span class="price-label">押金</span>
                <span class="price-value">¥ {{ order.deposit }}</span>
              </div>
            </div>
            <div class="status-badge" :class="getStatusClass(order.status)">
              {{ getStatusIcon(order.status) }} {{ getStatusText(order.status) }}
            </div>
          </div>
        </div>

        <div class="order-footer">
          <div class="payment-info" v-if="order.paymentTime">
            <span>💳 支付于 {{ order.paymentTime }}</span>
            <span class="divider">|</span>
            <span>{{ order.paymentMethod }}</span>
          </div>

          <div class="action-buttons">
            <button
              v-if="order.status === 0"
              class="btn-action btn-pay"
              @click="handlePay(order)"
            >
              💰 立即支付
            </button>
            <button
              v-if="order.status === 0"
              class="btn-action btn-cancel"
              @click="handleCancel(order)"
            >
              🚫 取消
            </button>
            <button
              v-if="order.status === 1"
              class="btn-action btn-refund"
              @click="handleRefund(order)"
            >
              ↩️ 申请退款
            </button>
            <button
              v-if="canReviewOrder(order)"
              class="btn-action btn-review"
              @click="handleReview(order)"
            >
              {{ order.hasReview ? '⭐ 查看评价' : '✨ 评价房源' }}
            </button>
            <button
              class="btn-action btn-detail"
              @click="viewOrderDetail(order)"
            >
              📄 详情
            </button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="orders.length > 0" class="pagination-section">
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

    <!-- 支付对话框 -->
    <el-dialog v-model="payDialogVisible" title="订单支付" width="500px" class="custom-dialog">
      <div class="dialog-content">
        <div class="info-row">
          <span class="label">订单编号</span>
          <span class="value">{{ payForm.orderNo }}</span>
        </div>
        <div class="info-row">
          <span class="label">房源</span>
          <span class="value">{{ payForm.houseTitle }}</span>
        </div>
        <div class="info-row price">
          <span class="label">订单金额</span>
          <span class="value price-value">¥ {{ payForm.amount }}</span>
        </div>
        <div class="payment-methods">
          <span class="label">支付方式</span>
          <div class="method-options">
            <label class="method-option">
              <input type="radio" v-model="payForm.paymentMethod" value="支付宝" />
              <span>💳 支付宝</span>
            </label>
            <label class="method-option">
              <input type="radio" v-model="payForm.paymentMethod" value="微信支付" />
              <span>💚 微信支付</span>
            </label>
            <label class="method-option">
              <input type="radio" v-model="payForm.paymentMethod" value="银行卡" />
              <span>🏦 银行卡</span>
            </label>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <button class="btn-secondary" @click="payDialogVisible = false">取消</button>
          <button class="btn-primary" :disabled="paying" @click="confirmPay">
            {{ paying ? '支付中...' : '✓ 确认支付' }}
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="订单详情" width="700px" class="custom-dialog">
      <div class="detail-grid">
        <div class="detail-item">
          <span class="label">订单编号</span>
          <span class="value">{{ detailForm.orderNo }}</span>
        </div>
        <div class="detail-item">
          <span class="label">订单状态</span>
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
          <span class="label">订单金额</span>
          <span class="value price">¥ {{ detailForm.amount }}</span>
        </div>
        <div class="detail-item">
          <span class="label">押金金额</span>
          <span class="value">¥ {{ detailForm.deposit }}</span>
        </div>
        <div class="detail-item">
          <span class="label">房东姓名</span>
          <span class="value">{{ detailForm.landlord?.name }}</span>
        </div>
        <div class="detail-item">
          <span class="label">房东电话</span>
          <span class="value">{{ detailForm.landlord?.phone }}</span>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <button class="btn-primary" @click="detailDialogVisible = false">关闭</button>
        </div>
      </template>
    </el-dialog>

    <!-- 评价对话框 -->
    <el-dialog
      v-model="reviewDialogVisible"
      :title="currentReview?.id ? '查看评价' : '评价房源'"
      width="600px"
      class="custom-dialog"
    >
      <div class="review-content">
        <div class="review-house">{{ reviewForm.houseTitle }}</div>

        <template v-if="!currentReview?.id">
          <div class="rating-section">
            <span class="rating-label">⭐ 评分</span>
            <el-rate
              v-model="reviewForm.score"
              :texts="['极差', '失望', '一般', '满意', '惊喜']"
              show-text
            />
          </div>
          <div class="input-section">
            <textarea
              v-model="reviewForm.content"
              class="review-textarea"
              placeholder="请分享您对房源的真实感受，帮助其他租客做出更好的选择..."
              maxlength="500"
            ></textarea>
            <span class="char-count">{{ reviewForm.content.length }}/500</span>
          </div>
        </template>

        <template v-else>
          <div class="existing-review">
            <div class="rating-display">
              <span class="rating-label">⭐ 评分</span>
              <el-rate v-model="currentReview.score" disabled />
            </div>
            <div class="review-text">{{ currentReview.content }}</div>
            <div class="review-time">{{ currentReview.createTime }}</div>
            <div v-if="currentReview.replyContent" class="landlord-reply">
              <span class="reply-label">📢 房东回复：</span>
              <span class="reply-content">{{ currentReview.replyContent }}</span>
            </div>
          </div>
        </template>
      </div>
      <template #footer v-if="!currentReview?.id">
        <div class="dialog-footer">
          <button class="btn-secondary" @click="reviewDialogVisible = false">取消</button>
          <button class="btn-primary" :disabled="submittingReview" @click="submitReview">
            {{ submittingReview ? '提交中...' : '🚀 提交评价' }}
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

// Tab选项
const tabs = [
  { label: '📋 全部', value: 'all' },
  { label: '⏳ 待支付', value: 'unpaid' },
  { label: '⏰ 待确认', value: 'unconfirmed' },
  { label: '✓ 已确认', value: 'confirmed' },
  { label: '✗ 已取消', value: 'cancelled' },
  { label: '↩️ 已退款', value: 'refunded' }
]

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

// 获取状态图标
const getStatusIcon = (status) => {
  const icons = {
    0: '⏳',
    1: '⏰',
    2: '✓',
    3: '✗',
    4: '↩️'
  }
  return icons[status] || '•'
}

// 获取状态样式类
const getStatusClass = (status) => {
  const classes = {
    0: 'status-pending',
    1: 'status-waiting',
    2: 'status-confirmed',
    3: 'status-cancelled',
    4: 'status-refunded'
  }
  return classes[status] || ''
}

// 数据加载与分页
const loading = ref(false)
const orders = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 筛选表单
const filterForm = reactive({
  orderNo: ''
})

// 当前激活的标签页
const activeTab = ref('all')

// 支付对话框
const payDialogVisible = ref(false)
const paying = ref(false)
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
  orderNo: '',
  status: 0,
  amount: 0,
  deposit: 0,
  house: {},
  landlord: {}
})

// 评价对话框
const reviewDialogVisible = ref(false)
const submittingReview = ref(false)
const currentReview = ref(null)
const reviewForm = reactive({
  orderId: '',
  houseId: '',
  houseTitle: '',
  score: 5,
  content: ''
})

// 存储租赁记录映射
const leaseRecordMap = ref({})

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

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value,
      orderNo: filterForm.orderNo || undefined
    }

    // 根据激活的标签页设置状态
    if (activeTab.value === 'unpaid') {
      params.status = 0
    } else if (activeTab.value === 'unconfirmed') {
      params.status = 1
    } else if (activeTab.value === 'confirmed') {
      params.status = 2
    } else if (activeTab.value === 'cancelled') {
      params.status = 3
    } else if (activeTab.value === 'refunded') {
      params.status = 4
    }

    await request.get('/orders/tenant', params, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        orders.value = res.records || []
        total.value = res.total || 0
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

// 处理标签页点击
const handleTabClick = () => {
  currentPage.value = 1
  fetchOrders()
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

// 查看订单详情
const viewOrderDetail = async (order) => {
  try {
    await request.get(`/orders/${order.id}`, {}, {
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

// 处理支付
const handlePay = (order) => {
  payForm.id = order.id
  payForm.orderNo = order.orderNo
  payForm.houseTitle = order.house?.title || '未知房屋'
  payForm.amount = order.amount
  payForm.paymentMethod = '支付宝'
  payDialogVisible.value = true
}

// 确认支付
const confirmPay = async () => {
  if (!payForm.paymentMethod) {
    ElMessage.warning('请选择支付方式')
    return
  }

  paying.value = true
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
  } finally {
    paying.value = false
  }
}

// 取消订单
const handleCancel = (order) => {
  ElMessageBox.confirm('确认取消此订单吗？取消后不可恢复', '订单取消', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.put(`/orders/${order.id}/cancel`, null, {
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
const handleRefund = (order) => {
  ElMessageBox.confirm('确认申请退款吗？申请后将立即处理退款', '退款申请', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.post(`/orders/${order.id}/refund`, null, {
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

// 获取订单关联的租赁记录
const fetchLeaseRecords = async () => {
  try {
    const res = await request.get('/lease-records/tenant', {
      currentPage: 1,
      size: 1000
    })

    const records = res.records || []
    const map = {}
    records.forEach(record => {
      if (record.orderId) {
        map[record.orderId] = record
      }
    })
    leaseRecordMap.value = map

    await checkOrderReviews()
  } catch (error) {
    console.error('获取租赁记录失败:', error)
  }
}

// 检查订单是否已有评价
const checkOrderReviews = async () => {
  try {
    for (const order of orders.value) {
      if (order.house?.id) {
        const res = await request.get(`/house-comments/house/${order.house.id}`, {
          showDefaultMsg: false
        })

        let comments = []
        if (Array.isArray(res)) {
          comments = res
        } else if (res && Array.isArray(res.records)) {
          comments = res.records
        } else if (res && Array.isArray(res.data)) {
          comments = res.data
        }

        const userReview = comments.find(c => Number(c.type) === 1)
        if (userReview) {
          order.hasReview = true
          order.reviewData = userReview
        }
      }
    }
  } catch (error) {
    console.error('检查评价状态失败:', error)
  }
}

// 判断订单是否可以评价
const canReviewOrder = (order) => {
  const lease = leaseRecordMap.value[order.id]
  if (!lease) return false
  return lease.status === 1 || lease.status === 2
}

// 处理评价按钮点击
const handleReview = async (order) => {
  if (!order.house) {
    ElMessage.warning('房源信息不完整')
    return
  }

  if (order.hasReview && order.reviewData) {
    currentReview.value = order.reviewData
    reviewDialogVisible.value = true
    return
  }

  currentReview.value = null
  reviewForm.orderId = order.id
  reviewForm.houseId = order.house.id
  reviewForm.houseTitle = order.house.title
  reviewForm.score = 5
  reviewForm.content = ''
  reviewDialogVisible.value = true
}

// 提交评价
const submitReview = async () => {
  if (!reviewForm.score || reviewForm.score < 1 || reviewForm.score > 5) {
    ElMessage.warning('请选择评分')
    return
  }

  if (!reviewForm.content.trim()) {
    ElMessage.warning('请输入评价内容')
    return
  }

  submittingReview.value = true
  try {
    await request.post('/house-comments', {
      houseId: reviewForm.houseId,
      type: 1,
      score: reviewForm.score,
      content: reviewForm.content
    }, {
      successMsg: '评价提交成功',
      onSuccess: () => {
        reviewDialogVisible.value = false
        fetchOrders().then(() => {
          fetchLeaseRecords()
        })
      }
    })
  } catch (error) {
    console.error('提交评价失败:', error)
  } finally {
    submittingReview.value = false
  }
}

onMounted(() => {
  fetchOrders().then(() => {
    fetchLeaseRecords()
  })
})
</script>

<style lang="scss" scoped>
.my-orders-page {
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

// Tab区域
.tab-section {
  margin-bottom: 20px;

  .tab-tabs {
    display: flex;
    gap: 10px;
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
    }

    .tab-tab {
      flex-shrink: 0;
      padding: 10px 18px;
      background: white;
      border: 2px solid var(--color-border-light);
      border-radius: var(--radius-full);
      font-size: 14px;
      font-weight: 500;
      color: var(--color-text-secondary);
      cursor: pointer;
      transition: all var(--transition-fast);

      &:hover {
        border-color: var(--color-primary-light);
      }

      &.active {
        background: var(--color-primary-gradient);
        border-color: transparent;
        color: white;
        box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
      }
    }
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

    .filter-input {
      flex: 1;
      padding: 10px 16px;
      border: 2px solid var(--color-border-light);
      border-radius: var(--radius-full);
      font-size: 14px;
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

    .btn-search {
      padding: 10px 20px;
      background: var(--color-primary-gradient);
      color: white;
      border: none;
      border-radius: var(--radius-full);
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: all var(--transition-base);
      box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);

      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 6px 16px rgba(255, 107, 107, 0.4);
      }
    }
  }
}

// 订单列表
.orders-list {
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

// 订单卡片
.order-card {
  padding: 20px;
  margin-bottom: 16px;
  animation: fadeInUp 0.4s ease-out backwards;

  .order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 16px;
    border-bottom: 1px solid var(--color-border-light);
    margin-bottom: 16px;

    .order-header-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .order-no {
        font-weight: 700;
        font-size: 14px;
        color: var(--color-text-primary);
      }

      .renewal-badge {
        padding: 4px 10px;
        background: rgba(255, 230, 109, 0.2);
        color: #B8860B;
        border-radius: var(--radius-full);
        font-size: 12px;
        font-weight: 600;
      }
    }

    .order-date {
      font-size: 13px;
      color: var(--color-text-tertiary);
    }
  }

  .renewal-info {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px;
    background: rgba(78, 205, 196, 0.1);
    border-radius: var(--radius-md);
    margin-bottom: 16px;
    font-size: 13px;
    color: var(--color-secondary-dark);

    .info-icon {
      font-size: 16px;
    }
  }

  .order-content {
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

  .order-info {
    flex: 2;
    min-width: 250px;
    display: flex;
    flex-direction: column;
    gap: 8px;

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

    .price-section {
      margin: 8px 0;
      padding: 12px;
      background: var(--color-bg-tertiary);
      border-radius: var(--radius-md);

      .price-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 6px;

        &:last-child {
          margin-bottom: 0;
        }

        .price-label {
          font-size: 13px;
          color: var(--color-text-tertiary);
        }

        .price-value {
          font-weight: 700;
          font-size: 16px;
          color: var(--color-text-tertiary);

          &.primary {
            color: var(--color-primary);
          }
        }
      }
    }

    .status-badge {
      align-self: flex-start;
      padding: 6px 14px;
      border-radius: var(--radius-full);
      font-size: 13px;
      font-weight: 600;

      &.status-pending {
        background: rgba(255, 230, 109, 0.2);
        color: #B8860B;
      }

      &.status-waiting {
        background: rgba(78, 205, 196, 0.2);
        color: var(--color-secondary-dark);
      }

      &.status-confirmed {
        background: rgba(78, 205, 196, 0.15);
        color: var(--color-secondary-dark);
      }

      &.status-cancelled {
        background: var(--color-bg-tertiary);
        color: var(--color-text-tertiary);
      }

      &.status-refunded {
        background: var(--color-bg-tertiary);
        color: var(--color-text-tertiary);
      }
    }
  }

  .order-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 16px;
    border-top: 1px solid var(--color-border-light);

    .payment-info {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      color: var(--color-text-secondary);

      .divider {
        color: var(--color-border);
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

  &.btn-pay {
    background: var(--color-primary-gradient);
    color: white;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
    }
  }

  &.btn-cancel {
    background: transparent;
    border-color: var(--color-border);
    color: var(--color-text-secondary);

    &:hover {
      border-color: var(--color-error);
      color: var(--color-error);
    }
  }

  &.btn-refund {
    background: transparent;
    border-color: var(--color-secondary);
    color: var(--color-secondary);

    &:hover {
      background: var(--color-secondary);
      color: white;
    }
  }

  &.btn-review {
    background: transparent;
    border-color: var(--color-accent);
    color: #B8860B;

    &:hover {
      background: var(--color-accent);
      color: var(--color-text-primary);
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

.dialog-content {
  .info-row {
    display: flex;
    justify-content: space-between;
    padding: 12px 0;
    border-bottom: 1px solid var(--color-border-light);

    &:last-of-type {
      border-bottom: none;
    }

    .label {
      color: var(--color-text-tertiary);
      font-weight: 500;
    }

    .value {
      color: var(--color-text-primary);
      font-weight: 500;

      &.price-value {
        color: var(--color-primary);
        font-size: 20px;
        font-weight: 700;
      }
    }

    &.price {
      background: var(--color-bg-tertiary);
      padding: 16px;
      border-radius: var(--radius-md);
      margin: 8px 0;
    }
  }

  .payment-methods {
    margin-top: 16px;

    .label {
      display: block;
      margin-bottom: 12px;
      color: var(--color-text-tertiary);
      font-weight: 500;
    }

    .method-options {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .method-option {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px 16px;
      border: 2px solid var(--color-border);
      border-radius: var(--radius-md);
      cursor: pointer;
      transition: all var(--transition-fast);

      &:hover {
        border-color: var(--color-primary-light);
      }

      input[type="radio"]:checked + span {
        color: var(--color-primary);
        font-weight: 600;
      }
    }
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

      &.status-badge {
        padding: 4px 10px;
        border-radius: var(--radius-full);
        font-size: 12px;
      }
    }
  }
}

.review-content {
  .review-house {
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text-primary);
    text-align: center;
    padding: 16px 0;
    border-bottom: 1px solid var(--color-border-light);
    margin-bottom: 16px;
  }

  .rating-section {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px;
    background: var(--color-bg-tertiary);
    border-radius: var(--radius-md);
    margin-bottom: 16px;

    .rating-label {
      font-weight: 600;
      color: var(--color-text-primary);
    }
  }

  .input-section {
    position: relative;

    .review-textarea {
      width: 100%;
      padding: 16px;
      border: 2px solid var(--color-border);
      border-radius: var(--radius-lg);
      font-size: 14px;
      font-family: inherit;
      resize: none;
      min-height: 120px;
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

  .existing-review {
    .rating-display {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px;
      background: var(--color-bg-tertiary);
      border-radius: var(--radius-md);
      margin-bottom: 16px;

      .rating-label {
        font-weight: 600;
      }
    }

    .review-text {
      padding: 16px;
      background: var(--color-bg-tertiary);
      border-radius: var(--radius-md);
      line-height: 1.6;
      white-space: pre-line;
    }

    .review-time {
      text-align: center;
      color: var(--color-text-tertiary);
      font-size: 13px;
      margin: 12px 0;
    }

    .landlord-reply {
      padding: 12px 16px;
      background: rgba(78, 205, 196, 0.1);
      border-radius: var(--radius-md);
      border-left: 3px solid var(--color-secondary);

      .reply-label {
        font-weight: 600;
        color: var(--color-secondary-dark);
      }

      .reply-content {
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
  .my-orders-page {
    padding: 15px;
  }

  .order-content {
    flex-direction: column;
  }

  .order-footer {
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
