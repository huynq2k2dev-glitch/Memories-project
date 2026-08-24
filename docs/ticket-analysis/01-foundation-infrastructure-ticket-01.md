# Ticket 01 — Nền tảng và hạ tầng tối thiểu

## Ticket làm gì

Ticket 01 tạo lát cắt chạy được từ browser đến Next.js, Spring Boot và PostgreSQL. Mục tiêu không phải nghiệp vụ người dùng mà là một baseline ổn định: schema do Liquibase quản lý, Hibernate chỉ kiểm tra schema, API có health endpoint, lỗi nhất quán và mỗi request có correlation ID.

Luồng health hiện tại:

```text
Browser
  → GET /api/platform-health (Next.js route)
  → GET /api/v1/health (Spring Boot)
  → SELECT 1 (PostgreSQL)
  → { status: "UP", database: "UP" }
```

## Cách triển khai trong code và ý nghĩa

- `backend/src/main/java/com/memories/platform/health` chứa `HealthController`, `HealthService` và `HealthResponse`. `HealthService` dùng `JdbcClient` chạy `select 1`, vì vậy health không chỉ chứng minh HTTP server sống mà còn chứng minh datasource đang truy cập được database.
- `frontend/app/api/platform-health/route.ts` là BFF route. Nó gọi backend qua `BACKEND_URL`, dùng `cache: no-store`, chuyển tiếp correlation ID và trả `503` có payload an toàn khi backend không truy cập được.
- `backend/src/main/resources/application.yml` đặt `ddl-auto: validate`, `open-in-view: false` và `hibernate.jdbc.time_zone: UTC`. Liquibase là nguồn tạo schema duy nhất; Hibernate không tự sửa database.
- `PlatformApplication` dùng Spring Boot và bật các cấu hình theo module. Entity dùng UUID do application sinh và thời gian nghiệp vụ dùng `Clock.systemUTC()`/`Instant`.
- `CorrelationIdFilter` chỉ chấp nhận UUID đúng chuẩn từ `X-Correlation-Id`; giá trị thiếu hoặc sai được thay bằng UUID mới. ID được đưa vào response header và SLF4J MDC để nối log của cùng request.
- `ApiExceptionHandler` và `SecurityProblemWriter` trả RFC Problem Details có `code` và `correlationId`. `instance` cố định ở `/api/v1`, tránh phản chiếu slug, token hoặc URL nhạy cảm từ request.
- REST controller trả DTO thay vì JPA entity. Việc tắt Open Session in View buộc service/repository phải tải dữ liệu cần thiết trong transaction, tránh lazy query phát sinh từ tầng web.
- `backend/Dockerfile` build JAR bằng Maven/Java 21 rồi chạy bằng JRE image nhỏ hơn. `frontend/Dockerfile` build và chạy Next.js. Hai Dockerfile tách build stage khỏi runtime stage.

## Docker Compose và mục đích từng service

`compose.yml` là cấu hình local cuối cùng của Foundation:

| Service | Mục đích | Cổng mặc định |
| --- | --- | --- |
| `postgres` | PostgreSQL 16, lưu schema và toàn bộ dữ liệu nghiệp vụ | `5432` |
| `backend` | Spring Boot API; đợi PostgreSQL khỏe và các dependency local sẵn sàng | `8080` |
| `frontend` | Next.js UI/BFF; gọi backend qua hostname nội bộ `backend` | `3000` |
| `mailpit` | SMTP giả lập và hộp thư web cho email xác thực local | `1025`, `8025` |
| `minio` | Object storage tương thích S3 cho media local | `9000`, `9001` |
| `minio-init` | Job một lần tạo bucket bằng MinIO Client | không expose |

PostgreSQL và MinIO có healthcheck. Backend dùng `depends_on` để không khởi động trước database hoặc bucket. Volume `postgres-data` và `minio-data` giữ dữ liệu khi container được tạo lại.

## Các lệnh Docker đã dùng

```powershell
docker compose up -d --build
```

Build image backend/frontend và khởi động toàn bộ stack ở background. Đây là lệnh dựng môi trường đầy đủ từ source hiện tại.

```powershell
docker compose ps
docker compose logs --tail 120 backend
```

Lệnh đầu kiểm tra trạng thái, health và port; lệnh sau xem Liquibase, Hibernate và quá trình Spring Boot startup mà không dump toàn bộ log.

```powershell
curl.exe --fail http://localhost:8080/api/v1/health
curl.exe --fail http://localhost:3000/api/platform-health
```

Xác minh riêng backend và toàn bộ đường đi qua frontend BFF.

```powershell
docker compose config --quiet
docker compose build backend
docker compose build frontend
```

Kiểm tra cú pháp Compose và build độc lập từng image. Dockerfile backend dùng `mvn -DskipTests package`; đây là compile/package, không chạy test.

## Công nghệ bên ngoài và cách tích hợp

- **PostgreSQL 16**: driver runtime `org.postgresql:postgresql`; datasource lấy URL/user/password từ biến môi trường. Dùng PostgreSQL thật để giữ đúng hành vi JSONB, partial index và locking.
- **Liquibase**: `liquibase-core` đọc `db/changelog/db.changelog-master.yaml` khi backend khởi động. Mỗi thay đổi schema là changeset mới có rollback.
- **Spring Boot 3.5 / Java 21**: Web, Validation, Data JPA, Security và Mail là các starter chính. Spring quản lý controller/service/repository và transaction.
- **Next.js 16 / React 19 / TypeScript**: UI và BFF route cùng một ứng dụng; backend URL nội bộ không bị đưa trực tiếp cho browser.
- **Docker Compose**: tạo network DNS nội bộ, dependency order, port mapping và volume local. Secret mặc định trong Compose chỉ dành cho development; môi trường thật phải truyền biến môi trường an toàn.

## Giới hạn có chủ ý

- Health endpoint chỉ kiểm tra kết nối database, không thay thế readiness sâu cho SMTP hoặc object storage.
- Không dùng H2 và không cho Hibernate `create/update` schema.
- Ticket này đặt nền tảng module; các rule auth, memory và media nằm ở ticket sau.

