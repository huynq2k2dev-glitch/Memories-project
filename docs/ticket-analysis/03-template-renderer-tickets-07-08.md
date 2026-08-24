# Ticket 07–08 — Template version và frontend renderer

## Ticket 07 — Quản trị template và phiên bản render

### Nó làm gì

Cho admin có `TEMPLATE_MANAGE` quản lý metadata template và lifecycle của version. Template version là hợp đồng dữ liệu giữa backend và frontend, không phải nơi lưu HTML/CSS/JavaScript tùy ý.

### Cách triển khai

- Migration `006-template-catalog.sql` tạo `templates` và `template_versions`, unique code/version, JSONB contract và các check/index cần thiết.
- `AdminTemplateController` cung cấp list/create/update template; create/update/publish/deprecate version.
- `TemplateAdministrationService` kiểm tra permission và ghi audit; `TemplateAdministrationPersistenceService` chứa transaction và row lock.
- Code template bất biến sau khi tạo. `version_no` do backend lấy `maximumVersionNumber + 1` trong transaction đã khóa template.
- Lifecycle version chỉ cho `DRAFT → PUBLISHED → DEPRECATED`. Chỉ DRAFT được sửa; publish lại là idempotent; version đã publish không có API delete.
- `TemplateContractValidator` kiểm tra renderer allowlist, hình dạng JSON, required section là tập con của section contract, và default config theo JSON Schema Draft 2020-12 trước publish.
- `coverRequired` là dữ liệu hợp đồng rõ ràng để publish memory biết có bắt buộc cover hay không.

Backend hiện allowlist renderer tương ứng registry frontend: `component_key=memories-basic-v1`, `renderer_version=1`. Điều này ngăn database chỉ định code chưa tồn tại trong build.

## Ticket 08 — Catalog và chọn template đã publish

### Nó làm gì

Cho user duyệt template khả dụng và chọn đúng một version đã publish để tạo memory.

### Cách triển khai

- `TemplateCatalogController` expose `GET /api/v1/templates` với `page`, `size`, `memoryType`, `status`.
- `TemplateCatalogService` chỉ truy vấn template `ACTIVE` có version `PUBLISHED`; page size tối đa 50.
- Catalog lấy page template trước, sau đó bulk-load version của toàn bộ ID trong page. Hai query cố định tránh N+1.
- DTO catalog chỉ trả default config, allowed/required sections và renderer key/version; không trả config schema quản trị.
- `TemplateSelectionService.selectForNewMemory` kiểm tra lại version `PUBLISHED` và template `ACTIVE` trong transaction tạo memory. UI nhìn thấy một version không đồng nghĩa backend tin version đó vẫn selectable.
- `frontend/templates/registry.tsx` ánh xạ cặp key/version sang React component đã compile. `RegisteredTemplateRenderer` trả null khi không tương thích; UI hiển thị lỗi thay vì tải code từ database.
- `memories-basic-v1.tsx` render payload typed gồm metadata, members, sections, locations, events và images; timezone event được format bằng `Intl.DateTimeFormat`.

Memory đã tạo giữ trực tiếp `template_version_id`. Khi version bị deprecated, memory cũ vẫn render được miễn frontend còn giữ renderer version tương ứng; chỉ việc chọn mới bị chặn.

## Code chính và ý nghĩa

| Thành phần | Vai trò |
| --- | --- |
| `template/controller` | Hợp đồng admin và catalog HTTP |
| `TemplateAdministrationPersistenceService` | Transaction, version lifecycle, DTO mapping |
| `TemplateContractValidator` | JSON Schema và renderer allowlist |
| `TemplateCatalogService` | Pagination/bulk query, loại draft/deprecated |
| `TemplateSelectionService` | Re-check tại thời điểm tạo memory |
| `frontend/templates/registry.tsx` | Registry code tĩnh trong build |
| `frontend/app/admin/templates` | UI quản trị template/version |
| `frontend/app/templates` | Catalog và editor memory |

## Docker đã dùng

```powershell
docker compose build backend
docker compose build frontend
docker compose up -d backend frontend
docker compose logs --tail 120 backend frontend
```

Build cả hai phía là cần thiết vì contract được kiểm tra ở backend nhưng renderer thật nằm trong frontend build. Sau khi dựng lại, backend chạy migration `006` và frontend bundle registry/component.

```powershell
docker compose ps
curl.exe --fail http://localhost:8080/api/v1/health
```

Kiểm tra stack ổn định sau khi thêm schema JSONB và module template.

## Công nghệ bên ngoài

- **networknt JSON Schema Validator 2.0.4**: compile và validate schema Draft 2020-12 ngay trong backend; không gọi dịch vụ ngoài mạng.
- **PostgreSQL JSONB**: lưu config schema/default config/section contract có cấu trúc, trong khi service giữ business validation phức tạp.
- **Next.js/React registry**: renderer là code tin cậy đã build; database chỉ chọn khóa và version.
- **Jackson `JsonNode`**: giữ JSON contract, `deepCopy` khi đi qua DTO/entity để tránh vô tình sửa chung object mutable.

## Giới hạn

- Chỉ có renderer `memories-basic-v1@1`; thêm renderer mới cần cập nhật allowlist backend và registry frontend cùng release.
- Không có custom HTML/CSS/JS template và không có drag-drop builder.
- Chưa có integration test chứng minh draft/deprecated selection; kiểm tra hiện tại dựa trên repository/service và build runtime.

