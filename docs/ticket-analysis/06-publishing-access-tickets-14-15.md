# Ticket 14–15 — Preview, publish và kiểm soát truy cập

## Ticket 14 — Preview và publish

### Nó làm gì

Cho owner/collaborator hợp lệ xem preview bằng renderer thật, publish memory đủ điều kiện và cung cấp public payload tối thiểu qua slug.

### Triển khai trong code

- Migration `012-memory-publishing.sql` bổ sung `published_at`, contract cover và bảng `audit_logs` append-only.
- `GET /api/v1/memories/{id}/preview` gọi `MemoryRenderService.preview`. Nó dùng cùng `MemoryRenderResponse`, template contract và frontend registry với trang public.
- `MemoryPublishingPersistenceService.publish` chạy trong một transaction và kiểm tra:
  - actor có capability publish;
  - memory còn DRAFT và expected version đúng;
  - visibility thuộc tập publishable và expiry còn tương lai;
  - template/version còn render/publish được;
  - theme và từng section config đúng JSON Schema;
  - mọi required section có visible content;
  - cover tồn tại nếu contract yêu cầu;
  - cover/avatar/gallery asset đều còn `READY`.
- Khi hợp lệ, entity chuyển `PUBLISHED`, ghi `published_at`, flush optimistic version và ghi audit success. Failure/denied được wrapper service ghi bằng transaction cô lập để rollback publish không làm mất audit thất bại.
- `MemoryRenderService` bulk-load member, section, location, event, image, approved message và asset metadata; public DTO không có owner, settings, guest contact/note, hash hoặc storage credential.
- Public media là URL tương đối qua backend; backend xác minh asset đang được một memory khả dụng tham chiếu rồi mới redirect tới presigned GET ngắn hạn.

Ticket 14 ban đầu mở PUBLIC/UNLISTED. Sau Ticket 15, tập publishable cuối cùng gồm cả PRIVATE và PASSWORD_PROTECTED, nhưng chỉ PUBLIC/UNLISTED được xem trực tiếp không credential.

## Ticket 15 — PRIVATE và PASSWORD_PROTECTED

### Nó làm gì

Bổ sung hai chính sách truy cập: PRIVATE chỉ actor/grant hợp lệ; PASSWORD_PROTECTED yêu cầu unlock bằng password trước khi trả payload.

### Triển khai trong code

- Migration `013-memory-password-access.sql` tạo `memory_access_grants` chỉ lưu token hash và expiry.
- Password dùng cùng BCrypt `PasswordEncoder`; raw password không lưu, không trả response và bị redacted trong DTO string.
- `MemoryPasswordAccessService.passwordHashForUpdate` buộc visibility/password nhất quán. Chuyển sang protected cần password mới hoặc giữ hash hiện hành; visibility khác không được gửi password.
- `POST /api/v1/public/memories/{slug}/unlock` tìm đúng memory PUBLISHED/protected/chưa hết hạn, verify BCrypt, sinh token 256 bit và lưu SHA-256.
- Browser nhận cookie theo memory: `HttpOnly`, `SameSite=Lax`, path `/`, TTL mặc định 30 phút. Đổi/gỡ password xóa toàn bộ grant cũ.
- `MemoryRenderService.publicMemory` áp chính sách cuối:
  - PUBLIC/UNLISTED: render trực tiếp khi published/chưa hết hạn.
  - PRIVATE: owner/collaborator hoặc share-link grant hợp lệ.
  - PASSWORD_PROTECTED: owner/collaborator hoặc password grant hợp lệ.
  - draft/archived/deleted/expired: not-found trước khi xét credential.
- Frontend `/memories/{slug}` dùng `memory-unlock-form.tsx`; BFF chỉ forward cookie có prefix memory access, không forward toàn bộ cookie client tới backend.

## Ý nghĩa kiến trúc

- Preview và public dùng một render DTO/registry, giảm nguy cơ “preview đúng nhưng public sai”.
- Access được kiểm tra lại mỗi lần lấy payload/media; presigned URL có TTL ngắn nên không trở thành quyền bền vững.
- Password grant là opaque random token trong cookie, không phải cờ client-side có thể tự sửa.
- Audit failure nằm ngoài transaction publish chính, nên vẫn giữ được dấu vết khi business transaction rollback.

## Docker đã dùng

```powershell
docker compose build backend
docker compose build frontend
docker compose up -d backend frontend
docker compose logs --tail 120 backend
curl.exe --fail http://localhost:8080/api/v1/health
```

Build cả render contract backend và React renderer/frontend access flow. Startup xác nhận migrations `012–013`, audit trigger và Hibernate schema validation.

```powershell
docker compose ps
```

Kiểm tra PostgreSQL, MinIO, backend và frontend đều còn hoạt động vì public render phụ thuộc cả dữ liệu và media delivery.

## Công nghệ bên ngoài

- **JSON Schema validator**: tái sử dụng để kiểm tra theme/section ngay trước publish, không chỉ khi admin tạo template.
- **BCrypt**: password access được hash giống password account, nhưng grant sau unlock là token ngẫu nhiên riêng.
- **MinIO/AWS S3 presigner**: public media endpoint chỉ redirect sau authorization.
- **Next.js server routes**: lọc cookie, proxy response/Set-Cookie và giữ backend URL nội bộ.

## Giới hạn

- PUBLIC và UNLISTED hiện khác nhau về policy sản phẩm nhưng chưa có public discovery/search, nên truy cập trực tiếp bằng slug giống nhau.
- Cookie secure mặc định false cho HTTP local; production HTTPS phải đặt `MEMORY_ACCESS_COOKIE_SECURE=true`.
- Không có integration authorization test cho anonymous/owner/collaborator/password đúng-sai theo quyết định dự án.

