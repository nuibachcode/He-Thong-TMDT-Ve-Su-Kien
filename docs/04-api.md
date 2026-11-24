# 04 — Hợp đồng API (chốt từ Ngày 1)

> ⚠️ Hợp đồng API của bản microservices/Docker đang chạy nằm ở
> [`09-kien-truc-microservices.md`](09-kien-truc-microservices.md) (mục API Gateway và AI service).
> Tài liệu này giữ lại cho các endpoint core-api cũ.

> Mục đích: FE và BE code song song mà không chờ nhau. **Mọi thay đổi API phải báo trong nhóm chat và cập nhật file này.**
> Base URL dev: `http://localhost:8080` · Swagger: `http://localhost:8080/swagger-ui.html`

---

## 1. Quy ước chung

| Quy ước | Chi tiết |
|---|---|
| Định dạng | JSON, UTF-8 |
| Xác thực | `Authorization: Bearer <accessToken>` (trừ API public) |
| Bao response | `{ "success": true, "message": "...", "data": ... }` |
| Lỗi | `{ "success": false, "code": "CONFLICT", "message": "...", "status": 409, "path": "...", "timestamp": "...", "errors": {...} }` |
| Thời gian | ISO-8601 UTC: `2026-12-05T12:00:00Z`; FE đổi sang giờ VN |
| Tiền | Số thập phân (VND, thường `.00`), FE format `1.250.000 ₫` |
| Phân trang | Query `page` (0-based), `size` (mặc định 12), `sort=field,asc` → `{ content, page, size, totalElements, totalPages }` |
| Mã đơn | `BOOKING_CODE` 10 ký tự, ví dụ `TKT7X9K2M` |

### Mã lỗi

| HTTP | code | Khi nào |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Thiếu/sai định dạng đầu vào (kèm `errors` theo field) |
| 401 | `UNAUTHORIZED` | Thiếu/sai/hết hạn token |
| 401 | `BAD_CREDENTIALS` | Sai email/mật khẩu |
| 403 | `FORBIDDEN` | Đúng đăng nhập nhưng sai vai trò |
| 404 | `NOT_FOUND` | Không tìm thấy tài nguyên |
| 409 | `CONFLICT` | Email trùng, hết vé, voucher hết hạn |
| 429 | `RATE_LIMITED` | Gọi AI quá nhanh |
| 500 | `INTERNAL_ERROR` | Lỗi không lường trước (đã log server) |

---

## 2. Auth — `/api/auth`

| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| POST | `/register` | public | Đăng ký CUSTOMER |
| POST | `/login` | public | Đăng nhập, nhận JWT |
| GET | `/me` | đã đăng nhập | Thông tin người dùng hiện tại |

**POST /api/auth/register**

```json
// Request
{ "email": "an@example.com", "password": "Secret@123", "fullName": "Nguyễn Văn An", "phone": "0901234567" }
```

```json
// 201 Created
{
  "success": true,
  "message": "Dang ky thanh cong",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": { "id": 4, "email": "an@example.com", "fullName": "Nguyễn Văn An", "phone": "0901234567", "role": "CUSTOMER" }
  }
}
```

**POST /api/auth/login** — body `{ "email", "password" }`, response như trên.

---

## 3. Danh mục công khai — `/api/public`

| Method | Path | Mô tả |
|---|---|---|
| GET | `/events` | Danh sách sự kiện đã publish. Query: `q, category, city, from, to, minPrice, maxPrice, page, size, sort` |
| GET | `/events/{slug}` | Chi tiết sự kiện + loại vé còn lại + đánh giá |
| GET | `/tours` | Danh sách tour. Query: `q, destination, durationDays, minPrice, maxPrice, from, page, size` |
| GET | `/tours/{slug}` | Chi tiết tour + các lịch khởi hành còn chỗ |
| GET | `/search?q=` | Tìm kiếm gộp (event + tour + destination) trả về gợi ý cho ô search |
| GET | `/categories` | Danh mục + số lượng (đổ vào menu/filter) |

**GET /api/public/events/{slug}` — response rút gọn**

```json
{
  "success": true,
  "data": {
    "id": 12,
    "title": "Đêm nhạc Trịnh Công Sơn",
    "slug": "dem-nhac-trinh-cong-son",
    "category": "music",
    "coverImageUrl": "https://...",
    "startAt": "2026-12-20T12:00:00Z",
    "venue": { "name": "Nhà hát lớn Hà Nội", "address": "1 Tràng Tiền", "city": "Hà Nội" },
    "organizer": { "id": 3, "companyName": "Saigon Show" },
    "ticketTypes": [
      { "id": 31, "name": "VIP", "price": 1500000.00, "remaining": 12, "maxPerOrder": 4 },
      { "id": 32, "name": "Standard", "price": 700000.00, "remaining": 88, "maxPerOrder": 6 }
    ],
    "ratingAvg": 4.6,
    "reviewCount": 18
  }
}
```

---

## 4. Đặt vé & thanh toán — `/api/bookings`, `/api/payments`

| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/bookings` | CUSTOMER | Tạo đơn + giữ chỗ 10 phút |
| GET | `/api/bookings` | CUSTOMER | Đơn của tôi (`status` filter) |
| GET | `/api/bookings/{code}` | chủ đơn | Chi tiết đơn + items + payment |
| POST | `/api/bookings/{code}/cancel` | chủ đơn | Huỷ (chỉ PENDING/CONFIRMED theo chính sách) |
| POST | `/api/payments/{code}/pay` | chủ đơn | Tạo giao dịch cổng mô phỏng |
| GET | `/api/payments/mock-return?txnRef=` | public | Cổng gọi về (mô phỏng) → xác nhận đơn |
| GET | `/api/bookings/{code}/tickets` | chủ đơn | Danh sách vé + QR (base64 PNG) |

**POST /api/bookings**

```json
// Request
{
  "bookingType": "EVENT",
  "items": [{ "itemType": "TICKET", "refId": 31, "quantity": 2 }],
  "voucherCode": "WELCOME10",
  "contact": { "name": "Nguyễn Văn An", "email": "an@example.com", "phone": "0901234567" },
  "note": "Cần hoá đơn công ty"
}
```

```json
// 201 Created
{
  "success": true,
  "data": {
    "code": "TKT7X9K2M",
    "status": "PENDING",
    "expiresAt": "2026-11-21T11:40:00Z",
    "subtotal": 3000000.00,
    "discountAmount": 300000.00,
    "totalAmount": 2700000.00,
    "currency": "VND",
    "paymentUrl": "/payment/TKT7X9K2M"
  }
}
```

**Lỗi hết vé (409)**

```json
{ "success": false, "code": "CONFLICT", "message": "Loại vé VIP chỉ còn 1 vé, bạn chọn 2", "status": 409 }
```

**GET /api/bookings/{code}/tickets**

```json
{
  "success": true,
  "data": [
    { "ticketId": 901, "itemName": "Đêm nhạc Trịnh — VIP", "seatHint": "Khu A", "qrPngBase64": "iVBORw0KGgo..." }
  ]
}
```

---

## 5. AI — `/api/ai`

| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| POST | `/chat` | public (guest được) | Hỏi trợ lý RAG |
| GET | `/chat/{sessionId}/messages` | chủ session | Lịch sử hội thoại |
| POST | `/feedback` | public | Đánh giá câu trả lời (👍/👎) |
| GET | `/recommendations` | đã đăng nhập | Gợi ý cá nhân hoá (top 6) |
| POST | `/organizer/describe` | ORGANIZER | AI soạn mô tả tour/sự kiện |
| GET | `/admin/metrics` | ADMIN | Số liệu chất lượng AI |

**POST /api/ai/chat**

```json
// Request
{ "sessionId": null, "message": "Tour Đà Lạt 3 ngày khác gì tour 4 ngày?" }
```

```json
// 200 OK
{
  "success": true,
  "data": {
    "sessionId": 15,
    "messageId": 240,
    "answer": "Tour Đà Lạt 3N2Đ tập trung vào trung tâm thành phố... Tour 4N3Đ có thêm một ngày tại Hồ Tuyền Lâm và làng Cù Lần. Chênh lệch giá khoảng 1.200.000₫.",
    "citations": [
      { "sourceType": "TOUR", "sourceId": 5, "title": "Tour Đà Lạt 3N2Đ", "score": 0.86 },
      { "sourceType": "TOUR", "sourceId": 6, "title": "Tour Đà Lạt 4N3Đ", "score": 0.83 }
    ],
    "provider": "gemini",
    "latencyMs": 1840,
    "fallback": false
  }
}
```

**Câu hỏi ngoài phạm vi** (guardrail bắt buộc phải có)

```json
{
  "success": true,
  "data": {
    "answer": "Mình chỉ tư vấn về sự kiện và tour trên hệ thống. Bạn có muốn xem các tour Đà Lạt đang mở bán không?",
    "citations": [],
    "provider": "gemini",
    "latencyMs": 950,
    "refused": true
  }
}
```

**POST /api/ai/organizer/describe**

```json
// Request
{ "targetType": "TOUR", "destination": "Đà Lạt", "days": 3, "interests": ["núi", "cà phê", "ẩm thực"] }
```

```json
// 200
{
  "success": true,
  "data": {
    "title": "Đà Lạt 3N2Đ — Săn mây và cà phê giữa núi",
    "description": "...(180-250 từ)...",
    "highlights": ["Săn mây Đồi chè Cầu Đất", "Check-in quán cà phê view thung lũng", "Ăn tối tại chợ đêm"],
    "estimatedTokens": 520
  }
}
```

---

## 6. Nhà tổ chức — `/api/organizer` (role ORGANIZER)

| Method | Path | Mô tả |
|---|---|---|
| GET/POST | `/events` | Danh sách / tạo sự kiện (tạo ở trạng thái PENDING chờ duyệt) |
| PUT/DELETE | `/events/{id}` | Sửa / xoá mềm (chỉ sự kiện của mình) |
| GET/POST | `/events/{id}/ticket-types` | Loại vé |
| GET/POST | `/tours` | Tour |
| GET/POST | `/tours/{id}/schedules` | Lịch khởi hành |
| GET | `/bookings?status=&from=&to=` | Đơn của mình |
| GET | `/stats/overview` | Doanh thu, số đơn, tỷ lệ lấp đầy |
| POST | `/checkin` | Quét QR soát vé `{ "bookingCode", "ticketId" }` |

**GET /api/organizer/stats/overview**

```json
{
  "success": true,
  "data": {
    "revenue": 48200000.00,
    "orders": 96,
    "ticketsSold": 214,
    "fillRate": 0.72,
    "revenueByDay": [{ "date": "2026-11-15", "revenue": 4200000.00 }]
  }
}
```

---

## 7. Quản trị — `/api/admin` (role ADMIN)

| Method | Path | Mô tả |
|---|---|---|
| GET | `/users?q=&role=&page=` | Danh sách người dùng |
| PATCH | `/users/{id}/status` | Khoá/mở `{ "enabled": false }` |
| GET | `/pending` | Nội dung chờ duyệt |
| PATCH | `/events/{id}/approve` · `/reject` | Duyệt / từ chối |
| GET | `/stats/overview` | Số liệu toàn hệ thống |
| GET | `/ai/metrics` | Số câu hỏi, % hài lòng, latency trung bình, tỷ lệ fallback |

---

## 8. Thứ tự triển khai API (dùng cho tuần 1–2)

1. `auth/*` — **ngày 1–2**
2. `public/events`, `public/tours` (đọc) — **ngày 3–4**
3. `bookings` (tạo/đọc) — **ngày 6–7**
4. `payments` + `tickets` — **ngày 7–8**
5. `ai/chat` — **ngày 6, hoàn thiện ngày 8–9**
6. `ai/recommendations` — **ngày 9**
7. `organizer/*`, `admin/*` — **ngày 11–12**

> Quy tắc: **không** code FE trước khi endpoint tương ứng có trong file này. Nếu cần đổi, sửa file này trước, rồi mới code.
