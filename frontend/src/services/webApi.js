// Spring chưa có các endpoint ghi cũ.
function pending() {
    window.dispatchEvent(new CustomEvent('bonsai:pending'));
    return Promise.reject(new Error('Chức năng đang được phát triển.'));
}

export default {post: pending, put: pending, patch: pending, delete: pending};
