<template>
  <div class="payment-container">
    <div class="payment-card">
      <h2 class="payment-title">订单支付</h2>
      
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-loading></el-loading>
      </div>
      
      <!-- 未找到订单 -->
      <div v-else-if="!order" class="not-found">
        <div class="not-found-content">
          <span class="not-found-icon">📄</span>
          <h3>未找到该订单</h3>
          <p>订单可能已被删除或不存在</p>
          <button class="btn-primary" @click="goBack">返回订单列表</button>
        </div>
      </div>
      
      <!-- 支付内容 -->
      <div v-else class="payment-content">
        <!-- 订单信息 -->
        <div class="order-info">
          <h3>订单信息</h3>
          <div class="info-row">
            <span class="label">订单编号：</span>
            <span class="value">{{ order.orderNo }}</span>
          </div>
          <div class="info-row">
            <span class="label">房屋地址：</span>
            <span class="value">{{ order.house?.address }}</span>
          </div>
          <div class="info-row">
            <span class="label">房东：</span>
            <span class="value">{{ order.landlord?.username }}</span>
          </div>
          <div class="info-row">
            <span class="label">创建时间：</span>
            <span class="value">{{ formatDateTime(order.createTime) }}</span>
          </div>
        </div>
        
        <!-- 费用明细 -->
        <div class="fee-details">
          <h3>费用明细</h3>
          <div class="fee-row">
            <span class="label">月租金：</span>
            <span class="value">¥{{ order.amount }}</span>
          </div>
          <div class="fee-row">
            <span class="label">押金：</span>
            <span class="value">¥{{ order.deposit }}</span>
          </div>
          <div class="divider"></div>
          <div class="fee-row total">
            <span class="label">应付总额：</span>
            <span class="value amount">¥{{ calculateTotal() }}</span>
          </div>
        </div>
        
        <!-- 支付方式 -->
        <div class="payment-method">
          <h3>支付方式</h3>
          <el-radio-group v-model="selectedPaymentMethod">
            <el-radio value="alipay">支付宝</el-radio>
            <el-radio value="wechat">微信支付</el-radio>
            <el-radio value="bank">银行卡</el-radio>
          </el-radio-group>
        </div>
        
        <!-- 操作按钮 -->
        <div class="payment-actions">
          <el-button @click="goBack" size="large">取消</el-button>
          <el-button 
            type="primary" 
            @click="handlePayment" 
            size="large" 
            :loading="paying"
          >
            立即支付
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const orderId = route.params.id

const loading = ref(true)
const paying = ref(false)
const order = ref(null)
const selectedPaymentMethod = ref('alipay')

// 获取订单详情
const fetchOrderDetail = async () => {
  try {
    loading.value = true
    console.log('Fetching order detail for ID:', orderId)
    const response = await request.get(`/orders/${orderId}`)
    console.log('Order response:', response)
    order.value = response
  } catch (error) {
    ElMessage.error('获取订单详情失败')
    console.error('获取订单详情失败:', error)
  } finally {
    loading.value = false
  }
}

// 计算总金额
const calculateTotal = () => {
  if (!order.value) return '0.00'
  const total = Number(order.value.amount || 0) + Number(order.value.deposit || 0)
  return total.toFixed(2)
}

// 处理支付
const handlePayment = async () => {
  if (!selectedPaymentMethod.value) {
    ElMessage.warning('请选择支付方式')
    return
  }
  
  paying.value = true
  try {
    console.log('Processing payment for order:', orderId, 'with method:', selectedPaymentMethod.value)
    const response = await request.put(`/orders/${orderId}/pay`, null, {
      params: { paymentMethod: selectedPaymentMethod.value }
    })
    console.log('Payment response:', response)
    
    ElMessage.success('支付成功！')
    // 跳转到订单详情页
    router.push('/my-orders')
  } catch (error) {
    ElMessage.error('支付失败')
    console.error('支付失败:', error)
  } finally {
    paying.value = false
  }
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 返回上一页
const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchOrderDetail()
})
</script>

<style scoped>
.payment-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.payment-card {
  width: 100%;
  max-width: 800px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  padding: 30px;
}

.payment-title {
  text-align: center;
  margin-bottom: 30px;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.not-found {
  text-align: center;
  padding: 40px;
}

.not-found-icon {
  font-size: 64px;
  margin-bottom: 20px;
  display: block;
}

.payment-content > div {
  margin-bottom: 30px;
}

h3 {
  margin-bottom: 15px;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.order-info,
.fee-details,
.payment-method {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 20px;
}

.info-row,
.fee-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-row:last-child,
.fee-row:last-child {
  border-bottom: none;
}

.label {
  color: #606266;
  font-size: 14px;
}

.value {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.divider {
  height: 1px;
  background-color: #ebeef5;
  margin: 10px 0;
}

.fee-row.total {
  border-top: 2px solid #ebeef5;
  margin-top: 10px;
  padding-top: 15px;
}

.fee-row.total .label,
.fee-row.total .amount {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
}

.payment-method .el-radio-group {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.payment-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
}

.payment-actions .el-button {
  min-width: 120px;
}

.btn-primary {
  background-color: #409eff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-primary:hover {
  background-color: #337ecc;
}
</style>