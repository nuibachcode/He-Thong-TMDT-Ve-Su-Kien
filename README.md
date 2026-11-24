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
| PostgreSQL | `localhost:5432` (db `identity_db`, `inventory_db`, `order_db`, `payment_db`, `ai_db`) |
| MongoDB | `localhost:27017` (db `ai`) |
| Redis | `localhost:6379` |

Kiểm tra nhanh:

```powershell
Invoke-RestMethod http://localhost:8080/api/health        # gateway health
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
   ┌──────────┬──────────┬────────┼──────────┬──────────┬──────────┐
   ▼          ▼          ▼        ▼          ▼          ▼          ▼
Identity   Catalog   Inventory  Order     Payment   AI Service  (frontend)
 :8081      :8083      :8084    :8085      :8086      :8082
Postgres   MongoDB    PG+Redis  Postgres   Postgres   pgvector+Mongo+Redis
```

Chi tiết: [docs/09-kien-truc-microservices.md](docs/09-kien-truc-microservices.md).

| Thành phần | Vai trò | Công nghệ |
|---|---|---|
| `services/gateway` | Định tuyến, xác thực JWT, CORS, phân quyền thô | Spring Cloud Gateway (WebFlux) |
| `services/identity` | Tài khoản, đăng nhập, JWT, quản lý user | Spring Boot 3.5, JPA, PostgreSQL |
| `services/catalog` | Sự kiện, tour, tìm kiếm, duyệt nội dung | Spring Boot 3.5, MongoDB |
| `services/inventory` | Hạng vé, tồn kho, giữ chỗ Redis, vé QR, check-in | Spring Boot 3.5, JPA, Redis |
| `services/order` | Đơn hàng, voucher, vòng đời đơn, saga compensation | Spring Boot 3.5, JPA, RabbitMQ |
| `services/payment` | Cổng thanh toán sandbox, callback/idempotency | Spring Boot 3.5, JPA |
| `services/ai` | Chat RAG, embedding, gợi ý, feedback, metrics | Spring Boot 3.5 WebFlux, R2DBC, Mongo, Redis, AMQP |
| `frontend` | Giao diện người dùng + chat + gợi ý | React 19, Vite, TypeScript, Tailwind |
| `infra/postgres/init.sql` | Tạo database cho từng service + pgvector | PostgreSQL 16 |

`backend/` là monolith cũ, chỉ giữ để tham khảo; **không được start trong Docker Compose hiện tại**.

## 3. AI/RAG

- Dữ liệu mẫu: 4 sự kiện, 5 tour, 4 tài liệu chính sách (hoàn/hủy, thanh toán, vé QR, trẻ em/hành lý).
- Pipeline: ingest → chunk 1.400 ký tự → embedding → pgvector top-k → prompt + guardrail → trả lời kèm citation.
- `AI_PROVIDER=mock` (mặc định): embedding lexical + trả lời trích xuất, **không cần Internet/quota**.
- `AI_PROVIDER=gemini` + `GEMINI_API_KEY=...`: dùng `gemini-embedding-001` (768 chiều) và `gemini-3.5-flash`.
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
| `JWT_SECRET` | Bắt buộc ≥ 32 ký tự, dùng chung identity + gateway + AI |
| `POSTGRES_*` | Tài khoản PostgreSQL |
| `AI_PROVIDER` | `mock` hoặc `gemini` |
| `GEMINI_API_KEY` | Key Gemini (không commit) |
| `CORS_ORIGINS` | Origin frontend được phép |
| `GATEWAY_PORT`, `FRONTEND_PORT` | Đổi cổng host nếu trùng |

## 7. Chạy dev không Docker (tuỳ chọn)

Khuyến nghị dùng Docker. Nếu chạy local, bật hạ tầng trước rồi chạy từng service:

```powershell
docker compose up -d postgres mongo redis rabbitmq

cd services/identity;  $env:DB_HOST='localhost'; mvn spring-boot:run
cd services/catalog;   $env:MONGO_HOST='localhost'; $env:INVENTORY_URL='http://localhost:8084'; mvn spring-boot:run
cd services/inventory; $env:DB_HOST='localhost'; mvn spring-boot:run
cd services/order;     $env:DB_HOST='localhost'; mvn spring-boot:run
cd services/payment;   $env:DB_HOST='localhost'; mvn spring-boot:run
cd services/ai;        mvn spring-boot:run
cd services/gateway;   mvn spring-boot:run
cd frontend;           npm install; npm run dev
```

## 8. Trạng thái

Đã có:

- Hạ tầng Docker Compose (PostgreSQL/pgvector, MongoDB, Redis, RabbitMQ) + healthcheck.
- API Gateway JWT/CORS/định tuyến, chống giả mạo header.
- Identity (PostgreSQL), Catalog (MongoDB), Inventory (PostgreSQL + Redis), Order (PostgreSQL),
  Payment (PostgreSQL), AI (pgvector/Mongo/Redis/RabbitMQ) — mỗi service database riêng.
- Luồng mua vé → giữ chỗ Redis → thanh toán sandbox → QR → check-in chạy end-to-end.
- AI RAG Gemini/lexical, citation có link, gợi ý theo sở hữu/độ phổ biến, cache, rate limit, reindex, metrics.
- Frontend chat + gợi ý + huỷ đơn + admin khoá user + organizer soát vé.
- Saga compensation: order gọi inventory release khi huỷ/hết hạn; job hết hạn ở cả hai phía.

Còn có thể nâng cấp:

- Adapter VNPay/MoMo sandbox thật (hiện là cổng mock qua `PaymentGateway`).
- Saga command/reply đầy đủ qua RabbitMQ thay cho HTTP + event.
- Bộ test tranh vé đồng thời và 20 câu vàng.

## 9. Ghi chú kỹ thuật

- Toàn bộ service dùng Java 21, Spring Boot 3.5; gateway/AI dùng WebFlux, domain service dùng MVC/JPA.
- PostgreSQL dùng `pgvector/pgvector:pg16`; extension `vector` tạo trong `ai_db`.
- R2DBC không bind trực tiếp kiểu `vector`; repository dựng literal số từ embedding (an toàn, không chứa input người dùng).
- Header `X-User-*` chỉ do gateway gắn sau khi xác thực; client gửi lên sẽ bị xoá.
- Guest chat dùng `X-Guest-Key` (UUID lưu localStorage) để giữ phiên và rate limit.
- `.env` chứa Gemini key và không được commit; chỉ commit `.env.example`.
