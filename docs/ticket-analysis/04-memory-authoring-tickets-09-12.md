# Ticket 09–12 — Tạo và biên soạn memory

Nhóm này xây aggregate `Memory` và các child có thứ tự. Schema nằm trong `007-memory-core.sql`, `008-memory-content.sql`, `009-memory-schedule.sql` và `010-memory-coordinate-pair-constraint.sql`.

## Ticket 09 — Tạo memory DRAFT

### Nó làm gì

Cho user chọn một template version đã publish và tạo memory nháp do mình sở hữu. Memory ghim trực tiếp version để catalog thay đổi sau này không đổi lịch sử render.

### Triển khai trong code

- `MemoryController.create` nhận `CreateMemoryRequest`; `MemoryService.create` lấy actor từ JWT, không nhận owner ID từ client.
- `TemplateSelectionService` kiểm tra lại template `ACTIVE` và version `PUBLISHED` trong transaction.
- Memory type của request phải trùng template; default config được `deepCopy` thành `theme_config` ban đầu.
- Entity `Memory` tự đặt `DRAFT`, `PRIVATE`, UUID và audit user/time.
- `MemorySlugService` bỏ dấu, chuẩn hóa URL-safe và nối UUID không dấu gạch. Phần UUID khiến hai title giống nhau vẫn sinh slug khác; unique partial index tiếp tục là lớp bảo vệ database.
- `MemoryAccessService.requireView` truy vấn record chưa soft-delete rồi so owner/collaborator. Actor không liên quan nhận not-found để hạn chế lộ resource tồn tại.

Migration `007` tạo `memories`, unique slug cho record chưa xóa, index owner/status và status/visibility/published time, cùng check cho status/visibility/JSONB/password policy.

## Ticket 10 — Metadata và theme

### Nó làm gì

Cho actor có quyền sửa cập nhật title, summary, visibility, password policy, theme và thời gian của memory draft mà không ghi đè thay đổi mới hơn.

### Triển khai trong code

- `UpdateMemoryRequest` bắt buộc gửi `version`. `MemoryService.update` so version hiện hành trước khi thay đổi; `@Version` và bắt `ObjectOptimisticLockingFailureException` bảo vệ race ở bước flush.
- `TemplateThemeConfigService` validate `theme_config` theo schema của template version đã ghim.
- `MemoryContentSafetyService` từ chối raw HTML và protocol `javascript:`/`data:text/html`; dữ liệu có thể là text/Markdown nhưng renderer không thực thi script.
- Chỉ owner có capability `canChangeAccessPolicy`; collaborator EDIT/ADMIN có thể sửa nội dung draft nhưng không đổi visibility/password.
- `accessPassword` bị redacted trong `UpdateMemoryRequest.toString`, tránh vô tình ghi secret khi object bị log.
- `settings` không nhận object tùy ý từ request. Trong code cuối, key nghiệp vụ đã được xác nhận là `messageModerationEnabled`, khởi tạo `true` và chỉ thay đổi qua service riêng của Ticket 20.

## Ticket 11 — Member và section

### Nó làm gì

Quản lý nhân vật chính và các section có thứ tự theo hợp đồng template.

### Triển khai trong code

- Migration `008` tạo `memory_members`, `memory_sections`, unique key/order, check sort order và JSONB section contract.
- `MemoryMemberService` và `MemorySectionService` luôn gọi `MemoryAccessService` trước khi đọc/ghi. Repository tìm child bằng cả `childId` và `memoryId`, nên đổi một UUID riêng lẻ không kéo child từ aggregate khác.
- Member hỗ trợ role code, tên, mô tả, avatar asset và sort order. Text đi qua content safety service.
- Section hỗ trợ `section_key`, `section_type`, title/content/config, visibility và sort order. `TemplateSectionContractService.check` xác nhận type được phép và config đúng schema.
- Response section có `required` và `contentComplete`; một row rỗng hoặc bị ẩn không làm required section trở nên hợp lệ.
- Batch reorder yêu cầu toàn bộ ID và version hiện hành. Service đổi sang một dải sort order tạm, flush, rồi ghi thứ tự `0..n-1`; hai pha tránh va chạm unique `(memory_id, sort_order)` trong cùng transaction.

Frontend `memory-content-editor.tsx` tải member và section, gửi version khi sửa/xóa/reorder, giữ form/error để user có thể reload khi conflict.

## Ticket 12 — Location và event

### Nó làm gì

Quản lý địa điểm và lịch/timeline dùng cho render và RSVP sau này.

### Triển khai trong code

- Migration `009` tạo `memory_locations` và `memory_events`; `010` siết tọa độ thành cặp cùng null hoặc cùng hợp lệ.
- `MemoryLocationService` và `MemoryEventService` dùng cùng access/version/reorder pattern như member/section.
- `MemoryScheduleValidationService` kiểm tra latitude `[-90,90]`, longitude `[-180,180]`, IANA `ZoneId`, và `endAt >= startAt`.
- Map URL phải HTTPS, không user-info/port lạ và host/path thuộc allowlist Google Maps hoặc OpenStreetMap.
- Event lưu `Instant` UTC và giữ timezone string riêng để frontend hiển thị đúng vùng giờ.
- Location reference được tìm bằng cả location ID và memory ID. Xóa location dùng FK `SET NULL`, nên event còn tồn tại nhưng không trỏ tới record đã xóa.
- Repository đọc danh sách theo memory và sort order; render service tải bulk location/event rồi nối bằng ID, không query theo từng event.

Frontend `memory-schedule-editor.tsx` quản lý hai collection và optimistic version, đồng thời dùng timezone/event data đúng hợp đồng backend.

## API chính

| Resource | Endpoint gốc | Ghi chú |
| --- | --- | --- |
| Memory | `/api/v1/memories` | create/get/update |
| Member | `/api/v1/memories/{memoryId}/members` | CRUD, order, avatar |
| Section | `/api/v1/memories/{memoryId}/sections` | CRUD và order |
| Location | `/api/v1/memories/{memoryId}/locations` | CRUD và order |
| Event | `/api/v1/memories/{memoryId}/events` | CRUD và order |

## Docker đã dùng

```powershell
docker compose build backend
docker compose build frontend
docker compose up -d backend frontend
docker compose logs --tail 120 backend
curl.exe --fail http://localhost:8080/api/v1/health
```

Backend build xác nhận entity/DTO/repository/service compile và Liquibase `007–010` áp được; frontend build xác nhận các editor TypeScript tương thích response. Health kiểm tra schema cuối vẫn được Hibernate validate trên PostgreSQL.

## Công nghệ và ý nghĩa

- **Hibernate/JPA optimistic locking**: cột `version` và `@Version` phát hiện lost update; request còn tự gửi expected version để trả conflict rõ ràng.
- **PostgreSQL JSONB**: lưu theme/settings/section config, đi kèm check `jsonb_typeof` và JSON Schema ở service.
- **Jackson `JsonNode`**: giữ contract động nhưng vẫn đóng khung bằng template version.
- **Java `Instant`, `ZoneId`, `BigDecimal`**: tách thời điểm tuyệt đối, timezone hiển thị và tọa độ chính xác.

## Giới hạn

- Memory chỉ sửa nội dung khi còn DRAFT; publish/lifecycle ở ticket sau.
- Không có renderer Markdown tùy ý hoặc HTML sanitizer phức tạp; raw HTML/protocol nguy hiểm bị chặn.
- Không tạo test file theo quyết định dự án, nên optimistic race và IDOR chưa có bằng chứng integration test tự động.
