<template>
  <div class="house-list">
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="houses.length === 0" class="empty-container">
      <el-empty description="暂无房源" />
    </div>

    <div v-else class="houses-grid">
      <div
        v-for="house in houses"
        :key="house.id"
        class="house-card"
        @click="$router.push(`/houses/${house.id}`)"
      >
        <div class="house-image">
          <img
            :src="getHouseImage(house.images)"
            :alt="house.title"
          />
          <div class="price">¥{{ house.price }}/月</div>
        </div>
        <div class="house-info">
          <h3 class="house-title">{{ house.title }}</h3>
          <p class="house-address">{{ house.address }}</p>
          <div class="house-details">
            <span v-if="house.floor !== null && house.totalFloors" class="detail-item">
              {{ house.floor }}/{{ house.totalFloors }}层
            </span>
            <span v-if="house.orientation !== null" class="detail-item">
              {{ getOrientationName(house.orientation) }}
            </span>
            <span v-if="house.decoration !== null" class="detail-item">
              {{ getDecorationName(house.decoration) }}
            </span>
          </div>
          <div class="house-tags">
            <el-tag size="small">{{ house.area }}㎡</el-tag>
            <el-tag size="small" type="success">{{ getTypeName(house.typeId) }}</el-tag>
            <el-tag v-if="house.avgScore > 0" size="small" type="warning">
              评分: {{ house.avgScore }}
            </el-tag>
            <el-tag v-if="house.viewCount" size="small" type="info">
              {{ house.viewCount }}次浏览
            </el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  houses: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const houseTypes = {
  1: '整租',
  2: '合租'
}

const decorationNames = {
  0: '毛坯',
  1: '简装',
  2: '精装',
  3: '豪装'
}

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

const getHouseImage = (images) => {
  if (!images) return '/img/default.jpeg'
  try {
    const imageList = JSON.parse(images)
    if (!imageList || imageList.length === 0) return '/img/default.jpeg'
    const imagePath = imageList[0]
    // 处理相对路径，添加baseAPI
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

const getDecorationName = (decoration) => {
  return decorationNames[decoration] || ''
}

const getOrientationName = (orientation) => {
  return orientationNames[orientation] || ''
}
</script>

<style scoped>
.loading-container,
.empty-container {
  padding: 40px 0;
}

.houses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.house-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.house-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.house-image {
  position: relative;
  height: 180px;
  overflow: hidden;
}

.house-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.price {
  position: absolute;
  bottom: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 6px 10px;
  font-size: 14px;
  font-weight: bold;
}

.house-info {
  padding: 12px;
}

.house-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 6px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.house-address {
  font-size: 13px;
  color: #606266;
  margin: 0 0 6px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.house-details {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;

  .detail-item {
    font-size: 12px;
    color: #909399;
    padding: 2px 6px;
    background: #f5f7fa;
    border-radius: 4px;
  }
}

.house-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
</style>
