<template>
  <div class="index-container">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="navbar-container">
        <a href="#" class="logo">Niko动漫</a>
        <ul class="nav-links">
          <li class="nav-link active"><a href="/index">首页</a></li>
          <li class="nav-link"><a href="/category">分类</a></li>
          <li class="nav-link"><a href="/forum">论坛</a></li>
          <li class="nav-link"><a href="/profile">个人中心</a></li>
          <li class="nav-link" v-if="isAdmin"><a href="/admin/users">管理员后台</a></li>
        </ul>
      </div>
    </nav>

    <!-- 轮播图 -->
    <div class="carousel">
      <div class="carousel-wrapper" ref="carouselWrapper">
        <div class="carousel-item" v-for="(item, index) in carouselData" :key="item.id" :class="{
          active: currentSlide === index,
          prev: currentSlide === (index + 1) % carouselData.length,
          next: currentSlide === (index - 1 + carouselData.length) % carouselData.length
        }" :style="{ backgroundColor: item.mainColor || '#1a1a1a' }" @click="goToDetail(item)">
          <img :src="getImageUrl(item.image)" :alt="item.title" @error="handleImageError($event, item)" />
          <div class="carousel-caption">
            <h3 class="carousel-title">{{ item.title }}</h3>
            <p class="carousel-desc">{{ item.genre || item.description || '' }}</p>
            <button class="carousel-btn">立即观看</button>
          </div>
        </div>
      </div>
      <button class="carousel-control prev" @click="prevSlide">&lt;</button>
      <button class="carousel-control next" @click="nextSlide">&gt;</button>
      <div class="carousel-indicators">
        <span class="indicator" v-for="(item, index) in carouselData" :key="index" :class="{ active: currentSlide === index }" @click="goToSlide(index)"></span>
      </div>
    </div>



    <!-- 个性化推荐 -->
    <div class="recommendation-container">
      <div class="section-header">
        <h2>为你推荐</h2>
      </div>
      <div class="recommendation-list">
        <div class="anime-card" v-for="anime in recommendations" :key="anime.id" @click="goToDetail(anime)">
          <div class="anime-card-image">
            <img :src="getImageUrl(anime.image)" :alt="anime.title" @error="handleImageError($event, anime)" />
          </div>
          <div class="anime-card-info">
            <h3 class="anime-card-title">{{ anime.title }}</h3>
            <p class="anime-card-category">{{ anime.category }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 排行榜 -->
    <div class="ranking-container">
      <div class="section-header">
        <h2>排行榜</h2>
      </div>
      <div class="ranking-tabs">
        <button class="ranking-tab" :class="{ active: activeRanking === 'weekly' }" @click="activeRanking = 'weekly'">周榜</button>
        <button class="ranking-tab" :class="{ active: activeRanking === 'monthly' }" @click="activeRanking = 'monthly'">月榜</button>
        <button class="ranking-tab" :class="{ active: activeRanking === 'yearly' }" @click="activeRanking = 'yearly'">年榜</button>
      </div>
      <div class="ranking-list">
        <div class="anime-card" v-for="(anime, index) in currentRanking" :key="anime.id" @click="goToDetail(anime)">
          <div class="ranking-number">{{ index + 1 }}</div>
          <div class="anime-card-image">
            <img :src="getImageUrl(anime.image)" :alt="anime.title" @error="handleImageError($event, anime)" />
          </div>
          <div class="anime-card-info">
            <h3 class="anime-card-title">{{ anime.title }}</h3>
            <p class="anime-card-category">{{ anime.genre }}</p>
            <div class="anime-card-stats">
              <span class="anime-card-watch-count" v-if="anime.watchCount">观看次数: {{ anime.watchCount }}</span>
              <span class="anime-card-rating" v-if="anime.rating">评分: {{ anime.rating }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新片上线 -->
    <div class="new-anime-container">
      <div class="section-header">
        <h2>新片上线</h2>
        <a href="/category" class="more-btn">更多 <span>&gt;</span></a>
      </div>
      <div class="new-anime-list">
        <div class="anime-card" v-for="anime in newAnimes" :key="anime.id" @click="goToDetail(anime)">
          <div class="anime-card-image">
            <img :src="getImageUrl(anime.image)" :alt="anime.title" @error="handleImageError($event, anime)" />
          </div>
          <div class="anime-card-info">
            <h3 class="anime-card-title">{{ anime.title }}</h3>
            <p class="anime-card-year">{{ anime.year }}</p>
          </div>
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
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import axios from 'axios';

const router = useRouter();

// 检查用户是否是管理员
const isAdmin = ref(false);

const checkAdmin = () => {
  // 先检查localStorage
  let role = localStorage.getItem('role');
  // 如果localStorage中没有，检查sessionStorage
  if (!role) {
    role = sessionStorage.getItem('role');
  }
  isAdmin.value = role === 'admin' || role === '1';
};

const goToDetail = (anime: any) => {
  if (Number(anime.status) !== 1) {
    ElMessage.warning('该动漫已被下架');
  } else {
    // 保存来源页面
    localStorage.setItem('fromPage', '/index');
    router.push(`/anime/${anime.id}`);
  }
};

// 轮播图相关
const carouselWrapper = ref<HTMLElement | null>(null);
const currentSlide = ref(0);
const direction = ref('next'); // 跟踪轮播方向

let autoplayInterval: number | undefined;

const updateCarousel = () => {
  // 轮播图现在通过CSS类和transform实现，不需要滚动
  // 这里可以添加额外的逻辑，如果需要的话
};

const nextSlide = () => {
  direction.value = 'next';
  currentSlide.value = (currentSlide.value + 1) % carouselData.value.length;
  updateCarousel();
  restartAutoplay();
};

const prevSlide = () => {
  direction.value = 'prev';
  currentSlide.value = (currentSlide.value - 1 + carouselData.value.length) % carouselData.value.length;
  updateCarousel();
  restartAutoplay();
};

const goToSlide = (index: number) => {
  currentSlide.value = index;
  updateCarousel();
  restartAutoplay();
};

const restartAutoplay = () => {
  stopAutoplay();
  startAutoplay();
};

const startAutoplay = () => {
  stopAutoplay(); // 先清除现有定时器，避免多个定时器同时运行
  autoplayInterval = setInterval(nextSlide, 5000);
};

const stopAutoplay = () => {
  if (autoplayInterval) {
    clearInterval(autoplayInterval);
  }
};

// 排行榜和新片上线数据
const activeRanking = ref('weekly');
const weeklyRanking = ref<Array<{
  id: number;
  title: string;
  description: string;
  image: string;
  status: number;
  genre: string;
  watchCount: number;
  rating: number;
}>>([]);
const monthlyRanking = ref<Array<{
  id: number;
  title: string;
  description: string;
  image: string;
  status: number;
  genre: string;
  watchCount: number;
  rating: number;
}>>([]);
const yearlyRanking = ref<Array<{
  id: number;
  title: string;
  description: string;
  image: string;
  status: number;
  genre: string;
  watchCount: number;
  rating: number;
}>>([]);

// 当前显示的排行榜数据
const currentRanking = computed(() => {
  switch (activeRanking.value) {
    case 'weekly':
      return weeklyRanking.value;
    case 'monthly':
      return monthlyRanking.value;
    case 'yearly':
      return yearlyRanking.value;
    default:
      return weeklyRanking.value;
  }
});

// 加载排行榜数据
const loadRankingData = async () => {
  try {
    // 并行请求所有排行榜数据
    const [weeklyResponse, monthlyResponse, yearlyResponse] = await Promise.all([
      axios.get('/api/anime/ranking/weekly'),
      axios.get('/api/anime/ranking/monthly'),
      axios.get('/api/anime/ranking/yearly')
    ]);
    
    console.log('周榜数据响应:', weeklyResponse.data);
    console.log('月榜数据响应:', monthlyResponse.data);
    console.log('年榜数据响应:', yearlyResponse.data);
    
    // 填充排行榜数据
    if (weeklyResponse.data && Array.isArray(weeklyResponse.data)) {
      // 确保评分是数字类型
      weeklyRanking.value = weeklyResponse.data.slice(0, 5).map(item => ({
        ...item,
        rating: Number(item.rating) || 0
      }));
      console.log('周榜数据加载成功:', weeklyRanking.value);
    }
    
    if (monthlyResponse.data && Array.isArray(monthlyResponse.data)) {
      // 确保评分是数字类型
      monthlyRanking.value = monthlyResponse.data.slice(0, 5).map(item => ({
        ...item,
        rating: Number(item.rating) || 0
      }));
      console.log('月榜数据加载成功:', monthlyRanking.value);
    }
    
    if (yearlyResponse.data && Array.isArray(yearlyResponse.data)) {
      // 确保评分是数字类型
      yearlyRanking.value = yearlyResponse.data.slice(0, 5).map(item => ({
        ...item,
        rating: Number(item.rating) || 0
      }));
      console.log('年榜数据加载成功:', yearlyRanking.value);
    }
    
    console.log('排行榜数据加载完成');
    console.log('当前周榜数据:', weeklyRanking.value);
  } catch (error) {
    console.error('加载排行榜数据失败:', error);
    // 加载失败时使用默认数据
    loadDefaultRankingData();
  }
};

// 加载默认排行榜数据（当API调用失败时）
const loadDefaultRankingData = () => {
  weeklyRanking.value = [
    {
      id: 1,
      title: "进击的巨人",
      description: "人类与巨人的史诗对决，为了自由而战",
      image: "http://localhost:8080/covers/giant.webp",
      status: 1,
      genre: "动作, 科幻",
      watchCount: 12500,
      rating: 9.3
    },
    {
      id: 2,
      title: "海贼王",
      description: "路飞和伙伴们的冒险，寻找传说中的One Piece",
      image: "http://localhost:8080/covers/onepiece.webp",
      status: 1,
      genre: "冒险, 动作",
      watchCount: 11800,
      rating: 9.2
    },
    {
      id: 4,
      title: "死神",
      description: "黑崎一护成为死神代理的故事",
      image: "http://localhost:8080/covers/death.webp",
      status: 1,
      genre: "动作, 奇幻",
      watchCount: 9200,
      rating: 8.8
    },
    {
      id: 5,
      title: "电锯人",
      description: "电次与波奇塔签订契约成为电锯人",
      image: "http://localhost:8080/covers/dian.webp",
      status: 1,
      genre: "黑暗, 动作",
      watchCount: 8700,
      rating: 8.7
    },
    {
      id: 3,
      title: "火影忍者",
      description: "鸣人成为火影的成长故事",
      image: "http://localhost:8080/covers/huo.webp",
      status: 1,
      genre: "动作, 冒险",
      watchCount: 8500,
      rating: 8.6
    }
  ];
  
  monthlyRanking.value = weeklyRanking.value;
  yearlyRanking.value = weeklyRanking.value;
};

// 新片上线数据
const newAnimes = ref<Array<{
  id: number;
  title: string;
  year: string;
  image: string;
  status: number;
}>>([]);

// 推荐数据
const recommendations = ref<Array<{
  id: number;
  title: string;
  category: string;
  image: string;
  status: number;
}>>([]);

// 热门动漫数据
const popularAnime = ref<any[]>([]);

// 轮播图数据
const carouselData = ref<any[]>([]);

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
  // 如果是Windows本地路径，转换为HTTP URL
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

// 获取图片主色调
const getImageMainColor = (imageUrl: string): Promise<string> => {
  return new Promise((resolve) => {
    const img = new Image();
    // 设置crossOrigin属性，允许跨域图片
    img.crossOrigin = 'anonymous';
    
    img.onload = () => {
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        resolve('#1a1a1a');
        return;
      }
      
      try {
        // 缩小画布尺寸以提高性能
        canvas.width = 50;
        canvas.height = 50;
        ctx.drawImage(img, 0, 0, 50, 50);
        
        const imageData = ctx.getImageData(0, 0, 50, 50);
        const data = imageData.data;
        const colorCounts: { [key: string]: number } = {};
        
        // 遍历像素数据，统计颜色出现次数
        for (let i = 0; i < data.length; i += 4) {
          const r = data[i] || 0;
          const g = data[i + 1] || 0;
          const b = data[i + 2] || 0;
          const alpha = data[i + 3] || 0;
          // 跳过透明像素
          if (alpha < 100) continue;
          // 量化颜色，减少颜色种类
          const rQuantized = Math.round(r / 32) * 32;
          const gQuantized = Math.round(g / 32) * 32;
          const bQuantized = Math.round(b / 32) * 32;
          const color = `rgb(${rQuantized}, ${gQuantized}, ${bQuantized})`;
          colorCounts[color] = (colorCounts[color] || 0) + 1;
        }
        
        // 找出出现次数最多的颜色
        let maxCount = 0;
        let mainColor = '#1a1a1a';
        
        for (const [color, count] of Object.entries(colorCounts)) {
          if (count > maxCount) {
            maxCount = count;
            mainColor = color;
          }
        }
        
        console.log('Detected main color for image:', imageUrl, mainColor);
        resolve(mainColor);
      } catch (error) {
        console.error('Error getting image main color:', error);
        // 如果出现跨域错误，根据图片URL生成一个固定的颜色
        let hash = 0;
        for (let i = 0; i < imageUrl.length; i++) {
          const char = imageUrl.charCodeAt(i);
          hash = ((hash << 5) - hash) + char;
          hash = hash & hash; // Convert to 32bit integer
        }
        
        // 使用哈希值生成一个颜色
        const hue = Math.abs(hash) % 360;
        const saturation = 70;
        const lightness = 40;
        
        const color = `hsl(${hue}, ${saturation}%, ${lightness}%)`;
        console.log('Generated color for image (fallback):', imageUrl, color);
        resolve(color);
      }
    };
    img.onerror = () => {
      console.error('Failed to load image:', imageUrl);
      // 如果图片加载失败，根据图片URL生成一个固定的颜色
      let hash = 0;
      for (let i = 0; i < imageUrl.length; i++) {
        const char = imageUrl.charCodeAt(i);
        hash = ((hash << 5) - hash) + char;
        hash = hash & hash; // Convert to 32bit integer
      }
      
      // 使用哈希值生成一个颜色
      const hue = Math.abs(hash) % 360;
      const saturation = 70;
      const lightness = 40;
      
      const color = `hsl(${hue}, ${saturation}%, ${lightness}%)`;
      console.log('Generated color for image (error fallback):', imageUrl, color);
      resolve(color);
    };
    img.src = getImageUrl(imageUrl);
  });
};

// 为轮播图数据添加主色调
const addMainColorsToCarousel = async () => {
  for (const item of carouselData.value) {
    if (item.image) {
      item.mainColor = await getImageMainColor(item.image);
    }
  }
};

// 加载热门动漫数据
const loadPopularAnime = async () => {
  try {
    console.log('开始加载周榜动漫...');
    const res = await axios.get('/api/anime/ranking/weekly');
    console.log('周榜动漫响应:', res.data);
    if (res.data && Array.isArray(res.data)) {
      // 确保评分是数字类型
      popularAnime.value = res.data.map(item => ({
        ...item,
        rating: Number(item.rating) || 0
      }));
      // 轮播图使用前5个周榜动漫
      carouselData.value = res.data.slice(0, 5).map(item => ({
        ...item,
        rating: Number(item.rating) || 0
      }));
      console.log('周榜动漫加载完成');
      // 为轮播图添加主色调
      await addMainColorsToCarousel();
    }
  } catch (error) {
    console.error('加载周榜动漫失败:', error);
  }
};

// 从后端获取动漫数据
const loadAnimes = async () => {
  try {
    console.log('开始加载动漫数据...');
    const response = await fetch('/api/anime/list');
    console.log('API响应状态:', response.status);
    
    if (response.ok) {
      const data = await response.json();
      console.log('获取到的动漫数据：', data);
      
      // 检查数据格式
      if (Array.isArray(data)) {
        // 过滤出上架的动漫
        const publishedAnimes = data.filter((anime: any) => anime.status === 1);
        console.log('上架的动漫数量:', publishedAnimes.length);
        
        // 获取热门动漫作为轮播图数据
        try {
          const weeklyResponse = await fetch('/api/anime/ranking/weekly');
          if (weeklyResponse.ok) {
            const weeklyData = await weeklyResponse.json();
            console.log('获取到的周榜动漫数据：', weeklyData);
            
            if (Array.isArray(weeklyData)) {
              // 转换周榜动漫数据格式，确保评分是数字类型
              carouselData.value = weeklyData.slice(0, 5).map((item: any) => ({
                id: item.id,
                title: item.title,
                description: item.description || '',
                image: item.image,
                status: 1,
                rating: Number(item.rating) || 0
              }));
              console.log('轮播图数据（周榜动漫）更新完成');
              // 为轮播图添加主色调
              await addMainColorsToCarousel();
            }
          } else {
            console.error('获取周榜动漫失败，使用默认轮播图数据');
            // 如果获取周榜动漫失败，使用默认数据
            if (publishedAnimes.length > 0) {
              carouselData.value = publishedAnimes.slice(0, 5).map((anime: any) => ({
                id: anime.id,
                title: anime.title,
                description: anime.description,
                image: anime.image,
                status: anime.status
              }));
              console.log('轮播图数据（默认）更新完成');
              // 为轮播图添加主色调
              await addMainColorsToCarousel();
            }
          }
        } catch (error) {
          console.error('获取周榜动漫失败：', error);
          // 如果获取周榜动漫失败，使用默认数据
          if (publishedAnimes.length > 0) {
            carouselData.value = publishedAnimes.slice(0, 5).map((anime: any) => ({
              id: anime.id,
              title: anime.title,
              description: anime.description,
              image: anime.image,
              status: anime.status
            }));
            console.log('轮播图数据（默认）更新完成');
            // 为轮播图添加主色调
            await addMainColorsToCarousel();
          }
        }
        
        // 按年份排序（最新）
        const sortedByYear = [...publishedAnimes].sort((a: any, b: any) => {
          const yearA = parseInt(a.year) || 0;
          const yearB = parseInt(b.year) || 0;
          return yearB - yearA;
        });
        
        // 更新新片上线数据
        newAnimes.value = sortedByYear.slice(0, 4).map((anime: any) => ({
          id: anime.id,
          title: anime.title,
          year: anime.year,
          image: anime.image,
          status: anime.status
        }));
        console.log('新片上线数据更新完成');
      } else {
        console.error('API返回的数据格式错误:', data);
      }
    } else {
      console.error('API请求失败，状态码:', response.status);
    }
  } catch (error) {
    console.error('获取动漫数据失败:', error);
  }
};

// 加载个性化推荐
const loadRecommendations = async () => {
  try {
    const username = localStorage.getItem('username') || 'user1';
    console.log('开始加载个性化推荐...');
    const response = await axios.post('/api/recommendation/personalized', {
      username: username
    });
    console.log('个性化推荐响应:', response.data);
    
    if (response.data.code === 200 && response.data.data && response.data.data.length > 0) {
      recommendations.value = response.data.data.map((item: any) => ({
        id: item.id,
        title: item.title,
        category: item.category || item.genre,
        image: item.image,
        status: 1
      }));
      console.log('个性化推荐加载完成');
    } else {
      // 如果没有推荐数据，使用默认推荐
      console.log('没有推荐数据，使用默认推荐');
      loadDefaultRecommendations();
    }
  } catch (error) {
    console.error('加载个性化推荐失败:', error);
    // 加载失败时使用默认推荐
    loadDefaultRecommendations();
  }
};

// 加载默认推荐数据
const loadDefaultRecommendations = () => {
  recommendations.value = [
    {
      id: 1,
      title: "进击的巨人",
      category: "动作, 科幻",
      image: "/covers/giant.webp",
      status: 1
    },
    {
      id: 2,
      title: "火影忍者",
      category: "动作, 冒险",
      image: "/covers/huo.webp",
      status: 1
    },
    {
      id: 3,
      title: "死神",
      category: "动作, 奇幻",
      image: "/covers/death.webp",
      status: 1
    },
    {
      id: 4,
      title: "电锯人",
      category: "黑暗, 动作",
      image: "/covers/dian.webp",
      status: 1
    },
    {
      id: 5,
      title: "海贼王",
      category: "冒险, 动作",
      image: "/covers/onepiece.webp",
      status: 1
    }
  ];
  console.log('默认推荐数据加载完成');
};





onMounted(async () => {
  updateCarousel();
  startAutoplay();
  checkAdmin();
  await loadAnimes();
  await addMainColorsToCarousel();
  loadRecommendations();
  loadPopularAnime();
  loadRankingData();
});

onUnmounted(() => {
  stopAutoplay();
});
</script>

<style scoped>
.index-container {
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

/* 轮播图 */
.carousel {
  margin-top: 20px;
  position: relative;
  max-width: 1240px;
  margin-left: auto;
  margin-right: auto;
  height: 500px;
  overflow: hidden;
  perspective: 1500px;
}

.carousel-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d;
}

.carousel-item {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  transform-style: preserve-3d;
  overflow: hidden;
  border-radius: 8px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
  z-index: 1;
  opacity: 0;
  visibility: hidden;
  display: flex;
  align-items: center;
  background-color: #1a1a1a;
}

.carousel-item.active {
  transform: scale(1) translateZ(0);
  z-index: 3;
  opacity: 1;
  visibility: visible;
}

.carousel-item.prev {
  transform: scale(0.9) translateX(-40%) translateZ(-100px);
  z-index: 2;
  opacity: 0.7;
  visibility: visible;
}

.carousel-item.next {
  transform: scale(0.9) translateX(40%) translateZ(-100px);
  z-index: 2;
  opacity: 0.7;
  visibility: visible;
}

.carousel-item img {
  height: 100%;
  max-width: 80%;
  object-fit: contain;
  margin-left: auto;
  margin-right: 100px;
  display: block;
  transition: transform 0.5s;
}

.carousel-item:hover img {
  transform: scale(1.05);
}

.carousel-caption {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
  color: #fff;
  padding: 40px;
}

.carousel-title {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 10px;
}

.carousel-desc {
  font-size: 16px;
  margin-bottom: 20px;
  opacity: 0.9;
}

.carousel-btn {
  background: #ff6b6b;
  color: #fff;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s;
}

.carousel-btn:hover {
  background: #ff5252;
}

.carousel-control {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  border: none;
  padding: 15px;
  border-radius: 50%;
  font-size: 24px;
  cursor: pointer;
  z-index: 10;
  transition: background 0.3s;
}

.carousel-control:hover {
  background: rgba(0, 0, 0, 0.8);
}

.carousel-control.prev {
  left: 20px;
}

.carousel-control.next {
  right: 20px;
}

.carousel-indicators {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
  z-index: 10;
}

.indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: background 0.3s;
}

.indicator.active {
  background: #ff6b6b;
}

.indicator:hover {
  background: rgba(255, 255, 255, 0.8);
}

/* 排行榜 */
.ranking-container {
  max-width: 1200px;
  margin: 40px auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.ranking-header {
  margin-bottom: 20px;
}

.ranking-header h2 {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.ranking-tabs {
  display: flex;
  margin-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.ranking-tab {
  background: none;
  border: none;
  padding: 10px 20px;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
  border-bottom: 2px solid transparent;
}

.ranking-tab:hover {
  color: #ff6b6b;
}

.ranking-tab.active {
  color: #ff6b6b;
  border-bottom-color: #ff6b6b;
  font-weight: 500;
}

.ranking-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.ranking-number {
  position: absolute;
  top: 10px;
  left: 10px;
  font-size: 24px;
  font-weight: bold;
  color: #ff6b6b;
  background: rgba(0, 0, 0, 0.7);
  padding: 5px 10px;
  border-radius: 4px;
  z-index: 10;
}

.anime-card {
  position: relative;
  background: #f9f9f9;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
}

/* 新片上线 */
.new-anime-container {
  max-width: 1200px;
  margin: 40px auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h2 {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.more-btn {
  color: #ff6b6b;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s;
}

.more-btn:hover {
  color: #ff5252;
}

.more-btn span {
  margin-left: 5px;
  font-size: 16px;
}

.new-anime-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.anime-card {
  background: #f9f9f9;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
}

.anime-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.anime-card-image {
  aspect-ratio: 2/3;
  overflow: hidden;
}

.anime-card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.anime-card:hover .anime-card-image img {
  transform: scale(1.05);
}

.anime-card-info {
  padding: 15px;
}

.anime-card-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin: 0 0 10px 0;
  height: 40px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.anime-card-year {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.anime-card-category {
  font-size: 14px;
  color: #666;
  margin: 0 0 5px 0;
}

.anime-card-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 5px;
}

.anime-card-watch-count {
  font-size: 12px;
  color: #999;
}

.anime-card-rating {
  font-size: 12px;
  color: #999;
}

/* 推荐模块 */
.recommendation-container {
  max-width: 1200px;
  margin: 40px auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.recommendation-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

/* 热门动漫模块 */
.popular-anime-container {
  max-width: 1200px;
  margin: 40px auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.popular-anime-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
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

  .carousel-item {
    height: 300px;
  }

  .carousel-caption {
    padding: 20px;
  }

  .carousel-title {
    font-size: 20px;
  }

  .carousel-desc {
    font-size: 14px;
  }

  .ranking-container,
  .new-anime-container {
    padding: 15px;
    margin: 20px auto;
  }

  .ranking-item {
    flex-direction: column;
    text-align: center;
  }

  .ranking-number {
    margin-right: 0;
    margin-bottom: 10px;
  }

  .ranking-image {
    margin-right: 0;
    margin-bottom: 10px;
  }

  .new-anime-list {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 15px;
  }

  .anime-card-image {
    height: 200px;
  }

  .footer-content {
    padding: 0 15px;
  }

  .sponsor-links {
    flex-direction: column;
    align-items: center;
  }

  .sponsor-link {
    width: 200px;
    text-align: center;
  }
}
</style>