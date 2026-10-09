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
