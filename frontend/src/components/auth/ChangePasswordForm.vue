<script setup>
import {ref, nextTick} from 'vue';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faCircleCheck, faTriangleExclamation} from '@fortawesome/free-solid-svg-icons';
import AuthField from './AuthField.vue';
import {authRequest, resetCsrf} from '../../services/api.js';
import {restoreSession} from '../../composables/useAuth.js';
import {passwordErrors} from '../../composables/useAuthFlowUi.js';

const currentPassword = ref(''), newPassword = ref(''), confirmPassword = ref('');
const loading = ref(false), error = ref(''), success = ref(''), fields = ref({});

function focusError() {
  nextTick(() => document.querySelector('.account-password [aria-invalid="true"]')?.focus());
}
function validate() {
  const next = {};
  if (!currentPassword.value) next.currentPassword = 'Vui lòng nhập mật khẩu hiện tại.';
  const checked = passwordErrors(newPassword.value, confirmPassword.value);
  if (checked.password) next.newPassword = checked.password;
  if (checked.confirmPassword) next.confirmPassword = checked.confirmPassword;
  if (!next.currentPassword && !next.newPassword && newPassword.value === currentPassword.value)
    next.newPassword = 'Mật khẩu mới phải khác mật khẩu hiện tại.';
  fields.value = next;
  if (Object.keys(next).length) {focusError(); return false;}
  return true;
}
async function submit() {
  if (loading.value) return;
  error.value = ''; success.value = '';
  if (!validate()) return;
  loading.value = true;
  try {
    await authRequest('/change-password', {
      currentPassword: currentPassword.value, newPassword: newPassword.value, confirmPassword: confirmPassword.value});
    // Server đã cấp lại session/CSRF của thiết bị; bỏ token cache và đọc lại người dùng.
    resetCsrf(); fields.value = {};
    success.value = 'Đổi mật khẩu thành công. Các thiết bị khác đã được đăng xuất.';
    currentPassword.value = ''; newPassword.value = ''; confirmPassword.value = '';
    await restoreSession(true);
  } catch (cause) {
    const next = {...(cause.fields || {})};
    if (cause.code === 'INVALID_CURRENT_PASSWORD') next.currentPassword = cause.message;
    if (['WEAK_PASSWORD', 'PASSWORD_UNCHANGED'].includes(cause.code)) next.newPassword = cause.message;
    if (cause.code === 'PASSWORD_MISMATCH') next.confirmPassword = cause.message;
    fields.value = next;
    error.value = Object.keys(next).length ? '' : (cause.message || 'Không thể đổi mật khẩu. Vui lòng thử lại.');
    currentPassword.value = '';
    focusError();
  } finally {loading.value = false;}
}
</script>
<template>
  <section class="account-password" aria-labelledby="account-password-title">
    <h2 id="account-password-title">Đổi mật khẩu</h2>
    <p class="account-password-intro">Nhập mật khẩu hiện tại và chọn mật khẩu mới. Sau khi đổi, các thiết bị khác sẽ phải đăng nhập lại.</p>
    <p v-if="success" class="account-password-ok" role="status"><FontAwesomeIcon :icon="faCircleCheck"/>{{ success }}</p>
    <p v-if="error" class="account-password-error" role="alert"><FontAwesomeIcon :icon="faTriangleExclamation"/>{{ error }}</p>
    <form class="account-password-form" novalidate :aria-busy="loading" @submit.prevent="submit">
      <AuthField id="current-password" v-model="currentPassword" label="Mật khẩu hiện tại" type="password" autocomplete="current-password" :error="fields.currentPassword" :disabled="loading" required/>
      <AuthField id="new-password" v-model="newPassword" label="Mật khẩu mới" type="password" autocomplete="new-password" placeholder="Nhập mật khẩu mới" help="Tối thiểu 12 ký tự, bao gồm cả chữ và số." :error="fields.newPassword" :disabled="loading" required/>
      <AuthField id="confirm-new-password" v-model="confirmPassword" label="Xác nhận mật khẩu mới" type="password" autocomplete="new-password" placeholder="Nhập lại mật khẩu mới" :error="fields.confirmPassword" :disabled="loading" required/>
      <button class="button button-primary" type="submit" :disabled="loading">{{ loading ? 'Đang cập nhật…' : 'Đổi mật khẩu' }}</button>
    </form>
    <p class="account-password-forgot">Quên mật khẩu hiện tại? <RouterLink to="/forgot-password">Đặt lại qua email</RouterLink></p>
  </section>
</template>
<style scoped>
.account-password{max-width:540px;margin:0 auto 32px;padding:0 24px}.account-password h2{font-size:23px;margin-bottom:8px}
.account-password-intro{color:var(--bm-muted);line-height:1.6;margin:0 0 16px}
.account-password-form{display:flex;flex-direction:column;gap:18px}
.account-password-ok,.account-password-error{padding:12px 14px;border-radius:var(--bm-radius);font-size:14px;line-height:1.6;margin:0 0 16px;overflow-wrap:anywhere}
.account-password-ok{background:var(--bm-soft);color:var(--bm-dark)}.account-password-error{color:#8b2525}
.account-password svg{width:15px;margin-right:8px}
.account-password-forgot{margin:16px 0 0;font-size:14px}.account-password-forgot a{color:var(--bm-forest);text-underline-offset:4px}
</style>
