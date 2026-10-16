<script setup>
import {computed, ref, watch} from 'vue';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faArrowRight} from '@fortawesome/free-solid-svg-icons';
import BonsaiMark from '../components/home/BonsaiMark.vue';
import {authVisuals} from '../config/authVisuals.js';

const props = defineProps({title: String, description: String, variant: {type: String, default: 'login'}});
const background = ref(''), foreground = ref('');
const backgroundPath = computed(() => props.variant === 'register' ? authVisuals.registerBackground : authVisuals.loginBackground);
const marketplaceEntry = computed(() => ['login', 'register'].includes(props.variant));

// Decode optional assets before mounting them. Missing slots keep the clean fallback.
function checkImage(path, destination, onCleanup) {
  destination.value = '';
  if (!path) return;
  const probe = new Image();
  probe.onload = () => {if (probe.naturalWidth > 0) destination.value = path;};
  probe.onerror = () => {destination.value = '';};
  onCleanup(() => {probe.onload = null; probe.onerror = null;});
  probe.src = path;
}
watch(backgroundPath, (path, _, onCleanup) => checkImage(path, background, onCleanup), {immediate: true});
watch(() => authVisuals.bonsaiForeground, (path, _, onCleanup) => checkImage(path, foreground, onCleanup), {immediate: true});
</script>

<template>
  <div class="auth-shell" :class="'auth-shell-' + variant">
    <div v-if="background" class="auth-background" :style="{backgroundImage: `url(${JSON.stringify(background)})`}" aria-hidden="true"></div>
    <img v-if="foreground" class="auth-bonsai" :src="foreground" alt="" aria-hidden="true" @error="foreground=''"/>
    <a class="auth-skip" href="#auth-main">Đi đến biểu mẫu</a>
    <header class="auth-header">
      <RouterLink to="/" class="auth-brand" aria-label="BonsaiMarket — Trang chủ"><BonsaiMark/><span>BonsaiMarket</span></RouterLink>
      <RouterLink to="/products" class="auth-back">Khám phá cây cảnh <FontAwesomeIcon :icon="faArrowRight"/></RouterLink>
    </header>
    <main id="auth-main" class="auth-main" tabindex="-1">
      <div class="auth-welcome">
        <h1>{{ title }}</h1>
        <p class="auth-welcome-description">{{ description }}</p>
        <p v-if="marketplaceEntry" class="auth-market-note">Mỗi gian hàng, một người trồng cây. Khám phá cây cảnh và tìm hiểu các gian hàng độc lập.</p>
      </div>
      <section class="auth-content" aria-labelledby="auth-panel-title"><slot/></section>
    </main>
    <footer class="auth-footer">
      <span>© {{ new Date().getFullYear() }} BonsaiMarket</span>
      <RouterLink to="/">Về trang chủ <FontAwesomeIcon :icon="faArrowRight"/></RouterLink>
    </footer>
  </div>
</template>
