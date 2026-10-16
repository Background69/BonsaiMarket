<script setup>
import {computed, ref} from 'vue';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faFacebook} from '@fortawesome/free-brands-svg-icons';
import {useAuth} from '../../composables/useAuth.js';
const {state} = useAuth();
defineProps({disabled: Boolean});
const redirecting = ref('');
const available = computed(() => ['google', 'facebook'].filter(provider => state.providers[provider] === true));
function start(provider) {
  if (!available.value.includes(provider) || redirecting.value) return;
  redirecting.value = provider; window.location.assign(`/oauth2/authorization/${provider}`);
}
</script>
<template>
  <template v-if="available.length">
  <div class="auth-social-group" aria-label="Đăng nhập qua tài khoản khác">
    <button v-for="provider in available" :key="provider" class="auth-social" :data-provider="provider" type="button"
      :disabled="disabled||!!redirecting" :aria-busy="redirecting===provider" @click="start(provider)">
      <svg v-if="provider==='google'" class="auth-google-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path fill="#4285F4" d="M21.6 12.23c0-.71-.06-1.39-.18-2.05H12v3.88h5.38a4.61 4.61 0 0 1-2 3.02v2.51h3.24c1.9-1.75 2.98-4.33 2.98-7.36Z"/>
        <path fill="#34A853" d="M12 22c2.7 0 4.96-.9 6.62-2.41l-3.24-2.51c-.9.6-2.04.96-3.38.96-2.6 0-4.8-1.76-5.59-4.12H3.07v2.59A10 10 0 0 0 12 22Z"/>
        <path fill="#FBBC05" d="M6.41 13.92A6 6 0 0 1 6.1 12c0-.67.11-1.32.31-1.92V7.49H3.07A10 10 0 0 0 2 12c0 1.61.39 3.14 1.07 4.51l3.34-2.59Z"/>
        <path fill="#EA4335" d="M12 5.96c1.47 0 2.79.51 3.82 1.51l2.87-2.87A9.6 9.6 0 0 0 12 2a10 10 0 0 0-8.93 5.49l3.34 2.59C7.2 7.72 9.4 5.96 12 5.96Z"/>
      </svg>
      <FontAwesomeIcon v-else class="auth-facebook-icon" :icon="faFacebook"/>
      <span>{{ redirecting===provider?'Đang chuyển hướng…':provider==='google'?'Google':'Facebook' }}</span>
    </button>
  </div>
  <div class="auth-divider">hoặc</div>
  </template>
</template>
