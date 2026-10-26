<template>
  <div class="house-list-container">
    <!-- 搜索横幅 -->
    <div class="search-banner">
      <div class="banner-decoration">
        <div class="deco-blob blob-1"></div>
        <div class="deco-blob blob-2"></div>
      </div>
      <div class="banner-content">
        <div class="banner-badge">
          <span>🏘️</span>
          <span>房源搜索</span>
        </div>
        <h1>找到你的理想居所</h1>
        <p class="banner-subtitle">海量优质房源，覆盖广州各大高校周边</p>
        <div class="search-box">
          <el-input
            v-model="searchForm.title"
            placeholder="搜索关键词、小区名、地址..."
            clearable
            @keyup.enter="handleSearch"
            class="search-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <button class="search-btn" @click="handleSearch">
            <span>搜索房源</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 筛选容器 -->
    <div class="filter-container">
      <div class="filter-card">
        <!-- 基础筛选 -->
        <div class="filter-section">
          <div class="filter-item">
            <span class="filter-label">房屋类型</span>
            <div class="filter-options">
              <button
                v-for="item in [{id: undefined, name: '全部'}, ...houseTypeOptions]"
                :key="item.id"
                class="filter-option-btn"
                :class="{ active: searchForm.typeId === item.id }"
                @click="selectType(item.id)"
              >
                {{ item.name }}
              </button>
            </div>
          </div>
          <div class="filter-item">
            <span class="filter-label">价格区间</span>
            <div class="filter-options">
              <button
                v-for="(option, index) in priceQuickOptions"
                :key="index"
                class="filter-option-btn"
                :class="{ active: isPriceOptionActive(index) }"
                @click="selectPriceOption(index)"
              >
                {{ option.label }}
              </button>
              <div class="price-input-group">
                <el-input-number
                  v-model="minPriceTemp"
                  :min="0"
                  :precision="0"
                  :step="100"
                  placeholder="最低"
                  @change="handleMinPriceChange"
                />
                <span class="range-separator">~</span>
                <el-input-number
                  v-model="maxPriceTemp"
                  :min="0"
                  :precision="0"
                  :step="100"
                  placeholder="最高"
                  @change="handleMaxPriceChange"
                />
              </div>
            </div>
          </div>
          <div class="filter-item">
            <span class="filter-label">装修情况</span>
            <div class="filter-options">
              <button
                v-for="item in decorationOptions"
                :key="item.value"
                class="filter-option-btn"
                :class="{ active: searchForm.decoration === item.value }"
                @click="selectDecoration(item.value)"
              >
                {{ item.label }}
              </button>
            </div>
          </div>
          <div class="filter-item">
            <span class="filter-label">朝向</span>
            <div class="filter-options">
              <button
                v-for="item in orientationOptions"
                :key="item.value"
                class="filter-option-btn"
                :class="{ active: searchForm.orientation === item.value }"
                @click="selectOrientation(item.value)"
              >
                {{ item.label }}
              </button>
            </div>
          </div>
          <div class="filter-item">
            <span class="filter-label">建筑类型</span>
            <div class="filter-options">
              <button
                v-for="item in buildingTypeOptions"
                :key="item.value"
                class="filter-option-btn"
                :class="{ active: searchForm.buildingType === item.value }"
                @click="selectBuildingType(item.value)"
              >
                {{ item.label }}
              </button>
            </div>
          </div>
          <div class="filter-item">
            <span class="filter-label">付款方式</span>
            <div class="filter-options">
              <button
                v-for="item in paymentOptions"
                :key="item.value"
                class="filter-option-btn"
                :class="{ active: searchForm.paymentMethod === item.value }"
                @click="selectPaymentMethod(item.value)"
              >
                {{ item.label }}
              </button>
            </div>
          </div>
          <div class="filter-actions">
            <button class="btn-primary" @click="handleSearch">筛选</button>
            <button class="btn-secondary" @click="resetSearch">重置</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 房源内容区 -->
    <div class="house-content">
      <!-- 排序工具栏 -->
      <div class="sort-toolbar" v-if="!loading && houseList.length > 0">
        <div class="sort-left">
          <span class="result-count">找到 <strong>{{ total }}</strong> 套房源</span>
        </div>
        <div class="sort-right">
          <!-- 视图模式切换 -->
          <div class="view-mode-toggle">
            <button
              class="view-btn"
              :class="{ active: viewMode === 'list' }"
              @click="switchViewMode('list')"
              title="列表视图"
            >
              <span>☰</span>
            </button>
            <button
              class="view-btn"
              :class="{ active: viewMode === 'split' }"
              @click="switchViewMode('split')"
              title="分屏视图"
            >
              <span>⚌</span>
            </button>
            <button
              class="view-btn"
              :class="{ active: viewMode === 'map' }"
              @click="switchViewMode('map')"
              title="地图视图"
            >
              <span>🗺️</span>
            </button>
          </div>
          <div class="divider"></div>
          <div class="sort-options">
            <span class="sort-label">排序</span>
            <button
              v-for="option in sortOptions"
              :key="option.value"
              class="sort-btn"
              :class="{ active: searchForm.sortBy === option.value }"
              @click="selectSort(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <div class="house-card-skeleton" v-for="i in 6" :key="i">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" style="width: 100%; height: 200px" />
              <div style="padding: 16px">
                <el-skeleton-item variant="h3" style="width: 70%" />
                <el-skeleton-item variant="text" style="margin-top: 12px" />
                <el-skeleton-item variant="text" style="width: 50%; margin-top: 8px" />
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>

      <!-- 房源列表 -->
      <div v-else>
        <div v-if="houseList.length === 0" class="empty-state">
          <span class="empty-icon">🔍</span>
          <h3>暂无符合条件的房源</h3>
          <p>试试调整筛选条件或搜索关键词</p>
          <button class="btn-primary" @click="resetSearch">清除所有筛选</button>
        </div>

        <!-- 列表模式 -->
        <div v-else-if="viewMode === 'list'" class="house-grid">
          <div
            v-for="house in houseList"
            :key="house.id"
            class="house-card"
            :data-house-id="house.id"
            @click="goToDetail(house.id)"
          >
            <div class="house-image">
              <el-image
                :src="getHouseImage(house)"
                fit="cover"
              >
                <template #error>
                  <div class="image-placeholder">
                    <span>🏠</span>
                  </div>
                </template>
              </el-image>
              <div class="house-price">¥{{ house.price }}<small>/月</small></div>
            </div>
            <div class="house-info">
              <h3 class="house-title">{{ house.title }}</h3>
              <p class="house-address">
                <span>📍</span>
                {{ house.address }}
                <span v-if="house.communityName" class="community-name">（{{ house.communityName }}）</span>
              </p>
              <div class="house-details">
                <span v-if="house.floor && house.totalFloors" class="detail-item">
                  {{ house.floor }}/{{ house.totalFloors }}层
                </span>
                <span v-if="house.orientationName" class="detail-item">
                  {{ house.orientationName }}
                </span>
                <span v-if="house.decorationName" class="detail-item">
                  {{ house.decorationName }}
                </span>
              </div>
              <div class="house-score" v-if="house.avgScore > 0">
                <span class="stars">⭐ {{ Number(house.avgScore).toFixed(1) }}</span>
                <span class="comment-count">({{ house.commentCount }}条评价)</span>
              </div>
              <div class="house-tags">
                <span class="tag-area">{{ house.area }}㎡</span>
                <span class="tag-type">{{ house.typeName }}</span>
                <span v-if="house.decorationName === '精装' || house.decorationName === '豪装'" class="tag-decoration">
                  {{ house.decorationName }}
                </span>
                <template v-if="parseFacilities(house.facilities)">
                  <span v-if="parseFacilities(house.facilities).wifi" class="tag-facility">WiFi</span>
                  <span v-if="parseFacilities(house.facilities).elevator" class="tag-facility">电梯</span>
                  <span v-if="parseFacilities(house.facilities).airConditioner" class="tag-facility">空调</span>
                </template>
              </div>
            </div>
          </div>
        </div>

        <!-- 分屏模式：左侧列表 + 右侧地图 -->
        <div v-else-if="viewMode === 'split'" class="split-view">
          <div class="split-list">
            <div
              v-for="house in houseList"
              :key="house.id"
              class="house-card compact"
              :data-house-id="house.id"
              :class="{ selected: selectedHouseId === house.id }"
              @click="handleCardClick(house, $event)"
              @dblclick="handleCardDoubleClick(house)"
            >
              <div class="house-image">
                <el-image :src="getHouseImage(house)" fit="cover" :preview-src-list="[]">
                  <template #error>
                    <div class="image-placeholder"><span>🏠</span></div>
                  </template>
                </el-image>
                <div class="house-price">¥{{ house.price }}</div>
              </div>
              <div class="house-info">
                <h3 class="house-title">{{ house.title }}</h3>
                <p class="house-address"><span>📍</span>{{ house.address }}</p>
                <div class="house-tags">
                  <span class="tag-area">{{ house.area }}㎡</span>
                  <span class="tag-type">{{ house.typeName }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="split-map">
            <AMapContainer
              ref="mapContainerRef"
              :houses="housesForMap"
              :center="mapCenter"
              :zoom="mapZoom"
              :selected-house-id="selectedHouseId"
              @marker-click="handleMarkerClick"
              @marker-double-click="handleMarkerDoubleClick"
              @bounds-change="handleMapBoundsChange"
            />
          </div>
        </div>

        <!-- 地图模式：全屏地图 -->
        <div v-else-if="viewMode === 'map'" class="map-view">
          <AMapContainer
            ref="mapContainerRef"
            :houses="housesForMap"
            :center="mapCenter"
            :zoom="mapZoom"
            @marker-click="handleMarkerClick"
            @marker-double-click="handleMarkerDoubleClick"
            @bounds-change="handleMapBoundsChange"
          />
          <div class="map-house-list">
            <el-scrollbar height="calc(100vh - 250px)">
              <div
                v-for="house in housesForMap"
                :key="house.id"
                class="house-card-map"
                :class="{ selected: selectedHouseId === house.id }"
                @click="handleCardClick(house, $event)"
                @dblclick="handleCardDoubleClick(house)"
              >
                <div class="house-price">¥{{ house.price }}</div>
                <div class="house-info">
                  <h4>{{ house.title }}</h4>
                  <p>{{ house.address }}</p>
                </div>
              </div>
            </el-scrollbar>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="viewMode === 'list'" class="pagination-container">
          <el-pagination
            :current-page="currentPage"
            :page-size="pageSize"
            :page-sizes="[12, 24, 36, 48]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            background
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import AMapContainer from '@/components/AMapContainer.vue'

// 路由实例
const router = useRouter()
const route = useRoute()

// 基础API路径
const baseAPI = import.meta.env.VITE_BASE_API || '/api'

// 数据列表
const houseList = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const houseTypeOptions = ref([])

// 视图模式
const viewMode = ref('list')
// 广州大学城中心坐标
const mapCenter = ref({ lng: 113.3874, lat: 23.0341 })
const mapZoom = ref(14)
const isMapSearchActive = ref(false)
const selectedHouseId = ref(null)
// 点击计时器用于区分单击和双击
const clickTimer = ref(null)
// 地图组件引用
const mapContainerRef = ref(null)

// 价格选项
const minPriceTemp = ref(null)
const maxPriceTemp = ref(null)
const priceQuickOptions = [
  { label: '全部', min: null, max: null },
  { label: '1000以下', min: 0, max: 1000 },
  { label: '1000-2000', min: 1000, max: 2000 },
  { label: '2000-3000', min: 2000, max: 3000 },
  { label: '3000-5000', min: 3000, max: 5000 },
  { label: '5000以上', min: 5000, max: null }
]

// 装修选项
const decorationOptions = [
  { label: '全部', value: undefined },
  { label: '毛坯', value: 0 },
  { label: '简装', value: 1 },
  { label: '精装', value: 2 },
  { label: '豪装', value: 3 }
]

// 朝向选项
const orientationOptions = [
  { label: '全部', value: undefined },
  { label: '南', value: 1 },
  { label: '东', value: 0 },
  { label: '北', value: 3 },
  { label: '西', value: 2 },
  { label: '南北通透', value: 8 }
]

// 建筑类型选项
const buildingTypeOptions = [
  { label: '全部', value: undefined },
  { label: '普通住宅', value: 0 },
  { label: '公寓', value: 1 },
  { label: '别墅', value: 2 },
  { label: 'loft', value: 3 },
  { label: '复式', value: 4 }
]

// 付款方式选项
const paymentOptions = [
  { label: '全部', value: undefined },
  { label: '押一付一', value: 0 },
  { label: '押一付三', value: 1 },
  { label: '押三付一', value: 2 },
  { label: '押一付六', value: 3 },
  { label: '半年付', value: 4 },
  { label: '年付', value: 5 },
  { label: '无押金', value: 6 }
]

// 排序选项
const sortOptions = [
  { label: '最新发布', value: '' },
  { label: '评分最高', value: 'score' },
  { label: '价格↑', value: 'price_asc' },
  { label: '价格↓', value: 'price_desc' }
]

// 搜索表单
const searchForm = reactive({
  title: '',
  minPrice: null,
  maxPrice: null,
  typeId: undefined,
  status: 1,
  sortBy: '',
  decoration: undefined,
  orientation: undefined,
  buildingType: undefined,
  paymentMethod: undefined
})

// 检查价格选项是否激活
const isPriceOptionActive = (index) => {
  const option = priceQuickOptions[index]
  return searchForm.minPrice === option.min && searchForm.maxPrice === option.max
}

// 选择房屋类型
const selectType = (id) => {
  searchForm.typeId = id
  handleSearch()
}

// 选择价格选项
const selectPriceOption = (index) => {
  const option = priceQuickOptions[index]
  searchForm.minPrice = option.min
  searchForm.maxPrice = option.max
  minPriceTemp.value = option.min
  maxPriceTemp.value = option.max
  handleSearch()
}

// 选择装修
const selectDecoration = (value) => {
  searchForm.decoration = value
  handleSearch()
}

// 选择朝向
const selectOrientation = (value) => {
  searchForm.orientation = value
  handleSearch()
}

// 选择建筑类型
const selectBuildingType = (value) => {
  searchForm.buildingType = value
  handleSearch()
}

// 选择付款方式
const selectPaymentMethod = (value) => {
  searchForm.paymentMethod = value
  handleSearch()
}

// 选择排序
const selectSort = (value) => {
  searchForm.sortBy = value
  handleSearch()
}

// 处理最小价格变化
const handleMinPriceChange = (val) => {
  searchForm.minPrice = val
  if (searchForm.maxPrice !== null && val > searchForm.maxPrice) {
    searchForm.maxPrice = val
    maxPriceTemp.value = val
  }
  setTimeout(handleSearch, 300)
}

// 处理最大价格变化
const handleMaxPriceChange = (val) => {
  searchForm.maxPrice = val
  if (searchForm.minPrice !== null && val < searchForm.minPrice) {
    searchForm.minPrice = val
    minPriceTemp.value = val
  }
  setTimeout(handleSearch, 300)
}

// 初始化
onMounted(() => {
  if (route.query.title) searchForm.title = route.query.title
  if (route.query.typeId !== undefined) searchForm.typeId = Number(route.query.typeId)
  if (route.query.decoration !== undefined) searchForm.decoration = Number(route.query.decoration)
  if (route.query.orientation !== undefined) searchForm.orientation = Number(route.query.orientation)
  if (route.query.buildingType !== undefined) searchForm.buildingType = Number(route.query.buildingType)
  if (route.query.paymentMethod !== undefined) searchForm.paymentMethod = Number(route.query.paymentMethod)
  if (route.query.tags && !searchForm.title) searchForm.title = route.query.tags

  minPriceTemp.value = searchForm.minPrice
  maxPriceTemp.value = searchForm.maxPrice

  fetchHouseTypeList()
  fetchHouseList()
})

// 获取房屋类型列表
const fetchHouseTypeList = async () => {
  try {
    await request.get('/house-types', {}, {
      onSuccess: (res) => {
        houseTypeOptions.value = res || []
      }
    })
  } catch (error) {
    console.error('获取房屋类型列表失败:', error)
  }
}

// 获取房屋列表
const fetchHouseList = async () => {
  loading.value = true
  try {
    const params = {
      title: searchForm.title,
      minPrice: searchForm.minPrice,
      maxPrice: searchForm.maxPrice,
      status: searchForm.status,
      currentPage: currentPage.value,
      size: pageSize.value,
      sortBy: searchForm.sortBy || undefined
    }

    if (searchForm.typeId !== undefined && searchForm.typeId !== null) {
      params.typeId = searchForm.typeId
    }
    if (searchForm.decoration !== undefined) params.decoration = searchForm.decoration
    if (searchForm.orientation !== undefined) params.orientation = searchForm.orientation
    if (searchForm.buildingType !== undefined) params.buildingType = searchForm.buildingType
    if (searchForm.paymentMethod !== undefined) params.paymentMethod = searchForm.paymentMethod

    await request.get('/houses/page', params, {
      onSuccess: (res) => {
        houseList.value = res.records || []
        total.value = res.total || 0
        if (res.current) currentPage.value = res.current
      }
    })
  } catch (error) {
    console.error('获取房屋列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  if (currentPage.value !== 1) {
    currentPage.value = 1
  } else {
    fetchHouseList()
  }
}

// 重置搜索
const resetSearch = () => {
  Object.assign(searchForm, {
    title: '',
    minPrice: null,
    maxPrice: null,
    typeId: undefined,
    sortBy: '',
    decoration: undefined,
    orientation: undefined,
    buildingType: undefined,
    paymentMethod: undefined
  })
  minPriceTemp.value = null
  maxPriceTemp.value = null

  if (currentPage.value !== 1) {
    currentPage.value = 1
  } else {
    fetchHouseList()
  }
}

// 每页数量变化
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  fetchHouseList()
}

// 当前页变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchHouseList()
}

// 获取房屋图片
const getHouseImage = (house) => {
  if (!house.images) return ''
  try {
    const imageArray = JSON.parse(house.images)
    if (imageArray.length === 0) return ''
    return formatImageUrl(imageArray[0])
  } catch (e) {
    return ''
  }
}

// 格式化图片URL
const formatImageUrl = (path) => {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return baseAPI + path
}

// 格式化图片列表
const formatImageList = (images) => {
  if (!images) return []
  try {
    const imageArray = JSON.parse(images)
    return imageArray.map(img => formatImageUrl(img))
  } catch (e) {
    return []
  }
}

// 解析房屋配套设施
const parseFacilities = (facilities) => {
  if (!facilities) return {}
  try {
    return JSON.parse(facilities)
  } catch (e) {
    return {}
  }
}

// 跳转到房屋详情页
const goToDetail = (id) => {
  router.push(`/houses/${id}`)
}

// 处理卡片点击 - 单击高亮地图，双击跳转详情
const handleCardClick = (house, event) => {
  // 清除之前的计时器
  if (clickTimer.value) {
    clearTimeout(clickTimer.value)
    clickTimer.value = null
  }

  // 设置新的计时器，延迟执行单击操作
  clickTimer.value = setTimeout(() => {
    // 单击操作 - 高亮地图位置
    selectedHouseId.value = house.id
    if (house.latitude && house.longitude) {
      // 在分屏模式下，地图会自动更新选中状态
      // 如果需要移动地图中心，可以在这里添加
    }
  }, 200) // 200ms 延迟等待双击判断
}

// 处理卡片双击 - 跳转详情页
const handleCardDoubleClick = (house) => {
  // 清除单击计时器
  if (clickTimer.value) {
    clearTimeout(clickTimer.value)
    clickTimer.value = null
  }
  // 双击直接跳转详情
  goToDetail(house.id)
}

// 地图标记单击事件 - 高亮卡片和显示信息
const handleMarkerClick = (house) => {
  selectedHouseId.value = house.id
  if (viewMode.value === 'split') {
    const cardElement = document.querySelector(`.house-card[data-house-id="${house.id}"]`)
    if (cardElement) {
      cardElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }
}

// 地图标记双击事件 - 跳转详情页
const handleMarkerDoubleClick = (house) => {
  goToDetail(house.id)
}

// 地图边界变化事件
let boundsChangeTimeout = null
const handleMapBoundsChange = (bounds) => {
  if (!isMapSearchActive.value) return
  clearTimeout(boundsChangeTimeout)
  boundsChangeTimeout = setTimeout(() => {
    fetchMapBoundsHouses(bounds)
  }, 500)
}

// 地图范围搜索
const fetchMapBoundsHouses = async (bounds) => {
  loading.value = true
  try {
    const params = {
      minLng: bounds.minLng,
      maxLng: bounds.maxLng,
      minLat: bounds.minLat,
      maxLat: bounds.maxLat,
      currentPage: 1,
      size: 50
    }

    await request.get('/houses/map-search', params, {
      onSuccess: (res) => {
        houseList.value = res.records || []
        total.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('地图范围搜索失败:', error)
  } finally {
    loading.value = false
  }
}

// 仅在地图上显示的房源
const housesForMap = computed(() => {
  return houseList.value.filter(house => house.latitude && house.longitude)
})

// 切换视图模式
const switchViewMode = (mode) => {
  viewMode.value = mode
  if (mode === 'map') {
    isMapSearchActive.value = true
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-system.scss';

.house-list-container {
  min-height: calc(100vh - 72px);
  background: var(--color-bg-primary);

  // ==================== 搜索横幅 ====================
  .search-banner {
    position: relative;
    padding: 48px 24px;
    overflow: hidden;

    .banner-decoration {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      pointer-events: none;

      .deco-blob {
        position: absolute;
        border-radius: 30% 70% 70% 30% / 30% 30% 70% 70%;
        filter: blur(80px);
        animation: float 12s ease-in-out infinite;

        &.blob-1 {
          width: 400px;
          height: 400px;
          background: var(--color-primary-gradient);
          opacity: 0.15;
          top: -100px;
          right: -100px;
        }

        &.blob-2 {
          width: 300px;
          height: 300px;
          background: var(--color-secondary-gradient);
          opacity: 0.12;
          bottom: -50px;
          left: -50px;
          animation-delay: -3s;
        }
      }
    }

    .banner-content {
      max-width: 700px;
      margin: 0 auto;
      text-align: center;
      position: relative;

      .banner-badge {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 6px 16px;
        background: rgba(255, 107, 107, 0.1);
        color: var(--color-primary);
        border-radius: var(--radius-full);
        font-size: 13px;
        font-weight: 600;
        margin-bottom: 16px;
      }

      h1 {
        font-family: $font-display;
        font-size: var(--font-5xl);
        font-weight: 800;
        color: var(--color-text-primary);
        margin-bottom: 8px;
        letter-spacing: -1px;
      }

      .banner-subtitle {
        font-size: var(--font-lg);
        color: var(--color-text-tertiary);
        margin-bottom: 32px;
      }

      .search-box {
        display: flex;
        align-items: center;
        background: white;
        border-radius: var(--radius-full);
        box-shadow: var(--shadow-lg);
        border: 2px solid var(--color-border);
        padding: 6px;
        transition: all var(--transition-base);

        &:focus-within {
          border-color: var(--color-primary);
          box-shadow: 0 0 0 4px rgba(255, 107, 107, 0.15);
        }

        .search-input {
          flex: 1;

          :deep(.el-input__wrapper) {
            border: none;
            box-shadow: none;
            background: transparent;

            .el-input__inner {
              font-size: var(--font-md);
            }

            .el-input__prefix {
              font-size: 18px;
              color: var(--color-text-tertiary);
            }
          }
        }

        .search-btn {
          padding: 12px 28px;
          background: var(--color-primary-gradient);
          color: white;
          border: none;
          border-radius: var(--radius-full);
          font-size: var(--font-md);
          font-weight: 600;
          font-family: $font-display;
          cursor: pointer;
          transition: all var(--transition-base);
          white-space: nowrap;

          &:hover {
            box-shadow: var(--shadow-colored);
            transform: translateY(-1px);
          }
        }
      }
    }
  }

  // ==================== 筛选容器 ====================
  .filter-container {
    margin-top: -24px;
    padding: 0 24px;
    position: relative;
    z-index: 1;

    .filter-card {
      background: var(--color-bg-secondary);
      border-radius: var(--radius-xl);
      box-shadow: var(--shadow-md);
      border: 1px solid var(--color-border-light);
      padding: 24px;
    }

    .filter-section {
      .filter-item {
        display: flex;
        align-items: center;
        margin-bottom: 20px;
        padding-bottom: 20px;
        border-bottom: 1px solid var(--color-border-light);

        &:last-child {
          margin-bottom: 0;
          padding-bottom: 0;
          border-bottom: none;
        }

        .filter-label {
          width: 80px;
          flex-shrink: 0;
          color: var(--color-text-primary);
          font-weight: 600;
          font-size: var(--font-sm);
        }

        .filter-options {
          flex: 1;
          display: flex;
          flex-wrap: wrap;
          gap: 8px;

          .filter-option-btn {
            padding: 8px 16px;
            background: var(--color-bg-primary);
            border: 1px solid var(--color-border);
            border-radius: var(--radius-full);
            font-size: var(--font-sm);
            color: var(--color-text-secondary);
            cursor: pointer;
            transition: all var(--transition-base);

            &:hover {
              border-color: var(--color-primary);
              color: var(--color-primary);
            }

            &.active {
              background: var(--color-primary-gradient);
              border-color: transparent;
              color: white;
            }
          }

          .price-input-group {
            display: flex;
            align-items: center;
            margin-left: 16px;

            .range-separator {
              margin: 0 8px;
              color: var(--color-text-tertiary);
            }

            :deep(.el-input-number) {
              width: 100px;

              .el-input__wrapper {
                border-radius: var(--radius-md);
              }
            }
          }
        }
      }

      .filter-actions {
        display: flex;
        justify-content: flex-end;
        gap: 12px;
        margin-top: 20px;
        padding-top: 20px;
        border-top: 1px solid var(--color-border-light);

        button {
          padding: 10px 20px;
          border-radius: var(--radius-full);
          font-size: var(--font-sm);
          font-weight: 500;
          cursor: pointer;
          transition: all var(--transition-base);
          border: none;

          &.btn-primary {
            background: var(--color-primary-gradient);
            color: white;

            &:hover {
              box-shadow: var(--shadow-colored);
              transform: translateY(-1px);
            }
          }

          &.btn-secondary {
            background: var(--color-bg-primary);
            color: var(--color-text-secondary);
            border: 1px solid var(--color-border);

            &:hover {
              border-color: var(--color-primary);
              color: var(--color-primary);
            }
          }
        }
      }
    }
  }

  // ==================== 房源内容区 ====================
  .house-content {
    padding: 24px;
    max-width: 1400px;
    margin: 0 auto;

    .sort-toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      padding: 16px 20px;
      background: var(--color-bg-secondary);
      border-radius: var(--radius-lg);
      border: 1px solid var(--color-border-light);

      .sort-left {
        .result-count {
          font-size: var(--font-sm);
          color: var(--color-text-secondary);

          strong {
            color: var(--color-primary);
            font-size: var(--font-md);
          }
        }
      }

      .sort-right {
        display: flex;
        align-items: center;
        gap: 16px;

        .view-mode-toggle {
          display: flex;
          background: var(--color-bg-primary);
          border-radius: var(--radius-md);
          padding: 4px;
          border: 1px solid var(--color-border);

          .view-btn {
            padding: 8px 12px;
            background: transparent;
            border: none;
            border-radius: var(--radius-sm);
            font-size: 16px;
            cursor: pointer;
            transition: all var(--transition-base);
            color: var(--color-text-tertiary);

            &:hover {
              color: var(--color-text-primary);
            }

            &.active {
              background: white;
              color: var(--color-primary);
              box-shadow: var(--shadow-sm);
            }
          }
        }

        .divider {
          width: 1px;
          height: 24px;
          background: var(--color-border);
        }

        .sort-options {
          display: flex;
          align-items: center;
          gap: 4px;

          .sort-label {
            font-size: var(--font-sm);
            color: var(--color-text-tertiary);
            margin-right: 8px;
          }

          .sort-btn {
            padding: 6px 12px;
            background: transparent;
            border: none;
            border-radius: var(--radius-sm);
            font-size: var(--font-sm);
            color: var(--color-text-secondary);
            cursor: pointer;
            transition: all var(--transition-base);

            &:hover {
              background: var(--color-bg-primary);
              color: var(--color-primary);
            }

            &.active {
              background: var(--color-primary-gradient);
              color: white;
            }
          }
        }
      }
    }

    .loading-container {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 24px;
    }

    .empty-state {
      text-align: center;
      padding: 80px 24px;

      .empty-icon {
        font-size: 64px;
        display: block;
        margin-bottom: 16px;
      }

      h3 {
        font-size: var(--font-xl);
        color: var(--color-text-primary);
        margin-bottom: 8px;
      }

      p {
        color: var(--color-text-tertiary);
        margin-bottom: 24px;
      }
    }

    .house-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 24px;

      .house-card {
        background: var(--color-bg-secondary);
        border-radius: var(--radius-xl);
        overflow: hidden;
        border: 1px solid var(--color-border-light);
        cursor: pointer;
        transition: all var(--transition-base);

        &:hover {
          box-shadow: var(--shadow-lg);
          transform: translateY(-4px);
          border-color: rgba(255, 107, 107, 0.3);
        }

        .house-image {
          position: relative;
          height: 200px;
          overflow: hidden;

          .el-image {
            width: 100%;
            height: 100%;
          }

          .image-placeholder {
            width: 100%;
            height: 100%;
            display: flex;
            align-items: center;
            justify-content: center;
            background: var(--color-bg-tertiary);
            font-size: 48px;
          }

          .house-price {
            position: absolute;
            bottom: 12px;
            right: 12px;
            background: var(--color-primary-gradient);
            color: white;
            padding: 8px 14px;
            border-radius: var(--radius-full);
            font-weight: 700;
            font-size: var(--font-lg);
            box-shadow: var(--shadow-colored);
            display: flex;
            align-items: baseline;
            gap: 2px;

            small {
              font-size: var(--font-xs);
              font-weight: 500;
              opacity: 0.9;
            }
          }
        }

        .house-info {
          padding: 16px;

          .house-title {
            font-size: var(--font-md);
            font-weight: 600;
            color: var(--color-text-primary);
            margin: 0 0 8px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .house-address {
            display: flex;
            align-items: center;
            gap: 4px;
            color: var(--color-text-tertiary);
            font-size: var(--font-sm);
            margin-bottom: 12px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;

            span {
              flex-shrink: 0;
            }

            .community-name {
              color: var(--color-text-secondary);
            }
          }

          .house-details {
            display: flex;
            gap: 8px;
            margin-bottom: 12px;

            .detail-item {
              padding: 4px 10px;
              background: var(--color-bg-tertiary);
              border-radius: var(--radius-sm);
              font-size: var(--font-xs);
              color: var(--color-text-secondary);
            }
          }

          .house-score {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 12px;

            .stars {
              color: #FFB800;
              font-size: var(--font-sm);
              font-weight: 500;
            }

            .comment-count {
              font-size: var(--font-xs);
              color: var(--color-text-tertiary);
            }
          }

          .house-tags {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;

            span {
              padding: 4px 10px;
              border-radius: var(--radius-sm);
              font-size: var(--font-xs);
              font-weight: 500;

              &.tag-area {
                background: rgba(78, 205, 196, 0.1);
                color: var(--color-secondary-dark);
              }

              &.tag-type {
                background: rgba(255, 107, 107, 0.1);
                color: var(--color-primary);
              }

              &.tag-decoration {
                background: rgba(255, 230, 109, 0.2);
                color: #D4A000;
              }

              &.tag-facility {
                background: var(--color-bg-tertiary);
                color: var(--color-text-tertiary);
              }
            }
          }
        }
      }
    }

    .pagination-container {
      margin-top: 32px;
      display: flex;
      justify-content: center;
    }
  }

  // ==================== 分屏视图 ====================
  .split-view {
    display: flex;
    gap: 24px;
    height: calc(100vh - 250px);

    .split-list {
      flex: 0 0 400px;
      overflow-y: auto;
      padding-right: 8px;

      .house-card {
        display: flex;
        margin-bottom: 16px;
        cursor: pointer;
        background: var(--color-bg-secondary);
        border-radius: var(--radius-lg);
        border: 1px solid var(--color-border-light);
        overflow: hidden;
        transition: all var(--transition-base);

        &.selected {
          border-color: var(--color-primary);
          box-shadow: 0 0 0 2px rgba(255, 107, 107, 0.2);
        }

        &:hover {
          border-color: rgba(255, 107, 107, 0.3);
        }

        .house-image {
          width: 140px;
          height: 100px;
          flex-shrink: 0;
          position: relative;

          .el-image {
            width: 100%;
            height: 100%;
          }

          .image-placeholder {
            width: 100%;
            height: 100%;
            display: flex;
            align-items: center;
            justify-content: center;
            background: var(--color-bg-tertiary);
            font-size: 32px;
          }

          .house-price {
            position: absolute;
            bottom: 8px;
            right: 8px;
            background: var(--color-primary-gradient);
            color: white;
            padding: 4px 10px;
            border-radius: var(--radius-full);
            font-size: var(--font-sm);
            font-weight: 600;
          }
        }

        .house-info {
          flex: 1;
          padding: 12px;

          .house-title {
            font-size: var(--font-md);
            font-weight: 600;
            color: var(--color-text-primary);
            margin: 0 0 8px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .house-address {
            font-size: var(--font-sm);
            color: var(--color-text-tertiary);
            margin-bottom: 8px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .house-tags {
            display: flex;
            gap: 6px;

            span {
              padding: 4px 8px;
              border-radius: var(--radius-sm);
              font-size: var(--font-xs);

              &.tag-area {
                background: rgba(78, 205, 196, 0.1);
                color: var(--color-secondary-dark);
              }

              &.tag-type {
                background: rgba(255, 107, 107, 0.1);
                color: var(--color-primary);
              }
            }
          }
        }
      }
    }

    .split-map {
      flex: 1;
      min-height: 500px;
      border-radius: var(--radius-xl);
      overflow: hidden;
      border: 1px solid var(--color-border-light);
    }
  }

  // ==================== 地图视图 ====================
  .map-view {
    position: relative;
    height: calc(100vh - 200px);
    border-radius: var(--radius-xl);
    overflow: hidden;
    border: 1px solid var(--color-border-light);

    .map-house-list {
      position: absolute;
      top: 20px;
      right: 20px;
      width: 320px;
      max-height: calc(100vh - 280px);
      background: var(--color-bg-secondary);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-xl);
      overflow: hidden;
      border: 1px solid var(--color-border-light);

      .house-card-map {
        display: flex;
        padding: 12px 16px;
        cursor: pointer;
        border-bottom: 1px solid var(--color-border-light);
        transition: background var(--transition-base);

        &:last-child {
          border-bottom: none;
        }

        &:hover {
          background: var(--color-bg-primary);
        }

        &.selected {
          background: rgba(255, 107, 107, 0.1);
          border-left: 3px solid var(--color-primary);
        }

        .house-price {
          font-weight: 700;
          color: var(--color-primary);
          margin-right: 12px;
          font-size: var(--font-lg);
        }

        .house-info {
          flex: 1;

          h4 {
            margin: 0 0 4px;
            font-size: var(--font-sm);
            font-weight: 600;
            color: var(--color-text-primary);
          }

          p {
            margin: 0;
            font-size: var(--font-xs);
            color: var(--color-text-tertiary);
          }
        }
      }
    }
  }
}

// ==================== 响应式 ====================
@media (max-width: 768px) {
  .house-list-container {
    .search-banner {
      padding: 32px 16px;

      .banner-content {
        h1 {
          font-size: var(--font-3xl);
        }

        .search-box {
          flex-direction: column;
          padding: 8px;

          .search-btn {
            width: 100%;
          }
        }
      }
    }

    .filter-container {
      margin-top: -16px;
      padding: 0 16px;

      .filter-card {
        padding: 16px;
      }

      .filter-section {
        .filter-item {
          flex-direction: column;
          align-items: flex-start;

          .filter-label {
            width: 100%;
            margin-bottom: 8px;
          }

          .filter-options {
            width: 100%;

            .price-input-group {
              margin-left: 0;
              margin-top: 12px;
              flex-wrap: wrap;
            }
          }
        }

        .filter-actions {
          flex-wrap: wrap;

          button {
            flex: 1;
            min-width: 80px;
          }
        }
      }
    }

    .house-content {
      padding: 16px;

      .sort-toolbar {
        flex-direction: column;
        gap: 12px;
        align-items: stretch;

        .sort-right {
          justify-content: space-between;
        }
      }

      .house-grid {
        grid-template-columns: 1fr;
      }
    }

    .split-view {
      flex-direction: column;

      .split-list {
        flex: none;
        max-height: 300px;
      }

      .split-map {
        min-height: 300px;
      }
    }

    .map-view {
      .map-house-list {
        width: calc(100% - 32px);
        left: 16px;
        right: 16px;
      }
    }
  }
}
</style>
