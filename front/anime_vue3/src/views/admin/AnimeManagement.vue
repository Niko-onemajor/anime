<template>
  <div class="admin-container">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="navbar-container">
        <div class="logo">Niko动漫 - 管理员</div>
        <ul class="nav-links">
          <li><a href="/admin/users">用户管理</a></li>
          <li><a href="/admin/animes" class="active">动漫管理</a></li>
          <li><a href="/admin/forum">论坛管理</a></li>
          <li><a href="/admin/deleted">删除记录管理</a></li>
          <li><a href="/index">返回前台</a></li>
        </ul>
      </div>
    </nav>

    <!-- 内容区域 -->
    <div class="container">
      <h2>动漫管理</h2>
      
      <!-- 搜索和添加动漫 -->
      <div class="actions">
        <div class="search-box">
          <input type="text" v-model="searchKeyword" placeholder="搜索动漫标题..." class="search-input">
          <button @click="searchAnimes" class="search-btn">搜索</button>
        </div>
        <div class="sort-box">
          <label>排序方式：</label>
          <span 
            v-for="sort in animeSortOptions" 
            :key="sort.value" 
            class="sort-option" 
            :class="{ active: animeSortBy === sort.value }"
            @click="selectAnimeSort(sort.value)"
          >
            {{ sort.label }}
          </span>
          <button @click="toggleAnimeSortDirection" class="sort-direction-btn">
            {{ animeSortDirection === 'desc' ? '降序' : '升序' }}
          </button>
        </div>
        <button @click="handleAddAnime" class="add-btn">添加动漫</button>
      </div>

      <!-- 动漫列表 -->
      <div class="anime-list">
        <div class="anime-grid">
          <div v-for="anime in pagedAnimes" :key="anime.id" class="anime-item">
            <div class="anime-image">
              <img :src="getImageUrl(anime.image)" :alt="anime.title" class="anime-poster">
              <div class="anime-status" :class="{ 'active': anime.status === 1 }">
                {{ anime.status === 1 ? '已上架' : '已下架' }}
              </div>
            </div>
            <div class="anime-info">
              <div class="anime-title-row">
                <h3 class="anime-title">{{ anime.title }}</h3>
                <span class="anime-year">{{ anime.year }}</span>
              </div>
              <div class="anime-meta-row">
                <span class="anime-rating">评分: {{ anime.rating }}</span>
                <span class="anime-views">观看: {{ anime.viewCount || 0 }}</span>
              </div>
              <div class="anime-actions">
                <button @click="toggleStatus(anime)" class="status-btn">
                  {{ anime.status === 1 ? '下架' : '上架' }}
                </button>
                <button @click="editAnime(anime)" class="edit-btn">编辑</button>
                <button @click="manageComments(anime)" class="comment-btn">评论管理</button>
                <button @click="showDeleteConfirm(anime.id)" class="delete-btn">删除</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页导航 -->
      <div class="pagination" v-if="animeTotalPages > 1">
        <button @click="animePage = 1" :disabled="animePage === 1" class="page-btn">首页</button>
        <button @click="animePage = animePage - 1" :disabled="animePage === 1" class="page-btn">上一页</button>
        <span 
          v-for="p in animePageNumbers" 
          :key="p" 
          class="page-num" 
          :class="{ active: p === animePage }"
          @click="animePage = p"
        >{{ p }}</span>
        <button @click="animePage = animePage + 1" :disabled="animePage === animeTotalPages" class="page-btn">下一页</button>
        <button @click="animePage = animeTotalPages" :disabled="animePage === animeTotalPages" class="page-btn">尾页</button>
        <span class="page-total">共 {{ animeTotalPages }} 页 / {{ animes.length }} 条</span>
      </div>

      <!-- 添加/编辑动漫对话框 -->
      <el-dialog
        v-model="showAnimeDialog"
        :title="showEditAnimeDialog ? '编辑动漫' : '添加动漫'"
        width="90%"
        :close-on-click-modal="false"
      >
        <div class="dialog-body">
          <div class="form-group">
            <label for="title">标题</label>
            <input type="text" id="title" v-model="animeForm.title" placeholder="请输入动漫标题" class="form-input">
          </div>
          <div class="form-group">
            <label for="description">描述</label>
            <textarea id="description" v-model="animeForm.description" placeholder="请输入动漫描述" class="form-textarea"></textarea>
          </div>
          <div class="form-group">
            <label for="image">封面图</label>
            <div v-if="animeForm.image" class="image-url-display">
              <span class="image-url-text">{{ animeForm.image }}</span>
            </div>
            <input type="file" id="image" @change="handleImageUpload" class="form-input">
            <div v-if="animeForm.image" class="image-preview">
              <img :src="getImageUrl(animeForm.image)" :alt="animeForm.title" class="preview-img">
            </div>
          </div>
          <div class="form-group">
            <label for="year">年份</label>
            <input type="text" id="year" v-model="animeForm.year" placeholder="请输入年份" class="form-input">
          </div>
          <div class="form-group">
            <label for="rating">评分</label>
            <input type="number" id="rating" v-model="animeForm.rating" placeholder="由用户评分自动计算" class="form-input" step="0.1" min="0" max="10" readonly>
          </div>
          <div class="form-group">
            <label for="letter">首字母</label>
            <input type="text" id="letter" v-model="animeForm.letter" placeholder="请输入首字母" class="form-input" maxlength="1">
          </div>
          <div class="form-group">
            <label for="genre">标签</label>
            <input type="text" id="genre" v-model="animeForm.genre" placeholder="请输入标签，多个标签用逗号分隔" class="form-input">
          </div>
          <div class="form-group">
            <label for="status">状态</label>
            <select id="status" v-model="animeForm.status" class="form-input">
              <option value="1">上架</option>
              <option value="0">下架</option>
            </select>
          </div>
          
          <!-- 集数管理 -->
          <div class="form-group">
            <label>集数管理</label>
            <div v-for="(episode, index) in animeForm.episodes" :key="index" class="episode-item">
              <div class="episode-fields">
                <input type="number" v-model="episode.episodeNumber" placeholder="集数" class="form-input episode-number">
                <div v-if="episode.videoUrl" class="video-url-display">
                  <span class="video-url-text">{{ episode.videoUrl }}</span>
                  <input type="file" @change="(e) => handleVideoUpload(e, index)" class="form-input episode-video">
                  <span class="video-url-hint">（点击重新上传）</span>
                </div>
                <input v-else type="file" @change="(e) => handleVideoUpload(e, index)" class="form-input episode-video">
                <button @click="removeEpisode(index)" class="remove-episode-btn">删除</button>
              </div>
              <div v-if="episode.videoUrl" class="video-preview">
                <video :src="getVideoUrl(episode.videoUrl)" controls class="preview-video"></video>
              </div>
            </div>
            <button @click="addEpisode" class="add-episode-btn">添加集数</button>
          </div>
        </div>
        <template #footer>
          <div class="dialog-footer">
            <button @click="closeDialog" class="btn cancel-btn">取消</button>
            <button @click="submitAnimeForm" class="btn submit-btn">{{ showEditAnimeDialog ? '保存' : '添加' }}</button>
          </div>
        </template>
      </el-dialog>

      <!-- 评论管理对话框 -->
      <el-dialog
        v-model="showCommentManagementDialog"
        :title="`评论管理 - ${currentAnime?.title}`"
        width="90%"
        :close-on-click-modal="false"
      >
        <div class="dialog-body">
          <!-- 搜索框 -->
          <div class="comment-search-box" v-if="comments.length > 0">
            <input type="text" v-model="commentSearchKeyword" placeholder="搜索评论内容或用户名..." class="search-input" @input="commentPage = 1">
          </div>
          <div v-if="filteredComments.length === 0" class="no-comments">
            <p>{{ comments.length === 0 ? '暂无评论' : '未找到匹配的评论' }}</p>
          </div>
          <div v-else class="comments-list">
            <div v-for="comment in pagedComments" :key="comment.id" class="comment-item">
              <div class="comment-header">
                <div class="comment-author">
                  <img :src="getImageUrl(comment.author?.avatar)" :alt="comment.author?.username" class="author-avatar">
                  <span class="author-name">{{ comment.author?.username }}</span>
                </div>
                <div class="comment-time">{{ formatDate(comment.createTime) }}</div>
              </div>
              <div class="comment-content" :class="{ 'deleted': comment.deleted }">
                {{ comment.deleted ? '该评论已被删除' : comment.content }}
              </div>
              <div class="comment-actions">
                <span class="comment-stats">
                  点赞: {{ comment.likeCount }} | 点踩: {{ comment.dislikeCount }}
                </span>
                <button @click="deleteComment(comment.id)" class="delete-comment-btn">删除</button>
              </div>
              <!-- 回复列表 -->
              <div v-if="comment.replies && comment.replies.length > 0" class="replies-list">
                <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                  <div class="reply-header">
                    <div class="reply-author">
                  <img :src="getImageUrl(reply.author?.avatar)" :alt="reply.author?.username" class="author-avatar">
                  <span class="author-name">{{ reply.author?.username }}</span>
                </div>
                    <div class="reply-time">{{ formatDate(reply.createTime) }}</div>
                  </div>
                  <div class="reply-content" :class="{ 'deleted': reply.deleted }">
                    {{ reply.deleted ? '该评论已被删除' : reply.content }}
                  </div>
                  <div class="reply-actions">
                    <span class="reply-stats">
                      点赞: {{ reply.likeCount }} | 点踩: {{ reply.dislikeCount }}
                    </span>
                    <button @click="deleteComment(reply.id)" class="delete-comment-btn">删除</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 评论分页 -->
          <div class="comment-pagination" v-if="commentTotalPages > 1">
            <button @click="commentPage = 1" :disabled="commentPage === 1" class="page-btn">首页</button>
            <button @click="commentPage = commentPage - 1" :disabled="commentPage === 1" class="page-btn">上一页</button>
            <span 
              v-for="p in commentPageNumbers" 
              :key="p" 
              class="page-num" 
              :class="{ active: p === commentPage }"
              @click="commentPage = p"
            >{{ p }}</span>
            <button @click="commentPage = commentPage + 1" :disabled="commentPage === commentTotalPages" class="page-btn">下一页</button>
            <button @click="commentPage = commentTotalPages" :disabled="commentPage === commentTotalPages" class="page-btn">尾页</button>
            <span class="page-total">共 {{ commentTotalPages }} 页 / {{ filteredComments.length }} 条</span>
          </div>
        </div>
        <template #footer>
          <div class="dialog-footer">
            <button @click="closeCommentManagementDialog" class="btn cancel-btn">关闭</button>
          </div>
        </template>
      </el-dialog>
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
import { ElMessageBox, ElMessage, ElDialog } from 'element-plus';

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
    // 检查是否包含avatar相关路径
    const lowerUrl = url.toLowerCase();
    const result = lowerUrl.includes('avatar') ? 
      'http://localhost:8080/avatars/' + filename : 
      'http://localhost:8080/covers/' + filename;
    console.log('本地路径转换结果:', result);
    return result;
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

// 处理视频URL
const getVideoUrl = (url: string | undefined | null): string => {
  if (!url) {
    return '';
  }
  console.log('原始视频URL:', url);
  // 如果是完整的URL，直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    console.log('完整URL，直接返回:', url);
    return url;
  }
  // 如果是本地文件路径（Windows或Unix风格），转换为HTTP URL
  if (url.includes(':')) {
    // 提取文件名和子路径，同时处理正斜杠和反斜杠
    const parts = url.split(/[\\/]/);
    const filename = parts.pop();
    // 找到最后一个mp4目录的位置
    const mp4Index = parts.lastIndexOf('mp4');
    if (mp4Index !== -1) {
      const subPath = parts.slice(mp4Index + 1).join('/');
      if (subPath) {
        const result = 'http://localhost:8080/videos/' + subPath + '/' + filename;
        console.log('本地路径转换结果:', result);
        return result;
      } else {
        const result = 'http://localhost:8080/videos/' + filename;
        console.log('本地路径转换结果:', result);
        return result;
      }
    } else {
      // 如果没有找到mp4目录，只返回文件名
      const result = 'http://localhost:8080/videos/' + filename;
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

// 搜索关键词
const searchKeyword = ref('');

// 动漫列表
const animes = ref<any[]>([]);

// 排序
const animeSortBy = ref('year');
const animeSortDirection = ref('desc');
const animeSortOptions = [
  { label: '年份', value: 'year' },
  { label: '评分', value: 'rating' },
  { label: '热度', value: 'views' }
];

const selectAnimeSort = (sortBy: string) => {
  animeSortBy.value = sortBy;
  animePage.value = 1;
};

const toggleAnimeSortDirection = () => {
  animeSortDirection.value = animeSortDirection.value === 'desc' ? 'asc' : 'desc';
  animePage.value = 1;
};

const sortedAnimes = computed(() => {
  const list = [...animes.value];
  const dir = animeSortDirection.value === 'desc' ? -1 : 1;
  switch (animeSortBy.value) {
    case 'year':
      return list.sort((a, b) => dir * ((a.year || 0) - (b.year || 0)));
    case 'rating':
      return list.sort((a, b) => dir * ((a.rating || 0) - (b.rating || 0)));
    case 'views':
      return list.sort((a, b) => dir * ((a.viewCount || 0) - (b.viewCount || 0)));
    default:
      return list;
  }
});

// 分页
const animePage = ref(1);
const animePageSize = 8;
const animeTotalPages = computed(() => Math.ceil(sortedAnimes.value.length / animePageSize) || 1);
const pagedAnimes = computed(() => {
  const start = (animePage.value - 1) * animePageSize;
  return sortedAnimes.value.slice(start, start + animePageSize);
});
const animePageNumbers = computed(() => {
  const total = animeTotalPages.value;
  const curr = animePage.value;
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

// 对话框状态
const showAddAnimeDialog = ref(false);
const showEditAnimeDialog = ref(false);
const showAnimeDialog = ref(false);
const showCommentManagementDialog = ref(false);
const currentAnime = ref<any>(null);
const comments = ref<any[]>([]);

// 动漫表单
const animeForm = ref({
  id: 0,
  title: '',
  description: '',
  image: '',
  year: '',
  rating: 0,
  letter: '',
  genre: '',
  status: 1,
  episodes: [] as {
    episodeNumber: number;
    videoUrl: string;
  }[]
});

// 加载动漫列表
const loadAnimes = async () => {
  try {
    const res = await api.post('http://localhost:8080/api/admin/animes');
    if (res.data.code === 200) {
      animes.value = res.data.data;
    }
  } catch (error) {
    console.error('加载动漫列表失败：', error);
  }
};

// 搜索动漫
const searchAnimes = async () => {
  try {
    const res = await api.post('http://localhost:8080/api/admin/animes/search', {
      keyword: searchKeyword.value
    });
    if (res.data.code === 200) {
      animes.value = res.data.data;
      animePage.value = 1;
    }
  } catch (error) {
    console.error('搜索动漫失败：', error);
  }
};

// 处理图片上传
const handleImageUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (input.files && input.files[0]) {
    const file = input.files[0];
    const formData = new FormData();
    formData.append('file', file);
    
    try {
      const res = await api.post('http://localhost:8080/api/upload/cover', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      if (res.data.code === 200) {
        animeForm.value.image = res.data.data;
      }
    } catch (error) {
      console.error('上传图片失败：', error);
    }
  }
};

// 处理视频上传
const handleVideoUpload = async (event: Event, index: number) => {
  const input = event.target as HTMLInputElement;
  if (input.files && input.files[0]) {
    const file = input.files[0];
    const formData = new FormData();
    formData.append('file', file);
    
    // 要求用户输入存储文件夹路径
    const folderPath = prompt('请输入视频在本地的存储文件夹路径（例如：test）:', 'test');
    if (!folderPath) return;
    
    // 自动使用原始文件名
    const fileName = folderPath + '/' + file.name;
    formData.append('fileName', fileName);
    
    try {
      console.log('开始上传视频:', file.name);
      const res = await api.post('http://localhost:8080/api/admin/upload/video', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      console.log('上传视频响应:', res.data);
      if (res.data.code === 200 && animeForm.value.episodes[index]) {
        animeForm.value.episodes[index].videoUrl = res.data.data;
        console.log('视频URL更新成功:', res.data.data);
      } else {
        console.error('上传视频失败，响应码:', res.data.code);
      }
    } catch (error) {
      console.error('上传视频失败：', error);
    }
  }
};

// 添加集数
const addEpisode = () => {
  // 计算新集数的编号
  let newEpisodeNumber = 1;
  if (animeForm.value.episodes.length > 0) {
    // 找到最大的集数编号
    const maxEpisodeNumber = Math.max(...animeForm.value.episodes.map(episode => episode.episodeNumber));
    newEpisodeNumber = maxEpisodeNumber + 1;
  }
  
  animeForm.value.episodes.push({
    episodeNumber: newEpisodeNumber,
    videoUrl: ''
  });
};

// 删除集数
const removeEpisode = async (index: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该集数吗？删除后将无法恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    
    // 用户确认后执行删除操作
    animeForm.value.episodes.splice(index, 1);
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除集数失败：', error);
    }
  }
};

// 添加动漫
const addAnime = async () => {
  try {
    const res = await api.post('http://localhost:8080/api/admin/animes/add', animeForm.value);
    if (res.data.code === 200) {
      ElMessage.success('动漫添加成功！');
      closeDialog();
      loadAnimes();
    }
  } catch (error) {
    console.error('添加动漫失败：', error);
  }
};

// 编辑动漫
const editAnime = async (anime: any) => {
  try {
    // 获取动漫的集数数据
    const episodesRes = await api.get(`http://localhost:8080/api/episode/anime/${anime.id}`);
    let episodes = [];
    if (episodesRes.data && episodesRes.data.code === 200) {
      episodes = episodesRes.data.data;
    }
    
    animeForm.value = { 
      ...anime,
      episodes: episodes
    };
    showEditAnimeDialog.value = true;
    showAnimeDialog.value = true;
  } catch (error) {
    console.error('获取动漫集数失败：', error);
    // 如果获取集数失败，仍然显示编辑对话框，但集数为空
    animeForm.value = { 
      ...anime,
      episodes: []
    };
    showEditAnimeDialog.value = true;
    showAnimeDialog.value = true;
  }
};

// 更新动漫
const updateAnime = async () => {
  try {
    console.log('更新动漫数据:', animeForm.value);
    const res = await api.post('http://localhost:8080/api/admin/animes/update', animeForm.value);
    console.log('更新动漫响应:', res.data);
    if (res.data.code === 200) {
      ElMessage.success('动漫更新成功！');
      closeDialog();
      loadAnimes();
    } else {
      ElMessage.error('更新动漫失败: ' + (res.data.msg || '未知错误'));
    }
  } catch (error: any) {
    console.error('更新动漫失败：', error);
    ElMessage.error('更新动漫失败，请重试: ' + (error.message || '网络错误'));
  }
};

// 显示删除确认对话框
const showDeleteConfirm = async (animeId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该动漫吗？删除后将无法恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    
    // 用户确认后执行删除操作
    const res = await api.post('http://localhost:8080/api/admin/animes/delete', {
      id: animeId
    });
    if (res.data.code === 200) {
      ElMessageBox.alert('动漫删除成功！', '成功', {
        confirmButtonText: '确定',
      });
      loadAnimes();
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除动漫失败：', error);
      ElMessageBox.alert('删除动漫失败，请重试', '错误', {
        confirmButtonText: '确定',
      });
    }
  }
};

// 切换动漫状态（上架/下架）
const toggleStatus = async (anime: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要${anime.status === 1 ? '下架' : '上架'}该动漫吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info',
      }
    );
    
    // 用户确认后执行切换操作
    const res = await api.post('http://localhost:8080/api/admin/animes/toggleStatus', {
      id: anime.id,
      status: anime.status === 1 ? 0 : 1
    });
    if (res.data.code === 200) {
      ElMessageBox.alert(anime.status === 1 ? '动漫已下架！' : '动漫已上架！', '成功', {
        confirmButtonText: '确定',
      });
      loadAnimes();
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('切换动漫状态失败：', error);
      ElMessageBox.alert('切换动漫状态失败，请重试', '错误', {
        confirmButtonText: '确定',
      });
    }
  }
};

// 提交动漫表单
const submitAnimeForm = () => {
  if (showEditAnimeDialog.value) {
    updateAnime();
  } else {
    addAnime();
  }
};

// 处理添加动漫
const handleAddAnime = () => {
  showAddAnimeDialog.value = true;
  showEditAnimeDialog.value = false;
  showAnimeDialog.value = true;
  // 重置表单
  animeForm.value = {
    id: 0,
    title: '',
    description: '',
    image: '',
    year: '',
    rating: 0,
    letter: '',
    genre: '',
    status: 1,
    episodes: []
  };
};

// 关闭对话框
const closeDialog = () => {
  showAddAnimeDialog.value = false;
  showEditAnimeDialog.value = false;
  showAnimeDialog.value = false;
  animeForm.value = {
    id: 0,
    title: '',
    description: '',
    image: '',
    year: '',
    rating: 0,
    letter: '',
    genre: '',
    status: 1,
    episodes: []
  };
};

// 管理评论
const manageComments = async (anime: any) => {
  currentAnime.value = anime;
  await loadComments(anime.id);
  showCommentManagementDialog.value = true;
};

// 加载评论
const loadComments = async (animeId: number) => {
  try {
    const res = await api.post('http://localhost:8080/api/anime/comment/list', {
      animeId: animeId
    });
    if (res.data.code === 200) {
      // 处理评论数据，将回复嵌套到父评论中
      const parentComments = res.data.data;
      
      // 为每个父评论加载回复
      for (const parent of parentComments) {
        try {
          const repliesRes = await api.post('http://localhost:8080/api/anime/comment/replies', {
            animeId: animeId,
            parentId: parent.id
          });
          if (repliesRes.data.code === 200) {
            parent.replies = repliesRes.data.data;
          } else {
            parent.replies = [];
          }
        } catch (error) {
          console.error('加载回复失败：', error);
          parent.replies = [];
        }
      }
      
      comments.value = parentComments;
    }
  } catch (error) {
    console.error('加载评论失败：', error);
  }
};

// 删除评论
const deleteComment = async (commentId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该评论及其所有回复吗？删除后将无法恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    
    // 用户确认后执行删除操作
    const res = await api.post('http://localhost:8080/api/anime/comment/delete', {
      commentId: commentId
    });
    if (res.data.code === 200) {
      ElMessageBox.alert('评论删除成功！', '成功', {
        confirmButtonText: '确定',
      });
      if (currentAnime.value) {
        await loadComments(currentAnime.value.id);
      }
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除评论失败：', error);
      ElMessageBox.alert('删除评论失败，请重试', '错误', {
        confirmButtonText: '确定',
      });
    }
  }
};

// 关闭评论管理对话框
const closeCommentManagementDialog = () => {
  showCommentManagementDialog.value = false;
  currentAnime.value = null;
  comments.value = [];
  commentSearchKeyword.value = '';
  commentPage.value = 1;
};

// 评论搜索和分页
const commentSearchKeyword = ref('');
const commentPage = ref(1);
const commentPageSize = 5;
const filteredComments = computed(() => {
  if (!commentSearchKeyword.value) return comments.value;
  const keyword = commentSearchKeyword.value.toLowerCase();
  return comments.value.filter(comment => {
    // 搜索顶级评论
    if (comment.content && comment.content.toLowerCase().includes(keyword)) return true;
    if (comment.author?.username && comment.author.username.toLowerCase().includes(keyword)) return true;
    // 搜索回复
    if (comment.replies && comment.replies.length > 0) {
      return comment.replies.some((reply: any) =>
        (reply.content && reply.content.toLowerCase().includes(keyword)) ||
        (reply.author?.username && reply.author.username.toLowerCase().includes(keyword))
      );
    }
    return false;
  });
});
const commentTotalPages = computed(() => Math.ceil(filteredComments.value.length / commentPageSize) || 1);
const pagedComments = computed(() => {
  const start = (commentPage.value - 1) * commentPageSize;
  return filteredComments.value.slice(start, start + commentPageSize);
});
const commentPageNumbers = computed(() => {
  const total = commentTotalPages.value;
  const curr = commentPage.value;
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

// 格式化日期
const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleString();
};

// 页面加载时加载动漫列表
onMounted(() => {
  loadAnimes();
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
  flex-wrap: wrap;
  align-items: center;
}

.search-input {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  min-width: 200px;
}

.search-select {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  min-width: 120px;
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
  flex-wrap: wrap;
}

.sort-box label {
  font-size: 14px;
  color: #555;
  font-weight: 500;
}

.sort-option {
  padding: 6px 14px;
  cursor: pointer;
  border-radius: 16px;
  font-size: 13px;
  transition: all 0.3s;
  background: #f0f0f0;
  color: #666;
}

.sort-option:hover {
  background: #e0e0e0;
}

.sort-option.active {
  background: #ff6b6b;
  color: white;
}

.sort-direction-btn {
  padding: 6px 14px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.3s;
}

.sort-direction-btn:hover {
  background: #45a049;
}

.add-btn {
  padding: 8px 16px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.add-btn:hover {
  background: #45a049;
}

/* 动漫列表样式 */
.anime-list {
  margin-top: 20px;
}

.anime-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}

.anime-item {
  background: #f9f9f9;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  transition: transform 0.3s, box-shadow 0.3s;
}

.anime-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0,0,0,0.1);
}

.anime-image {
  position: relative;
  aspect-ratio: 2/3;
  overflow: hidden;
}

.anime-poster {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.anime-item:hover .anime-poster {
  transform: scale(1.05);
}

.anime-status {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 8px;
  background: rgba(0,0,0,0.7);
  color: white;
  border-radius: 4px;
  font-size: 12px;
  transition: background 0.3s;
}

.anime-status.active {
  background: rgba(76, 175, 80, 0.7);
}

.anime-info {
  padding: 15px;
}

.anime-title-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin: 0 0 8px 0;
}

.anime-title {
  margin: 0;
  font-size: 16px;
  font-weight: bold;
  color: #333;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.anime-year {
  margin: 0;
  font-size: 13px;
  color: #999;
  flex-shrink: 0;
  margin-left: 8px;
}

.anime-rating {
  font-size: 14px;
  color: #666;
}

.anime-views {
  font-size: 14px;
  color: #666;
}

.anime-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0 0 15px 0;
}

.anime-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.status-btn {
  flex: 1;
  padding: 6px 12px;
  background: #2196F3;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.3s;
}

.status-btn:hover {
  background: #0b7dda;
}

.edit-btn {
  flex: 1;
  padding: 6px 12px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.3s;
}

.edit-btn:hover {
  background: #45a049;
}

.delete-btn {
  flex: 1;
  padding: 6px 12px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.3s;
}

.delete-btn:hover {
  background: #d32f2f;
}

.comment-btn {
  flex: 1;
  padding: 6px 12px;
  background: #9c27b0;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.3s;
}

.comment-btn:hover {
  background: #7b1fa2;
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
  max-height: 80vh;
  overflow-y: auto;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  background: white;
  z-index: 10;
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
  max-height: 60vh;
  overflow-y: auto;
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
  min-height: 100px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 20px;
  border-top: 1px solid #eee;
  position: sticky;
  bottom: 0;
  background: white;
  z-index: 10;
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

/* 图片预览样式 */
.image-preview {
  margin-top: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 10px;
  text-align: center;
}

.preview-img {
  max-width: 100%;
  max-height: 200px;
  object-fit: contain;
}

/* 集数管理样式 */
.episode-item {
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 10px;
  margin-bottom: 10px;
  background: #f9f9f9;
}

.episode-fields {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.episode-number {
  flex: 0 0 80px;
}

.episode-video {
  flex: 1;
  min-width: 200px;
}

.video-url-display {
  flex: 1;
  min-width: 200px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.video-url-text {
  font-size: 14px;
  color: #666;
  background: #f0f0f0;
  padding: 5px 10px;
  border-radius: 4px;
  word-break: break-all;
}

.image-url-display {
  margin-top: 10px;
  margin-bottom: 10px;
}

.image-url-text {
  font-size: 14px;
  color: #666;
  background: #f0f0f0;
  padding: 5px 10px;
  border-radius: 4px;
  word-break: break-all;
}

.video-url-hint {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.remove-episode-btn {
  padding: 10px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.remove-episode-btn:hover {
  background: #d32f2f;
}

.add-episode-btn {
  padding: 10px 20px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
  margin-top: 10px;
}

.add-episode-btn:hover {
  background: #45a049;
}

/* 视频预览样式 */
.video-preview {
  margin-top: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 10px;
  text-align: center;
}

.preview-video {
  max-width: 100%;
  max-height: 200px;
}

/* 评论管理样式 */
.no-comments {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

.comment-content.deleted {
  color: #999;
  font-style: italic;
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  border-left: 3px solid #ff6b6b;
}

.reply-content.deleted {
  color: #999;
  font-style: italic;
  background: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
  border-left: 3px solid #ff6b6b;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 15px;
  background: #f9f9f9;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.comment-author {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.author-name {
  font-weight: 500;
  color: #333;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-content {
  margin-bottom: 10px;
  line-height: 1.5;
  color: #333;
}

.comment-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.comment-stats {
  color: #666;
}

.delete-comment-btn {
  padding: 4px 12px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.3s;
}

.delete-comment-btn:hover {
  background: #d32f2f;
}

.replies-list {
  margin-top: 15px;
  margin-left: 40px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reply-item {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 10px;
  background: #f5f5f5;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.reply-author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reply-time {
  font-size: 11px;
  color: #999;
}

.reply-content {
  margin-bottom: 8px;
  line-height: 1.4;
  color: #333;
  font-size: 14px;
}

.reply-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.reply-stats {
  color: #666;
}

/* 评论搜索框 */
.comment-search-box {
  margin-bottom: 16px;
}

.comment-search-box .search-input {
  width: 100%;
  max-width: 400px;
}

/* 评论分页 */
.comment-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #eee;
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
  
  .anime-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }
  
  .anime-image {
    height: 250px;
  }
  
  .dialog-content {
    width: 95%;
    max-height: 90vh;
  }
  
  .episode-fields {
    flex-direction: column;
  }
  
  .episode-number,
  .episode-video {
    flex: 1;
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