# BonsaiMarket

BonsaiMarket là website thương mại điện tử dành cho việc giới thiệu, tìm kiếm và mua bán bonsai, cây cảnh, chậu cây cùng các sản phẩm liên quan. Dự án được xây dựng theo mô hình nhiều gian hàng, trong đó người mua có thể xem sản phẩm của các người bán khác nhau, còn người bán có khu vực quản lý riêng.

Dự án sử dụng Spring Boot cho backend, Vue cho frontend và MySQL để lưu trữ dữ liệu. Các thay đổi cấu trúc cơ sở dữ liệu được quản lý bằng Flyway.

## 1. Công nghệ sử dụng

| Thành phần | Công nghệ |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1.1, Spring Security |
| Quản lý thư viện backend | Maven |
| Frontend | Vue 3, Vite, Vue Router |
| Cơ sở dữ liệu | MySQL 8.4 |
| Quản lý thay đổi cơ sở dữ liệu | Flyway |
| Môi trường cơ sở dữ liệu cục bộ | Docker, Docker Compose |
| Kiểm thử backend | JUnit, H2, MockMvc và các thành phần giả lập phù hợp |

## 2. Các chức năng của hệ thống

### 2.1. Khu vực công khai

- Trang chủ giới thiệu sản phẩm và nội dung của sàn.
- Danh sách sản phẩm, tìm kiếm, lọc và xem chi tiết sản phẩm.
- Xem danh mục sản phẩm.
- Khám phá gian hàng và xem thông tin gian hàng.
- Xem các thông tin sản phẩm được công khai từ backend.

### 2.2. Tài khoản người dùng

- Đăng ký, đăng nhập, đăng xuất và xác thực tài khoản theo luồng đang được triển khai trong backend.
- Quản lý thông tin tài khoản và địa chỉ giao hàng.
- Phân quyền truy cập các khu vực dành cho người mua, người bán và quản trị viên.

### 2.3. Khu vực người bán

- Quản lý một số thông tin của gian hàng.
- Quản lý hình ảnh, thông tin bổ sung và tồn kho sản phẩm trong phạm vi gian hàng được sở hữu.
- Truy cập các màn hình quản lý đơn hàng và thống kê đã được tích hợp.

### 2.4. Khu vực quản trị

- Quản lý danh mục.
- Xem xét yêu cầu đăng ký người bán và báo cáo của người dùng.
- Tra cứu thông tin sản phẩm công khai.
- Truy cập các màn hình quản lý người dùng, đơn hàng và thống kê đã được tích hợp.

**Tiến độ sau merge:** Cart, Checkout COD, Orders, tồn kho và GHN dùng chung contract backend. Mỗi lần đặt chọn một gian hàng, nhập/chọn địa chỉ, lấy phí theo kiện đã đo rồi xác nhận COD; sản phẩm gian hàng khác vẫn trong giỏ. Người bán xác nhận và tạo vận đơn; người mua xem Tracking từ API. VNPAY sandbox cần cấu hình riêng; MoMo, chuyển khoản và hoàn tiền tự động chưa có contract xác thực. Không có xác nhận thanh toán giả.

**Database mới:** đã thống nhất một baseline V1 cho 36 bảng và một repeatable seed chỉ dùng dev. SQL cũ được lưu nguyên byte ngoài Flyway locations. Bản source này chỉ dùng database mới, rỗng; giữ nguyên database nhóm cũ. Xem [hướng dẫn chuyển môi trường](db/README.md).

## 3. Yêu cầu trước khi chạy dự án

Máy phát triển cần có:

1. **JDK 21** để biên dịch và chạy Spring Boot.
2. **Node.js và npm** tương thích với phiên bản Vite đang sử dụng trong `frontend/package.json`.
3. **Docker Desktop** hoặc Docker Engine có hỗ trợ Docker Compose để chạy MySQL tại máy cục bộ.
4. **Git** để tải và đồng bộ mã nguồn.
5. **IntelliJ IDEA** hoặc công cụ khác có thể chạy ứng dụng Java, nếu muốn khởi động backend bằng IDE.

Có thể kiểm tra các công cụ đã cài bằng terminal:

```powershell
java -version
node --version
npm --version
docker --version
docker compose version
```

Nếu một lệnh không chạy được, hãy kiểm tra đường dẫn môi trường (`PATH`) hoặc việc cài đặt công cụ đó trước khi tiếp tục.

## 4. Chuẩn bị database MySQL mới

Baseline hiện tại chỉ dành cho MySQL 8.4 mới, rỗng. Giữ nguyên database, container và volume nhóm đang dùng; Compose cũ vẫn mô tả môi trường cũ, không tự tạo lại hoặc đổi volume để dùng baseline. Nhờ người quản trị tạo schema mới, cấp quyền riêng và xác minh đích theo [db/README.md](db/README.md).

Thiết lập `DB_URL` rõ ràng trong Run Configuration riêng, ví dụ `jdbc:mysql://localhost:3306/bonsaimarket_dev_new` sau khi schema này đã được tạo và kiểm tra trống. Không có URL mặc định. Không ghi đè `.env` thật của nhóm; `.env.example` chỉ là ví dụ cấu hình mới.

Để kiểm thử tự động không dùng DB nhóm, harness `node frontend/qa/run-checkout-mysql.js --acceptance` tạo MySQL trên tmpfs, xác minh schema rỗng, rồi xóa đúng container/network task trong finally.

## 5. Chạy backend Spring Boot

### 5.1. Chạy từ IntelliJ IDEA

Đây là cách thuận tiện khi phát triển BonsaiMarket trên Windows.

1. Mở thư mục BonsaiMarket bằng IntelliJ IDEA.
2. Chờ IDE tải Maven dependencies và nhận diện JDK 21.
3. Vào **Run → Edit Configurations**.
4. Chọn cấu hình chạy ứng dụng Spring Boot của dự án.
5. Trong **Environment variables**, đặt các biến kết nối cơ sở dữ liệu và profile phát triển.
6. Lưu cấu hình và chạy ứng dụng.

Những biến tối thiểu cần kiểm tra:

```dotenv
DB_URL=jdbc:mysql://localhost:3306/bonsaimarket_dev_new
DB_USERNAME=<tai_khoan_database_moi>
DB_PASSWORD=<mat_khau_database_moi>
# DEV_SEED_PASSWORD_HASH tùy chọn; bỏ trống dùng mật khẩu demo dev 123456
SPRING_PROFILES_ACTIVE=dev
```

`DB_URL` bắt buộc; xác minh host/schema mới và trống trước khi chạy. `DEV_SEED_PASSWORD_HASH` là override BCrypt tùy chọn trong dev; bỏ trống dùng mật khẩu demo `123456`.

**Cấu hình dev hiện tại:** IntelliJ không tự nhập biến môi trường từ Docker Compose, nhưng `application.yaml` của profile `dev` có `optional:file:.env[.properties]`, nên Spring đọc `.env` ở thư mục làm việc. Profile khác không tự nạp file này. Biến môi trường/Run Configuration có thể ghi đè giá trị file; mật khẩu backend phải khớp MySQL.

### 5.2. Chạy từ PowerShell

Nếu không dùng IntelliJ IDEA, mở PowerShell tại thư mục gốc:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/bonsaimarket_dev_new'
$env:DB_USERNAME = '<tai_khoan_database_moi>'
$env:DB_PASSWORD = '<mat_khau_database_moi>'
# Không cần DEV_SEED_PASSWORD_HASH với database demo mới.
$env:SPRING_PROFILES_ACTIVE = 'dev'
.\mvnw.cmd spring-boot:run
```

Các biến được đặt bằng `$env:` chỉ áp dụng cho tiến trình được khởi chạy từ môi trường terminal tương ứng; chúng không tự động truyền sang phiên chạy độc lập của IntelliJ IDEA.

Nếu Maven Wrapper trên Windows gặp lỗi, có thể thử bản Maven đã được cài sẵn:

```powershell
mvn spring-boot:run
```

Trên macOS/Linux, dùng `./mvnw spring-boot:run` sau khi thiết lập các biến môi trường tương đương.

### 5.3. Kiểm tra backend

Khi ứng dụng khởi động thành công, kiểm tra:

```text
http://localhost:8080/actuator/health
http://localhost:8080/api/categories
http://localhost:8080/api/products
http://localhost:8080/api/products/1
http://localhost:8080/api/stores
http://localhost:8080/api/stores/1
```

Endpoint `actuator/health` được dùng để kiểm tra trạng thái ứng dụng. Những đường dẫn sản phẩm hoặc gian hàng theo ID chỉ trả dữ liệu khi bản ghi tương ứng tồn tại và có thể truy cập công khai.

Nếu health không trả trạng thái `UP`, hãy kiểm tra log Spring Boot và kết nối MySQL trước khi xử lý phía frontend.

## 5a. Gửi email xác thực bằng SMTP thật

Đăng ký, xác thực email và quên mật khẩu gửi mã OTP 6 số qua SMTP. Cấu hình trong `.env` (chạy backend với profile `dev` để file `.env` được nạp tự động):

| Biến | Ý nghĩa |
| --- | --- |
| `AUTH_OTP_HMAC_KEY` | Khóa ký OTP (base64, tối thiểu 32 byte). Tạo bằng `openssl rand -base64 32`. |
| `MAIL_HOST`, `MAIL_PORT` | Máy chủ SMTP. Gmail: `smtp.gmail.com`, `587`. |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Tài khoản SMTP. Gmail cần **Mật khẩu ứng dụng** 16 ký tự (bật xác minh 2 bước), viết liền không dấu cách. |
| `MAIL_FROM` | Địa chỉ gửi, với Gmail phải trùng `MAIL_USERNAME`. |
| `MAIL_SMTP_STARTTLS` / `MAIL_SMTP_SSL` | Cổng 587: `true` / `false`. Cổng 465: `false` / `true`. |

Khi khởi động, backend ghi log cho biết cấu hình email còn thiếu gì. Nếu gửi lỗi, log có dòng `SMTP delivery failed: ... — gợi ý` (không chứa mật khẩu). Muốn thử mà không gửi thật, dùng Mailpit trong `compose.yaml` (xem ghi chú trong `.env.example`).

Đổi mật khẩu khi đã đăng nhập: `POST /api/auth/change-password` (giao diện: Tài khoản > Bảo mật và liên kết). Sau khi đổi, các thiết bị khác bị đăng xuất.

## 6. Chạy frontend Vue

Mở một terminal khác và chuyển vào thư mục `frontend`:

```powershell
cd frontend
```

Nếu chưa cài dependencies, chạy:

```powershell
npm ci
```

Lệnh `npm ci` thích hợp khi repository có `package-lock.json` hợp lệ. Nếu dự án không có lockfile phù hợp, kiểm tra cấu hình và sử dụng `npm install` khi cần.

Khởi động giao diện:

```powershell
npm run dev
```

Sau khi chạy, Vite sẽ hiển thị URL truy cập trên terminal. Mở đúng URL được in ra thay vì giả định một cổng cố định.

Theo cấu hình hiện tại được mô tả trong dự án, Vite chuyển tiếp các request bắt đầu bằng `/api` sang Spring Boot trên cổng `8080`. Vì vậy, backend phải hoạt động thì các phần giao diện cần dữ liệu thật mới có thể hoạt động đúng.

## 7. Cấu trúc thư mục chính

```text
BonsaiMarket/
  frontend/
    public/                 Tài nguyên tĩnh của giao diện
    src/                    Mã nguồn Vue
    qa/                     Harness nghiệm thu MySQL/browser và đánh giá AI
    package.json            Dependencies và scripts frontend

  src/
    main/
      java/                 Mã nguồn Spring Boot
      resources/
        db/
          migration/        Các migration dùng cho schema
          dev/              Migration dữ liệu mẫu của môi trường dev
          reference/        SQL tham chiếu, không thay thế Flyway
    test/                   Mã nguồn kiểm thử Java

  compose.yaml              Cấu hình chạy dịch vụ bằng Docker Compose
  pom.xml                   Cấu hình Maven
  .env.example              Mẫu biến môi trường
  README.md                 Hướng dẫn dự án
```

Cây thư mục trên chỉ trình bày những khu vực chính, không phải danh sách tất cả file của repository.

## 8. Quản lý cơ sở dữ liệu bằng Flyway

Nguồn schema duy nhất: `src/main/resources/db/migration/V1__initial_schema.sql`, gồm 36 bảng từ toàn bộ source đã merge. SQL V1–V21 trước hợp nhất (kể cả hai cặp V18/V19 trùng version) được giữ nguyên byte trong `db/archive/pre-baseline-20261028`, ngoài classpath và Flyway locations.

Profile mặc định, `prod` và `production` chỉ chạy `classpath:db/migration`; `dev` thêm `classpath:db/dev`. Chỉ có một seed: `R__dev_seed.sql`. Flyway bật validate, tắt clean/baseline-on-migrate; Hibernate dùng `ddl-auto=validate`. Không có schema bootstrap độc lập; generator chỉ sinh manifest tham chiếu.

**Chỉ trỏ source mới vào database MySQL 8.4 mới và đã xác minh trống. Không nâng trực tiếp DB cũ bằng V1 mới, không sửa lịch sử/checksum, không repair/reset.** Làm theo [hướng dẫn dựng DB và chuyển môi trường](db/README.md). Khi baseline mới đã được áp dụng, thay đổi tiếp theo phải là migration mới từ V2.

## 9. Dữ liệu mẫu cho môi trường phát triển

Seed chỉ chèn mới theo khóa tự nhiên, không ghi đè dữ liệu đã sửa. Khi không có va chạm: 11 tài khoản, 6 danh mục, 6 gian hàng, 69 sản phẩm, 75 đơn/189 dòng hàng, giỏ/địa chỉ, review demo, Q&A và cộng đồng. Có một ví dụ COD đang chờ thu cùng ledger tồn, quote hết hạn và thông báo không gửi. Không có bằng chứng vận đơn, thanh toán hay đối soát thật trong seed.

Các tài khoản chính là `admin@bonsai.market` (ADMIN), `seller@bonsai.market` (SELLER), `user@bonsai.market` (CUSTOMER). Cả 11 tài khoản demo dùng mật khẩu **123456**, BCrypt cost 12 mặc định chỉ trong dev; tám tài khoản phụ đăng nhập được và giữ vai trò cũ. Danh sách đầy đủ, IntelliJ và phương án chuyển DB cũ có phê duyệt ở [db/README.md](db/README.md). Seed không reset tài khoản có sẵn. OTP/thông báo tới đúng 11 địa chỉ demo bị chặn trước SMTP vì chưa xác nhận quyền sở hữu domain. Production không được bật dev hoặc thêm location seed.

Kiểm tra tĩnh: `node db/build-reference.js` và `node db/check-dev-seed.js`. Đối chiếu dữ liệu bằng các truy vấn SELECT trong `db/verify_dev_seed.sql`, `db/verify_storefront.sql`. Chạy nghiệm thu MySQL/HTTP trên container dùng riêng bằng `node frontend/qa/run-checkout-mysql.js --acceptance`; harness kiểm tra schema rỗng và dọn tài nguyên task trong finally. Các kiểm thử cô lập không chứng minh database nhóm hoặc provider thật.

## 10. Build và kiểm thử

### 10.1. Kiểm thử backend

Tại thư mục gốc:

```powershell
.\mvnw.cmd test
```

Trên macOS/Linux:

```bash
./mvnw test
```

Nếu Maven Wrapper gặp lỗi môi trường nhưng máy có Maven phù hợp, có thể sử dụng `mvn test`.

Các bài kiểm thử backend có thể dùng H2, MockMvc hoặc đối tượng giả lập. Kết quả thành công không tự động chứng minh rằng kết nối MySQL hoặc các dịch vụ bên ngoài đã chạy thành công trong môi trường thật.

### 10.2. Build frontend

Tại thư mục `frontend`:

```powershell
npm run build
```

Lệnh này kiểm tra quá trình tạo phiên bản frontend để triển khai. Build thành công không thay thế cho việc kiểm tra từng thao tác trên trình duyệt.

### 10.3. Nghiệm thu MySQL và trình duyệt

Từ thư mục gốc, khi có Docker Desktop, Java 21, Maven, Node và Chrome:

```powershell
node frontend/qa/run-checkout-mysql.js --acceptance
```

Harness tạo MySQL riêng trên tmpfs, kiểm tra schema trống, chạy các test Java cùng hai browser runner rồi dọn container/network của lượt chạy. SMTP và hãng vận chuyển dùng doubles; kết quả không chứng minh provider hoặc database nhóm. Cấu hình Maven/Chrome và giới hạn được ghi tại [hướng dẫn QA](frontend/qa/README.md).

Các bộ UI fixture theo task Codex, runner cũ và ảnh/log lịch sử đã được loại bỏ khi dọn repository ngày 2026-10-28. `package.json` vẫn chỉ có dev/build/preview. Các test Java chính thức được giữ nguyên; kiểm tra responsive/bàn phím của UI cần thực hiện khi thay đổi giao diện. Kết quả lịch sử không thay thế kiểm thử hiện tại.

## 11. Một số lỗi thường gặp

### 11.1. Backend không kết nối được MySQL

Kiểm tra Docker đang chạy, container MySQL đã khởi động, cổng kết nối đúng và `DB_USERNAME`, `DB_PASSWORD`, `DB_URL` khớp với cấu hình cơ sở dữ liệu.

### 11.2. Frontend hiển thị nhưng không tải được sản phẩm

Kiểm tra backend trên cổng `8080`, request `/api` trong tab Network của trình duyệt và cấu hình proxy Vite. Nếu backend không chạy, frontend có thể hiển thị giao diện nhưng không nhận được dữ liệu thật.

### 11.3. Maven Wrapper không khởi chạy được

Kiểm tra JDK 21, quyền thực thi, Maven Wrapper và Maven cache. Có thể thử Maven cài trên máy nếu wrapper bị lỗi riêng về môi trường. Không tự cập nhật dependencies chỉ để che lỗi chưa rõ nguyên nhân.

### 11.4. Flyway báo lỗi migration hoặc checksum

Không xóa database và không chạy `repair` ngay. Trước tiên xác định migration nào bị ảnh hưởng, nội dung đã áp dụng và các thay đổi đang có trong Git. Các thành viên cần thống nhất cách xử lý nếu migration cũ từng chạy trên database của nhiều máy.

### 11.5. Đăng nhập không thành công với tài khoản mẫu

Kiểm tra profile `dev`, seed đã được áp dụng hay chưa, trạng thái tài khoản và các yêu cầu xác thực hiện tại. Không mặc định rằng có tài khoản mẫu trong tài liệu thì có thể đăng nhập thành công trong mọi database.

## 12. Lưu ý khi làm việc nhóm

- Trước khi sửa các phần dùng chung như router, security, API DTO hoặc migration, kiểm tra những module có thể bị ảnh hưởng.
- Không tự viết lại nghiệp vụ của thành viên khác khi chỉ cần sửa lớp tích hợp.
- Giữ các bài kiểm thử có giá trị để phát hiện lỗi sau mỗi lần merge.
- Không đưa `.env`, mật khẩu, khóa bí mật, token, log nhạy cảm hoặc dữ liệu người dùng thật lên repository.
- Kiểm tra `git status` và `git diff` trước khi commit hoặc tạo pull request.
- Sau khi merge code, nên chạy lại backend tests, frontend build và các luồng giao diện có liên quan.
