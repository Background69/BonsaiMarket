# Frontend BonsaiMarket

Vue 3, Vue Router và Vite hiển thị marketplace P2P tiếng Việt theo Direction C. Token dùng chung ở
`src/css/foundations.css`; điều hướng chức năng ở `src/navigation/marketplace.js`.

Chạy `npm run dev -- --host 127.0.0.1 --port 5176` để kiểm tra UI cục bộ. Người dùng chạy backend riêng bằng IntelliJ;
Vite chuyển `/api` tới cổng 8080. Chạy `npm run build` để kiểm tra import tĩnh, import động và CSS.

`package.json` có dev/build/preview/test. Chạy `npm test` để kiểm tra vòng đời request đọc,
phản hồi cũ, phân trang và CSRF; runner từ chối thư mục test rỗng. Các bộ test trước đây không còn đầy đủ
trong checkout. `frontend/qa/` và runner provider browser cũ đã thiếu; không dùng lệnh cũ làm bằng chứng nghiệm thu.
Test Java chính thức nằm trong `../src/test`; MySQL opt-in cần schema QA riêng theo điều kiện từng lớp.

Bảng quản lý dùng HTML ngữ nghĩa và phân trang server. Tồn kho giữ trình sửa riêng với phát hiện xung đột
`expectedStock`. Bộ chọn ảnh lấy từ phản hồi tồn kho có giới hạn; chỉ sản phẩm được chọn mới mở form tải ảnh.
Không cần thêm dependency frontend.

Xem [bàn giao hiện tại](../CODEX_HANDOFF.md) và [audit source/kiểm thử](../artifacts/source-performance-audit-20261114/AUDIT_REPORT.md) để biết kết quả và giới hạn kiểm chứng.

## Contract khám phá và thảo luận

`GET /api/stores` giữ phản hồi mảng có giới hạn khi không có query. Có query thì nhận
`search,categoryId,page,size` và trả `content,page,size,totalElements,totalPages`; size 1–50, page 0–10000.
Tìm kiếm chuẩn hóa tiếng Việt, tối đa 160 ký tự/12 từ. Chỉ gian hàng hoạt động, chủ SELLER/ADMIN hoạt động
và có liên kết sản phẩm/danh mục công khai mới đủ điều kiện; sắp xếp ổn định theo ID gian hàng tăng dần.
Danh mục sai/không tồn tại trả lỗi; tham số tùy chọn bị bỏ qua vẫn giữ contract hợp lệ không query.

Sản phẩm mới ở Trang chủ dùng `sort=latest`, theo createdAt, không phải lần đầu công khai. Gợi ý có thứ tự
xác định, chỉ lấy sản phẩm công khai còn hàng, tối đa tám kết quả và một sản phẩm mỗi gian hàng.
Product Detail xếp sản phẩm cùng danh mục, khác gian hàng theo giá gần nhất/thời gian/ID; Trang chủ loại ID
đã có trong nhóm mới nhất. Không cá nhân hóa hoặc tạo số bán, độ phổ biến, xác minh hay rating giả.

Hỏi đáp đọc `/api/products/{id}/questions?page=0`, mỗi trang 10 mục. Ghi bằng session/CSRF:
`POST /api/account/products/{id}/questions {body}`, `PUT /api/seller/product-questions/{id}/answer {body,revision}`
và `DELETE /api/account/product-questions/{id}?revision=...`. Mỗi câu hỏi có một câu trả lời chính thức;
kể cả ADMIN cũng phải sở hữu gian hàng mới được trả lời. Tác giả/ADMIN được xóa mềm câu hỏi.
Giới hạn câu hỏi/trả lời là 2000/4000 ký tự; backend cho 20 câu hỏi/giờ, trùng nội dung cùng sản phẩm trong
10 phút trả 409; tối đa 30 dòng câu hỏi được trả lời/người bán/giờ. Xung đột revision không ghi đè dữ liệu mới.

Community dùng `/community`, `/community/create`, `/community/posts/:id`, `/community/posts/:id/edit`.
Đọc công khai qua `/api/community/posts`, chi tiết và bình luận; ghi qua `/api/account/community/posts`,
chi tiết/bình luận và `/api/account/community/comments/{id}`. Áp dụng quyền tác giả/ADMIN, CSRF, khóa revision
và xóa mềm. Bài bị ẩn/xóa trả 404 và chặn bình luận. DTO tác giả công khai không có liên hệ riêng tư.
Chủ đề BONSAI/SUCCULENTS/CARE/OTHER; tiêu đề/nội dung/bình luận tối đa 180/12000/3000 ký tự; trang 10 mục;
bài mới trước, bình luận cũ trước, dùng ID giữ thứ tự ổn định. Giới hạn DB là 5 bài/giờ, 30 bình luận/giờ,
chặn trùng trong 10 phút và vẫn tính dòng đã xóa. Source hiện có like/bookmark, ảnh đính kèm, trạng thái hỗ trợ,
chọn câu trả lời, liên kết sản phẩm và thông báo qua các controller Community tương ứng. Admin đọc/xử lý nội dung
tại `/api/admin/community/content` bằng revision và audit; không suy ra kiểm duyệt tự động. Nội dung vẫn là văn bản
được escape; ảnh đi qua upload/ownership riêng. Tin nhắn riêng dùng `/messages`, tách khỏi Community và chatbot.

Java H2/MockMvc kiểm tra các ranh giới trên; các test MySQL opt-in cần môi trường dùng riêng.
Collation/EXPLAIN MySQL, HTTP/session triển khai và nhà cung cấp cần xác minh riêng.

## Checkout, Orders và Payment sau merge

Giữ màn hình của đồng đội, dùng chung nghiệp vụ GHN/COD, tồn kho và hủy đơn. Mỗi lần chọn một gian hàng;
Mua ngay không xóa giỏ. Phí lấy từ GHN theo kiện đã đo; thiếu cấu hình không được dùng phí cố định thay thế.
Chọn báo phí rồi xác nhận COD; seller xác nhận và tạo vận đơn, buyer xem sự kiện Tracking từ API.

VNPAY sandbox chỉ mở khi server được cấu hình; không có nút xác nhận thanh toán giả. MoMo/chuyển khoản chưa có
quy trình xác thực nên bị chặn. Đơn COD không đổi sang trả trước sau khi đặt. Thanh toán, xác nhận đơn,
thu hộ và đối soát là các trạng thái riêng. UI không suy ra thành công từ query URL.

Flyway dùng baseline V1 cùng V2–V9 và repeatable seed chỉ trong dev. Dựng mới trên database riêng, rỗng;
giữ database nhóm cũ. Xem [hướng dẫn schema](../src/main/resources/db/reference/README.md);
không sửa checksum/history hoặc repair/reset DB cũ.

## Hiệu năng và vòng đời request

Route/component chính được lazy-load theo router. `useServerList` và `useAdminResource` hủy GET cũ
khi đổi bộ lọc/đường dẫn, reload hoặc unmount; revision guard vẫn chặn phản hồi trễ.
`marketplaceRequest` nhận `{signal}` ở đối số thứ năm, giữ deadline 15 giây và contract CSRF hiện có.
Không dùng hủy request làm căn cứ gửi lại thao tác ghi chưa rõ kết quả.
Legacy icon observer chỉ quét node mới/thay đổi trong `#app`; tên, glyph, nhãn truy cập và fallback giữ nguyên.
