// Browser fetch is the only active HTTP client. The shape matches legacy api.get(...).data.
export async function apiGet(path) {
    const response = await fetch(`/api${path}`, {credentials: 'include', headers: {Accept: 'application/json'}});
    if (!response.ok) {
        if (response.status === 401) window.dispatchEvent(new CustomEvent('bonsai:session-expired'));
        const body = await response.json().catch(() => ({}));
        const error = new Error(body.message || `Không thể tải dữ liệu (HTTP ${response.status}).`);
        error.status = response.status;
        error.code = body.code;
        throw error;
    }
    return response.json();
}

function readCookie(name) {
    const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
    return match ? decodeURIComponent(match[1]) : null;
}

// The backend sets the XSRF-TOKEN cookie on any response; prime it once if it is missing.
async function ensureCsrfToken() {
    if (!readCookie('XSRF-TOKEN')) await fetch('/api/auth/me', {credentials: 'include', headers: {Accept: 'application/json'}});
}

export async function apiPost(path, body = {}) {
    await ensureCsrfToken();
    const response = await fetch(`/api${path}`, {
        method: 'POST',
        credentials: 'include',
        headers: {Accept: 'application/json', 'Content-Type': 'application/json', 'X-XSRF-TOKEN': readCookie('XSRF-TOKEN') ?? ''},
        body: JSON.stringify(body),
    });
    const data = response.status === 204 ? null : await response.json().catch(() => null);
    if (!response.ok) {
        const error = new Error(data?.message || `POST ${path} failed: HTTP ${response.status}`);
        error.status = response.status;
        error.code = data?.code;
        error.errors = data?.errors || {};
        throw error;
    }
    return data;
}

const api = {
    async get(path) {
        return {data: await apiGet(path)};
    }
};

export function setApiToken() { /* Session auth uses HttpOnly cookies; legacy token helpers store nothing. */
}

export function getApiToken() {
    return null;
}

export default api;

let csrf;
export function resetCsrf() {csrf = undefined;}
async function jsonResponse(response) {
    let data;
    try {data = await response.json();} catch {data = {};}
    if (!response.ok) {
        if (response.status === 401 && data.code !== 'INVALID_CREDENTIALS') window.dispatchEvent(new CustomEvent('bonsai:session-expired'));
        const error = new Error(data.message || 'Không thể xử lý yêu cầu. Vui lòng thử lại.');
        Object.assign(error, {status: response.status, code: data.code, fields: data.fields || {},
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
        if (!error.status) error.message = 'Không thể kết nối máy chủ. Vui lòng thử lại sau.';
        throw error;
    }
}
