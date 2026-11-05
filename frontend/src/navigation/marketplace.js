// Điều hướng chính chỉ dẫn tới luồng đang hoạt động.
// Liên kết chỉ phục vụ hiển thị; Spring quyết định quyền truy cập.
export const workspaceNavigation = {
    admin: [
        {to:'/admin/profile',label:'Hồ sơ quản trị viên',icon:'user',group:3},
        {to:'/admin/notifications',label:'Trung tâm thông báo',icon:'bell',group:3},
        {to:'/admin/audit',label:'Nhật ký quản trị',icon:'clock',group:3},
        {to:'/admin/stores',label:'Gian hàng',icon:'store'},
        {to:'/admin/transactions',label:'Giao dịch & thanh toán',icon:'wallet',group:2},
        {to:'/admin/disputes',label:'Tranh chấp',icon:'chat'},
        {to: '/messages', label: 'Tin nhắn hỗ trợ', icon: 'chat', description: 'Chỉ hội thoại bạn tham gia'},
        {to: '/admin', label: 'Tổng quan', icon: 'grid'},
        {to: '/admin/categories', label: 'Danh mục', icon: 'list', description: 'Thêm, sửa và quản lý hiển thị danh mục'},
        {to: '/admin/ai-assistant', label: 'Trợ lý AI', icon: 'ai', description: 'Phân tích dữ liệu và gợi ý kiểm tra, chỉ đọc'},
        {to: '/admin/store-applications', label: 'Yêu cầu mở gian hàng', icon: 'store', description: 'Kiểm tra hồ sơ và ghi nhận quyết định'},
        {to: '/admin/reports', label: 'Báo cáo vi phạm', icon: 'bag', description: 'Tiếp nhận báo cáo và theo dõi kết quả xử lý'},
        {to: '/admin/community', label: 'Cộng đồng', icon: 'users', description: 'Bài viết, báo cáo và lịch sử kiểm duyệt'},
        {to: '/admin/products', label: 'Tra cứu sản phẩm', icon: 'leaf', description: 'Tìm sản phẩm, gian hàng, giá và tồn kho công khai'},
        {to: '/admin/statistics', label: 'Thống kê', icon: 'chart', description: 'Doanh thu theo ngày và theo danh mục sản phẩm'},
        {to: '/admin/users', label: 'Người dùng', icon: 'users', description: 'Tìm kiếm, xem hồ sơ và khóa hoặc mở khóa tài khoản'},
        {to: '/admin/orders', label: 'Đơn hàng', icon: 'bag', description: 'Theo dõi và xử lý đơn hàng của toàn sàn'},
        {to: '/admin/refunds', label: 'Hồ sơ hoàn tiền', icon: 'bag', description: 'Xét duyệt và theo dõi đối soát nội bộ'},
    ],
    seller: [
        {to: '/messages', label: 'Tin nhắn riêng tư', icon: 'chat', description: 'Hộp thư trao đổi với người mua'},
        {to: '/seller', label: 'Tổng quan', icon: 'grid'},
        {to: '/seller/orders', label: 'Đơn hàng', icon: 'bag', description: 'Xác nhận, giao và theo dõi đơn hàng của gian hàng'},
        {to: '/seller/questions', label: 'Hỏi đáp sản phẩm', icon: 'chat', description: 'Tiếp nhận câu hỏi và mở đúng sản phẩm để trả lời'},
        {to: '/seller/products', label: 'Sản phẩm và tồn kho', icon: 'leaf', description: 'Kiểm tra số lượng và cập nhật tồn kho từng sản phẩm'},
        {to: '/seller/images', label: 'Hình ảnh sản phẩm', icon: 'leaf', description: 'Chọn sản phẩm, xem và cập nhật ảnh chính'},
        {to: '/seller/store', label: 'Thông tin gian hàng', icon: 'store', description: 'Chỉnh sửa giới thiệu và thông tin liên hệ công khai'},
    ],
};

export function sellerDestination(user) {
    if (user?.role === 'admin') return {to: '/admin', label: 'Quản trị sàn'};
    if (user?.role === 'seller') return {to: '/seller', label: 'Kênh người bán'};
    // Khách chờ duyệt hoặc bị từ chối tới trạng thái hồ sơ từ server.
    return {to: user ? '/store-application' : '/login?redirect=%2Fstore-application', label: 'Mở gian hàng'};
}
