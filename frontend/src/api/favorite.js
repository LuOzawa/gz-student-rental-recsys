import request from '@/utils/request'

// 添加收藏
export function addFavorite(houseId, note) {
  return request.post(`/favorites/${houseId}`, { note })
}

// 取消收藏
export function removeFavorite(houseId) {
  return request.delete(`/favorites/${houseId}`)
}

// 检查是否已收藏
export function checkFavorited(houseId) {
  return request.get(`/favorites/check/${houseId}`)
}

// 获取收藏列表
export function getFavoriteList(params) {
  return request.get('/favorites/list', params)
}

// 获取收藏数量
export function getFavoriteCount() {
  return request.get('/favorites/count')
}
