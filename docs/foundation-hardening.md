# Foundation hardening baseline

Tài liệu này ghi nhận trạng thái Ticket 22 tại ngày 2026-08-24. Các mục chưa được đo hoặc chưa có quyết định sản phẩm được giữ ở trạng thái mở, không được xem là đã hoàn thành.

## Kiểm soát đã triển khai

- Login dùng fixed-window rate limit 20 request/10 phút theo IP đã HMAC-SHA256; khóa tài khoản 5 lần sai/15 phút vẫn hoạt động độc lập. Upload initiation dùng 30 request/10 phút theo user. Bucket được khóa hàng trong PostgreSQL và xóa sau 24 giờ.
- Lời chúc giữ giới hạn 5 request/10 phút theo memory/IP. Backend mặc định không tin `X-Forwarded-For`; chỉ bật `RATE_LIMIT_TRUST_FORWARDED_FOR=true` khi backend chỉ nhận traffic từ proxy tin cậy có ghi đè header này.
- Upload chỉ cho JPEG, PNG, WebP và AVIF, tối đa 10 MiB. Backend kiểm tra content type, kích thước và magic bytes sau upload; SVG không được phép.
- Correlation ID từ client chỉ được giữ khi là UUID chuẩn. Giá trị khác được thay bằng UUID do server tạo. `ProblemDetail.instance` dùng `/api/v1`, không phản chiếu slug, token hoặc URL presigned từ request.
- Controller trả DTO. Public memory không trả owner, guest contact/note, token hash, storage key/credential hoặc presigned URL; public media dùng URL tương đối qua endpoint kiểm tra quyền.
- Login bất thường, access denied, publish, archive/delete, quản lý collaborator/share link, submit/moderation lời chúc được ghi vào audit append-only bằng ID và correlation ID, không ghi password, raw token hay email đầy đủ.
- Catalog, admin template, guest và moderation message là các danh sách độc lập có phân trang. Danh sách con phục vụ memory editor vẫn trả toàn bộ collection vì batch reorder yêu cầu toàn bộ ID; đây là ngoại lệ chức năng đã được xác nhận.
- Public renderer tải memory và từng loại collection bằng các truy vấn bulk cố định rồi nối trong memory; không truy vấn child theo từng phần tử. JPA tắt Open Session in View, association khai báo lazy, entity không dùng Lombok `@Data` và không có JPA cascade ngoài hợp đồng dữ liệu.
- Khóa ngoại tới `users`, `template_versions` và `media_assets` dùng `RESTRICT`. Query memory/media lọc `deletedAt`; luồng xác thực user luôn kiểm tra trạng thái active và `deletedAt` trước khi cấp quyền. Token, audit và reference data không có generic CRUD endpoint.

## Tiêu chí chưa được chứng minh tự động

Theo quyết định dự án, Ticket 22 không tạo hoặc chạy test. Vì vậy authorization matrix owner/admin/edit/view/guest/anonymous/unrelated và các ca thay UUID/slug/token mới chỉ được kiểm tra tĩnh từ access service cùng repository query, chưa được chứng minh bằng integration test.

Public page p95 `<500ms` chưa được đo. Lần xác minh hiện tại chỉ dùng Docker Compose local trên Windows với backend Java 21, PostgreSQL 16 và MinIO; không có dataset đại diện hoặc thông số phần cứng được chốt nên kết quả latency local sẽ không có ý nghĩa nghiệm thu. Benchmark sau này cần ghi rõ CPU/RAM, topology mạng, kích thước dataset, số lượng child/media/message trên mỗi memory, concurrency, warm-up, số mẫu và p95 backend không gồm CDN.

## Quyết định còn mở

- Quota số memory, image/storage theo user hoặc package; giới hạn media Foundation hiện tại chỉ là 200 asset hoặc 1 GiB cho mỗi owner asset.
- Thời hạn giữ dữ liệu sau soft-delete, cửa sổ restore và quy trình purge object storage.
- Retention/partitioning cho `audit_logs` và `auth_audit_events` dựa trên dữ liệu vận hành thực tế.
- Chính sách riêng tư cho funeral memory và ảnh trẻ em; quy trình data export, account deletion và xử lý yêu cầu chủ thể dữ liệu.
- RPO/RTO, lịch, encryption, nơi lưu, kiểm tra restore và quyền vận hành backup PostgreSQL/object storage. Hai nguồn dữ liệu phải được backup và restore độc lập.

Các mục trên cần ticket/quyết định sản phẩm-vận hành riêng trước khi triển khai; không dùng default ngầm trong code.
