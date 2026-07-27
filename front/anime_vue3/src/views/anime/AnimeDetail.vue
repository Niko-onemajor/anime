<template>
  <div class="anime-detail-container">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="navbar-container">
        <a href="/index" class="logo">Niko动漫</a>
        <ul class="nav-links">
          <li class="nav-link"><a href="/index">首页</a></li>
          <li class="nav-link"><a href="/category">分类</a></li>
          <li class="nav-link"><a href="/forum">论坛</a></li>
          <li class="nav-link"><a href="/messages">消息<span v-if="navUnreadCount > 0" class="nav-badge">{{ navUnreadCount }}</span></a></li>
          <li class="nav-link"><a href="/profile">个人中心</a></li>
          <li class="nav-link" v-if="isAdmin"><a href="/admin/users">管理员后台</a></li>
        </ul>
      </div>
    </nav>

    <!-- 加载状态 -->
    <div class="detail-content" v-if="loading">
      <div class="loading-message">
        <h2>加载中...</h2>
        <p>正在获取动漫详情，请稍候</p>
      </div>
    </div>
    
    <!-- 详情内容 -->
    <div class="detail-content" v-else-if="Number(anime.status) === 1">
      <div class="detail-header">
        <div class="detail-poster" @click="showFullImage = true">
          <img :src="getImageUrl(anime.image)" :alt="anime.title" @error="handleImageError($event, anime)" />
          <div class="poster-overlay">
            <span class="overlay-text">点击查看大图</span>
          </div>
        </div>
        <div class="detail-info">
          <div class="title-row">
            <h1 class="detail-title">{{ anime.title }}</h1>
            <button class="back-btn" @click="goBack">返回</button>
          </div>
          <div class="detail-meta">
            <span class="meta-item">年份：{{ anime.year }}</span>
            <span class="meta-item">类型：{{ anime.genre }}</span>
            <span class="meta-item">评分：{{ (anime.rating || 0).toFixed(1) }}</span>
            <span class="meta-item">观看次数：{{ watchCount }}</span>
          </div>
          <div class="detail-desc">
            <h3>剧情简介</h3>
            <p>{{ anime.description }}</p>
          </div>
          <div class="action-buttons">
            <button class="watch-btn" @click="goToPlay(episodes[0]?.episodeNumber || 1)">立即观看</button>
            <button class="favorite-btn" :class="{ active: isFavorited }" @click="toggleFavorite">
              {{ isFavorited ? '已收藏' : '收藏' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 选集 -->
      <div class="episodes-section">
        <h2>选集</h2>
        <div class="episodes-list">
          <button class="episode-btn" v-for="episode in episodes" :key="episode.episodeNumber" @click="goToPlay(episode.episodeNumber)">第{{ episode.episodeNumber }}集</button>
        </div>
      </div>

      <!-- 相关推荐 -->
      <div class="related-section">
        <h2>相关推荐</h2>
        <div class="related-list">
          <div class="related-item" v-for="item in filteredRelatedAnimes" :key="item.id" @click="goToDetail(item.id)">
            <div class="related-image">
              <img :src="getImageUrl(item.image)" :alt="item.title" @error="handleImageError($event, item)" />
            </div>
            <div class="related-info">
              <h3 class="related-title">{{ item.title }}</h3>
              <p class="related-year">{{ item.year }}</p>
            </div>
          </div>
        </div>
      </div>


    </div>
    
    <!-- 下架提示 -->
    <div class="detail-content" v-else>
      <div class="offline-message">
        <h1>该动漫已被下架</h1>
        <p>很抱歉，您访问的动漫已被管理员下架，暂时无法观看。</p>
        <button class="back-btn" @click="router.push('/index')">返回首页</button>
      </div>
    </div>

    <!-- 大图模态框 -->
    <div class="modal-overlay" v-if="showFullImage" @click="showFullImage = false; resetImageScale()">
      <div class="modal-content" @click.stop @wheel="handleWheel">
        <div class="modal-controls">
          <button class="modal-close" @click="showFullImage = false; resetImageScale()">×</button>
          <button class="modal-reset" @click="resetImageScale()">重置大小</button>
        </div>
        <div class="image-container">
          <img :src="getImageUrl(anime.image)" :alt="anime.title" class="full-image" :style="{ transform: `scale(${imageScale})` }" />
        </div>
      </div>
    </div>



    <!-- 底栏 -->
    <footer class="footer">
      <div class="footer-content">
        <p>© 2026 Niko动漫. 保留所有权利.</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import api from '@/utils/api';
import { ElMessageBox, ElMessage } from 'element-plus';

const router = useRouter();

const goToDetail = (id: number) => {
  // 从详情页跳转到其他详情页时，保留fromPage，让新的详情页知道来源
  router.push(`/anime/${id}`);
};

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

const route = useRoute();
const animeId = computed(() => {
  console.log('路由参数:', route.params);
  return Number(route.params.id);
});

const goToPlay = (episode: number) => {
  // 从详情页进入播放页时，保留fromPage，让播放页返回详情页后知道来源
  router.push(`/anime/${animeId.value}/play/${episode}`);
};

// 返回上一页
const goBack = () => {
  // 从localStorage获取来源页面
  const fromPage = localStorage.getItem('fromPage');
  if (fromPage) {
    // 跳转到来源页面
    router.push(fromPage);
    // 清除来源页面记录
    localStorage.removeItem('fromPage');
  } else {
    // 如果没有来源页面，使用默认的返回
    router.back();
  }
};

// 动漫数据
const anime = ref<any>({});

// 加载状态
const loading = ref(true);

// 观看次数
const watchCount = ref(0);

// 大图模态框状态
const showFullImage = ref(false);
const imageScale = ref(1);

// 收藏状态
const isFavorited = ref(false);

// 处理鼠标滚轮事件
const handleWheel = (event: WheelEvent) => {
  event.preventDefault();
  // 每次滚轮调整0.1的缩放比例
  const delta = event.deltaY > 0 ? -0.1 : 0.1;
  imageScale.value = Math.max(0.1, Math.min(3, imageScale.value + delta));
};

// 重置图片缩放
const resetImageScale = () => {
  imageScale.value = 1;
};

// 处理图片URL
const getImageUrl = (url: string): string => {
  if (!url) return '';
  // 如果是完整的URL，直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  // 如果是本地文件路径，转换为HTTP URL
  if (url.startsWith('file:///')) {
    // 提取文件名
    const filename = url.split('/').pop();
    return `http://localhost:8080/covers/${filename}`;
  }
  // 如果是本地文件路径（Windows或Unix风格），转换为HTTP URL
  if (url.includes(':')) {
    // 提取文件名，同时处理正斜杠和反斜杠
    const parts = url.split(/[\\/]/);
    const filename = parts.pop();
    return `http://localhost:8080/covers/${filename}`;
  }
  // 如果是本地路径，使用本地存储路径
  if (url.startsWith('/src/images/')) {
    const filename = url.split('/').pop();
    return `http://localhost:8080/covers/${filename?.replace('.jpg', '.webp')}`;
  }
  // 如果是上传路径，使用代理路径
  if (url.startsWith('/uploads/')) {
    return url;
  }
  // 默认返回原路径
  return url;
};

// 处理图片加载错误
const handleImageError = (event: Event, item: any) => {
  const target = event.target as HTMLImageElement;
  // 设置默认图片
  target.src = 'https://via.placeholder.com/300x450?text=暂无图片';
  console.error('图片加载失败:', item.image);
};

// 检查是否已收藏
const checkFavorite = async () => {
  let username = localStorage.getItem('username');
  if (!username) {
    // 尝试从sessionStorage获取
    username = sessionStorage.getItem('username');
    if (!username) return;
  }
  
  try {
    const res = await axios.post('/api/favorites/check', {
      username: username,
      animeId: animeId.value
    });
    if (res.data.code === 200) {
      isFavorited.value = res.data.data;
    }
  } catch (error) {
    console.error('检查收藏状态失败:', error);
  }
};

// 切换收藏状态
const toggleFavorite = async () => {
  let username = localStorage.getItem('username');
  if (!username) {
    // 尝试从sessionStorage获取
    username = sessionStorage.getItem('username');
    if (!username) {
      ElMessage.warning('请先登录');
      return;
    }
  }
  
  try {
    const url = isFavorited.value ? '/api/favorites/remove' : '/api/favorites/add';
    const res = await axios.post(url, {
      username: username,
      animeId: animeId.value
    });
    
    if (res.data.code === 200) {
      isFavorited.value = !isFavorited.value;
      ElMessage.success(res.data.msg);
    } else {
      ElMessage.error(res.data.msg);
    }
  } catch (error) {
    console.error('操作收藏失败:', error);
    ElMessage.error('操作失败，请重试');
  }
};

// 集数数据
const episodes = ref<any[]>([]);

// 从后端获取相关推荐数据
const relatedAnimes = ref<any[]>([]);

// 计算标签相似度
const calculateSimilarity = (tags1: string, tags2: string) => {
  const tags1Array = tags1.split(',').map(tag => tag.trim());
  const tags2Array = tags2.split(',').map(tag => tag.trim());
  
  const intersection = tags1Array.filter(tag => tags2Array.includes(tag));
  const union = [...new Set([...tags1Array, ...tags2Array])];
  
  return union.length > 0 ? intersection.length / union.length : 0;
};

// 过滤相关推荐，避免与当前动漫重复且只显示上架状态的动漫，并按标签相似度排序
const filteredRelatedAnimes = computed(() => {
  if (!anime.value.genre) return [];
  
  return relatedAnimes.value
    .filter(item => item.id !== anime.value.id && Number(item.status) === 1)
    .map(item => ({
      ...item,
      similarity: calculateSimilarity(anime.value.genre, item.genre || '')
    }))
    .sort((a, b) => b.similarity - a.similarity)
    .slice(0, 5);
});



// 根据ID获取动漫详情
const fetchAnimeDetail = async () => {
  loading.value = true;
  try {
    // 获取当前动漫详情
    const animeRes = await axios.get(`/api/anime/detail/${animeId.value}`);
    if (animeRes.data) {
      anime.value = animeRes.data;
    }

    // 获取动漫的观看次数
    const watchCountRes = await axios.get(`/api/anime/watch-count/${animeId.value}`);
    if (watchCountRes.data) {
      watchCount.value = watchCountRes.data.watchCount;
    }

    // 获取动漫的集数
    const episodesRes = await axios.get(`/api/episode/anime/${animeId.value}`);
    if (episodesRes.data && episodesRes.data.code === 200) {
      episodes.value = episodesRes.data.data;
    }

    // 获取所有上架的动漫作为相关推荐
    const allAnimesRes = await axios.get('/api/anime/list');
    if (allAnimesRes.data) {
      relatedAnimes.value = allAnimesRes.data;
    }

    // 检查收藏状态
    checkFavorite();
  } catch (error) {
    console.error('获取动漫详情失败:', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  checkAdmin();
  fetchUnreadCount();
  fetchAnimeDetail();
  
  // 检查用户登录状态，确保收藏功能正常
  setTimeout(() => {
    checkFavorite();
  }, 500);
});

// 监听路由参数变化，更新动漫详情
watch(animeId, () => {
  fetchAnimeDetail();
});
</script>

<style scoped>
.anime-detail-container {
  min-height: 100vh;
  background: #f5f5f5;
}

/* 导航栏 */
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
  text-decoration: none;
}

.nav-links {
  display: flex;
  list-style: none;
}

.nav-link {
  margin-left: 20px;
}

.nav-link a {
  text-decoration: none;
  color: white;
  font-size: 16px;
  transition: color 0.3s;
}

.nav-link a:hover {
  color: #ff6b6b;
}

.nav-link.active a {
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

/* 详情内容 */
.detail-content {
  max-width: 1200px;
  margin: 20px auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.back-btn {
  background: #4a90e2;
  color: #fff;
  border: none;
  padding: 12px 30px;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s;
}

.back-btn:hover {
  background: #357abd;
}

.detail-header {
  display: flex;
  gap: 30px;
  margin-bottom: 40px;
}

.detail-poster {
  width: 300px;
  height: 450px;
  overflow: hidden;
  border-radius: 8px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
  position: relative;
  cursor: pointer;
}

.detail-poster img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.detail-poster:hover img {
  transform: scale(1.05);
}

.poster-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s;
  border-radius: 8px;
}

.detail-poster:hover .poster-overlay {
  opacity: 1;
}

.overlay-text {
  color: white;
  font-size: 18px;
  font-weight: 500;
  text-align: center;
}

/* 大图模态框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  overflow: auto;
}

.modal-content {
  position: relative;
  width: 100%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.modal-controls {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  gap: 10px;
  z-index: 10;
}

.modal-close {
  background: rgba(0, 0, 0, 0.7);
  color: white;
  border: none;
  font-size: 24px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: background 0.3s;
}

.modal-close:hover {
  background: rgba(0, 0, 0, 0.9);
}

.modal-reset {
  background: rgba(0, 0, 0, 0.7);
  color: white;
  border: none;
  font-size: 14px;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}

.modal-reset:hover {
  background: rgba(0, 0, 0, 0.9);
}

.image-container {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 90vh;
  overflow: auto;
}

.full-image {
  width: 100%;
  object-fit: contain;
  transition: transform 0.2s ease;
}

.detail-info {
  flex: 1;
}

.detail-title {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.detail-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  font-size: 14px;
  color: #666;
}

.meta-item {
  background: #f0f0f0;
  padding: 5px 10px;
  border-radius: 4px;
}

.detail-desc {
  margin-top: 20px;
  margin-bottom: 30px;
}

.detail-desc h3 {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin: 0 0 10px 0;
}

.detail-desc p {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin: 0;
}

.action-buttons {
  display: flex;
  gap: 15px;
}

.watch-btn {
  background: #ff6b6b;
  color: #fff;
  border: none;
  padding: 12px 30px;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s;
}

.watch-btn:hover {
  background: #ff5252;
}

.favorite-btn {
  background: #f0f0f0;
  color: #333;
  border: 1px solid #ddd;
  padding: 12px 30px;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.favorite-btn:hover {
  background: #e0e0e0;
}

.favorite-btn.active {
  background: #4caf50;
  color: white;
  border-color: #4caf50;
}

.favorite-btn.active:hover {
  background: #45a049;
}

/* 选集 */
.episodes-section {
  margin-bottom: 40px;
}

.episodes-section h2 {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin: 0 0 20px 0;
}

.episodes-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 10px;
}

.episode-btn {
  background: #f0f0f0;
  border: 1px solid #ddd;
  padding: 10px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.episode-btn:hover {
  background: #ff6b6b;
  color: #fff;
  border-color: #ff6b6b;
}

/* 相关推荐 */
.related-section {
  margin-bottom: 40px;
}

.related-section h2 {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin: 0 0 20px 0;
}

.related-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.related-item {
  background: #f9f9f9;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
  cursor: pointer;
}

.related-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.related-image {
  height: 250px;
  overflow: hidden;
}

.related-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.related-item:hover .related-image img {
  transform: scale(1.05);
}

.related-info {
  padding: 15px;
}

.related-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin: 0 0 10px 0;
  height: 40px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-clamp: 2;
}

.related-year {
  font-size: 14px;
  color: #666;
  margin: 0;
}



/* 加载状态 */
.loading-message {
  text-align: center;
  padding: 100px 20px;
}

.loading-message h2 {
  font-size: 24px;
  color: #ff6b6b;
  margin-bottom: 20px;
}

.loading-message p {
  font-size: 16px;
  color: #666;
  margin-bottom: 30px;
}

/* 下架提示 */
.offline-message {
  text-align: center;
  padding: 100px 20px;
}

.offline-message h1 {
  font-size: 32px;
  color: #ff6b6b;
  margin-bottom: 20px;
}

.offline-message p {
  font-size: 18px;
  color: #666;
  margin-bottom: 30px;
}

.back-btn {
  background: #4a90e2;
  color: #fff;
  border: none;
  padding: 12px 30px;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s;
}

.back-btn:hover {
  background: #357abd;
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

/* 响应式适配 */
@media (max-width: 768px) {
  .navbar-container {
    padding: 0 15px;
  }

  .nav-link {
    margin-left: 20px;
  }

  .detail-content {
    padding: 15px;
  }

  .detail-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .detail-poster {
    width: 200px;
    height: 300px;
  }

  .detail-title {
    font-size: 24px;
  }

  .detail-meta {
    justify-content: center;
  }

  .action-buttons {
    flex-direction: column;
    align-items: center;
  }

  .watch-btn,
  .favorite-btn {
    width: 200px;
    text-align: center;
  }

  .episodes-list {
    grid-template-columns: repeat(auto-fill, minmax(60px, 1fr));
  }

  .related-list {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 15px;
  }

  .related-image {
    height: 200px;
  }

  .comment-controls {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
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

  .footer-content {
    padding: 0 15px;
  }
}
</style>