<template>
  <div class="avatar-upload-container">
    <div class="avatar-container">
      <img :src="avatarUrl" alt="头像" class="avatar" />
      <button class="avatar-upload-btn" @click="toggleUpload">+</button>
    </div>
    <my-upload
      field="img"
      @crop-success="cropSuccess"
      @crop-upload-success="cropUploadSuccess"
      @crop-upload-fail="cropUploadFail"
      v-model="showUpload"
      :width="300"
      :height="300"
      url="http://localhost:8081/api/user/avatar"
      :params="uploadParams"
      img-format="png"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import myUpload from 'vue-image-crop-upload';

const showUpload = ref(false);
const avatarUrl = ref('http://localhost:8080/avatars/avatar1.jpg');

const uploadParams = computed(() => {
  return {
    username: localStorage.getItem('username') || 'user1'
  };
});

const toggleUpload = () => {
  showUpload.value = !showUpload.value;
};

const cropSuccess = (imgDataUrl, field) => {
  console.log('裁剪成功:', imgDataUrl, field);
  avatarUrl.value = imgDataUrl;
};

const cropUploadSuccess = (jsonData, field) => {
  console.log('上传成功:', jsonData, field);
  if (jsonData.code === 200) {
    alert('头像上传成功！');
    // 从响应中获取头像文件名
    const avatar = jsonData.data.avatar;
    localStorage.setItem('avatar', avatar);
  } else {
    alert(jsonData.msg || '头像上传失败');
  }
};

const cropUploadFail = (status, field) => {
  console.log('上传失败:', status, field);
  alert('头像上传失败，请重试');
};
</script>

<style scoped>
.avatar-upload-container {
  text-align: center;
  margin-bottom: 40px;
}

.avatar-container {
  position: relative;
  display: inline-block;
  margin-bottom: 20px;
}

.avatar {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid #ff6b6b;
}

.avatar-upload-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  background-color: #ff6b6b;
  color: white;
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.avatar-upload-btn:hover {
  background-color: #ff5252;
}
</style>
