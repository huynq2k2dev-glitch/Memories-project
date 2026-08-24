# Ticket 18–21 — Guest, RSVP, lời chúc và share link

## Ticket 18 — Quản lý guest và link cá nhân

### Nó làm gì

Cho owner quản lý danh sách khách, phát hành token cá nhân hóa và chỉ trả đúng ngữ cảnh lời mời của guest đó.

### Triển khai trong code

- Migration `015-memory-guests.sql` tạo `memory_guests`, unique token hash khi có giá trị, party-size check và index theo memory/status/group.
- `MemoryGuestService` yêu cầu owner cho list/create/update/issue token/disable. List có pagination tối đa 50.
- Dữ liệu tên/group/note đi qua content safety; email/phone chỉ có trong DTO quản trị.
- Token là 32 byte `SecureRandom`, Base64 URL-safe. Raw token chỉ trả trong `GuestAccessTokenResponse` lúc phát hành; database chỉ lưu SHA-256.
- Phát hành lại thay hash hiện tại nên link cũ mất hiệu lực ngay. Disable chuyển `DISABLED` và xóa token hash; ticket không có reactivate.
- Public invitation tìm guest bằng token hash + trạng thái ACTIVE rồi gọi `MemoryGuestInvitationService`. Response chỉ có tên/group/max party size của guest hiện tại, title memory và event bật RSVP.
- Frontend `/invitations/{token}` render invitation và không gọi API lấy danh sách khách.

## Ticket 19 — RSVP theo event

### Nó làm gì

Cho guest phản hồi từng event RSVP thuộc chính memory của token và cập nhật phản hồi hiện hành.

### Triển khai trong code

- Migration `016-guest-event-responses.sql` tạo unique `(guest_id,event_id)`, status/party checks và optimistic version.
- `MemoryGuestService.respond` resolve guest từ token trước, sau đó `MemoryGuestInvitationService.requireRsvpEvent` xác nhận event thuộc cùng memory, PUBLISHED và bật RSVP.
- Không có row hiện hành thì tạo; có row thì yêu cầu expected `version` rồi update. Unique constraint là lớp bảo vệ race bổ sung.
- `DECLINED` bắt buộc party size 0; trạng thái khác cần `1..maxPartySize`.
- Dietary note/message được kiểm tra content safety. `responded_at` null với PENDING và cập nhật khi có phản hồi thực.
- Invitation chỉ bulk-load response của guest hiện tại cho tập event hiện tại, không lộ response guest khác.

## Ticket 20 — Gửi và moderation lời chúc

### Nó làm gì

Cho khách gửi plain text, rate-limit theo memory/IP và cho owner/collaborator ADMIN kiểm duyệt trước khi public render.

### Triển khai trong code

- Migration `017-guest-messages.sql` thêm setting moderation vào memory và tạo `guest_messages` với content/status/IP hash/index.
- Memory mới mặc định `messageModerationEnabled=true`; message mới là `PENDING`. Khi setting tắt, message mới là `APPROVED`.
- `GuestMessageSanitizationService` loại control character không cần thiết và từ chối raw HTML, `javascript:` hoặc `data:text/html`; độ dài tên/content được kiểm tra lại ở database.
- `ClientIpHashService` HMAC-SHA256 địa chỉ IP bằng `IP_HASH_SECRET`, không lưu raw IP. Mặc định bỏ qua `X-Forwarded-For`; chỉ bật trust khi backend đứng sau proxy tin cậy ghi đè header.
- Rate limit mặc định 5 request/10 phút cho mỗi memory/IP hash. Vi phạm trả 429 trước khi lưu message.
- Transition hiện tại: `PENDING→APPROVED/REJECTED`, `APPROVED→HIDDEN`, `HIDDEN→APPROVED`, `REJECTED→PENDING`.
- Public render chỉ query `APPROVED`; moderator/status/IP hash không nằm trong public DTO.
- Scheduled retention xóa riêng `ip_hash` sau 30 ngày, không xóa nội dung lời chúc.
- Submit và moderation ghi audit theo ID/correlation ID, không ghi raw content vào metadata audit.

## Ticket 21 — Share link có kiểm soát

### Nó làm gì

Cho owner hoặc collaborator ADMIN tạo link `VIEW`/`RSVP`, giới hạn expiry/lượt dùng, thu hồi và kiểm tra lại quyền ở mỗi request.

### Triển khai trong code

- Migration `018-share-links.sql` tạo `share_links`, unique token hash, permission/status/use constraints và index.
- Link chỉ tạo cho memory `PRIVATE` hoặc `UNLISTED`. `RSVP` bắt buộc gắn guest ACTIVE cùng memory; `VIEW` không được gắn guest.
- Raw token chỉ trả trong `IssuedShareLinkResponse`; list metadata không trả token/hash.
- `redeem` hash token, khóa row bằng `findForUpdate`, kiểm tra link/memory/guest/expiry/max-use rồi tăng `use_count` trong cùng transaction. Vì row bị khóa, request đồng thời không vượt `max_uses`.
- Một lượt được tính tại redemption `/shares/{token}`, không tính ở mỗi lần tải memory/media. Sau redeem, browser nhận opaque access cookie theo memory.
- Mọi request sau vẫn lookup token hash và status/expiry/guest. Revoke link hoặc disable guest chặn cookie đang có ngay.
- Permission VIEW chỉ mở payload; RSVP còn mở invitation/form của đúng guest và gọi response flow Ticket 19.
- Create/revoke ghi audit nhưng không lưu raw token/hash.

Frontend gồm `memory-guest-editor.tsx`, `guest-rsvp-form.tsx`, `guest-message-section.tsx`, `memory-message-editor.tsx`, `memory-share-link-editor.tsx` và route `/shares/{token}`.

## Docker đã dùng

```powershell
docker compose build backend
docker compose build frontend
docker compose up -d backend frontend
docker compose logs --tail 120 backend
curl.exe --fail http://localhost:8080/api/v1/health
```

Build và khởi động lại sau migrations `015–018`; PostgreSQL là nơi giữ guest token hash, response, moderation, atomic share use count và audit.

```powershell
docker compose ps
```

Kiểm tra các service nền vẫn sẵn sàng. Nhóm này không thêm một container ngoài mới; nó tái sử dụng PostgreSQL, backend và frontend.

## Công nghệ và cách tích hợp

- **Java `SecureRandom` + SHA-256**: tạo opaque guest/share token và chỉ lưu hash.
- **HMAC-SHA256**: giảm định danh IP nhưng vẫn cho rate limit/retention nhất quán; secret được truyền qua environment.
- **PostgreSQL pessimistic locking/unique constraint**: bảo vệ RSVP update và atomic share-link redemption.
- **Spring Scheduling**: chạy purge IP hash theo cron UTC.
- **Next.js BFF/cookie filtering**: chỉ forward memory access cookie tới public backend route, không chuyển tiếp toàn bộ Cookie header.

## Giới hạn

- Guest disable không có reactivate; muốn mở lại phải có rule/ticket riêng.
- Token không thể đọc lại; UI chỉ hiển thị raw secret ở response phát hành.
- Retention nội dung/audit và privacy policy rộng hơn vẫn chưa chốt.
- Các ca token sai, cross-memory, race RSVP/share và rate limit chưa có integration test tự động.
