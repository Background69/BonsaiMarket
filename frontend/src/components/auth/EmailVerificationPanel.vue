<script setup>
import {onMounted, ref} from 'vue';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faArrowRight} from '@fortawesome/free-solid-svg-icons';
import OtpInput from './OtpInput.vue';
import AuthSuccess from './AuthSuccess.vue';
import {authRequest} from '../../services/api.js';
import {useAuth} from '../../composables/useAuth.js';
import {useAuthFlowUi} from '../../composables/useAuthFlowUi.js';
const phase = ref('CHECKING'), code = ref(''), maskedEmail = ref('');
const contextId = ref(''), retryable = ref(false);
const auth = useAuth();
const {loading, error, fields, message, countdown, startCooldown, execute, focus, focusError} = useAuthFlowUi();
function expired(cause) {if (['VERIFICATION_CONTEXT_EXPIRED', 'SESSION_EXPIRED'].includes(cause.code)) {phase.value = 'BLOCKED'; code.value = ''; contextId.value = ''; retryable.value = false;}}
async function loadContext() {
  phase.value = 'CHECKING';
  const loaded = await execute(async () => {
    const context = await authRequest('/verification-context');
    maskedEmail.value = context.maskedEmail; contextId.value = context.contextId; startCooldown(context.resendAfterSeconds); phase.value = 'READY';
  }, cause => {retryable.value = !['VERIFICATION_CONTEXT_EXPIRED', 'SESSION_EXPIRED'].includes(cause.code);});
  if (loaded) await focus('.auth-otp input'); else phase.value = 'BLOCKED';
}
async function submit() {
  if (loading.value || phase.value !== 'READY') return;
  if (!/^[0-9]{6}$/.test(code.value)) {fields.value = {code: 'Vui lòng nhập đủ 6 chữ số.'}; await focusError(); return;}
  const verified = await execute(async () => {await auth.verifyEmail(code.value, contextId.value); code.value = ''; contextId.value = ''; phase.value = 'SUCCESS';}, expired);
  if (verified) await focus('#auth-panel-title');
}
async function resend() {
  if (loading.value || countdown.value || phase.value !== 'READY') return;
  await execute(async () => {
    const result = await authRequest('/resend-verification', {contextId: contextId.value});
    code.value = ''; startCooldown(result.resendAfterSeconds); message.value = 'Mã mới đã được gửi. Vui lòng kiểm tra email của bạn.';
  }, expired);
  if (phase.value === 'READY') await focus('.auth-otp input');
}
onMounted(loadContext);
</script>
<template>
  <div class="auth-flow" :data-verification-state="phase">
    <AuthSuccess v-if="phase==='SUCCESS'" title="Xác thực tài khoản thành công!" description="Chào mừng bạn đến với BonsaiMarket."
      action="Về trang chủ" destination="/" :delay="3"/>
    <template v-else>
      <h2 id="auth-panel-title">Xác thực tài khoản</h2>
      <p v-if="phase==='CHECKING'" class="auth-intro" role="status">Đang kiểm tra phiên xác thực của bạn…</p>
      <p v-else-if="phase==='READY'" class="auth-intro">Nhập mã xác thực 6 chữ số được gửi đến email bạn đã đăng ký. <strong class="auth-masked-email">{{ maskedEmail }}</strong></p>
      <p v-if="error" class="auth-status auth-status-error" role="alert">{{ error }}</p>
      <p v-if="message&&phase==='READY'" class="auth-status" role="status">{{ message }}</p>
      <form v-if="phase==='READY'" class="auth-form" novalidate :aria-busy="loading" @submit.prevent="submit">
        <OtpInput v-model="code" :error="fields.code" :disabled="loading"/>
        <button class="auth-primary" type="submit" :disabled="loading">{{ loading?'Đang xử lý…':'Xác thực tài khoản' }} <FontAwesomeIcon v-if="!loading" :icon="faArrowRight"/></button>
        <button class="auth-secondary" type="button" :disabled="loading||countdown>0" @click="resend">{{ countdown>0?'Gửi lại mã sau '+countdown+' giây':'Gửi lại mã' }}</button>
      </form>
      <div v-else-if="phase==='BLOCKED'" class="auth-blocked-actions">
        <p class="auth-help">Nếu đã tạo tài khoản, hãy đăng nhập bằng mật khẩu đã đăng ký để mở lại phiên xác thực.</p>
        <RouterLink to="/register" class="auth-primary">Về trang đăng ký <FontAwesomeIcon :icon="faArrowRight"/></RouterLink>
        <RouterLink to="/login">Đến trang đăng nhập</RouterLink>
        <button v-if="retryable" class="auth-secondary" type="button" :disabled="loading" @click="loadContext">Kiểm tra lại phiên</button>
      </div>
    </template>
  </div>
</template>
