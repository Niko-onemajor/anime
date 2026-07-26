<template>
  <div class="admin-container">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="navbar-container">
        <div class="logo">Niko动漫 - 管理员</div>
        <ul class="nav-links">
          <li><a href="/admin/users">用户管理</a></li>
          <li><a href="/admin/animes">动漫管理</a></li>
          <li><a href="/admin/forum" class="active">论坛管理</a></li>
          <li><a href="/admin/deleted">删除记录管理</a></li>
          <li><a href="/index">返回前台</a></li>
        </ul>
      </div>
    </nav>

    <!-- 内容区域 -->
    <div class="container">
      <h2>论坛管理</h2>
      
      <!-- 搜索和排序 -->
      <div class="actions">
        <div class="search-box">
          <input type="text" v-model="searchKeyword" placeholder="搜索帖子标题..." class="search-input">
          <button @click="searchPosts" class="search-btn">搜索</button>
        </div>
        <div class="sort-box">
          <label for="sort">排序方式：</label>
          <select id="sort" v-model="sortBy" class="sort-select" @change="sortPosts">
            <option value="time">按时间</option>
            <option value="comments">按评论数</option>
            <option value="likes">按点赞数</option>
            <option value="dislikes">按点踩数</option>
          </select>
          <button @click="toggleSortDirection" class="sort-direction-btn">
            切换
          </button>
        </div>
      </div>

      <!-- 帖子列表 -->
      <div class="post-list">
        <div v-for="post in pagedPosts" :key="post.id" class="post-item">
          <div class="post-header">
            <img :src="getImageUrl(post.authorAvatar)" :alt="post.authorName" class="author-avatar">
            <span class="post-author">作者：{{ post.authorName }}</span>
          </div>
          <div class="post-title-container">
            <h3 class="post-title">{{ post.title }}</h3>
          </div>
          <div class="post-content">{{ post.content }}</div>
          <div class="post-stats">
            <span class="stat-item">点赞：{{ post.likeCount }}</span>
            <span class="stat-item">点踩：{{ post.dislikeCount }}</span>
            <span class="stat-item">评论：{{ post.commentCount }}</span>
            <span class="stat-item">发布时间：{{ formatTime(post.createTime) }}</span>
          </div>
          <div class="post-actions">
            <button @click="editPost(post)" class="edit-btn">编辑</button>
            <button @click="showDeleteConfirm(post.id)" class="delete-btn">删除</button>
            <button @click="toggleComments(post.id)" class="comments-btn">
              {{ showComments[post.id] ? '隐藏评论' : '管理评论' }}
            </button>
          </div>
          
          <!-- 评论管理 -->
          <div v-if="showComments[post.id]" class="comments-section" :key="post.lastUpdated">
            <div class="comments-header">
              <div class="comment-sort">
                <span class="sort-label">排序方式：</span>
                <span 
                  v-for="sort in commentSortOptions" 
                  :key="sort.value" 
                  class="sort-option small" 
                  :class="{ active: commentSortBy[post.id] === sort.value }"
                  @click="selectCommentSort(post.id, sort.value)"
                >
                  {{ sort.label }}
                </span>
                <button @click="toggleCommentSort(post.id)" class="sort-order-btn small">
                  切换
                </button>
              </div>
            </div>
            <div v-for="comment in post.comments" :key="comment.id + '_' + comment.lastUpdated" class="comment-item">
              <div class="comment-header">
                <div class="comment-author-info">
                  <img :src="getImageUrl(comment.authorAvatar) || '/src/avatars/avatar1.jpg'" :alt="comment.authorName" class="author-avatar small">
                  <span class="comment-author">{{ comment.authorName }}</span>
                </div>
              </div>
              <div class="comment-content">{{ comment.content }}</div>
              <div class="comment-stats">
                <span class="stat-item">点赞：{{ comment.likeCount || 0 }}</span>
                <span class="stat-item">点踩：{{ comment.dislikeCount || 0 }}</span>
                <span class="stat-item">发布时间：{{ formatTime(comment.createTime) }}</span>
              </div>
              <div class="comment-actions">
                <button @click="editComment(post.id, comment)" class="edit-btn small">编辑</button>
                <button @click="showCommentDeleteConfirm(post.id, comment.id)" class="delete-btn small">删除</button>
              </div>

            </div>
          </div>
        </div>
      </div>

      <!-- 分页导航 -->
      <div class="pagination" v-if="postTotalPages > 1">
        <button @click="postPage = 1" :disabled="postPage === 1" class="page-btn">首页</button>
        <button @click="postPage = postPage - 1" :disabled="postPage === 1" class="page-btn">上一页</button>
        <span 
          v-for="p in postPageNumbers" 
          :key="p" 
          class="page-num" 
          :class="{ active: p === postPage }"
          @click="postPage = p"
        >{{ p }}</span>
        <button @click="postPage = postPage + 1" :disabled="postPage === postTotalPages" class="page-btn">下一页</button>
        <button @click="postPage = postTotalPages" :disabled="postPage === postTotalPages" class="page-btn">尾页</button>
        <span class="page-total">共 {{ postTotalPages }} 页 / {{ posts.length }} 条</span>
      </div>

      <!-- 编辑帖子对话框 -->
      <div class="dialog-overlay" v-if="showEditPostDialog">
        <div class="dialog-content">
          <div class="dialog-header">
            <h3>编辑帖子</h3>
            <button @click="closePostDialog" class="close-btn">×</button>
          </div>
          <div class="dialog-body">
            <div class="form-group">
              <label for="post-title">标题</label>
              <input type="text" id="post-title" v-model="postForm.title" placeholder="请输入帖子标题" class="form-input">
            </div>
            <div class="form-group">
              <label for="post-content">内容</label>
              <textarea id="post-content" v-model="postForm.content" placeholder="请输入帖子内容" class="form-textarea"></textarea>
            </div>
          </div>
          <div class="dialog-footer">
            <button @click="closePostDialog" class="btn cancel-btn">取消</button>
            <button @click="updatePost" class="btn submit-btn">保存</button>
          </div>
        </div>
      </div>

      <!-- 编辑评论对话框 -->
      <div class="dialog-overlay" v-if="showEditCommentDialog">
        <div class="dialog-content">
          <div class="dialog-header">
            <h3>编辑评论</h3>
            <button @click="closeCommentDialog" class="close-btn">×</button>
          </div>
          <div class="dialog-body">
            <div class="form-group">
              <label for="comment-content">内容</label>
              <textarea id="comment-content" v-model="commentForm.content" placeholder="请输入评论内容" class="form-textarea"></textarea>
            </div>
          </div>
          <div class="dialog-footer">
            <button @click="closeCommentDialog" class="btn cancel-btn">取消</button>
            <button @click="updateComment" class="btn submit-btn">保存</button>
          </div>
        </div>
      </div>


    </div>

    <!-- 版权信息 -->
    <footer class="footer">
      <div class="footer-content">
        <p>© 2026 Niko动漫. 保留所有权利.</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, inject, defineComponent, h } from 'vue';
import api from '@/utils/api';
import { ElMessageBox, ElMessage } from 'element-plus';

// 处理图片URL
const getImageUrl = (url: string | undefined | null): string => {
  if (!url) {
    return 'http://localhost:8080/avatars/avatar1.jpg';
  }
  
  // 检查是否已经是完整的URL
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  
  // 如果是本地文件路径（Windows或Unix风格），转换为HTTP URL
  if (url.includes(':')) {
    // 提取文件名，同时处理正斜杠和反斜杠
    const parts = url.split(/[\\/]/);
    const filename = parts.pop();
    return `http://localhost:8080/avatars/${filename}`;
  }
  
  // 直接返回url
  return url;
};





// 注入reload方法
const reload = inject('reload') as () => void;

// 搜索关键词
const searchKeyword = ref('');

// 排序方式
const sortBy = ref('time');
const sortDirection = ref('desc');

// 帖子列表
const posts = ref<any[]>([]);

// 分页
const postPage = ref(1);
const postPageSize = 6;
const postTotalPages = computed(() => Math.ceil(posts.value.length / postPageSize) || 1);
const pagedPosts = computed(() => {
  const start = (postPage.value - 1) * postPageSize;
  return posts.value.slice(start, start + postPageSize);
});
const postPageNumbers = computed(() => {
  const total = postTotalPages.value;
  const curr = postPage.value;
  const pages: number[] = [];
  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i);
  } else {
    pages.push(1);
    if (curr > 3) pages.push(-1);
    const start = Math.max(2, curr - 1);
    const end = Math.min(total - 1, curr + 1);
    for (let i = start; i <= end; i++) pages.push(i);
    if (curr < total - 2) pages.push(-1);
    pages.push(total);
  }
  return pages;
});

// 显示评论状态
const showComments = ref<Record<number, boolean>>({});
// 评论排序选项
const commentSortOptions = [
  { label: '按时间', value: 'time' },
  { label: '按点赞数', value: 'likes' },
  { label: '按点踩数', value: 'dislikes' }
];

// 评论排序方式
const commentSortBy = ref<Record<number, string>>({});

// 评论排序方向
const commentSortDirection = ref<Record<number, string>>({});

// 对话框状态
const showEditPostDialog = ref(false);
const showDeleteConfirmDialog = ref(false);
const postToDelete = ref(0);
const showEditCommentDialog = ref(false);
const commentToDelete = ref({ postId: 0, commentId: 0 });

// 帖子表单
const postForm = ref({
  id: 0,
  title: '',
  content: ''
});

// 评论表单
const commentForm = ref({
  id: 0,
  postId: 0,
  content: ''
});

// 加载帖子列表
const loadPosts = async () => {
  try {
    const res = await api.post('http://localhost:8080/api/admin/forum/posts');
    if (res.data.code === 200) {
          posts.value = res.data.data.map((post: any) => ({
            ...post,
            authorName: post.author?.username || '未知用户',
            authorAvatar: post.author?.avatar || ''
          }));
          postPage.value = 1;
        }
  } catch (error) {
    console.error('加载帖子列表失败：', error);
  }
};

// 搜索帖子
const searchPosts = async () => {
  try {
    const res = await api.post('http://localhost:8080/api/admin/forum/posts/search', {
      keyword: searchKeyword.value
    });
    if (res.data.code === 200) {
          posts.value = res.data.data.map((post: any) => ({
            ...post,
            authorName: post.author?.username || '未知用户',
            authorAvatar: post.author?.avatar || ''
          }));
          postPage.value = 1;
        }
  } catch (error) {
    console.error('搜索帖子失败：', error);
  }
};

// 切换排序方向
const toggleSortDirection = () => {
  sortDirection.value = sortDirection.value === 'desc' ? 'asc' : 'desc';
  sortPosts();
};

// 排序帖子
const sortPosts = async () => {
  try {
    const res = await api.post('http://localhost:8080/api/admin/forum/posts/sort', {
      sortBy: sortBy.value,
      direction: sortDirection.value
    });
    if (res.data.code === 200) {
          posts.value = res.data.data.map((post: any) => ({
            ...post,
            authorName: post.author?.username || '未知用户',
            authorAvatar: post.author?.avatar || ''
          }));
          postPage.value = 1;
        }
  } catch (error) {
    console.error('排序帖子失败：', error);
  }
};

// 编辑帖子
const editPost = (post: any) => {
  postForm.value = { ...post };
  showEditPostDialog.value = true;
};

// 更新帖子
const updatePost = async () => {
  try {
    const res = await api.post('http://localhost:8080/api/admin/forum/posts/update', postForm.value);
    if (res.data.code === 200) {
      ElMessage.success('帖子更新成功！');
      closePostDialog();
      loadPosts();
    }
  } catch (error) {
    console.error('更新帖子失败：', error);
    ElMessage.error('更新帖子失败，请重试');
  }
};

// 显示删除确认对话框
const showDeleteConfirm = async (postId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该帖子吗？删除后将无法恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    postToDelete.value = postId;
    await confirmDelete();
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除帖子失败：', error);
    }
  }
};

// 确认删除
const confirmDelete = async () => {
  try {
    const res = await api.post('http://localhost:8080/api/admin/forum/posts/delete', {
      id: postToDelete.value
    });
    if (res.data.code === 200) {
      ElMessage.success('帖子删除成功！');
      showDeleteConfirmDialog.value = false;
      loadPosts();
    }
  } catch (error) {
    console.error('删除帖子失败：', error);
    ElMessage.error('删除帖子失败，请重试');
    showDeleteConfirmDialog.value = false;
  }
};

// 切换评论显示
const toggleComments = async (postId: number) => {
  if (showComments.value[postId] === undefined) {
    showComments.value[postId] = false;
  }
  showComments.value[postId] = !showComments.value[postId];
  
  if (showComments.value[postId]) {
    await loadComments(postId);
  }
};

// 加载评论
const loadComments = async (postId: number) => {
  try {
    const res = await api.post('http://localhost:8080/api/comment/getComments', {
      postId
    });
    if (res.data.code === 200) {
      const postIndex = posts.value.findIndex(p => p.id === postId);
      if (postIndex !== -1) {
        // 处理评论数据，添加时间戳确保key值变化
        const timestamp = Date.now();
        let comments = res.data.data.map((comment: any) => ({
          ...comment,
          authorName: comment.authorName || '未知用户',
          authorAvatar: comment.authorAvatar || '',
          lastUpdated: timestamp
        }));
        
        // 根据排序方式和方向对评论进行排序
        const sortBy = commentSortBy.value[postId] || 'time';
        const direction = commentSortDirection.value[postId] || 'desc';
        comments.sort((a: any, b: any) => {
          if (sortBy === 'time') {
            const dateA = new Date(a.createTime).getTime();
            const dateB = new Date(b.createTime).getTime();
            return direction === 'desc' ? dateB - dateA : dateA - dateB;
          } else if (sortBy === 'likes') {
            const likesA = a.likeCount || 0;
            const likesB = b.likeCount || 0;
            return direction === 'desc' ? likesB - likesA : likesA - likesB;
          } else if (sortBy === 'dislikes') {
            const dislikesA = a.dislikeCount || 0;
            const dislikesB = b.dislikeCount || 0;
            return direction === 'desc' ? dislikesB - dislikesA : dislikesA - dislikesB;
          }
          return 0;
        });
        
        // 直接修改posts数组，触发Vue的响应式更新
        // 先创建一个新的数组副本
        const newPosts = JSON.parse(JSON.stringify(posts.value));
        // 更新对应帖子的评论
        newPosts[postIndex].comments = comments;
        // 为帖子添加时间戳，确保整个帖子组件重新渲染
        newPosts[postIndex].lastUpdated = timestamp;
        // 重新赋值整个数组，确保Vue检测到变化
        posts.value = newPosts;
      }
    }
  } catch (error) {
    console.error('加载评论失败：', error);
  }
};

// 选择评论排序方式
const selectCommentSort = (postId: number, sortBy: string) => {
  commentSortBy.value[postId] = sortBy;
  // 重新加载评论以应用排序
  loadComments(postId);
};

// 切换评论排序方向
const toggleCommentSort = (postId: number) => {
  // 初始化排序方向
  if (!commentSortDirection.value[postId]) {
    commentSortDirection.value[postId] = 'desc';
  }
  // 切换排序方向
  commentSortDirection.value[postId] = commentSortDirection.value[postId] === 'desc' ? 'asc' : 'desc';
  
  // 重新加载评论以应用排序
  loadComments(postId);
};





// 编辑评论
const editComment = (postId: number, comment: any) => {
  commentForm.value = { ...comment, postId };
  showEditCommentDialog.value = true;
};

// 更新评论
const updateComment = async () => {
  try {
    const res = await api.post('http://localhost:8080/api/admin/forum/comments/update', commentForm.value);
    if (res.data.code === 200) {
      ElMessage.success('评论更新成功！');
      closeCommentDialog();
      // 重新加载评论
      await loadComments(commentForm.value.postId);
    }
  } catch (error) {
    console.error('更新评论失败：', error);
    ElMessage.error('更新评论失败，请重试');
  }
};

// 显示评论删除确认对话框
const showCommentDeleteConfirm = async (postId: number, commentId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该评论吗？删除后将无法恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    commentToDelete.value = { postId, commentId };
    await confirmCommentDelete();
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除评论失败：', error);
    }
  }
};

// 确认删除评论
const confirmCommentDelete = async () => {
  try {
    const res = await api.post('http://localhost:8080/api/admin/forum/comments/delete', {
      id: commentToDelete.value.commentId
    });
    if (res.data.code === 200) {
      ElMessage.success('评论删除成功！');
      // 重新加载评论
      await loadComments(commentToDelete.value.postId);
      // 更新帖子的评论数
      const post = posts.value.find(p => p.id === commentToDelete.value.postId);
      if (post) {
        post.commentCount = post.commentCount - 1;
        if (post.commentCount < 0) post.commentCount = 0;
      }
      showDeleteConfirmDialog.value = false;
    }
  } catch (error) {
    console.error('删除评论失败：', error);
    ElMessage.error('删除评论失败，请重试');
    showDeleteConfirmDialog.value = false;
  }
};

// 关闭帖子对话框
const closePostDialog = () => {
  showEditPostDialog.value = false;
  postForm.value = {
    id: 0,
    title: '',
    content: ''
  };
};

// 关闭评论对话框
const closeCommentDialog = () => {
  showEditCommentDialog.value = false;
  commentForm.value = {
    id: 0,
    postId: 0,
    content: ''
  };
};

// 格式化时间
const formatTime = (time: Date | string) => {
  const date = typeof time === 'string' ? new Date(time) : time;
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// WebSocket连接
let ws: WebSocket | null = null;

// 初始化WebSocket连接
const initWebSocket = () => {
  // 创建WebSocket连接
  ws = new WebSocket('ws://localhost:5173/ws/comments');
  
  // 连接建立时
  ws.onopen = () => {
    console.log('WebSocket连接已建立');
  };
  
  // 接收消息时
  ws.onmessage = (event) => {
    const message = event.data;
    console.log('收到WebSocket消息:', message);
    
    // 处理评论更新消息
    if (message.startsWith('comment_updated:')) {
      // 提取评论ID
      const commentId = message.split(':')[1];
      console.log('评论已更新，ID:', commentId);
      
      // 重新加载所有帖子的评论，确保页面显示最新数据
      posts.value.forEach(post => {
        if (showComments.value[post.id]) {
          loadComments(post.id);
        }
      });
    }
  };
  
  // 连接关闭时
  ws.onclose = () => {
    console.log('WebSocket连接已关闭');
  };
  
  // 连接出错时
  ws.onerror = (error) => {
    console.error('WebSocket连接出错:', error);
  };
};

// 页面加载时加载帖子列表
onMounted(() => {
  // 初始加载时使用默认排序方式
  sortPosts();
  // 初始化WebSocket连接
  initWebSocket();
});

// 页面卸载时关闭WebSocket连接
onUnmounted(() => {
  if (ws) {
    ws.close();
  }
});
</script>

<style scoped>
.admin-container {
  min-height: 100vh;
  background: #f5f5f5;
}

/* 导航栏样式 */
.navbar {
  background-color: #333;
  color: white;
  padding: 10px 0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.navbar-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 60px;
}

.logo {
  font-size: 24px;
  font-weight: bold;
  color: #ff6b6b;
}

.nav-links {
  display: flex;
  list-style: none;
}

.nav-links li {
  margin-left: 20px;
}

.nav-links a {
  color: white;
  text-decoration: none;
  font-size: 16px;
  transition: color 0.3s;
}

.nav-links a:hover {
  color: #ff6b6b;
}

.nav-links a.active {
  color: #ff6b6b;
  font-weight: 500;
}

/* 内容区域样式 */
.container {
  max-width: 1200px;
  margin: 20px auto;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.container h2 {
  margin-bottom: 20px;
  color: #333;
}

/* 操作栏样式 */
.actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}

.search-box {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-input {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  min-width: 200px;
}

.search-btn {
  padding: 8px 16px;
  background: #ff6b6b;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.search-btn:hover {
  background: #ff5252;
}

.sort-box {
  display: flex;
  gap: 10px;
  align-items: center;
}

.sort-select {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  min-width: 120px;
}

.sort-direction-btn {
  padding: 8px 16px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.sort-direction-btn:hover {
  background: #45a049;
}

/* 帖子列表样式 */
.post-list {
  margin-top: 20px;
}

.post-item {
  background: #f9f9f9;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.post-header {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 8px;
  margin-bottom: 15px;
  width: 100%;
  font-size: 14px;
  color: #666;
}

.post-title-container {
  margin-bottom: 15px;
}

.post-author {
  font-size: 14px;
  color: #666;
  margin: 0;
  padding: 0;
}

.post-title {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.author-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.author-avatar.small {
  width: 24px;
  height: 24px;
}

.comment-author-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.post-content {
  margin-bottom: 15px;
  line-height: 1.5;
  color: #333;
}

.post-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
  font-size: 14px;
  color: #666;
}

.stat-item {
  display: flex;
  align-items: center;
}

.post-actions {
  display: flex;
  gap: 10px;
  border-top: 1px solid #eee;
  padding-top: 15px;
}

.edit-btn {
  padding: 8px 16px;
  background: #2196F3;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.edit-btn:hover {
  background: #0b7dda;
}

.delete-btn {
  padding: 8px 16px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.delete-btn:hover {
  background: #d32f2f;
}

.comments-btn {
  padding: 8px 16px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.comments-btn:hover {
  background: #45a049;
}

/* 评论管理样式 */
.comments-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.comments-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 15px;
}

.sort-order-btn {
  padding: 4px 12px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.3s;
}

.sort-order-btn:hover {
  background: #45a049;
}

.sort-order-btn.small {
  padding: 4px 12px;
  font-size: 12px;
}

.comment-sort {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sort-label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.sort-option.small {
  padding: 4px 12px;
  cursor: pointer;
  border-radius: 16px;
  font-size: 12px;
  transition: all 0.3s;
  background: #f0f0f0;
}

.sort-option.small:hover {
  background: #e0e0e0;
}

.sort-option.small.active {
  background: #ff6b6b;
  color: white;
}

.comment-stats {
  display: flex;
  gap: 15px;
  margin-bottom: 10px;
  font-size: 12px;
  color: #666;
}

.comment-stats .stat-item {
  display: flex;
  align-items: center;
}

.comment-item {
  background: white;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 10px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.comment-author {
  font-weight: bold;
  color: #333;
  font-size: 14px;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-content {
  margin-bottom: 10px;
  line-height: 1.5;
  color: #333;
  font-size: 14px;
}

.comment-actions {
  display: flex;
  gap: 8px;
}

.edit-btn.small {
  padding: 4px 12px;
  font-size: 12px;
}

.delete-btn.small {
  padding: 4px 12px;
  font-size: 12px;
}



/* 对话框样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.dialog-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  box-shadow: 0 5px 15px rgba(0,0,0,0.3);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.dialog-header h3 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  transition: color 0.3s;
}

.close-btn:hover {
  color: #333;
}

.dialog-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #555;
}

.form-input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
}

.form-textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
  resize: vertical;
  min-height: 150px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 20px;
  border-top: 1px solid #eee;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.3s;
}

.cancel-btn {
  background: #f0f0f0;
  color: #333;
}

.cancel-btn:hover {
  background: #e0e0e0;
}

.submit-btn {
  background: #ff6b6b;
  color: white;
}

.submit-btn:hover {
  background: #ff5252;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .navbar-container {
    padding: 0 15px;
  }
  
  .nav-links li {
    margin-left: 15px;
  }
  
  .container {
    padding: 15px;
  }
  
  .actions {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-box {
    width: 100%;
  }
  
  .search-input {
    flex: 1;
  }
  
  .post-header {
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
  }
  
  .post-author {
    margin-left: 8px;
    margin-top: 0;
  }
  
  .post-stats {
    flex-wrap: wrap;
    gap: 10px;
  }
  
  .post-actions {
    flex-wrap: wrap;
  }
  
  .dialog-content {
    width: 95%;
  }
}

/* 分页样式 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.page-btn {
  padding: 6px 14px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
}

.page-btn:hover:not(:disabled) {
  border-color: #ff6b6b;
  color: #ff6b6b;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-num {
  padding: 6px 12px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 13px;
  transition: all 0.3s;
  border: 1px solid #ddd;
  background: white;
}

.page-num:hover {
  background: #f0f0f0;
}

.page-num.active {
  background: #ff6b6b;
  color: white;
}

.page-total {
  font-size: 13px;
  color: #999;
  margin-left: 8px;
}

/* 版权信息样式 */
.footer {
  background: #333;
  color: #fff;
  padding: 20px 0;
  margin-top: 30px;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
  padding: 0 20px;
}

.footer-content p {
  margin: 0;
  font-size: 14px;
  color: #ccc;
}
</style>