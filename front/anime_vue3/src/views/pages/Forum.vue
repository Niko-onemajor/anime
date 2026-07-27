<template>
  <div class="forum-container">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="navbar-container">
        <div class="logo">Niko动漫</div>
        <ul class="nav-links">
          <li><a href="/index">首页</a></li>
          <li><a href="/category">分类</a></li>
          <li><a href="/forum" class="active">论坛</a></li>
          <li><a href="/messages">消息<span v-if="navUnreadCount > 0" class="nav-badge">{{ navUnreadCount }}</span></a></li>
          <li><a href="/profile">个人中心</a></li>
          <li v-if="isAdmin"><a href="/admin/users">管理员后台</a></li>
        </ul>
      </div>
    </nav>

    <!-- 搜索、发布帖子和排序选项 -->
    <div class="search-post-sort-container">
      <div class="search-container">
        <input type="text" v-model="searchKeyword" placeholder="搜索帖子..." class="search-input">
        <button @click="searchPosts" class="search-button">搜索</button>
      </div>
      <div class="sort-options">
        <span 
          v-for="sort in sortOptions" 
          :key="sort.value" 
          class="sort-option" 
          :class="{ active: selectedSort === sort.value }"
          @click="selectSort(sort.value)"
        >
          {{ sort.label }}
        </span>
        <button @click="toggleSortOrder" class="sort-order-btn">
          切换
        </button>
      </div>
      <button @click="showPostDialog = true" class="post-button">发布帖子</button>
    </div>

    <!-- 帖子列表 -->
    <div class="post-container">
      <div class="post-item" v-for="post in pagedPosts" :key="post.id">
        <div class="post-header">
          <div class="post-author">
            <img :src="getImageUrl(post.authorAvatar)" :alt="post.authorName" class="author-avatar" @click="goToUserProfile(post.authorName)" style="cursor: pointer;">
            <div class="author-info">
              <h4 class="author-name" @click="goToUserProfile(post.authorName)" style="cursor: pointer;">{{ post.authorName }}</h4>
              <span class="post-time">{{ formatTime(post.createTime) }}</span>
            </div>
          </div>
          <!-- 帖子操作按钮（仅作者可见） -->
          <div class="post-actions" v-if="post.authorName === currentUser">
            <button @click="editPost(post)" class="action-btn edit-btn">编辑</button>
            <button @click="deletePost(post.id)" class="action-btn delete-btn">删除</button>
          </div>
        </div>
        <div class="post-content">
          <h3 class="post-title">{{ post.title }}</h3>
          <p class="post-text">{{ post.content }}</p>
        </div>
        <div class="post-footer">
          <div class="post-stats">
            <span class="stat-item">
              <button 
                @click="likePost(post.id)" 
                class="stat-btn like-btn" 
                :class="{ active: post.likes.includes(currentUser) }"
              >
                👍 {{ post.likeCount }}
              </button>
            </span>
            <span class="stat-item">
              <button 
                @click="dislikePost(post.id)" 
                class="stat-btn dislike-btn" 
                :class="{ active: post.dislikes.includes(currentUser) }"
              >
                👎 {{ post.dislikeCount }}
              </button>
            </span>
            <span class="stat-item">
              <button 
                @click="toggleComments(post.id)" 
                class="stat-btn comment-btn"
              >
                💬 {{ post.commentCount }}
              </button>
            </span>
          </div>
          
          <!-- 评论区 -->
          <div v-if="showComments[post.id]" class="comments-section">
            <!-- 添加评论 -->
            <div class="add-comment">
              <textarea 
                v-model="commentForm[post.id]" 
                placeholder="写下你的评论..." 
                class="comment-textarea"
              ></textarea>
              <button @click="addComment(post.id)" class="comment-submit-btn">发布评论</button>
            </div>
            
            <!-- 评论排序和分页 -->
            <div class="comment-controls">
              <div class="comment-sort">
                <span class="sort-label">排序方式：</span>
                <span 
                  v-for="sort in commentSortOptions" 
                  :key="sort.value" 
                  class="sort-option small" 
                  :class="{ active: (commentSort[post.id] || 'time') === sort.value }"
                  @click="selectCommentSort(post.id, sort.value)"
                >
                  {{ sort.label }}
                </span>
                <button @click="toggleCommentSortOrder(post.id)" class="sort-order-btn small">
                  切换
                </button>
              </div>
              <div class="comment-pagination" v-if="(commentPages[post.id] || 0) > 1">
                <button 
                  @click="changeCommentPage(post.id, 1)" 
                  class="page-btn" 
                  :disabled="(commentCurrentPage[post.id] || 1) === 1"
                >
                  首页
                </button>
                <button 
                  @click="changeCommentPage(post.id, (commentCurrentPage[post.id] || 1) - 1)" 
                  class="page-btn" 
                  :disabled="(commentCurrentPage[post.id] || 1) === 1"
                >
                  上一页
                </button>
                <span class="page-info">
                  {{ commentCurrentPage[post.id] || 1 }} / {{ commentPages[post.id] || 0 }}
                </span>
                <button 
                  @click="changeCommentPage(post.id, (commentCurrentPage[post.id] || 1) + 1)" 
                  class="page-btn" 
                  :disabled="(commentCurrentPage[post.id] || 1) === (commentPages[post.id] || 0)"
                >
                  下一页
                </button>
                <button 
                  @click="changeCommentPage(post.id, commentPages[post.id] || 1)" 
                  class="page-btn" 
                  :disabled="(commentCurrentPage[post.id] || 1) === (commentPages[post.id] || 0)"
                >
                  末页
                </button>
              </div>
            </div>
            <div class="comments-list">
              <div v-for="comment in getSortedAndPagedComments(post.id)" :key="comment.id" class="comment-item">
                <div class="comment-author">
                  <img :src="getImageUrl(comment.authorAvatar)" :alt="comment.authorName" class="comment-avatar" @click="goToUserProfile(comment.authorName)" style="cursor: pointer;">
                  <div class="comment-info">
                    <h5 class="comment-author-name" @click="goToUserProfile(comment.authorName)" style="cursor: pointer;">{{ comment.authorName }}</h5>
                    <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                  </div>
                </div>
                <div class="comment-content">{{ comment.content }}</div>
                <div class="comment-actions">
                  <button @click="likeComment(comment)" class="action-btn like-btn small" :class="{ active: comment.likes?.includes(currentUser) }">👍 {{ comment.likeCount || 0 }}</button>
                  <button @click="dislikeComment(comment)" class="action-btn dislike-btn small" :class="{ active: comment.dislikes?.includes(currentUser) }">👎 {{ comment.dislikeCount || 0 }}</button>
                  <button @click="toggleReplyForm(post.id, comment.id)" class="action-btn reply-btn small">回复</button>
                  <button v-if="comment.authorName === currentUser" @click="deleteComment(post.id, comment.id)" class="action-btn delete-btn small">删除</button>
                </div>
                
                <!-- 回复输入框 -->
                <div v-if="replyFormVisible[post.id] === comment.id" class="reply-form">
                  <textarea v-model="replyFormContent[post.id]" placeholder="写下你的回复..." class="reply-textarea"></textarea>
                  <div class="reply-form-buttons">
                    <button @click="submitReply(post.id, comment.id)" class="reply-submit-btn">发布回复</button>
                    <button @click="cancelReply(post.id)" class="reply-cancel-btn">取消</button>
                  </div>
                </div>
                
                <!-- 回复列表 -->
                <div v-if="comment.replies && comment.replies.length > 0" class="replies-section">
                  <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                    <div class="comment-author">
                      <img :src="getImageUrl(reply.authorAvatar)" :alt="reply.authorName" class="comment-avatar" @click="goToUserProfile(reply.authorName)" style="cursor: pointer;">
                      <div class="comment-info">
                        <h5 class="comment-author-name" @click="goToUserProfile(reply.authorName)" style="cursor: pointer;">{{ reply.authorName }}</h5>
                        <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
                      </div>
                    </div>
                    <div class="comment-content"><span v-if="reply.replyToName" class="reply-to">@{{ reply.replyToName }}</span> {{ reply.content }}</div>
                    <div class="comment-actions">
                      <button @click="likeComment(reply)" class="action-btn like-btn small" :class="{ active: reply.likes?.includes(currentUser) }">👍 {{ reply.likeCount || 0 }}</button>
                      <button @click="dislikeComment(reply)" class="action-btn dislike-btn small" :class="{ active: reply.dislikes?.includes(currentUser) }">👎 {{ reply.dislikeCount || 0 }}</button>
                      <button v-if="reply.authorName === currentUser" @click="deleteReply(post.id, comment.id, reply.id)" class="action-btn delete-btn small">删除</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页导航 -->
    <div class="pagination" v-if="totalPages > 1">
      <button @click="changePage(1)" :disabled="currentPage === 1" class="page-btn">首页</button>
      <button @click="changePage(currentPage - 1)" :disabled="currentPage === 1" class="page-btn">上一页</button>
      <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
      <button @click="changePage(currentPage + 1)" :disabled="currentPage === totalPages" class="page-btn">下一页</button>
      <button @click="changePage(totalPages)" :disabled="currentPage === totalPages" class="page-btn">尾页</button>
    </div>

    <!-- 发布帖子对话框 -->
    <div class="dialog-overlay" v-if="showPostDialog">
      <div class="dialog-content">
        <div class="dialog-header">
          <h3>{{ isEditing ? '编辑帖子' : '发布帖子' }}</h3>
          <button @click="showPostDialog = false" class="close-btn">×</button>
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
          <button @click="showPostDialog = false" class="btn cancel-btn">取消</button>
          <button @click="submitPost" class="btn submit-btn">发布</button>
        </div>
      </div>
    </div>

    <!-- 用户资料弹窗 -->
    <div class="dialog-overlay" v-if="showUserProfile">
      <div class="dialog-content user-profile-dialog">
        <div class="dialog-header">
          <h3>{{ selectedUser.username }}的个人资料</h3>
          <button @click="closeUserProfile" class="close-btn">×</button>
        </div>
        <div class="dialog-body">
          <!-- 头像 -->
          <div class="user-avatar-section">
            <img :src="getImageUrl(selectedUser.avatar)" :alt="selectedUser.username" class="user-avatar" @click="showAvatarPreview">
            <span class="avatar-hint">点击查看大图</span>
          </div>
          
          <!-- 个人资料 -->
          <div class="user-profile-info">
            <div class="profile-item">
              <span class="profile-label">用户名：</span>
              <span class="profile-value">{{ selectedUser.username }}</span>
            </div>
            <div class="profile-item">
              <span class="profile-label">角色：</span>
              <span class="profile-value">{{ selectedUser.role === '1' ? '管理员' : '普通用户' }}</span>
            </div>
            <div class="profile-item">
              <span class="profile-label">性别：</span>
              <span class="profile-value">{{ selectedUser.gender || '未设置' }}</span>
            </div>
            <div class="profile-item">
              <span class="profile-label">生日：</span>
              <span class="profile-value">{{ selectedUser.birthday || '未设置' }}</span>
            </div>
            <div class="profile-item">
              <span class="profile-label">地区：</span>
              <span class="profile-value">{{ selectedUser.region || '未设置' }}</span>
            </div>
            <div class="profile-item">
              <span class="profile-label">邮箱：</span>
              <span class="profile-value">{{ selectedUser.email || '未设置' }}</span>
            </div>
            <div class="profile-item">
              <span class="profile-label">喜爱的动漫：</span>
              <span class="profile-value">{{ selectedUser.favorite || '未设置' }}</span>
            </div>
            <div class="profile-item">
              <span class="profile-label">签名：</span>
              <span class="profile-value">{{ selectedUser.signature || '未设置' }}</span>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="closeUserProfile" class="btn cancel-btn">关闭</button>
        </div>
      </div>
    </div>

    <!-- 头像大图预览弹窗 -->
    <div class="dialog-overlay" v-if="showAvatarPreviewModal" @click="closeAvatarPreview">
      <div class="avatar-preview-container" @click.stop>
        <button @click="closeAvatarPreview" class="close-btn avatar-close-btn">×</button>
        <img :src="getImageUrl(selectedUser.avatar)" :alt="selectedUser.username" class="avatar-preview-img">
      </div>
    </div>
  </div>

  <!-- 版权信息 -->
  <footer class="footer">
    <div class="footer-content">
      <p>© 2026 Niko动漫. 保留所有权利.</p>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, defineComponent, h } from 'vue';
import axios from 'axios';
import api from '@/utils/api';
import { ElMessageBox, ElMessage } from 'element-plus';

// 处理图片URL
const getImageUrl = (url: string | undefined | null): string => {
  if (!url) {
    return '/avatars/avatar1.jpg';
  }
  
  // 检查是否已经是完整的URL
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  
  // 检查是否是本地路径
  if (url.startsWith('/src/')) {
    return '/avatars/avatar1.jpg';
  }
  
  // 如果是本地文件路径（Windows或Unix风格），转换为HTTP URL
  if (url.includes(':')) {
    // 提取文件名，同时处理正斜杠和反斜杠
    const parts = url.split(/[\\/]/);
    const filename = parts.pop();
    // 判断是头像还是封面
    if (url.includes('avatars')) {
      return `/avatars/${filename}`;
    } else {
      return `/covers/${filename}`;
    }
  }
  
  // 直接返回url，因为后端应该已经返回了完整的阿里云URL
  return url;
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



// 当前用户
const currentUser = ref(localStorage.getItem('username') || 'user1');
console.log('当前用户:', currentUser.value);

// 检查用户是否为管理员
const isAdmin = ref(false);
const navUnreadCount = ref(0);

const checkAdmin = () => {
  // 先检查localStorage
  let role = localStorage.getItem('role');
  // 如果localStorage中没有，检查sessionStorage
  if (!role) {
    role = sessionStorage.getItem('role');
  }
  isAdmin.value = role === 'admin' || role === '1';
};

const fetchUnreadCount = async () => {
  const username = localStorage.getItem('username') || sessionStorage.getItem('username');
  if (!username) return;
  try {
    const res = await api.get('/api/notifications/unread-count', { params: { username } });
    if (res.data.code === 200) {
      navUnreadCount.value = res.data.unreadCount || 0;
    }
  } catch (e) { /* ignore */ }
};

// 发布帖子对话框
const showPostDialog = ref(false);
const isEditing = ref(false);
const editingPostId = ref<number | null>(null);

// 帖子表单
const postForm = ref({
  title: '',
  content: ''
});

// 评论相关
const showComments = ref<Record<number, boolean>>({});
const commentForm = ref<Record<number, string>>({});

// 排序选项
const selectedSort = ref('time');
const sortOptions = [
  { label: '按时间', value: 'time' },
  { label: '按点赞数', value: 'likes' },
  { label: '按点踩数', value: 'dislikes' },
  { label: '按评论数', value: 'comments' }
];

// 排序顺序
const sortOrder = ref('desc'); // desc: 降序, asc: 升序
const toggleSortOrder = () => {
  sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc';
  currentPage.value = 1;
};

// 搜索相关
const searchKeyword = ref('');
const searchPosts = async () => {
  try {
    const res = await axios.post('http://localhost:8080/api/post/search', {
      keyword: searchKeyword.value
    });
    if (res.data.code === 200) {
      posts.value = res.data.data.map((post: any) => ({
        ...post,
        authorName: post.author?.username || '未知用户',
        authorAvatar: post.author?.avatar || '',
        createTime: new Date(post.createTime),
        likes: post.likes || [],
        dislikes: post.dislikes || [],
        comments: []
      }));
      currentPage.value = 1;
    }
  } catch (error) {
    console.error('搜索帖子失败：', error);
  }
};

// 评论排序选项
const commentSortOptions = [
  { label: '按时间', value: 'time' },
  { label: '按点赞数', value: 'likes' },
  { label: '按点踩数', value: 'dislikes' }
];

// 评论排序状态
const commentSort = ref<Record<number, string>>({});
const commentSortOrder = ref<Record<number, string>>({});

// 回复相关状态
const replyFormVisible = ref<Record<number, number | null>>({}); // postId -> commentId (null表示隐藏)
const replyFormContent = ref<Record<number, string>>({});

// 评论分页
const commentCurrentPage = ref<Record<number, number>>({});
const commentPages = ref<Record<number, number>>({});
const commentsPerPage = 5;

// 选择评论排序
const selectCommentSort = (postId: number, sort: string) => {
  commentSort.value[postId] = sort;
  commentCurrentPage.value[postId] = 1; // 重置到第一页
};

// 切换评论排序顺序
const toggleCommentSortOrder = (postId: number) => {
  commentSortOrder.value[postId] = commentSortOrder.value[postId] === 'desc' ? 'asc' : 'desc';
  commentCurrentPage.value[postId] = 1; // 重置到第一页
};

// 切换评论页面
const changeCommentPage = (postId: number, page: number) => {
  commentCurrentPage.value[postId] = page;
};

// 切换回复输入框
const toggleReplyForm = (postId: number, commentId: number) => {
  if (replyFormVisible.value[postId] === commentId) {
    replyFormVisible.value[postId] = null;
  } else {
    replyFormVisible.value[postId] = commentId;
    replyFormContent.value[postId] = '';
  }
};

// 取消回复
const cancelReply = (postId: number) => {
  replyFormVisible.value[postId] = null;
  replyFormContent.value[postId] = '';
};

// 提交回复
const submitReply = async (postId: number, commentId: number) => {
  const content = replyFormContent.value[postId];
  if (!content) {
    ElMessage.warning('请输入回复内容');
    return;
  }

  try {
    const res = await axios.post('http://localhost:8080/api/comment/addComment', {
      postId,
      username: currentUser.value,
      content,
      parentId: commentId
    });
    
    if (res.data.code === 200) {
      const post = posts.value.find(p => p.id === postId);
      if (post) {
        const newReply = res.data.data as Comment;
        const userAvatar = localStorage.getItem('avatar');
        if (userAvatar) {
          newReply.authorAvatar = userAvatar;
        }
        newReply.likes = [];
        newReply.dislikes = [];
        newReply.likeCount = 0;
        newReply.dislikeCount = 0;
        newReply.replyToName = res.data.data.replyToName || '';
        
        // 找到父评论并添加回复
        const parentComment = post.comments.find(c => c.id === commentId);
        if (parentComment) {
          if (!parentComment.replies) {
            parentComment.replies = [];
          }
          parentComment.replies.push(newReply);
          post.commentCount++;
        }
      }
      replyFormVisible.value[postId] = null;
      replyFormContent.value[postId] = '';
      ElMessage.success('回复发布成功！');
    } else {
      ElMessage.error('回复发布失败：' + res.data.msg);
    }
  } catch (error) {
    console.error('发布回复失败：', error);
    ElMessage.error('发布回复失败，请重试');
  }
};

// 删除回复
const deleteReply = async (postId: number, commentId: number, replyId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条回复吗？', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    
    const res = await axios.post('http://localhost:8080/api/comment/deleteComment', {
      commentId: replyId
    });
    if (res.data.code === 200) {
      const post = posts.value.find(p => p.id === postId);
      if (post) {
        const parentComment = post.comments.find(c => c.id === commentId);
        if (parentComment && parentComment.replies) {
          parentComment.replies = parentComment.replies.filter(r => r.id !== replyId);
          post.commentCount--;
          ElMessage.success('回复删除成功！');
        }
      }
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除回复失败：', error);
      ElMessage.error('删除回复失败，请重试');
    }
  }
};

// 定义回复类型
interface Reply {
  id: number;
  content: string;
  createTime: Date | string;
  authorName: string;
  authorAvatar: string;
  replyToName: string;
  likes?: string[];
  dislikes?: string[];
  likeCount?: number;
  dislikeCount?: number;
  replies?: Reply[];
}

// 定义评论类型
interface Comment {
  id: number;
  content: string;
  createTime: Date | string;
  authorName: string;
  authorAvatar: string;
  replyToName?: string;
  likes?: string[];
  dislikes?: string[];
  likeCount?: number;
  dislikeCount?: number;
  replies?: Reply[];
}

// 定义帖子类型
interface Post {
  id: number;
  title: string;
  content: string;
  authorName: string;
  authorAvatar: string;
  createTime: Date;
  likes: string[];
  dislikes: string[];
  likeCount: number;
  dislikeCount: number;
  commentCount: number;
  comments: Comment[];
}

// 帖子数据
const posts = ref<Post[]>([]);

// 分页相关
const currentPage = ref(1);
const pageSize = 6;
const totalPages = computed(() => Math.ceil(sortedPosts.value.length / pageSize));

// 分页后的帖子列表
const pagedPosts = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  const end = start + pageSize;
  return sortedPosts.value.slice(start, end);
});

// 切换页码
const changePage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
  }
};

// 加载帖子列表
const loadPosts = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/post/list');
    // GET /api/post/list 直接返回帖子数组
    const postList = res.data;
    if (postList && Array.isArray(postList)) {
      // 保存旧评论数据，避免点赞后评论区消失
      const oldCommentsMap: Record<number, Comment[]> = {};
      posts.value.forEach(p => {
        if (p.comments && p.comments.length > 0) {
          oldCommentsMap[p.id] = p.comments;
        }
      });
      
      posts.value = postList.map((post: any) => ({
        ...post,
        authorName: post.author?.username || '未知用户',
        authorAvatar: post.author?.avatar || '',
        createTime: new Date(post.createTime),
        likes: post.likes || [],
        dislikes: post.dislikes || [],
        comments: oldCommentsMap[post.id] || []
      }));
      
      // 如果用户已登录，更新用户的互动状态
      if (currentUser.value) {
        await updateUserInteractionStatus();
      }
    }
  } catch (error) {
    console.error('加载帖子失败：', error);
  }
};

// 更新用户的互动状态
const updateUserInteractionStatus = async () => {
  try {
    // 清空之前的互动状态
    posts.value.forEach((post: any) => {
      post.likes = [];
      post.dislikes = [];
    });

    // 从后端获取用户对每个帖子的互动状态
    for (const post of posts.value) {
      try {
        const res = await axios.post('http://localhost:8080/api/post/interaction-status', {
          postId: post.id,
          username: currentUser.value
        });
        
        if (res.data.code === 200) {
          const interactionType = res.data.data;
          // 根据互动类型更新前端状态
          if (interactionType === 1) {
            // 点赞
            post.likes.push(currentUser.value);
          } else if (interactionType === 2) {
            // 点踩
            post.dislikes.push(currentUser.value);
          }
        }
      } catch (error) {
        console.error(`获取帖子${post.id}的互动状态失败：`, error);
      }
    }
  } catch (error) {
    console.error('更新用户互动状态失败：', error);
  }
};

// 排序后的帖子列表
const sortedPosts = computed(() => {
  const sorted = [...posts.value];
  switch (selectedSort.value) {
    case 'time':
      return sorted.sort((a, b) => {
        const timeA = new Date(a.createTime).getTime();
        const timeB = new Date(b.createTime).getTime();
        return sortOrder.value === 'desc' ? timeB - timeA : timeA - timeB;
      });
    case 'likes':
      return sorted.sort((a, b) => {
        const likesA = a.likeCount || 0;
        const likesB = b.likeCount || 0;
        return sortOrder.value === 'desc' ? likesB - likesA : likesA - likesB;
      });
    case 'dislikes':
      return sorted.sort((a, b) => {
        const dislikesA = a.dislikeCount || 0;
        const dislikesB = b.dislikeCount || 0;
        return sortOrder.value === 'desc' ? dislikesB - dislikesA : dislikesA - dislikesB;
      });
    case 'comments':
      return sorted.sort((a, b) => {
        const commentsA = a.commentCount || 0;
        const commentsB = b.commentCount || 0;
        return sortOrder.value === 'desc' ? commentsB - commentsA : commentsA - commentsB;
      });
    default:
      return sorted;
  }
});

// 获取排序和分页后的评论
const getSortedAndPagedComments = (postId: number) => {
  const post = posts.value.find(p => p.id === postId);
  if (!post || !post.comments) return [];
  
  // 排序评论
  const sorted = [...post.comments].sort((a, b) => {
    const sortType = commentSort.value[postId] || 'time';
    const order = commentSortOrder.value[postId] || 'desc';
    
    if (sortType === 'time') {
      const timeA = new Date(a.createTime).getTime();
      const timeB = new Date(b.createTime).getTime();
      return order === 'desc' ? timeB - timeA : timeA - timeB;
    } else if (sortType === 'likes') {
      const likesA = a.likeCount || 0;
      const likesB = b.likeCount || 0;
      return order === 'desc' ? likesB - likesA : likesA - likesB;
    } else if (sortType === 'dislikes') {
      const dislikesA = a.dislikeCount || 0;
      const dislikesB = b.dislikeCount || 0;
      return order === 'desc' ? dislikesB - dislikesA : dislikesA - dislikesB;
    }
    return 0;
  });
  
  // 分页
  const currentPage = commentCurrentPage.value[postId] || 1;
  const start = (currentPage - 1) * commentsPerPage;
  const end = start + commentsPerPage;
  
  return sorted.slice(start, end);
};

// 选择排序
const selectSort = (sort: string) => {
  selectedSort.value = sort;
  currentPage.value = 1;
};

// 发布帖子
const submitPost = async () => {
  if (!postForm.value.title || !postForm.value.content) {
    ElMessage.warning('请填写标题和内容');
    return;
  }

  try {
    if (isEditing.value && editingPostId.value) {
      // 编辑帖子
      const res = await axios.post('http://localhost:8080/api/post/update', {
        id: editingPostId.value,
        title: postForm.value.title,
        content: postForm.value.content
      });
      if (res.data.code === 200) {
        ElMessage.success('帖子编辑成功！');
        await loadPosts();
        // 确保所有帖子的作者头像正确显示
        posts.value.forEach(post => {
          if (post.authorName === currentUser.value) {
            const userAvatar = localStorage.getItem('avatar');
            if (userAvatar) {
              post.authorAvatar = userAvatar;
            }
          }
        });
      }
    } else {
        // 发布新帖子
        const res = await axios.post('http://localhost:8080/api/post/create', {
          title: postForm.value.title,
          content: postForm.value.content,
          username: currentUser.value
        });
      // 检查响应是否成功
      if (res.data && (res.data.code === 200 || res.data.id)) {
        ElMessage.success('帖子发布成功！');
        await loadPosts();
        // 确保所有帖子的作者头像正确显示
        posts.value.forEach(post => {
          if (post.authorName === currentUser.value) {
            const userAvatar = localStorage.getItem('avatar');
            if (userAvatar) {
              post.authorAvatar = userAvatar;
            }
          }
        });
      }
    }
  } catch (error) {
    console.error('发布帖子失败：', error);
    ElMessage.error('发布帖子失败，请重试');
  }

  // 重置表单
  postForm.value = {
    title: '',
    content: ''
  };
  showPostDialog.value = false;
  isEditing.value = false;
  editingPostId.value = null;
};

// 编辑帖子
const editPost = (post: Post) => {
  postForm.value = {
    title: post.title,
    content: post.content
  };
  isEditing.value = true;
  editingPostId.value = post.id;
  showPostDialog.value = true;
};

// 删除帖子
const deletePost = async (postId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这篇帖子吗？', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    
    const res = await axios.delete(`http://localhost:8080/api/post/delete/${postId}`);
    if (res.status === 200) {
      ElMessage.success('帖子删除成功！');
      await loadPosts();
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除帖子失败：', error);
      ElMessage.error('删除帖子失败，请重试');
    }
  }
};

// 点赞帖子
const likePost = async (postId: number) => {
  if (!currentUser.value) {
    ElMessage.warning('请先登录');
    return;
  }
  
  try {
    const res = await axios.post('http://localhost:8080/api/post/like', {
      postId: postId,
      username: currentUser.value
    });
    
    if (res.data.code === 200) {
      // 重新加载帖子数据，确保获取最新的互动数
      await loadPosts();
      
      // 显示操作结果
      const interactionRes = await axios.post('http://localhost:8080/api/post/interaction-status', {
        postId: postId,
        username: currentUser.value
      });
      
      if (interactionRes.data.code === 200) {
        const interactionType = interactionRes.data.data;
        if (interactionType === 1) {
          ElMessage.success('点赞成功');
        } else {
          ElMessage.success('取消点赞成功');
        }
      }
    } else {
      ElMessage.warning(res.data.msg);
    }
  } catch (error) {
    console.error('点赞失败:', error);
    ElMessage.error('点赞失败，请重试');
  }
};

// 点踩帖子
const dislikePost = async (postId: number) => {
  if (!currentUser.value) {
    ElMessage.warning('请先登录');
    return;
  }
  
  try {
    const res = await axios.post('http://localhost:8080/api/post/dislike', {
      postId: postId,
      username: currentUser.value
    });
    
    if (res.data.code === 200) {
      // 重新加载帖子数据，确保获取最新的互动数
      await loadPosts();
      
      // 显示操作结果
      const interactionRes = await axios.post('http://localhost:8080/api/post/interaction-status', {
        postId: postId,
        username: currentUser.value
      });
      
      if (interactionRes.data.code === 200) {
        const interactionType = interactionRes.data.data;
        if (interactionType === 2) {
          ElMessage.success('点踩成功');
        } else {
          ElMessage.success('取消点踩成功');
        }
      }
    } else {
      ElMessage.warning(res.data.msg);
    }
  } catch (error) {
    console.error('点踩失败:', error);
    ElMessage.error('点踩失败，请重试');
  }
};

// 切换评论区显示
const toggleComments = async (postId: number) => {
  // 确保 showComments 中存在该 postId 的键
  if (showComments.value[postId] === undefined) {
    showComments.value[postId] = false;
  }
  showComments.value[postId] = !showComments.value[postId];
  
  // 如果显示评论区，加载评论
  if (showComments.value[postId]) {
    console.log('切换到显示评论区，postId:', postId);
    await loadComments(postId);
  }
};

// 加载评论
const loadComments = async (postId: number) => {
  try {
    console.log('开始加载评论，postId:', postId);
    const res = await axios.post('http://localhost:8080/api/comment/getComments', { postId });
    console.log('加载评论响应：', res.data);
    if (res.data.code === 200) {
      const post = posts.value.find(p => p.id === postId);
      if (post) {
        console.log('找到帖子：', post.title);
        // 处理评论数据，确保头像路径正确
        const comments = res.data.data as Comment[];
        const userAvatar = localStorage.getItem('avatar');
        let totalCommentCount = 0;
        
        comments.forEach(comment => {
          if (comment.authorName === currentUser.value && userAvatar) {
            comment.authorAvatar = userAvatar;
          }
          comment.likes = comment.likes || [];
          comment.dislikes = comment.dislikes || [];
          comment.likeCount = comment.likeCount || 0;
          comment.dislikeCount = comment.dislikeCount || 0;
          totalCommentCount++; // 顶级评论
          
          // 处理回复
          if (comment.replies) {
            comment.replies.forEach(reply => {
              if (reply.authorName === currentUser.value && userAvatar) {
                reply.authorAvatar = userAvatar;
              }
              reply.likes = reply.likes || [];
              reply.dislikes = reply.dislikes || [];
              reply.likeCount = reply.likeCount || 0;
              reply.dislikeCount = reply.dislikeCount || 0;
              totalCommentCount++; // 回复
            });
          } else {
            comment.replies = [];
          }
        });
        
        post.comments = comments;
        post.commentCount = totalCommentCount;
        console.log('评论数据：', post.comments);
        
        // 初始化评论排序和分页状态
        if (commentSort.value[postId] === undefined) {
          commentSort.value[postId] = 'time';
        }
        if (commentSortOrder.value[postId] === undefined) {
          commentSortOrder.value[postId] = 'desc';
        }
        if (commentCurrentPage.value[postId] === undefined) {
          commentCurrentPage.value[postId] = 1;
        }
        // 计算评论页数（只计算顶级评论）
        commentPages.value[postId] = Math.ceil(comments.length / commentsPerPage);
      }
    }
  } catch (error) {
    console.error('加载评论失败：', error);
  }
};

// 添加评论
const addComment = async (postId: number) => {
  const content = commentForm.value[postId];
  if (!content) {
    ElMessage.warning('请输入评论内容');
    return;
  }

  console.log('发布评论，postId:', postId, 'username:', currentUser.value, 'content:', content);
  
  try {
    const res = await axios.post('http://localhost:8080/api/comment/addComment', {
      postId,
      username: currentUser.value,
      content
    });
    
    console.log('评论响应：', res.data);
    
    if (res.data.code === 200) {
      const post = posts.value.find(p => p.id === postId);
      if (post) {
        console.log('找到帖子：', post.title);
        // 处理新评论数据，确保头像路径正确
        const newComment = res.data.data as Comment;
        const userAvatar = localStorage.getItem('avatar');
        if (userAvatar) {
          // 使用最新的头像
          newComment.authorAvatar = userAvatar;
        }
        newComment.replies = [];
        newComment.likes = [];
        newComment.dislikes = [];
        post.comments.push(newComment);
        post.commentCount++;
        commentForm.value[postId] = '';
        ElMessage.success('评论发布成功！');
      }
    } else {
      console.error('评论失败原因：', res.data.msg);
      ElMessage.error('评论发布失败：' + res.data.msg);
    }
  } catch (error) {
    console.error('发布评论失败：', error);
    ElMessage.error('发布评论失败，请重试: ' + (error as Error).message);
  }
};

// 删除评论
const deleteComment = async (postId: number, commentId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    
    const res = await axios.post('http://localhost:8080/api/comment/deleteComment', {
      commentId: commentId
    });
    if (res.data.code === 200) {
      const post = posts.value.find(p => p.id === postId);
      if (post) {
        // 找到要删除的评论
        const comment = post.comments.find(c => c.id === commentId);
        if (comment) {
          // 计算要减少的评论数（包括回复）
          const replyCount = comment.replies ? comment.replies.length : 0;
          post.commentCount -= (1 + replyCount);
          // 从评论列表中移除
          post.comments = post.comments.filter(c => c.id !== commentId);
          ElMessage.success('评论删除成功！');
        }
      }
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除评论失败：', error);
      ElMessage.error('删除评论失败，请重试');
    }
  }
};

// 点赞评论
const likeComment = async (comment: any) => {
  if (!currentUser.value) {
    ElMessage.warning('请先登录');
    return;
  }
  
  try {
    const res = await axios.post('http://localhost:8080/api/comment/like', {
      commentId: comment.id,
      username: currentUser.value
    });
    
    if (res.data.code === 200) {
      // 更新评论数据
      comment.likeCount = res.data.data.likeCount;
      comment.dislikeCount = res.data.data.dislikeCount;
      
      // 更新用户互动状态
      if (comment.likes && comment.likes.includes(currentUser.value)) {
        // 取消点赞
        comment.likes = comment.likes.filter((user: string) => user !== currentUser.value);
        ElMessage.success('取消点赞成功');
      } else {
        // 添加点赞
        if (!comment.likes) {
          comment.likes = [];
        }
        comment.likes.push(currentUser.value);
        // 取消点踩（如果有）
        if (comment.dislikes && comment.dislikes.includes(currentUser.value)) {
          comment.dislikes = comment.dislikes.filter((user: string) => user !== currentUser.value);
        }
        ElMessage.success('点赞成功');
      }
    } else {
      ElMessage.warning(res.data.msg);
    }
  } catch (error) {
    console.error('点赞失败:', error);
    ElMessage.error('点赞失败，请重试');
  }
};

// 点踩评论
const dislikeComment = async (comment: any) => {
  if (!currentUser.value) {
    ElMessage.warning('请先登录');
    return;
  }
  
  try {
    const res = await axios.post('http://localhost:8080/api/comment/dislike', {
      commentId: comment.id,
      username: currentUser.value
    });
    
    if (res.data.code === 200) {
      // 更新评论数据
      comment.likeCount = res.data.data.likeCount;
      comment.dislikeCount = res.data.data.dislikeCount;
      
      // 更新用户互动状态
      if (comment.dislikes && comment.dislikes.includes(currentUser.value)) {
        // 取消点踩
        comment.dislikes = comment.dislikes.filter((user: string) => user !== currentUser.value);
        ElMessage.success('取消点踩成功');
      } else {
        // 添加点踩
        if (!comment.dislikes) {
          comment.dislikes = [];
        }
        comment.dislikes.push(currentUser.value);
        // 取消点赞（如果有）
        if (comment.likes && comment.likes.includes(currentUser.value)) {
          comment.likes = comment.likes.filter((user: string) => user !== currentUser.value);
        }
        ElMessage.success('点踩成功');
      }
    } else {
      ElMessage.warning(res.data.msg);
    }
  } catch (error) {
    console.error('点踩失败:', error);
    ElMessage.error('点踩失败，请重试');
  }
};



// 显示用户资料弹窗
const showUserProfile = ref(false);
const selectedUser = ref({
  username: '',
  role: '',
  email: '',
  birthday: '',
  favorite: '',
  gender: '',
  region: '',
  signature: '',
  avatar: ''
});

// 显示头像预览弹窗
const showAvatarPreviewModal = ref(false);

// 打开用户资料弹窗
const goToUserProfile = async (username: string) => {
  // 加载用户资料
  await loadUserProfile(username);
  // 显示弹窗
  showUserProfile.value = true;
};

// 关闭用户资料弹窗
const closeUserProfile = () => {
  showUserProfile.value = false;
};

// 显示头像预览
const showAvatarPreview = () => {
  showAvatarPreviewModal.value = true;
};

// 关闭头像预览
const closeAvatarPreview = () => {
  showAvatarPreviewModal.value = false;
};

// 加载用户资料
const loadUserProfile = async (username: string) => {
  try {
    const res = await axios.post('http://localhost:8080/api/user/profile', {
      username: username
    });
    
    if (res.data && res.data.code === 200) {
      const userData = res.data.data;
      selectedUser.value = {
        username: userData.username,
        role: userData.role.toString(),
        email: userData.email || '',
        birthday: userData.birthday || '',
        favorite: userData.favorite || '',
        gender: userData.gender || '',
        region: userData.region || '',
        signature: userData.signature || '',
        avatar: userData.avatar || ''
      };
      
      // 如果是当前用户，使用localStorage中的最新头像
      if (username === currentUser.value) {
        const userAvatar = localStorage.getItem('avatar');
        if (userAvatar) {
          selectedUser.value.avatar = userAvatar;
        }
      }
    }
  } catch (error) {
    console.error('获取用户资料失败：', error);
  }
};

// 页面加载时获取用户信息和帖子数据
onMounted(async () => {
  const username = localStorage.getItem('username');
  if (username) {
    currentUser.value = username;
  }
  console.log('页面加载时的当前用户:', currentUser.value);
  
  // 检查用户是否为管理员
  checkAdmin();
  fetchUnreadCount();
  
  // 加载帖子数据
  await loadPosts();
  
  // 确保所有帖子的作者头像正确显示
  posts.value.forEach(post => {
    if (post.authorName === currentUser.value) {
      const userAvatar = localStorage.getItem('avatar');
      if (userAvatar) {
        console.log('设置头像:', userAvatar);
        post.authorAvatar = userAvatar;
      }
    }
  });
});
</script>

<style scoped>
.forum-container {
  min-height: 100vh;
  background: #f5f5f5;
}

/* 导航栏样式 */
.navbar {
  background-color: #333;
  color: white;
  padding: 10px 0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
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

.nav-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: #ff4757;
  color: white;
  border-radius: 9px;
  font-size: 11px;
  font-weight: bold;
  margin-left: 4px;
  vertical-align: top;
}

/* 搜索、发布帖子和排序选项 */
.search-post-sort-container {
  max-width: 1200px;
  margin: 20px auto;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.search-container {
  display: flex;
  gap: 10px;
  flex: 1;
  min-width: 300px;
  max-width: 400px;
}

.search-input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
  transition: border-color 0.3s;
}

.search-input:focus {
  outline: none;
  border-color: #ff6b6b;
  box-shadow: 0 0 0 2px rgba(255, 107, 107, 0.2);
}

.search-button {
  padding: 10px 20px;
  background: #ff6b6b;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.3s;
}

.search-button:hover {
  background: #ff5252;
}

.sort-options {
  display: flex;
  gap: 10px;
  background: white;
  padding: 10px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  flex: 1;
  min-width: 300px;
  justify-content: center;
}

.sort-option {
  padding: 8px 16px;
  cursor: pointer;
  border-radius: 20px;
  transition: all 0.3s;
  font-size: 14px;
}

.sort-option:hover {
  background: #f0f0f0;
}

.sort-option.active {
  background: #ff6b6b;
  color: white;
}

.sort-order-btn {
  padding: 8px 16px;
  background: #ff6b6b;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.sort-order-btn:hover {
  background: #ff5252;
}

.post-button {
  padding: 10px 20px;
  background: #ff6b6b;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.3s;
  white-space: nowrap;
}

.post-button:hover {
  background: #ff5252;
}

/* 帖子列表 */
.post-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  margin-bottom: 30px;
}

.post-item {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  transition: box-shadow 0.3s;
}

.post-item:hover {
  box-shadow: 0 5px 15px rgba(0,0,0,0.1);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.post-author {
  display: flex;
  align-items: center;
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 10px;
  object-fit: cover;
}

.author-info {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-size: 16px;
  font-weight: bold;
  margin: 0;
  color: #333;
}

.post-time {
  font-size: 12px;
  color: #999;
}

.post-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  padding: 5px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s;
}

.edit-btn:hover {
  border-color: #4CAF50;
  color: #4CAF50;
}

.delete-btn:hover {
  border-color: #f44336;
  color: #f44336;
}

.post-content {
  margin-bottom: 15px;
}

.post-title {
  font-size: 18px;
  font-weight: bold;
  margin: 0 0 10px 0;
  color: #333;
}

.post-text {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  margin: 0;
}

.post-footer {
  border-top: 1px solid #eee;
  padding-top: 15px;
}

.post-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #666;
}

.stat-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: all 0.3s;
}

.stat-btn:hover {
  color: #ff6b6b;
}

.stat-btn.active {
  color: #ff6b6b;
  font-weight: bold;
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
  color: #333;
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

/* 评论区样式 */
.comments-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

/* 评论控制栏 */
.comment-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.comment-sort {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sort-label {
  font-size: 14px;
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

.sort-order-btn.small {
  padding: 4px 8px;
  background: #ff6b6b;
  color: white;
  border: none;
  border-radius: 16px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s;
}

.sort-order-btn.small:hover {
  background: #ff5252;
}

/* 帖子分页导航 */
.pagination {
  max-width: 1200px;
  margin: 0 auto 30px;
  padding: 0 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.pagination .page-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.pagination .page-btn:hover:not(:disabled) {
  border-color: #ff6b6b;
  color: #ff6b6b;
}

.pagination .page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination .page-info {
  font-size: 14px;
  color: #666;
  min-width: 80px;
  text-align: center;
}

/* 评论分页 */
.comment-pagination {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-btn {
  padding: 4px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  font-size: 12px;
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

.page-info {
  font-size: 12px;
  color: #666;
  min-width: 60px;
  text-align: center;
}

.comments-list {
  margin-bottom: 20px;
}

.comment-item {
  padding: 15px;
  background: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 10px;
}

.comment-author {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.comment-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  margin-right: 10px;
  object-fit: cover;
}

.comment-info {
  display: flex;
  flex-direction: column;
}

.comment-author-name {
  font-size: 14px;
  font-weight: bold;
  margin: 0;
  color: #333;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-content {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 10px;
}

.comment-actions {
  display: flex;
  gap: 8px;
}

.action-btn.small, .reply-item .action-btn {
  padding: 4px 12px;
  font-size: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
}

.delete-btn.small:hover, .reply-item .delete-btn:hover {
  border-color: #f44336;
  color: #f44336;
}

.reply-btn.small:hover, .reply-item .reply-btn:hover {
  border-color: #4CAF50;
  color: #4CAF50;
}



.add-comment {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.comment-textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: vertical;
  min-height: 100px;
  font-size: 14px;
}

.comment-submit-btn {
  align-self: flex-end;
  padding: 8px 16px;
  background: #ff6b6b;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.comment-submit-btn:hover {
  background: #ff5252;
}

/* 回复表单样式 */
.reply-form {
  margin-top: 12px;
  padding: 12px;
  background: #f0f0f0;
  border-radius: 6px;
}

.reply-textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: vertical;
  min-height: 60px;
  font-size: 13px;
  box-sizing: border-box;
}

.reply-textarea:focus {
  outline: none;
  border-color: #ff6b6b;
  box-shadow: 0 0 0 2px rgba(255, 107, 107, 0.2);
}

.reply-form-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.reply-submit-btn {
  padding: 5px 14px;
  background: #ff6b6b;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.3s;
}

.reply-submit-btn:hover {
  background: #ff5252;
}

.reply-cancel-btn {
  padding: 5px 14px;
  background: #e0e0e0;
  color: #666;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.3s;
}

.reply-cancel-btn:hover {
  background: #d0d0d0;
}

/* 回复列表样式 */
.replies-section {
  margin-top: 12px;
  padding-left: 20px;
  border-left: 2px solid #ff6b6b;
}

.reply-item {
  padding: 10px;
  background: #f5f5f5;
  border-radius: 6px;
  margin-bottom: 8px;
}

.reply-item:last-child {
  margin-bottom: 0;
}

.reply-item .comment-author {
  margin-bottom: 6px;
}

.reply-item .comment-avatar {
  width: 24px;
  height: 24px;
}

.reply-item .comment-author-name {
  font-size: 13px;
}

.reply-item .comment-time {
  font-size: 11px;
}

.reply-item .comment-content {
  font-size: 13px;
  margin-bottom: 6px;
}

.reply-to {
  color: #ff6b6b;
  font-weight: 500;
  margin-right: 2px;
}

.reply-item .action-btn {
  padding: 3px 10px;
  font-size: 11px;
}

/* 回复按钮样式 */
.reply-btn.small:hover {
  border-color: #4CAF50;
  color: #4CAF50;
}

/* 用户资料弹窗样式 */
.user-profile-dialog {
  max-width: 500px;
}

.user-avatar-section {
  text-align: center;
  margin-bottom: 20px;
}

.user-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid #ff6b6b;
  cursor: pointer;
  transition: transform 0.3s;
}

.user-avatar:hover {
  transform: scale(1.05);
}

.avatar-hint {
  display: block;
  text-align: center;
  margin-top: 8px;
  font-size: 12px;
  color: #999;
  cursor: pointer;
}

/* 头像预览弹窗样式 */
.avatar-preview-container {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 5px 15px rgba(0,0,0,0.3);
}

.avatar-close-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(0,0,0,0.5);
  color: white;
  border: none;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 20px;
  z-index: 10;
}

.avatar-preview-img {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
  border-radius: 4px;
}

.user-profile-info {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.profile-item {
  display: flex;
  align-items: flex-start;
}

.profile-label {
  font-weight: bold;
  width: 100px;
  flex-shrink: 0;
  color: #333;
}

.profile-value {
  flex: 1;
  color: #666;
  word-break: break-word;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .navbar-container {
    padding: 0 15px;
  }
  
  .nav-links li {
    margin-left: 15px;
  }
  
  .post-button-container {
    padding: 0 15px;
  }
  
  .sort-container {
    padding: 0 15px;
  }
  
  .post-container {
    padding: 0 15px;
  }
  
  .post-item {
    padding: 15px;
  }
  
  .dialog-content {
    width: 95%;
  }
  
  .comment-item {
    padding: 10px;
  }
  
  .comment-textarea {
    min-height: 80px;
  }
  
  .profile-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .profile-label {
    margin-bottom: 5px;
  }
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