import {computed, reactive} from 'vue';
import {usePage} from '../legacy/navigation.js';
import {authRequest, resetCsrf} from '../services/api.js';

const state = reactive({ready: false, unavailable: false, providers: {google: false, facebook: false}});
let restoring;
let revision = 0;
let successNotice = '';
function localDestination(value) {
    return typeof value === 'string' && /^\/(?!\/)[^\\\s]*$/.test(value)
        && /^\/(?:community|products|stores|categories|profile|orders|wishlist|cart|checkout|seller|admin|store-application)(?:[/?#]|$)/.test(value);
}
export function rememberDestination(value) {
    if (localDestination(value)) sessionStorage.setItem('bm-auth-destination', value);
}
function setUser(value) {
    usePage().props.auth.user = value ? {...value, role: value.role === 'customer' ? 'buyer' : value.role} : null;
}
export function safeDestination(value, user = usePage().props.auth.user) {
    if (localDestination(value)
        && (!/^\/admin(?:[/?#]|$)/.test(value) || user?.role === 'admin')
        && (!/^\/seller(?:[/?#]|$)/.test(value) || ['seller', 'admin'].includes(user?.role))) return value;
    return user?.role === 'admin' ? '/admin' : user?.role === 'seller' ? '/seller' : '/';
}
export async function restoreSession(force = false) {
    if (restoring) return restoring;
    if (state.ready && !force) return usePage().props.auth.user;
    restoring = (async () => {
        const started = revision;
        try {const user = await authRequest('/me'); if (started === revision) {setUser(user); state.unavailable = false;}}
        catch (error) {if (started === revision) {setUser(null); state.unavailable = error.status !== 401;}}
        finally {state.ready = true; restoring = null;}
        return usePage().props.auth.user;
    })();
    return restoring;
}
export async function loadProviders() {
    state.providers = {google: false, facebook: false};
    try {
        const configured = await authRequest('/providers');
        state.providers = {google: configured?.google === true, facebook: configured?.facebook === true};
    } catch {state.providers = {google: false, facebook: false};}
}
export function useAuth() {
    const user = computed(() => usePage().props.auth.user);
    async function login(credentials) {
        const value = await authRequest('/login', credentials); revision++; setUser(value); resetCsrf(); state.ready = true; state.unavailable = false;
        return user.value;
    }
    async function logout() {
        await authRequest('/logout', {}); revision++; setUser(null); resetCsrf(); state.ready = true;
        window.dispatchEvent(new CustomEvent('bonsai:logged-out'));
    }
    async function verifyEmail(code, contextId) {
        const value = await authRequest('/verify-email', {code, contextId});
        revision++; setUser(value); resetCsrf(); state.ready = true; state.unavailable = false;
        return user.value;
    }
    function passwordResetCompleted() {revision++; setUser(null); resetCsrf(); state.ready = true; successNotice = '';}
    return {state, user, isAuthenticated: computed(() => !!user.value), role: computed(() => user.value?.role),
        isBuyer: computed(() => user.value?.role === 'buyer'), isSeller: computed(() => user.value?.role === 'seller'),
        isAdmin: computed(() => user.value?.role === 'admin'), isEmployee: computed(() => false),
        login, logout, verifyEmail, passwordResetCompleted, restoreSession, loadProviders, syncFromPageProps() {},
        verifiedNotice() {successNotice = 'Email đã được xác thực. Bạn có thể đăng nhập.';},
        resetNotice() {successNotice = 'Mật khẩu đã được cập nhật. Vui lòng đăng nhập lại.';},
        consumeSuccessNotice() {const notice = successNotice; successNotice = ''; return notice;}};
}
window.addEventListener('bonsai:session-expired', () => {revision++; setUser(null); state.ready = true; resetCsrf();});
window.addEventListener('focus', () => {if (state.ready && usePage().props.auth.user) restoreSession(true);});
