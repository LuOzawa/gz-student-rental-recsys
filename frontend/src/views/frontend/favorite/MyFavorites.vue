<template>
  <div class="favorites-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <span class="page-icon">❤️</span>
        <div class="header-text">
          <h1 class="page-title">我的收藏</h1>
          <p class="page-subtitle">共收藏 {{ total }} 套心仪房源</p>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- 空状态 -->
    <div v-else-if="favorites.length === 0" class="empty-state">
      <div class="empty-icon">🏠</div>
      <h3 class="empty-title">还没有收藏任何房源</h3>
      <p class="empty-desc">收藏心仪的房源，方便日后查看</p>
      <button class="btn-browse" @click="$router.push('/houses')">
        🏘️ 去浏览房源
      </button>
    </div>

    <!-- 房源网格 -->
    <div v-else class="favorites-grid">
      <div
        v-for="(house, index) in favorites"
        :key="house.id"
        class="house-card card-creative"
        :style="{ animationDelay: `${index * 50}ms` }"
        @click="$router.push(`/houses/${house.id}`)"
      >
        <div class="house-image">
          <img :src="getHouseImage(house.images)" :alt="house.title" />
          <div class="price-badge">¥{{ house.price }}/月</div>
        </div>

        <div class="house-content">
          <h3 class="house-title">{{ house.title }}</h3>
          <p class="house-address">📍 {{ house.address }}</p>

          <div class="house-tags">
            <span class="tag">{{ house.area }}㎡</span>
            <span class="tag tag-type">{{ getTypeName(house.typeId) }}</span>
            <span v-if="house.avgScore > 0" class="tag tag-rating">
              ⭐ {{ house.avgScore }}
            </span>
          </div>

          <div class="house-actions" @click.stop>
            <button class="btn-remove" @click="handleRemoveFavorite(house.id)">
              🗑️ 取消收藏
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination-section">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFavoriteList, removeFavorite } from '@/api/favorite'

const favorites = ref([])
const loading = ref(true)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

const houseTypes = {
  1: '整租',
  2: '合租'
}

const getHouseImage = (images) => {
  if (!images) return '/img/default.jpeg'
  try {
    const imageList = JSON.parse(images)
    if (!imageList || imageList.length === 0) return '/img/default.jpeg'
    const imagePath = imageList[0]
    if (!imagePath) return '/img/default.jpeg'
    if (imagePath.startsWith('http')) return imagePath
    const baseAPI = import.meta.env.VITE_BASE_API || '/api'
    return baseAPI + imagePath
  } catch {
    return '/img/default.jpeg'
  }
}

const getTypeName = (typeId) => {
  return houseTypes[typeId] || '未知'
}

const loadFavorites = async () => {
  loading.value = true
  try {
    const res = await getFavoriteList({
      currentPage: currentPage.value,
      pageSize: pageSize.value
    })
    if (Array.isArray(res)) {
      favorites.value = res
      total.value = res.length
    } else if (res?.records) {
      favorites.value = res.records
      total.value = res.total || 0
    } else if (res?.data?.records) {
      favorites.value = res.data.records
      total.value = res.data.total || 0
    } else {
      favorites.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('加载收藏列表失败:', error)
    favorites.value = []
  } finally {
    loading.value = false
  }
}

const handleRemoveFavorite = async (houseId) => {
  try {
    await ElMessageBox.confirm('确定要取消收藏这个房源吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await removeFavorite(houseId)
    ElMessage.success('已取消收藏')
    loadFavorites()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadFavorites()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  loadFavorites()
})
</script>

<style lang="scss" scoped>
.favorites-page {
  max-width: 1200px;
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
    gap: 16px;
  }

  .page-icon {
    font-size: 48px;
    animation: float 3s ease-in-out infinite;
  }

  .header-text {
    .page-title {
      font-family: 'Outfit', sans-serif;
      font-size: 28px;
      font-weight: 800;
      color: var(--color-text-primary);
      margin: 0 0 4px 0;
    }

    .page-subtitle {
      color: var(--color-text-tertiary);
      margin: 0;
    }
  }
}

// 加载状态
.loading-state {
  background: white;
  border-radius: var(--radius-xl);
  padding: 40px;
  box-shadow: var(--shadow-md);
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

// 房源网格
.favorites-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;

  .house-card {
    overflow: hidden;
    cursor: pointer;
    animation: fadeInUp 0.4s ease-out backwards;

    .house-image {
      position: relative;
      height: 180px;
      overflow: hidden;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform var(--transition-slow);
      }

      .price-badge {
        position: absolute;
        bottom: 12px;
        right: 12px;
        background: var(--color-primary-gradient);
        color: white;
        padding: 6px 14px;
        border-radius: var(--radius-full);
        font-size: 16px;
        font-weight: 700;
        box-shadow: 0 4px 12px rgba(255, 107, 107, 0.4);
      }
    }

    &:hover .house-image img {
      transform: scale(1.05);
    }

    .house-content {
      padding: 16px;
    }

    .house-title {
      font-size: 16px;
      font-weight: 700;
      color: var(--color-text-primary);
      margin: 0 0 8px 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .house-address {
      font-size: 13px;
      color: var(--color-text-tertiary);
      margin: 0 0 12px 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .house-tags {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      margin-bottom: 12px;

      .tag {
        padding: 4px 10px;
        background: var(--color-bg-tertiary);
        border-radius: var(--radius-sm);
        font-size: 12px;
        font-weight: 500;
        color: var(--color-text-secondary);

        &.tag-type {
          background: rgba(78, 205, 196, 0.15);
          color: var(--color-secondary-dark);
        }

        &.tag-rating {
          background: rgba(255, 230, 109, 0.2);
          color: #B8860B;
        }
      }
    }

    .house-actions {
      display: flex;
      justify-content: flex-end;
    }
  }
}

.btn-remove {
  padding: 8px 16px;
  background: transparent;
  color: var(--color-text-tertiary);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    border-color: var(--color-error);
    color: var(--color-error);
    background: rgba(255, 107, 107, 0.1);
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
  .favorites-page {
    padding: 15px;
  }

  .favorites-grid {
    grid-template-columns: 1fr;
  }

  .page-header {
    .header-content {
      flex-direction: column;
      text-align: center;
    }

    .page-icon {
      font-size: 36px;
    }

    .page-title {
      font-size: 24px;
    }
  }
}
</style>
