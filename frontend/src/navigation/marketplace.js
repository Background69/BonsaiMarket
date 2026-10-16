// Only destinations backed by a working workflow belong in primary navigation.
// These links are presentation; Spring remains the authorization boundary.
export const workspaceNavigation = {
    admin: [
        {to: '/admin', label: 'Tổng quan', icon: 'grid'},
        {to: '/admin/categories', label: 'Danh mục', icon: 'list', description: 'Thêm, sửa và quản lý hiển thị danh mục'},
        {to: '/admin/store-applications', label: 'Yêu cầu mở gian hàng', icon: 'store', description: 'Kiểm tra hồ sơ và ghi nhận quyết định'},
        {to: '/admin/reports', label: 'Báo cáo vi phạm', icon: 'bag', description: 'Tiếp nhận báo cáo và theo dõi kết quả xử lý'},
        {to: '/admin/products', label: 'Tra cứu sản phẩm', icon: 'leaf', description: 'Tìm sản phẩm, gian hàng, giá và tồn kho công khai'},
    ],
    seller: [
        {to: '/seller', label: 'Tổng quan', icon: 'grid'},
        {to: '/seller/products', label: 'Sản phẩm và tồn kho', icon: 'leaf', description: 'Kiểm tra số lượng và cập nhật tồn kho từng sản phẩm'},
        {to: '/seller/images', label: 'Hình ảnh sản phẩm', icon: 'leaf', description: 'Chọn sản phẩm, xem và cập nhật ảnh chính'},
        {to: '/seller/store', label: 'Thông tin gian hàng', icon: 'store', description: 'Chỉnh sửa giới thiệu và thông tin liên hệ công khai'},
    ],
};

export function sellerDestination(user) {
    if (user?.role === 'admin') return {to: '/admin', label: 'Quản trị sàn'};
    if (user?.role === 'seller') return {to: '/seller', label: 'Kênh người bán'};
    // Pending and rejected customers land on their server-backed application state.
    return {to: user ? '/store-application' : '/login?redirect=%2Fstore-application', label: 'Mở gian hàng'};
}
