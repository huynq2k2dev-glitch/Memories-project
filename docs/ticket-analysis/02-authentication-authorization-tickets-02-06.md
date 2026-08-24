# Ticket 02–06 — Xác thực, phiên đăng nhập và phân quyền

Nhóm này đi từ tạo tài khoản đến khóa tài khoản. Schema chính nằm trong migration `002-auth-baseline.sql`, `003-auth-audit.sql`, `004-refresh-tokens.sql` và `005-auth-audit-actors.sql`.

## Ticket 02 — Đăng ký và phát hành email xác thực

### Nó làm gì

Tạo user `PENDING_VERIFICATION`, gán role `USER`, phát hành token xác thực một lần và gửi link qua SMTP. Email được lowercase, password dùng BCrypt và database chỉ giữ SHA-256 của token.

### Triển khai trong code

1. `AuthController.register` nhận `RegistrationRequest` đã validation.
2. `RegistrationService` chuẩn hóa email/display name rồi gọi `RegistrationPersistenceService.create`.
3. Transaction tạo `UserAccount`, quan hệ `UserRole` và `VerificationToken`. UUID sinh trong application; locale/timezone mặc định là `vi-VN` và `Asia/Ho_Chi_Minh`.
4. `SecureRandom` sinh 32 byte token, encode Base64 URL-safe; `TokenHashUtils` hash token trước khi lưu.
5. Sau khi transaction persistence hoàn thành, `SmtpVerificationEmailSender` gửi link dạng `/verify-email#token=...`.

Fragment `#token` không được browser gửi trong HTTP request ban đầu, giảm nguy cơ token đi vào access log. Nếu gửi mail lỗi, account vẫn ở trạng thái chờ; endpoint resend có thể vô hiệu token cũ và phát hành token mới. Đây là lý do persistence và SMTP được tách service.

Migration `002` tạo users/roles/permissions/user_roles/role_permissions/oauth_accounts/verification_tokens. Role và permission dùng UUID cố định. OAuth chỉ có schema dự phòng; Foundation không triển khai OAuth login.

## Ticket 03 — Xác thực email một lần

### Nó làm gì

Đổi tài khoản từ `PENDING_VERIFICATION` sang `ACTIVE` khi token đúng loại, chưa dùng và chưa hết hạn.

### Triển khai trong code

- Trang `frontend/app/verify-email` đọc token từ URL fragment và POST tới BFF; token không nằm trong server-render log hoặc query string.
- `EmailVerificationService.confirm` hash token rồi dùng repository query `findForUpdate...`. Row lock làm hai request đồng thời không thể cùng sử dụng token.
- User và token được cập nhật trong cùng transaction: user nhận `email_verified_at`, token nhận `used_at`.
- Resend luôn trả trạng thái chung `ACCEPTED`; tài khoản không tồn tại hoặc không còn chờ xác thực không bị lộ qua response.

## Ticket 04 — Login, khóa tạm thời và RBAC

### Nó làm gì

Cho user `ACTIVE` đăng nhập, phát JWT ngắn hạn, khóa tạm khi sai nhiều lần và kiểm tra permission ở backend.

### Triển khai trong code

- `LoginService` điều phối rate limit, `LoginPersistenceService.authenticate`, phát access token và mở refresh session.
- Password được kiểm tra bằng BCrypt cost 12. Cấu hình đã chốt: khóa sau 5 lần sai, thời gian khóa 15 phút, access token 15 phút.
- `AccessTokenService` phát JWT HS256 có issuer, subject là UUID user, `iat`, `exp` và `jti`. Secret Base64 phải ít nhất 256 bit.
- Spring Security chạy stateless OAuth2 Resource Server. `ActiveAccountFilter` kiểm tra lại trạng thái user trên request được bảo vệ, nên token chưa hết hạn vẫn bị chặn nếu account bị khóa/soft-delete.
- `AuthorizationService.requirePermission` kiểm tra permission từ database tại service layer. UI ẩn thao tác không đủ quyền, nhưng backend vẫn là lớp quyết định.
- `auth_audit_events` là bảng append-only; login bất thường và access denied chỉ ghi ID, reason code và correlation ID, không ghi password/token/email đầy đủ.

Frontend giữ access token trong biến memory của `frontend/lib/auth-session.ts`; token mất khi reload và được khôi phục qua refresh cookie. Điều này tránh lưu access token trong `localStorage`.

## Ticket 05 — Refresh rotation và logout

### Nó làm gì

Duy trì phiên bằng refresh token xoay vòng, phát hiện reuse và hỗ trợ logout một phiên hoặc mọi phiên.

### Triển khai trong code

- Refresh token là 32 byte ngẫu nhiên; database chỉ lưu hash, `family_id`, parent token, expiry và thông tin revoke.
- Family lifetime cố định 30 ngày. Rotation giữ nguyên expiry tuyệt đối của family thay vì kéo dài vô hạn.
- `RefreshTokenPersistenceService.rotate` khóa user và token row. Token hợp lệ bị đánh dấu `ROTATED`, sau đó token con được tạo trong cùng transaction.
- Nếu token đã rotation bị dùng lại, repository thu hồi toàn bộ family với reason `REUSE_DETECTED`.
- Refresh token nằm trong cookie `HttpOnly`, `SameSite=Strict`, path `/api/auth`; `Secure` bật bằng biến môi trường ở HTTPS.
- `authenticatedFetch` thử request bằng access token, khi nhận 401 chỉ chạy một refresh promise dùng chung rồi retry. Refresh thất bại xóa access token memory.
- Logout current revoke đúng token hash; logout-all revoke tất cả token active của current user.

Migration `004` tạo `refresh_tokens` và index theo family/user/parent để rotation và revoke không cần scan toàn bảng.

## Ticket 06 — Khóa tài khoản và thu hồi phiên

### Nó làm gì

Cho actor có `USER_MANAGE` khóa user và chặn mọi phiên tiếp theo.

### Triển khai trong code

- `AdminUserController` expose `POST /api/v1/admin/users/{userId}/lock`.
- `UserAccountAdministrationService` yêu cầu permission trước khi gọi persistence.
- `UserAccountLockPersistenceService` khóa row user, chuyển trạng thái `LOCKED`, revoke toàn bộ refresh token và ghi audit trong cùng luồng transaction.
- `ActiveAccountFilter` khiến access JWT cũ bị từ chối ngay ở request bảo vệ kế tiếp.
- API cố ý không có unlock vì SRS chưa định nghĩa rule phục hồi.

Migration `005` bổ sung actor/target cho audit để phân biệt người thực hiện và tài khoản bị tác động.

## API và frontend chính

| Chức năng | Endpoint backend | Frontend/BFF |
| --- | --- | --- |
| Register | `POST /api/v1/auth/register` | form/API auth |
| Verify/resend | `POST /api/v1/auth/email-verifications/*` | `/verify-email`, route proxy tương ứng |
| Login | `POST /api/v1/auth/login` | `/login`, `/api/auth/login` |
| Refresh/logout | `POST /api/v1/auth/refresh`, `logout`, `logout-all` | `auth-session.ts` và BFF auth routes |
| Lock user | `POST /api/v1/admin/users/{id}/lock` | backend API có bảo vệ permission |

## Docker và Mailpit

```powershell
docker compose up -d --build postgres mailpit backend frontend
docker compose ps
docker compose logs --tail 120 backend mailpit
```

Các lệnh này dựng PostgreSQL, SMTP local, API và UI; sau đó kiểm tra startup/migration và lỗi gửi mail. Do `backend` còn phụ thuộc MinIO trong Compose cuối, Compose có thể tự dựng thêm dependency đó.

Mailpit nhận SMTP tại hostname nội bộ `mailpit:1025`; UI hộp thư mở ở `http://localhost:8025`. Backend tích hợp qua `spring-boot-starter-mail` và `JavaMailSender`, nên production chỉ cần thay nhóm `SMTP_*`/`MAIL_FROM`, không đổi business service.

```powershell
docker compose build backend
docker compose up -d backend
curl.exe --fail http://localhost:8080/api/v1/health
```

Dùng sau thay đổi auth để compile/package backend, recreate service và kiểm tra Spring Security/Liquibase vẫn khởi động.

## Công nghệ bên ngoài

- **Spring Security OAuth2 Resource Server + Nimbus JOSE**: ký/verify JWT HS256 và dựng security filter chain.
- **BCrypt**: hash password với cost 12; cùng `PasswordEncoder` được tái sử dụng cho password-protected memory.
- **Spring Mail + Mailpit**: Spring Mail là adapter SMTP; Mailpit là SMTP development và web inbox, không phải nhà cung cấp production.
- **PostgreSQL row locking**: repository `findForUpdate` bảo vệ verification, login state, refresh rotation và account lock trước request đồng thời.

## Giới hạn và ý nghĩa bảo mật

- Token gốc chỉ tồn tại tại thời điểm phát hành/gửi; database lưu hash.
- Seed không tạo admin có password và không tự cấp role ADMIN cho user đăng ký.
- Không có OAuth flow, unlock account hoặc password reset trong phạm vi Foundation.
- Chưa có integration test theo quyết định dự án; hành vi runtime được build/start kiểm tra nhưng các race/auth matrix chưa có bằng chứng test tự động.

