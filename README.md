# SoulFlow Backend

Backend cung cấp REST API cho storefront và admin studio, đồng thời xử lý nghiệp vụ đơn hàng, thanh toán, shipping, AI và realtime notification.

## Tech stack

| Nhóm | Công nghệ | Vai trò |
| --- | --- | --- |
| Ngôn ngữ & framework | Java 17, Spring Boot 3.3.5 | API, dependency injection và cấu hình ứng dụng |
| Web API | Spring MVC, Jackson, Jakarta Validation | REST controller, JSON và kiểm tra dữ liệu đầu vào |
| Security | Spring Security, JJWT 0.11.5, BCrypt | JWT, phân quyền role, mã hóa mật khẩu |
| Đăng nhập ngoài | Google API Client, Google OAuth Client | Xác thực Google ID token |
| Database | Microsoft SQL Server 2022 | Lưu dữ liệu nghiệp vụ |
| ORM | Spring Data JPA, Hibernate, JdbcTemplate | Entity mapping, repository và SQL trực tiếp khi cần |
| Migration | Flyway | Version hóa schema database |
| Cache & token | Spring Cache, Spring Data Redis | Cache dữ liệu đọc nhiều và refresh token rotation |
| Object storage | MinIO SDK 9 | Lưu ảnh sản phẩm |
| Mapping | MapStruct, Lombok | DTO/entity mapping và giảm boilerplate |
| Realtime | Spring WebSocket + STOMP | Chat và notification realtime |
| AI | Spring AI 1.0.0, Ollama | Chatbot qua Ollama model service |
| Payment | VietQR, SePay webhook | QR chuyển khoản và xác nhận thanh toán |
| Shipping | Nominatim, OSRM | Geocode địa chỉ và driving distance |
| Build & deploy | Maven, Docker, Docker Compose | Build và chạy toàn hệ thống |

## Kiến trúc và design pattern

Backend theo layered architecture:

```text
HTTP request
  → Controller
  → Service interface
  → Service implementation
  → Repository
  → SQL Server
```

| Layer | Vị trí | Trách nhiệm |
| --- | --- | --- |
| Controller | `controllers/` | Nhận request, validate DTO, gọi service và trả HTTP response |
| DTO | `models/requests`, `models/responses` | Tách public API khỏi JPA entity |
| Service | `models/services`, `models/services/impl` | Business rule, transaction, quyền sở hữu, cache và integration |
| Repository | `models/repositories` | Truy cập SQL Server bằng Spring Data JPA/query chuyên biệt |
| Mapper | `models/mappers` | MapStruct chuyển `request ↔ entity ↔ response` |
| Config | `config/` | Security, Redis, MinIO, WebSocket và async executor |
| Exception handler | `exceptions/` | Chuẩn hóa API error response |

Luồng create/update chuẩn:

1. Controller nhận request DTO có validation.
2. Service kiểm tra rule nghiệp vụ, quyền và dữ liệu liên quan.
3. Mapper tạo entity mới hoặc cập nhật entity đã có.
4. Repository lưu thay đổi trong transaction.
5. Service xóa cache liên quan, map response DTO và trả về controller.

Controller không truy cập repository trực tiếp và không trả JPA entity ra public API.

## Cấu trúc thư mục backend

```text
soulflow-api/
├── src/main/java/com/poly/
│   ├── config/                 # Security, Redis, MinIO, WebSocket, async
│   ├── controllers/            # REST và WebSocket controllers
│   ├── exceptions/             # Global exception handler
│   ├── models/
│   │   ├── entities/           # JPA entities
│   │   ├── mappers/            # MapStruct mappers
│   │   ├── repositories/       # Spring Data repositories
│   │   ├── requests/           # DTO đầu vào
│   │   ├── responses/          # DTO đầu ra
│   │   └── services/           # Service interface và implementation
│   ├── seeding/                # Dữ liệu khởi tạo
│   └── utils/                  # JWT và tiện ích dùng chung
└── src/main/resources/
    ├── application.properties
    └── db/migration/           # Flyway migrations
```

## Các tính năng nổi bật

### 1. Authentication, refresh token và phân quyền

**Mục tiêu.** Đăng nhập username/password hoặc Google; access token nằm trong HTTP-only cookie và backend quyết định quyền `ADMIN`/`USER`.

#### Setup

- Dependencies: Spring Security, `jjwt-*`, BCrypt và Google OAuth libraries.
- `SecurityConfig` đăng ký `JwtFilter`, `AuthenticationProvider` và route authorization.
- `FRONTEND_URL` phải chứa frontend origins được CORS cho phép.
- Redis lưu refresh token để hỗ trợ rotation và revoke.
- Khi deploy HTTPS, cấu hình `APP_AUTH_COOKIE_SECURE=true`.

#### Luồng xử lý

1. Client gọi `POST /login` hoặc `POST /google/login`.
2. `AccountService` xác thực password hoặc Google ID token và lấy roles.
3. `JwtUtil` tạo access JWT; service tạo refresh token.
4. `NonUserController` trả `account_token` và `refresh_token` dưới dạng HTTP-only cookies.
5. `JwtFilter` đọc cookie/Bearer token, validate JWT và đặt authorities vào `SecurityContext`.
6. Spring Security cho phép hoặc từ chối `/admin/**`, `/user/**` theo role.
7. `POST /auth/refresh` rotate token; `POST /auth/logout` revoke refresh token và xóa cookies.

### 2. Catalog và ảnh sản phẩm với MinIO

**Mục tiêu.** Admin quản lý category, product, discount và ảnh product; file ảnh nằm trong object storage thay vì database.

#### Setup

- Dependency: `io.minio:minio`.
- Docker chạy service `minio` và MinIO Console.
- Các biến cấu hình:

```dotenv
MINIO_URL=http://minio:9000
MINIO_PUBLIC_URL=http://localhost:9000
MINIO_ACCESS_KEY=...
MINIO_SECRET_KEY=...
```

- `MinioConfig` tạo `MinioClient`; metadata ảnh nằm trong SQL Server, binary object nằm trong bucket `flower-shop`.

#### Luồng xử lý

1. Admin gửi multipart request tạo/cập nhật product.
2. Product service validate dữ liệu và chuyển file đến `ImageService`.
3. `ImageService` tạo object key, upload binary lên MinIO và trả image metadata.
4. Product/product-image rows được lưu trong SQL Server.
5. Mapper tạo public URL từ `MINIO_PUBLIC_URL` để frontend hiển thị.
6. Mutation xóa cache product/list/page liên quan.

### 3. Cache, async filter và phân trang

**Mục tiêu.** Tối ưu filter cho account, product, category, order, cart, comment, reply, discount và store.

#### Setup

- Dependencies: Spring Cache và Spring Data Redis.
- Docker Redis dùng AOF persistence và `allkeys-lru` eviction policy.
- Cấu hình executor:

```dotenv
REDIS_HOST=redis
REDIS_CONTAINER_PORT=6379
FILTER_EXECUTOR_CORE_POOL_SIZE=2
FILTER_EXECUTOR_MAX_POOL_SIZE=4
FILTER_EXECUTOR_QUEUE_CAPACITY=50
```

- `@EnableCaching`, `@EnableAsync`, `@Cacheable` và `FilterAsyncService` được dùng cho page result.

#### Luồng xử lý

1. Client gửi filter, sort, page number và page size.
2. Service tạo cache key từ toàn bộ filter.
3. Cache hit trả `PageResponse` từ Redis.
4. Cache miss chạy query trong `filterTaskExecutor` và trả `CompletableFuture<PageResponse<T>>`.
5. Result được cache trước khi trả client.
6. Create/update/delete xóa cache liên quan để không trả dữ liệu cũ.

### 4. Store selection, order và shipping fee

**Mục tiêu.** Đơn hàng dùng store đã chọn, địa chỉ customer và driving distance để tính shipping fee.

#### Setup

- Flyway migration tạo store, quan hệ store/order và `shipping_fee`.
- Selected store được giữ trong backend session.
- Hosted API configuration:

```dotenv
NOMINATIM_BASE_URL=https://nominatim.openstreetmap.org
OSRM_BASE_URL=https://router.project-osrm.org
```

- `ShippingFeeService` cache kết quả theo cặp địa chỉ.

#### Luồng xử lý

1. Admin chọn store, frontend gọi API và backend lưu selection vào session.
2. Customer tạo order; `OrderService` lấy store address và delivery address.
3. Nominatim geocode hai địa chỉ thành coordinates.
4. OSRM trả driving distance theo mét.
5. Shipping fee áp dụng: 2 km đầu là 13.000 VND; mỗi km tiếp theo cộng 5.000 VND.
6. Backend cộng item subtotal và shipping fee vào total, rồi lưu order, items và payment.

### 5. VietQR và SePay webhook

**Mục tiêu.** Customer thanh toán QR; backend chỉ xác nhận paid sau webhook được validate.

#### Setup

- `QrService` tạo VietQR image URL, không cần SDK riêng.
- SePay webhook endpoint: `POST /webhook/sepay`.
- Cấu hình:

```dotenv
SEPAY_WEBHOOK_API_KEY=...
SEPAY_ACCOUNT_NUMBER=...
```

- Expose webhook bằng HTTPS domain/tunnel khi chạy ngoài local machine.
- Flyway `V2__sepay_webhook_receipts.sql` lưu receipt để chống xử lý transaction trùng.

#### Luồng xử lý

1. Customer tạo E_BANKING order tại `POST /checkout/orders`.
2. Backend lưu order `PENDING`, payment `paid=false`, tạo reference `DH<orderId>` và QR URL.
3. Customer quét QR, chuyển khoản với nội dung chứa reference.
4. SePay gọi webhook có `Authorization` API key.
5. `SepayWebhookService` kiểm tra API key, tài khoản nhận, incoming transaction, amount, reference, trạng thái order và idempotency.
6. Trong transaction, backend cập nhật payment paid, order `PAID` và outcome receipt.
7. Sau commit, order cache bị xóa và WebSocket phát order update.
8. Frontend poll order detail khi pending; database là source of truth cho payment state.

### 6. AI chatbot với Ollama

**Mục tiêu.** Cung cấp chatbot bằng model chạy thông qua Ollama.

#### Setup

- Dependency: `spring-ai-starter-model-ollama`, Spring AI BOM `1.0.0`.
- Docker gồm `ollama` và `ollama-init`; init pull model sau khi Ollama healthy.

```dotenv
OLLAMA_BASE_URL=http://ollama:11434
OLLAMA_CHAT_MODEL=qwen2.5:0.5b
AI_REQUEST_TIMEOUT=125s
AI_EXECUTOR_CORE_POOL_SIZE=2
AI_EXECUTOR_MAX_POOL_SIZE=4
```

#### Luồng xử lý

1. Client gửi prompt tới `AiController`.
2. Controller chuyển request tới AI service thay vì gọi Ollama trực tiếp.
3. Service dùng executor và timeout phù hợp để không giữ request thread quá lâu.
4. Spring AI gọi Ollama, nhận generated response và trả về client.

### 7. WebSocket chat và persistent notification

**Mục tiêu.** User online nhận chat/order update realtime; user offline vẫn đọc notification sau khi quay lại.

#### Setup

- Dependency: `spring-boot-starter-websocket`.
- `WebSocketConfig` bật STOMP endpoint `/ws`.
- Flyway `V3__notifications.sql` tạo notification table chung.
- Notification service lưu database trước khi publish realtime event.

#### Luồng xử lý

1. Chat client publish tới `/chat.sendMessage`.
2. `WebSocketChatController` broadcast message đến subscriber.
3. Order create/update/paid tạo notification record.
4. `OrderNotificationWebSocketController` publish event cho client đang online.
5. Offline recipient không nhận event ngay nhưng notification vẫn ở SQL Server.
6. Notification API trả record chưa đọc khi user/admin quay lại.

## Database migration

| File | Nội dung |
| --- | --- |
| `V1__create_initial_schema.sql` | Schema account, role, catalog, cart, order, payment, store và dữ liệu nền |
| `V2__sepay_webhook_receipts.sql` | Receipt/audit và idempotency cho SePay webhook |
| `V3__notifications.sql` | Persistent notification cho user/admin |

Không sửa migration đã chạy trên shared/production database. Tạo migration version mới cho mọi schema change.

## Chạy backend local bằng Docker

Tạo `.env`, không commit secret:

```dotenv
DB_PASSWORD=...
SPRING_DATASOURCE_URL=jdbc:sqlserver://sqlserver:1433;databaseName=flower_shop;encrypt=true;trustServerCertificate=true
SPRING_DATASOURCE_USERNAME=sa
REDIS_HOST=redis
REDIS_CONTAINER_PORT=6379
MINIO_URL=http://minio:9000
MINIO_PUBLIC_URL=http://localhost:9000
MINIO_ACCESS_KEY=...
MINIO_SECRET_KEY=...
OLLAMA_BASE_URL=http://ollama:11434
OLLAMA_CHAT_MODEL=qwen2.5:0.5b
FRONTEND_URL=http://localhost:3000
SEPAY_WEBHOOK_API_KEY=...
SEPAY_ACCOUNT_NUMBER=...
```

```bash
# Development với hot reload
docker compose up -d

# Theo dõi API
docker compose logs -f api

# Dừng services
docker compose down
```

Xem `soulflow-api/LAPTOP_DEPLOYMENT.md` để chạy production image.

---
