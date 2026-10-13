<script setup>
import {ref} from 'vue';
import {useAuth} from '../../composables/useAuth.js';
import {authRequest} from '../../services/api.js';
import AuthField from './AuthField.vue';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faGoogle, faFacebookF} from '@fortawesome/free-brands-svg-icons';
const {state} = useAuth();
const password = ref(''), loading = ref(false), error = ref('');
async function link(provider) {
  if (loading.value) return;
  loading.value = true; error.value = '';
  try {const result = await authRequest('/oauth/link', {provider, password: password.value}); window.location.assign(result.authorizationUrl);}
  catch (cause) {error.value = cause.message; loading.value = false;}
  finally {password.value = '';}
}
</script>
<template>
  <section class="account-auth-links" aria-labelledby="account-link-title">
    <h2 id="account-link-title">Liên kết tài khoản</h2>
    <p class="auth-intro">Xác nhận mật khẩu BonsaiMarket, sau đó đăng nhập tại nhà cung cấp để liên kết. Nếu chỉ đăng nhập bằng mạng xã hội, hãy đặt mật khẩu qua <RouterLink to="/forgot-password">Quên mật khẩu</RouterLink> trước.</p>
    <p v-if="error" class="auth-status auth-status-error" role="alert">{{ error }}</p>
    <AuthField id="link-password" v-model="password" label="Mật khẩu BonsaiMarket" type="password" autocomplete="current-password" :disabled="loading"/>
    <div class="account-link-buttons"><button class="auth-social" :disabled="loading||!password||!state.providers.google" type="button" @click="link('google')"><FontAwesomeIcon :icon="faGoogle"/>Liên kết Google</button>
    <button class="auth-social" :disabled="loading||!password||!state.providers.facebook" type="button" @click="link('facebook')"><FontAwesomeIcon :icon="faFacebookF"/>Liên kết Facebook</button></div>
    <p v-if="!state.providers.google||!state.providers.facebook" class="auth-help">Nhà cung cấp chưa được cấu hình sẽ chưa thể liên kết.</p>
  </section>
</template>
<style scoped>
.account-auth-links{max-width:540px;margin:32px auto;padding:0 24px 32px}.account-auth-links h2{font-size:23px;margin-bottom:12px}.account-auth-links a{color:var(--bm-forest);text-underline-offset:4px}.account-link-buttons{display:flex;gap:12px;margin:16px 0;flex-wrap:wrap}.account-link-buttons button{flex:1;min-width:180px}
</style>
