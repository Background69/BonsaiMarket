// Browser fetch is the only active HTTP client. The shape matches legacy api.get(...).data.
export async function apiGet(path) {
    const response = await fetch(`/api${path}`, {credentials: 'include', headers: {Accept: 'application/json'}});
    if (!response.ok) {
        const error = new Error(`GET ${path} failed: HTTP ${response.status}`);
        error.status = response.status;
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

export function setApiToken() { /* Authentication is a later milestone. */
}

export function getApiToken() {
    return null;
}

export default api;
