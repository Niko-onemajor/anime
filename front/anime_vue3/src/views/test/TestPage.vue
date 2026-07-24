<template>
  <div class="test-container">
    <h1>测试页面</h1>
    <p>这是一个测试页面，用于检查前端应用是否正常加载。</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';

onMounted(() => {
  console.log('测试页面加载成功');
});
</script>

<style scoped>
.test-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.test-container h1 {
  color: #333;
  margin-bottom: 20px;
}

.test-container p {
  color: #666;
  font-size: 16px;
}
</style>