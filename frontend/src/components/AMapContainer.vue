<template>
  <div class="amap-container" ref="mapContainer"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'

const props = defineProps({
  // 房源列表数据
  houses: {
    type: Array,
    default: () => []
  },
  // 地图中心点 { lng, lat }
  center: {
    type: Object,
    default: () => ({ lng: 113.40, lat: 23.05 })
  },
  // 地图缩放级别
  zoom: {
    type: Number,
    default: 13
  },
  // 是否显示周边设施
  showFacilities: {
    type: Boolean,
    default: false
  },
  // 周边设施类型
  facilityTypes: {
    type: Array,
    default: () => ['150500', '050000', '060000', '070000']
  },
  // 当前选中的房源ID
  selectedHouseId: {
    type: [Number, String],
    default: null
  }
})

const emit = defineEmits(['markerClick', 'markerDoubleClick', 'boundsChange', 'mapReady'])

const mapContainer = ref(null)
let map = null
let markers = []
let facilityMarkers = []
let infoWindow = null

// 高德地图配置
const AMAP_CONFIG = {
  key: '3a3471f3d38b1f56d507b51ae00473b1',
  version: '2.0',
  plugins: ['AMap.MarkerClusterer', 'AMap.Geolocation', 'AMap.PlaceSearch', 'AMap.ToolBar', 'AMap.Scale']
}

// 初始化地图
const initMap = () => {
  AMapLoader.load({
    key: AMAP_CONFIG.key,
    version: AMAP_CONFIG.version,
    plugins: AMAP_CONFIG.plugins
  }).then((AMap) => {
    // 创建地图实例
    map = new AMap.Map(mapContainer.value, {
      zoom: props.zoom,
      center: [props.center.lng, props.center.lat],
      mapStyle: 'amap://styles/whites',
      viewMode: '2D'
    })

    // 添加工具条
    map.addControl(new AMap.ToolBar({
      position: {
        top: '110px',
        right: '40px'
      }
    }))

    // 添加比例尺
    map.addControl(new AMap.Scale())

    // 创建信息窗体
    infoWindow = new AMap.InfoWindow({
      isCustom: false,
      offset: new AMap.Pixel(0, -30)
    })

    // 监听地图事件
    map.on('moveend', handleBoundsChange)
    map.on('zoomend', handleBoundsChange)
    map.on('click', () => {
      infoWindow.close()
    })

    // 添加房源标记
    updateMarkers()

    // 发送地图就绪事件
    emit('mapReady', map)
  }).catch((e) => {
    console.error('地图加载失败:', e)
  })
}

// 更新房源标记
const updateMarkers = () => {
  if (!map) return

  // 清除旧标记
  clearMarkers()

  // 创建房源标记
  const markerData = []

  props.houses.forEach((house, index) => {
    if (house.latitude && house.longitude) {
      const position = [house.longitude, house.latitude]

      // 创建标记图标
      const iconDiv = document.createElement('div')
      iconDiv.className = 'custom-marker'
      iconDiv.style.cssText = `
        width: 32px;
        height: 32px;
        background: ${house.id === props.selectedHouseId ? '#ff6b6b' : '#4CAF50'};
        border: 3px solid white;
        border-radius: 50%;
        box-shadow: 0 2px 8px rgba(0,0,0,0.3);
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;
        color: white;
        font-weight: bold;
        position: relative;
      `

      // 添加价格标签
      const priceTag = document.createElement('div')
      priceTag.style.cssText = `
        position: absolute;
        bottom: -25px;
        left: 50%;
        transform: translateX(-50%);
        background: rgba(0,0,0,0.8);
        color: white;
        padding: 2px 6px;
        border-radius: 4px;
        font-size: 12px;
        white-space: nowrap;
      `
      priceTag.textContent = `¥${house.price}`
      iconDiv.appendChild(priceTag)

      const marker = new AMap.Marker({
        position: position,
        content: iconDiv,
        offset: new AMap.Pixel(-16, -16),
        title: house.title,
        extData: { house, index }
      })

      // 点击标记事件
      marker.on('click', (e) => {
        const houseData = e.target.getExtData().house
        showInfoWindow(houseData, position)
        emit('markerClick', houseData)
      })

      // 双击标记事件 - 跳转详情
      marker.on('dblclick', (e) => {
        const houseData = e.target.getExtData().house
        // 停止事件冒泡，防止触发其他点击事件
        e.stopPropagation()
        emit('markerDoubleClick', houseData)
      })

      markers.push(marker)
      markerData.push(marker)
    }
  })

  // 批量添加标记到地图
  if (markers.length > 0) {
    map.add(markers)

    // 如果标记太多，使用聚合
    if (markers.length > 50) {
      // 使用标记聚合器
      // 这里简化处理，实际可以使用 AMap.MarkerClusterer
    }
  }

  // 调整地图视野以包含所有标记
  if (markers.length > 1) {
    const bounds = new AMap.Bounds()
    markers.forEach(marker => {
      bounds.extend(marker.getPosition())
    })
    map.setBounds(bounds)
  } else if (markers.length === 1) {
    map.setCenter(markers[0].getPosition())
  }
}

// 清除标记
const clearMarkers = () => {
  if (markers.length > 0) {
    map.remove(markers)
    markers = []
  }
  if (facilityMarkers.length > 0) {
    map.remove(facilityMarkers)
    facilityMarkers = []
  }
}

// 显示信息窗体
const showInfoWindow = (house, position) => {
  const content = `
    <div style="padding: 12px; min-width: 200px;">
      <h4 style="margin: 0 0 8px 0; font-size: 16px; color: #333;">${house.title}</h4>
      <p style="margin: 4px 0; color: #666; font-size: 13px;">
        <i style="color: #999;">📍</i> ${house.address || ''}
      </p>
      <p style="margin: 4px 0; color: #ff6b6b; font-weight: bold; font-size: 16px;">
        ¥${house.price}/月
      </p>
      <p style="margin: 4px 0; color: #666; font-size: 13px;">
        ${house.area || 0}㎡ | ${house.typeName || '未知房型'} | ${house.decorationName || '简装'}
      </p>
    </div>
  `
  infoWindow.setContent(content)
  infoWindow.open(map, position)
}

// 处理地图边界变化
const handleBoundsChange = () => {
  if (!map) return

  const bounds = map.getBounds()
  const northeast = bounds.getNorthEast()
  const southwest = bounds.getSouthwest()

  emit('boundsChange', {
    minLng: southwest.getLng(),
    maxLng: northeast.getLng(),
    minLat: southwest.getLat(),
    maxLat: northeast.getLat()
  })
}

// 设置地图中心
const setCenter = (lng, lat) => {
  if (map) {
    map.setCenter([lng, lat])
  }
}

// 设置地图缩放级别
const setZoom = (zoom) => {
  if (map) {
    map.setZoom(zoom)
  }
}

// 添加周边设施标记
const addFacilityMarkers = (facilities) => {
  if (!map || !facilities || facilities.length === 0) return

  clearFacilityMarkers()

  const AMap = window.AMap
  const facilityIcons = {
    '150500': '🚇', // 交通
    '050000': '🍴', // 餐饮
    '060000': '🛒', // 购物
    '070000': '🏥'  // 生活
  }

  facilities.forEach(facility => {
    if (facility.longitude && facility.latitude) {
      const iconDiv = document.createElement('div')
      iconDiv.style.cssText = `
        width: 28px;
        height: 28px;
        background: white;
        border: 2px solid #666;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;
        box-shadow: 0 2px 4px rgba(0,0,0,0.2);
      `
      iconDiv.textContent = facilityIcons[facility.type] || '📍'

      const marker = new AMap.Marker({
        position: [facility.longitude, facility.latitude],
        content: iconDiv,
        offset: new AMap.Pixel(-14, -14),
        title: facility.name
      })

      facilityMarkers.push(marker)
    }
  })

  if (facilityMarkers.length > 0) {
    map.add(facilityMarkers)
  }
}

const clearFacilityMarkers = () => {
  if (facilityMarkers.length > 0) {
    map.remove(facilityMarkers)
    facilityMarkers = []
  }
}

// 监听props变化
watch(() => props.houses, () => {
  updateMarkers()
}, { deep: true })

watch(() => props.center, (newCenter) => {
  if (map && newCenter) {
    map.setCenter([newCenter.lng, newCenter.lat])
  }
}, { deep: true })

watch(() => props.zoom, (newZoom) => {
  if (map) {
    map.setZoom(newZoom)
  }
})

watch(() => props.selectedHouseId, () => {
  updateMarkers()
})

// 生命周期
onMounted(() => {
  nextTick(() => {
    initMap()
  })
})

onUnmounted(() => {
  if (map) {
    map.destroy()
    map = null
  }
})

// 暴露方法给父组件
defineExpose({
  setCenter,
  setZoom,
  addFacilityMarkers,
  clearFacilityMarkers,
  updateMarkers
})
</script>

<style scoped>
.amap-container {
  width: 100%;
  height: 100%;
  min-height: 400px;
  background-color: #f0f0f0;
  position: relative;
}

.custom-marker {
  transition: all 0.3s ease;
}

.custom-marker:hover {
  transform: scale(1.1);
}

/* 自定义 infoWindow 样式 */
:deep(.amap-info-content) {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

:deep(.amap-info-close) {
  display: none !important;
}
</style>
