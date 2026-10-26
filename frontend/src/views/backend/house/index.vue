<template>
  <div class="house-container">
    <div class="page-header">
      <h2>房屋管理</h2>
      <div class="header-buttons">
        <el-button type="success" @click="exportToExcel" :icon="Download">导出Excel</el-button>
        <el-button type="primary" @click="handleAddClick" :icon="Plus">添加房屋</el-button>
      </div>
    </div>

    <!-- 搜索区域 -->
    <div class="search-area">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="房屋ID">
          <el-input v-model="searchForm.id" placeholder="请输入房屋ID" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="房屋标题">
          <el-input v-model="searchForm.title" placeholder="请输入房屋标题" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="价格范围">
          <el-input-number v-model="searchForm.minPrice" placeholder="最低价" :min="0" :precision="2" :step="100" />
          <span class="range-separator">-</span>
          <el-input-number v-model="searchForm.maxPrice" placeholder="最高价" :min="0" :precision="2" :step="100" />
        </el-form-item>
        <el-form-item label="房屋类型">
          <el-select v-model="searchForm.typeId" placeholder="请选择类型" clearable>
            <el-option v-for="item in houseTypeOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房屋状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option :value="0" label="下架" />
            <el-option :value="1" label="待出租" />
            <el-option :value="2" label="已出租" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
          <el-button @click="resetSearch" :icon="Refresh">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格区域 -->
    <div class="table-area">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{ backgroundColor: '#EFF6FF', color: '#1D4ED8' }"
      >
        <el-table-column type="index" label="序号" width="60" align="center" fixed />
        <el-table-column prop="title" label="房屋标题" min-width="180" fixed />
        <el-table-column prop="area" label="面积(㎡)" width="100" align="center" />
        <el-table-column prop="price" label="价格(元/月)" width="120" align="center" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="typeName" label="房屋类型" width="100" align="center" />
        <el-table-column label="房屋图片" width="100" align="center">
          <template #default="scope">
            <el-image
              v-if="getFirstImage(scope.row.images)"
              :src="formatImageUrl(getFirstImage(scope.row.images))"
              :preview-src-list="formatImageList(scope.row.images)"
              style="width: 50px; height: 50px"
                    :preview-teleported="true"
              fit="cover"
            />
            <span v-else class="no-image">无图片</span>
          </template>
        </el-table-column>
        <el-table-column prop="landlordName" label="房东" width="100" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="info">下架</el-tag>
            <el-tag v-else-if="scope.row.status === 1" type="success">待出租</el-tag>
            <el-tag v-else-if="scope.row.status === 2" type="warning">已出租</el-tag>
            <el-tag v-else type="info">未知</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180" align="center">
          <template #default="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              text
              @click="handleEdit(scope.row)"
              :icon="Edit"
            >
              编辑
            </el-button>
            <el-button
              v-if="scope.row.status !== 0"
              type="info"
              size="small"
              text
              @click="handleChangeStatus(scope.row, 0)"
              :icon="Bottom"
            >
              下架
            </el-button>
            <el-button
              v-if="scope.row.status === 0"
              type="success"
              size="small"
              text
              @click="handleChangeStatus(scope.row, 1)"
              :icon="Top"
            >
              上架
            </el-button>
            <el-button
              v-if="scope.row.status !== 2"
              type="danger"
              size="small"
              text
              @click="handleDelete(scope.row)"
              :icon="Delete"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-container">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          @update:current-page="val => currentPage = val"
          @update:page-size="val => pageSize = val"
          background
        />
      </div>
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '添加房屋' : '编辑房屋'"
      width="900px"
    >
      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-width="100px"
        status-icon
      >
        <el-form-item label="房屋标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入房屋标题" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="面积(㎡)" prop="area">
              <el-input-number v-model="form.area" :min="0" :precision="2" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="价格(元/月)" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" :step="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="房屋类型" prop="typeId">
              <el-select v-model="form.typeId" placeholder="请选择房屋类型" style="width: 100%">
                <el-option v-for="item in houseTypeOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="小区名称">
              <el-input v-model="form.communityName" placeholder="请输入小区名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="房屋地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入房屋地址" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="装修情况" prop="decoration">
              <el-select v-model="form.decoration" placeholder="请选择装修情况" style="width: 100%">
                <el-option label="毛坯" :value="0" />
                <el-option label="简装" :value="1" />
                <el-option label="精装" :value="2" />
                <el-option label="豪装" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="楼层" prop="floor">
              <el-input-number v-model="form.floor" :min="1" :precision="0" placeholder="所在楼层" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总楼层" prop="totalFloors">
              <el-input-number v-model="form.totalFloors" :min="1" :precision="0" placeholder="总楼层数" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="朝向" prop="orientation">
              <el-select v-model="form.orientation" placeholder="请选择朝向" style="width: 100%">
                <el-option label="东" :value="0" />
                <el-option label="南" :value="1" />
                <el-option label="西" :value="2" />
                <el-option label="北" :value="3" />
                <el-option label="东南" :value="4" />
                <el-option label="西南" :value="5" />
                <el-option label="东北" :value="6" />
                <el-option label="西北" :value="7" />
                <el-option label="南北通透" :value="8" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="建筑年代">
              <el-input-number v-model="form.buildYear" :min="1980" :max="2030" :precision="0" placeholder="如：2010" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="房屋类型" prop="buildingType">
              <el-select v-model="form.buildingType" placeholder="请选择" style="width: 100%">
                <el-option label="普通住宅" :value="0" />
                <el-option label="公寓" :value="1" />
                <el-option label="别墅" :value="2" />
                <el-option label="loft" :value="3" />
                <el-option label="复式" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="押付方式" prop="paymentMethod">
              <el-select v-model="form.paymentMethod" placeholder="请选择押付方式" style="width: 100%" @change="handlePaymentMethodChange">
                <el-option label="押一付一" :value="0" />
                <el-option label="押一付三" :value="1" />
                <el-option label="押三付一" :value="2" />
                <el-option label="押一付六" :value="3" />
                <el-option label="半年付" :value="4" />
                <el-option label="年付" :value="5" />
                <el-option label="无押金" :value="6" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最短租期">
              <el-input-number v-model="form.minLeaseTerm" :min="1" :max="60" :precision="0" placeholder="月数" style="width: 100%" />
              <span style="margin-left: 5px; color: #999">个月（根据押付方式自动填充，可修改）</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="服务费">
              <el-input-number v-model="form.serviceFee" :min="0" :precision="0" placeholder="服务费/月" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="房屋描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入房屋描述"
            rows="4"
          />
        </el-form-item>

        <el-form-item label="配套设施" prop="facilities">
          <el-checkbox-group v-model="facilitiesSelected">
            <el-checkbox label="wifi">WiFi</el-checkbox>
            <el-checkbox label="elevator">电梯</el-checkbox>
            <el-checkbox label="airConditioner">空调</el-checkbox>
            <el-checkbox label="washer">洗衣机</el-checkbox>
            <el-checkbox label="refrigerator">冰箱</el-checkbox>
            <el-checkbox label="television">电视</el-checkbox>
            <el-checkbox label="waterHeater">热水器</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="房屋图片" prop="images" required>
          <el-upload
            action="#"
            list-type="picture-card"
            :http-request="uploadImage"
            :on-preview="handlePreview"
            :on-remove="handleRemove"
            :before-upload="beforeUpload"
            :file-list="imageFileList"
            multiple
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <el-dialog v-model="previewVisible" title="图片预览">
            <img w-full :src="previewUrl" alt="Preview Image" />
          </el-dialog>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Edit, Delete, Search, Refresh, Top, Bottom, Download } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import DateUtils from '@/utils/dateUtils'
import * as XLSX from 'xlsx'
import { useUserStore } from '@/store/user'

const router = useRouter()
// 基础API路径
const baseAPI = import.meta.env.VITE_BASE_API || '/api'

// 表格数据
const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const userStore = useUserStore()
// 房屋类型选项
const houseTypeOptions = ref([])

// 搜索表单
const searchForm = reactive({
  id: '',
  title: '',
  minPrice: null,
  maxPrice: null,
  typeId: null,
  status: null
})

// 添加/编辑表单
const dialogVisible = ref(false)
const dialogType = ref('add') // add 或 edit
const submitLoading = ref(false)
const form = reactive({
  id: null,
  title: '',
  description: '',
  area: 0,
  price: 0,
  address: '',
  typeId: null,
  images: '',
  facilities: '',
  decoration: 1,
  floor: 1,
  totalFloors: 1,
  orientation: null,
  buildYear: null,
  buildingType: 0,
  paymentMethod: 0,
  minLeaseTerm: 1,
  serviceFee: 0,
  communityName: '',
  contactPhone: ''
})

// 图片预览
const previewVisible = ref(false)
const previewUrl = ref('')
const imageFileList = ref([])

// 配套设施选择
const facilitiesSelected = ref([])

const formRef = ref(null)
const rules = {
  title: [
    { required: true, message: '请输入房屋标题', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  area: [
    { required: true, message: '请输入面积', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' }
  ],
  address: [
    { required: true, message: '请输入房屋地址', trigger: 'blur' },
    { min: 5, max: 200, message: '长度在 5 到 200 个字符', trigger: 'blur' }
  ],
  typeId: [
    { required: true, message: '请选择房屋类型', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请输入房屋描述', trigger: 'blur' }
  ],
  decoration: [
    { required: true, message: '请选择装修情况', trigger: 'change' }
  ],
  floor: [
    { required: true, message: '请输入所在楼层', trigger: 'blur' }
  ],
  totalFloors: [
    { required: true, message: '请输入总楼层', trigger: 'blur' }
  ],
  orientation: [
    { required: true, message: '请选择朝向', trigger: 'change' }
  ],
  buildingType: [
    { required: true, message: '请选择房屋类型', trigger: 'change' }
  ],
  paymentMethod: [
    { required: true, message: '请选择押付方式', trigger: 'change' }
  ],
  images: [
    {
      required: true,
      validator: (rule, value, callback) => {
        if (!imageFileList.value || imageFileList.value.length === 0) {
          callback(new Error('请至少上传一张房屋照片'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  facilities: [
    {
      required: true,
      validator: (rule, value, callback) => {
        if (!facilitiesSelected.value || facilitiesSelected.value.length === 0) {
          callback(new Error('请至少选择一项配套设施'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

// 生命周期钩子
onMounted(() => {
  // 获取房屋类型列表
  fetchHouseTypeList()

  // 获取房屋列表
  fetchData()
})

// 获取房屋类型列表
const fetchHouseTypeList = async () => {
  try {

    await request.get('/house-types', {}, {
      onSuccess: (res) => {
        houseTypeOptions.value = res
      }
    })
  } catch (error) {
    console.error('获取房屋类型列表失败:', error)
  }
}


// 获取表格数据
const fetchData = async () => {
  loading.value = true
  try {
    await request.get('/houses/page', {
      id: searchForm.id || undefined,
      title: searchForm.title,
      minPrice: searchForm.minPrice,
      maxPrice: searchForm.maxPrice,
      typeId: searchForm.typeId,
      landLordId: userStore.isLandlord ? userStore.userInfo.id : null,
      status: searchForm.status,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      onSuccess: (res) => {
        tableData.value = res.records||[]
        total.value = res.total||0
      }
    })
  } catch (error) {
    console.error('获取房屋列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchData()
}

// 重置搜索
const resetSearch = () => {
  searchForm.id = ''
  searchForm.title = ''
  searchForm.minPrice = null
  searchForm.maxPrice = null
  searchForm.typeId = null
  searchForm.status = null
  currentPage.value = 1
  fetchData()
}

// 每页数量变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchData()
}

// 当前页变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchData()
}

// 获取第一张图片
const getFirstImage = (images) => {
  if (!images) return null
  try {
    const imageArray = JSON.parse(images)
    return imageArray.length > 0 ? imageArray[0] : null
  } catch (e) {
    return null
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

// 添加按钮点击
const handleAddClick = async () => {
  // 如果是房东，先检查实名认证状态
  if (userStore.isLandlord) {
    try {
      const res = await request.get('/user/verify/status', {}, { showDefaultMsg: false })
      const verifyStatus = res?.verifyStatus

      if (verifyStatus === 0) {
        ElMessageBox.confirm(
          '您还未完成实名认证，无法发布房源。是否前往进行实名认证？',
          '需要实名认证',
          {
            confirmButtonText: '去认证',
            cancelButtonText: '取消',
            type: 'warning'
          }
        ).then(() => {
          // 跳转到个人信息页面进行认证
          router.push('/backend/person-info')
        }).catch(() => {})
        return
      } else if (verifyStatus === 1) {
        ElMessage.warning('您的实名认证正在审核中，请耐心等待审核通过后再发布房源')
        return
      } else if (verifyStatus === 3) {
        ElMessageBox.confirm(
          '您的实名认证未通过，请重新提交认证申请。是否前往重新认证？',
          '认证未通过',
          {
            confirmButtonText: '去认证',
            cancelButtonText: '取消',
            type: 'error'
          }
        ).then(() => {
          router.push('/backend/person-info')
        }).catch(() => {})
        return
      }
    } catch (error) {
      console.error('检查认证状态失败:', error)
    }
  }

  dialogType.value = 'add'
  resetForm()
  dialogVisible.value = true
}

// 押付方式改变时自动填充最短租期
const handlePaymentMethodChange = (value) => {
  // 根据押付方式设置最短租期（月）
  const minLeaseMap = {
    0: 1,  // 押一付一 -> 至少1个月
    1: 3,  // 押一付三 -> 至少3个月
    2: 1,  // 押三付一 -> 至少1个月
    3: 6,  // 押一付六 -> 至少6个月
    4: 6,  // 半年付 -> 至少6个月
    5: 12, // 年付 -> 至少12个月
    6: 1   // 无押金 -> 至少1个月
  }
  form.minLeaseTerm = minLeaseMap[value] || 1
}

// 编辑按钮点击
const handleEdit = (row) => {
  dialogType.value = 'edit'
  resetForm()

  // 复制数据到表单
  Object.keys(form).forEach(key => {
    if (key in row) {
      form[key] = row[key]
    }
  })

  // 处理图片列表
  if (row.images) {
    try {
      const imageArray = JSON.parse(row.images)
      imageFileList.value = imageArray.map((url, index) => ({
        name: `图片${index + 1}`,
        url: formatImageUrl(url)
      }))
    } catch (e) {
      imageFileList.value = []
    }
  }

  // 处理配套设施
  if (row.facilities) {
    try {
      const facilitiesObj = JSON.parse(row.facilities)
      facilitiesSelected.value = Object.keys(facilitiesObj).filter(key => facilitiesObj[key])
    } catch (e) {
      facilitiesSelected.value = []
    }
  }

  dialogVisible.value = true
}

// 修改房屋状态
const handleChangeStatus = (row, status) => {
  const statusText = status === 0 ? '下架' : '上架'
  ElMessageBox.confirm(`确认将房屋「${row.title}」${statusText}吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.put(`/houses/${row.id}/status`, null, {
        params: { status },
        successMsg: `${statusText}成功`,
        errorMsg: `${statusText}失败`,
        onSuccess: () => {
          fetchData()
        }
      })
    } catch (error) {
      console.error(`${statusText}房屋失败:`, error)
    }
  }).catch(() => {
    // 取消操作，不做任何处理
  })
}

// 删除房屋
const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除房屋「${row.title}」吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/houses/${row.id}`, {
        successMsg: '删除成功',
        errorMsg: '删除失败',
        onSuccess: () => {
          fetchData()
        }
      })
    } catch (error) {
      console.error('删除房屋失败:', error)
    }
  }).catch(() => {
    // 取消操作，不做任何处理
  })
}

// 上传图片前检查
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  
  return true
}

// 自定义上传
const uploadImage = async (options) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)

  try {
    await request.post('/file/upload/img', formData, {
      transformRequest: [(data) => data],
      successMsg: '图片上传成功',
      onSuccess: (data) => {
        // 添加到图片列表
        imageFileList.value.push({
          name: file.name,
          url: formatImageUrl(data)
        })
        // 触发表单验证，清除可能的错误提示
        if (formRef.value) {
          formRef.value.validateField('images')
        }
        options.onSuccess(data)
      }
    })
  } catch (error) {
    console.error('图片上传失败:', error)
    options.onError(error)
  }
}

// 图片预览
const handlePreview = (file) => {
  previewUrl.value = file.url
  previewVisible.value = true
}

// 移除图片
const handleRemove = (file, fileList) => {
  imageFileList.value = fileList
  // 触发表单验证，检查是否还有图片
  if (formRef.value) {
    formRef.value.validateField('images')
  }
}

// 重置表单
const resetForm = () => {
  form.id = null
  form.title = ''
  form.description = ''
  form.area = 0
  form.price = 0
  form.address = ''
  form.typeId = null
  form.images = ''
  form.facilities = ''
  form.decoration = 1
  form.floor = 1
  form.totalFloors = 1
  form.orientation = null
  form.buildYear = null
  form.buildingType = 0
  form.paymentMethod = 0
  form.minLeaseTerm = 1
  form.serviceFee = 0
  form.communityName = ''
  form.contactPhone = ''

  imageFileList.value = []
  facilitiesSelected.value = []

  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 提交表单
const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      // 额外检查：确保至少有一张图片
      if (!imageFileList.value || imageFileList.value.length === 0) {
        ElMessage.error('请至少上传一张房屋照片')
        return
      }

      submitLoading.value = true
      try {
        // 处理图片信息
        const images = imageFileList.value.map(file => {
          // 从完整URL提取路径部分
          const url = file.url
          if (url.startsWith(baseAPI)) {
            return url.replace(baseAPI, '')
          }
          return url
        })

        // 再次检查最终图片数量
        if (images.length === 0) {
          ElMessage.error('请至少上传一张房屋照片')
          submitLoading.value = false
          return
        }

        // 处理配套设施
        const facilities = {}
        const allFacilities = ['wifi', 'elevator', 'airConditioner', 'washer', 'refrigerator', 'television', 'waterHeater']
        allFacilities.forEach(facility => {
          facilities[facility] = facilitiesSelected.value.includes(facility)
        })

        // 准备提交的数据
        const submitData = {
          title: form.title,
          description: form.description,
          area: form.area,
          price: form.price,
          address: form.address,
          typeId: form.typeId,
          images: JSON.stringify(images),
          facilities: JSON.stringify(facilities),
          decoration: form.decoration,
          floor: form.floor,
          totalFloors: form.totalFloors,
          orientation: form.orientation,
          buildYear: form.buildYear,
          buildingType: form.buildingType,
          paymentMethod: form.paymentMethod,
          minLeaseTerm: form.minLeaseTerm,
          serviceFee: form.serviceFee,
          communityName: form.communityName,
          contactPhone: form.contactPhone
        }

        if (dialogType.value === 'add') {
          // 添加
          await request.post('/houses', submitData, {
            successMsg: '添加成功',
            errorMsg: '添加失败',
            onSuccess: () => {
              dialogVisible.value = false
              fetchData()
            }
          })
        } else {
          // 编辑
          await request.put(`/houses/${form.id}`, submitData, {
            successMsg: '更新成功',
            errorMsg: '更新失败',
            onSuccess: () => {
              dialogVisible.value = false
              fetchData()
            }
          })
        }
      } catch (error) {
        console.error(`${dialogType.value === 'add' ? '添加' : '更新'}房屋失败:`, error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 将 DateUtils 暴露给模板
const formatDateTime = (date) => {
  return DateUtils.formatDateTime(date);
}

// 导出Excel
const exportToExcel = async () => {
  const exporting = ref(false)
  exporting.value = true

  try {
    // 根据用户角色选择不同的导出接口
    const url = userStore.isLandlord
      ? '/api/houses/landlord/export/excel'
      : '/api/houses/admin/export/excel'

    // 构建查询参数 - 包含所有筛选条件
    const params = {
      title: searchForm.title || '',
      status: searchForm.status !== null && searchForm.status !== '' ? searchForm.status : undefined,
      minPrice: searchForm.minPrice || undefined,
      maxPrice: searchForm.maxPrice || undefined,
      typeId: searchForm.typeId || undefined
    }

    // 管理员还可以按审核状态筛选
    if (!userStore.isLandlord && searchForm.auditStatus !== null && searchForm.auditStatus !== '') {
      params.auditStatus = searchForm.auditStatus
    }

    const token = localStorage.getItem('token')

    // 构建查询字符串
    const queryString = Object.keys(params)
      .filter(key => params[key] !== undefined)
      .map(key => `${key}=${encodeURIComponent(params[key])}`)
      .join('&')

    const fullUrl = `${url}?${queryString}`

    // 使用fetch下载文件（通过代理）
    const response = await fetch(fullUrl, {
      method: 'GET',
      headers: {
        'token': token
      }
    })

    if (!response.ok) {
      throw new Error('导出失败')
    }

    // 获取文件blob
    const blob = await response.blob()

    // 创建下载链接
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl

    // 从响应头获取文件名，如果没有则使用默认名称
    const contentDisposition = response.headers.get('Content-Disposition')
    let fileName = `房屋列表_${new Date().getTime()}.xlsx`
    if (contentDisposition) {
      const fileNameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (fileNameMatch && fileNameMatch[1]) {
        fileName = decodeURIComponent(fileNameMatch[1].replace(/['"]/g, ''))
      }
    }

    link.download = fileName
    document.body.appendChild(link)
    link.click()

    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)

    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}
</script>

<style lang="scss" scoped>
.house-container {
  padding: 20px;
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h2 {
      font-size: 24px;
      color: #1D4ED8;
      margin: 0;
    }

    .header-buttons {
      display: flex;
      gap: 10px;
    }
  }
  
  .search-area {
    background: #FFFFFF;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(59, 130, 246, 0.08);
    margin-bottom: 20px;
    
    .range-separator {
      margin: 0 10px;
    }
  }
  
  .table-area {
    background: #FFFFFF;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(59, 130, 246, 0.08);
    
    .no-image {
      color: #909399;
      font-size: 12px;
    }
  }
  
  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}

:deep(.el-upload--picture-card) {
  background-color: #f0f9ff;
  border-color: #bfdbfe;
  &:hover {
    border-color: #3b82f6;
  }
}
</style> 