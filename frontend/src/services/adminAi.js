import {authRequest} from './api.js';

export async function adminAiRequest(path, body, signal, retry = true) {
    const csrf = body === undefined ? null : await authRequest('/csrf');
    let response;
    try {
        response = await fetch('/api/admin/ai' + path, {
            method: body === undefined ? 'GET' : 'POST', credentials: 'include',
            signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(35000)]) : AbortSignal.timeout(35000),
            headers: {Accept: 'application/json', ...(csrf ? {[csrf.headerName]: csrf.token, 'Content-Type': 'application/json'} : {})},
            ...(body === undefined ? {} : {body: JSON.stringify(body)}),
        });
    } catch (error) {
        if (signal?.aborted) throw error;
        throw new Error('Không nhận được phản hồi từ máy chủ. Bạn có thể gửi lại câu hỏi khi kết nối ổn định.');
    }
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        if (response.status === 401) window.dispatchEvent(new CustomEvent('bonsai:session-expired'));
        if (data.code === 'CSRF_INVALID' && retry) return adminAiRequest(path, body, signal, false);
        throw Object.assign(new Error(data.message || data.error || 'Không thể phân tích dữ liệu. Vui lòng thử lại sau.'),
            {code: data.code, status: response.status, retryAfter: Number(response.headers.get('Retry-After') || 0)});
    }
    return data;
}

export function safeAdminAiPath(path) {
    return typeof path === 'string' && /^(?:\/admin\/(?:products|categories|reports)|\/stores(?:\/[1-9][0-9]*)?|\/products(?:\/[1-9][0-9]*|\?categoryId=[1-9][0-9]*)?)$/.test(path) ? path : null;
}
