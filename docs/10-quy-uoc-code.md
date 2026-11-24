# 10 — Quy ước code và phối hợp

## 1. Cấu trúc monorepo

```
backend/                 # core-api composite (đang tách dần)
frontend/                # React + Vite + TypeScript
services/gateway/        # API Gateway (Spring Cloud Gateway)
services/ai/             # AI/RAG + recommendation
infra/postgres/init.sql  # tạo database + extension pgvector
docker-compose.yml       # chạy toàn hệ thống
docs/                    # tài liệu 01 → 10
```

Mỗi service là một Maven project độc lập, có `pom.xml`, `Dockerfile`, `src/main/java`.
Package gốc: `vn.edu.tmdt.<service>`.

## 2. Hexagonal / DDD (áp dụng cho service mới)

```
vn.edu.tmdt.ai
├── domain/          # record/entity, value object, không phụ thuộc framework
├── repository/      # port ra database (R2DBC, Mongo)
├── provider/        # adapter ra ngoài (Gemini, lexical)
├── ingest/          # use case ingest
├── rag/             # use case hỏi đáp
├── recommendation/  # use case gợi ý
├── cache/           # Redis adapter
├── web/             # controller, DTO
├── config/          # cấu hình Spring
└── mq/              # RabbitMQ adapter
```

Quy tắc:
- Domain không import `org.springframework.*` nếu có thể.
- Controller không gọi thẳng repository; đi qua use case/service.
- Mỗi bounded context sở hữu dữ liệu của mình; không join/xoá dữ liệu service khác.
- Giao tiếp qua ID và sự kiện, không dùng khoá ngoại liên service.

## 3. API và lỗi

- Response thành công: `{ "success": true, "message": "OK", "data": ... }`.
- Response lỗi: `{ "success": false, "code": "...", "message": "...", "status": 4xx, "path": "...", "timestamp": "..." }`.
- Mã lỗi dùng thống nhất: `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `BAD_REQUEST`,
  `CONFLICT`, `RATE_LIMITED`, `INTERNAL_ERROR`.
- Thời gian ISO-8601; tiền VND dạng số thập phân.
- API công khai: `/api/public/**`, `/api/auth/login`, `/api/auth/register`,
  `/api/ai/chat`, `/api/ai/recommendations`.

## 4. JWT

- HS256, secret >= 32 ký tự, đặt trong `JWT_SECRET`.
- Claims: `sub` (email), `uid` (user id), `role` (`CUSTOMER`/`ORGANIZER`/`ADMIN`).
- Gateway xác thực và gắn `X-User-Id`, `X-User-Email`, `X-User-Role` cho service sau.
- Không tin header `X-User-*` do client gửi; gateway luôn xoá trước khi gắn lại.
- Guest chat dùng `X-Guest-Key` (UUID sinh ở frontend) để giữ phiên.

## 5. Database

- PostgreSQL: dữ liệu giao dịch (core_db) và vector (ai_db).
- MongoDB: card sự kiện/tour và sở thích người dùng (ai).
- Redis: cache embedding, cache câu trả lời, rate limit.
- RabbitMQ: exchange `ticketgo.events`, routing key `catalog.*`, queue `ai.catalog.changed`.
- Không hard-code mật khẩu; dùng biến môi trường trong `.env`.

## 6. Git

- Nhánh: `main` (ổn định), `develop`, `feature/<service>-<việc>`.
- Commit Conventional Commits: `feat(ai): ...`, `fix(gateway): ...`, `docs: ...`, `chore: ...`.
- PR ≤ 400 dòng, mô tả rõ API/thay đổi schema, ít nhất 1 người review.
- Trước khi merge: `mvn test`, `npm run build`, `docker compose config`.

## 7. Biến môi trường

Mọi biến nằm trong `.env.example`, không commit `.env`.
Các biến bắt buộc khi chạy Docker: `JWT_SECRET`, `POSTGRES_PASSWORD`.
Tùy chọn: `AI_PROVIDER=mock|gemini`, `GEMINI_API_KEY`, `CORS_ORIGINS`.

## 8. Kiểm thử tối thiểu

- Gateway: 401 khi thiếu token, 403 khi sai role, route đúng service.
- AI: chat có citation, từ chối câu ngoài phạm vi, recommendation theo sở thích,
  rate limit 429, reindex không làm mất dữ liệu.
- core-api: đăng nhập, danh mục, đặt vé, thanh toán, QR, chống oversell.
- Frontend: build TypeScript, lint, luồng chat và gợi ý.
