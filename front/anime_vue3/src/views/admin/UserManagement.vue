<template>
  <div class="admin-container">
    <nav class="navbar">
      <div class="navbar-container">
        <div class="logo">Niko动漫 - 管理员</div>
        <ul class="nav-links">
          <li><a href="/admin/users" class="active">用户管理</a></li>
          <li><a href="/admin/animes">动漫管理</a></li>
          <li><a href="/admin/forum">论坛管理</a></li>
          <li><a href="/admin/deleted">删除记录管理</a></li>
          <li><a href="/index">返回前台</a></li>
        </ul>
      </div>
    </nav>

    <div class="container">
      <h2>用户信息管理</h2>
      
      <div class="actions">
        <div class="search-box">
          <input type="text" v-model="searchKeyword" placeholder="搜索用户名..." class="search-input">
          <button @click="searchUsers" class="search-btn">搜索</button>
        </div>
        <button @click="showAddUserDialog = true" class="add-btn">添加用户</button>
      </div>

      <div class="user-list">
        <div class="user-cards">
          <div v-for="user in users" :key="user.id" class="user-card">
            <div class="user-card-header">
              
              <img :src="getImageUrl(user.avatar)" :alt="user.username" class="user-avatar">
              <div class="user-card-info">
                <h3>{{ user.username }}</h3>
                <p class="user-role">{{ user.role === 'admin' ? '管理员' : '普通用户' }}</p>
              </div>
            </div>
            <div class="user-card-body">
              <div class="user-detail">
                <span class="detail-label">ID:</span>
                <span class="detail-value">{{ user.id }}</span>
              </div>
              <div class="user-detail">
                <span class="detail-label">性别:</span>
                <span class="detail-value">{{ user.gender || '-' }}</span>
              </div>
              <div class="user-detail">
                <span class="detail-label">生日:</span>
                <span class="detail-value">{{ user.birthday || '-' }}</span>
              </div>
              <div class="user-detail">
                <span class="detail-label">地区:</span>
                <span class="detail-value">{{ user.region || '-' }}</span>
              </div>
              <div class="user-detail">
                <span class="detail-label">邮箱:</span>
                <span class="detail-value">{{ user.email || '-' }}</span>
              </div>
              <div class="user-detail">
                <span class="detail-label">喜爱的动漫:</span>
                <span class="detail-value">{{ user.favorite || '-' }}</span>
              </div>
              <div class="user-detail">
                <span class="detail-label">签名:</span>
                <span class="detail-value">{{ user.signature || '-' }}</span>
              </div>
            </div>
            <div class="user-card-actions">
              <button @click="editUser(user)" class="edit-btn">编辑</button>
              <button @click="showDeleteConfirm(user.id)" class="delete-btn">删除</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页控件 -->
      <div class="pagination" v-if="pages > 1">
        <div class="pagination-info">
          共 {{ total }} 个用户，第 {{ currentPage }}/{{ pages }} 页
        </div>
        <div class="pagination-controls">
          <button @click="changePage(1)" :disabled="currentPage === 1" class="page-btn">首页</button>
          <button @click="changePage(currentPage - 1)" :disabled="currentPage === 1" class="page-btn">上一页</button>
          <button @click="changePage(currentPage + 1)" :disabled="currentPage === pages" class="page-btn">下一页</button>
          <button @click="changePage(pages)" :disabled="currentPage === pages" class="page-btn">末页</button>
          <div class="page-jump">
            <span>跳转到</span>
            <input type="number" v-model.number="pageJump" min="1" :max="pages" class="page-input">
            <button @click="jumpToPage" class="page-btn">确定</button>
          </div>
        </div>
      </div>

      <div class="dialog-overlay" v-if="showAddUserDialog || showEditUserDialog">
        <div class="dialog-content">
          <div class="dialog-header">
            <h3>{{ showEditUserDialog ? '编辑用户' : '添加用户' }}</h3>
            <button @click="closeDialog" class="close-btn">×</button>
          </div>
          <div class="dialog-body">
            <div class="form-group">
              <label for="role">角色</label>
              <select id="role" v-model="userForm.role" class="form-input">
                <option value="user">普通用户</option>
                <option value="admin">管理员</option>
              </select>
            </div>
            <div class="form-group">
              <label for="username">用户名</label>
              <input type="text" id="username" v-model="userForm.username" placeholder="请输入用户名" class="form-input">
            </div>
            <div class="form-group" v-if="!showEditUserDialog">
              <label for="password">密码</label>
              <div class="password-input">
                <input :type="showPassword ? 'text' : 'password'" id="password" v-model="userForm.password" placeholder="请输入密码" class="form-input">
                <button type="button" class="toggle-password" @click="showPassword = !showPassword">
                  {{ showPassword ? '隐藏' : '显示' }}
                </button>
              </div>
            </div>
            <!-- 只有在编辑用户时显示重置密码按钮 -->
            <div v-if="showEditUserDialog" class="form-group">
              <label>重置密码</label>
              <button @click="resetPassword" class="reset-password-btn">重置密码为123456</button>
            </div>
            <div class="form-group">
              <label for="avatar">头像</label>
              <input type="file" id="avatar" @change="handleAvatarUpload" accept="image/*" class="form-input">
              <div v-if="userForm.avatar" class="avatar-preview">
                <img :src="getImageUrl(userForm.avatar)" alt="头像预览" class="avatar-img" @click="previewAvatar(userForm.avatar)">
                <p class="avatar-hint">点击头像放大查看</p>
              </div>
            </div>
            <div class="form-group">
              <label for="gender">性别</label>
              <select id="gender" v-model="userForm.gender" class="form-input">
                <option value="">请选择</option>
                <option value="男">男</option>
                <option value="女">女</option>
                <option value="其他">其他</option>
              </select>
            </div>
            <div class="form-group">
              <label for="birthday">生日</label>
              <input type="date" id="birthday" v-model="userForm.birthday" class="form-input">
            </div>
            <div class="form-group">
              <label for="region">地区</label>
              <input type="text" id="region" v-model="userForm.region" placeholder="请输入地区" class="form-input">
            </div>
            <div class="form-group">
              <label for="email">邮箱</label>
              <input type="email" id="email" v-model="userForm.email" placeholder="请输入邮箱" class="form-input">
            </div>
            <div class="form-group">
              <label for="favorite">喜爱的动漫</label>
              <input type="text" id="favorite" v-model="userForm.favorite" placeholder="请输入喜爱的动漫" class="form-input">
            </div>
            <div class="form-group">
              <label for="signature">签名</label>
              <textarea id="signature" v-model="userForm.signature" placeholder="请输入个人签名" rows="3" class="form-input"></textarea>
            </div>
          </div>
          <div class="dialog-footer">
            <button @click="closeDialog" class="btn cancel-btn">取消</button>
            <button @click="submitUserForm" class="btn submit-btn">{{ showEditUserDialog ? '保存' : '添加' }}</button>
          </div>
        </div>
      </div>

      <!-- 删除确认对话框 -->
      <div class="dialog-overlay" v-if="showDeleteConfirmDialog">
        <div class="dialog-content">
          <div class="dialog-header">
            <h3>删除用户</h3>
            <button @click="showDeleteConfirmDialog = false" class="close-btn">×</button>
          </div>
          <div class="dialog-body">
            <p>确定要删除该用户吗？此操作不可恢复。</p>
          </div>
          <div class="dialog-footer">
            <button @click="showDeleteConfirmDialog = false" class="btn cancel-btn">取消</button>
            <button @click="confirmDelete" class="btn delete-btn">删除</button>
          </div>
        </div>
      </div>

      <!-- 头像预览对话框 -->
      <div class="dialog-overlay" v-if="showAvatarPreviewDialog">
        <div class="dialog-content avatar-preview-dialog">
          <div class="dialog-header">
            <h3>头像预览</h3>
            <button @click="showAvatarPreviewDialog = false" class="close-btn">×</button>
          </div>
          <div class="dialog-body">
            <div class="avatar-preview-large">
              <img :src="currentAvatarUrl" alt="头像预览" class="avatar-img-large">
            </div>
          </div>
          <div class="dialog-footer">
            <button @click="showAvatarPreviewDialog = false" class="btn cancel-btn">关闭</button>
          </div>
        </div>
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
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { ElMessage, ElMessageBox } from 'element-plus';

// 处理图片URL
const getImageUrl = (url: string | undefined | null): string => {
  if (!url) {
    return 'http://localhost:8080/avatars/avatar1.jpg';
  }
  console.log('原始头像URL:', url);
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
    const result = 'http://localhost:8080/avatars/' + filename;
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

const searchKeyword = ref('');
const users = ref<any[]>([]);// 对话框状态
// 分页相关变量
const currentPage = ref(1);
const total = ref(0);
const pages = ref(0);
const pageSize = ref(6);
const pageJump = ref(1);
const showAddUserDialog = ref(false);
const showEditUserDialog = ref(false);
const showDeleteConfirmDialog = ref(false);
const showAvatarPreviewDialog = ref(false);
const userToDelete = ref(0);
const currentAvatarUrl = ref('');
const userForm = ref({
  id: 0,
  username: '',
  password: '', // 明文密码，用于显示和修改
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  email: '',
  birthday: '',
  role: 'user',
  favorite: '',
  gender: '',
  region: '',
  signature: '',
  avatar: ''
});

// 密码显示/隐藏状态
const showPassword = ref(false);

// 头像文件
const avatarFile = ref<File | null>(null);

const loadUsers = async () => {
  try {
    console.log('开始加载用户列表，页码:', currentPage.value, '每页大小:', pageSize.value);
    const res = await axios.post('http://localhost:8080/api/admin/users', {
      page: currentPage.value,
      size: pageSize.value
    });
    console.log('加载用户列表响应:', res.data);
    if (res.data.code === 200) {
      users.value = res.data.data.map((user: any) => ({
        ...user,
        // 直接使用后端返回的头像URL，因为后端应该已经返回了完整的阿里云URL
        avatar: user.avatar || 'http://localhost:8080/avatars/avatar1.jpg',
        birthday: user.birthday ? user.birthday.split('T')[0] : ''
      }));
      // 更新分页信息
      total.value = res.data.total;
      pages.value = res.data.pages;
      currentPage.value = res.data.currentPage;
      console.log('用户列表加载成功，共', users.value.length, '个用户，总用户数:', total.value, '总页数:', pages.value);
    } else {
      console.error('加载用户列表失败，响应码:', res.data.code);
      ElMessage.error('加载用户列表失败：' + (res.data.msg || '未知错误'));
    }
  } catch (error) {
    console.error('加载用户列表失败：', error);
    const axiosError = error as any;
    if (axiosError.response) {
      ElMessage.error('加载用户列表失败：' + (axiosError.response.data?.msg || '服务器错误'));
    } else if (axiosError.request) {
      ElMessage.error('无法连接到服务器，请检查网络连接');
    } else {
      ElMessage.error('加载用户列表失败：' + axiosError.message);
    }
  }
};

const searchUsers = async () => {
  try {
    console.log('开始搜索用户，关键词:', searchKeyword.value, '页码:', currentPage.value, '每页大小:', pageSize.value);
    const res = await axios.post('http://localhost:8080/api/admin/users/search', {
      keyword: searchKeyword.value,
      page: currentPage.value,
      size: pageSize.value
    });
    console.log('搜索用户响应:', res.data);
    if (res.data.code === 200) {
      users.value = res.data.data.map((user: any) => ({
        ...user,
        // 直接使用后端返回的头像URL，因为后端应该已经返回了完整的阿里云URL
        avatar: user.avatar || 'http://localhost:8080/avatars/avatar1.jpg',
        birthday: user.birthday ? user.birthday.split('T')[0] : ''
      }));
      // 更新分页信息
      total.value = res.data.total;
      pages.value = res.data.pages;
      currentPage.value = res.data.currentPage;
      console.log('搜索用户成功，共', users.value.length, '个结果，总结果数:', total.value, '总页数:', pages.value);
    } else {
      console.error('搜索用户失败，响应码:', res.data.code);
      ElMessage.error('搜索用户失败：' + (res.data.msg || '未知错误'));
    }
  } catch (error) {
    console.error('搜索用户失败：', error);
    const axiosError = error as any;
    if (axiosError.response) {
      ElMessage.error('搜索用户失败：' + (axiosError.response.data?.msg || '服务器错误'));
    } else if (axiosError.request) {
      ElMessage.error('无法连接到服务器，请检查网络连接');
    } else {
      ElMessage.error('搜索用户失败：' + axiosError.message);
    }
  }
};

const addUser = async () => {
  try {
    // 先处理头像上传
    let avatarUrl = userForm.value.avatar;
    if (avatarFile.value) {
      const formData = new FormData();
      formData.append('file', avatarFile.value);
      formData.append('username', userForm.value.username);
      
      const avatarRes = await axios.post('http://localhost:8080/api/user/avatar', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      
      if (avatarRes.data.code === 200) {
        avatarUrl = avatarRes.data.data.avatar;
      }
    }
    
    // 创建一个副本，避免发送不需要的字段
    const userData: any = {
      username: userForm.value.username,
      password: userForm.value.password,
      email: userForm.value.email,
      birthday: userForm.value.birthday,
      role: userForm.value.role,
      favorite: userForm.value.favorite,
      gender: userForm.value.gender,
      region: userForm.value.region,
      signature: userForm.value.signature,
      avatar: avatarUrl
    };
    
    console.log('发送的用户数据:', userData);
    
    const res = await axios.post('http://localhost:8080/api/admin/users/add', userData);
    if (res.data.code === 200) {
      ElMessage.success('用户添加成功！');
      closeDialog();
      // 重新加载当前页的用户列表
      loadUsers();
    } else {
      ElMessage.error(res.data.msg || '添加用户失败');
    }
  } catch (error) {
    console.error('添加用户失败：', error);
    const axiosError = error as any;
    if (axiosError.response) {
      // 服务器返回错误状态码
      ElMessage.error('添加用户失败：' + (axiosError.response.data?.msg || '服务器错误'));
    } else if (axiosError.request) {
      // 请求已发出但没有收到响应
      ElMessage.error('无法连接到服务器，请检查网络连接');
    } else {
      // 其他错误
      ElMessage.error('添加用户失败：' + axiosError.message);
    }
  }
};

// 处理头像上传
const handleAvatarUpload = (e: Event) => {
  const target = e.target as HTMLInputElement;
  if (target.files && target.files[0]) {
    const file = target.files[0];
    avatarFile.value = file;
    // 显示预览
    const reader = new FileReader();
    reader.onload = (e) => {
      // 只用于预览，不修改 userForm.avatar
      // 创建一个临时的预览元素
      const previewElement = document.querySelector('.avatar-preview img') as HTMLImageElement;
      if (previewElement) {
        previewElement.src = e.target?.result as string;
      }
    };
    reader.readAsDataURL(file);
  }
};

const editUser = (user: any) => {
  userForm.value = { ...user };
  // 确保头像路径正确
  if (!user.avatar) {
    userForm.value.avatar = 'http://localhost:8080/avatars/avatar1.jpg';
  }
  // 确保role字段存在且为字符串
  userForm.value.role = user.role || 'user';
  // 确保其他字段存在
  userForm.value.favorite = user.favorite || '';
  userForm.value.gender = user.gender || '';
  userForm.value.region = user.region || '';
  userForm.value.signature = user.signature || '';
  // 清空修改密码相关字段
  userForm.value.oldPassword = '';
  userForm.value.newPassword = '';
  userForm.value.confirmPassword = '';
  showEditUserDialog.value = true;
};

const previewAvatar = (avatarUrl: string) => {
  currentAvatarUrl.value = getImageUrl(avatarUrl);
  showAvatarPreviewDialog.value = true;
};

const updateUser = async () => {
  try {
    // 先处理头像上传
    let avatarUrl = userForm.value.avatar;
    if (avatarFile.value) {
      const formData = new FormData();
      formData.append('file', avatarFile.value);
      formData.append('username', userForm.value.username);
      
      const avatarRes = await axios.post('http://localhost:8080/api/user/avatar', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      
      if (avatarRes.data.code === 200) {
        avatarUrl = avatarRes.data.data.avatar;
      }
    }
    
    // 创建一个副本，避免发送不需要的字段
    const userData: any = {
      ...userForm.value,
      avatar: avatarUrl
    };
    
    // 只在密码不为空时发送密码
    if (!userData.password) {
      delete userData.password;
    }
    
    // 删除修改密码相关字段，避免发送到更新用户接口
    delete userData.oldPassword;
    delete userData.newPassword;
    delete userData.confirmPassword;
    
    console.log('发送的用户数据:', userData);
    
    const res = await axios.post('http://localhost:8080/api/admin/users/update', userData);
    if (res.data.code === 200) {
      ElMessage.success('用户更新成功！');
      closeDialog();
      // 重新加载当前页的用户列表
      loadUsers();
    } else {
      ElMessage.error(res.data.msg || '更新失败');
    }
  } catch (error) {
    console.error('更新用户失败：', error);
    const axiosError = error as any;
    if (axiosError.response) {
      ElMessage.error('更新用户失败：' + (axiosError.response.data?.msg || '服务器错误'));
    } else if (axiosError.request) {
      ElMessage.error('无法连接到服务器，请检查网络连接');
    } else {
      ElMessage.error('更新用户失败：' + axiosError.message);
    }
  }
};

// 显示删除确认对话框
const showDeleteConfirm = (userId: number) => {
  userToDelete.value = userId;
  showDeleteConfirmDialog.value = true;
};

// 确认删除
const confirmDelete = async () => {
  try {
    console.log('删除用户ID:', userToDelete.value);
    const res = await axios.post('http://localhost:8080/api/admin/users/delete', {
      id: userToDelete.value
    });
    console.log('删除用户响应:', res.data);
    if (res.data.code === 200) {
      ElMessage.success('用户删除成功！');
      // 重新加载当前页的用户列表
      loadUsers();
    } else {
      ElMessage.error(res.data.msg || '删除失败');
    }
  } catch (error) {
    console.error('删除用户失败：', error);
    const axiosError = error as any;
    if (axiosError.response) {
      ElMessage.error('删除用户失败：' + (axiosError.response.data?.msg || '服务器错误'));
    } else if (axiosError.request) {
      ElMessage.error('无法连接到服务器，请检查网络连接');
    } else {
      ElMessage.error('删除用户失败：' + axiosError.message);
    }
  } finally {
    showDeleteConfirmDialog.value = false;
  }
};



const submitUserForm = () => {
  if (showEditUserDialog.value) {
    updateUser();
  } else {
    addUser();
  }
};

const resetPassword = async () => {
  try {
    // 二次确认
    await ElMessageBox.confirm(
      '确定要将该用户的密码重置为123456吗？',
      '确认重置密码',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    const res = await axios.post('http://localhost:8080/api/admin/users/reset-password', {
      id: userForm.value.id
    });
    if (res.data.code === 200) {
      ElMessage.success('密码重置成功！新密码为：123456');
    } else {
      ElMessage.error('密码重置失败：' + (res.data.msg || '未知错误'));
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('重置密码失败：', error);
      if (error.response) {
        ElMessage.error('重置密码失败：' + (error.response.data?.msg || '服务器错误'));
      } else if (error.request) {
        ElMessage.error('无法连接到服务器，请检查网络连接');
      } else {
        ElMessage.error('重置密码失败：' + error.message);
      }
    }
  }
};

const closeDialog = () => {
  showAddUserDialog.value = false;
  showEditUserDialog.value = false;
  userForm.value = {
    id: 0,
    username: '',
    password: '',
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    email: '',
    birthday: '',
    role: 'user',
    favorite: '',
    gender: '',
    region: '',
    signature: '',
    avatar: ''
  };
  // 重置头像文件
  avatarFile.value = null;
  // 清空文件输入框
  const fileInput = document.getElementById('avatar') as HTMLInputElement;
  if (fileInput) {
    fileInput.value = '';
  }
};

// 分页相关方法
const changePage = (page: number) => {
  if (page >= 1 && page <= pages.value) {
    currentPage.value = page;
    pageJump.value = page;
    loadUsers();
  }
};

const jumpToPage = () => {
  let page = pageJump.value;
  if (page < 1) {
    page = 1;
  } else if (page > pages.value) {
    page = pages.value;
  }
  currentPage.value = page;
  pageJump.value = page;
  loadUsers();
};

onMounted(() => {
  loadUsers();
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

.actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.search-box {
  display: flex;
  gap: 10px;
}

.search-input {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
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

.user-list {
  margin-top: 20px;
}

.user-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.user-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  padding: 20px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.user-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0,0,0,0.1);
}

.user-card-header {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.user-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 15px;
}

.user-card-info h3 {
  margin: 0 0 5px 0;
  color: #333;
  font-size: 18px;
}

.user-role {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.user-card-body {
  margin-bottom: 15px;
}

.user-detail {
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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-card-actions {
  display: flex;
  gap: 10px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.user-card-actions .edit-btn,
.user-card-actions .delete-btn {
  flex: 1;
  text-align: center;
  margin-right: 0;
}

.edit-btn {
  padding: 6px 12px;
  background: #2196F3;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.3s;
}

.edit-btn:hover {
  background: #0b7dda;
}

.delete-btn {
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
  max-width: 500px;
  box-shadow: 0 5px 15px rgba(0,0,0,0.3);
  display: flex;
  flex-direction: column;
  max-height: 80vh;
}

.dialog-body {
  flex: 1;
  overflow-y: auto;
  max-height: 60vh;
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

.password-input {
  position: relative;
}

.toggle-password {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: #666;
  font-size: 12px;
  padding: 5px;
}

.avatar-preview {
  margin-top: 10px;
}

.avatar-img {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #ddd;
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

.submit-btn {
  background: #ff6b6b;
  color: white;
}

.submit-btn:hover {
  background: #ff5252;
}

.reset-password-btn {
  padding: 10px 20px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.3s;
  width: 100%;
}

.reset-password-btn:hover {
  background: #45a049;
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
  
  .actions {
    flex-direction: column;
    gap: 10px;
  }
  
  .search-box {
    width: 100%;
  }
  
  .search-input {
    flex: 1;
  }
  
  .user-table th,
  .user-table td {
    padding: 8px;
  }
  
  .dialog-content {
    width: 95%;
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

  /* 头像预览相关样式 */
  .avatar-preview {
    margin-top: 10px;
    cursor: pointer;
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .avatar-img {
    width: 120px;
    height: 120px;
    border-radius: 50%;
    object-fit: cover;
    transition: transform 0.2s ease;
  }

  .avatar-img:hover {
    transform: scale(1.1);
  }

  .avatar-hint {
    margin-top: 5px;
    font-size: 12px;
    color: #999;
    text-align: center;
  }

  .avatar-preview-dialog {
    width: 600px;
  }

  .avatar-preview-large {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 20px;
    min-height: 400px;
  }

  .avatar-img-large {
    max-width: 500px;
    max-height: 500px;
    object-fit: contain;
    border-radius: 8px;
  }

  /* 分页控件样式 */
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 0;
    border-top: 1px solid #eee;
  }

  .pagination-info {
    font-size: 14px;
    color: #666;
  }

  .pagination-controls {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .page-btn {
    padding: 6px 12px;
    background: #f0f0f0;
    color: #333;
    border: 1px solid #ddd;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
    transition: background 0.3s;
  }

  .page-btn:hover:not(:disabled) {
    background: #e0e0e0;
  }

  .page-btn:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }

  .page-jump {
    display: flex;
    align-items: center;
    gap: 5px;
  }

  .page-jump span {
    font-size: 14px;
    color: #666;
  }

  .page-input {
    width: 60px;
    padding: 6px;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-size: 14px;
    text-align: center;
  }
</style>