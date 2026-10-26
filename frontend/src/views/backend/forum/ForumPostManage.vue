<template>
  <div class="forum-post-manage-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>帖子管理</h2>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索帖子标题或内容..."
          clearable
          style="width: 300px; margin-right: 10px;"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button icon="Search" @click="handleSearch" />
          </template>
        </el-input>

        <el-select
          v-model="searchForm.status"
          placeholder="帖子状态"
          clearable
          style="width: 150px; margin-right: 10px;"
          @change="handleSearch"
        >
          <el-option label="全部" value="" />
          <el-option label="正常" :value="1" />
          <el-option label="已删除" :value="2" />
        </el-select>

        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <!-- 帖子表格 -->
      <el-table :data="posts" stripe style="width: 100%; margin-top: 20px" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="作者" width="120">
          <template #default="scope">
            {{ scope.row.user?.name || scope.row.user?.username || '未知' }}
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="scope">
            {{ scope.row.category?.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="统计" width="150">
          <template #default="scope">
            <div>浏览: {{ scope.row.viewCount }}</div>
            <div>回复: {{ scope.row.replyCount }}</div>
          </template>
        </el-table-column>
        <el-table-column label="标记" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.isTop === 1" type="danger" size="small">置顶</el-tag>
            <el-tag v-if="scope.row.isEssence === 1" type="warning" size="small">精华</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '正常' : '已删除' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button type="primary" link size="small" @click="viewPost(scope.row)">
              查看
            </el-button>
            <el-button
              :type="scope.row.isTop === 1 ? 'warning' : 'success'"
              link
              size="small"
              @click="toggleTop(scope.row)"
            >
              {{ scope.row.isTop === 1 ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button
              :type="scope.row.isEssence === 1 ? 'warning' : 'success'"
              link
              size="small"
              @click="toggleEssence(scope.row)"
            >
              {{ scope.row.isEssence === 1 ? '取消精华' : '精华' }}
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              @click="deletePost(scope.row)"
              v-if="scope.row.status === 1"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-if="total > 0"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50]"
        @current-change="fetchPosts"
        @size-change="fetchPosts"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>

    <!-- 查看帖子详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="帖子详情"
      width="70%"
      top="5vh"
    >
      <div v-if="currentPost">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="帖子ID">{{ currentPost.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentPost.status === 1 ? 'success' : 'danger'">
              {{ currentPost.status === 1 ? '正常' : '已删除' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ currentPost.title }}</el-descriptions-item>
          <el-descriptions-item label="作者">
            {{ currentPost.user?.name || currentPost.user?.username }}
          </el-descriptions-item>
          <el-descriptions-item label="分类">
            {{ currentPost.category?.name || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="浏览数">{{ currentPost.viewCount }}</el-descriptions-item>
          <el-descriptions-item label="回复数">{{ currentPost.replyCount }}</el-descriptions-item>
          <el-descriptions-item label="发布时间" :span="2">
            {{ currentPost.createTime }}
          </el-descriptions-item>
          <el-descriptions-item label="内容" :span="2">
            <div class="post-content">{{ currentPost.content }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 回复管理 -->
        <div class="replies-section" v-if="currentPost.replyCount > 0">
          <h4 style="margin: 20px 0 10px;">回复列表 ({{ postReplies.length }})</h4>
          <el-table :data="postReplies" stripe size="small" max-height="300">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column label="回复者" width="100">
              <template #default="scope">
                {{ scope.row.username || '匿名' }}
              </template>
            </el-table-column>
            <el-table-column prop="content" label="内容" show-overflow-tooltip />
            <el-table-column label="回复对象" width="100">
              <template #default="scope">
                <span v-if="scope.row.replyToUsername">@{{ scope.row.replyToUsername }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="150" />
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="scope">
                <el-button type="danger" link size="small" @click="deleteReply(scope.row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const posts = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  keyword: '',
  status: ''
})

const detailVisible = ref(false)
const currentPost = ref(null)
const postReplies = ref([])

// 获取帖子列表
const fetchPosts = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value,
      keyword: searchForm.keyword || undefined,
      status: searchForm.status ? searchForm.status : undefined
    }

    const res = await request.get('/forum-posts', params)

    posts.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取帖子列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchPosts()
}

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.status = ''
  handleSearch()
}

// 查看帖子
const viewPost = async (post) => {
  currentPost.value = post
  detailVisible.value = true
  // 获取帖子的回复列表
  await fetchPostReplies(post.id)
}

// 获取帖子的回复列表
const fetchPostReplies = async (postId) => {
  try {
    const res = await request.get(`/forum-replies/post/${postId}`, {
      currentPage: 1,
      size: 100
    })
    postReplies.value = res?.records || res || []
  } catch (error) {
    console.error('获取回复列表失败:', error)
    postReplies.value = []
  }
}

// 删除回复（级联删除子回复）
const deleteReply = async (reply) => {
  // 计算子回复数量
  const childCount = postReplies.value.filter(r => r.parentId === reply.id).length
  const cascadeWarning = childCount > 0
    ? `\n\n该回复有 ${childCount} 条子回复，删除后将一并删除！`
    : ''

  try {
    await ElMessageBox.confirm(
      `确定要删除该回复吗？删除后无法恢复！${cascadeWarning}`,
      '警告',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      }
    )

    await request.delete(`/forum-replies/${reply.id}`)
    ElMessage.success('删除成功')
    // 刷新回复列表和帖子列表（更新回复数）
    await fetchPostReplies(currentPost.value.id)
    fetchPosts()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除回复失败:', error)
    }
  }
}

// 切换置顶
const toggleTop = async (post) => {
  const newStatus = post.isTop === 1 ? 0 : 1
  const action = newStatus === 1 ? '置顶' : '取消置顶'

  try {
    await ElMessageBox.confirm(`确定要${action}该帖子吗?`, '提示', {
      type: 'warning'
    })

    await request.put(`/forum-posts/${post.id}/top`, null, {
      params: { isTop: newStatus }
    })

    ElMessage.success(`${action}成功`)
    fetchPosts()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${action}失败:`, error)
    }
  }
}

// 切换精华
const toggleEssence = async (post) => {
  const newStatus = post.isEssence === 1 ? 0 : 1
  const action = newStatus === 1 ? '设为精华' : '取消精华'

  try {
    await ElMessageBox.confirm(`确定要${action}该帖子吗?`, '提示', {
      type: 'warning'
    })

    await request.put(`/forum-posts/${post.id}/essence`, null, {
      params: { isEssence: newStatus }
    })

    ElMessage.success(`${action}成功`)
    fetchPosts()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${action}失败:`, error)
    }
  }
}

// 删除帖子
const deletePost = async (post) => {
  try {
    const replyInfo = post.replyCount > 0 ? `\n\n该帖子有 ${post.replyCount} 条回复，删除帖子将同时删除所有回复！` : ''
    await ElMessageBox.confirm(`确定要删除该帖子吗？删除后将无法恢复！${replyInfo}`, '警告', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      dangerouslyUseHTMLString: true
    })

    await request.delete(`/forum-posts/${post.id}`)
    ElMessage.success('删除成功')
    fetchPosts()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

onMounted(() => {
  fetchPosts()
})
</script>

<style lang="scss" scoped>
.forum-post-manage-container {
  padding: 20px;
}

.card-header {
  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #1d4ed8;
  }
}

.search-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.post-content {
  white-space: pre-line;
  line-height: 1.6;
  max-height: 400px;
  overflow-y: auto;
}

// 响应式
@media (max-width: 768px) {
  .search-bar {
    .el-input,
    .el-select {
      width: 100% !important;
    }
  }
}
</style>
