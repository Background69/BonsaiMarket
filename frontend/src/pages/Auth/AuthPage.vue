<script setup>
import {computed, ref, watch, onMounted, onBeforeUnmount} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faArrowRight, faArrowLeft, faCircleCheck, faTriangleExclamation} from '@fortawesome/free-solid-svg-icons';
import AuthLayout from '../../layouts/AuthLayout.vue';
import AuthField from '../../components/auth/AuthField.vue';
import OtpInput from '../../components/auth/OtpInput.vue';
import SocialLogin from '../../components/auth/SocialLogin.vue';
import {useAuth, safeDestination, rememberDestination} from '../../composables/useAuth.js';
import {authRequest} from '../../services/api.js';

const route = useRoute(), router = useRouter(), auth = useAuth();
const mode = computed(() => route.name);
const email = ref(sessionStorage.getItem('bm-auth-email') || ''), name = ref(''), password = ref(''), confirmPassword = ref(''), code = ref('');
const loading = ref(false), error = ref(''), errorCode = ref(''), fields = ref({}), message = ref(''), time = ref(Date.now());
const titles = {login: ['Chào mừng trở lại!', 'Đăng nhập để tiếp tục khám phá BonsaiMarket'],
  register: ['Bắt đầu cùng BonsaiMarket', 'Tạo tài khoản để kết nối với những gian hàng cây cảnh độc lập.'],
  'verify-email': ['Xác thực email của bạn', 'Một bước nhỏ để bảo vệ tài khoản của bạn.'],
  'forgot-password': ['Quên mật khẩu?', 'Chúng tôi sẽ giúp bạn lấy lại quyền truy cập tài khoản.'],
  'reset-password': ['Tạo mật khẩu mới', 'Sử dụng mã xác minh trong email để đặt lại mật khẩu.'],
  'oauth-complete': ['Hoàn tất tài khoản', 'Xác thực email để tiếp tục cùng BonsaiMarket.'],
  'auth-callback': ['Đang đăng nhập', 'Chúng tôi đang kiểm tra phiên đăng nhập của bạn.']};
const title = computed(() => titles[mode.value] || titles.login);
const headings = {login: 'Đăng nhập', register: 'Tạo tài khoản', 'verify-email': 'Nhập mã xác thực',
  'forgot-password': 'Lấy lại mật khẩu', 'reset-password': 'Đặt lại mật khẩu', 'oauth-complete': 'Bổ sung email'};
const ctas = {login: 'Đăng nhập', register: 'Tạo tài khoản', 'verify-email': 'Xác thực email',
  'forgot-password': 'Gửi mã đặt lại mật khẩu', 'reset-password': 'Cập nhật mật khẩu', 'oauth-complete': 'Gửi mã xác thực'};
const cooldownKey = computed(() => mode.value === 'reset-password' || mode.value === 'forgot-password' ? 'bm-auth-reset-until' : 'bm-auth-verify-until');
const until = ref(Number(sessionStorage.getItem(cooldownKey.value) || 0));
const countdown = computed(() => Math.max(0, Math.ceil((until.value - time.value) / 1000)));
const maskedEmail = computed(() => {const [local, domain] = email.value.split('@'); return domain ? local.slice(0, 2) + '•••@' + domain : '';});
const verificationSent = computed(() => sessionStorage.getItem('bm-auth-sent-email') === email.value && Number(sessionStorage.getItem('bm-auth-verify-until') || 0) > 0);
let timer;
function startCooldown(seconds) {until.value = Date.now() + seconds * 1000; sessionStorage.setItem(cooldownKey.value, String(until.value)); time.value = Date.now();}
function rememberEmail() {email.value = email.value.trim().toLowerCase(); sessionStorage.setItem('bm-auth-email', email.value);}
function showError(cause) {
  error.value = cause.message; errorCode.value = cause.code; fields.value = cause.fields || {};
  if (['INVALID_CODE', 'CODE_EXPIRED', 'CODE_ATTEMPTS_EXCEEDED'].includes(cause.code)) fields.value.code = cause.message;
  if (cause.code === 'WEAK_PASSWORD') fields.value.password = cause.message;
  if (cause.code === 'PASSWORD_MISMATCH') fields.value.confirmPassword = cause.message;
  if (cause.retryAfterSeconds > 0 && ['RESEND_COOLDOWN', 'RATE_LIMITED'].includes(cause.code)) startCooldown(cause.retryAfterSeconds);
}
function focusFirstError() {
  requestAnimationFrame(() => document.querySelector('.auth-form [aria-invalid="true"]')?.focus());
}
function validate() {
  const next = {};
  if (!/^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?\.[a-z]{2,63}$/i.test(email.value.trim()) || email.value.trim().length > 255) next.email = 'Vui lòng nhập email hợp lệ, không dấu.';
  if (['register', 'oauth-complete'].includes(mode.value) && (!name.value.trim() || name.value.trim().length > 150)) next.name = 'Vui lòng nhập họ và tên.';
  if (['login', 'register', 'reset-password'].includes(mode.value) && !password.value.trim()) next.password = 'Vui lòng nhập mật khẩu.';
  if (['register', 'reset-password'].includes(mode.value)) {
    if (password.value.length < 12 || new TextEncoder().encode(password.value).length > 72 || !/\p{L}/u.test(password.value) || !/\p{Nd}/u.test(password.value) || /[\x00-\x1f\x7f]/.test(password.value)) next.password = 'Mật khẩu từ 12 ký tự, có chữ và số, tối đa 72 byte UTF-8.';
    if (password.value !== confirmPassword.value) next.confirmPassword = 'Mật khẩu xác nhận chưa khớp.';
  }
  if (['verify-email', 'reset-password'].includes(mode.value) && !/^[0-9]{6}$/.test(code.value)) next.code = 'Vui lòng nhập đủ 6 chữ số.';
  fields.value = next;
  if (Object.keys(next).length) {focusFirstError(); return false;}
  return true;
}
async function submit() {
  if (loading.value) return;
  error.value = ''; errorCode.value = ''; message.value = '';
  if (!validate()) return;
  rememberEmail(); loading.value = true;
  try {
    if (mode.value === 'login') {
      const user = await auth.login({email: email.value, password: password.value});
      await router.replace(safeDestination(route.query.redirect || sessionStorage.getItem('bm-auth-destination'), user));
      sessionStorage.removeItem('bm-auth-destination');
    } else if (mode.value === 'register') {
      const result = await authRequest('/register', {name: name.value.trim(), email: email.value, password: password.value, confirmPassword: confirmPassword.value});
      startCooldown(result.resendAfterSeconds); sessionStorage.setItem('bm-auth-sent-email', email.value); await router.push('/verify-email');
    } else if (mode.value === 'verify-email') {
      await authRequest('/verify-email', {email: email.value, code: code.value});
      sessionStorage.removeItem('bm-auth-verify-until'); auth.verifiedNotice(); await router.replace('/login');
    } else if (mode.value === 'forgot-password') {
      const result = await authRequest('/forgot-password', {email: email.value}); startCooldown(result.resendAfterSeconds); await router.push('/reset-password');
    } else if (mode.value === 'reset-password') {
      await authRequest('/reset-password', {email: email.value, code: code.value, password: password.value, confirmPassword: confirmPassword.value});
      sessionStorage.removeItem('bm-auth-reset-until'); auth.resetNotice(); await router.replace('/login');
    } else if (mode.value === 'oauth-complete') {
      const result = await authRequest('/oauth/complete', {name: name.value.trim(), email: email.value}); startCooldown(result.resendAfterSeconds); sessionStorage.setItem('bm-auth-sent-email', email.value); await router.push('/verify-email');
    }
  } catch (cause) {showError(cause); focusFirstError();}
  finally {loading.value = false; password.value = ''; confirmPassword.value = '';}
}
async function resend() {
  if (loading.value || countdown.value) return;
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {fields.value.email = 'Vui lòng nhập email hợp lệ.'; focusFirstError(); return;}
  rememberEmail(); loading.value = true; error.value = ''; errorCode.value = ''; message.value = '';
  try {
    const result = await authRequest(mode.value === 'reset-password' ? '/forgot-password' : '/resend-verification', {email: email.value});
    code.value = ''; message.value = result.message; startCooldown(result.resendAfterSeconds);
    if (mode.value === 'verify-email') sessionStorage.setItem('bm-auth-sent-email', email.value);
  } catch (cause) {showError(cause);} finally {loading.value = false;}
}
const oauthErrors = {OAUTH_EMAIL_COLLISION: 'Email đã có tài khoản. Hãy đăng nhập bằng phương thức hiện có, rồi liên kết Google/Facebook trong trang tài khoản.',
  EMAIL_NOT_VERIFIED: 'Tài khoản chưa xác thực email. Hãy nhập email ở trang xác thực để nhận mã mới.',
  ACCOUNT_LOCKED: 'Tài khoản đang bị khóa. Vui lòng liên hệ người quản trị.',
  OAUTH_ALREADY_LINKED: 'Tài khoản mạng xã hội đã được liên kết với tài khoản khác.',
  OAUTH_LINK_EXPIRED: 'Yêu cầu liên kết đã hết hạn. Hãy xác nhận mật khẩu lại.',
  OAUTH_FAILED: 'Không thể đăng nhập qua nhà cung cấp. Vui lòng thử lại từ nút Google hoặc Facebook.'};
async function initialize() {
  until.value = Number(sessionStorage.getItem(cooldownKey.value) || 0);
  if (mode.value === 'login') {
    message.value = auth.consumeSuccessNotice();
    if (route.query.notice === 'session-expired') error.value = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
    if (route.query.authError) {error.value = oauthErrors[route.query.authError] || oauthErrors.OAUTH_FAILED; errorCode.value = route.query.authError;}
    if (route.query.redirect) rememberDestination(route.query.redirect);
  }
  if (mode.value === 'oauth-complete') {
    loading.value = true;
    try {const pending = await authRequest('/oauth/pending'); name.value = pending.name;} catch (cause) {showError(cause);} finally {loading.value = false;}
  }
  if (mode.value === 'auth-callback') {
    loading.value = true;
    const user = await auth.restoreSession(true);
    if (user) {await router.replace(safeDestination(sessionStorage.getItem('bm-auth-destination'), user)); sessionStorage.removeItem('bm-auth-destination');}
    else {error.value = 'Không thể khôi phục phiên đăng nhập. Vui lòng thử lại.'; loading.value = false;}
  }
}
watch(mode, () => {password.value = ''; confirmPassword.value = ''; code.value = ''; error.value = ''; fields.value = {}; message.value = ''; initialize();});
onMounted(() => {timer = setInterval(() => {time.value = Date.now();}, 1000); initialize();});
onBeforeUnmount(() => clearInterval(timer));
</script>

<template>
  <AuthLayout :title="title[0]" :description="title[1]">
    <h2 v-if="headings[mode]">{{ headings[mode] }}</h2>
    <p v-if="mode==='login'" class="auth-intro">Chưa có tài khoản? <RouterLink to="/register">Đăng ký ngay</RouterLink></p>
    <p v-else-if="mode==='register'" class="auth-intro">Đã có tài khoản? <RouterLink to="/login">Đăng nhập</RouterLink></p>
    <p v-else-if="mode==='verify-email'" class="auth-intro">{{ verificationSent?'Chúng tôi đã gửi mã xác thực gồm 6 chữ số đến địa chỉ email của bạn.':'Nhập mã xác thực đã nhận hoặc chọn Gửi lại mã để nhận mã mới.' }} <strong v-if="maskedEmail">{{ maskedEmail }}</strong></p>
    <p v-else-if="mode==='forgot-password'" class="auth-intro">Nhập email của bạn để yêu cầu mã đặt lại mật khẩu.</p>
    <p v-else-if="mode==='reset-password'" class="auth-intro">Nếu tài khoản đủ điều kiện, bạn sẽ nhận được mã qua email. Nhập mã và mật khẩu mới bên dưới.</p>
    <p v-else-if="mode==='oauth-complete'" class="auth-intro">Chúng tôi cần xác thực email trước khi kích hoạt tài khoản của bạn.</p>
    <p v-if="error" class="auth-status auth-status-error" role="alert"><FontAwesomeIcon :icon="faTriangleExclamation"/>{{ error }}</p>
    <p v-if="message" class="auth-status" role="status"><FontAwesomeIcon :icon="faCircleCheck"/>{{ message }}</p>
    <template v-if="mode==='login'||mode==='register'"><SocialLogin :disabled="loading"/><div class="auth-divider">hoặc</div></template>
    <form v-if="mode!=='auth-callback'" class="auth-form" novalidate :aria-busy="loading" @submit.prevent="submit">
      <AuthField v-if="mode==='register'||mode==='oauth-complete'" id="name" v-model="name" label="Họ và tên" autocomplete="name" :error="fields.name" :disabled="loading" required/>
      <AuthField id="email" v-model="email" label="Email" type="email" autocomplete="email" :error="fields.email" :disabled="loading" required/>
      <OtpInput v-if="mode==='verify-email'||mode==='reset-password'" v-model="code" :error="fields.code" :disabled="loading"/>
      <AuthField v-if="mode==='login'||mode==='register'||mode==='reset-password'" id="password" v-model="password" :label="mode==='reset-password'?'Mật khẩu mới':'Mật khẩu'" type="password"
                 :autocomplete="mode==='login'?'current-password':'new-password'" :help="mode==='login'?undefined:'Từ 12 ký tự, có chữ và số. Tối đa 72 byte UTF-8.'" :error="fields.password" :disabled="loading" required/>
      <AuthField v-if="mode==='register'||mode==='reset-password'" id="confirmPassword" v-model="confirmPassword" label="Xác nhận mật khẩu" type="password" autocomplete="new-password" :error="fields.confirmPassword" :disabled="loading" required/>
      <div v-if="mode==='login'" class="auth-row"><span></span><RouterLink to="/forgot-password">Quên mật khẩu?</RouterLink></div>
      <button class="auth-primary" type="submit" :disabled="loading"><span>{{ loading?'Đang xử lý…':ctas[mode] }}</span><FontAwesomeIcon v-if="!loading" :icon="faArrowRight"/></button>
      <button v-if="mode==='verify-email'||mode==='reset-password'" class="auth-secondary" type="button" :disabled="loading||countdown>0" @click="resend">{{ countdown>0?'Gửi lại mã sau '+countdown+' giây':'Gửi lại mã' }}</button>
      <RouterLink v-if="errorCode==='EMAIL_NOT_VERIFIED'||errorCode==='MAIL_DELIVERY_FAILED'||errorCode==='EMAIL_EXISTS'" to="/verify-email">Đến trang xác thực để gửi lại mã</RouterLink>
    </form>
    <p v-if="mode==='login'" class="auth-bottom">Chưa có tài khoản? <RouterLink to="/register">Đăng ký ngay</RouterLink></p>
    <p v-else-if="mode==='register'" class="auth-bottom">Đã có tài khoản? <RouterLink to="/login">Đăng nhập</RouterLink></p>
    <p v-else class="auth-bottom"><RouterLink to="/login"><FontAwesomeIcon :icon="faArrowLeft"/> Về trang đăng nhập</RouterLink></p>
  </AuthLayout>
</template>
