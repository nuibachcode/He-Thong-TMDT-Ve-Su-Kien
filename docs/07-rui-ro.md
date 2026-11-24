# 07 — Rủi ro & phương án xử lý

> Đồ án 4 tuần thất bại không phải vì code khó, mà vì **hết thời gian**. Tài liệu này để phát hiện sớm và cắt đúng lúc.

---

## 1. Bảng rủi ro

| # | Rủi ro | Xác suất | Tác động | Dấu hiệu sớm | Cách xử lý | Phương án B |
|---|---|---|---|---|---|---|
| R1 | Hết quota free tier đúng lúc demo | Cao | Rất cao | Chat trả 429 khi tập | Cache, giới hạn request, dùng Groq khi Gemini hết | `AI_PROVIDER=mock` (trả lời extractive từ chunk) + video |
| R2 | Khoá API key / cần thẻ tín dụng | Trung bình | Cao | Không tạo được key | Dùng Groq (không cần thẻ) + embedding mock lexical | Chạy hoàn toàn mock, ghi rõ trong báo cáo |
| R3 | Docker Desktop không chạy | Cao | Thấp | `docker info` lỗi | Mặc định H2 file, MySQL chỉ khi cần | Không dùng Docker; nộp kèm script SQL |
| R4 | Xung đột thư viện/phiên bản (Jackson 3, Lombok, JDK 25) | Trung bình | Trung bình | Build đỏ sau khi thêm dependency | Bộ khung đã pin phiên bản chạy được; thêm gì thì build ngay | Bỏ dependency đó, tự viết phần nhỏ |
| R5 | Scope creep (thêm tính năng giữa tuần 2) | Cao | Cao | Board toàn thẻ mới | Luật: chỉ thêm vào "Should", không thêm vào "Must" | Cắt theo thang ưu tiên (mục 3) |
| R6 | Một thành viên vắng/ốm | Trung bình | Cao | Không push 2 ngày | Mỗi người viết README cho phần mình; review chéo | Người còn lại làm phần "Should" trước, "Must" san sẻ theo cặp |
| R7 | Conflict Git làm mất code | Trung bình | Cao | PR quá lớn, sửa cùng file | PR nhỏ, pull mỗi sáng, không sửa file người khác đang làm | Revert về commit cuối chạy được |
| R8 | Mất code/máy hỏng | Thấp | Rất cao | — | Push cuối ngày + tag cuối tuần; 1 người giữ bản zip | Clone từ remote |
| R9 | RAG trả lời kém vì dữ liệu mỏng | Cao | Cao | Câu trả lời chung chung | Dữ liệu demo dày (mô tả ≥ 300 từ), câu hỏi vàng bám dữ liệu | Thêm FAQ tổng hợp để tăng độ phủ |
| R10 | Oversell khi demo | Thấp | Cao | Test đồng thời thấy âm kho | Atomic update + `CHECK` constraint | Giới hạn 1 vé/người khi demo |
| R11 | Lộ API key trong repo | Trung bình | Cao | Key nằm trong file `.ts/.java` | Chỉ đọc từ env; `.gitignore`; quét trước khi push | Thu hồi key, tạo key mới |
| R12 | Demo chậm do AI 8–15s/câu | Cao | Trung bình | Câu trả lời lâu | Chọn model nhanh (Groq), prompt ngắn, pre-warm | Trả lời extractive tức thời cho câu đơn giản |
| R13 | Giảng viên hỏi vào phần không phải sở trường | Trung bình | Cao | — | Mỗi người học kiến trúc toàn hệ thống + 20 câu Q&A | Trả lời ở mức thiết kế, hẹn trao đổi sau |
| R14 | Trùng lịch thi môn khác tuần 3–4 | Cao | Cao | Lịch thi công bố muộn | Dồn việc nặng sang tuần 1–2 | Cắt scope sớm 1 tuần |

## 2. Nguyên tắc "3 ngày"

> Nếu đến cuối ngày thứ 3 của một tuần mà mốc tuần chưa đạt **50%**, lập tức:
> 1. Cắt 1 mục trong "Should have" (thang dưới).
> 2. Báo cả nhóm trong standup, không giấu.
> 3. Giữ nguyên mốc demo, giảm độ sâu tính năng.

## 3. Thang cắt scope (cắt từ trên xuống)

1. Soát vé QR tại cổng.
2. Email xác nhận thật (giữ log).
3. Review/rating.
4. AI soạn mô tả (AI-3).
5. Gợi ý cá nhân hoá (AI-2).
6. Dashboard organizer nâng cao.
7. Admin duyệt nội dung.

**Tuyệt đối giữ:** đăng nhập, danh mục, mua vé + QR, chat RAG có nguồn, chống oversell, dữ liệu demo, chạy offline được.

## 4. Bẫy kỹ thuật đã kiểm chứng trên môi trường này

| Bẫy | Triệu chứng | Cách xử lý |
|---|---|---|
| **Spring Boot 4 dùng Jackson 3** (`tools.jackson.*`) | `package com.fasterxml.jackson.databind does not exist` | Không import Jackson 2; dùng record/Map, hoặc `tools.jackson.databind.ObjectMapper` |
| Thuộc tính `spring.jackson.serialization.write-dates-as-timestamps` đã bỏ | Ứng dụng không khởi động: `No enum constant ... SerializationFeature` | Xoá khỏi `application.yml` (Jackson 3 mặc định ISO-8601) |
| **Lombok + JDK 25** | Lỗi annotation processor khi compile | Bộ khung không dùng Lombok; nếu muốn dùng thì build ngay sau khi thêm |
| **H2 file mode** | Khoá file khi chạy 2 tiến trình | `AUTO_SERVER=TRUE` đã bật; hoặc dùng H2 in-memory cho test |
| **Docker Desktop tắt** | `failed to connect ... dockerDesktopLinuxEngine` | Chạy H2; bật Docker khi cần MySQL |
| **Vite 8 + Node < 20.19** | Lỗi `crypto.hash is not a function` | Dùng Node 22 (máy đã có) |
| **Tailwind 4** | CSS không sinh ra | Dùng `@tailwindcss/vite` + `@import "tailwindcss";` (không dùng `tailwind.config.js` kiểu v3) |
| **Cổng 8080 bị chiếm** | `Port 8080 was already in use` | `netstat -ano \| findstr :8080` → kill PID, hoặc đổi `server.port` |
| **Proxy Vite** | 404/CORS khi gọi `/api` | `server.proxy` đã cấu hình; kiểm tra backend chạy đúng 8080 |
| **`react-router` v7** | Import cũ `Switch` không còn | Dùng `Routes`/`Route` (bộ khung đã đúng) |

## 5. Quy tắc dùng AI để viết code (quan trọng cho phần bảo vệ)

- Được phép: hỏi ý tưởng, xin mẫu code, nhờ giải thích lỗi.
- **Không** dán code vào dự án mà không đọc từng dòng. Nếu không giải thích được dòng đó làm gì → không merge.
- Mọi PR phải có 1 câu: "phần này em hiểu là ..." trong mô tả; người review hỏi ngẫu nhiên 1 hàm.
- Ghi rõ trong báo cáo phần nào có hỗ trợ của công cụ AI (nếu giảng viên yêu cầu) — trung thực luôn an toàn hơn.

## 6. Bảo mật tối thiểu trước khi nộp

- [ ] `.env` không nằm trong repo; `git log -p | Select-String "AIza"` không có kết quả.
- [ ] Mật khẩu DB, JWT secret đọc từ biến môi trường.
- [ ] Swagger tắt hoặc yêu cầu đăng nhập ở môi trường công khai (nếu deploy).
- [ ] CORS chỉ cho origin của FE, không dùng `*` khi có credentials.
- [ ] Tài khoản demo không phải tài khoản admin thật của hệ thống.
