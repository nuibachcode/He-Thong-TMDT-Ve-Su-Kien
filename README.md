# TicketGo — Hệ thống TMDT vé sự kiện & tour du lịch tích hợp AI/RAG

> Kiến trúc microservices theo báo cáo Nhóm 11 · **Chạy toàn hệ thống bằng Docker Compose**
> Spring Boot 3.5 WebFlux · API Gateway · PostgreSQL + pgvector · MongoDB · Redis · RabbitMQ · React 19 + Vite

## 1. Chạy trong một lệnh

Yêu cầu: **Docker Desktop** đang chạy.

```powershell
# (tuỳ chọn) tạo file cấu hình riêng
Copy-Item .env.example .env

# build và chạy toàn bộ hệ thống
docker compose up -d --build
```

Mở:

| Thành phần | URL |
|---|---|
| Ứng dụng (frontend) | http://localhost:5173 |
| API Gateway | http://localhost:8080 |
| AI service (debug) | http://localhost:8082 |
| RabbitMQ Management | http://localhost:15672 (`tmdt` / `tmdt123`) |
| PostgreSQL | `localhost:5432` (db `core_db`, `ai_db`) |
| MongoDB | `localhost:27017` (db `ai`) |
| Redis | `localhost:6379` |

Kiểm tra nhanh:

```powershell
Invoke-RestMethod http://localhost:8080/api/health        # core-api qua gateway
Invoke-RestMethod http://localhost:8080/api/ai/health     # AI service
```

Dừng / xoá dữ liệu:

```powershell
docker compose down          # dừng, giữ dữ liệu
docker compose down -v       # dừng và xoá volume (reset DB)
```

**Tài khoản demo**

| Email | Mật khẩu | Vai trò |
|---|---|---|
| `customer@demo.vn` | `User@12345` | CUSTOMER |
| `organizer@demo.vn` | `Org@12345` | ORGANIZER |
| `admin@demo.vn` | `Admin@123` | ADMIN |

Voucher demo: `WELCOME10` (giảm 10%, tối đa 300k), `TOUR500` (giảm 500k cho tour),
`HETHONG` (mã hết hạn — dùng để demo lỗi).

## 2. Kiến trúc

```
React + Vite (nginx) ──/api──▶ API Gateway :8080
                                  │ JWT + CORS + định tuyến
                  ┌───────────────┴────────────────┐
             /api/ai/**                      các /api/** khác
                  │                                │
            AI Service :8082                core-api :8081
        RAG + recommendation            Identity/Catalog/Inventory/
        pgvector · MongoDB ·            Order/Payment (composite,
        Redis · RabbitMQ                đang tách dần)
```

Chi tiết: [docs/09-kien-truc-microservices.md](docs/09-kien-truc-microservices.md).

| Thành phần | Vai trò | Công nghệ |
|---|---|---|
| `services/gateway` | Định tuyến, xác thực JWT, CORS, phân quyền thô | Spring Cloud Gateway (WebFlux) |
| `services/ai` | Chat RAG, embedding, gợi ý, feedback, metrics | Spring Boot 3.5 WebFlux, R2DBC, Mongo, Redis, AMQP |
| `backend` (core-api) | Nghiệp vụ mua vé, catalog, organizer, admin | Spring Boot, JPA, PostgreSQL |
| `frontend` | Giao diện người dùng + chat + gợi ý | React 19, Vite, TypeScript, Tailwind |
| `infra/postgres/init.sql` | Tạo `core_db`, `ai_db`, extension pgvector | PostgreSQL 16 |

`core-api` là composite service tạm thời để demo end-to-end; khi tách Identity/Catalog/Inventory/Order/Payment
thành service riêng, chỉ cần thêm route trong gateway.

## 3. AI/RAG

- Dữ liệu mẫu: 4 sự kiện, 5 tour, 4 tài liệu chính sách (hoàn/hủy, thanh toán, vé QR, trẻ em/hành lý).
- Pipeline: ingest → chunk 1.400 ký tự → embedding → pgvector top-k → prompt + guardrail → trả lời kèm citation.
- `AI_PROVIDER=mock` (mặc định): embedding lexical + trả lời trích xuất, **không cần Internet/quota**.
- `AI_PROVIDER=gemini` + `GEMINI_API_KEY=...`: dùng `gemini-embedding-001` (768 chiều) và `gemini-2.5-flash`.
- Citation có `url` để frontend mở thẳng trang sự kiện/tour.
- Redis cache embedding + câu trả lời; rate limit 20 tin/phút; guardrail từ chối câu ngoài phạm vi.
- Gợi ý theo sở thích đã lưu (`PUT /api/ai/interests`) hoặc độ phổ biến.

Endpoint chính:

| Method | Path | Ghi chú |
|---|---|---|
| POST | `/api/ai/chat` | `{sessionId?, message}` |
| GET | `/api/ai/recommendations?limit&interests` | trộn sự kiện + tour |
| GET/PUT | `/api/ai/interests` | cần đăng nhập |
| POST | `/api/ai/feedback` | `{messageId, rating}` |
| POST | `/api/ai/reindex` | ADMIN |
| GET | `/api/ai/metrics` | ADMIN |
| GET | `/api/ai/health` | trạng thái + provider + số chunk |

Ví dụ câu hỏi demo:

- “Huỷ tour trước 5 ngày có được hoàn tiền không?”
- “Tour Phú Quốc có bao gồm vé máy bay không?”
- “Tour Đà Lạt 3 ngày và 4 ngày khác nhau thế nào?”
- “Hôm nay thời tiết Hà Nội thế nào?” → phải từ chối.

Bật Gemini:

```powershell
# sửa .env
AI_PROVIDER=gemini
GEMINI_API_KEY=...
docker compose up -d --build ai-service
```

## 4. Luồng demo 12 phút

1. Trang chủ → chọn sở thích → khu gợi ý đổi theo sở thích.
2. Xem sự kiện/tour → chọn hạng vé/lịch → áp voucher → đặt đơn (giữ chỗ 10 phút).
3. Thanh toán mô phỏng → “Vé của tôi” → xem QR.
4. Chat AI: câu hỏi chéo dữ liệu + câu ngoài phạm vi → xem citation mở đúng sự kiện/tour.
5. Organizer: tạo sự kiện/tour, xem doanh thu, dùng AI viết mô tả.
6. Admin: duyệt nội dung, khoá user, xem metrics AI.
7. Kỹ thuật: chứng minh chống oversell, rate limit AI, reindex.

## 5. Lệnh hữu ích

| Việc | Lệnh |
|---|---|
| Xem log | `docker compose logs -f` |
| Log một service | `docker compose logs -f ai-service` |
| Rebuild service | `docker compose up -d --build ai-service` |
| Test core | `cd backend; mvn test` |
| Build gateway/AI | `cd services/gateway; mvn -DskipTests package` |
| Build frontend | `cd frontend; npm run build` |
| Kiểm tra compose | `docker compose config --quiet` |
| Reindex AI | đăng nhập admin → `POST /api/ai/reindex` hoặc gửi message RabbitMQ `catalog.changed` |

## 6. Biến môi trường

Xem [.env.example](.env.example). Quan trọng:

| Biến | Ý nghĩa |
|---|---|
| `JWT_SECRET` | Bắt buộc ≥ 32 ký tự, dùng chung gateway/core/AI |
| `POSTGRES_*` | Tài khoản PostgreSQL |
| `AI_PROVIDER` | `mock` hoặc `gemini` |
| `GEMINI_API_KEY` | Key Gemini (không commit) |
| `CORS_ORIGINS` | Origin frontend được phép |
| `GATEWAY_PORT`, `FRONTEND_PORT` | Đổi cổng host nếu trùng |

## 7. Chạy dev không Docker (tuỳ chọn)

Vẫn có thể chạy nhanh core-api với H2 như bản cũ, nhưng **khuyến nghị Docker** để đúng kiến trúc:

```powershell
# hạ tầng
docker compose up -d postgres mongo redis rabbitmq

# core-api (H2 hoặc postgres profile) - chay o 8081 de khong trung gateway
cd backend; $env:SERVER_PORT='8081'; mvn spring-boot:run

# AI service
cd services/ai; mvn spring-boot:run

# gateway
cd services/gateway; mvn spring-boot:run

# frontend
cd frontend; npm install; npm run dev
```

## 8. Trạng thái và việc tiếp theo

Đã có:

- Hạ tầng Docker Compose (PostgreSQL/pgvector, MongoDB, Redis, RabbitMQ) + healthcheck.
- API Gateway JWT/CORS/định tuyến, chống giả mạo header.
- AI service WebFlux: RAG, citation có link, gợi ý theo sở thích, cache, rate limit, reindex, metrics.
- Frontend chat + khu gợi ý + Docker/nginx.
- core-api chạy PostgreSQL trong Docker, giữ nguyên luồng mua vé/QR/organizer/admin.

Tiếp theo (đúng báo cáo):

- Tách core-api thành Identity (PostgreSQL), Catalog (MongoDB), Inventory (PostgreSQL + Redis),
  Order (PostgreSQL), Payment (PostgreSQL).
- Adapter thanh toán VNPay/MoMo sandbox + callback.
- Saga đầy đủ qua RabbitMQ (command/reply, compensation).
- Bổ sung test tranh vé đồng thời và bộ 20 câu vàng cho RAG.

## 9. Ghi chú kỹ thuật

- Gateway/AI dùng Java 21, Spring Boot 3.5; core giữ Spring Boot 4 nhưng build target Java 21.
- PostgreSQL dùng `pgvector/pgvector:pg16`; extension `vector` tạo trong `ai_db`.
- R2DBC không bind trực tiếp kiểu `vector`; repository dựng literal số từ embedding (an toàn, không chứa input người dùng).
- Header `X-User-*` chỉ do gateway gắn sau khi xác thực; client gửi lên sẽ bị xoá.
- Guest chat dùng `X-Guest-Key` (UUID lưu localStorage) để giữ phiên và rate limit.
