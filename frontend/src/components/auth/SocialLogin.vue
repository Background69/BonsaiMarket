<script setup>
import {ref} from 'vue';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faGoogle, faFacebook} from '@fortawesome/free-brands-svg-icons';
import {useAuth} from '../../composables/useAuth.js';
const {state} = useAuth();
defineProps({disabled: Boolean});
const redirecting = ref('');
function start(provider) {redirecting.value = provider; window.location.assign(`/oauth2/authorization/${provider}`);}
</script>
<template>
  <div class="auth-social-group">
    <button class="auth-social" type="button" :disabled="disabled||!!redirecting||!state.providers.google" :aria-busy="redirecting==='google'" @click="start('google')">
      <FontAwesomeIcon class="auth-google-icon" :icon="faGoogle"/>
      <span class="auth-social-label"><span>{{ redirecting==='google'?'Đang chuyển hướng…':'Tiếp tục với Google' }}</span><small v-if="!state.providers.google">Chưa được cấu hình</small></span>
    </button>
    <button class="auth-social" type="button" :disabled="disabled||!!redirecting||!state.providers.facebook" :aria-busy="redirecting==='facebook'" @click="start('facebook')">
      <FontAwesomeIcon class="auth-facebook-icon" :icon="faFacebook"/>
      <span class="auth-social-label"><span>{{ redirecting==='facebook'?'Đang chuyển hướng…':'Tiếp tục với Facebook' }}</span><small v-if="!state.providers.facebook">Chưa được cấu hình</small></span>
    </button>
  </div>
</template>
