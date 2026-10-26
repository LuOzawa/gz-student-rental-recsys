<template>
  <div class="recommendations-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <span class="page-icon">🏠</span>
        <div class="title-section">
          <h1 class="page-title">房源推荐</h1>
          <p class="page-subtitle">为你精选的优质房源</p>
        </div>
      </div>
    </div>

    <!-- 分类标签 -->
    <div class="category-section">
      <div class="category-tabs">
        <button
          :class="['category-tab', { active: activeTab === 'smart' }]"
          @click="switchTab('smart')"
        >
          🎯 为你推荐
        </button>
        <button
          :class="['category-tab', { active: activeTab === 'hot' }]"
          @click="switchTab('hot')"
        >
          🔥 热门房源
        </button>
      </div>
    </div>

    <!-- 为你推荐 -->
    <section v-if="activeTab === 'smart'" class="content-section">
      <div v-loading="smartLoading" class="houses-grid">
        <div
          v-for="item in smartHouses"
          :key="item.id"
          class="house-card card-styled"
          @click="goToHouse(item.id)"
        >
          <div class="house-image">
            <img :src="getHouseImage(item.images)" :alt="item.title" />
            <div class="price-tag">¥{{ item.price }}/月</div>
            <div v-if="item.score" class="score-badge">
              {{ Math.round(item.score * 100) }}% 匹配
            </div>
          </div>
          <div class="house-info">
            <h3 class="house-title">{{ item.title }}</h3>
            <p class="house-address">{{ item.address }}</p>
            <div class="house-details">
              <span v-if="item.floor !== null && item.totalFloors" class="detail-item">
                {{ item.floor }}/{{ item.totalFloors }}层
              </span>
              <span v-if="item.orientation !== null" class="detail-item">
                {{ getOrientationName(item.orientation) }}
              </span>
              <span v-if="item.decoration !== null" class="detail-item">
                {{ getDecorationName(item.decoration) }}
              </span>
            </div>
            <div class="house-tags">
              <span class="tag tag-area">{{ item.area }}㎡</span>
              <span class="tag tag-type">{{ getTypeName(item.typeId) }}</span>
              <span v-if="item.avgScore > 0" class="tag tag-score">
                ⭐ {{ item.avgScore }}
              </span>
            </div>
            <div v-if="item.explanation" class="recommendation-reason">
              <span class="reason-icon">💡</span>
              <span>{{ item.explanation }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-if="!smartLoading && smartHouses.length === 0" class="empty-state">
        <span class="empty-icon">📭</span>
        <p>暂无推荐，多浏览房源后会更有推荐哦</p>
      </div>
    </section>

    <!-- 热门房源 -->
    <section v-if="activeTab === 'hot'" class="content-section">
      <div v-loading="hotLoading" class="houses-grid">
        <div
          v-for="item in hotHouses"
          :key="item.id"
          class="house-card card-styled"
          @click="goToHouse(item.id)"
        >
          <div class="house-image">
            <img :src="getHouseImage(item.images)" :alt="item.title" />
            <div class="price-tag">¥{{ item.price }}/月</div>
            <div class="hot-badge">🔥 热门</div>
          </div>
          <div class="house-info">
            <h3 class="house-title">{{ item.title }}</h3>
            <p class="house-address">{{ item.address }}</p>
            <div class="house-details">
              <span v-if="item.floor !== null && item.totalFloors" class="detail-item">
                {{ item.floor }}/{{ item.totalFloors }}层
              </span>
              <span v-if="item.orientation !== null" class="detail-item">
                {{ getOrientationName(item.orientation) }}
              </span>
              <span v-if="item.decoration !== null" class="detail-item">
                {{ getDecorationName(item.decoration) }}
              </span>
            </div>
            <div class="house-tags">
              <span class="tag tag-area">{{ item.area }}㎡</span>
              <span class="tag tag-type">{{ getTypeName(item.typeId) }}</span>
              <span v-if="item.avgScore > 0" class="tag tag-score">
                ⭐ {{ item.avgScore }}
              </span>
            </div>
            <div class="house-stats">
              <span class="stat">👁 {{ item.viewCount || 0 }}</span>
              <span class="stat">❤️ {{ item.favoriteCount || 0 }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-if="!hotLoading && hotHouses.length === 0" class="empty-state">
        <span class="empty-icon">📭</span>
        <p>暂无热门房源</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSmartHybridRecommendations, getHotHouses } from '@/api/recommendation'

const router = useRouter()
const baseAPI = import.meta.env.VITE_BASE_API || '/api'

const activeTab = ref('smart')
const smartHouses = ref([])
const smartLoading = ref(false)
const hotHouses = ref([])
const hotLoading = ref(false)

// 房源类型
const houseTypes = {
  1: '整租',
  2: '合租'
}

// 装修类型
const decorationNames = {
  0: '毛坯',
  1: '简装',
  2: '精装',
  3: '豪装'
}

// 朝向
const orientationNames = {
  0: '东',
  1: '南',
  2: '西',
  3: '北',
  4: '东南',
  5: '西南',
  6: '东北',
  7: '西北',
  8: '南北通透'
}

// 获取房源图片
const getHouseImage = (images) => {
  if (!images) return '/img/default.jpeg'
  try {
    const imageList = JSON.parse(images)
    if (!imageList || imageList.length === 0) return '/img/default.jpeg'
    const imagePath = imageList[0]
    if (!imagePath) return '/img/default.jpeg'
    if (imagePath.startsWith('http')) return imagePath
    return baseAPI + imagePath
  } catch {
    return '/img/default.jpeg'
  }
}

const getTypeName = (typeId) => {
  return houseTypes[typeId] || '未知'
}

const getDecorationName = (decoration) => {
  return decorationNames[decoration] || ''
}

const getOrientationName = (orientation) => {
  return orientationNames[orientation] || ''
}

const loadSmartRecommendations = async () => {
  smartLoading.value = true
  try {
    const res = await getSmartHybridRecommendations({ limit: 12 })
    smartHouses.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (error) {
    console.error('加载智能推荐失败:', error)
    smartHouses.value = []
  } finally {
    smartLoading.value = false
  }
}

const loadHot = async () => {
  hotLoading.value = true
  try {
    const res = await getHotHouses({ limit: 12 })
    hotHouses.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (error) {
    console.error('加载热门房源失败:', error)
    hotHouses.value = []
  } finally {
    hotLoading.value = false
  }
}

const switchTab = (tab) => {
  activeTab.value = tab
  if (tab === 'hot' && hotHouses.value.length === 0) {
    loadHot()
  }
}

const goToHouse = (id) => {
  router.push(`/houses/${id}`)
}

onMounted(() => {
  loadSmartRecommendations()
})
</script>

<style lang="scss" scoped>
.recommendations-page {
  min-height: 100vh;
  background: var(--color-bg-primary);
  padding-bottom: 40px;
}

// 页面头部
.page-header {
  padding: 32px 20px 24px;
  background: white;
  border-bottom: 1px solid var(--color-border-light);

  .header-content {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .page-icon {
    font-size: 42px;
    animation: float 3s ease-in-out infinite;
  }

  .title-section {
    .page-title {
      font-family: 'Outfit', sans-serif;
      font-size: 28px;
      font-weight: 800;
      color: var(--color-text-primary);
      margin: 0 0 4px 0;
    }

    .page-subtitle {
      color: var(--color-text-tertiary);
      font-size: 14px;
      margin: 0;
    }
  }
}

// 分类标签
.category-section {
  background: white;
  border-bottom: 1px solid var(--color-border-light);

  .category-tabs {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    padding: 0 20px;

    .category-tab {
      padding: 16px 24px;
      background: transparent;
      border: none;
      border-bottom: 3px solid transparent;
      font-size: 16px;
      font-weight: 500;
      color: var(--color-text-secondary);
      cursor: pointer;
      transition: all var(--transition-base);
      position: relative;

      &:hover {
        color: var(--color-primary);
      }

      &.active {
        color: var(--color-primary);
        border-bottom-color: var(--color-primary);
        font-weight: 600;
      }
    }
  }
}

// 内容区域
.content-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

// 房源网格
.houses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  min-height: 200px;
}

// 房源卡片
.house-card {
  cursor: pointer;
  overflow: hidden;
  padding: 0;
  transition: all var(--transition-base);

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-lg);
  }

  .house-image {
    position: relative;
    height: 180px;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform var(--transition-base);
    }

    &:hover img {
      transform: scale(1.05);
    }

    .price-tag {
      position: absolute;
      bottom: 0;
      left: 0;
      background: rgba(0, 0, 0, 0.75);
      color: white;
      padding: 6px 12px;
      font-size: 16px;
      font-weight: 700;
      border-radius: 0 var(--radius-md) 0 0;
    }

    .score-badge {
      position: absolute;
      top: 10px;
      right: 10px;
      background: var(--color-primary-gradient);
      color: white;
      padding: 4px 10px;
      border-radius: var(--radius-full);
      font-size: 12px;
      font-weight: 600;
    }

    .hot-badge {
      position: absolute;
      top: 10px;
      right: 10px;
      background: linear-gradient(135deg, #FF6B6B 0%, #FF4757 100%);
      color: white;
      padding: 4px 10px;
      border-radius: var(--radius-full);
      font-size: 12px;
      font-weight: 600;
    }
  }

  .house-info {
    padding: 16px;
  }

  .house-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0 0 8px 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .house-address {
    color: var(--color-text-tertiary);
    font-size: 13px;
    margin: 0 0 10px 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .house-details {
    display: flex;
    gap: 6px;
    margin-bottom: 10px;
    flex-wrap: wrap;

    .detail-item {
      font-size: 12px;
      color: var(--color-text-secondary);
      padding: 3px 8px;
      background: var(--color-bg-tertiary);
      border-radius: var(--radius-sm);
    }
  }

  .house-tags {
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
    margin-bottom: 10px;

    .tag {
      font-size: 12px;
      padding: 4px 10px;
      border-radius: var(--radius-sm);
      font-weight: 500;

      &.tag-area {
        background: rgba(78, 205, 196, 0.15);
        color: var(--color-secondary);
      }

      &.tag-type {
        background: rgba(255, 107, 107, 0.15);
        color: var(--color-primary);
      }

      &.tag-score {
        background: rgba(255, 230, 109, 0.3);
        color: #F59E0B;
      }
    }
  }

  .recommendation-reason {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    padding: 10px;
    background: rgba(255, 107, 107, 0.08);
    border-radius: var(--radius-md);
    font-size: 12px;
    color: var(--color-primary);
    line-height: 1.4;

    .reason-icon {
      flex-shrink: 0;
    }
  }

  .house-stats {
    display: flex;
    gap: 16px;

    .stat {
      font-size: 13px;
      color: var(--color-text-tertiary);
    }
  }
}

// 空状态
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--color-text-tertiary);

  .empty-icon {
    font-size: 56px;
    display: block;
    margin-bottom: 12px;
    opacity: 0.5;
  }

  p {
    margin: 0;
  }
}

// 响应式
@media (max-width: 768px) {
  .page-header {
    padding: 20px 16px 16px;

    .page-icon {
      font-size: 32px;
    }

    .title-section .page-title {
      font-size: 22px;
    }
  }

  .category-tabs {
    padding: 0 16px;

    .category-tab {
      padding: 12px 16px;
      font-size: 14px;
    }
  }

  .houses-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;
  }

  .house-card .house-image {
    height: 120px;
  }

  .house-card .house-info {
    padding: 12px;
  }
}
</style>
