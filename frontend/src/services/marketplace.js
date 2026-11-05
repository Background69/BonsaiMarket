import {uiErrorMessage,fieldErrorMessages,responseData} from './presentation.js';
import {authRequest} from './api.js';

// These existing controller methods intentionally return void. Keep every JSON contract strict.
function bodylessReceipt(path, method, status) {
    const endpoint = path.split('?')[0];
    if (method === 'POST' && status === 201)
        return /^\/account\/products\/\d+\/(?:reviews|questions\/\d+\/replies)$/.test(endpoint);
    if (status !== 200) return false;
    if (method === 'DELETE')
        return /^\/account\/(?:product-questions\/\d+|community\/(?:posts|comments)\/\d+|products\/\d+\/questions\/\d+\/replies\/\d+)$/.test(endpoint);
    if (method === 'PUT') return /^\/account\/products\/\d+\/questions\/\d+\/replies\/\d+$/.test(endpoint);
    return method === 'POST' && /^\/account\/conversations\/\d+\/read$/.test(endpoint);
}

// Thao tác ghi dùng HttpSession/CSRF hiện có; không gửi owner ID.
export async function marketplaceRequest(path, method = 'GET', body, retry = true) {
    const csrf = method === 'GET' ? null : await authRequest('/csrf');
    let response;
    try {response = await fetch('/api' + path, {method, credentials: 'include', signal: AbortSignal.timeout(15000),
        headers: {Accept: 'application/json', ...(csrf ? {[csrf.headerName]: csrf.token} : {}), ...(body === undefined ? {} : {'Content-Type': 'application/json'})},
        ...(body === undefined ? {} : {body: JSON.stringify(body)})});}
    catch {throw new Error('Không thể kết nối máy chủ. Tải lại dữ liệu để kiểm tra trước khi thử lại.');}
    const data = bodylessReceipt(path, method, response.status) ? null : await responseData(response);
    if (!response.ok) {
        if (data?.code === 'CSRF_INVALID' && retry) return marketplaceRequest(path, method, body, false);
        if (response.status === 401) window.dispatchEvent(new CustomEvent('bonsai:session-expired'));
        throw Object.assign(new Error(uiErrorMessage(response.status, data)), {status: response.status, code: data?.code, fields: fieldErrorMessages(data?.fields)});
    }
    return data;
}
