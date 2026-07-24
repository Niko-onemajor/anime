<template>
  <div class="player-container">
    <div class="player-header">
      <h1>{{ anime.title }} - 第{{ currentEpisode }}集</h1>
      <button class="back-btn" @click="goBack">返回详情页</button>
    </div>
    <div class="video-wrapper">
    <video ref="videoRef" class="video-player" controls>
      <source :src="videoSrc" type="video/mp4">
      您的浏览器不支持视频播放
    </video>
  </div>
    <div class="episode-list">
      <h3>选集</h3>
      <div class="episodes">
        <button 
          v-for="episode in episodes" 
          :key="episode.episodeNumber"
          :class="{ active: currentEpisode === episode.episodeNumber }"
          @click="changeEpisode(episode.episodeNumber)"
        >
          第{{ episode.episodeNumber }}集
        </button>
      </div>
    </div>
    
    <!-- 评分区域 -->
    <div class="rating-section">
      <h3>评分</h3>
      <div class="rating-container">
        <div class="average-rating">
          <span class="rating-value">{{ (anime.rating || 0).toFixed(1) }}</span>
          <div class="stars">
            <span v-for="i in 5" :key="i" class="star" :class="{ active: i <= Math.floor((anime.rating || 0) / 2) }">★</span>
          </div>
        </div>
        <div class="user-rating">
          <span>我的评分：</span>
          <div class="stars interactive">
            <span v-for="i in 5" :key="i" class="star" :class="{ active: i <= userRating }" @click="rateAnime(i)">{{ i <= userRating ? '★' : '☆' }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 评论区域 -->
    <div class="comments-section">
      <h3>评论</h3>
      
      <!-- 发表评论 -->
      <div class="comment-form">
        <textarea v-model="commentContent" placeholder="写下你的评论..." class="comment-textarea"></textarea>
        <button @click="submitComment" class="comment-submit-btn">发表评论</button>
      </div>
      
      <!-- 评论排序和分页 -->
      <div class="comment-controls">
        <div class="comment-sort">
          <span class="sort-label">排序方式：</span>
          <span 
            v-for="sort in commentSortOptions" 
            :key="sort.value" 
            class="sort-option small" 
            :class="{ active: selectedCommentSort === sort.value }"
            @click="selectCommentSort(sort.value)"
          >
            {{ sort.label }}
          </span>
          <button @click="toggleCommentSortOrder" class="sort-order-btn small">
            切换
          </button>
        </div>
      </div>
      
      <!-- 评论列表 -->
      <div class="comments-list">
        <div v-for="comment in sortedComments" :key="comment.id" class="comment-item">
          <div class="comment-header">
            <div class="comment-author">
              <img :src="getImageUrl(comment.authorAvatar)" :alt="comment.authorName || '未知用户'" class="author-avatar" @click="goToUserProfile(comment.authorName)" style="cursor: pointer;">
              <span class="author-name" @click="goToUserProfile(comment.authorName)" style="cursor: pointer;">{{ comment.authorName || '未知用户' }}</span>
            </div>
            <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
          </div>
          <div class="comment-content" :class="{ 'deleted': comment.deleted }">
            {{ comment.deleted ? '该评论已被删除' : comment.content }}
          </div>
          <div class="comment-actions" v-if="!comment.deleted">
            <button @click="likeComment(comment)" class="action-btn like-btn" :class="{ active: comment.likes?.includes(currentUser) }">👍 {{ comment.likeCount }}</button>
            <button @click="dislikeComment(comment)" class="action-btn dislike-btn" :class="{ active: comment.dislikes?.includes(currentUser) }">👎 {{ comment.dislikeCount }}</button>
            <button @click="replyToComment(comment.id)" class="action-btn reply-btn">回复</button>
            <button @click="deleteComment(comment.id)" class="action-btn delete-btn" v-if="comment.authorName === currentUser">删除</button>
          </div>
          
          <!-- 回复列表 -->
          <div v-if="comment.replies && comment.replies.length > 0">
            <div class="reply-toggle" @click="toggleReplyVisibility(comment.id)">
              <span class="toggle-text">{{ comment.showReplies ? '收起回复' : `查看${comment.replies.length}条回复` }}</span>
              <span class="toggle-icon">{{ comment.showReplies ? '▼' : '▶' }}</span>
            </div>
            <div class="replies-list" v-if="comment.showReplies">
              <div class="reply-sort">
                <span class="sort-label">排序方式：</span>
                <span 
                  v-for="sort in replySortOptions" 
                  :key="sort.value" 
                  class="sort-option small" 
                  :class="{ active: comment.replySortBy === sort.value }"
                  @click="selectReplySort(comment.id, sort.value)"
                >
                  {{ sort.label }}
                </span>
                <button @click="toggleReplySortOrder(comment.id)" class="sort-order-btn small">
                  切换
                </button>
              </div>
              <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                <div class="comment-header">
                  <div class="comment-author">
                    <img :src="getImageUrl(reply.authorAvatar)" :alt="reply.authorName || '未知用户'" class="author-avatar small" @click="goToUserProfile(reply.authorName)" style="cursor: pointer;">
                    <span class="author-name" @click="goToUserProfile(reply.authorName)" style="cursor: pointer;">{{ reply.authorName || '未知用户' }}</span>
                  </div>
                  <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
                </div>
                <div class="comment-content" :class="{ 'deleted': reply.deleted }">
                  {{ reply.deleted ? '该评论已被删除' : reply.content }}
                </div>
                <div class="comment-actions" v-if="!reply.deleted">
                  <button @click="likeComment(reply)" class="action-btn like-btn small" :class="{ active: reply.likes?.includes(currentUser) }">👍 {{ reply.likeCount }}</button>
                  <button @click="dislikeComment(reply)" class="action-btn dislike-btn small" :class="{ active: reply.dislikes?.includes(currentUser) }">👎 {{ reply.dislikeCount }}</button>
                  <button @click="deleteComment(reply.id)" class="action-btn delete-btn small" v-if="reply.authorName === currentUser">删除</button>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 回复表单 -->
          <div class="reply-form" v-if="replyingTo === comment.id">
            <textarea v-model="replyContent" placeholder="写下你的回复..." class="comment-textarea small"></textarea>
            <div class="reply-form-actions">
              <button @click="replyingTo = null" class="cancel-btn">取消</button>
              <button @click="submitReply(comment.id)" class="submit-btn">回复</button>
            </div>
          </div>
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
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import { ElMessageBox, ElMessage } from 'element-plus';

// 处理图片URL
const getImageUrl = function(url: string | undefined | null): string {
  if (!url) {
    return '/avatars/avatar1.jpg';
  }
  
  // 检查是否已经是完整的URL
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  
  // 如果是本地文件路径，转换为HTTP URL
  if (url.startsWith('file:///')) {
    // 提取文件名
    const filename = url.split('/').pop();
    // 判断是头像还是封面
    if (url.includes('avatars')) {
      return '/avatars/' + filename;
    } else {
      return '/covers/' + filename;
    }
  }
  
  // 如果是本地文件路径（Windows或Unix风格），转换为HTTP URL
  if (url.includes(':')) {
    // 提取文件名，同时处理正斜杠和反斜杠
    const parts = url.split(/[\\/]/);
    const filename = parts.pop();
    // 判断是头像还是封面
    if (url.includes('avatars')) {
      return '/avatars/' + filename;
    } else if (url.includes('covers')) {
      return '/covers/' + filename;
    } else {
      return '/avatars/' + filename;
    }
  }
  
  // 检查是否是本地路径
  if (url.startsWith('/src/')) {
    return '/avatars/avatar1.jpg';
  }
  
  // 直接返回url，因为后端应该已经返回了完整的URL
  return url;
};

const route = useRoute();
const router = useRouter();
const videoRef = ref<HTMLVideoElement | null>(null);
const currentEpisode = ref<number>(1);
const animeId = ref<number>(1);

// 动漫数据
const anime = ref({
  id: 1,
  title: '进击的巨人',
  year: '2013',
  genre: '动作, 科幻',
  rating: 9.3,
  description: '人类与巨人的史诗对决，为了自由而战',
  image: 'http://localhost:8080/covers/giant.webp'
});

// 集数数据
const episodes = ref<any[]>([]);

// 评分相关
const userRating = ref<number>(0);

// 评论相关
const comments = ref<any[]>([]);
const commentContent = ref('');
const replyContent = ref('');
const replyingTo = ref<number | null>(null);

// 评论排序选项
const commentSortOptions = [
  { label: '按时间', value: 'time' },
  { label: '按点赞数', value: 'likes' },
  { label: '按点踩数', value: 'dislikes' },
  { label: '按回复数', value: 'replies' }
];

// 回复排序选项
const replySortOptions = [
  { label: '按时间', value: 'time' },
  { label: '按点赞数', value: 'likes' },
  { label: '按点踩数', value: 'dislikes' }
];

// 评论排序状态
const selectedCommentSort = ref('time');
const commentSortOrder = ref('desc'); // desc: 降序, asc: 升序

// 当前用户
const currentUser = ref(localStorage.getItem('username') || 'user1');

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

// 记录观看历史
const recordWatchHistory = async function() {
  try {
    const currentUser = localStorage.getItem('username') || 'user1';
    const episode = episodes.value.find(function(ep) {
      return ep.episodeNumber === currentEpisode.value;
    });
    if (episode) {
      const res = await axios.post('/api/watch-history/add', {
        username: currentUser,
        animeId: animeId.value,
        episodeId: episode.id
      });
      if (res.data.code === 200) {
        console.log('观看记录添加成功');
      }
    }
  } catch (error) {
    console.error('记录观看历史失败:', error);
  }
};

// 从路由参数获取动漫ID和集数
onMounted(function() {
  const id = Number(route.params.id);
  const episode = Number(route.params.episode);
  if (id) {
    animeId.value = id;
    // 根据动漫ID获取动漫信息和集数
    getAnimeInfo(id);
    // 加载评论
    loadComments(id);
    // 获取用户评分
    getUserRating(id);
  }
  if (episode) {
    currentEpisode.value = episode;
  }
  
  // 设置默认音量为40%
  if (videoRef.value) {
    videoRef.value.volume = 0.4;
  }
  
  // 记录观看历史
  setTimeout(recordWatchHistory, 1000);
});

// 监听路由参数变化
watch(function() {
  return route.params.id;
}, function(newId) {
  const id = Number(newId);
  if (id) {
    animeId.value = id;
    getAnimeInfo(id);
    loadComments(id);
    getUserRating(id);
  }
});

watch(function() {
  return route.params.episode;
}, function(newEpisode) {
  const episode = Number(newEpisode);
  if (episode) {
    currentEpisode.value = episode;
    // 记录观看历史
    setTimeout(recordWatchHistory, 500);
  }
});

// 监听episodes数组变化，当数据加载完成后设置音量
watch(episodes, function() {
  if (videoRef.value) {
    // 重新加载视频元素
    const video = videoRef.value;
    video.load();
    // 设置音量为40%
    video.volume = 0.4;
  }
}, { deep: true });

// 获取动漫信息和集数
const getAnimeInfo = async function(id: number) {
  try {
    // 获取动漫详情
    const animeRes = await axios.get('/api/anime/detail/' + id);
    if (animeRes.data) {
      anime.value = animeRes.data;
    }
    
    // 获取动漫的集数
    const episodesRes = await axios.get('/api/episode/anime/' + id);
    if (episodesRes.data && episodesRes.data.code === 200) {
      episodes.value = episodesRes.data.data;
    }
  } catch (error) {
    console.error('获取动漫信息失败:', error);
  }
};

// 处理视频URL
const getVideoUrl = function(url: string): string {
  if (!url) return '';
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
        const result = '/videos/' + subPath + '/' + filename;
        console.log('本地路径转换结果:', result);
        return result;
      } else {
        const result = '/videos/' + filename;
        console.log('本地路径转换结果:', result);
        return result;
      }
    } else {
      // 如果没有找到mp4目录，只返回文件名
      const result = '/videos/' + filename;
      console.log('本地路径转换结果:', result);
      return result;
    }
  }
  // 如果是相对路径，直接返回
  if (url.startsWith('/')) {
    console.log('相对路径，直接返回:', url);
    return url;
  }
  // 默认返回原路径
  console.log('默认返回原路径:', url);
  return url;
};

// 计算视频源路径
const videoSrc = computed(function() {
  const episode = currentEpisode.value;
  
  // 从集数数据中获取视频URL
  const currentEpisodeData = episodes.value.find(function(ep) {
    return ep.episodeNumber === episode;
  });
  if (currentEpisodeData && currentEpisodeData.videoUrl) {
    return getVideoUrl(currentEpisodeData.videoUrl);
  }
  
  // 默认视频路径
  return '';
});

// 切换集数
const changeEpisode = function(episode: number) {
  currentEpisode.value = episode;
  // 切换视频后重新加载视频源
  if (videoRef.value) {
    // 重新加载视频元素
    const video = videoRef.value;
    video.load();
    video.play();
    // 设置音量为40%
    video.volume = 0.4;
  }
  // 更新路由参数
  router.push('/anime/' + animeId.value + '/play/' + episode);
};

// 返回动漫详情页
const goBack = function() {
  // 从播放页进入详情页时，保留fromPage，让详情页知道它是从个人中心来的
  router.push('/anime/' + animeId.value);
};

// 评分相关
const rateAnime = async function(stars: number) {
  userRating.value = stars;
  const rating = stars * 2; // 1星=2分，最高10分
  
  try {
    const res = await axios.post('/api/anime/rating/submit', {
      animeId: animeId.value,
      username: currentUser.value,
      rating
    });
    
    if (res.data.code === 200) {
      // 直接更新评分，不重新加载整个动漫信息
      // 这样可以避免视频刷新
      const updatedRating = res.data.data.rating;
      anime.value.rating = updatedRating;
      ElMessage.success('评分成功！');
    }
  } catch (error) {
    console.error('评分失败:', error);
    ElMessage.error('评分失败，请重试');
  }
};

// 获取用户评分
const getUserRating = async function(animeId: number) {
  try {
    const res = await axios.post('/api/anime/rating/user', {
      animeId,
      username: currentUser.value
    });
    
    if (res.data.code === 200 && res.data.data) {
      const rating = res.data.data.rating;
      userRating.value = Math.floor(rating / 2); // 转换为星星数
    }
  } catch (error) {
    console.error('获取用户评分失败:', error);
  }
};

// 评论相关
const loadComments = async function(animeId: number) {
  try {
    const res = await axios.post('/api/anime/comment/list', {
      animeId
    });
    
    if (res.data.code === 200) {
      const commentList = res.data.data;
      // 保存当前评论的展开状态
      const currentCommentStates = new Map();
      comments.value.forEach(function(comment: any) {
        currentCommentStates.set(comment.id, comment.showReplies);
      });
      
      // 获取当前用户的最新头像
      const userAvatar = localStorage.getItem('avatar');
      
      // 处理评论数据，使用后端返回的作者信息
        comments.value = await Promise.all(commentList.map(async function(comment: any) {
          // 从后端返回的数据中获取作者信息
          let authorName = comment.author ? comment.author.username : '未知用户';
          let authorAvatar = comment.author ? (comment.author.avatar ? comment.author.avatar : 'http://localhost:8080/avatars/avatar1.jpg') : 'http://localhost:8080/avatars/avatar1.jpg';
          
          // 如果是当前用户，使用localStorage中的最新头像
          if (authorName === currentUser.value && userAvatar) {
            authorAvatar = userAvatar;
          }
          
          // 加载回复
          const repliesRes = await axios.post('/api/anime/comment/replies', {
            animeId,
            parentId: comment.id
          });
          
          const replies = repliesRes.data.code === 200 ? repliesRes.data.data : [];
          // 处理回复的作者信息
          const processedReplies = replies.map(function(reply: any) {
            let replyAuthorName = reply.author ? reply.author.username : '未知用户';
            let replyAuthorAvatar = reply.author ? (reply.author.avatar ? reply.author.avatar : 'http://localhost:8080/avatars/avatar1.jpg') : 'http://localhost:8080/avatars/avatar1.jpg';
            
            // 如果是当前用户，使用localStorage中的最新头像
            if (replyAuthorName === currentUser.value && userAvatar) {
              replyAuthorAvatar = userAvatar;
            }
            
            return {
              ...reply,
              authorName: replyAuthorName,
              authorAvatar: replyAuthorAvatar,
              likes: reply.likes || [],
              dislikes: reply.dislikes || [],
              likeCount: reply.likeCount || 0,
              dislikeCount: reply.dislikeCount || 0,
              deleted: reply.deleted || false
            };
          });
          
          // 保持之前的展开状态
          const showReplies = currentCommentStates.get(comment.id) || false;
          
          return {
            ...comment,
            authorName: authorName,
            authorAvatar: authorAvatar,
            replies: processedReplies,
            likes: comment.likes || [],
            dislikes: comment.dislikes || [],
            likeCount: comment.likeCount || 0,
            dislikeCount: comment.dislikeCount || 0,
            deleted: comment.deleted || false,
            showReplies: showReplies,
            replySortBy: 'time',
            replySortOrder: 'desc'
          };
        }));
    }
  } catch (error) {
    console.error('加载评论失败:', error);
  }
};

// 提交评论
const submitComment = async function() {
  if (!commentContent.value) {
    ElMessage.warning('请输入评论内容');
    return;
  }
  
  try {
    const res = await axios.post('/api/anime/comment/add', {
      animeId: animeId.value,
      username: currentUser.value,
      content: commentContent.value,
      parentId: null
    });
    
    if (res.data.code === 200) {
      ElMessage.success('评论成功！');
      commentContent.value = '';
      // 重新加载评论
      loadComments(animeId.value);
    }
  } catch (error) {
    console.error('提交评论失败:', error);
    ElMessage.error('评论失败，请重试');
  }
};

// 回复评论
const replyToComment = function(commentId: number) {
  replyingTo.value = commentId;
};

// 提交回复
const submitReply = async function(commentId: number) {
  if (!replyContent.value) {
    ElMessage.warning('请输入回复内容');
    return;
  }
  
  try {
    const res = await axios.post('/api/anime/comment/add', {
      animeId: animeId.value,
      username: currentUser.value,
      content: replyContent.value,
      parentId: commentId
    });
    
    if (res.data.code === 200) {
      ElMessage.success('回复成功！');
      replyContent.value = '';
      replyingTo.value = null;
      // 重新加载评论
      loadComments(animeId.value);
    }
  } catch (error) {
    console.error('提交回复失败:', error);
    ElMessage.error('回复失败，请重试');
  }
};

// 点赞评论
const likeComment = async function(comment: any) {
  if (!currentUser.value) {
    ElMessage.warning('请先登录');
    return;
  }
  
  try {
    const res = await axios.post('/api/anime/comment/like', {
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
        comment.likes = comment.likes.filter(function(user: string) {
          return user !== currentUser.value;
        });
        ElMessage.success('取消点赞成功');
      } else {
        // 添加点赞
        if (!comment.likes) {
          comment.likes = [];
        }
        comment.likes.push(currentUser.value);
        // 取消点踩（如果有）
        if (comment.dislikes && comment.dislikes.includes(currentUser.value)) {
          comment.dislikes = comment.dislikes.filter(function(user: string) {
            return user !== currentUser.value;
          });
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
const dislikeComment = async function(comment: any) {
  if (!currentUser.value) {
    ElMessage.warning('请先登录');
    return;
  }
  
  try {
    const res = await axios.post('/api/anime/comment/dislike', {
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
        comment.dislikes = comment.dislikes.filter(function(user: string) {
          return user !== currentUser.value;
        });
        ElMessage.success('取消点踩成功');
      } else {
        // 添加点踩
        if (!comment.dislikes) {
          comment.dislikes = [];
        }
        comment.dislikes.push(currentUser.value);
        // 取消点赞（如果有）
        if (comment.likes && comment.likes.includes(currentUser.value)) {
          comment.likes = comment.likes.filter(function(user: string) {
            return user !== currentUser.value;
          });
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

// 格式化时间
const formatTime = function(time: Date | string) {
  const date = typeof time === 'string' ? new Date(time) : time;
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 选择评论排序
const selectCommentSort = function(sort: string) {
  selectedCommentSort.value = sort;
};

// 切换评论排序顺序
const toggleCommentSortOrder = function() {
  commentSortOrder.value = commentSortOrder.value === 'desc' ? 'asc' : 'desc';
};

// 获取排序后的评论
const sortedComments = computed(function() {
  // 排序评论
  const sorted = [...comments.value].sort(function(a, b) {
    if (selectedCommentSort.value === 'time') {
      const timeA = new Date(a.createTime).getTime();
      const timeB = new Date(b.createTime).getTime();
      return commentSortOrder.value === 'desc' ? timeB - timeA : timeA - timeB;
    } else if (selectedCommentSort.value === 'likes') {
      const likesA = a.likeCount || 0;
      const likesB = b.likeCount || 0;
      return commentSortOrder.value === 'desc' ? likesB - likesA : likesA - likesB;
    } else if (selectedCommentSort.value === 'dislikes') {
      const dislikesA = a.dislikeCount || 0;
      const dislikesB = b.dislikeCount || 0;
      return commentSortOrder.value === 'desc' ? dislikesB - dislikesA : dislikesA - dislikesB;
    } else if (selectedCommentSort.value === 'replies') {
      const repliesA = a.replies ? a.replies.length : 0;
      const repliesB = b.replies ? b.replies.length : 0;
      return commentSortOrder.value === 'desc' ? repliesB - repliesA : repliesA - repliesB;
    }
    return 0;
  });
  return sorted;
});

// 打开用户资料弹窗
const goToUserProfile = async function(username: string) {
  // 加载用户资料
  await loadUserProfile(username);
  // 显示弹窗
  showUserProfile.value = true;
};

// 关闭用户资料弹窗
const closeUserProfile = function() {
  showUserProfile.value = false;
};

// 显示头像预览
const showAvatarPreview = function() {
  showAvatarPreviewModal.value = true;
};

// 关闭头像预览
const closeAvatarPreview = function() {
  showAvatarPreviewModal.value = false;
};

// 切换回复显示状态
const toggleReplyVisibility = function(commentId: number) {
  const comment = comments.value.find(function(c) {
    return c.id === commentId;
  });
  if (comment) {
    comment.showReplies = !comment.showReplies;
  }
};

// 选择回复排序方式
const selectReplySort = function(commentId: number, sortBy: string) {
  const comment = comments.value.find(function(c) {
    return c.id === commentId;
  });
  if (comment) {
    comment.replySortBy = sortBy;
    sortReplies(comment);
  }
};

// 切换回复排序顺序
const toggleReplySortOrder = function(commentId: number) {
  const comment = comments.value.find(function(c) {
    return c.id === commentId;
  });
  if (comment) {
    comment.replySortOrder = comment.replySortOrder === 'desc' ? 'asc' : 'desc';
    sortReplies(comment);
  }
};

// 排序回复
const sortReplies = function(comment: any) {
  if (comment && comment.replies) {
    comment.replies.sort(function(a: any, b: any) {
      if (comment.replySortBy === 'time') {
        const timeA = new Date(a.createTime).getTime();
        const timeB = new Date(b.createTime).getTime();
        return comment.replySortOrder === 'desc' ? timeB - timeA : timeA - timeB;
      } else if (comment.replySortBy === 'likes') {
        const likesA = a.likeCount || 0;
        const likesB = b.likeCount || 0;
        return comment.replySortOrder === 'desc' ? likesB - likesA : likesA - likesB;
      } else if (comment.replySortBy === 'dislikes') {
        const dislikesA = a.dislikeCount || 0;
        const dislikesB = b.dislikeCount || 0;
        return comment.replySortOrder === 'desc' ? dislikesB - dislikesA : dislikesA - dislikesB;
      }
      return 0;
    });
  }
};

// 加载用户资料
const loadUserProfile = async function(username: string) {
  try {
    const res = await axios.post('/api/user/profile', {
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

// 删除评论
const deleteComment = async function(commentId: number) {
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
    const res = await axios.post('/api/anime/comment/delete', {
      commentId: commentId
    });
    if (res.data.code === 200) {
      ElMessage.success('评论删除成功！');
      // 重新加载评论
      loadComments(animeId.value);
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除评论失败:', error);
      ElMessage.error('删除失败，请重试');
    }
  }
};
</script>

<style scoped>
.player-container {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.player-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.back-btn {
  padding: 8px 16px;
  background: #666;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s ease;
}

.back-btn:hover {
  background: #444;
}

h1 {
  font-size: 24px;
  margin-bottom: 20px;
  color: #333;
}

.video-wrapper {
  margin-bottom: 30px;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.video-player {
  width: 100%;
  height: 500px;
}

.episode-list {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
}

.episode-list h3 {
  font-size: 18px;
  margin-bottom: 15px;
  color: #333;
}

.episodes {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.episodes button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: #f9f9f9;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.episodes button:hover {
  background: #e0e0e0;
}

.episodes button.active {
  background: #4caf50;
  color: #fff;
  border-color: #4caf50;
}

/* 评分区域样式 */
.rating-section {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
}

.rating-section h3 {
  font-size: 18px;
  margin-bottom: 15px;
  color: #333;
}

.rating-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.average-rating {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rating-value {
  font-size: 24px;
  font-weight: bold;
  color: #ff6b6b;
}

.stars {
  display: flex;
  gap: 5px;
}

.star {
  font-size: 20px;
  color: #ddd;
  cursor: pointer;
  transition: color 0.3s ease;
}

.star.active {
  color: #ffd700;
}

.stars.interactive .star:hover {
  color: #ffd700;
}

.user-rating {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 评论区域样式 */
.comments-section {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.comments-section h3 {
  font-size: 18px;
  margin-bottom: 15px;
  color: #333;
}

/* 评论控制栏 */
.comment-controls {
  display: flex;
  justify-content: flex-start;
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

.comment-form {
  margin-bottom: 30px;
  display: flex;
  flex-direction: column;
  gap: 10px;
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

.comment-textarea.small {
  min-height: 80px;
}

.comment-submit-btn {
  align-self: flex-end;
  padding: 8px 16px;
  background: #ff6b6b;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s ease;
}

.comment-submit-btn:hover {
  background: #ff5252;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  padding: 15px;
  background: #f9f9f9;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
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
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.author-avatar.small {
  width: 30px;
  height: 30px;
}

.author-name {
  font-weight: bold;
  color: #333;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-content {
  margin-bottom: 15px;
  line-height: 1.5;
  color: #333;
}

.comment-content.deleted {
  color: #999;
  font-style: italic;
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  border-left: 3px solid #ff6b6b;
}

.comment-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  padding: 4px 12px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 12px;
}

.action-btn:hover {
  background: #f0f0f0;
}

.action-btn.active {
  color: #ff6b6b;
  font-weight: bold;
}

.delete-btn {
  color: #f44336;
  border-color: #f44336;
}

.delete-btn:hover {
  background: #f44336;
  color: #fff;
}

.delete-btn.small {
  color: #f44336;
  border-color: #f44336;
}

.delete-btn.small:hover {
  background: #f44336;
  color: #fff;
}

.action-btn.small {
  padding: 2px 8px;
  font-size: 10px;
}

.action-btn.small.active {
  color: #ff6b6b;
  font-weight: bold;
}

.reply-toggle {
  margin-top: 10px;
  margin-left: 50px;
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  color: #666;
  font-size: 14px;
  font-weight: 500;
  transition: color 0.3s;
}

.reply-toggle:hover {
  color: #ff6b6b;
}

.toggle-icon {
  font-size: 12px;
  transition: transform 0.3s;
}

.reply-sort {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.reply-sort .sort-label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.reply-sort .sort-option.small {
  padding: 2px 8px;
  cursor: pointer;
  border-radius: 12px;
  font-size: 10px;
  transition: all 0.3s;
  background: #f0f0f0;
}

.reply-sort .sort-option.small:hover {
  background: #e0e0e0;
}

.reply-sort .sort-option.small.active {
  background: #ff6b6b;
  color: white;
}

.reply-sort .sort-order-btn.small {
  padding: 2px 8px;
  background: #ff6b6b;
  color: white;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 10px;
  transition: background 0.3s;
}

.reply-sort .sort-order-btn.small:hover {
  background: #ff5252;
}

.replies-list {
  margin-top: 10px;
  margin-left: 50px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reply-item {
  padding: 10px;
  background: #f0f0f0;
  border-radius: 6px;
}

.reply-form {
  margin-top: 15px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-left: 50px;
}

.reply-form-actions {
  display: flex;
  gap: 10px;
  align-self: flex-end;
}

.cancel-btn {
  padding: 4px 12px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 12px;
}

.cancel-btn:hover {
  background: #f0f0f0;
}

.submit-btn {
  padding: 4px 12px;
  background: #4caf50;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s ease;
  font-size: 12px;
}

.submit-btn:hover {
  background: #45a049;
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
  .player-container {
    padding: 10px;
  }
  
  .video-player {
    height: 300px;
  }
  
  .rating-container {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .comment-controls {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .replies-list {
    margin-left: 30px;
  }
  
  .reply-form {
    margin-left: 30px;
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