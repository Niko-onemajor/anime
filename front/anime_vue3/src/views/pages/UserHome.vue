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
        <button class="nav-search-btn" @click="showSearchModal = true" title="搜索用户">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        </button>
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
              <img :src="getImageUrl(userInfo.avatar)" :alt="userInfo.username" class="user-avatar" @click="showAvatarPreview = true">
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
            <div class="stat-item" @click="switchTab('following')" :class="{ 'stat-disabled': isStatHidden('following') }">
              <span class="stat-num">{{ isStatHidden('following') ? '***' : followingCount }}</span>
              <span class="stat-label">关注</span>
            </div>
            <div class="stat-item" @click="switchTab('followers')" :class="{ 'stat-disabled': isStatHidden('followers') }">
              <span class="stat-num">{{ isStatHidden('followers') ? '***' : followerCount }}</span>
              <span class="stat-label">粉丝</span>
            </div>
            <div class="stat-item" @click="switchTab('posts')" :class="{ 'stat-disabled': isStatHidden('posts') }">
              <span class="stat-num">{{ isStatHidden('posts') ? '***' : postsCount }}</span>
              <span class="stat-label">帖子</span>
            </div>
            <div class="stat-item" @click="switchTab('comments')" :class="{ 'stat-disabled': isStatHidden('comments') }">
              <span class="stat-num">{{ isStatHidden('comments') ? '***' : commentsCount }}</span>
              <span class="stat-label">评论</span>
            </div>
            <div class="stat-item" @click="switchTab('favorites')" :class="{ 'stat-disabled': isStatHidden('favorites') }">
              <span class="stat-num">{{ isStatHidden('favorites') ? '***' : favoritesCount }}</span>
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
            :class="{ active: activeTab === tab.key, locked: !isSelf && isTabHidden(tab.key) }"
            @click="switchTab(tab.key)"
          >
            <span v-if="!isSelf && isTabHidden(tab.key)" class="tab-lock-icon">🔒</span>
            {{ tab.label }}
          </button>
        </div>

        <!-- 隐私保护提示 -->
        <div v-if="!isSelf && !privacySettings.profilePublic" class="privacy-lock">
          <div class="privacy-lock-icon">🔒</div>
          <h3>该用户已设置个人主页为私密</h3>
          <p>对方设置了隐私保护，你无法查看其详细资料</p>
        </div>

        <!-- 内容区域 -->
        <div class="tab-content" v-else>
          <!-- 被隐藏的tab显示提示 -->
          <div v-if="!isSelf && isTabHidden(activeTab)" class="privacy-hidden-tip">
            <div class="privacy-lock-icon">🔒</div>
            <p>该用户已隐藏此内容</p>
          </div>
          <!-- 个人资料 -->
          <div v-else-if="activeTab === 'profile'" class="info-grid">
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
          <div v-else-if="activeTab === 'watchHistory'">
            <div v-if="watchHistoryLoading" class="loading">加载中...</div>
            <div v-else-if="watchHistory.length === 0" class="empty">暂无观看记录</div>
            <div v-else>
              <div class="card-list">
                <div v-for="item in pagedWatchHistory" :key="item.id" class="card-item" @click="goToAnimeDetail(item.anime?.id)">
                  <img :src="getImageUrl(item.anime?.image)" :alt="item.anime?.title" class="card-img">
                  <div class="card-info">
                    <h4>{{ item.anime?.title }}</h4>
                    <p>第{{ item.episode?.episodeNumber || '?' }}集 · {{ formatTime(item.watchTime) }}</p>
                  </div>
                </div>
              </div>
              <div class="pagination" v-if="watchHistoryTotalPages > 1">
                <button @click="watchHistoryPage = 1" :disabled="watchHistoryPage === 1" class="page-btn">首页</button>
                <button @click="watchHistoryPage = watchHistoryPage - 1" :disabled="watchHistoryPage === 1" class="page-btn">上一页</button>
                <span class="page-info">{{ watchHistoryPage }} / {{ watchHistoryTotalPages }}</span>
                <button @click="watchHistoryPage = watchHistoryPage + 1" :disabled="watchHistoryPage === watchHistoryTotalPages" class="page-btn">下一页</button>
                <button @click="watchHistoryPage = watchHistoryTotalPages" :disabled="watchHistoryPage === watchHistoryTotalPages" class="page-btn">尾页</button>
              </div>
            </div>
          </div>

          <!-- 收藏 -->
          <div v-else-if="activeTab === 'favorites'">
            <div v-if="favoritesLoading" class="loading">加载中...</div>
            <div v-else-if="favorites.length === 0" class="empty">暂无收藏</div>
            <div v-else>
              <div class="card-list">
                <div v-for="anime in pagedFavorites" :key="anime.id" class="card-item" @click="goToAnimeDetail(anime.id)">
                  <img :src="getImageUrl(anime.image)" :alt="anime.title" class="card-img">
                  <div class="card-info">
                    <h4>{{ anime.title }}</h4>
                    <p>{{ anime.genre }} · {{ anime.year }}</p>
                  </div>
                </div>
              </div>
              <div class="pagination" v-if="favoritesTotalPages > 1">
                <button @click="favoritesPage = 1" :disabled="favoritesPage === 1" class="page-btn">首页</button>
                <button @click="favoritesPage = favoritesPage - 1" :disabled="favoritesPage === 1" class="page-btn">上一页</button>
                <span class="page-info">{{ favoritesPage }} / {{ favoritesTotalPages }}</span>
                <button @click="favoritesPage = favoritesPage + 1" :disabled="favoritesPage === favoritesTotalPages" class="page-btn">下一页</button>
                <button @click="favoritesPage = favoritesTotalPages" :disabled="favoritesPage === favoritesTotalPages" class="page-btn">尾页</button>
              </div>
            </div>
          </div>

          <!-- 评分 -->
          <div v-else-if="activeTab === 'ratings'">
            <div v-if="ratingsLoading" class="loading">加载中...</div>
            <div v-else-if="ratings.length === 0" class="empty">暂无评分</div>
            <div v-else>
              <div class="card-list">
                <div v-for="item in pagedRatings" :key="item.id" class="card-item" @click="goToAnimeDetail(item.anime?.id)">
                  <img :src="getImageUrl(item.anime?.image)" :alt="item.anime?.title" class="card-img">
                  <div class="card-info">
                    <h4>{{ item.anime?.title }}</h4>
                    <p>{{ item.anime?.genre }} · {{ item.anime?.year }}</p>
                    <span class="rating-badge">{{ item.rating }}分</span>
                  </div>
                </div>
              </div>
              <div class="pagination" v-if="ratingsTotalPages > 1">
                <button @click="ratingsPage = 1" :disabled="ratingsPage === 1" class="page-btn">首页</button>
                <button @click="ratingsPage = ratingsPage - 1" :disabled="ratingsPage === 1" class="page-btn">上一页</button>
                <span class="page-info">{{ ratingsPage }} / {{ ratingsTotalPages }}</span>
                <button @click="ratingsPage = ratingsPage + 1" :disabled="ratingsPage === ratingsTotalPages" class="page-btn">下一页</button>
                <button @click="ratingsPage = ratingsTotalPages" :disabled="ratingsPage === ratingsTotalPages" class="page-btn">尾页</button>
              </div>
            </div>
          </div>

          <!-- 帖子 -->
          <div v-else-if="activeTab === 'posts'">
            <div v-if="postsLoading" class="loading">加载中...</div>
            <div v-else-if="posts.length === 0" class="empty">暂无发布的帖子</div>
            <div v-else>
              <div class="post-list">
                <div v-for="post in pagedPosts" :key="post.id" class="post-item" @click="goToForumPost(post.id)">
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

          <!-- 评论 -->
          <div v-else-if="activeTab === 'comments'">
            <div v-if="commentsLoading" class="loading">加载中...</div>
            <div v-else-if="comments.length === 0" class="empty">暂无评论记录</div>
            <div v-else>
              <div class="post-list">
                <div v-for="comment in pagedComments" :key="comment.id" class="post-item" @click="goToCommentTarget(comment)">
                  <div class="comment-type-badge" :class="comment.type === 'forum' ? 'forum' : 'anime'">
                    {{ comment.type === 'forum' ? '论坛' : '动漫' }}
                  </div>
                  <h4 class="post-title">{{ comment.targetTitle || '未知' }}</h4>
                  <p class="post-content">{{ comment.content }}</p>
                  <div class="post-meta">
                    <span>{{ formatTime(comment.createTime) }}</span>
                    <span>👍 {{ comment.likeCount || 0 }}</span>
                  </div>
                </div>
              </div>
              <div class="pagination" v-if="commentsTotalPages > 1">
                <button @click="commentsPage = 1" :disabled="commentsPage === 1" class="page-btn">首页</button>
                <button @click="commentsPage = commentsPage - 1" :disabled="commentsPage === 1" class="page-btn">上一页</button>
                <span class="page-info">{{ commentsPage }} / {{ commentsTotalPages }}</span>
                <button @click="commentsPage = commentsPage + 1" :disabled="commentsPage === commentsTotalPages" class="page-btn">下一页</button>
                <button @click="commentsPage = commentsTotalPages" :disabled="commentsPage === commentsTotalPages" class="page-btn">尾页</button>
              </div>
            </div>
          </div>

          <!-- 关注 -->
          <div v-else-if="activeTab === 'following'">
            <div v-if="followingLoading" class="loading">加载中...</div>
            <div v-else-if="followingList.length === 0" class="empty">暂无关注</div>
            <div v-else>
              <div class="follow-list">
                <div v-for="user in pagedFollowing" :key="user.id" class="follow-item" @click="goToUserHome(user.username)">
                  <img :src="getImageUrl(user.avatar)" :alt="user.username" class="follow-avatar">
                  <div class="follow-details">
                    <h4>{{ user.username }}</h4>
                    <p>{{ user.signature || '这个人很懒，什么都没写' }}</p>
                  </div>
                </div>
              </div>
              <div class="pagination" v-if="followingTotalPages > 1">
                <button @click="followingPage = 1" :disabled="followingPage === 1" class="page-btn">首页</button>
                <button @click="followingPage = followingPage - 1" :disabled="followingPage === 1" class="page-btn">上一页</button>
                <span class="page-info">{{ followingPage }} / {{ followingTotalPages }}</span>
                <button @click="followingPage = followingPage + 1" :disabled="followingPage === followingTotalPages" class="page-btn">下一页</button>
                <button @click="followingPage = followingTotalPages" :disabled="followingPage === followingTotalPages" class="page-btn">尾页</button>
              </div>
            </div>
          </div>

          <!-- 粉丝 -->
          <div v-else-if="activeTab === 'followers'">
            <div v-if="followersLoading" class="loading">加载中...</div>
            <div v-else-if="followerList.length === 0" class="empty">暂无粉丝</div>
            <div v-else>
              <div class="follow-list">
                <div v-for="user in pagedFollowers" :key="user.id" class="follow-item" @click="goToUserHome(user.username)">
                  <img :src="getImageUrl(user.avatar)" :alt="user.username" class="follow-avatar">
                  <div class="follow-details">
                    <h4>{{ user.username }}</h4>
                    <p>{{ user.signature || '这个人很懒，什么都没写' }}</p>
                  </div>
                </div>
              </div>
              <div class="pagination" v-if="followersTotalPages > 1">
                <button @click="followersPage = 1" :disabled="followersPage === 1" class="page-btn">首页</button>
                <button @click="followersPage = followersPage - 1" :disabled="followersPage === 1" class="page-btn">上一页</button>
                <span class="page-info">{{ followersPage }} / {{ followersTotalPages }}</span>
                <button @click="followersPage = followersPage + 1" :disabled="followersPage === followersTotalPages" class="page-btn">下一页</button>
                <button @click="followersPage = followersTotalPages" :disabled="followersPage === followersTotalPages" class="page-btn">尾页</button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 头像大图预览弹窗 -->
    <div class="dialog-overlay" v-if="showAvatarPreview" @click="showAvatarPreview = false">
      <div class="avatar-preview-container" @click.stop>
        <button @click="showAvatarPreview = false" class="close-btn avatar-close-btn">×</button>
        <img :src="getImageUrl(userInfo.avatar)" :alt="userInfo.username" class="avatar-preview-img">
      </div>
    </div>

    <footer class="footer">
      <div class="footer-content">
        <p>© 2026 Niko动漫. 保留所有权利.</p>
      </div>
    </footer>
  </div>

  <!-- 用户搜索弹窗 -->
  <div class="search-overlay" v-if="showSearchModal" @click.self="showSearchModal = false">
    <div class="search-modal">
      <div class="search-modal-header">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input ref="searchInputRef" v-model="searchKeyword" class="search-modal-input" placeholder="搜索用户..." @input="onSearchInput" @keydown.esc="showSearchModal = false" />
        <button class="search-modal-close" @click="showSearchModal = false">&times;</button>
      </div>
      <div class="search-modal-body">
        <div v-if="searchLoading" class="search-loading">搜索中...</div>
        <div v-else-if="searchResults.length === 0 && searchKeyword.trim()" class="search-empty">未找到相关用户</div>
        <div v-else-if="searchResults.length === 0 && !searchKeyword.trim()" class="search-hint">输入用户名开始搜索</div>
        <div v-else class="search-results">
          <div v-for="user in searchResults" :key="user.id" class="search-result-item" @click="goToUserHome(user.username)">
            <img :src="getImageUrl(user.avatar)" :alt="user.username" class="search-result-avatar" />
            <div class="search-result-info">
              <span class="search-result-name">{{ user.username }}</span>
              <span class="search-result-sig">{{ user.signature || '这个人很懒，什么都没写' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import api from '@/utils/api';
import { ElMessage } from 'element-plus';

const route = useRoute();
const router = useRouter();

// 用户搜索
const showSearchModal = ref(false);
const searchKeyword = ref('');
const searchResults = ref<any[]>([]);
const searchLoading = ref(false);
const searchInputRef = ref<HTMLInputElement | null>(null);
let searchTimer: ReturnType<typeof setTimeout> | null = null;

const onSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer);
  const keyword = searchKeyword.value.trim();
  if (!keyword) {
    searchResults.value = [];
    return;
  }
  searchLoading.value = true;
  searchTimer = setTimeout(async () => {
    try {
      const res = await axios.get('http://localhost:8080/api/user/search', { params: { keyword } });
      if (res.data.code === 200) {
        searchResults.value = res.data.data || [];
      }
    } catch (e) {
      console.error('搜索用户失败:', e);
    } finally {
      searchLoading.value = false;
    }
  }, 300);
};

const loading = ref(true);
const isAdmin = ref(false);
const navUnreadCount = ref(0);
const activeTab = ref('profile');
const showAvatarPreview = ref(false);

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

// 隐私设置
const privacySettings = ref({
  profilePublic: true,
  showWatchHistory: true,
  showFavorites: true,
  showRatings: true,
  showPosts: true,
  showComments: true,
  showFollows: true
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

// 关注分页
const followingPage = ref(1);
const followingPageSize = 5;
const followingTotalPages = computed(() => Math.ceil(followingList.value.length / followingPageSize) || 1);
const pagedFollowing = computed(() => {
  const start = (followingPage.value - 1) * followingPageSize;
  return followingList.value.slice(start, start + followingPageSize);
});

// 粉丝分页
const followersPage = ref(1);
const followersPageSize = 5;
const followersTotalPages = computed(() => Math.ceil(followerList.value.length / followersPageSize) || 1);
const pagedFollowers = computed(() => {
  const start = (followersPage.value - 1) * followersPageSize;
  return followerList.value.slice(start, start + followersPageSize);
});

// 观看记录
const watchHistory = ref<any[]>([]);
const watchHistoryLoading = ref(false);
const watchHistoryPage = ref(1);
const watchHistoryPageSize = 9;
const watchHistoryTotalPages = computed(() => Math.ceil(watchHistory.value.length / watchHistoryPageSize) || 1);
const pagedWatchHistory = computed(() => {
  const start = (watchHistoryPage.value - 1) * watchHistoryPageSize;
  return watchHistory.value.slice(start, start + watchHistoryPageSize);
});

// 收藏
const favorites = ref<any[]>([]);
const favoritesCount = ref(0);
const favoritesLoading = ref(false);
const favoritesPage = ref(1);
const favoritesPageSize = 9;
const favoritesTotalPages = computed(() => Math.ceil(favorites.value.length / favoritesPageSize) || 1);
const pagedFavorites = computed(() => {
  const start = (favoritesPage.value - 1) * favoritesPageSize;
  return favorites.value.slice(start, start + favoritesPageSize);
});

// 评分
const ratings = ref<any[]>([]);
const ratingsLoading = ref(false);
const ratingsPage = ref(1);
const ratingsPageSize = 9;
const ratingsTotalPages = computed(() => Math.ceil(ratings.value.length / ratingsPageSize) || 1);
const pagedRatings = computed(() => {
  const start = (ratingsPage.value - 1) * ratingsPageSize;
  return ratings.value.slice(start, start + ratingsPageSize);
});

// 帖子
const posts = ref<any[]>([]);
const postsCount = ref(0);
const postsLoading = ref(false);
const postsPage = ref(1);
const postsPageSize = 3;
const postsTotalPages = computed(() => Math.ceil(posts.value.length / postsPageSize) || 1);
const pagedPosts = computed(() => {
  const start = (postsPage.value - 1) * postsPageSize;
  return posts.value.slice(start, start + postsPageSize);
});

// 评论
const comments = ref<any[]>([]);
const commentsCount = ref(0);
const commentsLoading = ref(false);
const commentsPage = ref(1);
const commentsPageSize = 3;
const commentsTotalPages = computed(() => Math.ceil(comments.value.length / commentsPageSize) || 1);
const pagedComments = computed(() => {
  const start = (commentsPage.value - 1) * commentsPageSize;
  return comments.value.slice(start, start + commentsPageSize);
});

const tabs = [
  { key: 'profile', label: '个人资料', privacyKey: '' },
  { key: 'watchHistory', label: '观看记录', privacyKey: 'showWatchHistory' },
  { key: 'favorites', label: '收藏', privacyKey: 'showFavorites' },
  { key: 'ratings', label: '评分', privacyKey: 'showRatings' },
  { key: 'posts', label: '帖子', privacyKey: 'showPosts' },
  { key: 'comments', label: '评论', privacyKey: 'showComments' },
  { key: 'following', label: '关注', privacyKey: 'showFollows' },
  { key: 'followers', label: '粉丝', privacyKey: 'showFollows' }
];

// 根据隐私设置过滤可见的标签页
const visibleTabs = computed(() => {
  if (isSelf.value) return tabs; // 自己看自己的主页，全部可见
  return tabs.filter(tab => {
    if (!tab.privacyKey) return true; // 个人资料始终可见
    return (privacySettings.value as any)[tab.privacyKey] !== false;
  });
});

// 切换 tab 时重置页码并加载数据
const switchTab = (key: string) => {
  activeTab.value = key;
  // 重置分页
  if (key === 'following' && followingList.value.length === 0) {
    loadFollowingList();
  } else if (key === 'followers' && followerList.value.length === 0) {
    loadFollowerList();
  } else if (key === 'comments' && comments.value.length === 0) {
    loadComments();
  }
};

// 检查当前tab是否被隐藏
const isTabHidden = (tabKey: string): boolean => {
  const tab = tabs.find(t => t.key === tabKey);
  if (!tab || !tab.privacyKey) return false;
  return (privacySettings.value as any)[tab.privacyKey] === false;
};

// 检查统计项是否被隐藏（用于显示***）
const isStatHidden = (tabKey: string): boolean => {
  return !isSelf.value && isTabHidden(tabKey);
};

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
      // 加载隐私设置
      privacySettings.value = {
        profilePublic: data.profilePublic !== false,
        showWatchHistory: data.showWatchHistory !== false,
        showFavorites: data.showFavorites !== false,
        showRatings: data.showRatings !== false,
        showPosts: data.showPosts !== false,
        showComments: data.showComments !== false,
        showFollows: data.showFollows !== false
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
    // 并行请求关注数和粉丝数
    const [fanRes, followRes] = await Promise.all([
      axios.get('http://localhost:8080/api/follow/follower-count', { params: { userId: userInfo.value.id } }),
      axios.get('http://localhost:8080/api/follow/following-count', { params: { userId: userInfo.value.id } })
    ]);
    if (fanRes.data?.code === 200) followerCount.value = fanRes.data.data.count || 0;
    if (followRes.data?.code === 200) followingCount.value = followRes.data.data.count || 0;

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

// 加载评论
const loadComments = async () => {
  if (!userInfo.value.id) return;
  commentsLoading.value = true;
  try {
    const [forumRes, animeRes] = await Promise.all([
      axios.get(`http://localhost:8080/api/comment/author/${userInfo.value.id}`),
      axios.get(`http://localhost:8080/api/anime/comment/author/${userInfo.value.id}`)
    ]);
    const allComments: any[] = [];
    if (forumRes.data?.code === 200 && Array.isArray(forumRes.data.data)) {
      allComments.push(...forumRes.data.data);
    }
    if (animeRes.data?.code === 200 && Array.isArray(animeRes.data.data)) {
      allComments.push(...animeRes.data.data);
    }
    // 按时间降序排序
    allComments.sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime());
    comments.value = allComments;
    commentsCount.value = allComments.length;
  } catch (e) {
    console.error('加载评论失败:', e);
  } finally {
    commentsLoading.value = false;
  }
};

// 切换关注
const toggleFollow = async () => {
  if (!currentUserId.value) {
    ElMessage.warning('请先登录');
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
      ElMessage.success(followed ? '关注成功' : '已取消关注');
      // 实时更新关注和粉丝列表
      await loadFollowStatus();
      if (followingList.value.length > 0 || activeTab.value === 'following') {
        loadFollowingList();
      }
      if (followerList.value.length > 0 || activeTab.value === 'followers') {
        loadFollowerList();
      }
    } else {
      ElMessage.warning(res.data.msg || '操作失败');
    }
  } catch (e) {
    console.error('关注操作失败:', e);
    ElMessage.error('操作失败，请重试');
  }
};

const goToDM = () => {
  router.push(`/chat?user=${userInfo.value.id}`);
};

const goToUserHome = (username: string) => {
  // 重新加载目标用户页面
  window.location.href = `/user/${username}`;
};

const goToAnime = (animeId: number, episode: number) => {
  if (animeId && episode) {
    router.push(`/anime/${animeId}/play/${episode}`);
  }
};

const goToAnimeDetail = (animeId: number) => {
  router.push(`/anime/${animeId}`);
};

const goToForumPost = (postId: number) => {
  router.push({ path: '/forum', query: { postId, highlightPost: 'true' } });
};

const goToCommentTarget = (comment: any) => {
  if (comment.type === 'forum') {
    router.push({ path: '/forum', query: { postId: comment.targetId, commentId: comment.id } });
  } else if (comment.type === 'anime') {
    router.push({ path: `/anime/${comment.targetId}/play/1`, query: { commentId: comment.id } });
  }
};

onMounted(async () => {
  checkAdmin();
  fetchUnreadCount();

  const username = route.params.username as string;
  if (!username) {
    loading.value = false;
    return;
  }

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
  position: sticky;
  top: 0;
  z-index: 1000;
}
.navbar-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
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
  margin-left: 30px;
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
  cursor: pointer;
  transition: transform 0.3s;
}
.user-avatar:hover {
  transform: scale(1.05);
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
.stat-item.stat-disabled {
  cursor: default;
  opacity: 0.7;
}
.stat-item.stat-disabled:hover { color: inherit; }
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
.tab-btn.locked {
  opacity: 0.6;
}
.tab-btn.locked.active {
  background: #bbb;
  color: white;
}
.tab-lock-icon {
  font-size: 11px;
  margin-right: 3px;
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
  cursor: pointer;
  transition: all 0.2s;
}
.post-item:hover {
  background: #f0f0f0;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
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

.comment-type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  margin-bottom: 6px;
}

.comment-type-badge.forum {
  background: #e3f2fd;
  color: #1976d2;
}

.comment-type-badge.anime {
  background: #fce4ec;
  color: #c62828;
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

/* 隐私保护提示 */
.privacy-lock {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.privacy-lock-icon {
  font-size: 48px;
  margin-bottom: 16px;
}
.privacy-lock h3 {
  font-size: 18px;
  color: #333;
  margin: 0 0 8px;
}
.privacy-lock p {
  font-size: 14px;
  color: #999;
  margin: 0;
}
.privacy-hidden-tip {
  text-align: center;
  padding: 60px 20px;
}
.privacy-hidden-tip .privacy-lock-icon {
  font-size: 48px;
  margin-bottom: 16px;
}
.privacy-hidden-tip p {
  font-size: 15px;
  color: #999;
  margin: 0;
}

/* 头像预览弹窗 */
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

/* 导航栏搜索按钮 */
.nav-search-btn {
  background: rgba(255,255,255,0.15);
  border: none;
  color: white;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.3s;
  flex-shrink: 0;
  margin-left: auto;
}
.nav-search-btn:hover {
  background: rgba(255,255,255,0.25);
}

/* 搜索弹窗 */
.search-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.4);
  z-index: 1000;
  display: flex;
  justify-content: center;
  padding-top: 15vh;
  animation: fadeIn 0.2s ease;
}
.search-modal {
  background: white;
  border-radius: 12px;
  width: 440px;
  max-width: 90vw;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 30px rgba(0,0,0,0.2);
  animation: searchSlideIn 0.25s ease;
}
@keyframes searchSlideIn {
  from { opacity: 0; transform: translateY(-20px); }
  to { opacity: 1; transform: translateY(0); }
}
.search-modal-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  gap: 12px;
}
.search-modal-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 16px;
  color: #333;
  background: transparent;
}
.search-modal-input::placeholder {
  color: #bbb;
}
.search-modal-close {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
  transition: color 0.2s;
}
.search-modal-close:hover {
  color: #333;
}
.search-modal-body {
  overflow-y: auto;
  flex: 1;
  padding: 8px 0;
}
.search-loading, .search-empty, .search-hint {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  font-size: 14px;
}
.search-results {
  display: flex;
  flex-direction: column;
}
.search-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  cursor: pointer;
  transition: background 0.2s;
}
.search-result-item:hover {
  background: #f5f5f5;
}
.search-result-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  background: #e0e0e0;
  flex-shrink: 0;
}
.search-result-info {
  flex: 1;
  min-width: 0;
}
.search-result-name {
  display: block;
  font-size: 15px;
  color: #333;
  font-weight: 500;
}
.search-result-sig {
  display: block;
  font-size: 13px;
  color: #999;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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