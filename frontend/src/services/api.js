import {uiErrorMessage,fieldErrorMessages,responseData} from './presentation.js';
async function fetchApi(url, options) {
    try {return await fetch(url, options);}
    catch (cause) {throw new Error('Không thể kết nối. Vui lòng thử lại.', {cause});}
}
// Chỉ dùng fetch của trình duyệt; giữ dạng phản hồi tương thích api.get(...).data cũ.
export async function apiGet(path) {
    const response = await fetchApi(`/api${path}`, {credentials: 'include', headers: {Accept: 'application/json'}});
    const data = await responseData(response);
    if (!response.ok) {
        if (response.status === 401) window.dispatchEvent(new CustomEvent('bonsai:session-expired'));
        const body = data ?? {};
        const error = new Error(uiErrorMessage(response.status, body));
        error.status = response.status;
        error.code = body.code;
        throw error;
    }
    return data;
}

function readCookie(name) {
    const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
    return match ? decodeURIComponent(match[1]) : null;
}

// Backend đặt cookie XSRF-TOKEN trong phản hồi; gọi một lần nếu cookie chưa có.
async function ensureCsrfToken() {
    if (!readCookie('XSRF-TOKEN')) await fetchApi('/api/auth/me', {credentials: 'include', headers: {Accept: 'application/json'}});
}

export async function apiPost(path, body = {}) {
    await ensureCsrfToken();
    const response = await fetchApi(`/api${path}`, {
        method: 'POST',
        credentials: 'include',
        headers: {Accept: 'application/json', 'Content-Type': 'application/json', 'X-XSRF-TOKEN': readCookie('XSRF-TOKEN') ?? ''},
        body: JSON.stringify(body),
    });
    const data = await responseData(response);
    if (!response.ok) {
        const error = new Error(uiErrorMessage(response.status, data));
        error.status = response.status;
        error.code = data?.code;
        error.errors = fieldErrorMessages(data?.errors);
        throw error;
    }
    return data;
}

const api = {
    async get(path) {
        return {data: await apiGet(path)};
    }
};

export function setApiToken() { /* Xác thực bằng cookie HttpOnly; hàm token cũ không lưu dữ liệu. */
}

export function getApiToken() {
    return null;
}

export default api;

let csrf;
export function resetCsrf() {csrf = undefined;}
async function jsonResponse(response) {
    const data = (await responseData(response)) ?? {};
    if (!response.ok) {
        if (response.status === 401 && data.code !== 'INVALID_CREDENTIALS') window.dispatchEvent(new CustomEvent('bonsai:session-expired'));
        const error = new Error(uiErrorMessage(response.status, data));
        Object.assign(error, {status: response.status, code: data.code, fields: fieldErrorMessages(data.fields),
            retryAfterSeconds: Number(response.headers.get('Retry-After') || data.retryAfterSeconds || 0)});
        throw error;
    }
    return data;
}
export async function authRequest(path, body, retry = true) {
    try {
        if (body !== undefined && !csrf) csrf = await authRequest('/csrf');
        const response = await fetch(`/api/auth${path}`, {method: body === undefined ? 'GET' : 'POST',
            credentials: 'include', signal: AbortSignal.timeout(15000), headers: {Accept: 'application/json',
                ...(body !== undefined ? {'Content-Type': 'application/json', [csrf.headerName]: csrf.token} : {})},
            ...(body !== undefined ? {body: JSON.stringify(body)} : {})});
        return await jsonResponse(response);
    } catch (error) {
        if (error.code === 'CSRF_INVALID' && retry) {resetCsrf(); return authRequest(path, body, false);}
        if (!error.status) {
            // DOMException.message is read-only; preserve timeout identity through a nested CSRF request.
            const message = error.name === 'TimeoutError'
                ? 'Máy chủ phản hồi quá lâu. Vui lòng thử lại.'
                : 'Không thể kết nối máy chủ. Vui lòng thử lại sau.';
            const connectionError = new Error(message, {cause: error});
            connectionError.name = error.name;
            throw connectionError;
        }
        throw error;
    }
}
