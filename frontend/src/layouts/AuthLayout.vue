<script setup>
import {ref} from 'vue';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faArrowLeft, faPause, faPlay} from '@fortawesome/free-solid-svg-icons';
import BonsaiMark from '../components/home/BonsaiMark.vue';
import AuthIllustration from '../components/auth/AuthIllustration.vue';

defineProps({title: String, description: String, variant: {type: String, default: 'login'}});
const motionPaused = ref(false);
</script>

<template>
  <div class="auth-shell" :class="'auth-shell-' + variant">
    <a class="auth-skip" href="#auth-main">Đi đến biểu mẫu</a>
    <main id="auth-main" class="auth-main" tabindex="-1">
      <div class="auth-visual">
        <div class="auth-visual-brand" aria-hidden="true"><BonsaiMark/><span>BonsaiMarket</span></div>
        <AuthIllustration :paused="motionPaused"/>
        <div class="auth-visual-copy" aria-hidden="true"><p>Một mầm xanh.<br/>Một kết nối mới.</p><span>Dành cho những người yêu cây.</span></div>
        <button class="auth-motion-toggle" type="button" :aria-pressed="motionPaused" @click="motionPaused=!motionPaused">
          <FontAwesomeIcon :icon="motionPaused?faPlay:faPause" aria-hidden="true"/>
          {{ motionPaused?'Tiếp tục minh họa':'Tạm dừng minh họa' }}
        </button>
      </div>
      <section class="auth-content" aria-labelledby="auth-page-title">
        <div class="auth-form-container">
          <header class="auth-form-header">
            <RouterLink to="/" class="auth-brand" aria-label="BonsaiMarket — Trang chủ"><BonsaiMark/><span class="auth-mobile-brand">BonsaiMarket</span></RouterLink>
            <h1 id="auth-page-title">{{ title }}</h1>
            <p class="auth-description">{{ description }}</p>
          </header>
          <slot/>
        </div>
      </section>
    </main>
    <footer class="auth-footer">
      <span>© {{ new Date().getFullYear() }} BonsaiMarket</span>
      <RouterLink to="/products"><FontAwesomeIcon :icon="faArrowLeft" aria-hidden="true"/> Khám phá cây cảnh</RouterLink>
    </footer>
  </div>
</template>
