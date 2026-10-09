// Legacy write endpoints do not exist in Spring yet.
function pending() {
    window.dispatchEvent(new CustomEvent('bonsai:pending'));
    return Promise.reject(new Error('Chức năng đang được phát triển.'));
}

export default {post: pending, put: pending, patch: pending, delete: pending};
