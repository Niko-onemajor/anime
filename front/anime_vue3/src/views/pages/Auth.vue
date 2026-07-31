<template>
  <div class="auth-container">
    <div class="auth-box">
      <h2>{{ isLogin ? '登录' : '注册' }}</h2>
      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="username">用户名</label>
          <input 
            type="text" 
            id="username" 
            v-model="form.username" 
            required 
            @blur="validateUsername"
            @input="validateUsername"
            :class="{ 'error': usernameError }"
            autocomplete="username"
            placeholder="请输入用户名（3-10个字符）"
          />
          <div v-if="usernameError" class="error-message">{{ usernameError }}</div>
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <div class="password-input">
            <input 
              :type="showPassword ? 'text' : 'password'" 
              id="password" 
              v-model="form.password" 
              required 
              @blur="validatePassword"
              @input="validatePassword"
              :class="{ 'error': passwordError }"
              :autocomplete="isLogin ? 'current-password' : 'new-password'"
              placeholder="请输入密码（8-20位，含大小写字母和数字）"
            />
            <button type="button" class="toggle-password" @click="showPassword = !showPassword">
              {{ showPassword ? '隐藏' : '显示' }}
            </button>
          </div>
          <div v-if="passwordError" class="error-message">{{ passwordError }}</div>
          <!-- 密码强度指示器（仅注册时显示） -->
          <div v-if="!isLogin && form.password" class="password-strength">
            <div class="strength-bar">
              <div class="strength-fill" :class="strengthClass" :style="{ width: strengthPercent + '%' }"></div>
            </div>
            <span class="strength-text" :class="strengthClass">{{ strengthText }}</span>
          </div>
        </div>
        <div class="form-group" v-if="!isLogin">
          <label for="confirmPassword">确认密码</label>
          <div class="password-input">
            <input 
              :type="showConfirmPassword ? 'text' : 'password'" 
              id="confirmPassword" 
              v-model="form.confirmPassword" 
              required 
              @blur="validateConfirmPassword"
              @input="validateConfirmPassword"
              :class="{ 'error': confirmPasswordError }"
              autocomplete="new-password"
              placeholder="请再次输入密码"
            />
            <button type="button" class="toggle-password" @click="showConfirmPassword = !showConfirmPassword">
              {{ showConfirmPassword ? '隐藏' : '显示' }}
            </button>
          </div>
          <div v-if="confirmPasswordError" class="error-message">{{ confirmPasswordError }}</div>
        </div>

        <button type="submit" class="btn btn-primary" :disabled="loading">{{ loading ? '处理中...' : (isLogin ? '登录' : '注册') }}</button>
      </form>
      <div class="auth-switch">
        <span>{{ isLogin ? '还没有账号？' : '已有账号？' }}</span>
        <a href="#" @click.prevent="isLogin = !isLogin">{{ isLogin ? '立即注册' : '立即登录' }}</a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { ElMessage } from 'element-plus';

const router = useRouter();
const isLogin = ref(true);
const showPassword = ref(false);
const showConfirmPassword = ref(false);
const form = ref({
  username: '',
  password: '',
  confirmPassword: ''
});

const usernameError = ref('');
const passwordError = ref('');
const confirmPasswordError = ref('');
const loading = ref(false);

// 密码强度计算
const passwordStrength = computed(() => {
  const pwd = form.value.password;
  if (!pwd) return 0;
  let score = 0;
  if (pwd.length >= 8) score += 25;
  if (pwd.length >= 12) score += 10;
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score += 25;
  if (/\d/.test(pwd)) score += 20;
  if (/[^a-zA-Z0-9]/.test(pwd)) score += 20;
  return Math.min(score, 100);
});

const strengthPercent = computed(() => passwordStrength.value);
const strengthClass = computed(() => {
  const s = passwordStrength.value;
  if (s >= 80) return 'strong';
  if (s >= 50) return 'medium';
  return 'weak';
});
const strengthText = computed(() => {
  const s = passwordStrength.value;
  if (s >= 80) return '强';
  if (s >= 50) return '中';
  return '弱';
});

// 监听isLogin变化，重置表单
watch(isLogin, () => {
  form.value = {
    username: '',
    password: '',
    confirmPassword: ''
  };
  usernameError.value = '';
  passwordError.value = '';
  confirmPasswordError.value = '';
});

// 验证用户名
const validateUsername = () => {
  const username = form.value.username.trim();
  if (username.length < 3 || username.length > 10) {
    usernameError.value = '用户名长度必须在3-10个字符之间';
    return false;
  }
  if (username.includes(' ')) {
    usernameError.value = '用户名不能包含空格';
    return false;
  }
  // 检查特殊字符（只允许字母、数字、下划线）
  const usernameRegex = /^[a-zA-Z0-9_]+$/;
  if (!usernameRegex.test(username)) {
    usernameError.value = '用户名只能包含字母、数字和下划线';
    return false;
  }
  usernameError.value = '';
  return true;
};

// 验证密码
const validatePassword = () => {
  const password = form.value.password;
  if (password.length < 6 || password.length > 20) {
    passwordError.value = '密码长度必须在6-20个字符之间';
    return false;
  }
  if (password.includes(' ')) {
    passwordError.value = '密码不能包含空格';
    return false;
  }
  passwordError.value = '';
  return true;
};

// 验证确认密码
const validateConfirmPassword = () => {
  if (form.value.password !== form.value.confirmPassword) {
    confirmPasswordError.value = '两次输入的密码不一致';
    return false;
  }
  confirmPasswordError.value = '';
  return true;
};

// 表单验证
const validateForm = () => {
  let isValid = true;
  
  if (!validateUsername()) {
    isValid = false;
  }
  
  if (!validatePassword()) {
    isValid = false;
  }
  
  if (!isLogin.value && !validateConfirmPassword()) {
    isValid = false;
  }
  
  return isValid;
};

const handleSubmit = async () => {
  // 表单验证
  if (!validateForm()) {
    return;
  }
  
  loading.value = true;
  
  try {
    const url = isLogin.value ? '/api/user/login' : '/api/user/register';
    const requestData = isLogin.value ? { ...form.value, rememberMe: false } : form.value;
    const res = await axios.post(url, requestData);
    
    if (res.data.code === 200) {
      if (isLogin.value) {
          // 登录成功，保存用户信息到localStorage（与api.ts拦截器保持一致）
          localStorage.setItem('token', res.data.data.token);
          localStorage.setItem('refreshToken', res.data.data.refreshToken);
          localStorage.setItem('username', res.data.data.username);
          localStorage.setItem('role', res.data.data.role.toString());
          localStorage.setItem('email', res.data.data.email || '');
          // 存储生日为YYYY-MM-DD格式
          if (res.data.data.birthday) {
            const date = new Date(res.data.data.birthday);
            const formattedDate = date.toISOString().split('T')[0];
            localStorage.setItem('birthday', formattedDate || '');
          } else {
            localStorage.setItem('birthday', '');
          }
          localStorage.setItem('favorite', res.data.data.favorite || '');
          localStorage.setItem('avatar', res.data.data.avatar || '');
        // 根据角色跳转到不同页面
        if (res.data.data.role === 'admin' || res.data.data.role === 1) {
          // 管理员跳转到用户管理页面
          router.push('/admin/users');
        } else {
          // 普通用户跳转到首页
          router.push('/index');
        }
      } else {
        // 注册成功，提示用户激活账号
        ElMessage.success('注册成功，请注意查收邮件并激活账号');
        isLogin.value = true;
      }
    } else {
      ElMessage.error(res.data.msg || '操作失败');
    }
  } catch (error) {
    console.error('操作失败：', error);
    const axiosError = error as any;
    if (axiosError.code === 'ERR_CONNECTION_REFUSED') {
      ElMessage.error('无法连接到服务器，请确保后端服务已启动并运行在端口8080');
    } else if (axiosError.response) {
      // 服务器返回错误状态码
      ElMessage.error('操作失败：' + (axiosError.response.data?.msg || '服务器错误'));
    } else if (axiosError.request) {
      // 请求已发出但没有收到响应
      ElMessage.error('无法连接到服务器，请检查网络连接');
    } else {
      // 其他错误
      ElMessage.error('操作出错，请检查网络或重试: ' + axiosError.message);
    }
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
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

.toggle-password:hover {
  color: #667eea;
  background: rgba(102, 126, 234, 0.08);
  border-radius: 4px;
}

.error {
  border-color: #f56c6c !important;
  box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.15);
}

.error-message {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 5px;
}
</style>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.auth-box {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
  animation: authFadeIn 0.5s ease;
}

@keyframes authFadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.auth-box h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
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

.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
  transition: border-color 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
}

.btn {
  display: block;
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
  margin-top: 20px;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover {
  background: #5a67d8;
}

.btn-primary:disabled {
  background: #a0aec0;
  cursor: not-allowed;
}

.auth-switch {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  color: #666;
}

.auth-switch a {
  color: #667eea;
  text-decoration: none;
  margin-left: 5px;
}

.auth-switch a:hover {
  text-decoration: underline;
}

/* 密码强度指示器 */
.password-strength {
  margin-top: 8px;
}

.strength-bar {
  height: 4px;
  background: #e0e0e0;
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s, background 0.3s;
}

.strength-fill.weak {
  background: #f56c6c;
}

.strength-fill.medium {
  background: #e6a23c;
}

.strength-fill.strong {
  background: #67c23a;
}

.strength-text {
  font-size: 12px;
  margin-top: 2px;
  display: inline-block;
}

.strength-text.weak {
  color: #f56c6c;
}

.strength-text.medium {
  color: #e6a23c;
}

.strength-text.strong {
  color: #67c23a;
}

@media (max-width: 480px) {
  .auth-box {
    padding: 24px;
    margin: 0 16px;
    border-radius: 6px;
  }

  .auth-box h2 {
    font-size: 20px;
    margin-bottom: 20px;
  }

  .form-group input {
    padding: 10px;
    font-size: 14px;
  }

  .btn {
    padding: 10px;
    font-size: 14px;
  }
}
</style>