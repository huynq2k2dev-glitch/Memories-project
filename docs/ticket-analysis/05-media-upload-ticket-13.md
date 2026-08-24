# Ticket 13 — Upload trực tiếp và quản lý media

## Ticket làm gì

Ticket 13 cho browser upload ảnh thẳng lên object storage bằng presigned PUT. Backend chỉ phát quyền upload ngắn hạn, lưu metadata, xác minh object sau upload và cho gắn asset khi trạng thái là `READY`.

```text
Browser → POST initiate → Backend tạo MediaAsset(UPLOADING)
Browser ← presigned PUT URL + required headers
Browser → PUT binary trực tiếp → MinIO/S3
Browser → POST complete → Backend HEAD + đọc magic bytes
Backend → MediaAsset(READY)
Browser → gắn asset vào memory/section/member/cover
```

Binary không đi qua Spring Boot, giảm memory/bandwidth backend và giữ API không phụ thuộc kích thước file upload.

## Triển khai trong code và ý nghĩa

### Schema và entity

- Migration `011-media-assets.sql` tạo `media_assets` và `memory_images`, unique storage key, trạng thái upload, kích thước/dimension checks và index quota.
- Asset có owner, provider, bucket, object key, MIME, size, checksum, upload expiry, trạng thái và soft-delete. Database không lưu public/presigned URL lâu dài.
- `memory_images` liên kết asset vào memory/section; bỏ liên kết chỉ xóa row liên kết, không xóa object.
- Cover và avatar chỉ giữ asset UUID; migration `020` chuẩn hóa FK thành `SET NULL` khi reference asset bị xóa vật lý trong một quy trình tương lai.

### Khởi tạo và hoàn tất upload

- `MemoryMediaUploadService` yêu cầu actor có quyền sửa memory rồi mới gọi `MediaUploadService.initiate`.
- MIME cho phép: JPEG, PNG, WebP, AVIF; tối đa 10 MiB. SVG không nằm trong allowlist.
- Quota hiện tại là 200 asset hoặc 1 GiB theo owner asset, tính cả `UPLOADING` và `READY`.
- Frontend tính SHA-256 bằng Web Crypto, gửi metadata tới backend rồi PUT `File` trực tiếp vào `uploadUrl`.
- `MediaObjectStorageService.presignUpload` ký Content-Type, Content-Length và checksum nếu có; TTL mặc định 10 phút.
- Khi complete, backend chạy HEAD kiểm tra size/MIME/checksum, sau đó chỉ đọc 64 byte đầu để nhận diện magic bytes JPEG/PNG/WebP/AVIF. Sai metadata chuyển asset sang `FAILED`.
- `MediaAssetAccessService.requireReadyOwned` bảo đảm asset READY và thuộc actor trước khi gắn. `MemoryImageService` còn kiểm tra section thuộc cùng memory.
- Delivery URL chỉ được tạo ngắn hạn khi cần. Public payload dùng URL tương đối `/api/v1/public/media/{assetId}` để backend kiểm tra lại quyền memory trước khi redirect tới presigned GET.

`frontend/app/templates/memory-media-editor.tsx` thực hiện đúng chuỗi initiate → PUT storage → complete → attach. Nó cũng quản lý cover, avatar, section image và optimistic version.

## Tích hợp MinIO/S3

### MinIO local

`compose.yml` chạy `minio/minio` với:

- API S3: `http://localhost:9000` cho browser/presigner public endpoint.
- Console: `http://localhost:9001` cho vận hành local.
- Endpoint nội bộ `http://minio:9000` cho backend trong Docker network.
- Volume `minio-data` giữ object.

`minio-init` dùng image MinIO Client và chạy `infra/minio/init-minio.sh`. Script cấu hình alias rồi `mc mb --ignore-existing`, nên bucket được tạo idempotent mỗi lần dựng stack.

### AWS SDK for Java

`MediaStorageConfiguration` tạo hai client:

- `S3Client` dùng internal endpoint để HEAD/GET object.
- `S3Presigner` dùng public endpoint để URL trả cho browser truy cập được từ máy host.

Cả hai dùng AWS SDK S3 v2, path-style access và static credentials từ cấu hình. Vì MinIO tương thích S3, production có thể đổi provider/endpoint/bucket/credentials qua `MEDIA_STORAGE_*` mà không đổi API nghiệp vụ.

## Các lệnh Docker đã dùng

```powershell
docker compose up -d --build minio minio-init backend frontend
docker compose ps
docker compose logs --tail 120 minio minio-init backend
```

Khởi động storage, chạy job tạo bucket, dựng lại API/UI và kiểm tra MinIO ready cùng kết quả init. Compose tự kéo thêm dependency của backend nếu cần.

```powershell
docker compose build backend
docker compose build frontend
docker compose up -d backend frontend
curl.exe --fail http://localhost:8080/api/v1/health
```

Xác nhận AWS SDK/config compile, migration `011` chạy và ứng dụng kết nối PostgreSQL sau khi tích hợp media.

Không cần dùng `docker exec` để tạo bucket thủ công; `minio-init` và script được version-control thực hiện việc đó nhất quán.

## Công nghệ bên ngoài

- **MinIO**: object storage local tương thích S3, phục vụ test thủ công luồng presigned URL.
- **MinIO Client (`mc`)**: job bootstrap bucket idempotent.
- **AWS SDK S3 v2 2.53.2**: ký URL, HEAD, range GET và tạo delivery URL; code không phụ thuộc SDK riêng của MinIO.
- **Web Crypto API**: frontend tính SHA-256 trước upload mà không gửi binary qua backend.

## Giới hạn và rủi ro còn mở

- Retention/purge object sau soft-delete chưa được định nghĩa; code không hard-delete object khi bỏ liên kết.
- CORS và credential production phải được cấu hình theo domain thật; giá trị Compose chỉ dành cho local.
- Checksum chỉ so khi storage trả checksum; magic-byte check luôn chạy để không chỉ tin Content-Type.
- Chưa có integration test/end-to-end upload theo quyết định không-test.

