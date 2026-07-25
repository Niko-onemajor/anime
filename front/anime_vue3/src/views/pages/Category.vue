<template>
  <div class="category-container">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="navbar-container">
        <div class="logo">Niko动漫</div>
        <ul class="nav-links">
          <li><a href="/index">首页</a></li>
          <li><a href="/category" class="active">分类</a></li>
          <li><a href="/forum">论坛</a></li>
          <li><a href="/messages">信息</a></li>
          <li><a href="/profile">个人中心</a></li>
          <li v-if="isAdmin"><a href="/admin/users">管理员后台</a></li>
        </ul>
      </div>
    </nav>

    <!-- 搜索栏 -->
    <div class="search-container">
      <div class="search-box">
        <input type="text" v-model="searchKeyword" placeholder="搜索动漫..." class="search-input">
        <button @click="handleSearch" class="search-btn">搜索</button>
      </div>
    </div>

    <!-- 分类标签 -->
    <div class="filter-container">
      <div class="filter-section">
        <h3>年份</h3>
        <div class="filter-tags">
          <span 
            v-for="year in years" 
            :key="year" 
            class="filter-tag" 
            :class="{ active: selectedYear === year }"
            @click="selectYear(year)"
          >
            {{ year }}
          </span>
        </div>
      </div>
      <div class="filter-section">
        <h3>首字母</h3>
        <div class="filter-tags">
          <span 
            v-for="letter in letters" 
            :key="letter" 
            class="filter-tag" 
            :class="{ active: selectedLetter === letter }"
            @click="selectLetter(letter)"
          >
            {{ letter }}
          </span>
        </div>
      </div>
      <div class="filter-section">
        <h3>排序</h3>
        <div class="filter-tags">
          <span 
            v-for="sort in sortOptions" 
            :key="sort.value" 
            class="filter-tag" 
            :class="{ active: selectedSort === sort.value }"
            @click="selectSort(sort.value)"
          >
            {{ sort.label }}
          </span>
          <button 
            class="sort-order-btn" 
            @click="toggleSortOrder"
            :title="sortOrder === 'desc' ? '切换为升序' : '切换为降序'"
          >
            切换
          </button>
        </div>
      </div>
    </div>

    <!-- 动漫列表 -->
    <div v-if="isLoading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    <div v-else class="anime-section">
      <div class="anime-container">
        <div class="anime-item" v-for="anime in displayedAnimes" :key="anime.id" @click="goToDetail(anime)">
          <div class="anime-image">
            <img :src="getImageUrl(anime.image)" :alt="anime.title">
          </div>
          <div class="anime-info">
            <h3 class="anime-title">{{ anime.title }}</h3>
            <p class="anime-desc">{{ anime.description }}</p>
            <div class="anime-meta">
              <span class="anime-year">
                {{ selectedSort === 'newest' ? anime.year : selectedSort === 'hottest' ? (anime.watchCount || 0) + '次观看' : (anime.rating || 0) + '分' }}
              </span>
              <span class="anime-genre">{{ anime.genre }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="pagination-container">
        <button class="page-btn" @click="goToPage(1)" :disabled="currentPage === 1">首页</button>
        <button class="page-btn" @click="goToPage(currentPage - 1)" :disabled="currentPage === 1">上一页</button>
        <span 
          v-for="p in pageNumbers" 
          :key="p" 
          class="page-num" 
          :class="{ active: p === currentPage }"
          @click="goToPage(p)"
        >{{ p }}</span>
        <button class="page-btn" @click="goToPage(currentPage + 1)" :disabled="currentPage === totalPages">下一页</button>
        <button class="page-btn" @click="goToPage(totalPages)" :disabled="currentPage === totalPages">尾页</button>
        <span class="page-total">共 {{ totalPages }} 页 / {{ filteredAnimes.length }} 条</span>
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
import { ElMessage } from 'element-plus';
import api from '@/utils/api';

const router = useRouter();

// 检查用户是否为管理员
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
  if (anime.status !== 1) {
    ElMessage.warning('该动漫已被下架');
  } else {
    // 保存筛选状态到localStorage
    localStorage.setItem('categoryFilters', JSON.stringify({
      selectedYear: selectedYear.value,
      selectedLetter: selectedLetter.value,
      selectedSort: selectedSort.value,
      sortOrder: sortOrder.value,
      searchKeyword: searchKeyword.value
    }));
    // 保存来源页面
    localStorage.setItem('fromPage', '/category');
    router.push(`/anime/${anime.id}`);
  }
};

// 搜索关键字
const searchKeyword = ref('');

// 筛选条件
const selectedYear = ref('All');
const selectedLetter = ref('All');
const selectedSort = ref('newest');
const sortOrder = ref('desc'); // 排序顺序：desc-降序，asc-升序
const isLoading = ref(true); // 加载状态
const currentPage = ref(1); // 当前页码
const pageSize = 6; // 每页数量

// 年份选项
const years = ['All', '2026', '2025', '2024', '2023', '2022', '2021', '2020', '2019', '2018', '2017', '2016', '2015', '2014', '2013', '2012', '2011', '2010'];

// 首字母选项
const letters = ['All', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'];

// 排序选项
const sortOptions = [
  { label: '最新', value: 'newest' },
  { label: '最热', value: 'hottest' },
  { label: '评分', value: 'rating' }
];

// 动漫数据
const animes = ref<any[]>([]);

// 从后端获取动漫数据
onMounted(async () => {
  checkAdmin();
  
  // 检查用户登录状态，确保筛选状态与当前用户关联
  const currentUsername = localStorage.getItem('username') || sessionStorage.getItem('username');
  const savedFilters = localStorage.getItem('categoryFilters');
  
  if (savedFilters) {
    try {
      const filters = JSON.parse(savedFilters);
      // 检查是否有用户信息，以及筛选状态是否属于当前用户
      if (currentUsername && filters.lastUser === currentUsername) {
        selectedYear.value = filters.selectedYear || 'All';
        selectedLetter.value = filters.selectedLetter || 'All';
        selectedSort.value = filters.selectedSort || 'newest';
        sortOrder.value = filters.sortOrder || 'desc';
        searchKeyword.value = filters.searchKeyword || '';
      } else {
        // 未登录用户或筛选状态不属于当前用户，使用默认筛选状态
        resetFilterState();
      }
    } catch (error) {
      console.error('恢复筛选状态失败:', error);
      resetFilterState();
    }
  } else {
    // 没有保存的筛选状态，使用默认值
    resetFilterState();
  }
  
  // 加载动漫数据
  await loadAnimeData();
});

// 重置筛选状态
const resetFilterState = () => {
  selectedYear.value = 'All';
  selectedLetter.value = 'All';
  selectedSort.value = 'newest';
  sortOrder.value = 'desc';
  searchKeyword.value = '';
  // 保存默认状态
  saveFilterState();
};

// 加载全部动漫数据（一次性加载，前端筛选+分页展示）
const loadAnimeData = async () => {
  try {
    isLoading.value = true;
    const response = await api.get('/api/anime/list');
    const data = response.data;
    animes.value = Array.isArray(data) ? data : [];
    
    // 为每个动漫获取观看次数
    for (let anime of animes.value) {
      try {
        const watchCountRes = await api.get(`/api/anime/watch-count/${anime.id}`);
        anime.watchCount = watchCountRes.data.watchCount || 0;
      } catch {
        anime.watchCount = 0;
      }
    }
  } catch (error) {
    console.error('获取动漫数据失败:', error);
  } finally {
    isLoading.value = false;
  }
};

// 模拟动漫数据（备用）
const animesBackup = ref([
  {
    id: 1,
    title: '进击的巨人',
    description: '人类与巨人的史诗对决，为了自由而战',
    image: 'http://localhost:8080/covers/giant.webp',
    year: '2013',
    genre: '动作, 科幻',
    rating: 9.8,
    letter: 'J',
    watchCount: 150
  },

  {
    id: 2,
    title: '海贼王',
    description: '路飞和伙伴们的冒险，寻找传说中的One Piece',
    image: 'http://localhost:8080/covers/onepiece.webp',
    year: '1999',
    genre: '冒险, 动作',
    rating: 9.9,
    letter: 'H',
    watchCount: 200
  },
  {
    id: 3,
    title: '火影忍者',
    description: '鸣人成为火影的成长故事',
    image: 'http://localhost:8080/covers/huo.webp',
    year: '2002',
    genre: '动作, 冒险',
    rating: 9.6,
    letter: 'H',
    watchCount: 180
  },
  {
    id: 4,
    title: '死神',
    description: '黑崎一护成为死神代理的故事',
    image: 'http://localhost:8080/covers/death.webp',
    year: '2004',
    genre: '动作, 奇幻',
    rating: 9.4,
    letter: 'S',
    watchCount: 120
  },
  {
    id: 5,
    title: '电锯人',
    description: '电次与波奇塔签订契约成为电锯人',
    image: 'http://localhost:8080/covers/dian.webp',
    year: '2022',
    genre: '黑暗, 动作',
    rating: 9.7,
    letter: 'D',
    watchCount: 160
  },
  {
    id: 6,
    title: '七龙珠',
    description: '孙悟空为了寻找七颗龙珠而展开的冒险',
    image: 'http://localhost:8080/covers/qi.webp',
    year: '1986',
    genre: '动作, 冒险',
    rating: 9.5,
    letter: 'Q',
    watchCount: 140
  }
]);

// 筛选后的动漫列表
const filteredAnimes = computed(() => {
  let result = animes.value.length > 0 ? [...animes.value] : [...animesBackup.value];
  
  // 确保所有动漫都有watchCount属性
  result = result.map(anime => {
    return {
      ...anime,
      watchCount: anime.watchCount || 0
    };
  });
  
  // 按搜索关键字筛选
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase();
    result = result.filter(anime => 
      anime.title.toLowerCase().includes(keyword) || 
      anime.description.toLowerCase().includes(keyword) ||
      anime.genre.toLowerCase().includes(keyword)
    );
  }
  
  // 按年份筛选
  if (selectedYear.value !== 'All') {
    // 转换类型进行比较，因为后端返回的year是数字类型，前端是字符串
    result = result.filter(anime => String(anime.year) === selectedYear.value);
  }
  
  // 按首字母筛选
  if (selectedLetter.value !== 'All') {
    result = result.filter(anime => anime.letter === selectedLetter.value);
  }
  
  // 排序
  switch (selectedSort.value) {
    case 'newest':
      result.sort((a, b) => {
        const yearA = parseInt(a.year) || 0;
        const yearB = parseInt(b.year) || 0;
        return sortOrder.value === 'desc' ? yearB - yearA : yearA - yearB;
      });
      break;
    case 'hottest':
      // 按观看次数排序，如果没有watchCount属性，则按评分排序
      result.sort((a, b) => {
        const countA = a.watchCount || 0;
        const countB = b.watchCount || 0;
        if (countA !== countB) {
          return sortOrder.value === 'desc' ? countB - countA : countA - countB;
        }
        // 观看次数相同时按评分排序
        const ratingA = a.rating || 0;
        const ratingB = b.rating || 0;
        return sortOrder.value === 'desc' ? ratingB - ratingA : ratingA - ratingB;
      });
      break;
    case 'rating':
      result.sort((a, b) => {
        const ratingA = a.rating || 0;
        const ratingB = b.rating || 0;
        return sortOrder.value === 'desc' ? ratingB - ratingA : ratingA - ratingB;
      });
      break;
  }
  
  return result;
});

// 分页展示：从筛选结果中取当前页
const displayedAnimes = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredAnimes.value.slice(start, start + pageSize);
});

// 总页数
const totalPages = computed(() => {
  return Math.ceil(filteredAnimes.value.length / pageSize) || 1;
});

// 显示的页码按钮（最多显示7个）
const pageNumbers = computed(() => {
  const total = totalPages.value;
  const curr = currentPage.value;
  const pages: number[] = [];
  
  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i);
  } else {
    pages.push(1);
    if (curr > 3) pages.push(-1); // 用 -1 表示省略号
    const start = Math.max(2, curr - 1);
    const end = Math.min(total - 1, curr + 1);
    for (let i = start; i <= end; i++) pages.push(i);
    if (curr < total - 2) pages.push(-1);
    pages.push(total);
  }
  return pages;
});

// 跳转到指定页
const goToPage = (page: number) => {
  if (page < 1 || page > totalPages.value) return;
  currentPage.value = page;
};

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1;
  saveFilterState();
};

// 选择年份
const selectYear = (year: string) => {
  selectedYear.value = year;
  currentPage.value = 1;
  saveFilterState();
};

// 选择首字母
const selectLetter = (letter: string) => {
  selectedLetter.value = letter;
  currentPage.value = 1;
  saveFilterState();
};

// 选择排序
const selectSort = (sort: string) => {
  selectedSort.value = sort;
  currentPage.value = 1;
  saveFilterState();
};

// 切换排序顺序
const toggleSortOrder = () => {
  sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc';
  // 保存排序状态到localStorage
  saveFilterState();
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
  // 默认返回原路径
  return url;
};

// 保存筛选状态到localStorage
const saveFilterState = () => {
  const currentUsername = localStorage.getItem('username') || sessionStorage.getItem('username');
  const filterState = {
    selectedYear: selectedYear.value,
    selectedLetter: selectedLetter.value,
    selectedSort: selectedSort.value,
    sortOrder: sortOrder.value,
    searchKeyword: searchKeyword.value,
    lastUser: currentUsername // 记录最后修改筛选状态的用户
  };
  localStorage.setItem('categoryFilters', JSON.stringify(filterState));
};
</script>

<style scoped>
.category-container {
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

/* 搜索栏样式 */
.search-container {
  background: white;
  padding: 20px 0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  margin-bottom: 20px;
}

.search-box {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  padding: 0 20px;
}

.search-input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px 0 0 4px;
  font-size: 16px;
}

.search-btn {
  padding: 10px 20px;
  background: #ff6b6b;
  color: white;
  border: none;
  border-radius: 0 4px 4px 0;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.3s;
}

.search-btn:hover {
  background: #ff5252;
}

/* 筛选容器样式 */
.filter-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  margin-bottom: 30px;
}

.filter-section {
  margin-bottom: 20px;
}

.filter-section h3 {
  margin-bottom: 10px;
  color: #333;
  font-size: 18px;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-tag {
  padding: 8px 16px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}

.filter-tag:hover {
  border-color: #ff6b6b;
  color: #ff6b6b;
}

.filter-tag.active {
  background: #ff6b6b;
  color: white;
  border-color: #ff6b6b;
}

.sort-order-btn {
  padding: 8px 12px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
  margin-left: 10px;
}

.sort-order-btn:hover {
  border-color: #ff6b6b;
  color: #ff6b6b;
}

/* 加载状态样式 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 40px;
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

/* 动漫列表样式 */
.anime-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.anime-item {
  background: white;
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
  height: 200px;
  overflow: hidden;
}

.anime-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.anime-item:hover .anime-image img {
  transform: scale(1.05);
}

.anime-info {
  padding: 15px;
}

.anime-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
}

.anime-desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 15px;
  line-height: 1.4;
  height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
}

.anime-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
}

.anime-year {
  background: #f0f0f0;
  padding: 2px 8px;
  border-radius: 10px;
}

.anime-genre {
  background: #f0f0f0;
  padding: 2px 8px;
  border-radius: 10px;
}

/* 分页样式 */
.pagination-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 20px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.page-btn {
  padding: 8px 16px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  transition: all 0.3s;
}

.page-btn:hover:not(:disabled) {
  border-color: #ff6b6b;
  color: #ff6b6b;
}

.page-btn:disabled {
  background: #f5f5f5;
  color: #ccc;
  cursor: not-allowed;
}

.page-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 36px;
  padding: 0 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  background: white;
  transition: all 0.3s;
}

.page-num:hover {
  border-color: #ff6b6b;
  color: #ff6b6b;
}

.page-num.active {
  background: #ff6b6b;
  color: white;
  border-color: #ff6b6b;
}

.page-total {
  font-size: 14px;
  color: #999;
  margin-left: 8px;
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

/* 响应式设计 */
@media (max-width: 768px) {
  .navbar-container {
    padding: 0 15px;
  }
  
  .nav-links li {
    margin-left: 15px;
  }
  
  .search-box {
    padding: 0 15px;
  }
  
  .filter-container {
    padding: 0 15px;
  }
  
  .anime-container {
    padding: 0 15px;
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  }
  
  .footer-content {
    padding: 0 15px;
  }
}
</style>