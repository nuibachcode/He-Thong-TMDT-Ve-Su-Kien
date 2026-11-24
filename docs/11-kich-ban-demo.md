# 11 — Kịch bản demo bản Docker/microservices

> Thời lượng gợi ý: 10–12 phút. Chuẩn bị trước: `docker compose up -d --build`,
> mở sẵn 3 tab: ứng dụng, RabbitMQ, terminal.

## 0. Mở đầu (1 phút)

- Kiến trúc: React → API Gateway (JWT/CORS) → AI service (RAG + gợi ý) và core-api (nghiệp vụ mua vé).
- Hạ tầng: PostgreSQL + pgvector, MongoDB, Redis, RabbitMQ — tất cả bằng Docker Compose.
- Điểm nhấn: AI trả lời có nguồn, từ chối ngoài phạm vi; gợi ý theo sở thích; gateway bảo vệ toàn hệ thống.

## 1. Trang chủ + gợi ý (1 phút)

1. Mở http://localhost:5173.
2. Bấm vài sở thích: “biển đảo”, “ẩm thực”.
3. Khu gợi ý đổi ngay: Tour Phú Quốc, Tour Hội An… kèm lý do “Phù hợp sở thích…”.
4. Chỉ ra request đi qua gateway (`/api/ai/recommendations`).

## 2. Mua vé end-to-end (3 phút)

1. Vào Sự kiện → chọn “Đêm nhạc Trịnh Công Sơn”.
2. Chọn hạng vé, số lượng → nhập voucher `WELCOME10` → đặt đơn.
3. Hệ thống giữ chỗ 10 phút; mở thanh toán mô phỏng → thanh toán.
4. Vào “Vé của tôi” → mở QR.
5. Nhấn mạnh: đơn đã CONFIRMED, tồn kho đã trừ, QR sẵn sàng soát.

## 3. Chat RAG (3 phút)

Mở Trợ lý AI, hỏi lần lượt:

1. “Huỷ tour trước 5 ngày có được hoàn tiền không?”
   - Kỳ vọng: nêu mốc 3–6 ngày hoàn 50%, trích dẫn “Chính sách hoàn và hủy vé, tour”.
2. “Tour Phú Quốc có bao gồm vé máy bay không?”
   - Kỳ vọng: trả lời “không bao gồm”, citation mở đúng tour.
3. “Tour Đà Lạt 3 ngày và 4 ngày khác nhau thế nào?”
   - Kỳ vọng: so sánh 2 tour, có link.
4. “Hôm nay thời tiết Hà Nội thế nào?”
   - Kỳ vọng: từ chối lịch sự, không bịa.

Chỉ vào citation dạng nút → mở trang sự kiện/tour tương ứng.

## 4. Bảo vệ / kỹ thuật (2 phút)

- Gọi API không token: `GET /api/bookings` → 401.
- Customer gọi `/api/ai/metrics` → 403; admin gọi → 200.
- Rate limit: gửi liên tiếp > 20 tin/phút → 429 `RATE_LIMITED`.
- Reindex: admin gọi `POST /api/ai/reindex` hoặc publish message `catalog.changed` trên RabbitMQ.
- Redis: cache câu trả lời/embedding; RabbitMQ management http://localhost:15672.
- Docker: `docker compose ps` — 8 container; `docker compose logs -f ai-service`.

## 5. Organizer + Admin (2 phút)

- Organizer: tạo sự kiện/tour → trạng thái PENDING; xem doanh thu; dùng “AI viết mô tả”.
- Admin: duyệt nội dung, xem metrics AI (số câu hỏi, tỷ lệ dự phòng, feedback).

## 6. Kết (1 phút)

- Đã đáp ứng: Gateway, AI/RAG, gợi ý, Docker, PostgreSQL/MongoDB/Redis/RabbitMQ, citation, guardrail.
- Hướng phát triển: tách core-api thành Identity/Catalog/Inventory/Order/Payment; VNPay/MoMo sandbox;
  saga đầy đủ; bộ 20 câu vàng và test tranh vé đồng thời.
