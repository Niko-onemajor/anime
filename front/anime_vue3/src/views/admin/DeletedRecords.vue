<template>
  <div class="admin-container">
    <nav class="navbar">
      <div class="navbar-container">
        <div class="logo">Niko动漫 - 管理员</div>
        <ul class="nav-links">
          <li><a href="/admin/users">用户管理</a></li>
          <li><a href="/admin/animes">动漫管理</a></li>
          <li><a href="/admin/forum">论坛管理</a></li>
          <li><a href="/admin/deleted" class="active">删除记录管理</a></li>
          <li><a href="/index">返回前台</a></li>
        </ul>
      </div>
    </nav>

    <div class="container">
      <h2>删除记录管理</h2>
      
      <div class="tabs">
        <button 
          v-for="tab in tabs" 
          :key="tab.value"
          :class="{ active: activeTab === tab.value }"
          @click="activeTab = tab.value; loadDeletedRecords()"
          class="tab-btn"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="actions">
        <div class="search-box">
          <input type="text" v-model="searchKeyword" placeholder="搜索..." class="search-input">
          <button @click="loadDeletedRecords()" class="search-btn">搜索</button>
        </div>
        <div class="sort-box">
          <label for="sort">排序方式:</label>
          <select id="sort" v-model="sortBy" @change="loadDeletedRecords()" class="sort-select">
            <option value="newest">最新删除</option>
            <option value="oldest">最早删除</option>
          </select>
        </div>
      </div>

      <div class="deleted-records">
        <div class="record-cards" v-if="records.length > 0">
          <div v-for="record in pagedRecords" :key="record.id" class="record-card">
            <div class="record-card-header">
              <div class="record-card-title">
                <div v-if="activeTab === 'users' && record.avatar" class="record-card-image user-avatar">
                  <img :src="getImageUrl(record.avatar)" :alt="record.username">
                </div>
                <div v-else-if="(activeTab === 'animes' || activeTab === 'episodes') && (record.image || record.animeImage)" class="record-card-image">
                  <img :src="getImageUrl(activeTab === 'animes' ? record.image : record.animeImage)" :alt="getRecordTitle(record)">
                </div>
                <h3>{{ getRecordTitle(record) }}</h3>
              </div>
              <p class="delete-time">删除时间: {{ formatDeleteTime(record.deletedAt) }}</p>
            </div>
            <div class="record-card-body">
              <div class="record-detail" v-if="activeTab === 'users'">
                <span class="detail-label">ID:</span>
                <span class="detail-value">{{ record.id }}</span>
              </div>
              <div class="record-detail" v-if="activeTab === 'users'">
                <span class="detail-label">角色:</span>
                <span class="detail-value">{{ record.role === 1 ? '管理员' : '普通用户' }}</span>
              </div>
              <div class="record-detail" v-if="activeTab === 'users'">
                <span class="detail-label">邮箱:</span>
                <span class="detail-value">{{ record.email || '-' }}</span>
              </div>
            </div>
            <div class="record-card-actions">
              <button @click="restoreRecord(record.id)" class="restore-btn">恢复</button>
              <button @click="hardDeleteRecord(record.id)" class="hard-delete-btn">彻底删除</button>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <p>暂无删除记录</p>
        </div>
      </div>

      <!-- 分页导航 -->
      <div class="pagination" v-if="recordTotalPages > 1">
        <button @click="recordPage = 1" :disabled="recordPage === 1" class="page-btn">首页</button>
        <button @click="recordPage = recordPage - 1" :disabled="recordPage === 1" class="page-btn">上一页</button>
        <span 
          v-for="p in recordPageNumbers" 
          :key="p" 
          class="page-num" 
          :class="{ active: p === recordPage }"
          @click="recordPage = p"
        >{{ p }}</span>
        <button @click="recordPage = recordPage + 1" :disabled="recordPage === recordTotalPages" class="page-btn">下一页</button>
        <button @click="recordPage = recordTotalPages" :disabled="recordPage === recordTotalPages" class="page-btn">尾页</button>
        <span class="page-total">共 {{ recordTotalPages }} 页 / {{ records.length }} 条</span>
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
import { ref, onMounted, computed } from 'vue';
import api from '@/utils/api';
import { ElMessage, ElMessageBox } from 'element-plus';

// 处理图片URL
const getImageUrl = (url: string | undefined | null): string => {
  if (!url) {
    return '';
  }
  console.log('原始图片URL:', url);
  // 如果是完整的URL，直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    console.log('完整URL，直接返回:', url);
    return url;
  }
  // 如果是本地文件路径（Windows或Unix风格），转换为HTTP URL
  if (url.includes(':')) {
    // 提取文件名，同时处理正斜杠和反斜杠
    const parts = url.split(/[\\/]/);
    const filename = parts.pop();
    // 根据文件路径判断是头像还是封面
    if (url.includes('avatars')) {
      const result = 'http://localhost:8080/avatars/' + filename;
      console.log('本地路径转换结果:', result);
      return result;
    } else if (url.includes('covers')) {
      const result = 'http://localhost:8080/covers/' + filename;
      console.log('本地路径转换结果:', result);
      return result;
    }
  }
  // 如果是相对路径，转换为完整的URL
  if (url.startsWith('/')) {
    const result = 'http://localhost:8080' + url;
    console.log('相对路径转换结果:', result);
    return result;
  }
  // 默认返回原路径
  console.log('默认返回原路径:', url);
  return url;
};

const activeTab = ref('users');
const records = ref<any[]>([]);

// 分页
const recordPage = ref(1);
const recordPageSize = 6;
const recordTotalPages = computed(() => Math.ceil(records.value.length / recordPageSize) || 1);
const pagedRecords = computed(() => {
  const start = (recordPage.value - 1) * recordPageSize;
  return records.value.slice(start, start + recordPageSize);
});
const recordPageNumbers = computed(() => {
  const total = recordTotalPages.value;
  const curr = recordPage.value;
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

const searchKeyword = ref('');
const sortBy = ref('newest');

const tabs = [
  { label: '用户', value: 'users' },
  { label: '动漫', value: 'animes' },
  { label: '集数', value: 'episodes' }
];

const loadDeletedRecords = async () => {
  try {
    let res;
    switch (activeTab.value) {
      case 'users':
        res = await api.post('http://localhost:8080/api/admin/users/deleted');
        break;
      case 'animes':
        res = await api.post('http://localhost:8080/api/admin/animes/deleted');
        break;
      case 'episodes':
        res = await api.post('http://localhost:8080/api/admin/episodes/deleted');
        break;
      default:
        return;
    }
    
    if (res.data.code === 200) {
      let data = res.data.data;
      
      // 搜索
      if (searchKeyword.value) {
        data = data.filter((record: any) => {
          switch (activeTab.value) {
            case 'users':
              return record.username.toLowerCase().includes(searchKeyword.value.toLowerCase());
            case 'animes':
              return record.title.toLowerCase().includes(searchKeyword.value.toLowerCase());
            case 'episodes':
              return (record.animeTitle || '').toLowerCase().includes(searchKeyword.value.toLowerCase()) || 
                     record.episodeNumber.toString().includes(searchKeyword.value);
            default:
              return true;
          }
        });
      }
      
      // 排序
      data.sort((a: any, b: any) => {
        const dateA = new Date(a.deletedAt).getTime();
        const dateB = new Date(b.deletedAt).getTime();
        return sortBy.value === 'newest' ? dateB - dateA : dateA - dateB;
      });
      
      records.value = data;
      recordPage.value = 1;
    }
  } catch (error) {
    console.error('加载删除记录失败：', error);
  }
};

const restoreRecord = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要恢复该记录吗？',
      '确认恢复',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info',
      }
    );
    
    let res;
    switch (activeTab.value) {
      case 'users':
        res = await api.post('http://localhost:8080/api/admin/users/restore', { id });
        break;
      case 'animes':
        res = await api.post('http://localhost:8080/api/admin/animes/restore', { id });
        break;
      case 'episodes':
        res = await api.post('http://localhost:8080/api/admin/episodes/restore', { id });
        break;
      default:
        return;
    }
    
    if (res.data.code === 200) {
      ElMessage.success('恢复成功！');
      loadDeletedRecords();
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('恢复记录失败：', error);
    }
  }
};

const hardDeleteRecord = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要彻底删除该记录吗？此操作不可恢复！',
      '确认彻底删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    
    let res;
    switch (activeTab.value) {
      case 'users':
        res = await api.post('http://localhost:8080/api/admin/users/hardDelete', { id });
        break;
      case 'animes':
        res = await api.post('http://localhost:8080/api/admin/animes/hardDelete', { id });
        break;
      case 'episodes':
        res = await api.post('http://localhost:8080/api/admin/episodes/hardDelete', { id });
        break;
      default:
        return;
    }
    
    if (res.data.code === 200) {
      ElMessage.success('彻底删除成功！');
      loadDeletedRecords();
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('彻底删除记录失败：', error);
    }
  }
};

const getRecordTitle = (record: any): string => {
  switch (activeTab.value) {
    case 'users':
      return record.username;
    case 'animes':
      return record.title;
    case 'episodes':
      return `${record.animeTitle || '未知动漫'} - 第${record.episodeNumber}集`;
    default:
      return '';
  }
};

const formatDeleteTime = (deletedAt: string): string => {
  if (!deletedAt) return '-';
  const date = new Date(deletedAt);
  return date.toLocaleString();
};

onMounted(() => {
  loadDeletedRecords();
});
</script>

<style scoped>
.admin-container {
  min-height: 100vh;
  background: #f5f5f5;
}

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

.tabs {
  display: flex;
  margin-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.tab-btn {
  padding: 10px 20px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 16px;
  color: #666;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.tab-btn:hover {
  color: #ff6b6b;
}

.tab-btn.active {
  color: #ff6b6b;
  border-bottom-color: #ff6b6b;
  font-weight: 500;
}

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
  min-width: 150px;
}

.sort-box label {
  font-weight: bold;
  color: #666;
  font-size: 14px;
}

.deleted-records {
  margin-top: 20px;
}

.record-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.record-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  padding: 20px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.record-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0,0,0,0.1);
}

.record-card-header {
  margin-bottom: 15px;
  padding-bottom: 15px;
}

.record-card-header h3 {
  margin: 0 0 5px 0;
  color: #333;
  font-size: 18px;
}

.record-card-title {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
  width: 100%;
}

.record-card-image {
  width: 100%;
  max-width: 100%;
  aspect-ratio: 2/3;
  overflow: hidden;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  display: block;
  padding: 0;
  margin: 0;
}

.record-card-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  margin: 0;
  padding: 0;
  transition: transform 0.3s;
}

.record-card-image.user-avatar {
  width: 80px;
  height: 80px;
  aspect-ratio: 1/1;
  border-radius: 50%;
}

.record-card:hover .record-card-image img {
  transform: scale(1.05);
}

.delete-time {
  margin: 0;
  color: #999;
  font-size: 14px;
}

.record-card-body {
  margin-bottom: 15px;
}

.record-detail {
  display: flex;
  margin-bottom: 8px;
  align-items: flex-start;
}

.detail-label {
  font-weight: bold;
  color: #666;
  width: 90px;
  flex-shrink: 0;
}

.detail-value {
  color: #333;
  flex: 1;
  word-break: break-word;
}

.record-card-actions {
  display: flex;
  gap: 10px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.restore-btn {
  flex: 1;
  padding: 8px 16px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.restore-btn:hover {
  background: #45a049;
}

.hard-delete-btn {
  flex: 1;
  padding: 8px 16px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.hard-delete-btn:hover {
  background: #d32f2f;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 18px;
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
  
  .tabs {
    flex-wrap: wrap;
  }
  
  .tab-btn {
    margin-right: 10px;
    margin-bottom: 10px;
  }
  
  .record-cards {
    grid-template-columns: 1fr;
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