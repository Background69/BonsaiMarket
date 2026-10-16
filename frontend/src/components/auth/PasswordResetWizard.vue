<script setup>
import {computed, ref, watch} from 'vue';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faArrowLeft} from '@fortawesome/free-solid-svg-icons';
import AuthField from './AuthField.vue';
import OtpInput from './OtpInput.vue';
import AuthSuccess from './AuthSuccess.vue';
import MaskedEmail from './MaskedEmail.vue';
import {authRequest} from '../../services/api.js';
import {useAuth} from '../../composables/useAuth.js';
import {passwordErrors, validEmail, useAuthFlowUi} from '../../composables/useAuthFlowUi.js';

const STEP = Object.freeze({EMAIL: 'EMAIL', OTP: 'VERIFY_OTP', PASSWORD: 'NEW_PASSWORD', SUCCESS: 'SUCCESS'});
const step = ref(STEP.EMAIL);
const emit = defineEmits(['stage']);
watch(step, value => emit('stage', value), {immediate: true});
// Correlation only: a stale tab cannot apply another tab's server authorization.
const flowId = crypto.randomUUID();
const email = ref(sessionStorage.getItem('bm-auth-email') || ''), code = ref(''), password = ref(''), confirmation = ref('');
const {loading, error, fields, message, countdown, submitCountdown, startCooldown, execute, focus, focusError} = useAuthFlowUi();
const auth = useAuth();
const copy = {
  EMAIL: {title: 'Quên mật khẩu?', description: 'Nhập email đăng ký. Nếu tài khoản đủ điều kiện, bạn sẽ nhận được mã xác minh.', action: 'Tiếp tục', step: 'Bước 1/3 · Xác nhận email'},
  VERIFY_OTP: {title: 'Nhập mã xác minh', description: 'Nếu tài khoản đủ điều kiện, mã xác minh sẽ được gửi đến:', action: 'Xác minh mã', step: 'Bước 2/3 · Nhập mã OTP'},
  NEW_PASSWORD: {title: 'Tạo mật khẩu mới', description: 'Chọn mật khẩu mới để hoàn tất khôi phục tài khoản.', action: 'Cập nhật mật khẩu', step: 'Bước 3/3 · Mật khẩu mới'},
};
const current = computed(() => copy[step.value]);
const maskedEmail = computed(() => {const [local, domain] = email.value.split('@'); return domain ? local.slice(0, 2) + '•••@' + domain : '';});
function recover(cause) {
  if (['RESET_FLOW_EXPIRED', 'RESET_AUTHORIZATION_EXPIRED', 'SESSION_EXPIRED'].includes(cause.code)) {
    step.value = STEP.EMAIL; code.value = ''; password.value = ''; confirmation.value = '';
  }
}
function validate() {
  error.value = ''; message.value = '';
  const next = {};
  if (step.value === STEP.EMAIL && !validEmail(email.value)) next.email = 'Vui lòng nhập email hợp lệ, không dấu.';
  if (step.value === STEP.OTP && !/^[0-9]{6}$/.test(code.value)) next.code = 'Vui lòng nhập đủ 6 chữ số.';
  if (step.value === STEP.PASSWORD) Object.assign(next, passwordErrors(password.value, confirmation.value));
  fields.value = next;
  if (Object.keys(next).length) {focusError(); return false;}
  return true;
}
async function submit() {
  if (loading.value || submitCountdown.value || !validate()) return;
  const completed = await execute(async () => {
    switch (step.value) {
      case STEP.EMAIL: {
        const canonical = email.value.trim().toLowerCase();
        const result = await authRequest('/forgot-password', {email: canonical, flowId});
        email.value = canonical; sessionStorage.setItem('bm-auth-email', canonical);
        startCooldown(result.resendAfterSeconds); code.value = ''; step.value = STEP.OTP; break;
      }
      case STEP.OTP:
        await authRequest('/verify-reset-code', {code: code.value, flowId});
        code.value = ''; step.value = STEP.PASSWORD; break;
      case STEP.PASSWORD:
        await authRequest('/reset-password', {password: password.value, confirmPassword: confirmation.value, flowId});
        password.value = ''; confirmation.value = ''; auth.passwordResetCompleted(); step.value = STEP.SUCCESS; break;
      default: break;
    }
  }, recover);
  if (completed) await focus(step.value === STEP.OTP ? '.auth-otp input' : step.value === STEP.PASSWORD ? '#password' : '#auth-panel-title');
  else if (step.value === STEP.EMAIL) await focus('#email');
}
async function resend() {
  if (loading.value || countdown.value || step.value !== STEP.OTP) return;
  await execute(async () => {
    const result = await authRequest('/resend-reset-code', {flowId});
    code.value = ''; startCooldown(result.resendAfterSeconds);
    message.value = 'Yêu cầu gửi lại đã được tiếp nhận. Nếu email đủ điều kiện, bạn sẽ nhận được mã mới.';
  }, recover);
  if (step.value === STEP.OTP) await focus('.auth-otp input');
}
async function backToEmail() {
  if (loading.value) return;
  step.value = STEP.EMAIL; code.value = ''; error.value = ''; message.value = ''; fields.value = {};
  await focus('#email');
}
</script>
<template>
  <div class="auth-flow" :data-auth-step="step">
    <AuthSuccess v-if="step===STEP.SUCCESS" title="Đặt lại mật khẩu thành công!"
      description="Nếu muốn tiếp tục sử dụng BonsaiMarket, hãy đăng nhập bằng mật khẩu mới." action="Đến trang đăng nhập" destination="/login"/>
    <template v-else>
      <h2 id="auth-panel-title">{{ current.title }}</h2>
      <p class="auth-step" role="status" aria-live="polite" aria-atomic="true">{{ current.step }}</p>
      <p class="auth-intro">{{ current.description }}</p>
      <MaskedEmail v-if="step===STEP.OTP" :email="maskedEmail"/>
      <p v-if="error" class="auth-status auth-status-error" role="alert">{{ error }}</p>
      <p v-if="message" class="auth-status" role="status">{{ message }}</p>
      <form class="auth-form" novalidate :aria-busy="loading" @submit.prevent="submit">
        <AuthField v-if="step===STEP.EMAIL" id="email" v-model="email" label="Email" type="email" autocomplete="email" :error="fields.email" :disabled="loading" required/>
        <OtpInput v-else-if="step===STEP.OTP" v-model="code" :error="fields.code" :disabled="loading"/>
        <template v-else-if="step===STEP.PASSWORD">
          <AuthField id="password" v-model="password" label="Mật khẩu mới" placeholder="Nhập mật khẩu mới của bạn" type="password" autocomplete="new-password"
            help="Tối thiểu 12 ký tự, bao gồm cả chữ và số." :error="fields.password" :disabled="loading" required/>
          <AuthField id="confirmPassword" v-model="confirmation" label="Xác nhận mật khẩu" placeholder="Nhập lại mật khẩu của bạn" type="password" autocomplete="new-password" :error="fields.confirmPassword" :disabled="loading" required/>
        </template>
        <button class="auth-primary" type="submit" :disabled="loading||submitCountdown>0">{{ loading?'Đang xử lý…':submitCountdown>0?'Thử lại sau '+submitCountdown+' giây':current.action }}</button>
        <button v-if="step===STEP.OTP" class="auth-secondary" type="button" :disabled="loading||countdown>0" @click="resend">{{ countdown>0?'Gửi lại mã sau '+countdown+' giây':'Gửi lại mã' }}</button>
      </form>
      <p v-if="step===STEP.OTP" class="auth-bottom"><button class="auth-secondary" type="button" :disabled="loading" @click="backToEmail"><FontAwesomeIcon :icon="faArrowLeft"/> Nhập lại email</button></p>
      <p v-else class="auth-bottom"><RouterLink to="/login"><FontAwesomeIcon :icon="faArrowLeft"/> Về trang đăng nhập</RouterLink></p>
    </template>
  </div>
</template>
