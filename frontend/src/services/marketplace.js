import {authRequest} from './api.js';

// Mutations share the existing HttpSession/CSRF architecture; never send an owner ID.
export async function marketplaceRequest(path, method = 'GET', body, retry = true) {
    const csrf = method === 'GET' ? null : await authRequest('/csrf');
    let response;
    try {response = await fetch('/api' + path, {method, credentials: 'include', signal: AbortSignal.timeout(15000),
        headers: {Accept: 'application/json', ...(csrf ? {[csrf.headerName]: csrf.token} : {}), ...(body === undefined ? {} : {'Content-Type': 'application/json'})},
        ...(body === undefined ? {} : {body: JSON.stringify(body)})});}
    catch {throw new Error('Không thể kết nối máy chủ. Tải lại dữ liệu để kiểm tra trước khi thử lại.');}
    const data = response.status === 204 ? null : await response.json().catch(() => ({}));
    if (!response.ok) {
        if (data.code === 'CSRF_INVALID' && retry) return marketplaceRequest(path, method, body, false);
        if (response.status === 401) window.dispatchEvent(new CustomEvent('bonsai:session-expired'));
        throw Object.assign(new Error(data.message || 'Không thể xử lý yêu cầu. Vui lòng tải lại và thử lại.'), {status: response.status, code: data.code, fields: data.fields || {}});
    }
    return data;
}
