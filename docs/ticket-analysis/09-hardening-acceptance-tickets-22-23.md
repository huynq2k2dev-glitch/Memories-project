# Ticket 22–23 — Hardening và cổng nghiệm thu Foundation

## Ticket 22 — Security, privacy và observability hardening

### Nó làm gì

Ticket 22 rà lại các luồng đã có, thêm rate limit còn thiếu, giảm dữ liệu nhạy cảm trong log/payload và xử lý các điểm vận hành trước phát hành.

### Triển khai trong code

- Migration `019-rate-limit-buckets.sql` tạo bucket persistent với khóa chính `(scope,subject_key)` và index retention.
- `RateLimitService` tạo bucket bằng PostgreSQL `INSERT ... ON CONFLICT DO NOTHING`, sau đó pessimistic-lock row và tăng fixed-window counter trong transaction `REQUIRES_NEW`.
- Login giới hạn mặc định 20 request/10 phút theo HMAC IP; upload initiation giới hạn 30 request/10 phút theo user ID. Guest message giữ 5 request/10 phút theo memory/HMAC IP.
- `RateLimitRetentionService` xóa bucket cũ sau 24 giờ bằng scheduled cron UTC.
- `ClientIpHashService` mặc định dùng `remoteAddr`; `RATE_LIMIT_TRUST_FORWARDED_FOR=false` ngăn client tự giả `X-Forwarded-For`. Frontend BFF cũng không chủ động chuyển header IP do client cung cấp.
- MIME upload được kiểm tra bằng magic bytes và SVG bị chặn.
- `CorrelationIdFilter` chỉ nhận UUID đúng chuẩn. Problem Details dùng instance redacted và không phản chiếu URL/token.
- Public DTO/media proxy không trả owner/settings/contact/note/hash/storage key/credential/presigned URL lâu dài.
- Admin template, catalog, guest và moderation list có pagination. Public render bulk-load collection thay vì N+1.
- Entity không dùng `@Data`, không khai báo EAGER; Open Session in View tắt. Query memory/media mặc định lọc soft-delete.
- Audit append-only bao phủ auth bất thường, access denied, publish, collaboration/share, lifecycle và moderation.

Phân tích chi tiết quyết định và mục còn mở nằm trong `docs/foundation-hardening.md`. P95 `<500ms` chưa được đo vì chưa có dataset, topology và cấu hình phần cứng đại diện.

## Ticket 23 — Migration gate và nghiệm thu

### Nó làm gì

Đối chiếu schema với SRS, chứng minh clean migration/rollback/re-apply trên PostgreSQL và lập báo cáo pass/gap trung thực.

### Migration `020`

`020-foundation-schema-alignment.sql` được thêm mới thay vì sửa changeset cũ. Nó:

- thêm `users.avatar_asset_id` và FK thumbnail template;
- chuẩn hóa delete action cho auth relation, token, cover/avatar và collaborator;
- thêm check uppercase role/permission, OAuth provider, token expiry và READY asset size;
- thêm index còn thiếu cho role-permission, user-role và OAuth account;
- đổi tên FK về quy ước `fk_<table>_<target>` rõ ràng;
- chuẩn hóa timestamp seed role/permission/admin relation về `2026-01-01T00:00:00Z` trong trạng thái cuối;
- xóa `verification_tokens` có `expires_at <= created_at` trước khi thêm check mới.

Việc xóa token không hợp lệ đã được xác nhận trước khi thực hiện. Nó không thể phục hồi khi rollback. Rollback schema đã chạy thành công, nhưng timestamp seed cũ chỉ được đặt lại bằng thời điểm rollback, không khôi phục chính xác lịch sử.

### Kết quả nghiệm thu thực tế

- Database đang chạy nâng cấp `019→020`, Liquibase thành công, Hibernate validate thành công, health `UP`.
- PostgreSQL 16 tạm chạy: clean update 26 changeset → validate → rollback 26 → update 26 → validate → backend Hibernate validate/health. Toàn bộ chu trình thành công.
- Liquibase không báo duplicate ID/include/checksum.
- Changeset đã commit `002–012` không bị sửa; thay đổi tiếp theo nằm ở file mới.
- Không có H2; `ddl-auto` vẫn là `validate`.
- Repo không có CI, tag hoặc release baseline; chỉ có commit khởi tạo. Vì vậy local upgrade `019→020` không được trình bày giả thành release-baseline upgrade.
- Không tạo/chạy integration test theo quyết định dự án. Các tiêu chí auth flow, authorization matrix, IDOR, upload, publish và secret contract còn là khoảng trống nghiệm thu tự động.

Bảng đối chiếu đầy đủ SRS mục 11 nằm trong `docs/foundation-acceptance-report.md`.

## Các lệnh Docker dùng cho Ticket 22

```powershell
docker compose build backend
docker compose build frontend
docker compose up -d backend frontend
docker compose ps
docker compose logs --tail 120 backend
curl.exe --fail http://localhost:8080/api/v1/health
```

Mục đích là compile/package rate-limit và hardening ở cả backend/BFF, áp migration `019`, kiểm tra startup và health trên stack local.

## Các lệnh Docker/Liquibase dùng cho Ticket 23

Các giá trị password/secret thật không được ghi trong tài liệu; dưới đây dùng placeholder.

```powershell
docker compose build backend
docker compose up -d backend
docker compose logs --tail 120 backend
curl.exe --fail http://localhost:8080/api/v1/health
```

Dùng để nâng database local từ 25 lên 26 changeset và chứng minh Hibernate validate sau upgrade.

```powershell
docker pull liquibase/liquibase:4.31.1
docker network create memories-ticket23-network
docker run -d --name memories-ticket23-postgres `
  --network memories-ticket23-network `
  -e POSTGRES_DB=memories_acceptance `
  -e POSTGRES_USER=memories_acceptance `
  -e POSTGRES_PASSWORD=<temporary-password> `
  postgres:16-alpine
```

Tạo network và PostgreSQL tạm, tách khỏi volume/database đang chạy. `pg_isready` được dùng để đợi database sẵn sàng trước migration.

```powershell
docker run -d --name memories-ticket23-backend `
  --network memories-ticket23-network -p 18081:8080 `
  -e DB_URL=jdbc:postgresql://memories-ticket23-postgres:5432/memories_acceptance `
  -e DB_USERNAME=memories_acceptance `
  -e DB_PASSWORD=<temporary-password> `
  -e ACCESS_TOKEN_SECRET=<temporary-base64-secret> `
  -e IP_HASH_SECRET=<temporary-base64-secret> `
  memories_project-backend

curl.exe --fail http://localhost:18081/api/v1/health
```

Chạy image backend thật trên database rỗng. Spring tự apply 26 changeset rồi Hibernate validate toàn bộ entity mapping.

Liquibase CLI được chạy với resources bind mount read-only và `--search-path=/liquibase/changelog`:

```powershell
docker run --rm --network memories-ticket23-network `
  --mount type=bind,source=<resources-path>,target=/liquibase/changelog,readonly `
  liquibase/liquibase:4.31.1 `
  --search-path=/liquibase/changelog `
  --url=jdbc:postgresql://memories-ticket23-postgres:5432/memories_acceptance `
  --username=memories_acceptance `
  --password=<temporary-password> `
  --changelog-file=db/changelog/db.changelog-master.yaml validate

# Cùng common arguments:
# rollback-count --count=26
# update
# validate
```

`validate` kiểm tra changelog; `rollback-count` tháo toàn bộ Foundation; `update` dựng lại để chứng minh rollback không làm migration mất khả năng re-apply. Lần gọi CLI đầu thiếu search path nên không tìm thấy file trong bind mount; chạy lại với `--search-path` đã thành công và không thay đổi database ở lần lỗi đó.

```powershell
docker rm -f memories-ticket23-backend memories-ticket23-postgres
docker network rm memories-ticket23-network
```

Chỉ xóa đúng hai container và network tạm sau khi đã xác minh tên; database/volume Compose chính không bị tác động.

## Công nghệ bên ngoài

- **PostgreSQL 16**: môi trường migration thật cho JSONB, partial index, FK/check và locking.
- **Liquibase runtime + CLI 4.31.1**: runtime update qua Spring; CLI validate/rollback/update độc lập.
- **Docker network/container tạm**: cô lập nghiệm thu destructive rollback khỏi dữ liệu phát triển.
- **Spring Scheduling/PostgreSQL**: retention rate-limit/IP hash chạy trong application, không cần Redis hoặc scheduler ngoài.

## Kết luận và rủi ro còn lại

Phần schema/runtime migration đạt. Foundation tổng thể mới nghiệm thu một phần vì chưa có integration test, CI, release baseline, p95 benchmark và quyết định cuối về quota/retention/privacy/backup. Các thiếu hụt này được ghi nhận, không được coi là hoàn thành ngầm.

