<script setup>
import BonsaiMark from '../components/home/BonsaiMark.vue';
defineProps({title: String, description: String});
</script>
<template>
  <div class="auth-shell">
    <header class="auth-header">
      <RouterLink to="/" class="auth-brand" aria-label="BonsaiMarket — Trang chủ"><BonsaiMark/><span>BonsaiMarket</span></RouterLink>
      <RouterLink to="/products" class="auth-back">Khám phá cây cảnh</RouterLink>
    </header>
    <main id="auth-main" class="auth-main">
      <div class="auth-welcome"><h1>{{ title }}</h1><p>{{ description }}</p></div>
      <section class="auth-content" aria-label="Xác thực tài khoản"><slot/></section>
    </main>
    <footer class="auth-footer"><span>© {{ new Date().getFullYear() }} BonsaiMarket</span><RouterLink to="/">Về trang chủ</RouterLink></footer>
  </div>
</template>
