<template>
  <div class="message-container">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="navbar-container">
        <div class="logo">Niko动漫</div>
        <ul class="nav-links">
          <li><a href="/index">首页</a></li>
          <li><a href="/category">分类</a></li>
          <li><a href="/forum">论坛</a></li>
          <li><a href="/messages" class="active">消息<span v-if="navUnreadCount > 0" class="nav-badge">{{ navUnreadCount }}</span></a></li>
          <li><a href="/profile">个人中心</a></li>
          <li v-if="isAdmin"><a href="/admin/users">管理员后台</a></li>
        </ul>
      </div>
    </nav>

    <!-- 信息内容 -->
    <div class="content">
      <div class="message-header">
        <h2>我的消息</h2>
        <div class="header-actions">
          <span v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }} 条未读</span>
          <button v-if="unreadCount > 0" class="read-all-btn" @click="markAllAsRead">全部已读</button>
        </div>
      </div>

      <!-- 分类标签 -->
      <div class="category-tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'all' }"
          @click="activeTab = 'all'"
        >
          <span>全部</span>
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'comment' }"
          @click="activeTab = 'comment'"
        >
          <span class="tab-icon">💬</span>
          <span>评论</span>
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'like' }"
          @click="activeTab = 'like'"
        >
          <span class="tab-icon">👍</span>
          <span>点赞</span>
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'dislike' }"
          @click="activeTab = 'dislike'"
        >
          <span class="tab-icon">👎</span>
          <span>点踩</span>
        </button>
        <button class="clear-all-btn" @click="handleClearAll" title="一键清除全部未读消息">
          🗑️
        </button>
      </div>

      <!-- 二次确认弹窗 -->
      <div v-if="showClearConfirm" class="modal-overlay" @click.self="showClearConfirm = false">
        <div class="confirm-modal">
          <div class="confirm-header">
            <h3>确认清除</h3>
          </div>
          <div class="confirm-body">
            <p>确定要清除全部未读消息吗？</p>
            <p class="confirm-hint">清除后所有消息将标记为已读，导航栏未读角标将消失。</p>
          </div>
          <div class="confirm-footer">
            <button class="confirm-cancel-btn" @click="showClearConfirm = false">取消</button>
            <button class="confirm-ok-btn" @click="confirmClearAll">确认清除</button>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="filteredNotifications.length === 0" class="empty-container">
        <p class="empty-text">{{ activeTab === 'all' ? '暂无消息' : activeTab === 'comment' ? '暂无评论消息' : activeTab === 'like' ? '暂无点赞消息' : '暂无点踩消息' }}</p>
      </div>

      <!-- 通知列表 -->
      <div v-else class="notification-list">
        <div
          v-for="notification in filteredNotifications"
          :key="notification.id"
          class="notification-item"
          :class="{ unread: !notification.isRead }"
          @click="handleNotificationClick(notification)"
        >
          <div class="notification-icon">
            <span v-if="notification.type === 'FORUM_REPLY'" class="icon-reply">💬</span>
            <span v-else-if="notification.type === 'FORUM_LIKE'" class="icon-like">👍</span>
            <span v-else-if="notification.type === 'FORUM_DISLIKE'" class="icon-dislike">👎</span>
            <span v-else-if="notification.type === 'ANIME_REPLY'" class="icon-reply">💬</span>
            <span v-else-if="notification.type === 'ANIME_LIKE'" class="icon-like">👍</span>
            <span v-else-if="notification.type === 'ANIME_DISLIKE'" class="icon-dislike">👎</span>
            <span v-else class="icon-default">🔔</span>
          </div>
          <div class="notification-content">
            <span v-if="getTypeLabel(notification.type)" class="notification-type">{{ getTypeLabel(notification.type) }}</span>
            <p class="notification-message">{{ notification.message }}</p>
            <span class="notification-time">{{ formatTime(notification.createTime) }}</span>
          </div>
          <div v-if="!notification.isRead" class="unread-dot"></div>
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
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/utils/api';

const router = useRouter();
const isAdmin = ref(false);
const isLoading = ref(true);
const activeTab = ref('all');
const navUnreadCount = ref(0);
const notifications = ref<any[]>([]);
const unreadCount = ref(0);
const showClearConfirm = ref(false);

const filteredNotifications = computed(() => {
  if (activeTab.value === 'all') {
    return notifications.value;
  } else if (activeTab.value === 'comment') {
    return notifications.value.filter(n => n.type === 'FORUM_REPLY' || n.type === 'ANIME_REPLY');
  } else if (activeTab.value === 'like') {
    return notifications.value.filter(n => n.type === 'FORUM_LIKE' || n.type === 'ANIME_LIKE');
  } else {
    return notifications.value.filter(n => n.type === 'FORUM_DISLIKE' || n.type === 'ANIME_DISLIKE');
  }
});

const checkAdmin = () => {
  let role = localStorage.getItem('role');
  if (!role) {
    role = sessionStorage.getItem('role');
  }
  isAdmin.value = role === 'admin' || role === '1';
};

const loadNotifications = async () => {
  const username = localStorage.getItem('username') || sessionStorage.getItem('username');
  if (!username) return;

  try {
    isLoading.value = true;
    const response = await api.get('/api/notifications/list', {
      params: { username }
    });
    if (response.data.code === 200) {
      notifications.value = response.data.data || [];
      unreadCount.value = response.data.unreadCount || 0;
      navUnreadCount.value = response.data.unreadCount || 0;
    }
  } catch (error) {
    console.error('获取通知失败:', error);
  } finally {
    isLoading.value = false;
  }
};

const handleNotificationClick = async (notification: any) => {
  // 先标记已读
  if (!notification.isRead) {
    try {
      await api.post('/api/notifications/read', { id: notification.id });
      notification.isRead = true;
      unreadCount.value = Math.max(0, unreadCount.value - 1);
      navUnreadCount.value = Math.max(0, navUnreadCount.value - 1);
    } catch (error) {
      console.error('标记已读失败:', error);
    }
  }

  // 根据通知类型跳转到对应页面
  if (notification.targetType === 'forum' && notification.targetId) {
    const query: any = { postId: notification.targetId };
    if (notification.subTargetId) {
      query.commentId = notification.subTargetId;
    }
    router.push({ path: '/forum', query });
  } else if (notification.targetType === 'anime' && notification.targetId) {
    router.push({ path: `/anime/${notification.targetId}` });
  }
};

const handleClearAll = () => {
  showClearConfirm.value = true;
};

const confirmClearAll = async () => {
  showClearConfirm.value = false;
  const username = localStorage.getItem('username') || sessionStorage.getItem('username');
  try {
    await api.post('/api/notifications/read-all', { username });
    notifications.value.forEach(n => n.isRead = true);
    unreadCount.value = 0;
    navUnreadCount.value = 0;
  } catch (error) {
    console.error('全部已读失败:', error);
  }
};

const formatTime = (time: string) => {
  if (!time) return '';
  const date = new Date(time);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes} 分钟前`;
  if (hours < 24) return `${hours} 小时前`;
  if (days < 7) return `${days} 天前`;

  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${d} ${h}:${min}`;
};

// 获取通知类型标签
const getTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    FORUM_REPLY: '论坛回复',
    FORUM_LIKE: '论坛点赞',
    FORUM_DISLIKE: '论坛点踩',
    ANIME_REPLY: '动漫回复',
    ANIME_LIKE: '动漫点赞',
    ANIME_DISLIKE: '动漫点踩',
    PASSWORD_RESET: '密码重置'
  };
  return labels[type] || '';
};

onMounted(() => {
  checkAdmin();
  loadNotifications();
});
</script>

<style scoped>
.message-container {
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

/* 内容区域 */
.content {
  max-width: 800px;
  margin: 30px auto;
  padding: 0 20px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.message-header h2 {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.unread-badge {
  background: #ff6b6b;
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
}

.read-all-btn {
  padding: 6px 16px;
  background: white;
  border: 1px solid #ff6b6b;
  color: #ff6b6b;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.read-all-btn:hover {
  background: #ff6b6b;
  color: white;
}

/* 分类标签 */
.category-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
  color: #666;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s;
}

.tab-btn:hover {
  border-color: #ff6b6b;
  color: #ff6b6b;
}

.tab-btn.active {
  background: #ff6b6b;
  border-color: #ff6b6b;
  color: white;
}

.tab-icon {
  font-size: 18px;
  line-height: 1;
}

.clear-all-btn {
  margin-left: auto;
  width: 40px;
  height: 40px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.clear-all-btn:hover {
  border-color: #ff6b6b;
  background: #fff0f0;
}

/* 加载状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #ff6b6b;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-container p {
  font-size: 16px;
  color: #666;
}

/* 空状态 */
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  background: white;
  border-radius: 8px;
}

.empty-text {
  font-size: 16px;
  color: #999;
}

/* 通知列表 */
.notification-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.notification-item {
  display: flex;
  align-items: flex-start;
  padding: 16px 20px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f0f0f0;
  position: relative;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  background: #fafafa;
}

.notification-item.unread {
  background: #fff8f0;
}

.notification-item.unread:hover {
  background: #fff3e6;
}

.notification-icon {
  margin-right: 14px;
  font-size: 22px;
  line-height: 1;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-type {
  display: inline-block;
  font-size: 12px;
  color: #ff6b6b;
  background: #fff0f0;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 6px;
}

.notification-message {
  font-size: 15px;
  color: #333;
  margin: 0 0 6px 0;
  line-height: 1.5;
  word-break: break-all;
}

.notification-time {
  font-size: 13px;
  color: #999;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background: #ff6b6b;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 8px;
  margin-left: 8px;
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
  .navbar-container {
    padding: 0 15px;
  }

  .nav-links li {
    margin-left: 10px;
  }

  .nav-links a {
    font-size: 14px;
  }

  .content {
    padding: 0 15px;
  }
}

/* 确认弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.confirm-modal {
  background: white;
  border-radius: 12px;
  width: 400px;
  max-width: 90%;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.confirm-header {
  padding: 20px 24px 0;
}

.confirm-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.confirm-body {
  padding: 16px 24px 24px;
}

.confirm-body p {
  margin: 0;
  font-size: 15px;
  color: #555;
  line-height: 1.6;
}

.confirm-hint {
  margin-top: 8px !important;
  font-size: 13px !important;
  color: #999 !important;
}

.confirm-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  background: #f9f9f9;
  border-top: 1px solid #eee;
}

.confirm-cancel-btn {
  padding: 8px 20px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: white;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.confirm-cancel-btn:hover {
  background: #f0f0f0;
}

.confirm-ok-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  background: #ff6b6b;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.confirm-ok-btn:hover {
  background: #e55a5a;
}
</style>