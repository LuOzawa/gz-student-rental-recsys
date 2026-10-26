<template>
  <div class="house-detail-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="skeleton-wrapper">
        <div class="skeleton-carousel"></div>
        <div class="skeleton-info">
          <div v-for="i in 8" :key="i" class="skeleton-line"></div>
        </div>
      </div>
    </div>

    <!-- 未找到房源 -->
    <div v-else-if="!house" class="not-found">
      <div class="not-found-content">
        <span class="not-found-icon">🏠</span>
        <h3>未找到该房源信息</h3>
        <p>该房源可能已被删除或下架</p>
        <button class="btn-primary" @click="goBack">返回房源列表</button>
      </div>
    </div>

    <!-- 房源详情 -->
    <template v-else>
      <!-- 顶部标题区域 -->
      <div class="detail-header">
        <div class="header-content">
          <div class="header-badges">
            <span v-if="house.status === 1" class="badge badge-success">可租赁</span>
            <span v-else-if="house.status === 2" class="badge badge-warning">已出租</span>
            <span v-else class="badge badge-info">未上架</span>
            <span class="badge badge-type">{{ house.typeName }}</span>
          </div>
          <h1 class="house-title">{{ house.title }}</h1>
          <p class="house-address">
            <span>📍</span>
            {{ house.address }}
            <span v-if="house.communityName" class="community-name">· {{ house.communityName }}</span>
          </p>
          <div class="header-price">
            <span class="price-amount">¥{{ house.price }}</span>
            <span class="price-unit">/月</span>
          </div>
        </div>
      </div>

      <!-- 主内容布局 -->
      <div class="detail-main">
        <!-- 左侧内容 -->
        <div class="main-content">
          <!-- 房屋图片轮播 -->
          <div class="house-carousel-card">
            <el-carousel v-if="houseImages.length > 0" :interval="0" height="400px" arrow="always" indicator-position="outside">
              <el-carousel-item v-for="(image, index) in houseImages" :key="index">
                <img
                  :src="image"
                  class="carousel-image"
                  @click="openImagePreview(index)"
                />
              </el-carousel-item>
            </el-carousel>
            <div v-else class="no-images">
              <span>📸</span>
              <span>暂无图片</span>
            </div>
          </div>

          <!-- 房屋信息卡片 -->
          <div class="info-card">
            <div class="card-title">
              <span class="title-icon">🏠</span>
              <h3>房屋信息</h3>
            </div>

            <!-- 基本信息网格 -->
            <div class="info-grid">
              <div class="info-item">
                <span class="info-icon">📐</span>
                <div class="info-content">
                  <span class="info-label">户型</span>
                  <span class="info-value">{{ house.typeName }}</span>
                </div>
              </div>
              <div class="info-item">
                <span class="info-icon">📏</span>
                <div class="info-content">
                  <span class="info-label">面积</span>
                  <span class="info-value">{{ house.area }}㎡</span>
                </div>
              </div>
              <div class="info-item">
                <span class="info-icon">🏢</span>
                <div class="info-content">
                  <span class="info-label">楼层</span>
                  <span class="info-value">{{ house.floor }}/{{ house.totalFloors }}层</span>
                </div>
              </div>
              <div class="info-item">
                <span class="info-icon">🎨</span>
                <div class="info-content">
                  <span class="info-label">装修</span>
                  <span class="info-value">{{ getDecorationName(house.decoration) }}</span>
                </div>
              </div>
              <div class="info-item">
                <span class="info-icon">☀️</span>
                <div class="info-content">
                  <span class="info-label">朝向</span>
                  <span class="info-value">{{ getOrientationName(house.orientation) }}</span>
                </div>
              </div>
              <div class="info-item">
                <span class="info-icon">🏗️</span>
                <div class="info-content">
                  <span class="info-label">类型</span>
                  <span class="info-value">{{ getBuildingTypeName(house.buildingType) }}</span>
                </div>
              </div>
              <div class="info-item">
                <span class="info-icon">💳</span>
                <div class="info-content">
                  <span class="info-label">付款方式</span>
                  <span class="info-value">{{ getPaymentMethodName(house.paymentMethod) }}</span>
                </div>
              </div>
              <div class="info-item" v-if="house.minLeaseTerm">
                <span class="info-icon">📅</span>
                <div class="info-content">
                  <span class="info-label">最短租期</span>
                  <span class="info-value">{{ house.minLeaseTerm }}个月</span>
                </div>
              </div>
              <div class="info-item" v-if="house.buildYear">
                <span class="info-icon">📆</span>
                <div class="info-content">
                  <span class="info-label">建筑年代</span>
                  <span class="info-value">{{ house.buildYear }}年</span>
                </div>
              </div>
              <div class="info-item" v-if="house.checkInTime">
                <span class="info-icon">🔑</span>
                <div class="info-content">
                  <span class="info-label">入住时间</span>
                  <span class="info-value">{{ house.checkInTime }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 配套设施 -->
          <div class="info-card">
            <div class="card-title">
              <span class="title-icon">🛠️</span>
              <h3>配套设施</h3>
            </div>
            <div class="facilities-grid">
              <div
                v-for="(value, key) in houseFacilities"
                :key="key"
                class="facility-item"
                :class="{ 'available': value }"
              >
                <span class="facility-icon">{{ value ? '✅' : '❌' }}</span>
                <span class="facility-label">{{ getFacilityLabel(key) }}</span>
              </div>
            </div>
          </div>

          <!-- 房源描述 -->
          <div class="info-card" v-if="house.description">
            <div class="card-title">
              <span class="title-icon">📝</span>
              <h3>房屋描述</h3>
            </div>
            <div class="description-content">
              {{ house.description }}
            </div>
          </div>

          <!-- 房源标签 -->
          <div class="info-card" v-if="houseTags.length > 0">
            <div class="card-title">
              <span class="title-icon">🏷️</span>
              <h3>房源标签</h3>
            </div>
            <div class="tags-list">
              <span v-for="(tag, index) in houseTags" :key="index" class="tag-item">{{ tag }}</span>
            </div>
          </div>

          <!-- 租客评价 -->
          <div class="info-card reviews-card">
            <div class="card-title">
              <span class="title-icon">⭐</span>
              <h3>租客评价</h3>
              <div class="review-summary" v-if="house.avgScore > 0">
                <span class="avg-score">{{ house.avgScore }}</span>
                <span class="review-count">{{ house.commentCount || 0 }} 条评价</span>
              </div>
            </div>

            <div v-if="reviews.length > 0" class="reviews-list">
              <div v-for="review in reviews" :key="review.id" class="review-item">
                <div class="review-header">
                  <div class="user-info">
                    <div class="user-avatar">{{ (review.username || '匿').charAt(0) }}</div>
                    <div class="user-details">
                      <span class="username">{{ review.username || '匿名用户' }}</span>
                      <span class="user-tag">已租住</span>
                    </div>
                  </div>
                  <div class="review-meta">
                    <span class="review-score">⭐ {{ review.score }}</span>
                    <span class="review-time">{{ review.createTime }}</span>
                  </div>
                </div>
                <div class="review-content">{{ review.content }}</div>
                <div v-if="review.replyContent" class="landlord-reply">
                  <div class="reply-label">
                    <span>💬</span>
                    <span>{{ review.replyUserType === 2 ? '管理员' : '房东' }}回复</span>
                  </div>
                  <div class="reply-content">{{ review.replyContent }}</div>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">
              <span>💬</span>
              <p>暂无租客评价</p>
            </div>
          </div>

          <!-- 房源留言 -->
          <div class="info-card comments-card">
            <div class="card-title">
              <span class="title-icon">💬</span>
              <h3>房源留言</h3>
            </div>

            <!-- 发表留言表单 -->
            <div v-if="isLoggedIn" class="comment-form">
              <div class="form-alert">
                <span>💡</span>
                <span>您可以在此留言咨询房源相关信息</span>
              </div>
              <el-form :model="commentForm">
                <el-input
                  v-model="commentForm.content"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入您想咨询的问题..."
                  maxlength="500"
                  show-word-limit
                />
                <button class="btn-primary" @click="submitComment" :loading="submittingComment">
                  发表留言
                </button>
              </el-form>
            </div>
            <div v-else class="login-prompt">
              <span>🔐</span>
              <p>请先登录后发表留言</p>
              <router-link to="/login" class="btn-link">去登录</router-link>
            </div>

            <div v-if="comments.length > 0" class="comments-list">
              <div v-for="comment in comments" :key="comment.id" class="comment-item">
                <div class="comment-header">
                  <div class="user-avatar">{{ (comment.username || '匿').charAt(0) }}</div>
                  <div class="user-details">
                    <span class="username">{{ comment.username || '匿名用户' }}</span>
                    <span class="comment-time">{{ comment.createTime }}</span>
                  </div>
                </div>
                <div class="comment-content">{{ comment.content }}</div>
                <div v-if="comment.replyContent" class="landlord-reply">
                  <div class="reply-label">
                    <span>💬</span>
                    <span>{{ comment.replyUserType === 2 ? '管理员' : '房东' }}回复</span>
                  </div>
                  <div class="reply-content">{{ comment.replyContent }}</div>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">
              <span>💬</span>
              <p>暂无留言</p>
            </div>
          </div>

          <!-- 位置及周边地图 -->
          <div class="info-card map-card" v-if="house.longitude && house.latitude">
            <div class="card-title">
              <span class="title-icon">🗺️</span>
              <h3>位置及周边</h3>
            </div>
            <div class="map-section">
              <div class="map-container">
                <AMapContainer
                  :houses="[house]"
                  :center="mapCenter"
                  :zoom="15"
                  :show-facilities="true"
                />
              </div>
            </div>
          </div>

          <!-- 相似房源 -->
          <div class="info-card similar-card">
            <div class="card-title">
              <span class="title-icon">🏘️</span>
              <h3>相似房源</h3>
              <router-link to="/recommendations" class="view-more">查看更多 →</router-link>
            </div>

            <div v-if="similarHousesLoading" class="loading-grid">
              <div v-for="i in 3" :key="i" class="similar-skeleton"></div>
            </div>
            <div v-else-if="similarHouses.length > 0" class="similar-houses-grid">
              <div
                v-for="similarHouse in similarHouses"
                :key="similarHouse.id"
                class="similar-house-card"
                @click="$router.push(`/houses/${similarHouse.id}`)"
              >
                <div class="similar-house-image">
                  <img :src="getHouseImage(similarHouse.images)" :alt="similarHouse.title" />
                  <div class="price">¥{{ similarHouse.price }}/月</div>
                </div>
                <div class="similar-house-info">
                  <h4>{{ similarHouse.title }}</h4>
                  <p>📍 {{ similarHouse.address }}</p>
                  <div class="tags">
                    <span class="tag-area">{{ similarHouse.area }}㎡</span>
                    <span class="tag-type">{{ getTypeName(similarHouse.typeId) }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">
              <span>🏠</span>
              <p>暂无相似房源</p>
            </div>
          </div>
        </div>

        <!-- 右侧边栏 -->
        <div class="sidebar">
          <!-- 联系房东卡片 -->
          <div class="contact-card">
            <div class="landlord-info">
              <div class="landlord-avatar">{{ (house.landlordName || '房').charAt(0) }}</div>
              <div class="landlord-details">
                <h3>{{ house.landlordName || '房东' }}</h3>
                <div class="landlord-credit" v-if="house.landlordCreditScore !== null">
                  <span class="credit-label">信用分</span>
                  <span class="credit-score" :class="getCreditScoreClass(house.landlordCreditScore)">
                    {{ house.landlordCreditScore }}
                  </span>
                </div>
              </div>
            </div>

            <div class="contact-actions">
              <button
                class="action-btn"
                :class="{ 'favorited': isFavorited }"
                @click="toggleFavorite"
                :disabled="favoriteLoading"
              >
                <span>{{ isFavorited ? '❤️' : '🤍' }}</span>
                <span>{{ isFavorited ? '已收藏' : '收藏' }}</span>
              </button>

              <button
                v-if="house.landlordPhone"
                class="action-btn primary"
                @click="showContactDialog = true"
              >
                <span>📞</span>
                <span>显示电话</span>
              </button>

              <button class="action-btn" @click="showNavigationDialog">
                <span>🧭</span>
                <span>导航</span>
              </button>

              <button
                v-if="isLoggedIn && !isOwnedByCurrentUser"
                class="action-btn"
                @click="startChat"
              >
                <span>💬</span>
                <span>联系房东</span>
              </button>

              <button
                v-if="house.status === 1"
                class="action-btn success"
                @click="showAppointmentDialog"
                :disabled="isOwnedByCurrentUser"
              >
                <span>📅</span>
                <span>预约看房</span>
              </button>

              <button
                v-if="house.status === 1 && isLoggedIn && isTenant"
                class="action-btn primary large"
                @click="handleRent"
                :disabled="isOwnedByCurrentUser"
              >
                <span>🔑</span>
                <span>立即租赁</span>
              </button>

              <button class="action-btn danger" @click="showReportDialog">
                <span>⚠️</span>
                <span>举报房源</span>
              </button>
            </div>

            <div class="safety-tips">
              <div class="tips-title">🛡️ 租房安全提示</div>
              <ul>
                <li>看房前请通过平台联系房东</li>
                <li>切勿支付可疑定金或押金</li>
                <li>签订合同前请仔细核对信息</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 联系电话对话框 -->
    <el-dialog v-model="showContactDialog" title="联系方式" width="320px" center>
      <div class="contact-dialog-content">
        <div class="dialog-alert">
          <span>⚠️</span>
          <span>请通过平台联系房东，谨防诈骗</span>
        </div>
        <div class="phone-number" v-if="house">
          <span>📞</span>
          <span>{{ house.landlordPhone }}</span>
        </div>
      </div>
      <template #footer>
        <button class="btn-primary" @click="showContactDialog = false">确定</button>
      </template>
    </el-dialog>

    <!-- 创建订单对话框 -->
    <el-dialog v-model="rentDialogVisible" title="确认租赁信息" width="480px">
      <div class="rent-dialog-content">
        <div class="dialog-alert info">
          <span>ℹ️</span>
          <span>签订租赁合同前，建议您实地看房并核实房屋信息</span>
        </div>
        <div class="rent-info">
          <div class="rent-row">
            <span class="rent-label">房屋</span>
            <span class="rent-value">{{ house.title }}</span>
          </div>
          <div class="rent-row">
            <span class="rent-label">月租金</span>
            <span class="rent-value price">¥{{ house.price }}</span>
          </div>
          <div class="rent-row">
            <span class="rent-label">押付方式</span>
            <span class="rent-value">{{ getPaymentMethodName(house.paymentMethod) }}</span>
          </div>
          <div class="rent-row">
            <span class="rent-label">押金</span>
            <span class="rent-value price">¥{{ depositAmount }}</span>
          </div>
          <div class="rent-row" v-if="house.serviceFee">
            <span class="rent-label">服务费</span>
            <span class="rent-value price">¥{{ serviceFeeAmount }}</span>
          </div>
          <div class="rent-row total">
            <span class="rent-label">总金额</span>
            <span class="rent-value total-price">¥{{ totalAmount }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <button class="btn-secondary" @click="rentDialogVisible = false">取消</button>
        <button class="btn-primary" @click="createOrder" :loading="creating">提交订单</button>
      </template>
    </el-dialog>

    <!-- 预约看房对话框 -->
    <el-dialog v-model="showAppointment" title="预约看房" width="480px">
      <el-form :model="appointmentForm" :rules="appointmentRules" ref="appointmentFormRef" label-width="100px">
        <el-form-item label="预约日期" prop="appointmentDate">
          <el-date-picker
            v-model="appointmentForm.appointmentDate"
            type="date"
            placeholder="选择预约日期"
            :disabledDate="disabledDate"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="预约时间" prop="appointmentTime">
          <el-select v-model="appointmentForm.appointmentTime" placeholder="请选择时间段" style="width: 100%">
            <el-option label="上午 9:00-11:00" value="上午09:00-11:00" />
            <el-option label="上午 10:00-12:00" value="上午10:00-12:00" />
            <el-option label="下午 14:00-16:00" value="下午14:00-16:00" />
            <el-option label="下午 15:00-17:00" value="下午15:00-17:00" />
            <el-option label="下午 16:00-18:00" value="下午16:00-18:00" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="appointmentForm.contactPhone" placeholder="请输入联系电话" maxlength="11" />
        </el-form-item>
        <el-form-item label="留言" prop="message">
          <el-input
            v-model="appointmentForm.message"
            type="textarea"
            :rows="4"
            placeholder="请输入留言信息（选填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="btn-secondary" @click="showAppointment = false">取消</button>
        <button class="btn-primary" @click="submitAppointment" :loading="submittingAppointment">提交预约</button>
      </template>
    </el-dialog>

    <!-- 举报房源对话框 -->
    <el-dialog v-model="reportDialogVisible" title="举报房源" width="480px">
      <div class="dialog-alert warning">
        <span>⚠️</span>
        <span>请如实举报违规房源，恶意举报将会影响您的账号信用</span>
      </div>
      <el-form :model="reportForm" :rules="reportRules" ref="reportFormRef" label-width="100px">
        <el-form-item label="举报类型" prop="type">
          <el-select v-model="reportForm.type" placeholder="请选择举报类型" style="width: 100%">
            <el-option label="房源信息虚假" value="房源信息虚假" />
            <el-option label="价格欺诈" value="价格欺诈" />
            <el-option label="图片与实际不符" value="图片与实际不符" />
            <el-option label="联系方式无效" value="联系方式无效" />
            <el-option label="已租出未下架" value="已租出未下架" />
            <el-option label="涉嫌诈骗" value="涉嫌诈骗" />
            <el-option label="其他违规" value="其他违规" />
          </el-select>
        </el-form-item>
        <el-form-item label="举报说明" prop="description">
          <el-input
            v-model="reportForm.description"
            type="textarea"
            :rows="5"
            placeholder="请详细描述您发现的问题（选填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="btn-secondary" @click="reportDialogVisible = false">取消</button>
        <button class="btn-danger" @click="submitReport" :loading="submittingReport">提交举报</button>
      </template>
    </el-dialog>

    <!-- 导航对话框 -->
    <el-dialog v-model="navigationDialogVisible" title="路线导航" width="700px" :close-on-click-modal="false">
      <div class="navigation-dialog-content">
        <div class="route-info" v-if="routeInfo.distance">
          <div class="route-item">
            <span class="route-icon">📍</span>
            <div class="route-details">
              <span class="route-label">距离</span>
              <span class="route-value">{{ routeInfo.distance }}</span>
            </div>
          </div>
          <div class="route-item">
            <span class="route-icon">⏱️</span>
            <div class="route-details">
              <span class="route-label">预计用时</span>
              <span class="route-value">{{ routeInfo.duration }}</span>
            </div>
          </div>
        </div>
        <div class="start-point-input">
          <label>出发地点</label>
          <el-input
            v-model="startPoint"
            placeholder="输入出发地点或使用当前位置"
            clearable
          >
            <template #append>
              <el-button @click="getCurrentLocation" :loading="gettingLocation">
                <span v-if="!gettingLocation">📍</span>
                <span v-else>...</span>
                {{ gettingLocation ? '定位中' : '当前位置' }}
              </el-button>
            </template>
          </el-input>
        </div>
        <div class="navigation-map">
          <div ref="navigationMapContainer" class="nav-map-container"></div>
        </div>
      </div>
      <template #footer>
        <button class="btn-secondary" @click="navigationDialogVisible = false">关闭</button>
        <button class="btn-primary" @click="openExternalNavigation">
          <span>🚀</span>
          打开高德地图导航
        </button>
      </template>
    </el-dialog>

    <!-- 图片预览（隐藏的el-image用于触发预览） -->
    <el-image
      v-if="imagePreviewVisible"
      style="display: none"
      :src="houseImages[imagePreviewIndex]"
      :preview-src-list="houseImages"
      :initial-index="imagePreviewIndex"
      :preview-teleported="true"
      @load="imagePreviewVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AMapLoader from '@amap/amap-jsapi-loader'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { checkFavorited, addFavorite, removeFavorite } from '@/api/favorite'
import { getSimilarHouses } from '@/api/recommendation'
import AMapContainer from '@/components/AMapContainer.vue'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const houseId = route.params.id

// 用户登录状态和角色判断
const isLoggedIn = computed(() => userStore.isLoggedIn)
const isTenant = computed(() => userStore.userInfo?.roleCode === 'TENANT')
const isLandlord = computed(() => userStore.userInfo?.roleCode === 'LANDLORD')
const isOwnedByCurrentUser = computed(() => {
  return house.value && userStore.userInfo && house.value.landlordId === userStore.userInfo.id
})

// 基础API路径
const baseAPI = import.meta.env.VITE_BASE_API || '/api'

// 房屋数据
const house = ref(null)
const loading = ref(true)
const showContactDialog = ref(false)

// 租赁相关
const rentDialogVisible = ref(false)
const creating = ref(false)

// 预约相关
const showAppointment = ref(false)
const submittingAppointment = ref(false)
const appointmentFormRef = ref(null)
const appointmentForm = reactive({
  houseId: houseId,
  appointmentDate: null,
  appointmentTime: '',
  contactPhone: '',
  message: ''
})
const appointmentRules = {
  appointmentDate: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  appointmentTime: [{ required: true, message: '请选择预约时间', trigger: 'change' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// 举报相关
const reportDialogVisible = ref(false)
const submittingReport = ref(false)
const reportFormRef = ref(null)
const reportForm = reactive({
  houseId: houseId,
  type: '',
  description: ''
})
const reportRules = {
  type: [{ required: true, message: '请选择举报类型', trigger: 'change' }]
}

// 收藏相关
const isFavorited = ref(false)
const favoriteLoading = ref(false)

// 留言相关
const commentForm = reactive({ content: '' })
const submittingComment = ref(false)

// 评价和评论
const reviews = ref([])
const comments = ref([])

// 相似房源
const similarHouses = ref([])
const similarHousesLoading = ref(false)

// 地图中心
const mapCenter = computed(() => {
  if (house.value && house.value.longitude && house.value.latitude) {
    return { lng: house.value.longitude, lat: house.value.latitude }
  }
  return { lng: 113.40, lat: 23.05 }
})

// 计算押金、租金等
const depositMonths = computed(() => {
  if (!house.value) return 0
  const depositMap = { 0: 1, 1: 1, 2: 3, 3: 1, 4: 1, 5: 1, 6: 0 }
  return depositMap[house.value.paymentMethod] || 1
})

const rentMonths = computed(() => {
  if (!house.value) return 1
  const rentMap = { 0: 1, 1: 3, 2: 1, 3: 6, 4: 6, 5: 12, 6: 1 }
  return rentMap[house.value.paymentMethod] || 1
})

const depositAmount = computed(() => {
  if (!house.value) return '0.00'
  return (parseFloat(house.value.price) * depositMonths.value).toFixed(2)
})

const rentAmount = computed(() => {
  if (!house.value) return '0.00'
  return (parseFloat(house.value.price) * rentMonths.value).toFixed(2)
})

const serviceFeeAmount = computed(() => {
  if (!house.value || !house.value.serviceFee) return '0.00'
  return (parseFloat(house.value.serviceFee) * rentMonths.value).toFixed(2)
})

const totalAmount = computed(() => {
  const total = parseFloat(depositAmount.value) + parseFloat(rentAmount.value) + parseFloat(serviceFeeAmount.value)
  return total.toFixed(2)
})

// 房屋图片
const houseImages = computed(() => {
  if (!house.value || !house.value.images) return []
  try {
    const images = JSON.parse(house.value.images)
    return images.map(img => formatImageUrl(img)).filter(img => img)
  } catch {
    return []
  }
})

// 房屋设施
const houseFacilities = computed(() => {
  if (!house.value || !house.value.facilities) return {}
  try {
    return JSON.parse(house.value.facilities)
  } catch {
    return {}
  }
})

// 房屋标签
const houseTags = computed(() => {
  if (!house.value || !house.value.tags) return []
  try {
    const tags = JSON.parse(house.value.tags)
    return Array.isArray(tags) ? tags : []
  } catch {
    return []
  }
})

// 格式化图片URL
const formatImageUrl = (path) => {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return baseAPI + path
}

// 获取房屋图片
const getHouseImage = (images) => {
  if (!images) return '/default-house.jpg'
  try {
    const imageArray = JSON.parse(images)
    if (imageArray.length === 0) return '/default-house.jpg'
    return formatImageUrl(imageArray[0])
  } catch {
    return '/default-house.jpg'
  }
}

// 各种名称转换方法
const getDecorationName = (decoration) => {
  const names = { 0: '毛坯', 1: '简装', 2: '精装', 3: '豪装' }
  return names[decoration] || ''
}

const getOrientationName = (orientation) => {
  const names = { 0: '东', 1: '南', 2: '西', 3: '北', 8: '南北通透' }
  return names[orientation] || ''
}

const getBuildingTypeName = (type) => {
  const names = { 0: '普通住宅', 1: '公寓', 2: '别墅', 3: 'loft', 4: '复式' }
  return names[type] || ''
}

const getPaymentMethodName = (method) => {
  const names = { 0: '押一付一', 1: '押一付三', 2: '押三付一', 3: '押一付六', 4: '半年付', 5: '年付', 6: '无押金' }
  return names[method] || ''
}

const getTypeName = (typeId) => {
  const names = { 1: '整租', 2: '合租' }
  return names[typeId] || '未知'
}

const getFacilityLabel = (key) => {
  const labels = {
    wifi: 'WiFi',
    elevator: '电梯',
    airConditioner: '空调',
    washer: '洗衣机',
    refrigerator: '冰箱',
    television: '电视',
    waterHeater: '热水器'
  }
  return labels[key] || key
}

const getCreditScoreClass = (score) => {
  if (score >= 80) return 'excellent'
  if (score >= 60) return 'good'
  return 'normal'
}

const getCreditLevel = (score) => {
  if (score >= 80) return '优秀'
  if (score >= 60) return '良好'
  return '一般'
}

// 获取房源详情
const fetchHouseDetail = async () => {
  loading.value = true
  try {
    await request.get(`/houses/${houseId}`, {}, {
      onSuccess: (res) => {
        house.value = res
      }
    })
  } catch (error) {
    console.error('获取房源详情失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取评价列表
const fetchReviews = async () => {
  try {
    await request.get(`/house-comments/house/${houseId}`, {}, {
      onSuccess: (res) => {
        reviews.value = Array.isArray(res) ? res : (res?.records || [])
      }
    })
  } catch (error) {
    console.error('获取评价失败:', error)
  }
}

// 获取留言列表
const fetchComments = async () => {
  try {
    await request.get(`/house-comments/house/${houseId}`, {}, {
      onSuccess: (res) => {
        comments.value = Array.isArray(res) ? res.filter(c => c.type === 0) : (res?.records || []).filter(c => c.type === 0)
      }
    })
  } catch (error) {
    console.error('获取留言失败:', error)
  }
}

// 获取相似房源
const fetchSimilarHouses = async () => {
  similarHousesLoading.value = true
  try {
    const res = await getSimilarHouses(houseId, { limit: 3 })
    similarHouses.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (error) {
    console.error('获取相似房源失败:', error)
  } finally {
    similarHousesLoading.value = false
  }
}

// 检查收藏状态
const checkFavoriteStatus = async () => {
  if (!isLoggedIn.value) return
  try {
    const res = await checkFavorited(houseId)
    isFavorited.value = typeof res === 'boolean' ? res : (res?.data || false)
  } catch {
    isFavorited.value = false
  }
}

// 切换收藏
const toggleFavorite = async () => {
  if (!isLoggedIn.value) {
    ElMessageBox.confirm('请先登录后再收藏房源', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'info'
    }).then(() => router.push('/login')).catch(() => {})
    return
  }

  favoriteLoading.value = true
  try {
    if (isFavorited.value) {
      await removeFavorite(houseId)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(houseId)
      isFavorited.value = true
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    ElMessage.error('操作失败，请重试')
  } finally {
    favoriteLoading.value = false
  }
}

// 显示预约对话框
const showAppointmentDialog = () => {
  if (!isLoggedIn.value) {
    ElMessageBox.confirm('请先登录后再预约看房', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'info'
    }).then(() => router.push('/login')).catch(() => {})
    return
  }
  showAppointment.value = true
}

// 提交预约
const submitAppointment = () => {
  appointmentFormRef.value.validate(async (valid) => {
    if (!valid) return
    submittingAppointment.value = true
    try {
      await request.post('/appointments', appointmentForm, {
        onSuccess: () => {
          ElMessage.success('预约提交成功，请等待房东确认')
          showAppointment.value = false
          appointmentFormRef.value.resetFields()
        }
      })
    } catch (error) {
      ElMessage.error('预约提交失败')
    } finally {
      submittingAppointment.value = false
    }
  })
}

// 禁用过去的日期
const disabledDate = (time) => {
  return time.getTime() < Date.now() - 24 * 60 * 60 * 1000
}

// 显示举报对话框
const showReportDialog = () => {
  if (!isLoggedIn.value) {
    ElMessageBox.confirm('请先登录后再举报房源', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'info'
    }).then(() => router.push('/login')).catch(() => {})
    return
  }
  reportDialogVisible.value = true
}

// 提交举报
const submitReport = () => {
  reportFormRef.value.validate(async (valid) => {
    if (!valid) return
    submittingReport.value = true
    try {
      await request.post('/violations', reportForm, {
        onSuccess: () => {
          ElMessage.success('举报提交成功，我们会尽快处理')
          reportDialogVisible.value = false
          reportFormRef.value.resetFields()
        }
      })
    } catch (error) {
      ElMessage.error('举报提交失败')
    } finally {
      submittingReport.value = false
    }
  })
}

// 提交留言
const submitComment = async () => {
  if (!commentForm.content.trim()) {
    ElMessage.warning('请输入留言内容')
    return
  }
  submittingComment.value = true
  try {
    await request.post('/house-comments', {
      houseId: houseId,
      content: commentForm.content,
      type: 0
    }, {
      onSuccess: () => {
        ElMessage.success('留言发表成功')
        commentForm.content = ''
        fetchComments()
      }
    })
  } catch (error) {
    ElMessage.error('留言发表失败')
  } finally {
    submittingComment.value = false
  }
}

// 开始聊天
const startChat = () => {
  // 跳转到消息页面，传递房东ID、房源ID和房东名字
  router.push({
    path: '/messages',
    query: {
      userId: house.value?.landlordId,
      userName: house.value?.landlordName,
      houseId: houseId
    }
  })
}

// 处理租赁
const handleRent = () => {
  rentDialogVisible.value = true
}

// 创建订单
const createOrder = async () => {
  creating.value = true
  try {
    await request.post('/orders', {
      houseId: houseId,
      depositAmount: depositAmount.value,
      rentAmount: rentAmount.value,
      serviceFeeAmount: serviceFeeAmount.value,
      totalAmount: totalAmount.value,
      paymentMethod: house.value.paymentMethod
    }, {
      onSuccess: (res) => {
        console.log('Order created successfully:', res)
        ElMessage.success('订单创建成功，请前往支付')
        rentDialogVisible.value = false
        if (res && res.id) {
          router.push(`/orders/${res.id}`)
        } else {
          ElMessage.error('订单创建成功但获取订单ID失败')
        }
      }
    })
  } catch (error) {
    ElMessage.error('订单创建失败')
  } finally {
    creating.value = false
  }
}

// 返回
const goBack = () => {
  router.push('/houses')
}

// 图片预览
const imagePreviewVisible = ref(false)
const imagePreviewIndex = ref(0)

// 导航相关
const navigationDialogVisible = ref(false)
const navigationMapContainer = ref(null)
const startPoint = ref('')
const gettingLocation = ref(false)
const routeInfo = reactive({
  distance: '',
  duration: ''
})
let navigationMap = null
let driving = null
let marker = null
let startMarker = null

const openImagePreview = (index) => {
  imagePreviewIndex.value = index
  imagePreviewVisible.value = true
}

// 显示导航对话框
const showNavigationDialog = () => {
  navigationDialogVisible.value = true
  nextTick(() => {
    initNavigationMap()
  })
}

// 初始化导航地图
const initNavigationMap = () => {
  if (!house.value?.latitude || !house.value?.longitude) {
    ElMessage.warning('该房源暂无位置信息')
    return
  }

  if (!navigationMapContainer.value) return

  AMapLoader.load({
    key: '3a3471f3d38b1f56d507b51ae00473b1',
    version: '2.0',
    plugins: ['AMap.Driving', 'AMap.Geolocation', 'AMap.Marker', 'AMap.Geocoder']
  }).then((AMap) => {
    // 创建地图实例
    navigationMap = new AMap.Map(navigationMapContainer.value, {
      zoom: 14,
      center: [house.value.longitude, house.value.latitude],
      mapStyle: 'amap://styles/whites'
    })

    // 添加房源标记
    marker = new AMap.Marker({
      position: [house.value.longitude, house.value.latitude],
      icon: new AMap.Icon({
        image: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png',
        size: new AMap.Size(25, 34),
        imageSize: new AMap.Size(25, 34)
      }),
      title: house.value.title
    })
    navigationMap.add(marker)

    // 初始化驾车路线规划
    driving = new AMap.Driving({
      map: navigationMap,
      panel: null,
      hideMarkers: true,
      showTraffic: false
    })

    // 如果用户之前获取过位置，自动规划路线
    if (userLocation.value.lng && userLocation.value.lat) {
      planRoute(userLocation.value.lng, userLocation.value.lat)
    }
  }).catch((e) => {
    console.error('导航地图加载失败:', e)
    ElMessage.error('地图加载失败')
  })
}

// 用户位置缓存
const userLocation = ref({ lng: null, lat: null })

// 获取当前位置
const getCurrentLocation = () => {
  gettingLocation.value = true
  AMapLoader.load({
    key: '3a3471f3d38b1f56d507b51ae00473b1',
    version: '2.0',
    plugins: ['AMap.Geolocation']
  }).then((AMap) => {
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000
    })

    geolocation.getCurrentPosition((status, result) => {
      gettingLocation.value = false
      if (status === 'complete') {
        const lng = result.position.lng
        const lat = result.position.lat
        userLocation.value = { lng, lat }

        // 反地理编码获取地址
        const geocoder = new AMap.Geocoder()
        geocoder.getAddress([lng, lat], (status, data) => {
          if (status === 'complete') {
            startPoint.value = data.reformattedAddress
          } else {
            startPoint.value = `当前位置 (${lng.toFixed(6)}, ${lat.toFixed(6)})`
          }
        })

        // 规划路线
        planRoute(lng, lat)
      } else {
        ElMessage.error('获取位置失败，请检查定位权限')
      }
    })
  }).catch(() => {
    gettingLocation.value = false
    ElMessage.error('定位服务加载失败')
  })
}

// 规划路线
const planRoute = (startLng, startLat) => {
  if (!house.value?.longitude || !house.value?.longitude) return
  if (!driving) return

  driving.search(
    new AMap.LngLat(startLng, startLat),
    new AMap.LngLat(house.value.longitude, house.value.latitude),
    (status, result) => {
      if (status === 'complete') {
        const route = result.routes[0]
        const distance = (route.distance / 1000).toFixed(1)
        const time = Math.round(route.time / 60)
        routeInfo.distance = `${distance}公里`
        routeInfo.duration = time >= 60
          ? `${Math.floor(time / 60)}小时${time % 60}分钟`
          : `${time}分钟`

        // 调整视野显示完整路线
        if (navigationMap) {
          navigationMap.setFitView()
        }
      } else {
        ElMessage.warning('路线规划失败，请稍后重试')
      }
    },
    { policy: 'LEAST_TIME' }
  )
}

// 打开外部导航
const openExternalNavigation = () => {
  if (!house.value?.latitude || !house.value?.longitude) {
    ElMessage.warning('该房源暂无位置信息')
    return
  }

  const destName = encodeURIComponent(house.value.title || '房源')
  const destLng = house.value.longitude
  const destLat = house.value.latitude

  // 构建高德地图导航URL
  let url = `https://uri.amap.com/navigation?to=${destLng},${destLat},${destName}&mode=car&src=广寓租房`

  if (startPoint.value) {
    url += `&from=${encodeURIComponent(startPoint.value)}`
  }

  window.open(url, '_blank')
}

// 监听导航对话框关闭，清理资源
watch(navigationDialogVisible, (newVal) => {
  if (!newVal && navigationMap) {
    navigationMap.destroy()
    navigationMap = null
    driving = null
    marker = null
    startMarker = null
    routeInfo.distance = ''
    routeInfo.duration = ''
  }
})

// 初始化
onMounted(async () => {
  await fetchHouseDetail()
  if (house.value) {
    await Promise.all([
      checkFavoriteStatus(),
      fetchReviews(),
      fetchComments(),
      fetchSimilarHouses()
    ])
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/design-system.scss';

.house-detail-container {
  min-height: calc(100vh - 72px);
  background: var(--color-bg-primary);
}

// ==================== 加载状态 ====================
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 80px 24px;

  .skeleton-wrapper {
    width: 100%;
    max-width: 900px;

    .skeleton-carousel {
      height: 400px;
      background: linear-gradient(90deg, var(--color-bg-secondary) 25%, var(--color-bg-tertiary) 50%, var(--color-bg-secondary) 75%);
      background-size: 200% 100%;
      animation: shimmer 1.5s infinite;
      border-radius: var(--radius-xl);
      margin-bottom: 24px;
    }

    .skeleton-info {
      .skeleton-line {
        height: 20px;
        background: linear-gradient(90deg, var(--color-bg-secondary) 25%, var(--color-bg-tertiary) 50%, var(--color-bg-secondary) 75%);
        background-size: 200% 100%;
        animation: shimmer 1.5s infinite;
        border-radius: var(--radius-sm);
        margin-bottom: 12px;

        &:nth-child(1) { width: 70%; height: 32px; }
        &:nth-child(2) { width: 50%; }
        &:nth-child(3) { width: 90%; }
        &:nth-child(4) { width: 60%; }
      }
    }
  }
}

// ==================== 未找到 ====================
.not-found {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 80px 24px;

  .not-found-content {
    text-align: center;

    .not-found-icon {
      font-size: 80px;
      display: block;
      margin-bottom: 24px;
      opacity: 0.5;
    }

    h3 {
      font-size: var(--font-2xl);
      color: var(--color-text-primary);
      margin-bottom: 8px;
    }

    p {
      color: var(--color-text-tertiary);
      margin-bottom: 24px;
    }
  }
}

// ==================== 详情头部 ====================
.detail-header {
  background: white;
  padding: 32px 24px;
  border-bottom: 1px solid var(--color-border-light);

  .header-content {
    max-width: 1200px;
    margin: 0 auto;

    .header-badges {
      display: flex;
      gap: 8px;
      margin-bottom: 16px;

      .badge {
        padding: 6px 14px;
        border-radius: var(--radius-full);
        font-size: 13px;
        font-weight: 500;

        &.badge-success {
          background: rgba(78, 205, 196, 0.15);
          color: var(--color-secondary-dark);
        }

        &.badge-warning {
          background: rgba(255, 230, 109, 0.2);
          color: #D4A000;
        }

        &.badge-info {
          background: var(--color-bg-tertiary);
          color: var(--color-text-tertiary);
        }

        &.badge-type {
          background: rgba(255, 107, 107, 0.1);
          color: var(--color-primary);
        }
      }
    }

    .house-title {
      font-family: $font-display;
      font-size: var(--font-4xl);
      font-weight: 700;
      color: var(--color-text-primary);
      margin-bottom: 12px;
      letter-spacing: -0.5px;
    }

    .house-address {
      display: flex;
      align-items: center;
      gap: 6px;
      color: var(--color-text-secondary);
      font-size: var(--font-md);
      margin-bottom: 20px;

      .community-name {
        color: var(--color-text-tertiary);
      }
    }

    .header-price {
      display: flex;
      align-items: baseline;
      gap: 4px;

      .price-amount {
        font-family: $font-display;
        font-size: var(--font-5xl);
        font-weight: 800;
        background: var(--color-primary-gradient);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }

      .price-unit {
        font-size: var(--font-lg);
        color: var(--color-text-tertiary);
      }
    }
  }
}

// ==================== 主内容 ====================
.detail-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 24px;
}

.main-content {
  .house-carousel-card {
    background: var(--color-bg-secondary);
    border-radius: var(--radius-xl);
    overflow: hidden;
    border: 1px solid var(--color-border-light);
    margin-bottom: 24px;

    :deep(.el-carousel__container) {
      height: 400px;
    }

    .carousel-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
      cursor: pointer;
      transition: transform var(--transition-base);

      &:hover {
        transform: scale(1.02);
      }
    }

    .no-images {
      height: 400px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: var(--color-bg-tertiary);
      font-size: 48px;
      color: var(--color-text-tertiary);
    }
  }

  .info-card {
    background: var(--color-bg-secondary);
    border-radius: var(--radius-xl);
    padding: 24px;
    border: 1px solid var(--color-border-light);
    margin-bottom: 24px;

    .card-title {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 20px;
      padding-bottom: 16px;
      border-bottom: 1px solid var(--color-border-light);

      .title-icon {
        font-size: 24px;
      }

      h3 {
        font-family: $font-display;
        font-size: var(--font-xl);
        font-weight: 600;
        color: var(--color-text-primary);
        margin: 0;
      }

      .review-summary, .view-more {
        margin-left: auto;
        display: flex;
        align-items: center;
        gap: 8px;

        .avg-score {
          font-size: var(--font-2xl);
          font-weight: 700;
          color: #FFB800;
        }
      }

      .view-more {
        font-size: var(--font-sm);
        color: var(--color-primary);
        text-decoration: none;
        font-weight: 500;

        &:hover {
          text-decoration: underline;
        }
      }
    }

    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
      gap: 16px;

      .info-item {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 12px;
        background: var(--color-bg-primary);
        border-radius: var(--radius-lg);

        .info-icon {
          font-size: 20px;
        }

        .info-content {
          display: flex;
          flex-direction: column;

          .info-label {
            font-size: var(--font-xs);
            color: var(--color-text-tertiary);
          }

          .info-value {
            font-size: var(--font-sm);
            font-weight: 500;
            color: var(--color-text-primary);
          }
        }
      }
    }

    .facilities-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
      gap: 12px;

      .facility-item {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 10px 14px;
        background: var(--color-bg-primary);
        border-radius: var(--radius-md);
        border: 1px solid var(--color-border);
        font-size: var(--font-sm);
        color: var(--color-text-tertiary);

        &.available {
          border-color: var(--color-secondary);
          background: rgba(78, 205, 196, 0.08);
          color: var(--color-secondary-dark);
        }

        .facility-icon {
          font-size: 16px;
        }
      }
    }

    .description-content {
      font-size: var(--font-md);
      line-height: 1.8;
      color: var(--color-text-secondary);
      white-space: pre-wrap;
    }

    .tags-list {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;

      .tag-item {
        padding: 8px 16px;
        background: rgba(255, 107, 107, 0.1);
        color: var(--color-primary);
        border-radius: var(--radius-full);
        font-size: var(--font-sm);
        font-weight: 500;
      }
    }

    // 评价列表
    .reviews-list {
      .review-item {
        padding: 20px;
        background: var(--color-bg-primary);
        border-radius: var(--radius-lg);
        margin-bottom: 16px;

        &:last-child {
          margin-bottom: 0;
        }

        .review-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          margin-bottom: 12px;

          .user-info {
            display: flex;
            align-items: center;
            gap: 12px;

            .user-avatar {
              width: 40px;
              height: 40px;
              display: flex;
              align-items: center;
              justify-content: center;
              background: var(--color-primary-gradient);
              color: white;
              border-radius: 50%;
              font-weight: 600;
            }

            .user-details {
              display: flex;
              flex-direction: column;

              .username {
                font-weight: 500;
                color: var(--color-text-primary);
              }

              .user-tag {
                font-size: var(--font-xs);
                color: var(--color-secondary);
              }
            }
          }

          .review-meta {
            text-align: right;

            .review-score {
              font-size: var(--font-sm);
              color: #FFB800;
            }

            .review-time {
              font-size: var(--font-xs);
              color: var(--color-text-tertiary);
              margin-top: 4px;
              display: block;
            }
          }
        }

        .review-content {
          font-size: var(--font-md);
          line-height: 1.6;
          color: var(--color-text-secondary);
        }

        .landlord-reply {
          margin-top: 12px;
          padding: 12px;
          background: rgba(78, 205, 196, 0.08);
          border-radius: var(--radius-md);

          .reply-label {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: var(--font-sm);
            color: var(--color-secondary);
            margin-bottom: 6px;
            font-weight: 500;
          }

          .reply-content {
            font-size: var(--font-sm);
            color: var(--color-text-secondary);
          }
        }
      }
    }

    // 留言表单
    .comment-form {
      .form-alert {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 12px 16px;
        background: rgba(255, 107, 107, 0.08);
        border-radius: var(--radius-md);
        margin-bottom: 16px;
        font-size: var(--font-sm);
        color: var(--color-primary);
      }

      .el-form {
        :deep(.el-textarea__inner) {
          border-radius: var(--radius-md);
          border: 1px solid var(--color-border);
        }

        .btn-primary {
          margin-top: 12px;
        }
      }
    }

    .login-prompt {
      text-align: center;
      padding: 32px;

      span {
        font-size: 40px;
        display: block;
        margin-bottom: 12px;
      }

      p {
        color: var(--color-text-tertiary);
        margin-bottom: 16px;
      }

      .btn-link {
        color: var(--color-primary);
        font-weight: 500;
        text-decoration: none;
      }
    }

    .comments-list {
      .comment-item {
        padding: 16px;
        background: var(--color-bg-primary);
        border-radius: var(--radius-lg);
        margin-bottom: 12px;

        .comment-header {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-bottom: 8px;

          .user-avatar {
            width: 32px;
            height: 32px;
            display: flex;
            align-items: center;
            justify-content: center;
            background: var(--color-bg-tertiary);
            border-radius: 50%;
            font-size: var(--font-sm);
            font-weight: 600;
          }

          .user-details {
            display: flex;
            flex-direction: column;

            .username {
              font-size: var(--font-sm);
              font-weight: 500;
            }

            .comment-time {
              font-size: var(--font-xs);
              color: var(--color-text-tertiary);
            }
          }
        }

        .comment-content {
          font-size: var(--font-sm);
          color: var(--color-text-secondary);
          line-height: 1.5;
        }
      }
    }

    // 相似房源
    .loading-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 16px;

      .similar-skeleton {
        aspect-ratio: 4/3;
        background: linear-gradient(90deg, var(--color-bg-secondary) 25%, var(--color-bg-tertiary) 50%, var(--color-bg-secondary) 75%);
        background-size: 200% 100%;
        animation: shimmer 1.5s infinite;
        border-radius: var(--radius-lg);
      }
    }

    .similar-houses-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 16px;

      .similar-house-card {
        background: var(--color-bg-primary);
        border-radius: var(--radius-lg);
        overflow: hidden;
        border: 1px solid var(--color-border);
        cursor: pointer;
        transition: all var(--transition-base);

        &:hover {
          border-color: var(--color-primary);
          transform: translateY(-2px);
        }

        .similar-house-image {
          position: relative;
          height: 120px;

          img {
            width: 100%;
            height: 100%;
            object-fit: cover;
          }

          .price {
            position: absolute;
            bottom: 8px;
            right: 8px;
            background: var(--color-primary-gradient);
            color: white;
            padding: 4px 10px;
            border-radius: var(--radius-full);
            font-size: var(--font-xs);
            font-weight: 600;
          }
        }

        .similar-house-info {
          padding: 12px;

          h4 {
            font-size: var(--font-sm);
            font-weight: 600;
            color: var(--color-text-primary);
            margin: 0 0 6px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          p {
            font-size: var(--font-xs);
            color: var(--color-text-tertiary);
            margin: 0 0 8px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .tags {
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
  }
}

// ==================== 侧边栏 ====================
.sidebar {
  .contact-card {
    background: var(--color-bg-secondary);
    border-radius: var(--radius-xl);
    padding: 24px;
    border: 1px solid var(--color-border-light);
    position: sticky;
    top: 24px;

    .landlord-info {
      display: flex;
      align-items: center;
      gap: 16px;
      padding-bottom: 20px;
      border-bottom: 1px solid var(--color-border-light);
      margin-bottom: 20px;

      .landlord-avatar {
        width: 64px;
        height: 64px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--color-secondary-gradient);
        color: white;
        border-radius: var(--radius-xl);
        font-size: 24px;
        font-weight: 700;
      }

      .landlord-details {
        h3 {
          font-size: var(--font-lg);
          font-weight: 600;
          color: var(--color-text-primary);
          margin: 0 0 8px;
        }

        .landlord-credit {
          display: flex;
          align-items: center;
          gap: 8px;

          .credit-label {
            font-size: var(--font-xs);
            color: var(--color-text-tertiary);
          }

          .credit-score {
            font-weight: 700;
            font-size: var(--font-md);

            &.excellent { color: var(--color-secondary); }
            &.good { color: #FFB800; }
            &.normal { color: var(--color-text-secondary); }
          }
        }
      }
    }

    .contact-actions {
      display: flex;
      flex-direction: column;
      gap: 10px;
      margin-bottom: 20px;

      .action-btn {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        padding: 12px 20px;
        border-radius: var(--radius-lg);
        font-size: var(--font-md);
        font-weight: 500;
        cursor: pointer;
        transition: all var(--transition-base);
        border: none;
        background: var(--color-bg-primary);
        color: var(--color-text-secondary);
        border: 1px solid var(--color-border);

        &:hover:not(:disabled) {
          border-color: var(--color-primary);
          color: var(--color-primary);
        }

        &:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }

        &.primary {
          background: var(--color-primary-gradient);
          color: white;
          border-color: transparent;

          &:hover:not(:disabled) {
            box-shadow: var(--shadow-colored);
            transform: translateY(-1px);
          }
        }

        &.success {
          background: var(--color-secondary-gradient);
          color: white;
          border-color: transparent;

          &:hover:not(:disabled) {
            box-shadow: 0 4px 12px rgba(78, 205, 196, 0.4);
            transform: translateY(-1px);
          }
        }

        &.danger {
          border-color: rgba(255, 107, 107, 0.3);
          color: var(--color-primary);

          &:hover:not(:disabled) {
            background: rgba(255, 107, 107, 0.1);
          }
        }

        &.large {
          padding: 16px 20px;
          font-size: var(--font-lg);
          font-weight: 600;
        }

        &.favorited {
          background: rgba(255, 107, 107, 0.1);
          border-color: var(--color-primary);
          color: var(--color-primary);
        }
      }
    }

    .safety-tips {
      padding: 16px;
      background: rgba(255, 230, 109, 0.1);
      border-radius: var(--radius-lg);
      border: 1px solid rgba(255, 230, 109, 0.3);

      .tips-title {
        font-size: var(--font-sm);
        font-weight: 600;
        color: var(--color-text-primary);
        margin-bottom: 12px;
      }

      ul {
        margin: 0;
        padding-left: 20px;
        list-style: none;

        li {
          position: relative;
          font-size: var(--font-sm);
          color: var(--color-text-secondary);
          margin-bottom: 8px;
          padding-left: 12px;

          &:last-child {
            margin-bottom: 0;
          }

          &::before {
            content: '•';
            position: absolute;
            left: 0;
            color: #D4A000;
          }
        }
      }
    }
  }
}

// ==================== 对话框 ====================
.contact-dialog-content {
  text-align: center;

  .dialog-alert {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px 16px;
    background: rgba(255, 230, 109, 0.1);
    border-radius: var(--radius-md);
    margin-bottom: 20px;
    font-size: var(--font-sm);
    color: #D4A000;
  }

  .phone-number {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    font-size: var(--font-2xl);
    font-weight: 700;
    color: var(--color-primary);
  }
}

.rent-dialog-content {
  .dialog-alert {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: rgba(78, 205, 196, 0.1);
    border-radius: var(--radius-md);
    margin-bottom: 20px;
    font-size: var(--font-sm);
    color: var(--color-secondary);
  }

  .rent-info {
    .rent-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid var(--color-border-light);

      &.total {
        border-bottom: none;
        border-top: 2px solid var(--color-border);
        margin-top: 8px;
        padding-top: 16px;
      }

      .rent-label {
        font-size: var(--font-md);
        color: var(--color-text-secondary);
      }

      .rent-value {
        font-size: var(--font-md);
        font-weight: 500;
        color: var(--color-text-primary);

        &.price, &.total-price {
          font-weight: 700;
        }

        &.total-price {
          font-size: var(--font-xl);
          color: var(--color-primary);
        }
      }
    }
  }
}

// 导航对话框样式
.navigation-dialog-content {
  .route-info {
    display: flex;
    gap: 24px;
    margin-bottom: 16px;
    padding: 16px;
    background: var(--color-bg-primary);
    border-radius: var(--radius-lg);

    .route-item {
      display: flex;
      align-items: center;
      gap: 10px;

      .route-icon {
        font-size: 24px;
      }

      .route-details {
        display: flex;
        flex-direction: column;

        .route-label {
          font-size: var(--font-xs);
          color: var(--color-text-tertiary);
        }

        .route-value {
          font-size: var(--font-lg);
          font-weight: 600;
          color: var(--color-primary);
        }
      }
    }
  }

  .start-point-input {
    margin-bottom: 16px;

    label {
      display: block;
      font-size: var(--font-sm);
      font-weight: 500;
      color: var(--color-text-primary);
      margin-bottom: 8px;
    }
  }

  .navigation-map {
    .nav-map-container {
      width: 100%;
      height: 350px;
      border-radius: var(--radius-lg);
      overflow: hidden;
      background: var(--color-bg-tertiary);
    }
  }
}

// ==================== 响应式 ====================
@media (max-width: 1024px) {
  .detail-main {
    grid-template-columns: 1fr;
  }

  .sidebar {
    .contact-card {
      position: static;
    }
  }
}

@media (max-width: 768px) {
  .detail-header {
    .header-content {
      .house-title {
        font-size: var(--font-2xl);
      }

      .header-price .price-amount {
        font-size: var(--font-3xl);
      }
    }
  }

  .detail-main {
    padding: 16px;
  }

  .main-content {
    .info-card {
      .info-grid {
        grid-template-columns: repeat(2, 1fr);
      }

      .facilities-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }
  }
}
</style>
