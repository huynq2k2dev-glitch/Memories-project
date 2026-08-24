# Báo cáo nghiệm thu Foundation và migration

Báo cáo này ghi nhận kết quả Ticket 23 tại ngày 2026-08-24. Kết luận tổng thể là **nghiệm thu một phần**: migration và kiểm tra tĩnh đạt, nhưng các tiêu chí cần integration test, CI và release baseline chưa có đủ bằng chứng.

## Môi trường xác minh

- Backend: Spring Boot 3.5.16, Java 21, Hibernate 6.6.53.
- Database: PostgreSQL 16.15 trong Docker.
- Migration: Liquibase 4.31.1, 26 changeset từ `002` đến `020`.
- Hibernate giữ `spring.jpa.hibernate.ddl-auto=validate`; dự án không có H2.

## Kết quả theo SRS mục 11

| Tiêu chí | Kết quả | Bằng chứng và giới hạn |
| --- | --- | --- |
| Khởi động trên PostgreSQL sạch; Liquibase và Hibernate validate không lỗi | Đạt runtime | Database tạm rỗng chạy đủ 26 changeset, backend khởi động và `/api/v1/health` trả `UP`; `EntityManagerFactory` được khởi tạo với Hibernate `validate`. |
| Bảng, FK, unique/check constraint và index trong mục 6 có tên rõ ràng | Đạt migration | Đã đối chiếu changelog với mục 6. Migration `020` bổ sung FK avatar/thumbnail, check/index còn thiếu, chuẩn hóa tên FK và delete action. Toàn bộ DDL chạy được trên PostgreSQL sạch. |
| Không EAGER mặc định, không `@Data`, controller không trả entity | Đạt kiểm tra tĩnh | Không tìm thấy `FetchType.EAGER` hoặc Lombok `@Data`; controller trả DTO/Problem Details. |
| Register/login/refresh/logout, token reuse và khóa tài khoản có integration test | Chưa nghiệm thu | Theo quyết định dự án, không tạo và không chạy test. Luồng chỉ có bằng chứng từ code/build, chưa có bằng chứng hành vi tự động. |
| Owner/collaborator/guest không thể đổi ID để vượt quyền | Chưa nghiệm thu | Access service và repository đã có ràng buộc theo actor, nhưng authorization matrix và IDOR chưa được chạy integration test. |
| Upload không truyền binary qua backend; chỉ asset `READY` được gắn | Đạt kiểm tra tĩnh | API phát presigned PUT; service kiểm tra trạng thái asset trước khi liên kết. Chưa chạy luồng upload end-to-end trong Ticket 23. |
| Template version đã publish bất biến; memory ghim đúng version | Đạt kiểm tra tĩnh | Không có API sửa/xóa version `PUBLISHED`; memory lưu `template_version_id`. Chưa có integration test hồi quy. |
| Publish từ chối thiếu section bắt buộc hoặc config sai schema | Đạt kiểm tra tĩnh | Publish service dùng contract/schema của template version đã ghim. Chưa chạy các ca từ chối bằng integration test. |
| Public payload không lộ dữ liệu nhạy cảm | Đạt kiểm tra tĩnh | DTO public không chứa password/token hash, liên hệ/ghi chú khách khác hoặc storage credential. Chưa có contract/integration test tự động. |
| CI kiểm tra validate, clean migration và nâng cấp từ baseline trước | Chưa nghiệm thu | Repo chưa có cấu hình CI, chỉ có một commit khởi tạo và không có tag/release baseline. Local đã xác minh clean migration và nâng cấp schema đang chạy từ `019` lên `020`, nhưng không thể coi đó là nâng cấp từ release baseline. |

## Kết quả migration và rollback

- `020-foundation-schema-alignment.sql` được thêm mới; không sửa nội dung các changeset `002`-`019` đã tồn tại.
- Database local đang chạy đã nâng cấp từ 25 lên 26 changeset; Liquibase thành công, Hibernate validate thành công và health trả `UP`.
- PostgreSQL tạm đã chạy chu trình: database rỗng → update 26 changeset → validate → rollback 26 changeset → update lại 26 changeset → validate → khởi động backend với Hibernate validate. Tất cả các bước này đều thành công.
- Seed `USER`, `ADMIN`, ba permission và quan hệ permission của `ADMIN` dùng UUID cố định; migration `020` chuẩn hóa timestamp seed về `2026-01-01T00:00:00Z` để trạng thái cuối có tính xác định.
- Để thêm check `expires_at > created_at`, migration xóa các `verification_tokens` đã vi phạm điều kiện. Việc xóa này đã được xác nhận trước khi triển khai và không thể phục hồi khi rollback.
- Rollback schema đã chạy thành công, nhưng không phải data rollback tuyệt đối: token không hợp lệ đã xóa không thể phục hồi và timestamp seed cũ được đặt lại bằng thời điểm rollback thay vì giá trị lịch sử chính xác.

## Lệnh kiểm tra đã chạy

```text
docker compose build backend
docker compose up -d backend
curl --fail http://localhost:8080/api/v1/health

liquibase ... validate
liquibase ... rollback-count --count=26
liquibase ... update
liquibase ... validate

curl --fail http://localhost:18081/api/v1/health
```

Lần gọi Liquibase CLI đầu tiên thiếu `--search-path` nên không tìm thấy changelog trong bind mount. Đây là lỗi câu lệnh xác minh, không thay đổi database; chạy lại với search path đúng đã validate thành công trước và sau rollback/re-apply.

## Phần còn lại để nghiệm thu đầy đủ

- Bổ sung bằng chứng integration test cho auth, token reuse/account lock, authorization matrix, IDOR, upload, publish validation và public payload khi chính sách không-test của dự án được thay đổi.
- Chọn nền tảng CI rồi cấu hình clean migration, Liquibase validate và upgrade migration.
- Tạo một release/tag baseline thực tế và kiểm tra nâng cấp từ snapshot database của baseline đó.
- Các quyết định quota, retention, privacy và backup/RPO/RTO vẫn mở như ghi nhận trong `foundation-hardening.md`.
