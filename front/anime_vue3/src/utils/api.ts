import axios from 'axios';

// 创建axios实例
const api = axios.create({
  timeout: 10000
});

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response;
  },
  async error => {
    const originalRequest = error.config;
    
    // 如果是401错误且不是重试请求
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        // 获取刷新令牌
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
          // 没有刷新令牌，跳转到登录页
          window.location.href = '/auth';
          return Promise.reject(error);
        }
        
        // 调用刷新令牌接口
        const response = await axios.post('/api/user/refresh-token', {
          refreshToken
        });
        
        if (response.data.code === 200) {
          // 保存新的令牌
          localStorage.setItem('token', response.data.data.token);
          localStorage.setItem('refreshToken', response.data.data.refreshToken);
          
          // 更新原始请求的token
          originalRequest.headers['Authorization'] = `Bearer ${response.data.data.token}`;
          
          // 重试原始请求
          return api(originalRequest);
        } else {
          // 刷新令牌失败，跳转到登录页
          window.location.href = '/auth';
          return Promise.reject(error);
        }
      } catch (refreshError) {
        // 刷新令牌失败，跳转到登录页
        window.location.href = '/auth';
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);

export default api;