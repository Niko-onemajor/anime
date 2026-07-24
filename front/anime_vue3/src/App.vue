<template>
  <div id="app">
    <router-view v-if="isRouterAlive"></router-view>
  </div>
</template>

<script setup lang="ts">
import { ref, provide } from 'vue';

// 控制router-view的显示和隐藏，用于实现页面刷新
const isRouterAlive = ref(true);

// 提供reload方法给子组件
const reload = () => {
  isRouterAlive.value = false;
  setTimeout(() => {
    isRouterAlive.value = true;
  }, 100);
};

provide('reload', reload);
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: "Microsoft YaHei", sans-serif;
  background: #f5f5f5;
}
</style>