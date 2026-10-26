<template>
  <div class="dashboard">
    <!-- 欢迎卡片 -->
    <el-card class="welcome-card">
      <template #header>
        <div class="welcome-header">
          <el-avatar :size="64" :src="avatarUrl">
            {{ userInfo?.name?.charAt(0) }}
          </el-avatar>
          <div class="welcome-info">
            <h2>欢迎回来, {{ userInfo?.name || userInfo?.username }}</h2>
            <p>{{ currentTime }}</p>
          </div>
        </div>
      </template>
      <div class="role-info">
        <el-tag :type="getRoleTagType(userInfo?.roleCode)">{{ roleLabel }}</el-tag>
      </div>
    </el-card>
    
    <!-- 数据统计卡片 -->
    <div class="stat-cards">
      <el-row :gutter="20">
        <!-- 用户统计卡片 - 仅管理员可见 -->
        <el-col :xs="24" :sm="12" :md="6" v-if="isAdmin">
          <el-card class="stat-card user-card" shadow="hover">
            <div class="stat-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">用户总数</div>
              <div class="stat-value">{{ statistics.userCount || 0 }}</div>
              <div class="stat-desc">
                <span>租客: {{ statistics.tenantCount || 0 }}</span>
                <span>房东: {{ statistics.landlordCount || 0 }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="isAdmin ? 6 : 8">
          <el-card class="stat-card house-card" shadow="hover">
            <div class="stat-icon">
              <el-icon><House /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">{{ isAdmin ? '房源总数' : '我的房源' }}</div>
              <div class="stat-value">{{ statistics.houseCount || 0 }}</div>
              <div class="stat-desc">
                <span>可租: {{ statistics.availableHouseCount || 0 }}</span>
                <span>已租: {{ statistics.rentedHouseCount || 0 }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="isAdmin ? 6 : 8">
          <el-card class="stat-card order-card" shadow="hover">
            <div class="stat-icon">
              <el-icon><Tickets /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">{{ isAdmin ? '订单总数' : '我的订单' }}</div>
              <div class="stat-value">{{ statistics.orderCount || 0 }}</div>
              <div class="stat-desc">
                <span>本月: {{ statistics.monthOrderCount || 0 }}</span>
                <span>待支付: {{ statistics.pendingOrderCount || 0 }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="isAdmin ? 6 : 8">
          <el-card class="stat-card transaction-card" shadow="hover">
            <div class="stat-icon">
              <el-icon><Money /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">{{ isAdmin ? '交易总额' : '我的收入' }}</div>
              <div class="stat-value">¥ {{ statistics.totalAmount || '0.00' }}</div>
              <div class="stat-desc">
                <span>本月: ¥ {{ statistics.monthAmount || '0.00' }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
    
    <!-- 租赁统计图表 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>月度订单统计</span>
            </div>
          </template>
          <div class="chart-container" v-loading="loading.orderChart">
            <div id="orderChart" class="chart"></div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>房源类型分布</span>
            </div>
          </template>
          <div class="chart-container" v-loading="loading.houseChart">
            <div id="houseChart" class="chart"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
 
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { User, House, Tickets, Money } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import { 
  TitleComponent, 
  TooltipComponent, 
  GridComponent, 
  LegendComponent 
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 注册 ECharts 组件
echarts.use([
  TitleComponent, 
  TooltipComponent, 
  GridComponent, 
  LegendComponent,
  BarChart,
  PieChart,
  CanvasRenderer
])

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const baseAPI = import.meta.env.VITE_BASE_API || '/api'

// 判断是否为管理员
const isAdmin = computed(() => userInfo.value?.roleCode === 'ADMIN')

// 加载状态
const loading = ref({
  statistics: true,
  orderChart: true,
  houseChart: true,
  recentLeases: true
})

// 统计数据
const statistics = ref({
  userCount: 0,
  tenantCount: 0,
  landlordCount: 0,
  houseCount: 0,
  availableHouseCount: 0,
  rentedHouseCount: 0,
  orderCount: 0,
  monthOrderCount: 0,
  pendingOrderCount: 0,
  totalAmount: '0.00',
  monthAmount: '0.00'
})

// 最近租赁记录
const recentLeases = ref([])

// 角色标签
const roleLabel = computed(() => {
  const roleMap = {
    'ADMIN': '系统管理员',
    'LANDLORD': '房东',
    'TENANT': '租客',
    'USER': '普通用户'
  }
  return roleMap[userInfo.value?.roleCode] || '未知角色'
})

// 获取角色标签类型
const getRoleTagType = (roleCode) => {
  const typeMap = {
    'ADMIN': 'danger',
    'LANDLORD': 'warning',
    'TENANT': 'success',
    'USER': 'info'
  }
  return typeMap[roleCode] || 'info'
}

// 获取租赁状态文本
const getLeaseStatusText = (status) => {
  const statusMap = {
    0: '已取消',
    1: '租赁中',
    2: '已结束',
    3: '已退租'
  }
  return statusMap[status] || '未知状态'
}

// 获取租赁状态标签类型
const getLeaseStatusType = (status) => {
  const typeMap = {
    0: 'info',
    1: 'success',
    2: 'warning',
    3: 'danger'
  }
  return typeMap[status] || 'info'
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const avatarUrl = computed(() => {
  return userInfo.value?.avatar ? baseAPI + userInfo.value.avatar : '';
})

// 当前时间
const currentTime = ref('')
let timeInterval = null // 保存定时器引用

const updateTime = () => {
  const now = new Date()
  const options = { 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric', 
    weekday: 'long',
    hour: '2-digit',
    minute: '2-digit'
  }
  currentTime.value = now.toLocaleDateString('zh-CN', options)
}

// 获取统计数据
const fetchStatistics = async () => {
  loading.value.statistics = true
  try {
    await request.get('/statistics/dashboard', {}, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        statistics.value = res
      }
    })
  } catch (error) {
    console.error('获取统计数据失败:', error)
  } finally {
    loading.value.statistics = false
  }
}

// 获取最近租赁记录
const fetchRecentLeases = async () => {
  loading.value.recentLeases = true
  try {
    await request.get('/statistics/recent-leases', {}, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        recentLeases.value = res
      }
    })
  } catch (error) {
    console.error('获取最近租赁记录失败:', error)
  } finally {
    loading.value.recentLeases = false
  }
}

// 初始化订单图表
const initOrderChart = async () => {
  loading.value.orderChart = true
  try {
    const res = await request.get('/statistics/monthly-orders', {}, {
      showDefaultMsg: false
    })

    const chartDom = document.getElementById('orderChart')
    if (!chartDom) return

    const myChart = echarts.init(chartDom, null, { renderer: 'canvas' })
    const months = res.months || []
    const orderCounts = res.orderCounts || []

    const option = {
      title: {
        text: isAdmin.value ? '近6个月订单数量' : '近6个月我的订单',
        left: 'center',
        textStyle: {
          color: '#1E293B'
        }
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: months,
        axisLine: {
          lineStyle: {
            color: '#E9ECEF'
          }
        },
        axisLabel: {
          color: '#64748B'
        }
      },
      yAxis: {
        type: 'value',
        axisLine: {
          show: false
        },
        axisLabel: {
          color: '#64748B'
        },
        splitLine: {
          lineStyle: {
            color: '#E9ECEF'
          }
        }
      },
      series: [
        {
          name: '订单数量',
          type: 'bar',
          data: orderCounts,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#3B82F6' },
              { offset: 1, color: '#93C5FD' }
            ])
          },
          emphasis: {
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#1D4ED8' },
                { offset: 1, color: '#3B82F6' }
              ])
            }
          }
        }
      ]
    }
    
    myChart.setOption(option)
    window.addEventListener('resize', () => myChart.resize())
    
  } catch (error) {
    console.error('获取月度订单数据失败:', error)
  } finally {
    loading.value.orderChart = false
  }
}

// 初始化房源类型图表
const initHouseChart = async () => {
  loading.value.houseChart = true
  try {
    const res = await request.get('/statistics/house-types', {}, {
      showDefaultMsg: false
    })

    const chartDom = document.getElementById('houseChart')
    if (!chartDom) return

    const myChart = echarts.init(chartDom, null, { renderer: 'canvas' })
    const data = res.map(item => ({
      name: item.name,
      value: item.count
    }))
    
    const option = {
      title: {
        text: isAdmin.value ? '房源类型分布' : '我的房源类型分布',
        left: 'center',
        textStyle: {
          color: '#1E293B'
        }
      },
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'horizontal',
        bottom: 10,
        data: data.map(item => item.name),
        textStyle: {
          color: '#64748B'
        }
      },
      series: [
        {
          name: '房源类型',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 6,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: '18',
              fontWeight: 'bold',
              formatter: '{b}: {c} ({d}%)'
            }
          },
          labelLine: {
            show: false
          },
          data: data
        }
      ],
      color: ['#3B82F6', '#10B981', '#F97316', '#8B5CF6', '#EC4899']
    }
    
    myChart.setOption(option)
    window.addEventListener('resize', () => myChart.resize())
    
  } catch (error) {
    console.error('获取房源类型数据失败:', error)
  } finally {
    loading.value.houseChart = false
  }
}

// 跳转到租赁管理页面
const goToLeaseManagement = () => {
  router.push('/back/lease')
}

onMounted(() => {
  updateTime()
  // 每分钟更新一次时间
  timeInterval = setInterval(updateTime, 60000)
  
  // 获取数据
  fetchStatistics()
  fetchRecentLeases()
  
  // 初始化图表
  setTimeout(() => {
    initOrderChart()
    initHouseChart()
  }, 300)
})

onUnmounted(() => {
  // 清除定时器
  if (timeInterval) {
    clearInterval(timeInterval)
    timeInterval = null
  }
  
  // 销毁图表实例
  const charts = ['orderChart', 'houseChart']
  charts.forEach(id => {
    const chartDom = document.getElementById(id)
    if (chartDom) {
      echarts.getInstanceByDom(chartDom)?.dispose()
    }
  })
})
</script>

<style lang="scss" scoped>
.dashboard {
  .welcome-card {
    margin-bottom: 20px;
    transition: all 0.3s ease;
    background: linear-gradient(to right, #EFF6FF, #F8FAFC);
    border-radius: 8px;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
    }
    
    .welcome-header {
      display: flex;
      align-items: center;
      gap: 20px;
      
      .el-avatar {
        transition: transform 0.3s ease;
        border: 3px solid #FFFFFF;
        box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
        
        &:hover {
          transform: scale(1.1);
        }
      }
      
      .welcome-info {
        h2 {
          margin: 0 0 8px 0;
          font-size: 24px;
          background: linear-gradient(to right, #3B82F6, #1D4ED8);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
        }
        p {
          margin: 0;
          color: #64748B;
        }
      }
    }
    
    .role-info {
      margin-top: 16px;
    }
  }
  
  .stat-cards {
    margin-bottom: 20px;
    
    .stat-card {
      // height: 120px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 8px;
      transition: all 0.3s ease;
      // margin-bottom: 20px;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
      }
      
      .stat-icon {
        width: 64px;
        height: 64px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 8px;
       
        
        .el-icon {
          font-size: 30px;
          color: #FFFFFF;
        }

      }
      
      .stat-info {
        flex: 1;
        
        .stat-title {
          font-size: 14px;
          color: #64748B;
          margin-bottom: 8px;
        }
        
        .stat-value {
          font-size: 24px;
          font-weight: 600;
          margin-bottom: 8px;
        }
        
        .stat-desc {
          font-size: 12px;
          color: #94A3B8;
          display: flex;
          justify-content: space-between;
        }
      }
      
      &.user-card .stat-icon {
        background: linear-gradient(135deg, #3B82F6, #1D4ED8);
      }
      
      &.house-card .stat-icon {
        background: linear-gradient(135deg, #10B981, #059669);
      }
      
      &.order-card .stat-icon {
        background: linear-gradient(135deg, #F97316, #EA580C);
      }
      
      &.transaction-card .stat-icon {
        background: linear-gradient(135deg, #8B5CF6, #7C3AED);
      }
    }
  }
  
  .chart-row {
    margin-bottom: 20px;
  }
  
  .chart-card, .recent-card {
    margin-bottom: 20px;
    border-radius: 8px;
    transition: all 0.3s ease;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
    }
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      span {
        font-size: 16px;
        font-weight: 600;
        color: #1D4ED8;
      }
    }
    
    .chart-container {
      height: 300px;
      
      .chart {
        width: 100%;
        height: 100%;
      }
    }
  }
  
  :deep(.el-table) {
    --el-table-header-bg-color: #F8FAFC;
    --el-table-border-color: #E9ECEF;
    --el-table-row-hover-bg-color: #EFF6FF;
    
    .el-table__header th {
      font-weight: 600;
      color: #1E293B;
    }
  }
}

@media (max-width: 768px) {
  .dashboard {
    .welcome-header {
      flex-direction: column;
      align-items: center;
      text-align: center;
      
      .welcome-info h2 {
        font-size: 20px;
      }
    }
    
    .stat-card {
      height: auto;
      padding: 16px;
      
      .stat-icon {
        width: 48px;
        height: 48px;
        
        .el-icon {
          font-size: 24px;
        }
      }
      
      .stat-value {
        font-size: 20px;
      }
    }
    
    .chart-container {
      height: 250px !important;
    }
  }
}
</style> 