# BẢN KẾ HOẠCH DỰ ÁN

> ⚠️ **Tài liệu lịch sử (bản monolith, 3 người).** Kế hoạch và kiến trúc đang dùng nằm ở
> [`README.md`](README.md), [`docs/09-kien-truc-microservices.md`](docs/09-kien-truc-microservices.md)
> và [`docs/10-quy-uoc-code.md`](docs/10-quy-uoc-code.md). Phần phân công theo thành viên trong file này
> không còn dùng cho bản hiện tại.

**Đề tài:** Hệ thống thương mại điện tử vé sự kiện và tour du lịch tích hợp AI/RAG
**Quy mô:** nhóm 3 người · **Thời gian:** 4 tuần (20 ngày làm việc ≈ 60 người-ngày)
**Stack:** Java 25 · Spring Boot 4 · React 19 + Vite · MySQL 8 (H2 khi dev) · RAG tự viết, AI free tier

---

## 1. Mục tiêu

| # | Mục tiêu | Thước đo |
|---|---|---|
| M1 | Mua vé/tour trong một luồng liền mạch | Hoàn tất đơn < 2 phút, không rời hệ thống |
| M2 | Nhà tổ chức tự đăng bán và theo dõi doanh thu | Tạo tour + xem dashboard < 5 phút |
| M3 | Trợ lý AI trả lời dựa trên dữ liệu, không bịa | ≥ 80% đúng trên bộ 20 câu; 100% câu trả lời có nguồn |
| M4 | Chịu được tình huống thực tế | Không oversell khi 10 request đồng thời; lỗi có mã rõ ràng |

## 2. Phạm vi

**Bắt buộc (4 tuần phải xong)**

- Khách: đăng ký/đăng nhập JWT; xem + tìm/lọc sự kiện, tour; chọn vé/lịch khởi hành; giữ chỗ 10 phút; voucher; thanh toán mô phỏng; vé điện tử QR; xem/huỷ đơn; **chat RAG có trích nguồn**; **gợi ý cá nhân hoá**.
- Organizer: CRUD sự kiện/tour/lịch; xem đơn, doanh thu, tỷ lệ lấp đầy; **AI soạn mô tả**; soát vé QR.
- Admin: quản lý người dùng; duyệt/từ chối nội dung; số liệu tổng hợp + **số liệu chất lượng AI**.

**Nên có (nếu còn thời gian):** đánh giá/rating, email xác nhận, xuất CSV, upload ảnh.

**Cắt hẳn:** thanh toán thật, mobile app, đa ngôn ngữ, fine-tune, multi-agent, OCR.

## 3. Kiến trúc

```
React SPA (Vite)  ──JSON/JWT──▶  Spring Boot 4  ──JPA──▶  MySQL 8
                                     │
                                     ├─ Security (JWT, role)
                                     ├─ Booking (giữ chỗ, atomic update)
                                     └─ RagService ──▶ Embedding (Gemini/lexical)
                                                   ├─▶ Vector store (bảng knowledge_chunks)
                                                   └─▶ LLM (Gemini/Groq/mock)
```

**Quyết định chính**

| Quyết định | Lý do |
|---|---|
| Monolith Spring Boot | 3 người/4 tuần; microservices là bẫy |
| JWT stateless | Hợp SPA, ít cấu hình |
| Vector store trong MySQL, cosine trong Java | Bớt 1 service; ~1000 chunk ≈ 5–10 ms; đổi sang Qdrant chỉ thay 1 class |
| RAG tự viết (~250 dòng) | Kiểm soát prompt, dễ bảo vệ, không bị version churn |
| Provider interface + mock | Đổi nhà cung cấp 1 dòng config; demo offline không chết |
| Cổng thanh toán mô phỏng | Không cần tài khoản doanh nghiệp, vẫn đủ luồng callback |
| H2 cho dev | Docker Desktop hay tắt; giảm rủi ro "máy bạn kia không chạy" |

**Cấu trúc mã nguồn**

```
backend/src/main/java/vn/edu/tmdt/
  auth/ catalog/ booking/ ai/ organizer/ admin/ common/ config/ web/
frontend/src/  api/ features/ components/ pages/ lib/
docs/          tài liệu chi tiết 01→08
database/      schema.sql (16 bảng)
```

## 4. Mô hình dữ liệu (16 bảng)

| Nhóm | Bảng |
|---|---|
| Người dùng | `users` (role, interests), `organizer_profiles` |
| Danh mục | `venues`, `events`, `ticket_types`, `tours`, `tour_schedules` |
| Bán hàng | `bookings`, `booking_items`, `payments`, `vouchers` |
| AI | `knowledge_chunks` (embedding JSON), `chat_sessions`, `chat_messages`, `ai_feedback` |
| Khác | `reviews` |

**Quy ước:** tiền `DECIMAL(12,2)`; thời gian lưu UTC; trạng thái lưu VARCHAR; mã đơn 10 ký tự random; `booking_items` lưu snapshot giá/tên.

**Điểm kỹ thuật đáng trình bày**

```sql
-- Chống oversell: atomic, không SELECT rồi if
UPDATE ticket_types SET sold_count = sold_count + :qty
WHERE id = :id AND sold_count + :qty <= quota;
```

## 5. API chính

| Nhóm | Endpoint |
|---|---|
| Auth | `POST /api/auth/register`, `/login`, `GET /me` |
| Public | `GET /api/public/events`, `/events/{slug}`, `/tours`, `/tours/{slug}` |
| Đặt vé | `POST /api/bookings`, `GET /api/bookings`, `/{code}`, `/{code}/tickets`, `POST /{code}/cancel` |
| Thanh toán | `POST /api/payments/{code}/pay` (mô phỏng) |
| AI | `POST /api/ai/chat`, `POST /api/ai/feedback`, `GET /api/ai/recommendations` |
| Organizer | `/api/organizer/events`, `/tours`, `/bookings`, `/stats/overview`, `/checkin`, `/ai/describe` |
| Admin | `/api/admin/users`, `/pending`, `/content/{type}/{id}/status`, `/stats/overview`, `/ai/metrics` |

Chuẩn response: `{ success, message, data }`; lỗi kèm `code` + `status` (400/401/403/404/409/429/500).

## 6. Thiết kế AI/RAG (linh hồn đồ án)

**Pipeline 6 bước**

1. **Ingest:** sự kiện, tour, FAQ, chính sách → văn bản có cấu trúc.
2. **Chunk:** 500–700 token, overlap 15%, mỗi chunk tự đứng được, giữ metadata nguồn.
3. **Embed:** `gemini-embedding-001` (768 chiều) hoặc lexical hashing khi không có key.
4. **Store:** `knowledge_chunks.embedding` (JSON) + `content_hash` để tránh re-embed.
5. **Retrieve:** cosine top-5, ngưỡng 0.55 (Gemini) / 0.25 (lexical), rerank nhẹ.
6. **Generate + guardrail:** prompt chỉ dùng ngữ cảnh → trả lời kèm citation → kiểm tra số liệu có trong ngữ cảnh → từ chối khi thiếu dữ liệu.

**3 tính năng AI**

| # | Tính năng | Điểm ăn điểm |
|---|---|---|
| AI-1 | Chat tư vấn RAG | Câu hỏi chéo 2–3 tài liệu; từ chối câu ngoài phạm vi |
| AI-2 | Gợi ý cá nhân hoá | Vector sở thích + luật (ngày, giá, còn chỗ) |
| AI-3 | AI soạn mô tả cho organizer | Demo hai phía marketplace |

**Đo lường:** bộ 20 câu vàng (8 tra cứu, 5 so sánh, 3 chính sách, 2 ngoài phạm vi, 2 mơ hồ) × 4 tiêu chí: đúng ý, có nguồn, không bịa, latency.

## 7. Kế hoạch 4 tuần (23/11 – 18/12/2026)

**Vai trò:** A = Backend nghiệp vụ · B = Frontend · C = AI/RAG + Data + QA

| Tuần | Chủ đề | Mốc demo |
|---|---|---|
| 1 | Nền móng: auth, danh mục, dữ liệu | **M1:** duyệt danh sách/chi tiết + chat trả lời có nguồn |
| 2 | Đặt vé & RAG v1 | **M2:** mua vé → QR + chat RAG |
| 3 | Hai phía + AI nâng cao | **M3:** đủ 3 vai trò, gợi ý + AI soạn mô tả |
| 4 | Ổn định, báo cáo, bảo vệ | **M4:** tổng duyệt + video dự phòng |

### Tuần 1 — Nền móng & dữ liệu

| Ngày | A | B | C |
|---|---|---|---|
| D1 | Repo, bảo vệ main, chạy skeleton, auth | Layout, router, login/register | Key AI, thử API, soạn 20 câu vàng |
| D2 | Entity Event/Venue/TicketType + API list/detail | Danh sách + chi tiết sự kiện | DDL, docker-compose, seeder dữ liệu |
| D3 | Tour/Schedule + tìm kiếm từ khoá | Chi tiết sự kiện (chọn vé) | Ingest v0: chunk + embedding |
| D4 | Phân quyền organizer, CRUD sự kiện | Danh sách/chi tiết tour, responsive | Chat extractive (chưa gọi LLM) |
| D5 | Fix + unit test (≥ 6 test) | Trang chủ + loading/empty | Nối LLM thật, prompt v1, đo latency |

### Tuần 2 — Đặt vé & RAG v1

| Ngày | A | B | C |
|---|---|---|---|
| D6 | Booking + giữ chỗ + atomic update + job hết hạn | Form chọn vé → tạo đơn | Chunking chuẩn, prompt v2 + guardrail |
| D7 | Payment mock + callback idempotent | Trang thanh toán + kết quả | Chat UI + lịch sử hội thoại |
| D8 | QR + `/tickets` + email log | Vé của tôi + QR | Kiểm tra 20 câu, ghi bảng kết quả |
| D9 | Voucher + validation chặt | Áp voucher + trang đơn | Recommendation API + block gợi ý |
| D10 | Test tích hợp luồng đặt vé | Fix UI | Test 20 câu lần 2 | 

### Tuần 3 — Hai phía & AI nâng cao

| Ngày | A | B | C |
|---|---|---|---|
| D11 | Stats + danh sách đơn organizer | Dashboard organizer | AI describe + nút trong form |
| D12 | Admin duyệt nội dung + khoá user | Admin UI | Đồng bộ index khi CRUD |
| D13 | Review/rating + rate limit login | Review UI + polish | Cache embedding + xử lý 429 + metrics |
| D14 | Rà validation, log, xử lý lỗi | Empty/error/loading đủ trang | Seed dữ liệu đẹp (mô tả ≥ 300 từ) |
| D15 | Fix bug | Fix bug | Chốt số liệu AI → bắt đầu báo cáo |

### Tuần 4 — Ổn định, báo cáo, bảo vệ

| Ngày | Việc |
|---|---|
| D16 | Test tích hợp, sửa bug, chốt bản chạy |
| D17 | Báo cáo chương 1–4 (chia 3 người) |
| D18 | Chương 5–6 + slide + số liệu |
| D19 | Tổng duyệt 2 lần + quay video dự phòng |
| D20 | Nộp + bảo vệ |

## 8. Phân công & quy trình

- **Nhịp ngày:** 08:30 standup 15' · 09:00–11:30 deep work · 13:30–17:00 deep work · 17:00 merge/push · 17:15 demo nội bộ 5'.
- **Git:** `main` (ổn định) ← `develop` ← `feat/<việc>`; PR ≤ 400 dòng, 1 reviewer, chạy `mvn test` + `npm run build` trước khi merge; push cuối ngày.
- **Board:** Backlog → Todo → Doing (WIP ≤ 2/người) → Review → Done.
- **DoD:** chạy được trên máy người khác · API khớp tài liệu · có test/checklist · không lộ key · PR được review.

## 9. Kiểm thử & đánh giá

| Lớp | Cách làm |
|---|---|
| Unit | Service quan trọng: booking, voucher, auth, chunker |
| Tích hợp | Luồng đặt vé end-to-end; 10 request đồng thời chứng minh không oversell |
| API | Swagger + bộ request mẫu |
| RAG | 20 câu vàng, bảng điểm 4 tiêu chí, % đúng, latency trung bình |
| UI | Checklist thủ công 3 trình duyệt + mobile |

## 10. Demo & bảo vệ

Kịch bản 12 phút: mở đầu → khách tìm/chọn vé → voucher → thanh toán → QR → **chat RAG (câu chéo dữ liệu + câu ngoài phạm vi)** → gợi ý → organizer dashboard + AI mô tả → admin duyệt + số liệu AI + chứng minh chống oversell → kiến trúc/bài học.

Chuẩn bị: 2 profile trình duyệt, dữ liệu seed sẵn, `AI_PROVIDER` mock dự phòng, video 5–8 phút, tập 2 lần.

## 11. Rủi ro & thang cắt scope

| Rủi ro | Xử lý |
|---|---|
| Hết quota free tier | Cache embedding, giới hạn 5 tin/phút, chuyển Groq/mock |
| RAG trả lời nhạt do dữ liệu mỏng | Mô tả ≥ 300 từ + FAQ phong phú |
| Oversell | UPDATE atomic + test đồng thời |
| Trễ tiến độ | Cắt theo thang: check-in → email → review → AI-3 → AI-2 → dashboard nâng cao → duyệt nội dung |
| Trùng lịch thi | Dồn việc nặng sang tuần 1–2 |

**Không bao giờ cắt:** đăng nhập, mua vé + QR, chat RAG có nguồn, chống oversell, dữ liệu demo, chế độ offline.

## 12. Sản phẩm bàn giao

- [ ] Mã nguồn + README chạy trong 5 phút
- [ ] Script CSDL + dữ liệu mẫu
- [ ] Báo cáo PDF (6 chương, ERD, sơ đồ, ảnh chụp, bảng 20 câu)
- [ ] Slide 16 trang + video dự phòng
- [ ] Bảng phân công & mức đóng góp

---

## Phụ lục — tài liệu chi tiết

| File | Nội dung |
|---|---|
| `docs/01-tong-quan.md` | Bài toán, mục tiêu, phạm vi must/should/cut |
| `docs/02-kien-truc.md` | Sơ đồ, stack, luồng đặt vé, luồng RAG, lớp AI |
| `docs/03-co-so-du-lieu.md` | ERD + DDL 16 bảng + query quan trọng |
| `docs/04-api.md` | Hợp đồng API + payload mẫu + mã lỗi |
| `docs/05-ke-hoach-4-tuan.md` | Kế hoạch ngày × người bản đầy đủ |
| `docs/06-demo-va-bao-ve.md` | Kịch bản demo, 20 câu phản biện, dàn ý báo cáo/slide |
| `docs/07-rui-ro.md` | Bảng rủi ro, bẫy kỹ thuật, quy tắc dùng AI |
| `docs/08-rag-playbook.md` | Công thức + code mẫu triển khai RAG |

> Ghi chú: mã nguồn hiện có trong repo là bản tham chiếu đã chạy được. Kế hoạch trên là bản để nhóm và giảng viên duyệt trước khi bắt tay làm; nhóm có thể dùng repo làm mẫu hoặc tự triển khai theo kế hoạch.
