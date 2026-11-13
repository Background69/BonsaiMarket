# Thiết lập VNPAY Sandbox và GHN Staging

## 1. Đăng ký VNPAY Sandbox

Đăng ký tại [cổng sandbox](https://sandbox.vnpayment.vn/devreg/), chờ VNPAY cấp merchant cho Payment Gateway phiên bản 2.1.0. Dùng tài khoản, phương thức và thẻ thử nghiệm do VNPAY cung cấp; không nhập thẻ hoặc chuyển tiền thật. Đọc [đặc tả PAY](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html).

## 2. TMN Code

Lấy `vnp_TmnCode` của website/terminal được cấp trong email hoặc merchant portal sandbox. Đặt riêng trong Run Configuration dưới tên `VNPAY_TMN_CODE`. Không dùng mã merchant của người khác trong ví dụ tài liệu.

## 3. Hash Secret

Lấy `vnp_HashSecret` tương ứng đúng terminal, đặt `VNPAY_HASH_SECRET` trong cấu hình riêng. Không đưa khóa vào Git, ảnh, log, lệnh shell có thể lưu lịch sử hoặc phía Vue. Chỉ backend ký HMAC-SHA512.

## 4. Phân biệt định danh

`TMN Code`/Terminal ID nhận diện website ở Payment Gateway. `Hash Secret` ký dữ liệu gateway. OAuth Client ID và Client Secret thuộc hợp đồng khác, không thay thế hai trường này. GHN Token/ShopId cũng hoàn toàn độc lập với VNPAY.

## 5. Return URL

Source ký URL `${AUTH_FRONTEND_ORIGIN}/payment/callback`. Origin do server cấu hình, không nhận URL tùy ý từ Buyer. Đăng ký đúng URL này với merchant sandbox. Trang callback chỉ đọc trạng thái đã lưu sau kiểm tra chữ ký và ownership; không ghi nhận thanh toán từ trình duyệt.

## 6. IPN URL

Đăng ký `https://<public-host>/api/payments/vnpay/ipn`. Đây là GET server-to-server, không cần session và không tắt CSRF toàn cục. Gateway trả JSON `RspCode`: `00` ghi nhận, `02` đã xử lý, `97` sai chữ ký, `01` không có giao dịch, `04` sai merchant/amount, `99` chưa xác minh. Không redirect IPN sang frontend/login.

## 7. Public HTTPS khi chạy local

Dùng reverse proxy hoặc tunnel HTTPS do bạn quản lý để chuyển đúng IPN tới backend development đã chạy trong IntelliJ. Giới hạn đường dẫn được công khai, tắt access-log query string ở proxy và không lưu callback raw. Nếu Return cũng qua tunnel, cấu hình proxy Vue/API cùng origin để session hoạt động. HTTPS dùng cookie secure; HTTP localhost chỉ dùng `AUTH_COOKIE_SECURE=false`. Kiểm tra certificate, routing và firewall bằng callback hợp lệ; không coi GET trống trả `97` là thanh toán thành công. Task không tự cài tunnel, thay origin hay restart IntelliJ.

## 8. Đăng ký GHN Staging

Đăng ký/đăng nhập [Developer Portal Staging](https://developer.ghn.dev/vi). Staging và production dùng tài khoản riêng. Adapter chỉ có host `https://dev-online-gateway.ghn.vn`; không có công tắc đổi sang production.

## 9. API Token

Theo [hướng dẫn Token](https://developer.ghn.dev/vi/docs/token/get-token), lấy Token của đúng tài khoản staging. Đặt `SHIPPING_GHN_TOKEN` ngoài source; không dùng biến `VITE_*`. Không chụp màn hình phần token.

## 10. ShopId của từng gian hàng

Theo [hướng dẫn Shop](https://developer.ghn.dev/vi/docs/shop/get-shop), chọn shop đang hoạt động thuộc tài khoản Token, xác minh kho lấy hàng là kho được cấp quyền. Mỗi BonsaiMarket store phải có một ShopId riêng; mapping trùng bị từ chối. Nếu chỉ được cấp một shop QA, chỉ cấu hình một store QA đó. Không dùng địa chỉ lấy hàng của gian hàng khác.

## 11. Kiểm tra quyền shop

Backend gọi GET `/shiip/public-api/v2/shop/all?offset=0&limit=100` với header Token, tìm ShopId có `status=1` trong tối đa 5 trang. Có thể kiểm tra qua công cụ HTTP cục bộ lưu Token bằng secret variable: xác minh `_id`, `district_id`, `ward_code` và kho; chỉ lưu kết luận đã che PII. Không copy toàn bộ response chứa thông tin cá nhân vào report.

## 12. GHN webhook

Trong portal staging, menu tài khoản → Cấu hình webhook → tab Order, đăng ký `https://<public-host>/api/shipping/webhooks/ghn`. Thêm custom header `X-GHN-Webhook-Secret` bằng giá trị bí mật ngẫu nhiên ít nhất 32 ký tự, trùng `SHIPPING_GHN_WEBHOOK_SECRET` trên backend. Đăng ký các sự kiện `create`, `switch_status`; `cod` chỉ tạo báo cáo cần đối soát. Portal hiện hỗ trợ custom headers, timeout và retry; thay đổi có thể trễ vì cache. Xem [contract webhook](https://developer.ghn.dev/vi/docs/webhook/callback-order-status).

Nếu account không hỗ trợ custom header hoặc header chưa tới backend: giữ callback đóng, dùng Seller/Admin sync để đọc chi tiết từ GHN. Không bỏ xác thực để lấy PASS. ShopID, OrderCode, ClientOrderCode và thời điểm phải khớp. Delivered không đồng nghĩa đã thu hoặc quyết toán COD.

## 13. Environment variables và cấu hình server

Chỉ điền qua Run Configuration/private config, không sửa template bằng secrets:

```dotenv
VNPAY_ENABLED=true
VNPAY_TMN_CODE=<SANDBOX_TMN_CODE>
VNPAY_HASH_SECRET=<SANDBOX_HASH_SECRET>
VNPAY_REFUND_ENABLED=false
VNPAY_SERVER_IP=<MERCHANT_SERVER_IP>
SHIPPING_GHN_ENABLED=true
SHIPPING_GHN_TOKEN=<STAGING_TOKEN>
SHIPPING_GHN_WEBHOOK_SECRET=<RANDOM_SECRET_MIN_32_CHARS>
AUTH_FRONTEND_ORIGIN=http://localhost:5173
```

Mapping là cấu hình Spring trên server. Ví dụ nội dung private properties (thay cả placeholders, không commit file):

```properties
shipping.ghn.shop-ids[<STORE_ID>]=<OWNED_STAGING_SHOP_ID>
shipping.ghn.cod-enabled-store-ids[0]=<COD_AUTHORIZED_STORE_ID>
```

Có thể truyền private properties qua `SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:C:/<private-directory>/provider-sandbox.properties`. Không đưa Token/Secret vào JSON của frontend. COD allowlist độc lập với mapping shop; VNPAY prepaid không cần quyền thu hộ COD. Profile `dev` hiện đã import `.env` tùy chọn; Run Configuration có thể ghi đè file. Không bật wallet sandbox trên database development: ví giữ gate schema `bm_wallet_qa_*` và enrollment hiện có.

V9 là migration mới cho provider lifecycle, refund operations và partial wallet. Chưa áp dụng MySQL trong task do Docker/MySQL không khả dụng. Trước khi user restart source mới: đọc history thực, backup và rehearsal trên clone/QA; Flyway validate, không repair/clean/baseline. Không sửa V1–V8 đã áp dụng.

## 14. Kiểm thử thanh toán

Buyer chọn VNPAY **trước tính phí**. Server lấy quote prepaid GHN, đặt một store, ghi order/inventory/receipt; vào chi tiết đơn → thanh toán trực tuyến. Init lưu một PENDING attempt, mã riêng, integer VND ×100, hạn 15 phút. Trình duyệt chuyển tới sandbox. Sau Return phải chờ IPN hoặc dùng đối soát; đóng trình duyệt không mất order/attempt. IPN hợp lệ cập nhật giao dịch, không tự xác nhận đơn thay Seller.

Thử success/failure/cancel, Return trước IPN/IPN trước Return, callback lặp/sai chữ ký/amount/reference, quote hết hạn và concurrent attempts. Không tạo attempt mới khi PENDING/UNKNOWN/EXPIRED. QueryDr tối đa 5 lần, cách nhau 5 phút; terminal FAILED/SUCCESS không bị callback trái chiều đổi lại. Khi hết giới hạn hoặc mất ngày khởi tạo gốc, chuyển hỗ trợ merchant.

## 15. Kiểm thử giao nhận

Seller khai kiện **đã đo** qua contract CheckoutPackage (gram/cm, một kiện dưới 20kg), không dùng chiều cao cây. Địa chỉ phải khớp duy nhất danh mục GHN. Fee hiện yêu cầu district/ward hệ cũ; địa chỉ hai cấp được nhận diện nhưng bị chặn nếu không có mã tương thích đã xác minh. Không bịa district hoặc phí 0. Xem [fee](https://developer.ghn.dev/vi/docs/order/calculate-fee) và [create](https://developer.ghn.dev/vi/docs/order/create).

Sau order: Seller xác nhận → tạo vận đơn. COD phải trong allowlist; VNPAY phải SUCCESS đủ tiền và không có refund đang mở. Ví mô phỏng không được tạo vận đơn bên ngoài. Tạo shipment dùng reservation/client_order_code bền vững, gọi mạng ngoài DB transaction. Timeout/crash giữ UNKNOWN, đọc theo client code trước mọi xử lý tiếp; không gửi create lần hai. Kiểm tra tracking DB, webhook, sync, hủy trước lấy hàng và isolation Seller. Không suy diễn giao hàng vật lý từ staging.

## 16. Kiểm thử hoàn tiền

Đọc [đặc tả QueryDr/Refund](https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr%26refund.html). Chỉ bật `VNPAY_REFUND_ENABLED=true` sau khi merchant được cấp quyền sandbox. Buyer/Admin lập hồ sơ; Admin duyệt → thực thi riêng. Một hồ sơ điều chỉnh được duyệt trên mỗi order theo policy hiện tại. Full trước bàn giao cần chưa có shipment hoặc shipment đã xác minh hủy. Partial chỉ sau return `INSPECTED`, không vượt nguồn gốc. Hoàn tiền không tự nhập kho hàng đang vận chuyển.

VNPAY dùng loại `02` full, `03` partial, request ID riêng và ngày khởi tạo payment gốc. `05`/`06` vẫn chờ provider; chỉ chữ ký hợp lệ và trạng thái hoàn tất đúng amount/type mới ghi SUCCEEDED. Nếu đã có mã hoàn được xác minh, Admin dùng đối soát QueryDr, kiểm đúng mã hoàn/type/amount. Timeout không có mã hoàn: tra merchant bằng provider_request_id qua endpoint Admin operation; **không tự gửi lại** và không coi QueryDr giao dịch thu tiền là refund. Cần hỗ trợ VNPAY nếu API không cung cấp liên kết hoàn đủ rõ.

Ví VND: hoàn về đúng Buyer và original debit, balanced append-only journal, cùng transaction với refund history/outbox. Ví vẫn là tiền mô phỏng. COD chưa thu có thể hủy; COD có dấu hiệu thu/chuyển/đối soát chỉ lập nghĩa vụ review, không credit ví và không ghi đã hoàn khi chưa có payout contract.

## 17. Lỗi thường gặp

| Tình huống | Xử lý |
|---|---|
| VNPAY unavailable | Kiểm enabled, TMN/Hash Secret đúng gateway, không dùng OAuth credentials |
| Checksum không khớp | Kiểm secret của terminal, thứ tự/encoding gateway 2.1.0; không bỏ verification |
| Return vẫn chờ | Kiểm routing IPN/public HTTPS, dùng QueryDr có giới hạn; không thanh toán thêm |
| QueryDr 91/94 hoặc timeout | Giữ UNKNOWN và cooldown; không suy thành FAILED/PAID |
| Refund chưa có quyền | Giữ APPROVED; cấp quyền merchant rồi mới thực thi |
| Refund đã gửi nhưng timeout | Tra mã lệnh trong merchant, không retry blind |
| GHN shop không thuộc Token | Sửa mapping đúng account/kho, không mượn shop Seller khác |
| Địa chỉ hai cấp không báo phí | Fee contract còn cần district compatibility; yêu cầu mapping provider, không tự chuyển mã |
| Phí đổi trước create | Chặn create, Buyer phải xem tổng mới qua order/quote mới; không tự sửa tổng đã thu |
| GHN create chưa rõ | Sync theo stable client reference, giữ inventory/order; không tạo double shipment |

## 18. Bảo mật và evidence

Chỉ các host sandbox/staging cố định. Giữ session/ownership/CSRF và quyền Admin; không nhận total, ShopId hay owner ID làm nguồn tin từ client. Không lưu raw callback, token, secret, mật khẩu, thông tin thẻ vào report/screenshot/Git. Chỉ ghi IDs thử nghiệm và kết luận đã che PII.

Phân loại A=unit, B=real MySQL + services + doubles, C=HTTP provider thật có credentials, D=browser→MySQL→provider→callback/query. H2/Chrome với doubles là bằng chứng cô lập, không thay B/C/D. Khi chưa có account: `BLOCKED_EXTERNAL — VNPAY sandbox credentials/merchant access missing`, và GHN staging token/shop access missing. Cần credentials, shop được cấp quyền và URL callback public từ người vận hành để mở khóa.
