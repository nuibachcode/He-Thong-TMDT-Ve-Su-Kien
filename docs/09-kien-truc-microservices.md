# 09 — Kiến trúc microservices (bản đang chạy)

> Tài liệu này thay thế phần kiến trúc monolith trong `02-kien-truc.md` cho phiên bản Docker hiện tại.
> Mục tiêu: bám theo báo cáo Nhóm 11 — Spring Boot 3 WebFlux, API Gateway, PostgreSQL/MongoDB/Redis/RabbitMQ,
> AI/RAG dùng pgvector + Gemini, DDD/Hexagonal.

## 1. Sơ đồ tổng thể

```
                        ┌─────────────────────────────┐
                        │        React + Vite         │
                        │  (chat AI, gợi ý, mua vé)   │
                        └──────────────┬──────────────┘
                                       │ /api (nginx proxy)
                        ┌──────────────▼──────────────┐
                        │        API Gateway          │
                        │  JWT · CORS · định tuyến    │
                        └───────┬──────────────┬──────┘
                                │              │
                 /api/ai/**     │              │   các /api/** khác
                    ┌───────────▼───┐      ┌───▼────────────────────┐
                    │  AI Service   │      │  core-api (tạm thời)   │
                    │  WebFlux      │      │  Identity/Catalog/     │
                    │  RAG + gợi ý  │      │  Inventory/Order/      │
                    └──┬───┬───┬────┘      │  Payment (composite)   │
                       │   │   │           └───────────┬────────────┘
              pgvector │   │   │ Redis                 │ PostgreSQL
            (Postgres) │   │   └── cache/rate limit   │ (core_db)
                       │   └────── MongoDB             │
                       │          (card, interests)    │
                       └────────── RabbitMQ ───────────┘
                                  (catalog.changed)
```

`core-api` là **composite service chuyển tiếp**: giữ nguyên nghiệp vụ đang chạy để demo end-to-end,
đồng thời là nơi tách dần Identity/Catalog/Inventory/Order/Payment thành service riêng.
Khi tách xong, chỉ cần thêm route trong `services/gateway` và bỏ route `core-api`.

## 2. Thành phần và dữ liệu

| Thành phần | Công nghệ | Dữ liệu | Cổng |
|---|---|---|---|
| Gateway | Spring Cloud Gateway (WebFlux) | — | 8080 |
| AI Service | Spring Boot 3.5 WebFlux, R2DBC, Spring Data Reactive MongoDB/Redis, Spring AMQP | pgvector `ai_db`, Mongo `ai`, Redis cache, RabbitMQ | 8082 |
| core-api (tạm) | Spring Boot, JPA, PostgreSQL | `core_db` | 8081 |
| Frontend | React 19, Vite, nginx | — | 5173 |
| PostgreSQL | `pgvector/pgvector:pg16` | `core_db`, `ai_db` | 5432 |
| MongoDB | `mongo:7` | `ai` | 27017 |
| Redis | `redis:7` | cache, rate limit | 6379 |
| RabbitMQ | `rabbitmq:3.13-management` | event bus | 5672 / 15672 |

## 3. API Gateway

- Là cửa ngõ duy nhất của frontend (`/api/**`).
- Xác thực JWT HS256 bằng `JWT_SECRET` dùng chung với core-api; token chứa `sub` (email), `uid`, `role`.
- Chặn header giả mạo `X-User-Id`, `X-User-Role`, `X-User-Email`; sau khi xác thực mới gắn lại.
- Phân quyền thô:
  - `/api/admin/**`, `/api/ai/metrics`, `/api/ai/reindex` → ADMIN
  - `/api/organizer/**` → ORGANIZER hoặc ADMIN
  - các API còn lại → đã đăng nhập
- Public: `/api/auth/register`, `/api/auth/login`, `/api/public/**`, `/api/health`,
  `/api/ai/chat`, `/api/ai/recommendations`, actuator health, swagger.
- CORS cấu hình bằng `CORS_ORIGINS`.
- Định tuyến:
  - `/api/ai/**` → `ai-service:8082`
  - `/api/**` → `core-api:8081`

## 4. AI/RAG Service

### 4.1 Pipeline

1. **Seed**: `services/ai/src/main/resources/seed/*.json` gồm 4 sự kiện, 5 tour, 4 tài liệu chính sách.
2. **Chunk**: 1.400 ký tự/chunk, overlap 200 ký tự, mỗi chunk có tiêu đề nguồn.
3. **Embed**: Gemini `gemini-embedding-001` (768 chiều) nếu có `GEMINI_API_KEY`;
   nếu không dùng **lexical hashing** để demo offline, không tốn quota.
4. **Store**: `knowledge_chunks.embedding vector(768)` + `content_hash` + `provider`.
5. **Retrieve**: pgvector cosine top-5; ngưỡng 0.55 (Gemini) / 0.12 (lexical), lọc thêm từ khóa đặc trưng.
6. **Generate + guardrail**: Gemini `gemini-2.5-flash` hoặc trả lời trích xuất; kiểm tra số liệu có trong ngữ cảnh;
   từ chối khi thiếu dữ liệu hoặc câu hỏi ngoài phạm vi.
7. **Citation**: trả `sourceType`, `sourceId`, `title`, `url`, `score`; frontend render link tới sự kiện/tour.

### 4.2 Redis

- Cache embedding theo SHA-256 nội dung (30 ngày) — tiết kiệm quota khi reindex.
- Cache câu trả lời phổ biến 15 phút, key gắn với version index.
- Rate limit 20 tin/phút/người dùng hoặc guest key → lỗi 429 `RATE_LIMITED`.

### 4.3 MongoDB

- `event_cards`, `tour_cards`: dữ liệu gợi ý + độ phổ biến.
- `user_interests`: sở thích người dùng khai báo (`PUT /api/ai/interests`).

### 4.4 RabbitMQ

- Exchange `ticketgo.events`, queue `ai.catalog.changed`, binding `catalog.#`.
- Khi Catalog phát `catalog.content.changed`, AI reindex. Hiện có thể gọi thủ công
  `POST /api/ai/reindex` (ADMIN) để demo.

### 4.5 Endpoint

| Method | Path | Ghi chú |
|---|---|---|
| POST | `/api/ai/chat` | `{sessionId?, message}`; guest gửi `X-Guest-Key` |
| GET | `/api/ai/chat/{sessionId}/messages` | kiểm tra chủ phiên |
| POST | `/api/ai/feedback` | `{messageId, rating: 1/-1, comment?}` |
| GET | `/api/ai/recommendations?limit&interests` | trộn sự kiện + tour theo sở thích/độ phổ biến |
| GET/PUT | `/api/ai/interests` | đọc/lưu sở thích (cần đăng nhập) |
| POST | `/api/ai/reindex` | ADMIN |
| GET | `/api/ai/metrics` | ADMIN, số liệu chat/feedback |
| GET | `/api/ai/health` | trạng thái + provider + số chunk |

## 5. Luồng mua vé (core-api, sẽ tách dần)

1. Khách xem danh mục → chi tiết (catalog).
2. Tạo đơn → giữ chỗ 10 phút bằng atomic update, chống oversell (inventory).
3. Áp voucher, tính tiền (order).
4. Thanh toán mô phỏng → xác nhận idempotent (payment).
5. Phát hành vé QR (ZXing), soát vé một lần (inventory).
6. Hết hạn giữ chỗ → job nhả vé (saga compensation bản đơn giản).

Khi tách service, các bước này sẽ chuyển thành saga qua RabbitMQ với hai kịch bản:
thanh toán thành công và thanh toán lỗi/hết hạn giữ chỗ.

## 6. Quyết định thiết kế

| Quyết định | Lý do |
|---|---|
| Gateway xác thực JWT tại biên | Service con không phải lặp lại logic auth; dễ thay Identity sau này |
| AI service tách riêng, WebFlux | RAG gọi API ngoài (Gemini) là I/O-bound; reactive phù hợp |
| Gọi Gemini qua WebClient thay vì Spring AI | Kiểm soát trực tiếp prompt, citation và fallback; có thể thay bằng Spring AI khi cần |
| pgvector thay vì vector store ngoài | Đủ cho ~1.000 chunk, giảm một service; đổi sang Qdrant chỉ thay repository |
| Lexical fallback | Demo offline, không phụ thuộc quota/Internet |
| Mongo cho card + sở thích | Dữ liệu đọc nhiều, schema linh hoạt; tách khỏi Postgres giao dịch |
| core-api composite tạm thời | Không chặn demo khi các service khác chưa tách xong |
| Docker Compose một lệnh | Đúng yêu cầu báo cáo, dễ chạy trên máy khác |

## 7. Việc còn lại để đúng 100% báo cáo

- Tách core-api thành Identity (PostgreSQL), Catalog (MongoDB), Inventory (PostgreSQL + Redis),
  Order (PostgreSQL), Payment (PostgreSQL).
- Thay thanh toán mô phỏng bằng adapter VNPay/MoMo sandbox (đã có cổng `PaymentPort` trong thiết kế).
- Saga orchestration đầy đủ qua RabbitMQ (command/reply, compensation).
- Bổ sung notification service (ngoài phạm vi báo cáo).
