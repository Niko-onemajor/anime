<template>
  <div class="user-home-container">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="navbar-container">
        <div class="logo">Niko动漫</div>
        <ul class="nav-links">
          <li><a href="/index">首页</a></li>
          <li><a href="/category">分类</a></li>
          <li><a href="/forum">论坛</a></li>
          <li><a href="/messages">消息<span v-if="navUnreadCount > 0" class="nav-badge">{{ navUnreadCount }}</span></a></li>
          <li><a href="/profile">个人中心</a></li>
          <li v-if="isAdmin"><a href="/admin/users">管理员后台</a></li>
        </ul>
      </div>
    </nav>

    <div class="container">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <template v-else>
        <!-- 用户头部信息 -->
        <div class="user-header">
          <div class="user-header-bg"></div>
          <div class="user-header-content">
            <div class="user-avatar-wrap">
              <img :src="getImageUrl(userInfo.avatar)" :alt="userInfo.username" class="user-avatar">
            </div>
            <div class="user-info-main">
              <h2 class="user-name">{{ userInfo.username }}</h2>
              <span class="user-role" :class="{ admin: userInfo.role === '1' }">{{ userInfo.role === '1' ? '管理员' : '普通用户' }}</span>
              <p class="user-signature">{{ userInfo.signature || '这个人很懒，什么都没写' }}</p>
            </div>
            <div class="user-actions" v-if="!isSelf">
              <button @click="toggleFollow" class="btn follow-btn" :class="{ following: isFollowing }">
                {{ isFollowing ? '已关注' : '+ 关注' }}
              </button>
              <button @click="goToDM" class="btn dm-btn">私信</button>
            </div>
          </div>
          <div class="user-stats">
            <div class="stat-item" @click="activeTab = 'following'">
              <span class="stat-num">{{ followingCount }}</span>
              <span class="stat-label">关注</span>
            </div>
            <div class="stat-item" @click="activeTab = 'followers'">
              <span class="stat-num">{{ followerCount }}</span>
              <span class="stat-label">粉丝</span>
            </div>
            <div class="stat-item" @click="activeTab = 'posts'">
              <span class="stat-num">{{ postsCount }}</span>
              <span class="stat-label">帖子</span>
            </div>
            <div class="stat-item" @click="activeTab = 'favorites'">
              <span class="stat-num">{{ favoritesCount }}</span>
              <span class="stat-label">收藏</span>
            </div>
          </div>
        </div>

        <!-- 标签导航 -->
        <div class="tab-nav">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="tab-btn"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>

        <!-- 内容区域 -->
        <div class="tab-content">
          <!-- 个人资料 -->
          <div v-if="activeTab === 'profile'" class="info-grid">
            <div class="info-card">
              <div class="info-label">用户名</div>
              <div class="info-value">{{ userInfo.username }}</div>
            </div>
            <div class="info-card">
              <div class="info-label">角色</div>
              <div class="info-value">{{ userInfo.role === '1' ? '管理员' : '普通用户' }}</div>
            </div>
            <div class="info-card">
              <div class="info-label">性别</div>
              <div class="info-value">{{ userInfo.gender || '未设置' }}</div>
            </div>
            <div class="info-card">
              <div class="info-label">生日</div>
              <div class="info-value">{{ userInfo.birthday || '未设置' }}</div>
            </div>
            <div class="info-card">
              <div class="info-label">地区</div>
              <div class="info-value">{{ userInfo.region || '未设置' }}</div>
            </div>
            <div class="info-card">
              <div class="info-label">邮箱</div>
              <div class="info-value">{{ userInfo.email || '未设置' }}</div>
            </div>
            <div class="info-card">
              <div class="info-label">喜爱的动漫</div>
              <div class="info-value">{{ userInfo.favorite || '未设置' }}</div>
            </div>
            <div class="info-card full-width">
              <div class="info-label">签名</div>
              <div class="info-value">{{ userInfo.signature || '未设置' }}</div>
            </div>
          </div>

          <!-- 观看记录 -->
          <div v-if="activeTab === 'watchHistory'">
            <div v-if="watchHistoryLoading" class="loading">加载中...</div>
            <div v-else-if="watchHistory.length === 0" class="empty">暂无观看记录</div>
            <div v-else class="card-list">
              <div v-for="item in watchHistory" :key="item.id" class="card-item" @click="goToAnime(item.anime?.id, item.episode?.episodeNumber)">
                <img :src="getImageUrl(item.anime?.image)" :alt="item.anime?.title" class="card-img">
                <div class="card-info">
                  <h4>{{ item.anime?.title }}</h4>
                  <p>第{{ item.episode?.episodeNumber || '?' }}集 · {{ formatTime(item.watchTime) }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 收藏 -->
          <div v-if="activeTab === 'favorites'">
            <div v-if="favoritesLoading" class="loading">加载中...</div>
            <div v-else-if="favorites.length === 0" class="empty">暂无收藏</div>
            <div v-else class="card-list">
              <div v-for="anime in favorites" :key="anime.id" class="card-item" @click="goToAnimeDetail(anime.id)">
                <img :src="getImageUrl(anime.image)" :alt="anime.title" class="card-img">
                <div class="card-info">
                  <h4>{{ anime.title }}</h4>
                  <p>{{ anime.genre }} · {{ anime.year }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 评分 -->
          <div v-if="activeTab === 'ratings'">
            <div v-if="ratingsLoading" class="loading">加载中...</div>
            <div v-else-if="ratings.length === 0" class="empty">暂无评分</div>
            <div v-else class="card-list">
              <div v-for="item in ratings" :key="item.id" class="card-item" @click="goToAnimeDetail(item.anime?.id)">
                <img :src="getImageUrl(item.anime?.image)" :alt="item.anime?.title" class="card-img">
                <div class="card-info">
                  <h4>{{ item.anime?.title }}</h4>
                  <p>{{ item.anime?.genre }} · {{ item.anime?.year }}</p>
                  <span class="rating-badge">{{ item.rating }}分</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 帖子 -->
          <div v-if="activeTab === 'posts'">
            <div v-if="postsLoading" class="loading">加载中...</div>
            <div v-else-if="posts.length === 0" class="empty">暂无发布的帖子</div>
            <div v-else>
              <div class="post-list">
                <div v-for="post in pagedPosts" :key="post.id" class="post-item">
                  <h4 class="post-title">{{ post.title }}</h4>
                  <p class="post-content">{{ post.content }}</p>
                  <div class="post-meta">
                    <span>{{ formatTime(post.createTime) }}</span>
                    <span>👍 {{ post.likeCount || 0 }}</span>
                    <span>💬 {{ post.commentCount || 0 }}</span>
                  </div>
                </div>
              </div>
              <div class="pagination" v-if="postsTotalPages > 1">
                <button @click="postsPage = 1" :disabled="postsPage === 1" class="page-btn">首页</button>
                <button @click="postsPage = postsPage - 1" :disabled="postsPage === 1" class="page-btn">上一页</button>
                <span class="page-info">{{ postsPage }} / {{ postsTotalPages }}</span>
                <button @click="postsPage = postsPage + 1" :disabled="postsPage === postsTotalPages" class="page-btn">下一页</button>
                <button @click="postsPage = postsTotalPages" :disabled="postsPage === postsTotalPages" class="page-btn">尾页</button>
              </div>
            </div>
          </div>

          <!-- 关注 -->
          <div v-if="activeTab === 'following'">
            <div v-if="followingLoading" class="loading">加载中...</div>
            <div v-else-if="followingList.length === 0" class="empty">暂无关注</div>
            <div v-else class="follow-list">
              <div v-for="user in followingList" :key="user.id" class="follow-item" @click="goToUserHome(user.username)">
                <img :src="getImageUrl(user.avatar)" :alt="user.username" class="follow-avatar">
                <div class="follow-details">
                  <h4>{{ user.username }}</h4>
                  <p>{{ user.signature || '这个人很懒，什么都没写' }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 粉丝 -->
          <div v-if="activeTab === 'followers'">
            <div v-if="followersLoading" class="loading">加载中...</div>
            <div v-else-if="followerList.length === 0" class="empty">暂无粉丝</div>
            <div v-else class="follow-list">
              <div v-for="user in followerList" :key="user.id" class="follow-item" @click="goToUserHome(user.username)">
                <img :src="getImageUrl(user.avatar)" :alt="user.username" class="follow-avatar">
                <div class="follow-details">
                  <h4>{{ user.username }}</h4>
                  <p>{{ user.signature || '这个人很懒，什么都没写' }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <footer class="footer">
      <div class="footer-content">
        <p>© 2026 Niko动漫. 保留所有权利.</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import api from '@/utils/api';

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const isAdmin = ref(false);
const navUnreadCount = ref(0);
const activeTab = ref('profile');

// 用户信息
const userInfo = ref({
  id: 0,
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

// 当前登录用户
const currentUser = ref(localStorage.getItem('username') || '');
const currentUserId = ref(0);
const isSelf = computed(() => currentUser.value === userInfo.value.username);

// 关注状态
const isFollowing = ref(false);
const followingCount = ref(0);
const followerCount = ref(0);
const followingList = ref<any[]>([]);
const followerList = ref<any[]>([]);
const followingLoading = ref(false);
const followersLoading = ref(false);

// 观看记录
const watchHistory = ref<any[]>([]);
const watchHistoryLoading = ref(false);

// 收藏
const favorites = ref<any[]>([]);
const favoritesCount = ref(0);
const favoritesLoading = ref(false);

// 评分
const ratings = ref<any[]>([]);
const ratingsLoading = ref(false);

// 帖子
const posts = ref<any[]>([]);
const postsCount = ref(0);
const postsLoading = ref(false);
const postsPage = ref(1);
const postsPageSize = 6;
const postsTotalPages = computed(() => Math.ceil(posts.value.length / postsPageSize) || 1);
const pagedPosts = computed(() => {
  const start = (postsPage.value - 1) * postsPageSize;
  return posts.value.slice(start, start + postsPageSize);
});

const tabs = [
  { key: 'profile', label: '个人资料' },
  { key: 'watchHistory', label: '观看记录' },
  { key: 'favorites', label: '收藏' },
  { key: 'ratings', label: '评分' },
  { key: 'posts', label: '帖子' },
  { key: 'following', label: '关注' },
  { key: 'followers', label: '粉丝' }
];

// 图片处理
const getImageUrl = (url: string | undefined | null): string => {
  if (!url) return '/avatars/avatar1.jpg';
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  if (url.includes(':')) {
    const parts = url.split(/[\\/]/);
    const filename = parts.pop();
    if (url.includes('avatars')) return `/avatars/${filename}`;
    return `/covers/${filename}`;
  }
  return url;
};

const formatTime = (time: string | Date) => {
  if (!time) return '';
  const date = typeof time === 'string' ? new Date(time) : time;
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${d} ${h}:${min}`;
};

const checkAdmin = () => {
  let role = localStorage.getItem('role');
  if (!role) role = sessionStorage.getItem('role');
  isAdmin.value = role === 'admin' || role === '1';
};

const fetchUnreadCount = async () => {
  const username = localStorage.getItem('username') || sessionStorage.getItem('username');
  if (!username) return;
  try {
    const res = await api.get('/api/notifications/unread-count', { params: { username } });
    if (res.data.code === 200) navUnreadCount.value = res.data.unreadCount || 0;
  } catch (e) { /* ignore */ }
};

// 加载用户资料
const loadUserProfile = async (username: string) => {
  try {
    const res = await axios.post('http://localhost:8080/api/user/profile', { username });
    if (res.data && res.data.code === 200) {
      const data = res.data.data;
      userInfo.value = {
        id: data.id || 0,
        username: data.username,
        role: (data.role || '').toString(),
        email: data.email || '',
        birthday: data.birthday || '',
        favorite: data.favorite || '',
        gender: data.gender || '',
        region: data.region || '',
        signature: data.signature || '',
        avatar: data.avatar || ''
      };
    }
  } catch (e) {
    console.error('加载用户资料失败:', e);
  }
};

// 加载关注状态
const loadFollowStatus = async () => {
  if (!userInfo.value.id) return;
  try {
    // 粉丝数
    const fanRes = await axios.get('http://localhost:8080/api/follow/follower-count', {
      params: { userId: userInfo.value.id }
    });
    if (fanRes.data?.code === 200) followerCount.value = fanRes.data.data.count || 0;

    // 关注数
    const followRes = await axios.get('http://localhost:8080/api/follow/following-count', {
      params: { userId: userInfo.value.id }
    });
    if (followRes.data?.code === 200) followingCount.value = followRes.data.data.count || 0;

    // 当前用户是否关注了此用户
    if (currentUserId.value && currentUserId.value !== userInfo.value.id) {
      const statusRes = await axios.get('http://localhost:8080/api/follow/status', {
        params: { followerId: currentUserId.value, followedId: userInfo.value.id }
      });
      if (statusRes.data?.code === 200) isFollowing.value = statusRes.data.data.following || false;
    }
  } catch (e) {
    console.error('加载关注状态失败:', e);
  }
};

// 加载关注列表
const loadFollowingList = async () => {
  followingLoading.value = true;
  try {
    const res = await axios.get('http://localhost:8080/api/follow/following-list', {
      params: { userId: userInfo.value.id }
    });
    if (res.data?.code === 200) followingList.value = res.data.data || [];
  } catch (e) {
    console.error('加载关注列表失败:', e);
  } finally {
    followingLoading.value = false;
  }
};

// 加载粉丝列表
const loadFollowerList = async () => {
  followersLoading.value = true;
  try {
    const res = await axios.get('http://localhost:8080/api/follow/follower-list', {
      params: { userId: userInfo.value.id }
    });
    if (res.data?.code === 200) followerList.value = res.data.data || [];
  } catch (e) {
    console.error('加载粉丝列表失败:', e);
  } finally {
    followersLoading.value = false;
  }
};

// 加载观看记录
const loadWatchHistory = async () => {
  watchHistoryLoading.value = true;
  try {
    const res = await axios.post('http://localhost:8080/api/watch-history/list', {
      username: userInfo.value.username
    });
    if (res.data?.code === 200) watchHistory.value = res.data.data || [];
  } catch (e) {
    console.error('加载观看记录失败:', e);
  } finally {
    watchHistoryLoading.value = false;
  }
};

// 加载收藏
const loadFavorites = async () => {
  favoritesLoading.value = true;
  try {
    const res = await axios.post('http://localhost:8080/api/favorites/list', {
      username: userInfo.value.username
    });
    if (res.data?.code === 200) {
      favorites.value = res.data.data || [];
      favoritesCount.value = favorites.value.length;
    }
  } catch (e) {
    console.error('加载收藏失败:', e);
  } finally {
    favoritesLoading.value = false;
  }
};

// 加载评分
const loadRatings = async () => {
  ratingsLoading.value = true;
  try {
    const res = await axios.post('http://localhost:8080/api/anime/rating/user/list', {
      username: userInfo.value.username
    });
    if (res.data?.code === 200) ratings.value = res.data.data || [];
  } catch (e) {
    console.error('加载评分失败:', e);
  } finally {
    ratingsLoading.value = false;
  }
};

// 加载帖子
const loadPosts = async () => {
  postsLoading.value = true;
  try {
    const res = await axios.get('http://localhost:8080/api/post/list');
    if (res.data && Array.isArray(res.data)) {
      const userPosts = res.data.filter((p: any) =>
        p.author?.username === userInfo.value.username
      ).map((p: any) => ({
        ...p,
        authorName: p.author?.username || '未知用户',
        createTime: p.createTime
      }));
      posts.value = userPosts;
      postsCount.value = userPosts.length;
    }
  } catch (e) {
    console.error('加载帖子失败:', e);
  } finally {
    postsLoading.value = false;
  }
};

// 切换关注
const toggleFollow = async () => {
  if (!currentUserId.value) {
    alert('请先登录');
    return;
  }
  try {
    const res = await axios.post('http://localhost:8080/api/follow/toggle', {
      followerId: currentUserId.value,
      followedId: userInfo.value.id
    });
    if (res.data?.code === 200) {
      const followed = res.data.data.followed;
      isFollowing.value = followed;
      followerCount.value += followed ? 1 : -1;
    }
  } catch (e) {
    console.error('关注操作失败:', e);
  }
};

const goToDM = () => {
  router.push(`/chat?user=${userInfo.value.id}`);
};

const goToUserHome = (username: string) => {
  router.push(`/user/${username}`);
};

const goToAnime = (animeId: number, episode: number) => {
  if (animeId && episode) {
    router.push(`/anime/${animeId}/play/${episode}`);
  }
};

const goToAnimeDetail = (animeId: number) => {
  router.push(`/anime/${animeId}`);
};

onMounted(async () => {
  checkAdmin();
  fetchUnreadCount();

  const username = route.params.username as string;
  if (!username) {
    loading.value = false;
    return;
  }

  // 获取当前用户ID
  const curUsername = localStorage.getItem('username');
  if (curUsername) {
    try {
      const profileRes = await axios.post('http://localhost:8080/api/user/profile', { username: curUsername });
      if (profileRes.data?.code === 200) currentUserId.value = profileRes.data.data.id || 0;
    } catch (e) { /* ignore */ }
  }

  await loadUserProfile(username);
  await loadFollowStatus();

  loading.value = false;

  // 预加载各tab数据
  loadWatchHistory();
  loadFavorites();
  loadRatings();
  loadPosts();
});
</script>

<style scoped>
.user-home-container {
  min-height: 100vh;
  background: #f5f5f5;
}

/* 导航栏 */
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
.nav-links a:hover { color: #ff6b6b; }
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
}

/* 容器 */
.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

/* 用户头部 */
.user-header {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  margin-bottom: 20px;
}
.user-header-bg {
  height: 120px;
  background: linear-gradient(135deg, #ff6b6b, #ff8e8e);
}
.user-header-content {
  display: flex;
  align-items: center;
  padding: 0 30px 20px;
  margin-top: -50px;
  gap: 20px;
  flex-wrap: wrap;
}
.user-avatar-wrap {
  flex-shrink: 0;
}
.user-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 4px solid white;
  object-fit: cover;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}
.user-info-main {
  flex: 1;
  min-width: 200px;
}
.user-name {
  font-size: 22px;
  font-weight: bold;
  color: #333;
  margin: 0 0 6px;
}
.user-role {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  background: #e8f5e9;
  color: #4caf50;
  margin-bottom: 8px;
}
.user-role.admin {
  background: #fff3e0;
  color: #ff9800;
}
.user-signature {
  font-size: 14px;
  color: #999;
  margin: 0;
}
.user-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}
.btn {
  padding: 8px 20px;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
  font-weight: 500;
}
.follow-btn {
  background: #4caf50;
  color: white;
}
.follow-btn:hover { background: #45a049; }
.follow-btn.following {
  background: #ccc;
  color: #666;
}
.dm-btn {
  background: #2196f3;
  color: white;
}
.dm-btn:hover { background: #1976d2; }

/* 用户统计 */
.user-stats {
  display: flex;
  justify-content: space-around;
  padding: 16px 30px;
  border-top: 1px solid #f0f0f0;
}
.stat-item {
  text-align: center;
  cursor: pointer;
  transition: color 0.2s;
}
.stat-item:hover { color: #ff6b6b; }
.stat-num {
  display: block;
  font-size: 20px;
  font-weight: bold;
  color: #333;
}
.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

/* 标签导航 */
.tab-nav {
  display: flex;
  background: white;
  border-radius: 8px;
  padding: 4px;
  gap: 4px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.tab-btn {
  flex: 1;
  padding: 10px 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}
.tab-btn:hover { color: #ff6b6b; }
.tab-btn.active {
  background: #ff6b6b;
  color: white;
}

/* 内容区 */
.tab-content {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  min-height: 300px;
}

/* 个人资料网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.info-card {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}
.info-card.full-width {
  grid-column: 1 / -1;
}
.info-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 6px;
}
.info-value {
  font-size: 15px;
  color: #333;
  word-break: break-word;
}

/* 卡片列表 */
.card-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.card-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #f0f0f0;
}
.card-item:hover {
  background: #f0f0f0;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.card-img {
  width: 80px;
  height: 100px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}
.card-info {
  flex: 1;
  min-width: 0;
}
.card-info h4 {
  font-size: 15px;
  color: #333;
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-info p {
  font-size: 13px;
  color: #999;
  margin: 0;
}
.rating-badge {
  display: inline-block;
  margin-top: 6px;
  padding: 2px 8px;
  background: #fff3e0;
  color: #ff9800;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

/* 帖子列表 */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.post-item {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}
.post-title {
  font-size: 16px;
  color: #333;
  margin: 0 0 8px;
}
.post-content {
  font-size: 14px;
  color: #666;
  margin: 0 0 10px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.post-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}

/* 关注列表 */
.follow-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.follow-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #f0f0f0;
}
.follow-item:hover {
  background: #f0f0f0;
}
.follow-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}
.follow-details h4 {
  font-size: 15px;
  color: #333;
  margin: 0 0 4px;
}
.follow-details p {
  font-size: 13px;
  color: #999;
  margin: 0;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
}
.page-btn {
  padding: 6px 14px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
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
  font-size: 14px;
  color: #666;
  padding: 0 8px;
}

/* 加载/空状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}
.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #ff6b6b;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
.loading, .empty {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 15px;
}

/* 底栏 */
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

@media (max-width: 768px) {
  .user-header-content {
    flex-direction: column;
    text-align: center;
    padding: 0 20px 20px;
  }
  .user-actions {
    margin-top: 10px;
  }
  .info-grid {
    grid-template-columns: 1fr;
  }
  .card-list {
    grid-template-columns: 1fr;
  }
  .tab-nav {
    flex-wrap: wrap;
  }
  .tab-btn {
    flex: none;
    padding: 8px 14px;
    font-size: 13px;
  }
}
</style>