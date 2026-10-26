<template>
  <div class="carousel-banner" :class="{ 'is-loading': loading }">
    <el-carousel v-if="carouselItems.length > 0" :height="height" :interval="interval" arrow="always" indicator-position="outside">
      <el-carousel-item v-for="(item, index) in carouselItems" :key="index">
        <div class="carousel-item" :style="{ backgroundImage: `url(${formatImageUrl(item.imageUrl)})` }">
          <div class="carousel-content" v-if="showContent">
            <h1>{{ item.title }}</h1>
            <p>{{ item.description }}</p>
            <el-button v-if="item.linkUrl" type="primary" size="large" @click="navigateTo(item.linkUrl)">
              查看详情
            </el-button>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>
    <div v-else-if="loading" class="carousel-loading">
      <el-skeleton :rows="3" animated />
    </div>
    <div v-else class="carousel-empty">
      <el-empty description="暂无轮播图" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, defineProps } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const props = defineProps({
  height: {
    type: String,
    default: '500px'
  },
  interval: {
    type: Number,
    default: 4000
  },
  showContent: {
    type: Boolean,
    default: true
  }
})

const router = useRouter()
const loading = ref(false)
const carouselItems = ref([])

// 基础API路径
const baseAPI = import.meta.env.VITE_BASE_API || '/api'

// 初始化
onMounted(() => {
  fetchCarousels()
})

// 获取轮播图数据
const fetchCarousels = async () => {
  loading.value = true
  try {
    await request.get('/carousels', {}, {
      onSuccess: (data) => {
        carouselItems.value = data
      }
    })
  } catch (error) {
    console.error('获取轮播图失败:', error)
  } finally {
    loading.value = false
  }
}

// 格式化图片URL
const formatImageUrl = (path) => {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return baseAPI + path
}

// 导航到链接
const navigateTo = (path) => {
  if (path.startsWith('http')) {
    window.open(path, '_blank')
  } else {
    router.push(path)
  }
}
</script>

<style lang="scss" scoped>
.carousel-banner {
  position: relative;
  
  .carousel-item {
    height: 100%;
    background-size: cover;
    background-position: center;
    display: flex;
    align-items: center;
    
    .carousel-content {
      max-width: 600px;
      margin-left: 10%;
      color: white;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
      
      h1 {
        font-size: 40px;
        font-weight: 700;
        margin-bottom: 20px;
      }
      
      p {
        font-size: 18px;
        margin-bottom: 30px;
      }
    }
  }
  
  .carousel-loading,
  .carousel-empty {
    height: v-bind('props.height');
    display: flex;
    justify-content: center;
    align-items: center;
    background-color: #f5f7fa;
  }
  
  &.is-loading {
    opacity: 0.7;
  }
  
  :deep(.el-carousel__arrow) {
    background-color: rgba(0, 0, 0, 0.5);
    
    &:hover {
      background-color: rgba(0, 0, 0, 0.8);
    }
  }
  
  :deep(.el-carousel__indicators) {
    padding: 8px 0;
  }
}

// 响应式调整
@media (max-width: 768px) {
  .carousel-banner {
    .carousel-item {
      .carousel-content {
        margin-left: 5%;
        max-width: 90%;
        
        h1 {
          font-size: 28px;
        }
        
        p {
          font-size: 16px;
        }
      }
    }
  }
}
</style> 