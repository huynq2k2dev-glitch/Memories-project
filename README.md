# Memories Platform

Foundation cho nền tảng tạo thiệp online và lưu giữ kỷ niệm cá nhân. Repository gồm:

- `backend/`: Java 21, Spring Boot 3.5, PostgreSQL, Liquibase và JPA/Hibernate.
- `frontend/`: Next.js 16 App Router, React 19 và TypeScript.
- `compose.yml`: PostgreSQL, Mailpit, MinIO cùng môi trường chạy/build Java 21 và frontend.

## Chạy toàn bộ nền tảng

Yêu cầu Docker Desktop đang hoạt động.

```powershell
docker compose up --build
```

Sau khi các service khởi động:

- Frontend: `http://localhost:3000`
- Backend health: `http://localhost:8080/api/v1/health`
- PostgreSQL: `localhost:5432`
- Mailpit: `http://localhost:8025`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`

Các port có thể đổi bằng `FRONTEND_PORT`, `BACKEND_PORT`, `POSTGRES_PORT`, `MAILPIT_SMTP_PORT`, `MAILPIT_UI_PORT`, `MINIO_API_PORT` và `MINIO_CONSOLE_PORT`. Database credentials và URL của backend lấy từ `DB_URL`, `DB_USERNAME` và `DB_PASSWORD`. SMTP generic lấy từ `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS` và `MAIL_FROM`. JWT HS256 lấy khóa Base64 tối thiểu 256 bit từ `ACCESS_TOKEN_SECRET`; giá trị mặc định trong Compose chỉ dành cho local development, không dùng hoặc commit secret thật của môi trường triển khai.

Đăng ký tài khoản qua `POST /api/v1/auth/register`, xác thực một lần qua `POST /api/v1/auth/email-verifications/confirm` và yêu cầu gửi lại qua `POST /api/v1/auth/email-verifications/resend`. Link email đặt token trong URL fragment (`/verify-email#token=...`) để token không đi vào access log. Môi trường local gửi email vào Mailpit; môi trường triển khai có thể trỏ cùng cấu hình SMTP sang nhà cung cấp phù hợp.

Đăng nhập qua `POST /api/v1/auth/login` hoặc trang `/login`. Access token có TTL 15 phút, được truyền bằng `Authorization: Bearer` và frontend chỉ giữ trong memory. Tài khoản bị khóa tạm thời 15 phút sau 5 lần sai liên tiếp; đăng nhập thành công hoặc hết thời gian khóa sẽ đặt lại bộ đếm phù hợp.

Phiên đăng nhập dùng refresh cookie `HttpOnly`, `SameSite=Strict`, có family lifetime tuyệt đối 30 ngày và được rotation qua `POST /api/v1/auth/refresh`. Token cũ chỉ được lưu dưới dạng SHA-256; reuse thu hồi toàn bộ family. `POST /api/v1/auth/logout` kết thúc phiên hiện tại, còn `POST /api/v1/auth/logout-all` yêu cầu Bearer token và thu hồi mọi phiên của người dùng. Đặt `REFRESH_COOKIE_SECURE=true` ở môi trường HTTPS ngoài local.

Quản trị viên có quyền `USER_MANAGE` khóa tài khoản qua `POST /api/v1/admin/users/{userId}/lock`. Thao tác là idempotent, chuyển tài khoản sang `LOCKED`, thu hồi toàn bộ refresh token và khiến access token hiện có bị từ chối ngay ở request bảo vệ tiếp theo. Ticket hiện tại chưa định nghĩa thao tác mở khóa nên API không cung cấp chức năng đó.

Quản trị template tại `/admin/templates` và nhóm API `/api/v1/admin/templates` yêu cầu permission `TEMPLATE_MANAGE`. Code template là bất biến; version number được server tăng tuần tự. Version chỉ đi qua `DRAFT → PUBLISHED → DEPRECATED`, và hợp đồng đã publish không có API sửa hoặc xóa. Khi publish, backend kiểm tra default config bằng JSON Schema Draft 2020-12 và chỉ chấp nhận renderer `memories-basic-v1@1` đã đăng ký trong frontend build. Mỗi version có cờ `coverRequired` tường minh để bước publish memory không phải suy đoán từ section type. Registry cũ phải được giữ lại khi còn memory ghim vào version tương ứng.

Ứng dụng không tự tạo hoặc tự cấp quyền ADMIN. Tài khoản quản trị phải được provision bằng quy trình vận hành riêng; cấu hình local cũng không seed mật khẩu quản trị mặc định.

Người dùng đã đăng nhập duyệt catalog tại `/templates` hoặc `GET /api/v1/templates`. API dùng `page` bắt đầu từ 0, `size` từ 1 đến 50, cùng bộ lọc tùy chọn `memoryType` và `status`. Catalog người dùng chỉ trả template `ACTIVE` có ít nhất một version `PUBLISHED`; payload render chỉ gồm default config và required sections, không trả config schema quản trị. UI tra `component_key` và `renderer_version` trong registry nội bộ, đồng thời hiển thị lỗi tương thích thay vì tải hoặc thực thi code từ database.

Từ catalog, người dùng tạo memory qua `POST /api/v1/memories` và đọc chi tiết quản trị thuộc sở hữu của mình qua `GET /api/v1/memories/{memoryId}`. Memory mới luôn ở `DRAFT`/`PRIVATE`, ghim trực tiếp version template đã chọn và sao chép default config làm theme ban đầu. Slug gồm phần URL-safe từ tiêu đề cùng UUID memory, nên không có race khi hai người tạo cùng tiêu đề. API đọc lọc đồng thời theo memory ID và actor hiện tại để không lộ sự tồn tại của memory người khác.

Owner cập nhật metadata và theme của memory nháp qua `PUT /api/v1/memories/{memoryId}`. Request phải gửi `version` hiện hành để phát hiện ghi đồng thời, theme phải thỏa JSON Schema của template version đã ghim, visibility hiện chỉ nhận `PRIVATE`, `UNLISTED` hoặc `PUBLIC`, và summary chỉ nhận plain text/Markdown không chứa raw HTML hay protocol nguy hiểm. `settings` vẫn là object rỗng không cập nhật ở ticket này; quy tắc publish của collaborator được dành cho ticket cộng tác.

Hợp đồng template version khai báo `sectionContracts`, ánh xạ từng section type được phép tới JSON Schema của config; `requiredSections` phải là tập con của các type đó. Owner quản lý nhân vật qua `/api/v1/memories/{memoryId}/members` và section qua `/api/v1/memories/{memoryId}/sections`. Mỗi child dùng optimistic version; endpoint `PUT .../order` nhận toàn bộ `orderedIds` cùng version để sắp xếp nguyên tử. Section bắt buộc vẫn được lưu khi draft chưa hoàn chỉnh, nhưng response đánh dấu `contentComplete=false` nếu bị ẩn hoặc không có content/config để bước publish sau này từ chối.

Địa điểm và sự kiện được quản lý lần lượt qua `/api/v1/memories/{memoryId}/locations` và `/api/v1/memories/{memoryId}/events`, cùng quy ước optimistic version và batch order. Tọa độ phải cùng có hoặc cùng null; map URL chỉ chấp nhận HTTPS Google Maps/OpenStreetMap đã allowlist. Event lưu `startAt`/`endAt` dưới dạng UTC `Instant`, giữ timezone `ZoneId` riêng để hiển thị và không cho end trước start. Event chỉ nhận location cùng memory; xóa location sẽ đặt `locationId` liên quan thành null. Frontend tải một danh sách location và một danh sách event rồi nối bằng ID, không truy vấn location theo từng event.

Ảnh được upload trực tiếp từ frontend lên object storage S3-compatible. Owner xin URL PUT 10 phút qua `/api/v1/memories/{memoryId}/media/uploads`, gửi file thẳng tới storage rồi gọi `/api/v1/media/{assetId}/complete`; backend xác minh size, content type, magic bytes và checksum SHA-256 khi provider trả về trước khi chuyển asset sang `READY`. MIME cho phép là JPEG, PNG, WebP và AVIF, tối đa 10 MiB/file; quota mỗi owner là 200 asset hoặc 1 GiB, tính cả `UPLOADING` và `READY`. Ảnh READY có thể gắn vào memory/section, làm cover hoặc avatar; bỏ liên kết không xóa object. Xóa asset là soft delete và bị từ chối khi asset còn được tham chiếu. Compose dùng MinIO local với CORS giới hạn cho frontend; production có thể đổi endpoint/provider/credentials qua nhóm biến `MEDIA_STORAGE_*` mà không đổi API ứng dụng.

Owner lấy payload preview qua `GET /api/v1/memories/{memoryId}/preview` và publish qua `POST /api/v1/memories/{memoryId}/publish` với optimistic `version`. Ticket này chỉ publish `PUBLIC` hoặc `UNLISTED`; backend kiểm tra template/version còn selectable, theme/section config, section bắt buộc, cover theo contract, hạn truy cập và toàn bộ asset còn `READY`. Public payload được đọc qua `GET /api/v1/public/memories/{slug}` hoặc trang `/memories/{slug}`; draft, archived, deleted và expired đều trả not found. `PUBLIC` và `UNLISTED` có cùng quyền truy cập trực tiếp bằng slug, còn discovery/search chưa được triển khai. Preview và public dùng chung payload cùng frontend registry. Payload public không chứa owner, settings, location note hoặc storage credential; URL ảnh tương đối đi qua endpoint kiểm tra lại memory còn public trước khi redirect ngắn hạn tới object storage. Mọi lần publish thành công, thất bại hoặc bị từ chối được ghi vào `audit_logs` append-only cùng correlation ID.

## Kiểm tra backend

Máy phát triển không bắt buộc cài JDK 21 vì backend được build trong Docker:

```powershell
docker compose up -d --build backend
curl.exe --fail http://localhost:8080/api/v1/health
```

Backend dùng PostgreSQL thật do Compose cung cấp. Liquibase cập nhật schema trước, sau đó Hibernate chạy `ddl-auto=validate`; H2 không được sử dụng.

## Kiểm tra frontend

Node.js 20.9+ là bắt buộc; repository hiện được xác minh với Node.js 22.

```powershell
cd frontend
npm install
npm run typecheck
npm run lint
npm run build
```

## Quy ước nền tảng

- Liquibase changelog là nguồn sự thật duy nhất của schema.
- Thời gian tuyệt đối dùng UTC ở persistence và Java `Instant` trong domain code.
- REST trả DTO, không trả JPA entity trực tiếp.
- Request/response dùng `X-Correlation-Id`; lỗi API dùng Problem Details và không chứa credential hoặc token.
- Module nghiệp vụ đặt trực tiếp dưới `com.memories.platform.<module>` (ví dụ `com.memories.platform.auth`) và gom `controller`, `dto`, `service`, `repository`, `entity`, `constants`, `exception` theo trách nhiệm thực tế.
- `config`, `utils` và `common` là các package dùng chung nằm ngoài feature module; không tạo tầng hoặc package rỗng chỉ để đủ cấu trúc.
