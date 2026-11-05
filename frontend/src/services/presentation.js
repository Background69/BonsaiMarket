// Presentation of machine values only. Never run these helpers over user-authored content.
export function labelFor(labels, value, fallback = 'Chưa có thông tin') {
    return Object.hasOwn(labels, value) ? labels[value] : fallback;
}

export const paymentMethods = {COD: 'Thanh toán khi nhận hàng (COD)', VNPAY: 'Thanh toán qua VNPAY',
    MOMO: 'Ví MoMo', BANK_TRANSFER: 'Chuyển khoản ngân hàng'};
export const transactionStates = {PENDING: 'Chờ thanh toán', SUCCESS: 'Giao dịch thành công',
    FAILED: 'Giao dịch thất bại', CANCELLED: 'Đã hủy giao dịch'};
export const auditActions = {COMMUNITY_REPORTED: 'Đã gửi báo cáo nội dung', COMMUNITY_HIDE: 'Đã ẩn nội dung',
    COMMUNITY_RESTORE: 'Đã khôi phục nội dung', COMMUNITY_REMOVE: 'Đã gỡ nội dung',
    COMMUNITY_PIN: 'Đã ghim bài viết', COMMUNITY_UNPIN: 'Đã bỏ ghim bài viết',
    COMMUNITY_LOCK: 'Đã đóng thảo luận', COMMUNITY_UNLOCK: 'Đã mở lại thảo luận',
    REPORT_IN_REVIEW: 'Đang xem xét báo cáo', REPORT_RESOLVED: 'Đã xử lý báo cáo', REPORT_DISMISSED: 'Đã bác bỏ báo cáo'};
export const paymentMethodLabel = value => labelFor(paymentMethods, value, 'Phương thức cần kiểm tra');
export const transactionLabel = value => labelFor(transactionStates, value, 'Trạng thái giao dịch cần kiểm tra');
export function dateTimeLabel(value) {
    if (!value) return 'Chưa có thông tin thời gian';
    const date = new Date(/(?:Z|[+-]\d\d:\d\d)$/.test(value) ? value : value + 'Z');
    return Number.isNaN(date.getTime()) ? 'Chưa có thông tin thời gian'
        : new Intl.DateTimeFormat('vi-VN', {dateStyle: 'short', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh'}).format(date);
}

const messages = {400: 'Vui lòng kiểm tra lại thông tin đã nhập.',
    401: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
    403: 'Bạn không có quyền thực hiện thao tác này.', 404: 'Nội dung này không còn khả dụng.',
    409: 'Nội dung đã được cập nhật ở nơi khác. Vui lòng tải lại trước khi tiếp tục.',
    413: 'Tệp vượt quá dung lượng cho phép.', 422: 'Vui lòng kiểm tra lại thông tin đã nhập.',
    429: 'Bạn thao tác quá nhanh. Vui lòng chờ trước khi thử lại.'};
const codeMessages = {INVALID_CREDENTIALS: 'Email hoặc mật khẩu chưa đúng.',
    CSRF_INVALID: 'Phiên thao tác đã hết hạn. Vui lòng tải lại trang và thử lại.',
    PAYMENT_NOT_CONFIGURED: 'Thanh toán trực tuyến hiện chưa khả dụng. Vui lòng chọn phương thức khác nếu có.',
    SHIPPING_NOT_CONFIGURED: 'Chưa thể tính phí vận chuyển. Vui lòng thử lại sau; đơn hàng hiện tại được giữ nguyên.',
    AUTH_NOT_CONFIGURED: 'Hiện chưa thể gửi mã xác minh. Vui lòng thử lại sau.',
    OAUTH_NOT_CONFIGURED: 'Đăng nhập mạng xã hội hiện chưa khả dụng. Vui lòng dùng email và mật khẩu.',
    MAIL_DELIVERY_FAILED: 'Chưa xác nhận được việc gửi mã. Vui lòng thử gửi lại sau.'};
// Only error-channel text is checked here, not post bodies, names, reasons, messages or other user data.
const internalError = /(?:SQLException|NullPointerException|Exception\b|org\.springframework|java\.[a-z]|stack\s*trace|SQL\s+(?:syntax|statement)|\b(?:SELECT|INSERT|UPDATE|DELETE)\b[\s\S]*\b(?:FROM|INTO|SET|WHERE)\b|bodySHA256|revision=|[A-Z][A-Z0-9]+(?:_[A-Z0-9]+)+|https?:\/\/|(?:[A-Za-z]:[\\/]|\/(?:home|usr|var|api)\/)|[<>]|^[\s]*[\[{])/i;
function safeMessage(value) {
    return typeof value === 'string' && value.length <= 800 && /[À-ỹĐđ]/u.test(value)
        && !internalError.test(value) ? value : null;
}
export function uiErrorMessage(status, payload = {}) {
    if (Object.hasOwn(codeMessages, payload?.code)) return codeMessages[payload.code];
    if (status >= 500) return 'Đã xảy ra sự cố. Vui lòng thử lại sau.';
    return safeMessage(payload?.message)
        || messages[status] || 'Không thể kết nối. Vui lòng thử lại.';
}
export function fieldErrorMessages(fields) {
    if (!fields || typeof fields !== 'object' || Array.isArray(fields)) return {};
    return Object.fromEntries(Object.entries(fields).filter(([key]) => /^[\w.[\]]{1,100}$/.test(key))
        .map(([key, value]) => [key, safeMessage(value) || 'Vui lòng kiểm tra lại thông tin ở trường này.']));
}
export function unreadableResponseError() {
    return Object.assign(new Error('Dữ liệu phản hồi chưa hợp lệ. Vui lòng tải lại để kiểm tra kết quả.'),
        {status: 502, code: 'RESPONSE_INVALID'});
}
export async function responseData(response) {
    if (response.status === 204) return null;
    try {return await response.json();}
    catch {
        if (response.ok) throw unreadableResponseError();
        return {}; // Non-JSON failures still keep their actual HTTP status and localized fallback.
    }
}
