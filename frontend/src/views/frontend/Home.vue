<template>
  <div class="home-container">
    <!-- 顶部轮播图 -->
    <div class="hero-section">
      <carousel-banner />
    </div>

    <!-- 搜索区域 -->
    <div class="search-section">
      <div class="search-container">
        <div class="search-badge">
          <span>🔍</span>
          <span>智能搜索</span>
        </div>
        <h2>寻找你的理想小窝</h2>
        <p class="search-subtitle">覆盖广州各大高校周边，海量优质房源等你发现</p>
        <div class="search-box-wrapper">
          <div class="search-box">
            <el-input
              v-model="searchKeyword"
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
              <span>搜索</span>
            </button>
          </div>
        </div>
        <div class="quick-tags">
          <span class="tags-label">热门：</span>
          <button
            v-for="tag in quickSearchTags"
            :key="tag.label"
            @click="quickSearch(tag)"
            class="quick-tag"
          >
            {{ tag.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- 公告区域 -->
    <div class="announcement-section">
      <div class="section-header center">
        <div class="section-badge">
          <span>📢</span>
          <span>最新动态</span>
        </div>
        <h2>平台公告</h2>
        <p>了解平台最新动态和活动信息</p>
      </div>
      <div class="announcement-container">
        <el-row :gutter="24">
          <el-col v-if="loadingAnnouncements" :span="24" class="text-center">
            <el-skeleton :rows="3" animated />
          </el-col>
          <template v-else>
            <el-col v-if="announcements.length === 0" :span="24" class="text-center">
              <el-empty description="暂无公告" />
            </el-col>
            <el-col v-for="item in announcements" :key="item.id" :xs="24" :sm="24" :md="8">
              <div class="announcement-card" @click="viewAnnouncementDetail(item)">
                <div class="announcement-icon">
                  <span>📋</span>
                </div>
                <div class="announcement-content">
                  <h3 class="announcement-title">{{ item.title }}</h3>
                  <p class="announcement-excerpt">{{ getExcerpt(item.content) }}</p>
                  <div class="announcement-footer">
                    <span class="announcement-date">{{ formatDate(item.createTime) }}</span>
                    <span class="read-more">查看详情 →</span>
                  </div>
                </div>
              </div>
            </el-col>
          </template>
        </el-row>
        <div class="view-all-btn">
          <button class="btn-outline" @click="navigateTo('/announcements')">
            查看全部公告
          </button>
        </div>
      </div>
    </div>

    <!-- 热门房源区域 -->
    <div class="featured-section">
      <div class="section-header">
        <div class="header-left">
          <div class="section-badge">
            <span>{{ isLoggedIn ? '✨' : '🔥' }}</span>
            <span>{{ isLoggedIn ? '为你推荐' : '热门房源' }}</span>
          </div>
          <h2>{{ isLoggedIn ? '个性化推荐' : '精选房源' }}</h2>
        </div>
        <router-link to="/recommendations" class="view-more-link">
          查看更多 <span>→</span>
        </router-link>
      </div>

      <div v-if="loading" class="loading-container">
        <div class="house-card-skeleton" v-for="i in 4" :key="i">
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
      <div v-else class="houses-grid">
        <div
          v-for="house in featuredHouses"
          :key="house.id"
          class="house-card"
          @click="viewHouseDetail(house.id)"
        >
          <div class="house-image">
            <el-image :src="getHouseImage(house)" fit="cover">
              <template #error>
                <div class="image-placeholder">
                  <span>🏠</span>
                </div>
              </template>
            </el-image>
            <div class="house-price">¥{{ house.price }}<small>/月</small></div>
            <button
              class="favorite-btn"
              :class="{ 'active': house.isFavorited }"
              @click.stop="toggleFavorite(house)"
            >
              <span>{{ house.isFavorited ? '❤️' : '🤍' }}</span>
            </button>
          </div>
          <div class="house-info">
            <h3 class="house-title">{{ house.title }}</h3>
            <p class="house-address">
              <span>📍</span>
              {{ house.address }}
            </p>
            <div class="house-details" v-if="house.floor || house.totalFloors || house.decoration !== null">
              <span v-if="house.floor && house.totalFloors" class="detail-item">
                {{ house.floor }}/{{ house.totalFloors }}层
              </span>
              <span v-if="house.decoration !== null && house.decoration !== undefined" class="detail-item">
                {{ getDecorationName(house.decoration) }}
              </span>
            </div>
            <div class="house-tags">
              <span class="tag-area">{{ house.area }}㎡</span>
              <span class="tag-type">{{ getTypeName(house.typeId) }}</span>
              <template v-if="house.facilities">
                <span v-if="hasFacility(house, 'wifi')" class="tag-facility">WiFi</span>
                <span v-if="hasFacility(house, 'elevator')" class="tag-facility">电梯</span>
                <span v-if="hasFacility(house, 'airConditioner')" class="tag-facility">空调</span>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 系统特色介绍 -->
    <div class="features-section">
      <div class="section-header center">
        <div class="section-badge">
          <span>💎</span>
          <span>平台优势</span>
        </div>
        <h2>为什么选择广寓</h2>
        <p>为广州大学生量身打造的租房体验</p>
      </div>

      <div class="feature-list">
        <div class="feature-item" v-for="(feature, index) in systemFeatures" :key="index">
          <div class="feature-icon">{{ feature.emoji }}</div>
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.description }}</p>
        </div>
      </div>
    </div>

    <!-- 注册引导区 -->
    <div class="cta-section" v-if="!isLoggedIn">
      <div class="cta-content">
        <div class="cta-decoration">
          <span class="float-emoji" style="top: 10%; left: 5%;">🏠</span>
          <span class="float-emoji" style="top: 20%; right: 10%;">✨</span>
          <span class="float-emoji" style="bottom: 30%; left: 15%;">💫</span>
          <span class="float-emoji" style="bottom: 20%; right: 20%;">🔑</span>
        </div>
        <h2>加入广寓，开启美好租房生活</h2>
        <p>注册成为会员，享受智能推荐、预约看房等专属服务</p>
        <div class="cta-buttons">
          <button class="btn-primary" @click="navigateTo('/register')">立即注册</button>
          <button class="btn-secondary" @click="navigateTo('/login')">已有账号？登录</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

import request from '@/utils/request'
import DateUtils from '@/utils/dateUtils'
import CarouselBanner from '@/components/CarouselBanner.vue'
import { getPersonalizedRecommendations, getHotHouses } from '@/api/recommendation'
import { checkFavorited, addFavorite, removeFavorite } from '@/api/favorite'

const router = useRouter()
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)

// 基础API路径
const baseAPI = import.meta.env.VITE_BASE_API || '/api'

// 搜索相关
const searchKeyword = ref('')
const quickSearchTags = [
  { label: '精装修', decoration: 2 },
  { label: '豪装', decoration: 3 },
  { label: '整租', typeId: 1 },
  { label: '合租', typeId: 2 },
  { label: '近地铁', tag: '近地铁' },
  { label: '押一付一', paymentMethod: 1 },
  { label: '公寓', buildingType: 1 },
  { label: '南北通透', orientation: 8 }
]

// 热门房源
const featuredHouses = ref([])
const loading = ref(false)

// 系统特色
const systemFeatures = [
  {
    emoji: '🏘️',
    title: '丰富优质房源',
    description: '精选各类房源，满足不同需求，为您提供多样化的居住选择'
  },
  {
    emoji: '📅',
    title: '便捷预约看房',
    description: '在线预约看房，高效便捷，节省您的宝贵时间'
  },
  {
    emoji: '🔒',
    title: '安全可靠保障',
    description: '房东实名认证，房源真实可靠，保障您的租房安全'
  },
  {
    emoji: '📝',
    title: '规范租赁合同',
    description: '标准化合同模板，明确双方权责，避免不必要的纠纷'
  },
  {
    emoji: '💬',
    title: '专业客服团队',
    description: '专业的客服团队，随时为您解答疑问，提供贴心服务'
  },
  {
    emoji: '💰',
    title: '透明价格体系',
    description: '房源价格公开透明，无隐藏费用，租房更放心'
  }
]

// 公告相关
const announcements = ref([])
const loadingAnnouncements = ref(false)

// 获取热门房源
const fetchFeaturedHouses = async () => {
  loading.value = true
  try {
    const apiFunc = isLoggedIn.value ? getPersonalizedRecommendations : getHotHouses
    const res = await apiFunc({ limit: 8 })
    featuredHouses.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (error) {
    console.error('获取推荐房源失败:', error)
    if (isLoggedIn.value) {
      try {
        const res = await getHotHouses({ limit: 8 })
        featuredHouses.value = Array.isArray(res) ? res : (res?.data || [])
      } catch (e) {
        featuredHouses.value = []
      }
    } else {
      featuredHouses.value = []
    }
  } finally {
    loading.value = false
  }
}

// 获取房屋图片
const getHouseImage = (house) => {
  if (!house.images) return '/default-house.jpg'
  try {
    const imageArray = JSON.parse(house.images)
    if (imageArray.length === 0) return '/default-house.jpg'
    const imagePath = imageArray[0]
    return formatImageUrl(imagePath)
  } catch (e) {
    return '/default-house.jpg'
  }
}

// 格式化图片URL
const formatImageUrl = (path) => {
  if (!path) return '/default-house.jpg'
  if (path.startsWith('http')) return path
  return baseAPI + path
}

// 处理搜索
const handleSearch = () => {
  if (!searchKeyword.value.trim()) return
  router.push({
    path: '/houses',
    query: {
      title: searchKeyword.value.trim()
    }
  })
}

// 快速搜索
const quickSearch = (tag) => {
  const query = {}
  if (tag.decoration !== undefined) query.decoration = tag.decoration
  if (tag.typeId !== undefined) query.typeId = tag.typeId
  if (tag.paymentMethod !== undefined) query.paymentMethod = tag.paymentMethod
  if (tag.buildingType !== undefined) query.buildingType = tag.buildingType
  if (tag.orientation !== undefined) query.orientation = tag.orientation
  if (tag.tag) query.tags = tag.tag

  router.push({
    path: '/houses',
    query
  })
}

// 路由跳转
const navigateTo = (path) => {
  router.push(path)
}

// 查看房屋详情
const viewHouseDetail = (id) => {
  router.push(`/houses/${id}`)
}

// 获取最新公告
const fetchLatestAnnouncements = async () => {
  loadingAnnouncements.value = true
  try {
    await request.get('/announcements', {
      status: 1,
      currentPage: 1,
      size: 3
    }, {
      onSuccess: (res) => {
        announcements.value = res.records||[]
      }
    })
  } catch (error) {
    console.error('获取公告失败:', error)
  } finally {
    loadingAnnouncements.value = false
  }
}

// 查看公告详情
const viewAnnouncementDetail = (announcement) => {
  router.push(`/announcements/${announcement.id}`)
}

// 获取内容摘要
const getExcerpt = (content) => {
  if (!content) return ''
  return content.length > 100 ? content.substring(0, 100) + '...' : content
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  return DateUtils.formatDate(date)
}

// 房屋类型映射
const houseTypeNames = {
  1: '整租',
  2: '合租'
}

// 装修情况映射
const decorationNames = {
  0: '毛坯',
  1: '简装',
  2: '精装',
  3: '豪装'
}

const getTypeName = (typeId) => {
  return houseTypeNames[typeId] || '未知'
}

const getDecorationName = (decoration) => {
  return decorationNames[decoration] || ''
}

// 检查房源是否有指定设施
const hasFacility = (house, facilityKey) => {
  if (!house.facilities) return false
  try {
    const facilities = typeof house.facilities === 'string' ? JSON.parse(house.facilities) : house.facilities
    return facilities && facilities[facilityKey] === true
  } catch {
    return false
  }
}

// 切换收藏状态
const toggleFavorite = async (house) => {
  if (!isLoggedIn.value) {
    ElMessageBox.confirm(
      '请先登录后再收藏房源',
      '提示',
      {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'info'
      }
    ).then(() => {
      router.push('/login')
    }).catch(() => {})
    return
  }

  try {
    if (house.isFavorited) {
      await removeFavorite(house.id)
      house.isFavorited = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(house.id)
      house.isFavorited = true
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    console.error('收藏操作失败:', error)
    ElMessage.error('操作失败，请重试')
  }
}

// 批量检查收藏状态
const checkFavoritesStatus = async () => {
  if (!isLoggedIn.value || featuredHouses.value.length === 0) return

  try {
    for (const house of featuredHouses.value) {
      try {
        const res = await checkFavorited(house.id)
        if (typeof res === 'boolean') {
          house.isFavorited = res
        } else if (res?.data !== undefined) {
          house.isFavorited = res.data
        } else {
          house.isFavorited = false
        }
      } catch {
        house.isFavorited = false
      }
    }
  } catch (error) {
    console.error('检查收藏状态失败:', error)
  }
}

onMounted(() => {
  fetchFeaturedHouses().then(() => {
    checkFavoritesStatus()
  })
  fetchLatestAnnouncements()
})
</script>

<style lang="scss" scoped>
@import '@/styles/design-system.scss';

.home-container {
  // ==================== Hero区域 ====================
  .hero-section {
    margin-bottom: var(--space-2xl);
  }

  // ==================== 搜索区域 ====================
  .search-section {
    padding: var(--space-3xl) 0;
    background: linear-gradient(180deg, rgba(255, 107, 107, 0.05) 0%, transparent 100%);
    position: relative;

    .search-container {
      max-width: 700px;
      margin: 0 auto;
      text-align: center;
      padding: 0 24px;

      .search-badge {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 6px 16px;
        background: rgba(255, 107, 107, 0.1);
        color: var(--color-primary);
        border-radius: var(--radius-full);
        font-size: 13px;
        font-weight: 600;
        margin-bottom: var(--space-lg);
        animation: fadeInUp 0.6s ease-out;
      }

      h2 {
        font-family: $font-display;
        font-size: var(--font-5xl);
        font-weight: 800;
        color: var(--color-text-primary);
        margin-bottom: var(--space-md);
        letter-spacing: -1px;
        animation: fadeInUp 0.6s ease-out 0.1s both;
      }

      .search-subtitle {
        font-size: var(--font-lg);
        color: var(--color-text-tertiary);
        margin-bottom: var(--space-xl);
        animation: fadeInUp 0.6s ease-out 0.2s both;
      }

      .search-box-wrapper {
        margin-bottom: var(--space-lg);
        animation: fadeInUp 0.6s ease-out 0.3s both;

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
              padding: 12px 16px;

              .el-input__inner {
                font-size: var(--font-md);
                color: var(--color-text-primary);
              }

              .el-input__prefix {
                font-size: 18px;
                color: var(--color-text-tertiary);
              }
            }
          }

          .search-btn {
            padding: 14px 28px;
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

            &:active {
              transform: translateY(0);
            }
          }
        }
      }

      .quick-tags {
        display: flex;
        flex-wrap: wrap;
        justify-content: center;
        align-items: center;
        gap: 8px;
        animation: fadeInUp 0.6s ease-out 0.4s both;

        .tags-label {
          color: var(--color-text-tertiary);
          font-size: var(--font-sm);
        }

        .quick-tag {
          padding: 8px 16px;
          background: white;
          border: 1px solid var(--color-border);
          border-radius: var(--radius-full);
          font-size: var(--font-sm);
          color: var(--color-text-secondary);
          cursor: pointer;
          transition: all var(--transition-base);

          &:hover {
            border-color: var(--color-primary);
            color: var(--color-primary);
            background: rgba(255, 107, 107, 0.06);
            transform: translateY(-1px);
          }
        }
      }
    }
  }

  // ==================== 公告区域 ====================
  .announcement-section {
    padding: var(--space-3xl) 0;
    background: var(--color-bg-secondary);

    .announcement-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 24px;
    }

    .announcement-card {
      display: flex;
      gap: 16px;
      padding: 20px;
      background: var(--color-bg-primary);
      border-radius: var(--radius-lg);
      border: 1px solid var(--color-border-light);
      cursor: pointer;
      transition: all var(--transition-base);
      margin-bottom: 16px;

      &:hover {
        border-color: var(--color-primary);
        box-shadow: var(--shadow-md);
        transform: translateY(-2px);
      }

      .announcement-icon {
        width: 48px;
        height: 48px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--color-primary-gradient);
        border-radius: var(--radius-md);
        font-size: 24px;
        flex-shrink: 0;
      }

      .announcement-content {
        flex: 1;
        display: flex;
        flex-direction: column;

        .announcement-title {
          font-size: var(--font-lg);
          font-weight: 600;
          color: var(--color-text-primary);
          margin-bottom: 8px;
        }

        .announcement-excerpt {
          flex: 1;
          color: var(--color-text-secondary);
          font-size: var(--font-sm);
          line-height: 1.6;
          margin-bottom: 12px;
          overflow: hidden;
          text-overflow: ellipsis;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
        }

        .announcement-footer {
          display: flex;
          justify-content: space-between;
          align-items: center;

          .announcement-date {
            color: var(--color-text-tertiary);
            font-size: var(--font-xs);
          }

          .read-more {
            color: var(--color-primary);
            font-size: var(--font-sm);
            font-weight: 500;
          }
        }
      }
    }

    .view-all-btn {
      text-align: center;
      margin-top: var(--space-lg);
    }
  }

  // ==================== 房源区域 ====================
  .featured-section {
    padding: var(--space-3xl) 0;

    .loading-container {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 24px;
    }

    .houses-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 24px;
    }

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

        .favorite-btn {
          position: absolute;
          top: 12px;
          right: 12px;
          width: 40px;
          height: 40px;
          display: flex;
          align-items: center;
          justify-content: center;
          background: rgba(255, 255, 255, 0.95);
          border-radius: var(--radius-full);
          border: none;
          cursor: pointer;
          font-size: 20px;
          transition: all var(--transition-base);
          box-shadow: var(--shadow-sm);

          &:hover {
            transform: scale(1.1);
          }

          &.active {
            background: white;
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

            &.tag-facility {
              background: var(--color-bg-tertiary);
              color: var(--color-text-tertiary);
            }
          }
        }
      }
    }
  }

  // ==================== 特色区域 ====================
  .features-section {
    padding: var(--space-3xl) 0;
    background: var(--color-bg-secondary);
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 1px;
      background: linear-gradient(90deg, transparent, var(--color-border) 50%, transparent);
    }

    .feature-list {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 24px;
      margin-top: var(--space-xl);
    }

    .feature-item {
      text-align: center;
      padding: 32px 24px;
      background: var(--color-bg-primary);
      border-radius: var(--radius-xl);
      border: 1px solid var(--color-border-light);
      transition: all var(--transition-base);

      &:hover {
        transform: translateY(-4px);
        box-shadow: var(--shadow-md);
        border-color: rgba(255, 107, 107, 0.3);
      }

      .feature-icon {
        font-size: 48px;
        margin-bottom: 16px;
        display: inline-block;
        filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.1));
      }

      h3 {
        font-family: $font-display;
        font-size: var(--font-lg);
        font-weight: 600;
        color: var(--color-text-primary);
        margin-bottom: 8px;
      }

      p {
        color: var(--color-text-secondary);
        font-size: var(--font-sm);
        line-height: 1.6;
      }
    }
  }

  // ==================== CTA区域 ====================
  .cta-section {
    padding: var(--space-3xl) 0;
    position: relative;

    .cta-content {
      max-width: 700px;
      margin: 0 auto;
      text-align: center;
      padding: 48px 32px;
      background: var(--color-primary-gradient);
      border-radius: var(--radius-2xl);
      box-shadow: var(--shadow-colored);
      position: relative;
      overflow: hidden;

      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M30 0 L30 60 M0 30 L60 30' stroke='rgba(255,255,255,0.1)' stroke-width='1'/%3E%3C/svg%3E");
        opacity: 0.5;
      }

      .cta-decoration {
        position: absolute;
        width: 100%;
        height: 100%;
        top: 0;
        left: 0;
        pointer-events: none;

        .float-emoji {
          position: absolute;
          font-size: 24px;
          animation: float 4s ease-in-out infinite;
        }
      }

      h2 {
        font-family: $font-display;
        font-size: var(--font-4xl);
        font-weight: 800;
        color: white;
        margin-bottom: 12px;
        position: relative;
      }

      p {
        font-size: var(--font-md);
        color: rgba(255, 255, 255, 0.9);
        margin-bottom: 32px;
        position: relative;
      }

      .cta-buttons {
        display: flex;
        justify-content: center;
        gap: 16px;
        flex-wrap: wrap;
        position: relative;

        button {
          padding: 14px 32px;
          border-radius: var(--radius-full);
          font-size: var(--font-md);
          font-weight: 600;
          font-family: $font-display;
          cursor: pointer;
          transition: all var(--transition-base);
          border: none;

          &.btn-primary {
            background: white;
            color: var(--color-primary);

            &:hover {
              box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
              transform: translateY(-2px);
            }
          }

          &.btn-secondary {
            background: transparent;
            color: white;
            border: 2px solid rgba(255, 255, 255, 0.5);

            &:hover {
              background: rgba(255, 255, 255, 0.15);
              border-color: white;
            }
          }
        }
      }
    }
  }
}

// ==================== 通用样式 ====================
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--space-xl);
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
  padding: 0 24px;

  .section-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px 14px;
    background: rgba(255, 107, 107, 0.1);
    color: var(--color-primary);
    border-radius: var(--radius-full);
    font-size: 12px;
    font-weight: 600;
    margin-bottom: 12px;
  }

  .header-left {
    .section-badge {
      margin-bottom: 8px;
    }

    h2 {
      font-family: $font-display;
      font-size: var(--font-3xl);
      font-weight: 700;
      color: var(--color-text-primary);
      margin: 0;
    }
  }

  p {
    color: var(--color-text-tertiary);
    margin-top: 8px;
  }

  .view-more-link {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: var(--color-primary);
    font-weight: 500;
    font-size: var(--font-sm);
    transition: all var(--transition-base);

    &:hover {
      gap: 8px;
    }
  }

  &.center {
    flex-direction: column;
    text-align: center;
    align-items: center;

    h2 {
      font-size: var(--font-3xl);
    }
  }
}

// ==================== 按钮样式 ====================
.btn-outline {
  padding: 12px 28px;
  background: transparent;
  border: 2px solid var(--color-primary);
  color: var(--color-primary);
  border-radius: var(--radius-full);
  font-size: var(--font-md);
  font-weight: 600;
  font-family: $font-display;
  cursor: pointer;
  transition: all var(--transition-base);

  &:hover {
    background: var(--color-primary);
    color: white;
    box-shadow: var(--shadow-colored);
  }
}

// ==================== 响应式 ====================
@media (max-width: 768px) {
  .home-container {
    .search-section {
      padding: var(--space-xl) 0;

      .search-container {
        h2 {
          font-size: var(--font-3xl);
        }

        .search-subtitle {
          font-size: var(--font-md);
        }

        .search-box-wrapper {
          .search-box {
            flex-direction: column;
            padding: 8px;

            .search-btn {
              width: 100%;
            }
          }
        }
      }
    }

    .featured-section {
      .houses-grid {
        grid-template-columns: 1fr;
      }
    }

    .features-section {
      .feature-list {
        grid-template-columns: 1fr;
      }
    }

    .cta-section {
      .cta-content {
        padding: 32px 24px;

        h2 {
          font-size: var(--font-2xl);
        }

        .cta-buttons {
          flex-direction: column;

          button {
            width: 100%;
          }
        }
      }
    }
  }

  .section-header {
    &.center {
      h2 {
        font-size: var(--font-2xl);
      }
    }
  }
}

// ==================== 动画 ====================
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
