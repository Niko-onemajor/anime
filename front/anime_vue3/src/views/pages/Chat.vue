<template>
  <div class="chat-container">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="navbar-container">
        <div class="logo">Niko动漫</div>
        <ul class="nav-links">
          <li><a href="/index">首页</a></li>
          <li><a href="/category">分类</a></li>
          <li><a href="/forum">论坛</a></li>
          <li><a href="/messages">消息</a></li>
          <li><a href="/profile">个人中心</a></li>
        </ul>
      </div>
    </nav>

    <!-- 聊天主体 -->
    <div class="chat-body">
      <!-- 左侧会话列表 -->
      <div class="conversation-sidebar">
        <div class="sidebar-header">
          <h3>会话列表</h3>
        </div>
        <div class="conversation-list" v-if="conversations.length > 0">
          <div
            v-for="conv in conversations"
            :key="conv.userId"
            class="conversation-item"
            :class="{ active: activePartnerId === conv.userId }"
            @click="selectConversation(conv.userId)"
          >
            <div class="conv-avatar">
              <img :src="conv.avatar || defaultAvatar" :alt="conv.username" />
            </div>
            <div class="conv-info">
              <div class="conv-top">
                <span class="conv-username">{{ conv.username }}</span>
                <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
              </div>
              <div class="conv-bottom">
                <span class="conv-preview">{{ conv.lastMessage || '暂无消息' }}</span>
                <span v-if="conv.unreadCount > 0" class="conv-unread">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="no-conversations">
          <p>暂无聊天记录</p>
        </div>
      </div>

      <!-- 右侧聊天区域 -->
      <div class="chat-main">
        <!-- 未选择会话 -->
        <div v-if="!activePartnerId" class="no-chat-selected">
          <div class="no-chat-icon">💬</div>
          <p>请选择一个会话开始聊天</p>
        </div>

        <!-- 已选择会话 -->
        <template v-else>
          <!-- 聊天头部 -->
          <div class="chat-header">
            <div class="chat-header-info">
              <img :src="activePartner?.avatar || defaultAvatar" :alt="activePartner?.username" class="chat-header-avatar" />
              <span class="chat-header-name">{{ activePartner?.username }}</span>
            </div>
          </div>

          <!-- 消息列表 -->
          <div class="message-list" ref="messageListRef">
            <div v-if="messages.length === 0" class="no-messages">
              <p>暂无消息，发送第一条消息吧</p>
            </div>
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-item"
              :class="{ sent: msg.senderId === currentUserId, received: msg.senderId !== currentUserId }"
            >
              <div class="message-bubble">
                <p class="message-content">{{ msg.content }}</p>
                <span class="message-time">{{ formatTime(msg.createTime) }}</span>
              </div>
            </div>
          </div>

          <!-- 输入区域 -->
          <div class="input-area">
            <textarea
              v-model="newMessage"
              class="message-input"
              placeholder="输入消息..."
              rows="3"
              @keydown.enter.exact.prevent="sendMessage"
            ></textarea>
            <button class="send-btn" @click="sendMessage" :disabled="!newMessage.trim()">
              发送
            </button>
          </div>
        </template>
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
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { useRoute } from 'vue-router';
import api from '@/utils/api';
import { ElMessage } from 'element-plus';

const route = useRoute();
const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iI2UwZTBlMCIvPjxjaXJjbGUgY3g9IjIwIiBjeT0iMTUiIHI9IjgiIGZpbGw9IiNjY2MiLz48ZWxsaXBzZSBjeD0iMjAiIGN5PSIzNSIgcng9IjEyIiByeT0iOCIgZmlsbD0iI2NjYyIvPjwvc3ZnPg==';

const currentUserId = ref<number | null>(null);
const currentUsername = ref('');
const activePartnerId = ref<number | null>(null);
const activePartner = ref<any>(null);
const conversations = ref<any[]>([]);
const messages = ref<any[]>([]);
const newMessage = ref('');
const messageListRef = ref<HTMLElement | null>(null);
let pollTimer: ReturnType<typeof setInterval> | null = null;

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

const scrollToBottom = async () => {
  await nextTick();
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
  }
};

const getCurrentUser = async () => {
  const username = localStorage.getItem('username') || sessionStorage.getItem('username');
  if (!username) {
    ElMessage.warning('请先登录');
    return;
  }
  currentUsername.value = username;

  try {
    const res = await api.get('/api/user/info', { params: { username } });
    if (res.data.code === 200) {
      currentUserId.value = res.data.data.id;
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
  }
};

const loadConversations = async () => {
  if (!currentUserId.value) return;
  try {
    const res = await api.get('/api/chat/conversations', {
      params: { userId: currentUserId.value }
    });
    if (res.data.code === 200) {
      conversations.value = res.data.data || [];
    }
  } catch (error) {
    console.error('加载会话列表失败:', error);
  }
};

const selectConversation = async (userId: number) => {
  activePartnerId.value = userId;
  const partner = conversations.value.find(c => c.userId === userId);
  activePartner.value = partner || null;

  if (partner) {
    partner.unreadCount = 0;
  }

  await loadMessages(userId);
  await markAsRead(userId);
};

const loadMessages = async (partnerId?: number) => {
  const targetId = partnerId || activePartnerId.value;
  if (!currentUserId.value || !targetId) return;

  try {
    const res = await api.get('/api/chat/conversation', {
      params: {
        userId1: currentUserId.value,
        userId2: targetId
      }
    });
    if (res.data.code === 200) {
      messages.value = res.data.data || [];
      await scrollToBottom();
    }
  } catch (error) {
    console.error('加载消息失败:', error);
  }
};

const markAsRead = async (partnerId: number) => {
  try {
    await api.post('/api/chat/read', {
      userId: currentUserId.value,
      partnerId: partnerId
    });
  } catch (error) {
    console.error('标记已读失败:', error);
  }
};

const sendMessage = async () => {
  const content = newMessage.value.trim();
  if (!content) return;
  if (!currentUserId.value || !activePartnerId.value) {
    ElMessage.warning('请先选择聊天对象');
    return;
  }

  try {
    const res = await api.post('/api/chat/send', {
      senderId: currentUserId.value,
      receiverId: activePartnerId.value,
      content
    });
    if (res.data.code === 200) {
      newMessage.value = '';
      await loadMessages();
      await loadConversations();
    } else {
      ElMessage.error(res.data.msg || '发送失败');
    }
  } catch (error) {
    console.error('发送消息失败:', error);
    ElMessage.error('发送失败，请重试');
  }
};

const pollMessages = () => {
  if (activePartnerId.value) {
    loadMessages();
  }
  loadConversations();
};

const startPolling = () => {
  pollTimer = setInterval(pollMessages, 5000);
};

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
};

watch(activePartnerId, () => {
  scrollToBottom();
});

onMounted(async () => {
  await getCurrentUser();
  await loadConversations();
  startPolling();

  const queryUser = route.query.user;
  if (queryUser) {
    const userId = Number(queryUser);
    if (!isNaN(userId)) {
      selectConversation(userId);
    }
  }
});

onUnmounted(() => {
  stopPolling();
});
</script>

<style scoped>
.chat-container {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

/* 导航栏样式 */
.navbar {
  background-color: #333;
  color: white;
  padding: 10px 0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  flex-shrink: 0;
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

/* 聊天主体 */
.chat-body {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 20px auto;
  padding: 0 20px;
  display: flex;
  gap: 0;
  min-height: 0;
}

/* 左侧会话列表 */
.conversation-sidebar {
  width: 300px;
  flex-shrink: 0;
  background: white;
  border-radius: 8px 0 0 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid #eee;
}

.sidebar-header {
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f5f5f5;
}

.conversation-item:hover {
  background: #fafafa;
}

.conversation-item.active {
  background: #fff0f0;
}

.conv-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  margin-right: 12px;
  background: #f0f0f0;
}

.conv-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.conv-username {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-time {
  font-size: 11px;
  color: #bbb;
  flex-shrink: 0;
  margin-left: 8px;
}

.conv-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.conv-preview {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.conv-unread {
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
  flex-shrink: 0;
  margin-left: 8px;
}

.no-conversations {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.no-conversations p {
  font-size: 14px;
  color: #999;
  margin: 0;
}

/* 右侧聊天区域 */
.chat-main {
  flex: 1;
  background: white;
  border-radius: 0 8px 8px 0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* 未选择会话 */
.no-chat-selected {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #ccc;
}

.no-chat-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.no-chat-selected p {
  font-size: 16px;
  color: #999;
  margin: 0;
}

/* 聊天头部 */
.chat-header {
  padding: 14px 20px;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
  background: #fafafa;
}

.chat-header-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-header-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  background: #f0f0f0;
}

.chat-header-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

/* 消息列表 */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fafafa;
  scroll-behavior: smooth;
}

.no-messages {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.no-messages p {
  font-size: 14px;
  color: #bbb;
  margin: 0;
}

.message-item {
  display: flex;
  margin-bottom: 16px;
}

.message-item.sent {
  justify-content: flex-end;
}

.message-item.received {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 60%;
  padding: 10px 14px;
  border-radius: 12px;
  position: relative;
}

.message-item.sent .message-bubble {
  background: #ff6b6b;
  color: white;
  border-bottom-right-radius: 4px;
}

.message-item.received .message-bubble {
  background: #f0f0f0;
  color: #333;
  border-bottom-left-radius: 4px;
}

.message-content {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-all;
}

.message-time {
  display: block;
  font-size: 11px;
  margin-top: 4px;
  opacity: 0.7;
  text-align: right;
}

.message-item.received .message-time {
  color: #999;
}

/* 输入区域 */
.input-area {
  display: flex;
  gap: 10px;
  padding: 12px 20px;
  border-top: 1px solid #eee;
  background: white;
  flex-shrink: 0;
  align-items: flex-end;
}

.message-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  resize: none;
  outline: none;
  font-family: inherit;
  transition: border-color 0.3s;
  line-height: 1.5;
}

.message-input:focus {
  border-color: #ff6b6b;
}

.send-btn {
  padding: 10px 24px;
  background: #ff6b6b;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s;
  white-space: nowrap;
  flex-shrink: 0;
}

.send-btn:hover {
  background: #e55a5a;
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* 底栏 */
.footer {
  background: #333;
  color: #fff;
  padding: 20px 0;
  flex-shrink: 0;
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

/* 响应式 */
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

  .chat-body {
    padding: 0 10px;
    margin: 10px auto;
    flex-direction: column;
  }

  .conversation-sidebar {
    width: 100%;
    max-height: 250px;
    border-radius: 8px 8px 0 0;
    border-right: none;
    border-bottom: 1px solid #eee;
  }

  .chat-main {
    border-radius: 0 0 8px 8px;
  }

  .message-bubble {
    max-width: 80%;
  }
}
</style>