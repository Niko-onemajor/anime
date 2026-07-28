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
      </div>

      <!-- 分类标签 -->
      <div class="category-tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'dm' }"
          @click="activeTab = 'dm'"
        >
          <span class="tab-icon">✉️</span>
          <span>私信</span>
          <span v-if="dmUnreadCount > 0" class="tab-unread">{{ dmUnreadCount }}</span>
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'comment' }"
          @click="activeTab = 'comment'"
        >
          <span class="tab-icon">💬</span>
          <span>评论</span>
          <span v-if="commentUnreadCount > 0" class="tab-unread">{{ commentUnreadCount }}</span>
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'like' }"
          @click="activeTab = 'like'"
        >
          <span class="tab-icon">👍</span>
          <span>点赞</span>
          <span v-if="likeUnreadCount > 0" class="tab-unread">{{ likeUnreadCount }}</span>
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'dislike' }"
          @click="activeTab = 'dislike'"
        >
          <span class="tab-icon">👎</span>
          <span>点踩</span>
          <span v-if="dislikeUnreadCount > 0" class="tab-unread">{{ dislikeUnreadCount }}</span>
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
      <div v-else-if="currentPageData.length === 0" class="empty-container">
        <p class="empty-text">{{ emptyText }}</p>
      </div>

      <!-- 通知列表 -->
      <div v-else class="notification-list">
        <div
          v-for="notification in currentPageData"
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
            <span v-else class="icon-default">✉️</span>
          </div>
          <div class="notification-content">
            <span v-if="getTypeLabel(notification.type)" class="notification-type">{{ getTypeLabel(notification.type) }}</span>
            <p class="notification-message">{{ notification.message }}</p>
            <span class="notification-time">{{ formatTime(notification.createTime) }}</span>
          </div>
          <div v-if="!notification.isRead" class="unread-dot"></div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="totalPages > 1">
        <button @click="goToPage(1)" class="page-btn" :disabled="currentPage === 1">首页</button>
        <button @click="goToPage(currentPage - 1)" class="page-btn" :disabled="currentPage === 1">上一页</button>
        <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
        <button @click="goToPage(currentPage + 1)" class="page-btn" :disabled="currentPage === totalPages">下一页</button>
        <button @click="goToPage(totalPages)" class="page-btn" :disabled="currentPage === totalPages">尾页</button>
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
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/utils/api';

const router = useRouter();
const isAdmin = ref(false);
const isLoading = ref(true);
const activeTab = ref('dm');
const navUnreadCount = ref(0);
const notifications = ref<any[]>([]);
const showClearConfirm = ref(false);
const currentPage = ref(1);
const pageSize = 6;
const currentUserId = ref<number | null>(null);

// 加载聊天会话列表用于私信tab
const loadDmConversations = async () => {
  if (!currentUserId.value) return;
  try {
    const res = await api.get('/api/chat/conversations', {
      params: { userId: currentUserId.value }
    });
    if (res.data.code === 200) {
      const conversations = res.data.data || [];
      // 转换为通知格式以便在列表中显示
      const dmNotifications = conversations.map((conv: any) => ({
        id: 'dm-' + conv.userId,
        type: 'DM',
        message: conv.lastMessage || '暂无消息',
        createTime: conv.lastMessageTime || new Date().toISOString(),
        isRead: conv.unreadCount === 0,
        senderId: conv.userId,
        senderName: conv.username,
        senderAvatar: conv.avatar
      }));
      // 合并：保留非DM的通知 + DM会话
      const nonDmNotifications = notifications.value.filter(n =>
        n.type === 'FORUM_REPLY' || n.type === 'FORUM_LIKE' || n.type === 'FORUM_DISLIKE' ||
        n.type === 'ANIME_REPLY' || n.type === 'ANIME_LIKE' || n.type === 'ANIME_DISLIKE'
      );
      notifications.value = [...nonDmNotifications, ...dmNotifications];
    }
  } catch (error) {
    console.error('加载私信会话失败:', error);
  }
};

const filteredNotifications = computed(() => {
  if (activeTab.value === 'dm') {
    return notifications.value.filter(n =>
      n.type !== 'FORUM_REPLY' && n.type !== 'FORUM_LIKE' && n.type !== 'FORUM_DISLIKE' &&
      n.type !== 'ANIME_REPLY' && n.type !== 'ANIME_LIKE' && n.type !== 'ANIME_DISLIKE'
    );
  } else if (activeTab.value === 'comment') {
    return notifications.value.filter(n => n.type === 'FORUM_REPLY' || n.type === 'ANIME_REPLY');
  } else if (activeTab.value === 'like') {
    return notifications.value.filter(n => n.type === 'FORUM_LIKE' || n.type === 'ANIME_LIKE');
  } else {
    return notifications.value.filter(n => n.type === 'FORUM_DISLIKE' || n.type === 'ANIME_DISLIKE');
  }
});

const totalPages = computed(() => Math.ceil(filteredNotifications.value.length / pageSize) || 1);

const currentPageData = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredNotifications.value.slice(start, start + pageSize);
});

const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
  }
};

// 各分类未读数
const dmUnreadCount = computed(() =>
  notifications.value.filter(n =>
    !n.isRead && n.type !== 'FORUM_REPLY' && n.type !== 'FORUM_LIKE' && n.type !== 'FORUM_DISLIKE' &&
    n.type !== 'ANIME_REPLY' && n.type !== 'ANIME_LIKE' && n.type !== 'ANIME_DISLIKE'
  ).length
);

const commentUnreadCount = computed(() =>
  notifications.value.filter(n => !n.isRead && (n.type === 'FORUM_REPLY' || n.type === 'ANIME_REPLY')).length
);

const likeUnreadCount = computed(() =>
  notifications.value.filter(n => !n.isRead && (n.type === 'FORUM_LIKE' || n.type === 'ANIME_LIKE')).length
);

const dislikeUnreadCount = computed(() =>
  notifications.value.filter(n => !n.isRead && (n.type === 'FORUM_DISLIKE' || n.type === 'ANIME_DISLIKE')).length
);

const emptyText = computed(() => {
  const map: Record<string, string> = {
    dm: '暂无私信',
    comment: '暂无评论消息',
    like: '暂无点赞消息',
    dislike: '暂无点踩消息'
  };
  return map[activeTab.value] || '暂无消息';
});

// 切换 tab 时重置页码
watch(activeTab, () => {
  currentPage.value = 1;
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
    // 获取当前用户ID
    const userRes = await api.get('/api/user/info', { params: { username } });
    if (userRes.data.code === 200) {
      currentUserId.value = userRes.data.data.id;
    }
    
    const response = await api.get('/api/notifications/list', {
      params: { username }
    });
    if (response.data.code === 200) {
      notifications.value = response.data.data || [];
      navUnreadCount.value = response.data.unreadCount || 0;
      // 始终同步通知数据（清理孤儿通知 + 补充遗漏的通知）
      try {
        const syncRes = await api.post('/api/notifications/sync');
        if (syncRes.data.code === 200) {
          // 如果有清理或新增，重新加载通知列表
          if (syncRes.data.count > 0 || syncRes.data.cleanupCount > 0 || syncRes.data.fixCount > 0) {
            const reloadRes = await api.get('/api/notifications/list', { params: { username } });
            if (reloadRes.data.code === 200) {
              notifications.value = reloadRes.data.data || [];
              navUnreadCount.value = reloadRes.data.unreadCount || 0;
            }
          }
        }
      } catch (e) {
        console.error('同步通知失败:', e);
      }
    }
    // 加载私信会话
    await loadDmConversations();
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
      navUnreadCount.value = Math.max(0, navUnreadCount.value - 1);
    } catch (error) {
      console.error('标记已读失败:', error);
    }
  }

  // 私信消息跳转到聊天页面
  if (notification.type === 'DM') {
    router.push({ path: '/chat', query: { user: notification.senderId } });
    return;
  }

  // 根据通知类型跳转到对应页面
  // 多层 fallback 确保 targetType 一定被正确推断
  let targetType = notification.targetType;
  const type = (notification.type || '').toUpperCase();
  
  // 1. 如果 targetType 缺失，从 type 字段推断
  if (!targetType) {
    if (type.startsWith('ANIME')) {
      targetType = 'anime';
    } else if (type.startsWith('FORUM')) {
      targetType = 'forum';
    }
  }
  
  // 2. 如果 targetType 值为空字符串，同样视为缺失
  if (targetType && typeof targetType === 'string' && targetType.trim() === '') {
    targetType = null;
    if (type.startsWith('ANIME')) {
      targetType = 'anime';
    } else if (type.startsWith('FORUM')) {
      targetType = 'forum';
    }
  }
  
  // 3. 最后兜底：从 message 内容推断（消息中包含"动漫"或"论坛"关键词）
  if (!targetType && notification.message) {
    if (notification.message.includes('动漫')) {
      targetType = 'anime';
    } else if (notification.message.includes('论坛')) {
      targetType = 'forum';
    }
  }
  
  console.log('通知跳转:', { type: notification.type, targetType, targetId: notification.targetId, subTargetId: notification.subTargetId, message: notification.message?.substring(0, 30) });
  
  if (targetType === 'forum' && notification.targetId) {
    const query: any = { postId: notification.targetId };
    if (notification.subTargetId) {
      query.commentId = notification.subTargetId;
    } else {
      // 没有 subTargetId 说明是帖子级别的点赞/点踩，高亮帖子本身
      query.highlightPost = 'true';
    }
    console.log('[Message] 跳转到论坛: postId=' + notification.targetId + ', commentId=' + (notification.subTargetId || '无'));
    router.push({ path: '/forum', query });
  } else if (targetType === 'anime' && notification.targetId) {
    const query: any = {};
    if (notification.subTargetId) {
      query.commentId = notification.subTargetId;
    }
    console.log('[Message] 跳转到动漫播放页: animeId=' + notification.targetId + ', commentId=' + (notification.subTargetId || '无'));
    router.push({ path: `/anime/${notification.targetId}/play/1`, query });
  } else {
    console.warn('[Message] 无法确定通知跳转目标, 完整通知数据:', JSON.stringify(notification));
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
  position: relative;
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

.tab-btn.active .tab-unread {
  background: white;
  color: #ff6b6b;
}

.tab-icon {
  font-size: 18px;
  line-height: 1;
}

.tab-unread {
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

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
  padding: 12px 0;
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