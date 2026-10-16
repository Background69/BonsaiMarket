import {computed, nextTick, onBeforeUnmount, onMounted, ref} from 'vue';

export function passwordErrors(password, confirmation) {
    const errors = {};
    if (new TextEncoder().encode(password).length > 72) errors.password = 'Mật khẩu quá dài, vui lòng rút ngắn lại.';
    else if (password.length < 12 || !/\p{L}/u.test(password)
        || !/\p{Nd}/u.test(password) || /[\x00-\x1f\x7f]/.test(password))
        errors.password = 'Tối thiểu 12 ký tự, bao gồm cả chữ và số.';
    if (password !== confirmation) errors.confirmPassword = 'Mật khẩu xác nhận chưa khớp.';
    return errors;
}
export function validEmail(email) {
    return email.trim().length <= 255 && /^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?\.[a-z]{2,63}$/i.test(email.trim());
}
export function useAuthFlowUi() {
    const loading = ref(false), error = ref(''), fields = ref({}), message = ref('');
    const time = ref(Date.now()), until = ref(0), submitUntil = ref(0);
    const countdown = computed(() => Math.max(0, Math.ceil((until.value - time.value) / 1000)));
    const submitCountdown = computed(() => Math.max(0, Math.ceil((submitUntil.value - time.value) / 1000)));
    let timer;
    onMounted(() => {timer = setInterval(() => {time.value = Date.now();}, 1000);});
    onBeforeUnmount(() => clearInterval(timer));
    function startCooldown(seconds) {time.value = Date.now(); until.value = time.value + Math.max(0, Number(seconds) || 0) * 1000;}
    async function focus(selector) {await nextTick(); document.querySelector(selector)?.focus();}
    async function focusError() {await focus('.auth-form [aria-invalid="true"]');}
    async function execute(action, onError = () => {}) {
        if (loading.value) return false;
        loading.value = true; error.value = ''; message.value = ''; fields.value = {};
        try {await action(); return true;}
        catch (cause) {
            const text = cause.message || 'Không thể xử lý yêu cầu. Vui lòng thử lại.';
            fields.value = {...cause.fields};
            if (['INVALID_CODE', 'CODE_EXPIRED', 'CODE_ATTEMPTS_EXCEEDED'].includes(cause.code)) fields.value.code = text;
            if (cause.code === 'WEAK_PASSWORD') fields.value.password = text;
            if (cause.code === 'PASSWORD_MISMATCH') fields.value.confirmPassword = text;
            error.value = Object.keys(fields.value).length ? '' : text;
            if (cause.retryAfterSeconds > 0 && ['RESEND_COOLDOWN', 'RATE_LIMITED'].includes(cause.code)) startCooldown(cause.retryAfterSeconds);
            if (cause.retryAfterSeconds > 0 && cause.code === 'RATE_LIMITED') {
                time.value = Date.now(); submitUntil.value = time.value + cause.retryAfterSeconds * 1000;
            }
            onError(cause); return false;
        } finally {loading.value = false; await focusError();}
    }
    return {loading, error, fields, message, countdown, submitCountdown, startCooldown, execute, focus, focusError};
}
