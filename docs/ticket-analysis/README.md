# Phân tích triển khai các ticket Foundation

Bộ tài liệu này giải thích 23 ticket Foundation dựa trên issue gốc trong `.scratch/nen-tang-thiep-va-ky-niem-online-foundation/issues`, SRS và code hiện tại. Các ticket liên quan được gom chung một file, nhưng mỗi ticket vẫn có mục riêng để truy vết mục tiêu, cách làm và phần code tương ứng.

## Nhóm tài liệu

| Nhóm | Ticket | Nội dung |
| --- | --- | --- |
| [Nền tảng và hạ tầng](01-foundation-infrastructure-ticket-01.md) | 01 | Spring Boot, Next.js, PostgreSQL, Liquibase, Docker Compose, health và lỗi chuẩn hóa |
| [Xác thực và phân quyền](02-authentication-authorization-tickets-02-06.md) | 02–06 | Đăng ký, email verification, login, JWT, refresh rotation, RBAC và khóa tài khoản |
| [Template và renderer](03-template-renderer-tickets-07-08.md) | 07–08 | Quản trị template/version, JSON Schema, catalog và frontend renderer registry |
| [Biên soạn memory](04-memory-authoring-tickets-09-12.md) | 09–12 | Memory draft, metadata/theme, member, section, location và event |
| [Media và upload trực tiếp](05-media-upload-ticket-13.md) | 13 | Presigned URL, MinIO/S3, xác minh file và gắn asset |
| [Publish và kiểm soát truy cập](06-publishing-access-tickets-14-15.md) | 14–15 | Preview, publish, public render, PRIVATE và PASSWORD_PROTECTED |
| [Cộng tác và vòng đời](07-collaboration-lifecycle-tickets-16-17.md) | 16–17 | Ma trận collaborator, archive và soft-delete |
| [Guest, RSVP, lời chúc và share link](08-guest-engagement-sharing-tickets-18-21.md) | 18–21 | Khách mời, token cá nhân, RSVP, moderation và link chia sẻ |
| [Hardening và nghiệm thu](09-hardening-acceptance-tickets-22-23.md) | 22–23 | Rate limit, privacy, observability, schema alignment, rollback và cổng nghiệm thu |

## Cách đọc phần code

Backend là modular monolith. Mỗi feature nằm trực tiếp dưới `com.memories.platform.<module>` và chia theo trách nhiệm:

- `controller`: hợp đồng HTTP, nhận DTO và trả DTO.
- `service`: orchestration, business rule, transaction và kiểm tra quyền.
- `repository`: truy vấn JPA đã giới hạn theo owner/memory/status để chống truy cập chéo.
- `entity`: trạng thái persistence và optimistic locking.
- `dto`, `constants`, `exception`: hợp đồng dữ liệu, hằng số nghiệp vụ và lỗi an toàn.
- `config`, `common`, `utils`: cấu hình và cơ chế dùng chung, không bị bọc trong một package `modules` trung gian.

Frontend dùng Next.js App Router. Browser gọi route nội bộ dưới `frontend/app/api`; route này proxy tới backend, chỉ chuyển tiếp header/cookie được cho phép. Access token được giữ trong memory của browser, còn refresh token và access grant nằm trong cookie `HttpOnly`.

## Phạm vi bằng chứng

Theo quyết định của dự án, các ticket không tạo hoặc chạy test. Tài liệu mô tả code đã triển khai và các lệnh build, static check, Docker runtime, Liquibase/health đã thực sự dùng; nó không biến các luồng chưa có integration test thành đã được nghiệm thu tự động. Khoảng trống này được tổng hợp ở nhóm Ticket 22–23 và báo cáo nghiệm thu Foundation.

