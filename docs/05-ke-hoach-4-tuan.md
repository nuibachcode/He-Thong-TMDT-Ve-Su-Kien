# 05 — Kế hoạch 4 tuần cho nhóm 3 người

> Mốc tham chiếu: **23/11/2026 → 18/12/2026** (20 ngày làm việc). Nếu hạn nộp của bạn khác, giữ nguyên thứ tự công việc và dịch ngày.
> Ngân sách: **60 người-ngày**. Mỗi ngày mỗi người chỉ nên có **1 việc chính** + việc phụ.

---

## 1. Phân vai

| Ký hiệu | Vai trò | Phụ trách | Kỹ năng cần |
|---|---|---|---|
| **A** | Backend Domain | Auth, sự kiện, tour, đặt vé, thanh toán, voucher, QR, dashboard API | Java, Spring, SQL |
| **B** | Frontend | Toàn bộ UI/UX, tích hợp API, responsive, states | React, CSS/Tailwind |
| **C** | AI/RAG + Data + QA | Ingest, embedding, chat, gợi ý, prompt, seeder, test, deploy, số liệu AI | Java cơ bản + kiên nhẫn debug API AI |

**Nguyên tắc chéo:** mỗi người phải hiểu phần của 2 người còn lại ở mức *kiến trúc + luồng dữ liệu*, vì hội đồng sẽ hỏi bất kỳ ai.

## 2. Nhịp làm việc (áp dụng từ ngày 1)

| Thời gian | Việc |
|---|---|
| 08:30 | Standup 15' (mỗi người 3 câu: hôm qua / hôm nay / đang vướng) — ghi vào board |
| 09:00–11:30 | Deep work #1 (tắt thông báo, không họp) |
| 13:30–17:00 | Deep work #2 |
| 17:00 | Merge + `git push` hết, cập nhật board |
| 17:15 | Demo nội bộ 5' trên máy người làm (không cần đẹp) |

**Quy tắc Git**

```
main      ← chỉ merge từ develop cuối mỗi tuần, luôn chạy được
develop   ← nhánh tích hợp, mọi PR đổ vào đây
feat/<việc-ngắn>   ví dụ: feat/booking-reserve, feat/chat-ui
```

- Commit nhỏ, theo dạng `feat(booking): giu cho 10 phut`, `fix(auth): 401 sai header`.
- Mỗi PR ≤ 400 dòng, **1 người review trong 2 giờ**, reviewer phải chạy `mvn test` và `npm run build`.
- Cuối ngày **bắt buộc push** — không giữ code trên máy.
- Cấm commit file `.env`, key AI, `data/`, `target/`, `node_modules/`.

**Board (GitHub Projects/Trello):** `Backlog → Todo → Doing (WIP ≤ 2/người) → Review → Done`. Mỗi thẻ có: việc, người, ước lượng (giờ), tiêu chí xong.

## 3. Bức tranh tổng thể

| Tuần | Chủ đề | Mốc demo được | Ngày |
|---|---|---|---|
| 1 | Nền móng: auth, danh mục, dữ liệu | **M1:** duyệt danh sách/chi tiết + chat trả lời có nguồn | 23–27/11 |
| 2 | Đặt vé & RAG v1 | **M2:** mua vé hoàn chỉnh → QR + chat RAG | 30/11–4/12 |
| 3 | Hai phía + AI nâng cao | **M3:** đủ 3 vai trò, AI gợi ý + soạn mô tả | 7–11/12 |
| 4 | Ổn định, báo cáo, bảo vệ | **M4:** tổng duyệt + video dự phòng | 14–18/12 |

> **Luật bất biến:** cuối mỗi ngày, nhánh `develop` phải chạy được. Nếu không, người gây lỗi có trách nhiệm sửa ngay trong tối hôm đó.

---

## 4. TUẦN 1 — Nền móng & dữ liệu (23–27/11)

**Mục tiêu tuần:** mọi người chạy được cả hệ thống trên máy mình; có dữ liệu thật để AI trả lời.

| Ngày | A — Backend | B — Frontend | C — AI/Data | Cuối ngày phải thấy |
|---|---|---|---|---|
| **D1 23/11** | Chạy bộ khung (đã có), tạo repo + bảo vệ `main`, mời thành viên, viết README chạy dự án | Chạy frontend, dựng layout + router, trang Login/Register gọi API thật | Tạo tài khoản Google AI Studio + Groq, lưu key vào `.env` (không commit); thử `curl` chat + embedding; soạn **20 câu hỏi golden set** | Cả 3 máy chạy BE+FE, đăng nhập thật thành công |
| **D2 24/11** | Entity `User/Event/Venue/TicketType`, API public list + detail, seeder 2 sự kiện | Trang danh sách sự kiện (grid + filter bar), trang chi tiết (khung) | DDL MySQL + `docker-compose.yml`; viết seeder dữ liệu mẫu (SQL) + chọn ảnh | FE hiển thị 2 sự kiện lấy từ API thật |
| **D3 25/11** | Entity `Tour/TourSchedule`, API list + detail + tìm kiếm từ khoá (LIKE) | Trang chi tiết sự kiện đầy đủ: chọn loại vé, số lượng, giá | Ingest v0: đọc event/tour → chunk → embedding → lưu `knowledge_chunks` (≥ 40 chunk) | Trang chi tiết chọn được vé; DB có chunks |
| **D4 26/11** | Phân quyền ORGANIZER, CRUD sự kiện ở trạng thái PENDING | Trang danh sách + chi tiết tour, responsive mobile | `POST /api/ai/chat` bản **extractive**: top-3 chunk ghép câu trả lời (chưa gọi LLM), lưu hội thoại | Hỏi "tour Đà Lạt giá bao nhiêu" → trả lời từ dữ liệu |
| **D5 27/11** | Fix bug + unit test service (≥ 6 test) | Trang chủ (hero, nổi bật) + loading/empty state | Nối LLM thật, prompt v1, citation; đo latency 10 câu đầu | **M1 — demo 5': duyệt + chat có nguồn** |

**Định nghĩa xong tuần 1:** clone repo mới → chạy 2 lệnh → đăng nhập → xem sự kiện → chat. Không cần Docker (mặc định H2).

---

## 5. TUẦN 2 — Đặt vé & RAG v1 (30/11–4/12)

**Mục tiêu tuần:** luồng mua vé chạy từ đầu đến cuối, có QR; chat RAG trả lời đúng ≥ 70% bộ câu hỏi.

| Ngày | A — Backend | B — Frontend | C — AI/Data | Cuối ngày phải thấy |
|---|---|---|---|---|
| **D6 30/11** | Entity `Booking/BookingItem`, giữ chỗ + **atomic update chống oversell**, job nhả vé hết hạn | UI chọn vé → form thông tin → gọi tạo đơn | Chuẩn hoá chunking (500–700 token, overlap 15%), re-index; prompt v2 + guardrail | Tạo được đơn PENDING, `sold_count` tăng |
| **D7 1/12** | Payment mock + callback idempotent + transaction xác nhận đơn | Trang thanh toán mô phỏng + trang kết quả | Chat UI: khung chat, loading, chip nguồn, lịch sử | Đơn chuyển CONFIRMED sau khi "thanh toán" |
| **D8 2/12** | Sinh QR (ZXing) + `GET /tickets` + email mock (log) | "Vé của tôi": danh sách + màn hình QR | Guardrail: từ chối ngoài phạm vi, chặn bịa giá; chạy 20 câu, ghi bảng kết quả | Vé có QR hiển thị trên điện thoại |
| **D9 3/12** | Voucher + tính tiền + validation chặt | Áp voucher UI + trang đơn hàng | `GET /ai/recommendations` + block "Gợi ý cho bạn" | Voucher giảm giá đúng, đơn sai bị chặn |
| **D10 4/12** | Test tích hợp luồng đặt vé | Fix UI + đồng bộ style | Test 20 câu lần 2, ghi số liệu | **M2 — demo mua vé + QR + chat RAG** |

**Rủi ro tuần này:** oversell và race condition. Bắt buộc test bằng script bắn 10 request đồng thời (`curl` song song hoặc JMeter) trước D10.

---

## 6. TUẦN 3 — Hai phía & AI nâng cao (7–11/12)

**Mục tiêu tuần:** đủ 3 vai trò; AI có 3 tính năng; dữ liệu demo đẹp.

| Ngày | A — Backend | B — Frontend | C — AI/Data | Cuối ngày phải thấy |
|---|---|---|---|---|
| **D11 7/12** | `/organizer/stats/overview` + danh sách đơn | Organizer dashboard (số liệu + bảng đơn) | `POST /ai/organizer/describe` + nút "AI viết mô tả" trong form | Organizer thấy doanh thu; bấm AI ra mô tả |
| **D12 8/12** | Admin: duyệt nội dung, khoá user | Admin UI (duyệt, danh sách user) | Đồng bộ index khi CRUD event/tour; search hybrid keyword + vector | Duyệt xong sự kiện mới hiện ra public |
| **D13 9/12** | Review/rating + rate limit login | Review UI + polish toàn bộ | Cache embedding, xử lý 429/quota, log metrics AI | Gửi review được, spam login bị chặn |
| **D14 10/12** | Rà validation + log + xử lý lỗi | Responsive + empty/error/loading đủ mọi trang | Seed dữ liệu đẹp: 6 sự kiện, 8 tour, mô tả ≥ 300 từ, ảnh | Hệ thống "như thật", có dữ liệu để kể chuyện |
| **D15 11/12** | Fix bug từ demo | Fix bug từ demo | Chốt số liệu AI cho báo cáo | **M3 — demo đủ 3 vai trò**; bắt đầu viết báo cáo |

**Chốt scope tại D15:** nếu chưa xong tính năng nào trong "Should have", cắt theo thang ưu tiên ở mục 9.

---

## 7. TUẦN 4 — Ổn định, báo cáo, bảo vệ (14–18/12)

| Ngày | Việc chung | Chia việc |
|---|---|---|
| **D16 14/12** | Sửa bug, test tích hợp, chốt bản chạy | A: bug backend + test; B: kiểm thử Chrome/Edge/mobile; C: docker-compose full + quyết định deploy hay không |
| **D17 15/12** | Viết báo cáo chương 1–2 (giới thiệu, phân tích) | A: chương 3 (thiết kế CSDL); B: chương 2 (khảo sát, yêu cầu); C: chương 4 (kiến trúc AI) |
| **D18 16/12** | Chương 5–6 + slide | A: kiểm thử + kết quả; B: ảnh chụp, slide; C: đánh giá RAG + hạn chế |
| **D19 17/12** | Tổng duyệt 2 lần, quay video dự phòng 5–8' | Cả nhóm, phân vai nói |
| **D20 18/12** | Nộp bài + bảo vệ | — |

**Bắt buộc D17–D19:** báo cáo là *sản phẩm*, không phải việc làm thêm. Chia chương cho 3 người ngay từ D15; ai xong chương mình thì review chương người khác.

---

## 8. Definition of Done (một việc chỉ được coi là XONG khi)

- [ ] Chạy được trên máy người khác (không chỉ máy tác giả).
- [ ] API khớp `docs/04-api.md`; nếu lệch → đã cập nhật tài liệu.
- [ ] Có ít nhất 1 test (BE) hoặc đã test tay theo checklist (FE).
- [ ] Không có `console.error`, không lộ key, không hard-code URL.
- [ ] PR được review bởi 1 người khác, CI `mvn test` + `npm run build` xanh.
- [ ] Có 1 dòng mô tả trong `CHANGELOG.md` hoặc PR description (dùng cho báo cáo sau này).

## 9. Thang cắt scope khi trễ (áp dụng từ D12)

Cắt **từ trên xuống**, dừng ngay khi kịp tiến độ:

1. Soát vé QR tại cổng (check-in).
2. Email xác nhận thật (giữ log).
3. Review/rating.
4. AI soạn mô tả cho organizer (AI-3).
5. Gợi ý cá nhân hoá (AI-2).
6. Dashboard organizer nâng cao (giữ bảng đơn).
7. Admin duyệt nội dung (tự động publish).

**Không bao giờ cắt:** đăng nhập, danh mục, mua vé + QR, **chat RAG có nguồn**, chống oversell, dữ liệu demo, khả năng chạy offline (mock).

## 10. Checklist nộp bài

- [ ] Source code (zip hoặc link repo, có README chạy trong 5 phút).
- [ ] Script CSDL + dữ liệu mẫu (`docs/03`, seeder).
- [ ] Báo cáo PDF đủ chương, có ERD, sơ đồ kiến trúc, ảnh chụp màn hình.
- [ ] Slide PDF (15–18 slide).
- [ ] Video demo dự phòng (link Drive/YouTube unlisted).
- [ ] Bảng 20 câu kiểm thử RAG + kết quả (đưa vào phụ lục báo cáo).
- [ ] Phân công công việc + mức đóng góp từng người (bảng trong báo cáo).

## 11. Chuẩn bị ngay hôm nay (30 phút)

1. Tạo repo GitHub/GitLab **private**, thêm 3 thành viên, bật bảo vệ nhánh `main`.
2. Tạo tài khoản [Google AI Studio](https://aistudio.google.com/) (lấy `GEMINI_API_KEY`) và [Groq Console](https://console.groq.com/) (lấy `GROQ_API_KEY`).
3. Lập nhóm chat + board công việc, dán link vào README.
4. Mỗi người chạy thử: `cd backend && mvn spring-boot:run` và `cd frontend && npm run dev`.
5. Thống nhất lịch standup 8:30 và buổi demo nội bộ 17:15.
