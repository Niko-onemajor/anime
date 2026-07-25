<template>
  <div class="profile-container">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="navbar-container">
        <div class="logo">Niko动漫</div>
        <ul class="nav-links">
          <li><a href="/index">首页</a></li>
          <li><a href="/category">分类</a></li>
          <li><a href="/forum">论坛</a></li>
          <li><a href="/messages">信息</a></li>
          <li><a href="/profile" class="active">个人中心</a></li>
          <li v-if="role === 'admin' || role === '1'"><a href="/admin/users">管理员后台</a></li>
        </ul>
      </div>
    </nav>

    <!-- 个人中心内容 -->
    <div class="container">
      <div class="profile-content">
        <!-- 侧边栏 -->
        <div class="sidebar">
          <h3>{{ isViewingOtherUser ? username + '的个人主页' : '个人中心' }}</h3>
          <ul class="sidebar-menu">
            <li><a href="#" :class="{ active: activeTab === 'profile' }" @click.prevent="switchTab('profile')">个人资料</a></li>
            <li v-if="!isViewingOtherUser"><a href="#" :class="{ active: activeTab === 'account-security' }" @click.prevent="switchTab('account-security')">修改密码</a></li>
            <li v-if="!isViewingOtherUser"><a href="#" :class="{ active: activeTab === 'watch-history' }" @click.prevent="switchTab('watch-history')">观看记录</a></li>
            <li v-if="!isViewingOtherUser"><a href="#" :class="{ active: activeTab === 'favorites' }" @click.prevent="switchTab('favorites')">我的收藏</a></li>
            <li v-if="!isViewingOtherUser"><a href="#" :class="{ active: activeTab === 'ratings' }" @click.prevent="switchTab('ratings')">我的评分</a></li>
            <li v-if="!isViewingOtherUser"><a href="#" @click.prevent="handleLogout">退出登录</a></li>
          </ul>
        </div>

        <!-- 主内容区域 -->
        <div class="main-content">
          <!-- 头像上传部分 -->
          <div class="avatar-section">
            <div class="avatar-container">
              <img :src="getImageUrl(avatarUrl)" alt="头像" class="avatar">
              <button v-if="!isViewingOtherUser && activeTab === 'profile'" class="avatar-upload-btn" @click="triggerFileInput">+</button>
              <input v-if="!isViewingOtherUser && activeTab === 'profile'" ref="fileInput" type="file" style="display: none;" accept="image/*" @change="handleAvatarUpload">
            </div>
            <h2>{{ username }}</h2>
            <p>{{ getRoleText(role) }}</p>
          </div>

          <!-- 个人资料 -->
          <div v-if="activeTab === 'profile'">
            <h3>个人资料</h3>
            <br>
            <div class="form-group">
              <label for="edit-username">用户名</label>
              <input type="text" id="edit-username" v-model="username" :disabled="isViewingOtherUser">
            </div>
            <div class="form-group">
              <label for="edit-gender">性别</label>
              <select id="edit-gender" v-model="gender" :disabled="isViewingOtherUser">
                <option value="">请选择</option>
                <option value="男">男</option>
                <option value="女">女</option>
                <option value="其他">其他</option>
              </select>
            </div>
            <div class="form-group">
              <label for="edit-birthday">生日</label>
              <input type="date" id="edit-birthday" v-model="birthday" :disabled="isViewingOtherUser">
            </div>
            <div class="form-group">
              <label for="edit-region">地区</label>
              <input type="text" id="edit-region" v-model="region" placeholder="请输入地区" :disabled="isViewingOtherUser">
            </div>
            <div class="form-group">
              <label for="edit-email">邮箱</label>
              <input type="email" id="edit-email" v-model="email" :disabled="isViewingOtherUser">
            </div>
            <div class="form-group">
              <label for="edit-favorite">喜爱的动漫</label>
              <input type="text" id="edit-favorite" v-model="favorite" :disabled="isViewingOtherUser">
            </div>
            <div class="form-group">
              <label for="edit-signature">签名</label>
              <textarea id="edit-signature" v-model="signature" placeholder="请输入个人签名" rows="3" :disabled="isViewingOtherUser"></textarea>
            </div>
            <button v-if="!isViewingOtherUser" class="btn btn-primary" @click="handleSave">保存修改</button>
          </div>

          <!-- 修改密码 -->
          <div v-if="activeTab === 'account-security' && !isViewingOtherUser">
            <h3>修改密码</h3>
            <br>
            <div class="form-group">
              <label for="current-password">当前密码</label>
              <div class="password-input">
                <input :type="showCurrentPassword ? 'text' : 'password'" id="current-password" v-model="currentPassword" placeholder="请输入当前密码">
                <button type="button" class="password-toggle" @click="togglePassword('current')">
                  {{ showCurrentPassword ? '隐藏' : '显示' }}
                </button>
              </div>
            </div>
            <div class="form-group">
              <label for="new-password">新密码</label>
              <div class="password-input">
                <input :type="showNewPassword ? 'text' : 'password'" id="new-password" v-model="newPassword" placeholder="请输入新密码">
                <button type="button" class="password-toggle" @click="togglePassword('new')">
                  {{ showNewPassword ? '隐藏' : '显示' }}
                </button>
              </div>
            </div>
            <div class="form-group">
              <label for="confirm-password">确认新密码</label>
              <div class="password-input">
                <input :type="showConfirmPassword ? 'text' : 'password'" id="confirm-password" v-model="confirmPassword" placeholder="请确认新密码">
                <button type="button" class="password-toggle" @click="togglePassword('confirm')">
                  {{ showConfirmPassword ? '隐藏' : '显示' }}
                </button>
              </div>
            </div>
            <button class="btn btn-primary" @click="handlePasswordSave">保存修改</button>
          </div>

          <!-- 观看记录 -->
          <div v-if="activeTab === 'watch-history' && !isViewingOtherUser">
            <h3>观看记录</h3>
            <!-- 排序控制 -->
            <div class="watch-history-controls">
              <div class="sort-controls">
                <span class="sort-label">排序方式：</span>
                <span 
                  class="sort-option" 
                  :class="{ active: sortBy === 'time' }"
                  @click="sortBy = 'time'"
                >
                  按时间
                </span>
                <button 
                  class="sort-order-btn"
                  @click="toggleSortOrder"
                >
                  {{ sortOrder === 'desc' ? '最新优先' : '最早优先' }}
                </button>
              </div>
            </div>
            <div v-if="loading" class="loading">加载中...</div>
            <div v-else-if="sortedWatchHistory.length === 0" class="no-history">暂无观看记录</div>
            <div v-else class="watch-history-list">
              <div v-for="(history, index) in sortedWatchHistory" :key="history.id" class="watch-history-item">
                <div class="history-info">
                  <img :src="getImageUrl(history.anime?.image)" :alt="history.anime?.title" class="history-anime-image">
                  <div class="history-details">
                    <h4>{{ history.anime?.title }}</h4>
                    <div class="history-meta">
                      <span class="history-meta-item">第{{ history.episode?.episodeNumber || '?' }}集</span>
                      <span class="history-meta-separator">|</span>
                      <span class="history-meta-item">最后观看：{{ formatTime(history.watchTime) }}</span>
                    </div>
                  </div>
                </div>
                <button class="btn btn-primary" @click="goToAnime(history.anime?.id, history.episode?.episodeNumber)">继续观看</button>
              </div>
            </div>
          </div>

          <!-- 我的收藏 -->
          <div v-if="activeTab === 'favorites' && !isViewingOtherUser">
            <h3>我的收藏</h3>
            <div v-if="favoritesLoading" class="loading">加载中...</div>
            <div v-else-if="favorites.length === 0" class="no-history">暂无收藏</div>
            <div v-else class="favorites-list">
              <div v-for="(anime, index) in favorites" :key="anime.id" class="favorite-item">
                <div class="favorite-info">
                  <img :src="getImageUrl(anime.image)" :alt="anime.title" class="favorite-anime-image">
                  <div class="favorite-details">
                    <h4>{{ anime.title }}</h4>
                    <p>{{ anime.genre }}</p>
                    <p>{{ anime.year }}</p>
                  </div>
                </div>
                <div class="favorite-actions">
                  <button class="btn btn-primary" @click="goToAnimeDetail(anime.id)">查看详情</button>
                  <button class="btn btn-danger" @click="removeFavorite(anime.id)">取消收藏</button>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 我的评分 -->
          <div v-if="activeTab === 'ratings' && !isViewingOtherUser">
            <h3>我的评分</h3>
            <div v-if="ratingsLoading" class="loading">加载中...</div>
            <div v-else-if="ratings.length === 0" class="no-history">暂无评分</div>
            <div v-else class="ratings-list">
              <div v-for="(rating, index) in ratings" :key="rating.id" class="rating-item">
                <div class="rating-info">
                  <img :src="getImageUrl(rating.anime?.image)" :alt="rating.anime?.title" class="rating-anime-image">
                  <div class="rating-details">
                    <h4>{{ rating.anime?.title }}</h4>
                    <p>{{ rating.anime?.genre }}</p>
                    <p>{{ rating.anime?.year }}</p>
                    <div class="rating-score">
            <span class="rating-label">我的评分：</span>
            <span class="rating-value">{{ rating.rating }}分</span>
          </div>
                  </div>
                </div>
                <div class="rating-actions">
                  <button class="btn btn-primary" @click="goToAnimeDetail(rating.anime?.id)">查看详情</button>
                </div>
              </div>
            </div>
          </div>
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
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import axios from 'axios';
import { ElMessage, ElMessageBox } from 'element-plus';

const router = useRouter();
const route = useRoute();
// 获取URL参数中的用户名，如果没有则使用当前登录用户
const urlUsername = route.query.username as string;
const isViewingOtherUser = !!urlUsername;
const username = ref(urlUsername || localStorage.getItem('username') || sessionStorage.getItem('username') || 'user1');
// 先检查localStorage，再检查sessionStorage
let userRole = localStorage.getItem('role');
if (!userRole) {
  userRole = sessionStorage.getItem('role');
}
const role = ref(userRole || '0');
const email = ref(localStorage.getItem('email') || 'user1@example.com');
const birthday = ref(localStorage.getItem('birthday') || '2000-01-01');
const favorite = ref(localStorage.getItem('favorite') || '鬼灭之刃, 进击的巨人, 海贼王');
const gender = ref(localStorage.getItem('gender') || '');
const region = ref(localStorage.getItem('region') || '');
const signature = ref(localStorage.getItem('signature') || '');
const avatarUrl = ref('/avatars/avatar1.jpg');
const fileInput = ref<HTMLInputElement | null>(null);

// 密码修改相关
const currentPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const showCurrentPassword = ref(false);
const showNewPassword = ref(false);
const showConfirmPassword = ref(false);

// 标签切换
const activeTab = ref('profile');
const watchHistory = ref<any[]>([]);
const loading = ref(false);
const favorites = ref<any[]>([]);
const favoritesLoading = ref(false);
const ratings = ref<any[]>([]);
const ratingsLoading = ref(false);

// 排序相关
const sortBy = ref('time');
const sortOrder = ref('desc'); // desc: 最新优先, asc: 最早优先

const getRoleText = (role: string) => {
  return role === 'admin' || role === '1' ? '管理员' : '普通用户';
};

// 切换标签
const switchTab = (tab: string) => {
  activeTab.value = tab;
  if (tab === 'watch-history' && !isViewingOtherUser) {
    loadWatchHistory();
  }
  if (tab === 'favorites' && !isViewingOtherUser) {
    loadFavorites();
  }
  if (tab === 'ratings' && !isViewingOtherUser) {
    loadRatings();
  }
};

// 加载观看记录
const loadWatchHistory = async () => {
  loading.value = true;
  try {
    const res = await axios.post('http://localhost:8080/api/watch-history/list', {
      username: username.value
    });
    if (res.data.code === 200) {
      watchHistory.value = res.data.data;
    }
  } catch (error) {
    console.error('加载观看记录失败:', error);
    ElMessage.error('加载观看记录失败');
  } finally {
    loading.value = false;
  }
};

// 处理图片URL
const getImageUrl = (url: string): string => {
  if (!url) return '/avatars/avatar1.jpg';
  // 如果是完整的URL，直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  // 如果是本地文件路径，转换为HTTP URL
  if (url.startsWith('file:///')) {
    // 提取文件名
    const filename = url.split('/').pop();
    // 判断是头像还是封面
    if (url.includes('avatars')) {
      return `/avatars/${filename}`;
    } else {
      return `/covers/${filename}`;
    }
  }
  // 如果是本地文件路径（Windows或Unix风格），转换为HTTP URL
  if (url.includes(':')) {
    // 提取文件名，同时处理正斜杠和反斜杠
    const parts = url.split(/[\\/]/);
    const filename = parts.pop();
    // 判断是头像还是封面
    if (url.includes('avatars')) {
      return `/avatars/${filename}`;
    } else if (url.includes('covers')) {
      return `/covers/${filename}`;
    } else {
      return `/avatars/${filename}`;
    }
  }
  // 如果是本地路径，使用默认头像
  if (url.startsWith('/src/')) {
    return '/avatars/avatar1.jpg';
  }
  // 如果是相对路径，确保返回正确的绝对路径
  if (url.startsWith('covers/')) {
    return `/${url}`;
  }
  if (url.startsWith('avatars/')) {
    return `/${url}`;
  }
  // 默认返回原路径，因为后端应该已经返回了完整的URL
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

// 跳转到动漫播放页
const goToAnime = (animeId: number, episodeNumber: number) => {
  if (animeId && episodeNumber) {
    // 设置来源页面为个人中心
    localStorage.setItem('fromPage', '/profile');
    router.push(`/anime/${animeId}/play/${episodeNumber}`);
  }
};

// 跳转到动漫详情页
const goToAnimeDetail = (animeId: number) => {
  if (animeId) {
    // 设置来源页面为个人中心
    localStorage.setItem('fromPage', '/profile');
    router.push(`/anime/${animeId}`);
  }
};

// 加载收藏列表
const loadFavorites = async () => {
  favoritesLoading.value = true;
  try {
    const res = await axios.post('http://localhost:8080/api/favorites/list', {
      username: username.value
    });
    if (res.data.code === 200) {
      favorites.value = res.data.data;
    }
  } catch (error) {
    console.error('加载收藏列表失败:', error);
    ElMessage.error('加载收藏列表失败');
  } finally {
    favoritesLoading.value = false;
  }
};

// 取消收藏
const removeFavorite = async (animeId: number) => {
  ElMessageBox.confirm('确定要取消收藏吗？', '取消收藏', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await axios.post('http://localhost:8080/api/favorites/remove', {
        username: username.value,
        animeId: animeId
      });
      if (res.data.code === 200) {
        ElMessage.success('取消收藏成功');
        // 重新加载收藏列表
        loadFavorites();
      } else {
        ElMessage.error(res.data.msg || '取消收藏失败');
      }
    } catch (error) {
      console.error('取消收藏失败:', error);
      ElMessage.error('取消收藏失败');
    }
  }).catch(() => {
    // 取消操作
  });
};

// 加载评分列表
const loadRatings = async () => {
  ratingsLoading.value = true;
  try {
    const res = await axios.post('http://localhost:8080/api/anime/rating/user/list', {
      username: username.value
    });
    if (res.data.code === 200) {
      ratings.value = res.data.data;
    }
  } catch (error) {
    console.error('加载评分列表失败:', error);
    ElMessage.error('加载评分列表失败');
  } finally {
    ratingsLoading.value = false;
  }
};

// 切换排序顺序
const toggleSortOrder = () => {
  sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc';
};

// 切换密码显示/隐藏
const togglePassword = (type: string) => {
  switch (type) {
    case 'current':
      showCurrentPassword.value = !showCurrentPassword.value;
      break;
    case 'new':
      showNewPassword.value = !showNewPassword.value;
      break;
    case 'confirm':
      showConfirmPassword.value = !showConfirmPassword.value;
      break;
  }
};

// 排序后的观看记录
const sortedWatchHistory = computed(() => {
  return [...watchHistory.value].sort((a, b) => {
    if (sortBy.value === 'time') {
      const timeA = new Date(a.watchTime).getTime();
      const timeB = new Date(b.watchTime).getTime();
      return sortOrder.value === 'desc' ? timeB - timeA : timeA - timeB;
    }
    return 0;
  });
});

const handleSave = async () => {
  ElMessageBox.confirm('确定要保存个人资料修改吗？', '保存修改', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    try {
      const oldUsername = localStorage.getItem('username') || 'user1';
      
      // 保存基本信息
      const res = await axios.post('http://localhost:8080/api/user/update', {
        oldUsername: oldUsername,
        username: username.value,
        email: email.value,
        birthday: birthday.value,
        favorite: favorite.value,
        gender: gender.value,
        region: region.value,
        signature: signature.value
      });
      
      if (res.data.code === 200) {
        localStorage.setItem('username', username.value);
        localStorage.setItem('email', email.value);
        localStorage.setItem('birthday', birthday.value);
        localStorage.setItem('favorite', favorite.value);
        localStorage.setItem('gender', gender.value);
        localStorage.setItem('region', region.value);
        localStorage.setItem('signature', signature.value);
        ElMessage.success('保存成功！');
      } else {
        // 保存失败，恢复用户名到原来的状态
        username.value = oldUsername;
        ElMessage.error(res.data.msg || '保存失败');
      }
    } catch (error) {
      // 保存出错，恢复用户名到原来的状态
      const oldUsername = localStorage.getItem('username') || 'user1';
      username.value = oldUsername;
      console.error('保存失败：', error);
      ElMessage.error('保存出错，请检查网络或重试: ' + (error as Error).message);
    }
  }).catch(() => {
    // 取消保存
  });
};

const handlePasswordSave = async () => {
  // 验证密码相关字段
  if (!currentPassword.value) {
    ElMessage.error('请输入当前密码');
    return;
  }
  if (!newPassword.value) {
    ElMessage.error('请输入新密码');
    return;
  }
  if (newPassword.value !== confirmPassword.value) {
    ElMessage.error('新密码和确认密码不一致');
    return;
  }
  
  // 验证新密码格式
  if (newPassword.value.length < 6 || newPassword.value.length > 20 || newPassword.value.includes(' ')) {
    ElMessage.error('新密码长度必须在6-20个字符之间，且不能包含空格');
    return;
  }
  
  ElMessageBox.confirm('确定要修改密码吗？', '修改密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const oldUsername = localStorage.getItem('username') || 'user1';
      
      // 修改密码
      console.log('开始修改密码，参数:', {
        username: oldUsername,
        oldPassword: currentPassword.value,
        newPassword: newPassword.value
      });
      
      try {
        console.log('发送密码修改请求到: http://localhost:8080/api/user/change-password');
        const passwordRes = await axios.post('http://localhost:8080/api/user/change-password', {
          username: oldUsername,
          oldPassword: currentPassword.value,
          newPassword: newPassword.value
        });
        
        console.log('密码修改响应:', passwordRes);
        console.log('密码修改响应数据:', passwordRes.data);
        
        if (passwordRes.data.code === 200) {
          // 密码修改成功，清空密码字段
          currentPassword.value = '';
          newPassword.value = '';
          confirmPassword.value = '';
          ElMessage.success('保存成功，密码已修改！');
        } else {
          console.log('密码修改失败:', passwordRes.data.msg);
          ElMessage.error(passwordRes.data.msg || '密码修改失败');
        }
      } catch (error: any) {
        console.error('密码修改请求失败:', error);
        console.error('错误详情:', error.response);
        ElMessage.error('密码修改请求失败，请检查网络连接');
      }
    } catch (error) {
      console.error('保存失败：', error);
      ElMessage.error('保存出错，请检查网络或重试: ' + (error as Error).message);
    }
  }).catch(() => {
    // 取消修改密码
  });
};

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    localStorage.removeItem('email');
    localStorage.removeItem('birthday');
    localStorage.removeItem('favorite');
    localStorage.removeItem('avatar');
    router.push('/auth');
  }).catch(() => {
    // 取消退出登录
  });
};

// 触发文件输入
const triggerFileInput = () => {
  fileInput.value?.click();
};

const handleAvatarUpload = async (e: Event) => {
  const target = e.target as HTMLInputElement;
  if (target.files && target.files[0]) {
    const file = target.files[0];
    const formData = new FormData();
    formData.append('file', file);
    formData.append('username', username.value);
    
    try {
      const res = await axios.post('http://localhost:8080/api/user/avatar', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      
      if (res.data.code === 200) {
        const avatar = res.data.data.avatar;
        localStorage.setItem('avatar', avatar);
        avatarUrl.value = avatar;
        ElMessage.success('头像上传成功！');
      } else {
        ElMessage.error(res.data.msg || '头像上传失败');
      }
    } catch (error) {
      console.error('头像上传失败：', error);
      ElMessage.error('头像上传出错，请检查网络或重试: ' + (error as Error).message);
    }
  }
};

onMounted(async () => {
  // 立即设置默认头像
  const avatar = localStorage.getItem('avatar');
  if (avatar) {
    // 直接使用存储的头像URL，因为后端应该已经返回了完整的阿里云URL
    avatarUrl.value = avatar;
  }
  
  // 从后端获取用户资料
  try {
    const res = await axios.post('http://localhost:8080/api/user/profile', {
      username: username.value
    });
    
    if (res.data && res.data.code === 200) {
            const userData = res.data.data;
            username.value = userData.username;
            // 确保 role 的值正确设置
            role.value = userData.role.toString();
            // 同时更新 localStorage 和 sessionStorage 中的 role
            localStorage.setItem('role', role.value);
            sessionStorage.setItem('role', role.value);
            email.value = userData.email || '';
            // 转换生日格式为YYYY-MM-DD
            if (userData.birthday) {
              const date = new Date(userData.birthday);
              const formattedDate = date.toISOString().split('T')[0];
              birthday.value = formattedDate || '';
            } else {
              birthday.value = '';
            }
            favorite.value = userData.favorite || '';
            gender.value = userData.gender || '';
            region.value = userData.region || '';
            signature.value = userData.signature || '';
            
            if (userData.avatar) {
                // 直接使用后端返回的头像URL，因为后端应该已经返回了完整的阿里云URL
                avatarUrl.value = userData.avatar;
                localStorage.setItem('avatar', userData.avatar);
                sessionStorage.setItem('avatar', userData.avatar);
            }
            
            localStorage.setItem('username', userData.username);
            sessionStorage.setItem('username', userData.username);
            localStorage.setItem('email', userData.email || '');
            sessionStorage.setItem('email', userData.email || '');
            // 存储生日为YYYY-MM-DD格式
            if (userData.birthday) {
              const date = new Date(userData.birthday);
              const formattedDate = date.toISOString().split('T')[0];
              localStorage.setItem('birthday', formattedDate || '');
              sessionStorage.setItem('birthday', formattedDate || '');
            } else {
              localStorage.setItem('birthday', '');
              sessionStorage.setItem('birthday', '');
            }
            localStorage.setItem('favorite', userData.favorite || '');
            sessionStorage.setItem('favorite', userData.favorite || '');
            localStorage.setItem('gender', userData.gender || '');
            sessionStorage.setItem('gender', userData.gender || '');
            localStorage.setItem('region', userData.region || '');
            sessionStorage.setItem('region', userData.region || '');
            localStorage.setItem('signature', userData.signature || '');
            sessionStorage.setItem('signature', userData.signature || '');
        }
  } catch (error) {
    console.error('获取用户资料失败：', error);
  }
  

});
</script>

<style scoped>
.profile-container {
  min-height: 100vh;
  background: #f5f5f5;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
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

/* 个人中心内容样式 */
.profile-content {
  display: flex;
  margin-top: 30px;
  gap: 30px;
}

/* 侧边栏样式 */
.sidebar {
  width: 250px;
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.sidebar h3 {
  margin-bottom: 20px;
  color: #333;
}

.sidebar-menu {
  list-style: none;
}

.sidebar-menu li {
  margin-bottom: 10px;
}

.sidebar-menu a {
  display: block;
  padding: 10px;
  color: #333;
  text-decoration: none;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.sidebar-menu a:hover {
  background-color: #f0f0f0;
}

.sidebar-menu a.active {
  background-color: #ff6b6b;
  color: white;
}

/* 主内容区域样式 */
.main-content {
  flex: 1;
  background-color: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

/* 头像上传样式 */
.avatar-section {
  text-align: center;
  margin-bottom: 40px;
}

.avatar-container {
  position: relative;
  display: inline-block;
  margin-bottom: 20px;
}

.avatar {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid #ff6b6b;
}

.avatar-upload-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  background-color: #ff6b6b;
  color: white;
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.avatar-upload-btn:hover {
  background-color: #ff5252;
}

/* 表单样式 */
.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
}

.form-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
}

/* 密码输入框样式 */
.password-input {
  position: relative;
  display: flex;
  align-items: center;
}

.password-input input {
  flex: 1;
  padding-right: 100px;
}

.password-toggle {
  position: absolute;
  right: 10px;
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  padding: 5px 10px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.password-toggle:hover {
  background-color: #f0f0f0;
}

/* 按钮样式 */
.btn {
  display: inline-block;
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s;
}

.btn-primary {
  background-color: #ff6b6b;
  color: white;
}

.btn-primary:hover {
  background-color: #ff5252;
}

/* 观看记录样式 */
.watch-history-controls {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.sort-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sort-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.sort-option {
  padding: 6px 12px;
  border-radius: 16px;
  background: #f0f0f0;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.sort-option:hover {
  background: #e0e0e0;
}

.sort-option.active {
  background: #ff6b6b;
  color: white;
}

.sort-order-btn {
  padding: 6px 12px;
  background: #ff6b6b;
  color: white;
  border: none;
  border-radius: 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s;
}

.sort-order-btn:hover {
  background: #ff5252;
}

.loading {
  text-align: center;
  padding: 20px;
  color: #666;
}

.no-history {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 16px;
}

.watch-history-list {
  margin-top: 20px;
  max-height: 500px;
  overflow-y: auto;
}

.watch-history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #eee;
  transition: background-color 0.3s;
}

.watch-history-item:hover {
  background-color: #f9f9f9;
}

.history-info {
  display: flex;
  align-items: center;
  gap: 15px;
  flex: 1;
}

.history-anime-image {
  width: 80px;
  height: 120px;
  object-fit: cover;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.history-details {
  flex: 1;
}

.history-details h4 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #333;
}

.history-details p {
  margin: 5px 0;
  color: #666;
  font-size: 14px;
}

.history-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  font-size: 14px;
  color: #666;
}

.history-meta-item {
  display: flex;
  align-items: center;
}

.history-meta-separator {
  color: #ccc;
  margin: 0 5px;
}

/* 收藏列表样式 */
.favorites-list {
  margin-top: 20px;
  max-height: 500px;
  overflow-y: auto;
}

.favorite-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #eee;
  transition: background-color 0.3s;
}

.favorite-item:hover {
  background-color: #f9f9f9;
}

.favorite-info {
  display: flex;
  align-items: center;
  gap: 15px;
  flex: 1;
}

.favorite-anime-image {
  width: 80px;
  height: 120px;
  object-fit: cover;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.favorite-details {
  flex: 1;
}

.favorite-details h4 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #333;
}

.favorite-details p {
  margin: 5px 0;
  color: #666;
  font-size: 14px;
}

.favorite-actions {
  display: flex;
  gap: 10px;
}

.btn-danger {
  background-color: #3498db;
  color: white;
}

.btn-danger:hover {
  background-color: #2980b9;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .profile-content {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .watch-history-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .favorite-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .history-info {
    width: 100%;
  }

  .favorite-info {
    width: 100%;
  }

  .history-anime-image {
    width: 60px;
    height: 90px;
  }

  .favorite-anime-image {
    width: 60px;
    height: 90px;
  }

  .favorite-actions {
    width: 100%;
    justify-content: flex-start;
  }
}

/* 评分列表样式 */
.ratings-list {
  margin-top: 20px;
  max-height: 500px;
  overflow-y: auto;
}

.rating-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #eee;
  transition: background-color 0.3s;
}

.rating-item:hover {
  background-color: #f9f9f9;
}

.rating-info {
  display: flex;
  align-items: center;
  gap: 15px;
  flex: 1;
}

.rating-anime-image {
  width: 80px;
  height: 120px;
  object-fit: cover;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.rating-details {
  flex: 1;
}

.rating-details h4 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #333;
}

.rating-details p {
  margin: 5px 0;
  color: #666;
  font-size: 14px;
}

.rating-stars {
  margin-top: 10px;
}

.rating-label {
  font-size: 14px;
  color: #666;
  margin-right: 10px;
}

.rating-value {
  font-size: 16px;
  font-weight: bold;
  color: #ff6b6b;
}

.rating-actions {
  display: flex;
  gap: 10px;
}

@media (max-width: 768px) {
  .rating-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .rating-info {
    width: 100%;
  }

  .rating-anime-image {
    width: 60px;
    height: 90px;
  }

  .rating-actions {
    width: 100%;
    justify-content: flex-start;
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