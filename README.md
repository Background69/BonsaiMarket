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

**Lưu ý về tiến độ:** Một số trang hoặc mã nguồn của giỏ hàng, thanh toán, lịch sử mua hàng và quy trình đặt hàng vẫn đang trong quá trình tích hợp. Việc có sẵn giao diện, bảng dữ liệu hoặc dữ liệu mẫu không có nghĩa là toàn bộ quy trình mua hàng đã hoàn chỉnh. Những chức năng này chưa được đưa đầy đủ vào điều hướng chính khi chưa kết nối xong.

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

## 4. Chuẩn bị cơ sở dữ liệu MySQL

Tại thư mục gốc của dự án, tạo file `.env` từ file mẫu:

```powershell
Copy-Item .env.example .env
```

Mở `.env` và điền những biến môi trường mà `compose.yaml` yêu cầu, đặc biệt là:

```dotenv
DB_PASSWORD=<mat_khau_database_cua_ban>
MYSQL_ROOT_PASSWORD=<mat_khau_root_cua_ban>
```

Các giá trị trên chỉ là chỗ giữ chỗ; không giữ nguyên dấu `<` và `>` khi điền cấu hình thật. Nên đặt hai mật khẩu đủ mạnh và không đưa file `.env` chứa mật khẩu lên GitHub.

Khởi động MySQL:

```powershell
docker compose up -d mysql
```

Kiểm tra container:

```powershell
docker compose ps
```

Nếu MySQL khởi động không thành công, xem log:

```powershell
docker compose logs mysql
```

Theo cấu hình mặc định được mô tả trong dự án, địa chỉ kết nối backend là:

```text
jdbc:mysql://localhost:3306/bonsai_market
```

Nếu bạn đổi cổng hoặc chạy MySQL ở máy khác, phải cấu hình lại `DB_URL` cho phù hợp.

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
DB_USERNAME=bonsai
DB_PASSWORD=<mat_khau_trong_file_env>
SPRING_PROFILES_ACTIVE=dev
```

Có thể đặt thêm `DB_URL` nếu không dùng kết nối MySQL mặc định.

**Quan trọng:** IntelliJ IDEA không tự động đọc file `.env` của Docker Compose. Mật khẩu mà backend nhận được phải khớp với tài khoản MySQL do Docker Compose tạo ra.

### 5.2. Chạy từ PowerShell

Nếu không dùng IntelliJ IDEA, mở PowerShell tại thư mục gốc:

```powershell
$env:DB_USERNAME = 'bonsai'
$env:DB_PASSWORD = '<mat_khau_trong_file_env>'
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
    tests/                  Một số bộ kiểm thử giao diện
    qa/                     Các bộ kiểm thử và dữ liệu giả lập bổ sung
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

BonsaiMarket sử dụng Flyway để tạo và cập nhật cấu trúc MySQL. Các file migration được áp dụng theo phiên bản và Flyway ghi nhận những phiên bản đã chạy trong database.

### 8.1. Migration cho cấu trúc dữ liệu

Profile mặc định đọc migration trong:

```text
src/main/resources/db/migration/
```

Source hiện tại có các migration schema V1–V8, V10–V13, V15–V16 và V18–V21. V18 bổ sung purchase reviews/replies; V19 bổ sung Shipping và vị trí Store công khai; V20 bổ sung Checkout/COD; V21 bổ sung notification outbox, returns và báo cáo COD. Đây là version trong source, không chứng minh database của nhóm đã áp dụng chúng.

### 8.2. Migration cho dữ liệu mẫu

Khi dùng profile `dev`, ứng dụng đọc thêm:

```text
src/main/resources/db/dev/
```

Tài liệu hiện tại liệt kê các migration dữ liệu mẫu V9, V14 và V17.

### 8.3. Những quy tắc cần tuân thủ

- Không sửa tên hoặc nội dung của migration đã áp dụng trên database.
- Khi cần thay đổi schema, tạo migration mới thay vì sửa migration cũ.
- Không chạy `flyway clean`, `repair` hoặc `baseline` chỉ để bỏ qua lỗi khi chưa xác minh nguyên nhân.
- Không xóa dữ liệu hay lịch sử Flyway khi chưa có kế hoạch và bản sao lưu phù hợp.
- Không xem SQL tổng hợp trong `db/reference` là file thay thế cho các migration đang dùng.

Hibernate đang sử dụng `ddl-auto: validate`, nghĩa là cấu trúc database được kiểm tra với mapping entity, chứ không được tự động tạo hoặc sửa bởi Hibernate.

Lưu ý rằng tên file SQL trong source không chứng minh các phiên bản tương ứng đã được áp dụng trên MySQL của từng thành viên. Muốn biết trạng thái thực tế, phải kiểm tra database và lịch sử Flyway trong môi trường đó.

## 9. Dữ liệu mẫu cho môi trường phát triển

**Dữ liệu trong profile `dev` chỉ dùng cho phát triển và kiểm thử. Không sử dụng tài khoản mẫu hoặc mật khẩu mẫu trên môi trường production.**

Bộ seed ban đầu V9 có các tài khoản:

| Vai trò | Email | Mật khẩu mẫu |
| --- | --- | --- |
| Quản trị viên (ADMIN) | `admin@bonsaimarket.local` | `Password123!` |
| Người bán (SELLER) | `seller@bonsaimarket.local` | `Password123!` |
| Khách hàng (CUSTOMER) | `user@bonsaimarket.local` | `Password123!` |

Mật khẩu được lưu trong database dưới dạng BCrypt, không lưu trực tiếp ở dạng văn bản thuần.

Theo tài liệu của seed ban đầu, V9 tạo 5 danh mục, 1 gian hàng, 20 sản phẩm, 1 giỏ hàng có 3 mặt hàng, 75 đơn hàng với 189 dòng hàng, 12 đánh giá và 4 mục yêu thích. Các migration seed tiếp theo có thể thêm dữ liệu gian hàng, sản phẩm và nội dung mẫu, nên tổng số bản ghi thực tế có thể lớn hơn số liệu của V9.

Các đơn hàng mẫu dùng để kiểm thử dữ liệu không phải bằng chứng cho thấy quy trình đặt hàng hoặc thanh toán thực tế đã hoàn thiện. Khả năng đăng nhập bằng tài khoản mẫu còn phụ thuộc trạng thái tài khoản và xác thực được lưu trong database.

Để kiểm tra logic tính toán dữ liệu seed mà không cần MySQL:

```powershell
node db/check-dev-seed.js
```

Để đối chiếu số lượng bản ghi và tổng tiền sau khi khởi động MySQL, tham khảo các câu truy vấn trong:

```text
db/verify_dev_seed.sql
```

Chỉ chạy các truy vấn phù hợp và đọc kỹ nội dung SQL trước khi thực thi trên database có dữ liệu cần bảo toàn.

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

### 10.3. Kiểm thử giao diện

Tài liệu dự án liệt kê các nhóm kiểm thử như điều hướng, quản lý, danh mục và xác thực. Trước khi chạy, kiểm tra các script hiện có trong `frontend/package.json` vì danh sách lệnh có thể thay đổi trong quá trình nhóm cập nhật repository.

Ví dụ, nếu các script tương ứng vẫn được khai báo:

```powershell
npm run test:navigation
npm run test:management
npm run test:catalog
npm run test:auth
```

Các kiểm thử trình duyệt có thể sử dụng dữ liệu giả lập và chặn các yêu cầu `/api`, nên kết quả của chúng không phải là xác nhận rằng API hoặc MySQL thực đang hoạt động. Xem `frontend/qa/README.md` để biết lệnh kiểm thử và cách đặt biến `UI_TEST_ORIGIN`, `CHROME_PATH` khi cần.

Artifacts, ảnh chụp và log được sinh ra trong quá trình kiểm thử nên được quản lý riêng; không cần mặc định đưa toàn bộ file kết quả lên Git.

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
