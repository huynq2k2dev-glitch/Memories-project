# Ticket 16–17 — Cộng tác và vòng đời memory

## Ticket 16 — Cộng tác trên memory

### Nó làm gì

Cho owner cấp quyền cho tài khoản đã tồn tại và cho collaborator làm việc theo ba mức `VIEW`, `EDIT`, `ADMIN`.

### Ma trận quyền đã triển khai

| Hành động | Owner | VIEW | EDIT | ADMIN |
| --- | --- | --- | --- | --- |
| Xem dữ liệu quản trị/preview | Có | Có | Có | Có |
| Sửa content draft, upload/gắn ảnh | Có | Không | Có | Có |
| Publish | Có | Không | Không | Có |
| Quản lý collaborator/share link | Có | Không | Không | Có |
| Đổi visibility/password | Có | Không | Không | Không |
| Quản lý guest | Có | Không | Không | Không |
| Archive/soft-delete | Có | Không | Không | Không |

Hai điểm mơ hồ trong SRS được chốt bảo thủ: EDIT không publish; ADMIN không archive/delete/đổi owner hoặc access policy.

### Triển khai trong code

- Migration `014-memory-collaborators.sql` tạo quan hệ unique `(memory_id,user_id)`, permission/status/revoked time và index theo user/status.
- `MemoryCollaboratorController` expose list/add/change permission/revoke dưới `/api/v1/memories/{memoryId}/collaborators`.
- `MemoryCollaborationPersistenceService.add` yêu cầu capability manage, tìm account `ACTIVE` bằng email chuẩn hóa, từ chối actor/owner và khóa quan hệ cũ.
- Nếu quan hệ cũ là `REVOKED`, add sẽ reactivate cùng row thay vì tạo duplicate. Quan hệ active trùng trả conflict.
- List bulk-load account summary theo tập user ID, tránh query account cho từng collaborator.
- `MemoryAccessService` là điểm tập trung tính `MemoryCapabilitiesResponse`. Các service member/section/schedule/media/publish gọi lại service này, không tin nút UI.
- Repository luôn ghép memory ID với child/collaborator ID, hạn chế IDOR khi đổi UUID.
- Add/change/revoke ghi audit append-only bằng actor ID, target user ID, memory ID, permission và correlation ID; không lưu email vào metadata.

Frontend `memory-collaborator-editor.tsx` hiển thị đúng action theo capability, nhưng backend vẫn kiểm tra lại ở transaction.

## Ticket 17 — Archive và soft-delete

### Nó làm gì

Cho owner dừng public access bằng archive hoặc đánh dấu memory đã xóa mà chưa hard-delete child/object.

### Triển khai trong code

- Cột `status`, `deleted_at` và các index cần thiết đã có từ migration `007`, nên Ticket 17 không cần tạo bảng mới.
- `MemoryLifecyclePersistenceService.archive` yêu cầu owner và expected version; DRAFT/PUBLISHED có thể chuyển `ARCHIVED`, còn archive lặp lại trả conflict.
- `softDelete` ghi `deleted_at`, `updated_by`, `updated_at`; không cascade xóa application data.
- `MemoryRepository` và access service dùng query `deletedAtIsNull`; public query còn yêu cầu `PUBLISHED`, chưa expired và visibility phù hợp.
- Sau commit archive/delete, public payload bị chặn ngay. Dữ liệu child và object storage vẫn giữ để một policy restore/purge tương lai xử lý.
- Chỉ owner có `canArchive/canDelete`; collaborator ADMIN cũng bị từ chối.
- Success/denied/failure được ghi vào `audit_logs` với correlation ID; failure audit dùng transaction cô lập.

Frontend `memory-lifecycle-editor.tsx` gửi optimistic version, yêu cầu xác nhận trước thao tác destructive và cập nhật trạng thái UI từ response.

## Ý nghĩa thiết kế

- Permission là capability theo use case, không phải một annotation chung áp cho mọi endpoint. Cách này giữ rõ ngoại lệ owner-only.
- Revoke collaborator chặn request mới ngay vì quyền được đọc lại từ database, không nhúng cố định trong JWT.
- Soft-delete giữ dữ liệu nhưng mọi repository nghiệp vụ phải lọc rõ; hard purge không bị lẫn vào request người dùng.

## Docker đã dùng

```powershell
docker compose build backend
docker compose build frontend
docker compose up -d backend frontend
docker compose logs --tail 120 backend
curl.exe --fail http://localhost:8080/api/v1/health
```

Các lệnh build cả capability/UI và kiểm tra migration `014`, Spring context, Hibernate validate. PostgreSQL giữ trạng thái permission/revoke để kiểm tra quyền ở request kế tiếp.

## Công nghệ liên quan

- **Spring transaction và PostgreSQL row lock**: add/reactivate/change/revoke không tạo quan hệ trùng trong request đồng thời.
- **Spring Data JPA projections/bulk query**: lấy account summary cho danh sách collaborator mà không lộ entity auth sang module memory.
- **Audit append-only trigger**: database từ chối update/delete audit row; service chỉ insert event.

## Giới hạn

- Không có chuyển owner, restore memory hoặc hard purge.
- Không có lời mời collaborator cho email chưa có account.
- Authorization matrix chưa có integration test tự động theo chính sách dự án.

