import request from '@/utils/request'

// 获取个性化推荐房源
export function getPersonalizedRecommendations(params) {
  return request.get('/recommendations/personalized', params)
}

// 获取相似房源
export function getSimilarHouses(houseId, params) {
  return request.get(`/recommendations/similar/${houseId}`, params)
}

// 获取热门房源
export function getHotHouses(params) {
  return request.get('/recommendations/hot', params)
}

// 获取新用户推荐(冷启动)
export function getColdStartRecommendations(params) {
  return request.get('/recommendations/cold-start', params)
}

// 获取最近搜索关键词
export function getRecentSearchKeywords(params) {
  return request.get('/recommendations/search-keywords', params)
}

// 智能混合推荐（动态权重）
export function getSmartHybridRecommendations(params) {
  return request.get('/recommendations/smart-hybrid', params)
}

// Item-Based协同过滤推荐
export function getItemBasedCF(params) {
  return request.get('/recommendations/item-cf', params)
}

// User-Based协同过滤推荐
export function getUserBasedCF(params) {
  return request.get('/recommendations/user-cf', params)
}

// 混合推荐(User-CF + Item-CF)
export function getHybridRecommendations(params) {
  return request.get('/recommendations/hybrid', params)
}

// 基于内容的推荐
export function getContentBasedRecommendations(params) {
  return request.get('/recommendations/content-based', params)
}

// 基于用户偏好的推荐
export function getPreferenceBasedRecommendations(params) {
  return request.get('/recommendations/preference-based', params)
}

// 获取推荐解释
export function getRecommendationExplanation(houseId) {
  return request.get(`/recommendations/explanation/${houseId}`)
}

// 获取推荐房源列表（含推荐理由）
export function fetchRecommendationsWithReasons(params) {
  return request.get('/recommendations/with-reasons', params)
}
