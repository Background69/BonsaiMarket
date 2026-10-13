<script setup>
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faGoogle, faFacebookF} from '@fortawesome/free-brands-svg-icons';
import {useAuth} from '../../composables/useAuth.js';
const {state} = useAuth();
defineProps({disabled: Boolean});
function start(provider) {window.location.assign(`/oauth2/authorization/${provider}`);}
</script>
<template>
  <div class="auth-social-group">
    <button class="auth-social" type="button" :disabled="disabled||!state.providers.google" @click="start('google')"><FontAwesomeIcon :icon="faGoogle"/><span>Tiếp tục với Google</span></button>
    <button class="auth-social" type="button" :disabled="disabled||!state.providers.facebook" @click="start('facebook')"><FontAwesomeIcon class="auth-facebook-icon" :icon="faFacebookF"/><span>Tiếp tục với Facebook</span></button>
    <p v-if="!state.providers.google||!state.providers.facebook" class="auth-help">{{ !state.providers.google&&!state.providers.facebook?'Google và Facebook':!state.providers.google?'Google':'Facebook' }} chưa được cấu hình.</p>
  </div>
</template>
