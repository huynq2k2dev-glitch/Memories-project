# Memories Platform

Memories Platform là nền tảng tạo thiệp và không gian kỷ niệm trực tuyến. Người dùng có thể đăng ký tài khoản, chọn mẫu, biên soạn nội dung, tải ảnh, mời cộng tác viên, quản lý khách mời và xuất bản memory để chia sẻ.

## 1. Khởi động nhanh

### Yêu cầu

- Docker Desktop đang hoạt động.
- Các cổng `3000`, `8080`, `5432`, `8025`, `9000` và `9001` chưa bị ứng dụng khác sử dụng.

Tại thư mục gốc của dự án, chạy:

```powershell
docker compose up --build
```

Lần chạy đầu tiên có thể mất vài phút vì Docker phải tải image và dependency. Khi các service đã sẵn sàng, mở các địa chỉ sau:

| Thành phần | Địa chỉ | Mục đích |
| --- | --- | --- |
| Ứng dụng | http://localhost:3000 | Giao diện chính |
| Backend health | http://localhost:8080/api/v1/health | Kiểm tra backend |
| Mailpit | http://localhost:8025 | Đọc email xác thực ở môi trường local |
| MinIO Console | http://localhost:9001 | Xem object storage local |
| MinIO API | http://localhost:9000 | Endpoint lưu trữ ảnh |

Tài khoản MinIO mặc định dành riêng cho local:

- Username: `minioadmin`
- Password: `minioadmin`

## 2. Đăng ký và đăng nhập

### Đăng ký tài khoản

1. Mở http://localhost:3000/register.
2. Nhập tên hiển thị, email và mật khẩu.
3. Mật khẩu phải có từ 12 đến 72 ký tự.
4. Nhập lại mật khẩu và chọn **Đăng ký**.
5. Ứng dụng sẽ thông báo kiểm tra hộp thư.

### Xác thực email ở môi trường local

1. Mở Mailpit tại http://localhost:8025.
2. Chọn email vừa được gửi tới địa chỉ đăng ký.
3. Mở liên kết xác thực trong email.
4. Sau khi xác thực thành công, ứng dụng chuyển về trang đăng nhập.

Nếu liên kết đã hết hạn hoặc không hợp lệ, trang xác thực cho phép nhập email để yêu cầu gửi lại liên kết.

### Đăng nhập

1. Mở http://localhost:3000/login.
2. Nhập email đã xác thực và mật khẩu.
3. Chọn **Đăng nhập**.
4. Sau khi đăng nhập, ứng dụng chuyển tới trang **Memory của tôi** hoặc trang bảo vệ mà người dùng đang muốn truy cập.

Phiên đăng nhập được tự động khôi phục bằng refresh cookie. Để kết thúc phiên, mở menu tài khoản trên thanh điều hướng và chọn **Đăng xuất**.

## 3. Quản lý memory

### Xem danh sách memory

Mở http://localhost:3000/memories hoặc chọn **Memory của tôi** trên thanh điều hướng.

Danh sách chỉ hiển thị memory thuộc tài khoản đang đăng nhập, sắp xếp theo lần cập nhật mới nhất. Mỗi thẻ hiển thị ảnh bìa, loại memory, trạng thái và thời điểm cập nhật.

### Tạo memory mới

1. Từ thanh điều hướng hoặc dashboard, chọn **Tạo memory**.
2. Chọn loại kỷ niệm, ví dụ **Kỷ niệm cá nhân**.
3. Chọn template và xem trước giao diện.
4. Đặt tiêu đề cho memory.
5. Chọn **Tạo memory**.

Memory mới được tạo ở trạng thái `DRAFT` và tự động mở tại địa chỉ `/memories/{id}/edit`.

Compose chạy backend với Liquibase context `dev`, vì vậy môi trường local có sẵn template **Nhật ký kỷ niệm** để thử luồng tạo memory.

### Chỉnh sửa memory

Từ dashboard, chọn **Chỉnh sửa** trên memory cần cập nhật. Editor được chia thành các nhóm chức năng:

- **Cập nhật draft**: sửa tiêu đề, mô tả, thời gian, giao diện và chính sách truy cập.
- **Nhân vật và section**: thêm nhân vật, nội dung câu chuyện và sắp xếp section.
- **Địa điểm và sự kiện**: quản lý địa điểm, lịch sự kiện và thông tin RSVP.
- **Ảnh memory**: tải ảnh, gắn ảnh vào memory, chọn ảnh bìa và avatar nhân vật.
- **Cộng tác viên**: mời người khác cùng xem hoặc biên soạn.
- **Khách mời**: tạo danh sách khách và phát hành liên kết mời cá nhân.
- **Link chia sẻ**: phát hành hoặc thu hồi link truy cập có giới hạn.
- **Kiểm duyệt lời chúc**: duyệt, từ chối hoặc ẩn lời chúc của khách.
- **Preview và publish**: xem trước rồi xuất bản memory.
- **Vòng đời memory**: archive hoặc xóa mềm memory khi có quyền.

Một số khu vực chỉ xuất hiện khi tài khoản có quyền phù hợp hoặc memory đang ở trạng thái cho phép.

### Quyền cộng tác viên

| Quyền | Khả năng chính |
| --- | --- |
| `VIEW` | Xem dữ liệu quản trị của memory |
| `EDIT` | Xem và chỉnh sửa nội dung draft |
| `ADMIN` | Chỉnh sửa, publish và quản lý cộng tác viên |
| Owner | Toàn quyền, bao gồm chính sách truy cập, archive và xóa memory |

Tài khoản được mời làm cộng tác viên phải tồn tại và đã xác thực email.

## 4. Xuất bản và chia sẻ

### Trạng thái memory

- `DRAFT`: đang biên soạn, chưa xuất hiện công khai.
- `PUBLISHED`: đã xuất bản và có thể truy cập theo chính sách đã chọn.
- `ARCHIVED`: đã lưu trữ, không còn truy cập công khai.

Trước khi publish, backend kiểm tra template, cấu hình giao diện, section bắt buộc, ảnh bìa và trạng thái các file đã tải lên. Editor sẽ hiển thị lỗi cụ thể nếu memory chưa đủ điều kiện.

### Chính sách truy cập

- `PUBLIC`: bất kỳ ai có URL đều có thể xem.
- `UNLISTED`: không niêm yết công khai nhưng người có URL vẫn truy cập được.
- `PASSWORD_PROTECTED`: khách phải nhập đúng mật khẩu trước khi xem.
- `PRIVATE`: chỉ owner, cộng tác viên hoặc người có quyền được cấp riêng mới truy cập.

Memory đã publish được mở bằng URL `/memories/{slug}`.

### Khách mời và link chia sẻ

- Link khách mời cá nhân mở tại `/invitations/{token}` và có thể dùng để phản hồi RSVP.
- Link chia sẻ mở tại `/shares/{token}`.
- Token gốc chỉ hiển thị khi phát hành. Hãy sao chép và lưu lại trước khi rời màn hình.
- Thu hồi link hoặc vô hiệu hóa khách sẽ làm link cũ mất hiệu lực.

### Lời chúc

Khách có thể gửi lời chúc trên memory đã publish khi chính sách truy cập cho phép. Khi kiểm duyệt đang bật, lời chúc mới ở trạng thái chờ và chỉ xuất hiện công khai sau khi được duyệt.

## 5. Quản trị template

Trang quản trị template nằm tại http://localhost:3000/admin/templates.

Tài khoản cần có permission `TEMPLATE_MANAGE`. Dự án không tự tạo tài khoản admin và không seed mật khẩu quản trị mặc định; quyền quản trị phải được cấp bằng quy trình vận hành riêng.

Quản trị viên có thể:

- Tạo template và version draft.
- Cấu hình renderer, JSON Schema, section bắt buộc và ảnh bìa.
- Publish hoặc deprecate version.
- Kích hoạt hoặc ngừng sử dụng template trong catalog.

## 6. Dữ liệu và dịch vụ local

### PostgreSQL

PostgreSQL lưu tài khoản, phiên đăng nhập, template, memory và các dữ liệu nghiệp vụ. Liquibase tự cập nhật schema khi backend khởi động.

### Mailpit

Mọi email local được giữ trong Mailpit thay vì gửi ra Internet. Dùng Mailpit để lấy link xác thực tài khoản.

### MinIO

Ảnh được tải trực tiếp lên MinIO. Bucket `memories` được tạo tự động bởi service `minio-init`.

### Lưu dữ liệu khi dừng ứng dụng

Lệnh sau dừng và gỡ container nhưng vẫn giữ named volume:

```powershell
docker compose down
```

Không thêm tùy chọn `-v` nếu muốn giữ database và ảnh đã tải lên.

## 7. Lệnh vận hành thường dùng

Xem trạng thái service:

```powershell
docker compose ps
```

Xem log backend và frontend:

```powershell
docker compose logs -f backend frontend
```

Khởi động lại một service:

```powershell
docker compose restart backend
```

Build lại ứng dụng sau khi thay đổi mã nguồn:

```powershell
docker compose up -d --build backend frontend
```

## 8. Cấu hình bằng biến môi trường

Có thể tạo file `.env` tại thư mục gốc để đổi port hoặc credential local. Các biến thường dùng:

| Biến | Mặc định trong Compose | Ý nghĩa |
| --- | --- | --- |
| `FRONTEND_PORT` | `3000` | Cổng giao diện |
| `BACKEND_PORT` | `8080` | Cổng backend |
| `POSTGRES_PORT` | `5432` | Cổng PostgreSQL |
| `MAILPIT_UI_PORT` | `8025` | Cổng giao diện Mailpit |
| `MINIO_API_PORT` | `9000` | Cổng MinIO API |
| `MINIO_CONSOLE_PORT` | `9001` | Cổng MinIO Console |
| `MINIO_ROOT_USER` | `minioadmin` | Username MinIO local |
| `MINIO_ROOT_PASSWORD` | `minioadmin` | Password MinIO local |
| `ACCESS_TOKEN_SECRET` | Giá trị local có sẵn | Khóa JWT dạng Base64, tối thiểu 256 bit |
| `IP_HASH_SECRET` | Giá trị local có sẵn | Khóa HMAC dạng Base64, tối thiểu 256 bit |
| `REFRESH_COOKIE_SECURE` | `false` | Đặt `true` khi chạy qua HTTPS |

Giá trị secret mặc định chỉ dành cho local development. Không sử dụng chúng ở staging hoặc production và không commit secret thật vào repository.

Nếu đổi `FRONTEND_PORT`, cần cập nhật cả `FRONTEND_URL` của service `backend` trong `compose.yml` để liên kết xác thực email trỏ về đúng cổng.

## 9. Xử lý sự cố

### Không mở được ứng dụng

Kiểm tra Docker Desktop và trạng thái container:

```powershell
docker compose ps
docker compose logs backend frontend
```

### Không nhận được email xác thực

- Kiểm tra Mailpit tại http://localhost:8025.
- Kiểm tra log backend để xác nhận Mailpit đang hoạt động.
- Dùng chức năng gửi lại liên kết trên trang xác thực nếu link cũ hết hạn.

### Không thấy template khi tạo memory

- Đợi backend khởi động và Liquibase chạy xong.
- Xác nhận backend trong Compose đang dùng `SPRING_LIQUIBASE_CONTEXTS=dev`.
- Kiểm tra log Liquibase trong `docker compose logs backend`.

### Không tải được ảnh

- Kiểm tra `minio` và `minio-init` bằng `docker compose ps`.
- Mở MinIO Console để xác nhận bucket `memories` tồn tại.
- Nếu đổi cổng frontend, cập nhật CORS của MinIO cho đúng origin.

### Cổng đã được sử dụng

Đổi cổng tương ứng trong file `.env`, sau đó chạy lại `docker compose up --build`.

## 10. Cấu trúc dự án

```text
backend/     Spring Boot API, domain, persistence và Liquibase
frontend/    Next.js App Router và giao diện người dùng
infra/       Cấu hình dịch vụ hạ tầng local
docs/        Tài liệu đặc tả và kế hoạch triển khai
compose.yml  Cấu hình chạy toàn bộ nền tảng bằng Docker Compose
```
