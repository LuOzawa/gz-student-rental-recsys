<template>
  <div class="appointments-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <span class="page-icon">📅</span>
        <h1 class="page-title">我的预约</h1>
      </div>
    </div>

    <!-- 状态筛选 -->
    <div class="filter-section">
      <div class="filter-tabs">
        <button
          v-for="status in statusFilters"
          :key="status.value"
          :class="['filter-tab', { active: filterStatus === status.value }]"
          @click="setStatusFilter(status.value)"
        >
          {{ status.label }}
        </button>
      </div>
    </div>

    <!-- 预约列表 -->
    <div v-if="appointments.length > 0" class="appointments-list">
      <div
        v-for="(appointment, index) in appointments"
        :key="appointment.id"
        class="appointment-card card-styled"
        :style="{ animationDelay: `${index * 50}ms` }"
      >
        <div class="appointment-header">
          <div class="house-info">
            <h3 class="house-title">{{ appointment.house?.title || '房源信息' }}</h3>
            <p class="house-address">📍 {{ appointment.house?.address }}</p>
          </div>
          <span :class="['status-badge', getStatusClass(appointment.status)]">
            {{ getStatusIcon(appointment.status) }} {{ getStatusText(appointment.status) }}
          </span>
        </div>

        <div class="appointment-details">
          <div class="detail-row">
            <span class="detail-icon">📆</span>
            <span class="detail-label">预约日期</span>
            <span class="detail-value">{{ appointment.appointmentDate }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-icon">⏰</span>
            <span class="detail-label">预约时间</span>
            <span class="detail-value">{{ appointment.appointmentTime }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-icon">📞</span>
            <span class="detail-label">联系电话</span>
            <span class="detail-value">{{ appointment.contactPhone }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-icon">👤</span>
            <span class="detail-label">房东</span>
            <span class="detail-value">{{ appointment.landlord?.name || appointment.landlord?.username }}</span>
          </div>
          <div v-if="appointment.message" class="detail-row detail-row-full">
            <span class="detail-icon">💬</span>
            <span class="detail-label">留言</span>
            <span class="detail-value">{{ appointment.message }}</span>
          </div>
          <div v-if="appointment.rejectReason" class="detail-row detail-row-full">
            <span class="detail-icon">❌</span>
            <span class="detail-label">拒绝原因</span>
            <span class="detail-value detail-reject">{{ appointment.rejectReason }}</span>
          </div>
        </div>

        <div class="appointment-footer">
          <span class="create-time">创建于 {{ appointment.createTime }}</span>
          <div class="actions">
            <button
              v-if="appointment.status === 1"
              class="btn-action btn-complete"
              @click="handleComplete(appointment.id)"
            >
              ✅ 完成看房
            </button>
            <button
              v-if="appointment.status === 0 || appointment.status === 1"
              class="btn-action btn-cancel"
              @click="handleCancel(appointment.id)"
            >
              🚫 取消预约
            </button>
            <button
              class="btn-action btn-view"
              @click="viewHouse(appointment.houseId)"
            >
              🏠 查看房源
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <div class="empty-icon">📭</div>
      <h3 class="empty-title">暂无预约记录</h3>
      <p class="empty-desc">预约心仪房源，安排实地看房</p>
      <button class="btn-browse" @click="$router.push('/houses')">
        🏘️ 去浏览房源
      </button>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination-section">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchAppointments"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()

const appointments = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref('')

const statusFilters = [
  { label: '📋 全部', value: '' },
  { label: '⏳ 待确认', value: 0 },
  { label: '✓ 已确认', value: 1 },
  { label: '✗ 已拒绝', value: 2 },
  { label: '✔ 已完成', value: 3 },
  { label: '🚫 已取消', value: 4 }
]

const setStatusFilter = (status) => {
  filterStatus.value = status
  currentPage.value = 1
  fetchAppointments()
}

// 获取预约列表
const fetchAppointments = async () => {
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value
    }
    if (filterStatus.value !== '') {
      params.status = filterStatus.value
    }

    const res = await request.get('/appointments/tenant', params)
    appointments.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取预约列表失败:', error)
  }
}

// 取消预约
const handleCancel = async (id) => {
  try {
    await ElMessageBox.confirm('确定要取消该预约吗？', '提示', {
      type: 'warning'
    })

    await request.put(`/appointments/${id}/cancel`)
    ElMessage.success('预约已取消')
    fetchAppointments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消预约失败:', error)
    }
  }
}

// 完成看房
const handleComplete = async (id) => {
  try {
    await ElMessageBox.confirm('确认已完成看房吗？', '提示', {
      type: 'info'
    })

    await request.put(`/appointments/${id}/complete`)
    ElMessage.success('预约已标记为完成')
    fetchAppointments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('完成看房失败:', error)
    }
  }
}

// 查看房源
const viewHouse = (houseId) => {
  router.push(`/houses/${houseId}`)
}

// 获取状态样式类
const getStatusClass = (status) => {
  const classMap = {
    0: 'status-pending',
    1: 'status-confirmed',
    2: 'status-rejected',
    3: 'status-completed',
    4: 'status-cancelled'
  }
  return classMap[status] || ''
}

// 获取状态图标
const getStatusIcon = (status) => {
  const iconMap = {
    0: '⏳',
    1: '✓',
    2: '✗',
    3: '✔',
    4: '🚫'
  }
  return iconMap[status] || ''
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

onMounted(() => {
  fetchAppointments()
})
</script>

<style lang="scss" scoped>
.appointments-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
}

// 页面头部
.page-header {
  margin-bottom: 24px;

  .header-content {
    display: flex;
    align-items: center;
    gap: 12px;

    .page-icon {
      font-size: 36px;
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
}

// 筛选区域
.filter-section {
  margin-bottom: 20px;

  .filter-tabs {
    display: flex;
    gap: 10px;
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
    }

    .filter-tab {
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

// 预约列表
.appointments-list {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .appointment-card {
    padding: 20px;
    animation: fadeInUp 0.4s ease-out backwards;
  }
}

.appointment-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border-light);

  .house-info {
    flex: 1;
    padding-right: 16px;

    .house-title {
      font-size: 18px;
      font-weight: 700;
      color: var(--color-text-primary);
      margin: 0 0 6px 0;
    }

    .house-address {
      color: var(--color-text-tertiary);
      font-size: 14px;
      margin: 0;
    }
  }

  .status-badge {
    flex-shrink: 0;
    padding: 6px 14px;
    border-radius: var(--radius-full);
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;

    &.status-pending {
      background: rgba(255, 230, 109, 0.2);
      color: #B8860B;
    }

    &.status-confirmed {
      background: rgba(78, 205, 196, 0.2);
      color: var(--color-secondary-dark);
    }

    &.status-rejected {
      background: rgba(255, 107, 107, 0.15);
      color: var(--color-error);
    }

    &.status-completed {
      background: rgba(78, 205, 196, 0.15);
      color: var(--color-secondary-dark);
    }

    &.status-cancelled {
      background: var(--color-bg-tertiary);
      color: var(--color-text-tertiary);
    }
  }
}

.appointment-details {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;

  .detail-row {
    display: flex;
    align-items: center;
    gap: 8px;

    &.detail-row-full {
      grid-column: 1 / -1;
    }

    .detail-icon {
      font-size: 16px;
    }

    .detail-label {
      font-weight: 600;
      color: var(--color-text-secondary);
      font-size: 14px;
    }

    .detail-value {
      color: var(--color-text-primary);
      font-size: 14px;

      &.detail-reject {
        color: var(--color-error);
        font-weight: 500;
      }
    }
  }
}

.appointment-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  border-top: 1px solid var(--color-border-light);

  .create-time {
    font-size: 13px;
    color: var(--color-text-tertiary);
  }

  .actions {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
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

  &.btn-complete {
    background: var(--color-secondary-gradient);
    color: white;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(78, 205, 196, 0.3);
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

  &.btn-view {
    background: var(--color-primary-gradient);
    color: white;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
    }
  }
}

// 空状态
.empty-state {
  text-align: center;
  padding: 80px 20px;

  .empty-icon {
    font-size: 80px;
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

// 响应式
@media (max-width: 768px) {
  .appointments-page {
    padding: 15px;
  }

  .filter-section {
    .filter-tabs {
      flex-wrap: nowrap;
    }
  }

  .appointment-header {
    flex-direction: column;
    gap: 12px;

    .house-info {
      padding-right: 0;
    }

    .status-badge {
      align-self: flex-start;
    }
  }

  .appointment-details {
    grid-template-columns: 1fr;
  }

  .appointment-footer {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;

    .actions {
      justify-content: stretch;

      .btn-action {
        flex: 1;
        text-align: center;
      }
    }
  }
}
</style>
