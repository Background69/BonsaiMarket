# Thiết lập trợ lý AI BonsaiMarket

Chatbot sử dụng Vue → Spring Boot → Google Gemini API, với kho kiến thức có nguồn trong classpath và catalog công khai MySQL. **Chỉ dùng Gemini**, không có provider selector hay fallback. Mặc định `AI_ENABLED=false`. Không cần cài package hoặc SDK mới. Chat chỉ nhận văn bản; không lưu hội thoại vào database. Kiến trúc và kết quả mới: [src/main/resources/ai/README.md](src/main/resources/ai/README.md).

## 1. Google AI Studio và model

Tạo API key trong project của bạn tại [Google AI Studio](https://aistudio.google.com/apikey). Chọn model Gemini mà tài khoản có quyền truy cập, hỗ trợ **generateContent, văn bản và structured output với JSON schema**. Điền bare model ID dạng `gemini-...` vào `GEMINI_MODEL`, không thêm `models/`, URL hoặc query string. Module không có model mặc định; mẫu `your_supported_model_id` bên dưới phải được thay bằng ID hợp lệ của bạn. Tra [danh sách model Gemini](https://ai.google.dev/gemini-api/docs/models) và kiểm tra quyền truy cập trong project API của bạn. Không còn cần OpenAI key; không cần Groq key hay biến `AI_PROVIDER`.

Mọi câu hỏi dùng structured output để AI chỉ chọn ID; Spring dựng tên, giá, tồn kho, gian hàng và đoạn kiến thức từ kết quả truy xuất. Câu chăm sóc cũng dùng RAG dạng trích xuất, không chuyển văn bản tự do từ model ra người dùng. Tham chiếu [structured output](https://ai.google.dev/gemini-api/docs/generate-content/structured-output?hl=en) và [generateContent REST reference](https://ai.google.dev/api/generate-content). Implementation giữ `generationConfig.responseFormat.text` với `mimeType=application/json` và `schema`. Model không hỗ trợ cấu hình này trả lỗi an toàn; không chuyển sang prose hay provider khác. Không đặt model mặc định hoặc tự đổi model của người vận hành.

Gemini có quota và có thể phát sinh chi phí tùy gói/model. Theo dõi usage và billing trong project của bạn; đọc chính sách dữ liệu phù hợp với gói sử dụng. Module không tự retry, không gọi provider khi Spring khởi động và không gọi provider trong automated tests. Key chỉ nằm trong Spring environment và được gửi bằng header `x-goog-api-key`, không ở URL, Vue hoặc biến Vite.

## 2. MySQL và Spring trên Windows/IntelliJ IDEA

1. Sao chép `.env.example` thành `.env` nếu chưa có và đặt mật khẩu database riêng; không commit `.env`. Không ghi API key vào source hoặc Vue.
2. Mở Docker Desktop và chờ engine sẵn sàng. Chạy `docker compose up -d mysql` ở thư mục gốc.
3. IntelliJ: **Run → Edit Configurations → BonsaiMarketApplication → Modify options → Environment variables**. Đặt các biến sau trong môi trường của **Spring**:

   ```text
   DB_USERNAME=bonsai
   DB_PASSWORD=<mật khẩu database của bạn>
   SPRING_PROFILES_ACTIVE=dev
   AI_ENABLED=true
   GEMINI_API_KEY=your_private_key
   GEMINI_MODEL=your_supported_model_id
   ```

   Nếu cần, đặt `DB_URL`. Không chia sẻ Run Configuration chứa key, ảnh màn hình hoặc log môi trường. File `.env` không tự thành biến môi trường IntelliJ. Tuy nhiên profile `dev` hiện import `.env` qua `optional:file:.env[.properties]` tại thư mục làm việc; profile khác cần cấu hình riêng. Không in hoặc chia sẻ giá trị khóa.
4. Chọn JDK 21 rồi tự chạy `BonsaiMarketApplication` trong IntelliJ. Agent không tự chạy Spring trên 8080.
5. Kiểm tra `http://localhost:8080/actuator/health`, `/api/products`, `/api/stores` và Flyway/Hibernate trong log khởi động. Kiểm tra migrations hiện tại và schema history của môi trường dự định dùng; không suy rằng V1–V9 là toàn bộ schema nữa. Cải tiến AI này không thêm/sửa migration hoặc seed. Không repair/reset MySQL.
6. Terminal thứ hai: `cd frontend`, `npm run dev` với dependencies đã có. Không chạy lệnh cài thư viện.
7. Mở URL Vite (thường `http://localhost:5173`), bấm nút trợ lý nổi. Vite proxy `/api` tới Spring 8080. Production cần cấu hình reverse proxy cùng origin để `/api` tới Spring.
8. Hỏi `Cách tưới bonsai`, rồi `Cây bonsai dưới 500.000đ` và `Sản phẩm #1 còn hàng không?`. Mỗi câu hỏi độc lập; dùng tên/mã cụ thể khi hỏi sản phẩm. FAQ và nguồn xuất hiện trong UI; không có ảnh upload.

## 3. Contract và lỗi

```http
POST /api/ai/chat
Content-Type: application/json

{"message":"Có bonsai nào dưới 500.000 đồng?"}
```

Thành công: `{"answer":"...","sources":[{"type":"PRODUCT","id":1,"name":"..."}]}`. Lỗi an toàn: `{"code":"AI_NOT_CONFIGURED","error":"Trợ lý AI hiện chưa sẵn sàng: thiếu cấu hình máy chủ."}`.

| HTTP | Trường hợp |
|---|---|
| 400 | Message thiếu/rỗng/quá 2.000 ký tự, JSON lỗi |
| 413 | Body vượt 16 KiB, kể cả không khai báo Content-Length |
| 429 | Quá hạn mức module hoặc provider; có Retry-After |
| 503 | AI tắt, thiếu key/model, provider từ chối cấu hình/quyền, database chưa sẵn sàng |
| 504 | Provider timeout |
| 502 | Lỗi mạng/provider, response rỗng/lỗi/chưa hoàn tất |
| 422 | Provider từ chối hỗ trợ câu hỏi |

Thiếu cấu hình AI không làm ứng dụng crash và không truy xuất DB cho câu hỏi AI. Spring vẫn cần MySQL để khởi động ứng dụng bình thường do kiến trúc JPA/Flyway hiện tại. Unit tests loại database auto configuration, không chứng minh khởi động live.

## 4. Hạn mức và triển khai

Message được trim và giới hạn 2.000 ký tự; body 16 KiB; mỗi peer socket tối đa 10 lượt/60 giây; toàn instance 60 lượt/60 giây; tối đa 4 lượt đồng thời. Không tin X-Forwarded-For/X-Real-IP. Khi đứng sau proxy, nhiều người có thể chung một peer và chung quota. Map peer giới hạn 4.096 phần tử.

Provider: connect timeout 5 giây, request timeout 25 giây; frontend timeout 35 giây. Context tối đa 16.000 ký tự, tối đa 10 sản phẩm, 10 gian hàng, 4 đoạn kiến thức; output tối đa 1.200 tokens (gồm reasoning nếu model dùng). Envelope provider giới hạn 128 KiB ngay lúc tải. Không tools, không SQL do model sinh, không retry tự động. Endpoint POST công khai chỉ nhận JSON, không cần cookie; CSRF chỉ được bỏ qua đúng POST này, các rule Security khác vẫn hoạt động. `GET /api/ai/knowledge?q=...` chỉ tìm trong kho kiến thức công khai, không gọi provider hoặc truy vấn DB.

Đây là bảo vệ demo **single-instance**. Trước khi mở Internet production, cần rate limit/body limit/timeouts ở reverse proxy, hạn mức phân tán nếu chạy nhiều instance, monitoring usage và đánh giá chất lượng/prompt injection. Không tăng quota tùy tiện. `AI_ENABLED=false` giúp giữ module tắt khi chưa sẵn sàng.

Kho kiến thức là bản tham khảo chung gồm 25 mục với nguồn/ngày kiểm tra/aliases, **cần người có kiến thức cây cảnh kiểm duyệt**. Quy trình cập nhật: [src/main/resources/ai/README.md](src/main/resources/ai/README.md). Phải đánh giá với model thực tế trước khi coi là hướng dẫn chính thức. Không gửi thông tin cá nhân vào ô chat; message người dùng được gửi tới provider. Backend chỉ gửi các field catalog công khai cần thiết, không gửi dữ liệu user/order/address/password hay mô tả sản phẩm. Tra cứu gian hàng dùng lại public directory và quyền hiển thị của service này.

## 5. Kiểm thử offline và thực tế

```text
mvn test
cd frontend
npm run build
```

Kiểm tra frontend bằng `npm run build` trong `frontend`; kiểm tra logic AI bằng các test Java trong `src/test`. Các bộ browser fixture AI/catalog cũ đã được loại bỏ ngày 2026-10-28. Khi sửa giao diện chatbot, cần kiểm tra thủ công responsive, bàn phím, loading/error/retry và nguồn trích dẫn; build không thay thế kiểm tra trình duyệt.

Kiểm tra thực tế: MySQL khỏe; Flyway history khớp migrations hiện tại; Hibernate validate; health UP; GET catalog có row thực; POST chatbot có phản hồi từ Gemini; nguồn khớp ID/giá/tồn kho lúc truy xuất. Khi backend/model đã cấu hình, chạy bộ 50 câu bằng `node frontend/qa/evaluate-live.js --allow-provider-calls --phase after`. Script có thể phát sinh tối đa 50 lượt API, tạm dừng khi lỗi cấu hình/quota và lưu JSON cần người review. Không thể dựng lại baseline Gemini live nếu chưa đo trên bản cũ. Môi trường chưa kiểm chứng phải ghi **LIVE MYSQL TEST: NOT VERIFIED** và **LIVE GEMINI API TEST: NOT VERIFIED**. Không truy tìm key ở nơi khác.
