<template>
  <div class="top-rated-houses">
    <div class="page-header">
      <h1 class="page-title">房源评分排行榜</h1>
      <p class="page-subtitle">根据用户评价为您推荐优质房源</p>
    </div>

    <el-card shadow="hover" class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="显示数量">
          <el-select v-model="filterForm.limit" @change="fetchTopRatedHouses" style="width: 120px">
            <el-option label="前10名" :value="10" />
            <el-option label="前20名" :value="20" />
            <el-option label="前30名" :value="30" />
            <el-option label="前50名" :value="50" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchTopRatedHouses" :icon="Refresh">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-loading="loading" class="houses-container">
      <el-empty v-if="!loading && houseList.length === 0" description="暂无评分数据" />

      <div v-else class="house-list">
        <div
          v-for="(house, index) in houseList"
          :key="house.id"
          class="house-card-wrapper"
        >
          <el-card
            shadow="hover"
            class="house-card"
            :class="{ 'top-three': index < 3 }"
          >
            <div class="rank-badge" :class="`rank-${index + 1}`">
              <span v-if="index === 0" class="rank-icon">🥇</span>
              <span v-else-if="index === 1" class="rank-icon">🥈</span>
              <span v-else-if="index === 2" class="rank-icon">🥉</span>
              <span v-else class="rank-number">{{ index + 1 }}</span>
            </div>

            <div class="house-content">
              <div class="house-image">
                <el-image
                  :src="getHouseImage(house)"
                  fit="cover"
                  :preview-src-list="formatImageList(house.images)"
                >
                  <template #error>
                    <div class="image-error">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
              </div>

              <div class="house-info">
                <div class="house-header">
                  <h3 class="house-title">{{ house.title }}</h3>
                  <div class="house-rating">
                    <el-rate
                      v-model="house.avgScore"
                      disabled
                      show-score
                      :colors="['#ff9900', '#ff9900', '#ff9900']"
                    >
                      <template #default="{ value }">
                        <span class="score-text">{{ value?.toFixed(1) || '0.0' }}</span>
                      </template>
                    </el-rate>
                  </div>
                </div>

                <div class="house-details">
                  <el-tag type="info" size="small">{{ house.typeName }}</el-tag>
                  <span class="detail-item">
                    <el-icon><Location /></el-icon>
                    {{ house.address }}
                  </span>
                </div>

                <div class="house-stats">
                  <div class="stat-item">
                    <el-icon><House /></el-icon>
                    <span>{{ house.area }} ㎡</span>
                  </div>
                  <div class="stat-item">
                    <el-icon><ChatDotRound /></el-icon>
                    <span>{{ house.commentCount || 0 }} 条评价</span>
                  </div>
                  <div class="stat-item">
                    <el-icon><User /></el-icon>
                    <span>{{ house.landlordName }}</span>
                  </div>
                </div>

                <div class="house-footer">
                  <div class="house-price">
                    <span class="price-label">月租金</span>
                    <span class="price-value">¥{{ house.price }}</span>
                    <span class="price-unit">/月</span>
                  </div>
                  <el-button type="primary" @click="viewHouseDetail(house.id)">
                    查看详情
                  </el-button>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture, Location, House, ChatDotRound, User, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()

// 基础API路径
const baseAPI = import.meta.env.VITE_BASE_API || '/api'

// 状态数据
const loading = ref(false)
const houseList = ref([])
const filterForm = reactive({
  limit: 10
})

// 初始化
onMounted(() => {
  fetchTopRatedHouses()
})

// 获取评分排行榜
const fetchTopRatedHouses = async () => {
  loading.value = true
  try {
    await request.get('/houses/top-rated', {
      limit: filterForm.limit
    }, {
      onSuccess: (res) => {
        houseList.value = res || []
      }
    })
  } catch (error) {
    console.error('获取评分排行榜失败:', error)
    ElMessage.error('获取评分排行榜失败')
  } finally {
    loading.value = false
  }
}

// 获取房屋图片
const getHouseImage = (house) => {
  if (!house.images) return ''
  try {
    const imageArray = JSON.parse(house.images)
    if (imageArray.length === 0) return ''
    const imagePath = imageArray[0]
    return formatImageUrl(imagePath)
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

// 查看房屋详情
const viewHouseDetail = (id) => {
  router.push(`/houses/${id}`)
}
</script>

<style lang="scss" scoped>
.top-rated-houses {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;

  .page-header {
    text-align: center;
    margin-bottom: 40px;
    color: white;

    .page-title {
      font-size: 36px;
      font-weight: 700;
      margin-bottom: 10px;
      text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
    }

    .page-subtitle {
      font-size: 16px;
      opacity: 0.9;
    }
  }

  .filter-card {
    max-width: 1200px;
    margin: 0 auto 30px;
    border-radius: 12px;
  }

  .houses-container {
    max-width: 1200px;
    margin: 0 auto;
    min-height: 400px;

    .house-list {
      display: flex;
      flex-direction: column;
      gap: 20px;

      .house-card-wrapper {
        position: relative;

        .house-card {
          border-radius: 16px;
          transition: all 0.3s ease;
          position: relative;
          overflow: visible;

          &.top-three {
            border: 2px solid #ffd700;
          }

          &:hover {
            transform: translateY(-5px);
            box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
          }

          .rank-badge {
            position: absolute;
            top: -10px;
            left: -10px;
            width: 50px;
            height: 50px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: bold;
            font-size: 18px;
            z-index: 10;
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);

            &.rank-1 {
              background: linear-gradient(135deg, #ffd700, #ffed4e);
              color: #8b4513;
              .rank-icon {
                font-size: 24px;
              }
            }

            &.rank-2 {
              background: linear-gradient(135deg, #c0c0c0, #e8e8e8);
              color: #696969;
              .rank-icon {
                font-size: 24px;
              }
            }

            &.rank-3 {
              background: linear-gradient(135deg, #cd7f32, #e6a15e);
              color: #4a2511;
              .rank-icon {
                font-size: 24px;
              }
            }

            &:not(.rank-1):not(.rank-2):not(.rank-3) {
              background: linear-gradient(135deg, #667eea, #764ba2);
              color: white;

              .rank-number {
                font-size: 20px;
              }
            }
          }

          .house-content {
            display: flex;
            gap: 20px;

            .house-image {
              flex-shrink: 0;
              width: 280px;
              height: 200px;
              border-radius: 12px;
              overflow: hidden;

              :deep(.el-image) {
                width: 100%;
                height: 100%;
              }

              .image-error {
                width: 100%;
                height: 100%;
                display: flex;
                justify-content: center;
                align-items: center;
                background: linear-gradient(135deg, #f5f7fa, #c3cfe2);
                color: #909399;
                font-size: 40px;
              }
            }

            .house-info {
              flex: 1;
              display: flex;
              flex-direction: column;
              justify-content: space-between;

              .house-header {
                display: flex;
                justify-content: space-between;
                align-items: flex-start;
                margin-bottom: 12px;

                .house-title {
                  font-size: 22px;
                  font-weight: 600;
                  color: #303133;
                  margin: 0;
                  flex: 1;
                  margin-right: 20px;
                }

                .house-rating {
                  :deep(.el-rate) {
                    height: auto;
                  }

                  .score-text {
                    font-size: 20px;
                    font-weight: 600;
                    color: #ff9900;
                    margin-left: 8px;
                  }
                }
              }

              .house-details {
                display: flex;
                align-items: center;
                gap: 12px;
                margin-bottom: 12px;

                .detail-item {
                  display: flex;
                  align-items: center;
                  gap: 4px;
                  color: #606266;
                  font-size: 14px;
                }
              }

              .house-stats {
                display: flex;
                gap: 24px;
                margin-bottom: 16px;

                .stat-item {
                  display: flex;
                  align-items: center;
                  gap: 6px;
                  color: #909399;
                  font-size: 14px;

                  .el-icon {
                    font-size: 16px;
                  }
                }
              }

              .house-footer {
                display: flex;
                justify-content: space-between;
                align-items: center;

                .house-price {
                  display: flex;
                  align-items: baseline;
                  gap: 6px;

                  .price-label {
                    font-size: 14px;
                    color: #909399;
                  }

                  .price-value {
                    font-size: 28px;
                    font-weight: 700;
                    color: #f56c6c;
                  }

                  .price-unit {
                    font-size: 14px;
                    color: #909399;
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .top-rated-houses {
    padding: 20px 10px;

    .page-header {
      .page-title {
        font-size: 28px;
      }

      .page-subtitle {
        font-size: 14px;
      }
    }

    .houses-container {
      .house-list {
        .house-card-wrapper {
          .house-card {
            .house-content {
              flex-direction: column;

              .house-image {
                width: 100%;
                height: 180px;
              }

              .house-info {
                .house-header {
                  flex-direction: column;
                  align-items: flex-start;
                  gap: 10px;

                  .house-title {
                    font-size: 18px;
                    margin-right: 0;
                  }
                }

                .house-stats {
                  flex-wrap: wrap;
                  gap: 12px;
                }

                .house-footer {
                  flex-direction: column;
                  align-items: flex-start;
                  gap: 12px;

                  .el-button {
                    width: 100%;
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
</style>
