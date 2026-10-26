<template>
  <div class="landlord-statistics-container">
    <el-card class="statistics-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>数据统计</h2>
          <el-button type="primary" :icon="Refresh" @click="refreshData">刷新数据</el-button>
        </div>
      </template>

      <div v-loading="loading">
        <!-- 统计卡片 -->
        <el-row :gutter="20" class="stats-cards">
          <el-col :xs="24" :sm="12" :md="6">
            <div class="stat-card">
              <div class="stat-icon" style="background: #ecf5ff; color: #409eff">
                <el-icon :size="24"><House /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-label">房源总数</div>
                <div class="stat-value">{{ statistics.totalHouses }}</div>
              </div>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <div class="stat-card">
              <div class="stat-icon" style="background: #f0f9ff; color: #67c23a">
                <el-icon :size="24"><View /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-label">总浏览量</div>
                <div class="stat-value">{{ formatNumber(statistics.totalViews) }}</div>
              </div>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <div class="stat-card">
              <div class="stat-icon" style="background: #fef0f0; color: #f56c6c">
                <el-icon :size="24"><Collection /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-label">总收藏数</div>
                <div class="stat-value">{{ formatNumber(statistics.totalFavorites) }}</div>
              </div>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <div class="stat-card">
              <div class="stat-icon" style="background: #fff7e6; color: #e6a23c">
                <el-icon :size="24"><Calendar /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-label">总预约数</div>
                <div class="stat-value">{{ statistics.totalAppointments }}</div>
              </div>
            </div>
          </el-col>
        </el-row>

        <!-- 房源状态分布 -->
        <el-row :gutter="20" class="charts-row">
          <el-col :xs="24" :md="12">
            <div class="chart-card">
              <h3>房源状态分布</h3>
              <div class="chart-placeholder" v-if="statistics.houseStatus">
                <div class="status-bars">
                  <div class="status-bar" v-for="(value, key) in statusConfig" :key="key">
                    <div class="status-label">{{ value.label }}</div>
                    <div class="status-progress">
                      <div
                        class="status-fill"
                        :style="{
                          width: getPercentage(statistics.houseStatus[key] || 0, statistics.totalHouses) + '%',
                          backgroundColor: value.color
                        }"
                      ></div>
                      <span class="status-count">{{ statistics.houseStatus[key] || 0 }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无数据" />
            </div>
          </el-col>

          <el-col :xs="24" :md="12">
            <div class="chart-card">
              <h3>热门房源 TOP 5</h3>
              <div class="top-houses-list" v-if="statistics.topHouses && statistics.topHouses.length > 0">
                <div
                  v-for="(house, index) in statistics.topHouses"
                  :key="house.id"
                  class="top-house-item"
                  @click="$router.push(`/houses/${house.id}`)"
                >
                  <div class="house-rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
                  <div class="house-info">
                    <div class="house-title">{{ house.title }}</div>
                    <div class="house-stats">
                      <span><el-icon><View /></el-icon> {{ house.viewCount || 0 }}</span>
                      <span><el-icon><Collection /></el-icon> {{ house.favoriteCount || 0 }}</span>
                      <span class="house-price">¥{{ house.price }}/月</span>
                    </div>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无数据" />
            </div>
          </el-col>
        </el-row>

        <!-- 最近预约 -->
        <div class="recent-section">
          <h3>最近预约记录</h3>
          <el-table :data="statistics.recentAppointments" stripe v-if="statistics.recentAppointments && statistics.recentAppointments.length > 0">
            <el-table-column prop="houseTitle" label="房源" />
            <el-table-column prop="tenantName" label="预约人" />
            <el-table-column prop="appointmentTime" label="预约时间" width="180" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无预约记录" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { House, View, Collection, Calendar, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)

const statistics = reactive({
  totalHouses: 0,
  totalViews: 0,
  totalFavorites: 0,
  totalAppointments: 0,
  houseStatus: {},
  topHouses: [],
  recentAppointments: []
})

const statusConfig = {
  0: { label: '已下架', color: '#909399' },
  1: { label: '待出租', color: '#67c23a' },
  2: { label: '已出租', color: '#e6a23c' }
}

// 获取统计数据
const fetchStatistics = async () => {
  loading.value = true
  try {
    // 获取房东的房源列表统计数据
    await request.get('/houses/landlord', {
      currentPage: 1,
      size: 100
    }, {
      onSuccess: (res) => {
        const houses = res.records || []

        // 计算统计数据
        statistics.totalHouses = houses.length
        statistics.totalViews = houses.reduce((sum, h) => sum + (h.viewCount || 0), 0)
        statistics.totalFavorites = houses.reduce((sum, h) => sum + (h.favoriteCount || 0), 0)

        // 统计房源状态
        statistics.houseStatus = {}
        houses.forEach(h => {
          const status = h.status
          statistics.houseStatus[status] = (statistics.houseStatus[status] || 0) + 1
        })

        // 获取热门房源（按浏览量排序）
        statistics.topHouses = [...houses]
          .sort((a, b) => (b.viewCount || 0) - (a.viewCount || 0))
          .slice(0, 5)

        // 获取最近预约（需要额外的API调用）
        fetchRecentAppointments()
      }
    })
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  } finally {
    loading.value = false
  }
}

// 获取最近预约记录
const fetchRecentAppointments = async () => {
  try {
    await request.get('/appointments/landlord', {
      currentPage: 1,
      size: 5
    }, {
      onSuccess: (res) => {
        statistics.totalAppointments = res.total || 0
        statistics.recentAppointments = (res.records || []).map(a => ({
          houseTitle: a.houseTitle || '未知房源',
          tenantName: a.tenantName || '租客',
          appointmentTime: a.appointmentTime,
          status: a.status
        }))
      }
    })
  } catch (error) {
    console.error('获取预约记录失败:', error)
  }
}

// 格式化数字
const formatNumber = (num) => {
  if (!num) return 0
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toLocaleString()
}

// 计算百分比
const getPercentage = (value, total) => {
  if (!total) return 0
  return Math.round((value / total) * 100)
}

// 获取状态类型
const getStatusType = (status) => {
  const types = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return types[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const texts = { 0: '待确认', 1: '已确认', 2: '已完成', 3: '已拒绝' }
  return texts[status] || '未知'
}

// 刷新数据
const refreshData = () => {
  fetchStatistics()
  ElMessage.success('数据已刷新')
}

onMounted(() => {
  fetchStatistics()
})
</script>

<style lang="scss" scoped>
.landlord-statistics-container {
  padding: 20px;
}

.statistics-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    h2 {
      margin: 0;
      font-size: 18px;
      color: #303133;
    }
  }
}

.stats-cards {
  margin-bottom: 30px;

  .stat-card {
    display: flex;
    align-items: center;
    padding: 20px;
    background: #f8fafc;
    border-radius: 8px;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .stat-icon {
      width: 56px;
      height: 56px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 12px;
      flex-shrink: 0;
    }

    .stat-content {
      margin-left: 16px;

      .stat-label {
        font-size: 14px;
        color: #909399;
        margin-bottom: 4px;
      }

      .stat-value {
        font-size: 24px;
        font-weight: bold;
        color: #303133;
      }
    }
  }
}

.charts-row {
  margin-bottom: 30px;

  .chart-card {
    background: #f8fafc;
    padding: 20px;
    border-radius: 8px;

    h3 {
      margin: 0 0 20px;
      font-size: 16px;
      color: #303133;
    }
  }
}

.status-bars {
  .status-bar {
    display: flex;
    align-items: center;
    margin-bottom: 15px;

    &:last-child {
      margin-bottom: 0;
    }

    .status-label {
      width: 80px;
      font-size: 14px;
      color: #606266;
    }

    .status-progress {
      flex: 1;
      height: 24px;
      background: #e4e7ed;
      border-radius: 4px;
      position: relative;
      overflow: hidden;

      .status-fill {
        height: 100%;
        transition: width 0.5s ease;
      }

      .status-count {
        position: absolute;
        right: 10px;
        top: 50%;
        transform: translateY(-50%);
        font-size: 12px;
        font-weight: bold;
        color: #303133;
      }
    }
  }
}

.top-houses-list {
  .top-house-item {
    display: flex;
    align-items: center;
    padding: 12px;
    background: white;
    border-radius: 8px;
    margin-bottom: 10px;
    cursor: pointer;
    transition: all 0.2s;

    &:last-child {
      margin-bottom: 0;
    }

    &:hover {
      background: #ecf5ff;
      transform: translateX(4px);
    }

    .house-rank {
      width: 28px;
      height: 28px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      font-weight: bold;
      flex-shrink: 0;

      &.rank-1 { background: #ffd700; color: white; }
      &.rank-2 { background: #c0c0c0; color: white; }
      &.rank-3 { background: #cd7f32; color: white; }
      &:not(.rank-1):not(.rank-2):not(.rank-3) { background: #e4e7ed; color: #909399; }
    }

    .house-info {
      flex: 1;
      margin-left: 12px;
      min-width: 0;

      .house-title {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        margin-bottom: 4px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .house-stats {
        display: flex;
        align-items: center;
        gap: 12px;
        font-size: 12px;
        color: #909399;

        .el-icon {
          margin-right: 2px;
        }

        .house-price {
          margin-left: auto;
          color: #f56c6c;
          font-weight: 500;
        }
      }
    }
  }
}

.recent-section {
  background: #f8fafc;
  padding: 20px;
  border-radius: 8px;

  h3 {
    margin: 0 0 15px;
    font-size: 16px;
    color: #303133;
  }
}
</style>
