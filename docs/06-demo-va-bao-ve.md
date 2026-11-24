# 06 — Kịch bản demo & bảo vệ

> Mục tiêu: 12 phút demo **không sửa code**, mỗi thành viên nói đúng phần mình, và trả lời được mọi câu hỏi về kiến trúc.

---

## 1. Ba nguyên tắc

1. **Không thao tác chưa tập.** Mọi cú click trong demo đều đã tập ≥ 2 lần.
2. **Luôn có phương án B.** AI lỗi → mock; mạng lỗi → video; DB lỗi → H2 đã seed sẵn.
3. **Nói về quyết định, không chỉ nói tính năng.** "Bọn em chọn RAG vì..." ăn điểm hơn "Đây là trang chủ".

## 2. Chuẩn bị trước 30 phút

- [ ] `backend`: chạy với profile mong muốn, đã seed dữ liệu demo, **đã test chat 3 lần** (làm nóng cache/quota).
- [ ] `frontend`: chạy build production (`npm run build && npm run preview`) — demo trên bản build, không dùng dev server.
- [ ] Trình duyệt: 2 profile (khách + organizer/admin), zoom 110%, tắt thông báo, tắt extension.
- [ ] Đã mở sẵn 4 tab: trang chủ, chi tiết tour, chat, dashboard organizer.
- [ ] Video dự phòng 5–8 phút đã tải sẵn trên máy (không phụ thuộc mạng).
- [ ] Điện thoại đã đăng nhập sẵn để demo QR (tuỳ chọn, rất ấn tượng).
- [ ] File slide PDF + báo cáo PDF trên Desktop, đặt tên rõ ràng.

## 3. Kịch bản 12 phút

| Phút | Người | Hành động | Điểm nhấn cần nói |
|---|---|---|---|
| 0:00–1:00 | C | Mở slide 1–2 | Bài toán phân mảnh; 3 vai trò; mục tiêu 4 tuần |
| 1:00–3:00 | B | Trang chủ → danh sách sự kiện → lọc theo danh mục/giá → chi tiết | Tìm kiếm + filter; dữ liệu thật từ MySQL; responsive |
| 3:00–5:30 | B | Chọn 2 vé VIP → áp `WELCOME10` → thanh toán mô phỏng → vé QR | Giữ chỗ 10 phút; voucher; mã QR; trạng thái đơn |
| 5:30–7:30 | C | Mở chat, hỏi **câu chéo dữ liệu**; bấm chip nguồn; hỏi **câu ngoài phạm vi** | RAG: embed → top-k → prompt → citation; guardrail từ chối |
| 7:30–8:30 | C | Trang chủ, block "Gợi ý cho bạn" (đổi sở thích để thấy kết quả đổi) | Hybrid: vector + luật, không cần train |
| 8:30–10:00 | A | Đăng nhập organizer → dashboard doanh thu → tạo tour, bấm "AI viết mô tả" | Hai phía marketplace; AI sinh nội dung có cấu trúc |
| 10:00–11:00 | A | Admin: duyệt nội dung, xem **số liệu AI**; chạy script 10 request đồng thời chứng minh không oversell | Kiểm duyệt; đo lường AI; atomic update chống oversell |
| 11:00–12:00 | C | Slide kiến trúc + bài học + hướng phát triển | Vì sao monolith, vì sao vector trong MySQL, hạn chế |

**Câu chuyện xuyên suốt nên kể:** "Một khách muốn đi Đà Lạt cuối tuần. Hệ thống không chỉ bán vé — nó **hiểu** tour nào phù hợp và **giải thích được** vì sao, dựa trên dữ liệu thật."

### Ba câu hỏi demo "ăn điểm" nên chuẩn bị sẵn

1. *"Tour Đà Lạt 3 ngày và 4 ngày khác nhau chỗ nào, giá chênh bao nhiêu?"* → câu chéo 2 tài liệu, buộc RAG phải truy xuất cả hai.
2. *"Chính sách hoàn tiền khi huỷ vé sự kiện?"* → trả lời từ FAQ, có nguồn FAQ.
3. *"Giá vàng hôm nay bao nhiêu?"* → **từ chối** đúng cách, không bịa.

## 4. Phương án dự phòng khi demo lỗi

| Tình huống | Xử lý trong 10 giây | Nói gì |
|---|---|---|
| Chat AI lỗi/quota | Bật `AI_PROVIDER=mock` (đã cấu hình sẵn 1 terminal) | "Hệ thống có chế độ dự phòng extractive để không phụ thuộc nhà cung cấp AI." |
| Mạng hỏng | Mở video dự phòng, tiếp tục thuyết trình | "Em xin trình bày phần demo qua video đã ghi." |
| Backend chết | `mvn spring-boot:run` đã mở sẵn terminal; H2 file còn dữ liệu | "Cho em 20 giây khởi động lại dịch vụ." |
| Trang trắng | F5 một lần, nếu vẫn lỗi → chuyển tab khác | Không giải thích dài dòng, tiếp tục mạch demo |
| Hỏi câu ngoài dự kiến | "Em xin phép trả lời sau buổi demo" + ghi lại | Không bịa |

## 5. 20 câu hỏi phản biện thường gặp

| # | Câu hỏi | Trả lời mẫu (ngắn) |
|---|---|---|
| 1 | Vì sao dùng RAG mà không fine-tune? | Dữ liệu thay đổi liên tục (giá, lịch, voucher), fine-tune tốn chi phí và không trích nguồn được. RAG cập nhật bằng cách re-index, phù hợp đồ án 4 tuần. |
| 2 | Chunk bao nhiêu token, vì sao? | 500–700 token, overlap 15%. Đủ chứa một mục lịch trình/chính sách, không vượt context; overlap giữ câu bị cắt ngang. |
| 3 | Làm sao chứng minh AI không bịa? | Ba lớp: prompt chỉ cho phép dùng ngữ cảnh; kiểm tra số liệu trong câu trả lời phải xuất hiện trong chunk; ngưỡng tương đồng thấp thì từ chối. Kèm bộ 20 câu và bảng kết quả. |
| 4 | Embedding lưu ở đâu, tìm kiếm thế nào? | Bảng `knowledge_chunks` (JSON float 768 chiều), cosine trong Java, top-k. 1000 chunk ≈ 5–10ms. |
| 5 | Nếu 1 triệu tài liệu thì sao? | Chuyển sang ANN (Qdrant/pgvector HNSW); hiện tại `VectorStore` là interface nên chỉ thay implementation. |
| 6 | Chống bán quá vé thế nào? | `UPDATE ... WHERE sold_count + q <= quota` là atomic; kèm `@Transactional` và job nhả vé hết hạn. Có test 10 request đồng thời. |
| 7 | Hai người thanh toán cùng lúc? | Trạng thái đơn có điều kiện (`PENDING → CONFIRMED`), callback idempotent theo `txn_ref` unique. |
| 8 | JWT lưu ở đâu, có rủi ro gì? | localStorage cho đơn giản; rủi ro XSS. Hướng cải tiến: httpOnly cookie + refresh token. Ghi rõ trong phần hạn chế. |
| 9 | Phân quyền ở đâu? | Filter JWT + `@PreAuthorize`/kiểm tra role ở service; organizer chỉ thao tác trên tài nguyên của mình. |
| 10 | Vì sao monolith? | 3 người/4 tuần; microservices tăng chi phí vận hành, không đem lại điểm cộng tương xứng. |
| 11 | Vì sao không dùng vector database? | Giảm một service phải chạy; dữ liệu nhỏ; interface cho phép nâng cấp. Đánh đổi đã ghi trong báo cáo. |
| 12 | Giá đổi thì index thế nào? | Lưu `content_hash`; khi event/tour cập nhật, so hash → re-chunk + re-embed phần thay đổi. |
| 13 | Chi phí AI? | Free tier; cache embedding; giới hạn token; fallback extractive. Số liệu latency trung bình nằm trong dashboard. |
| 14 | Prompt injection? | System prompt cố định, ngữ cảnh được đánh dấu rõ, không thực thi chỉ thị trong dữ liệu; đầu ra bị kiểm tra. |
| 15 | Đo chất lượng AI thế nào? | 20 câu vàng × 4 tiêu chí: đúng ý, đúng nguồn, không bịa, < 5s; tính % và ghi vào báo cáo. |
| 16 | Sao không dùng Spring AI? | Muốn kiểm soát prompt/retrieval và hiểu từng bước; Spring AI là hướng mở rộng, không phù hợp thời gian 4 tuần. |
| 17 | Index nào cho query nào? | `events(status,start_at)` cho trang chủ; `bookings(user_id,created_at)` cho đơn của tôi; `knowledge_chunks(source_type,source_id)` cho re-index. |
| 18 | Có N+1 query không? | Dùng `JOIN FETCH`/`@EntityGraph` ở các danh sách chính; bật `show-sql` kiểm tra. |
| 19 | Deploy thế nào? | Docker Compose (MySQL + backend + nginx FE), hoặc chạy local + video. Secret qua biến môi trường. |
| 20 | Hướng phát triển? | Thanh toán thật, ANN vector DB, đa ngôn ngữ, đánh giá RAG tự động (RAGAS), mobile app. |

## 6. Cấu trúc báo cáo (gợi ý 45–60 trang)

| Chương | Nội dung | Người viết | Trang |
|---|---|---|---|
| Mở đầu | Lý do, mục tiêu, phạm vi, bố cục | B | 2–3 |
| 1. Tổng quan | Bài toán, khảo sát, đối thủ, yêu cầu chức năng/phi chức năng | B | 6–8 |
| 2. Cơ sở lý thuyết | Spring Boot, React, JWT, RAG, embedding, cosine, vector search | C | 8–10 |
| 3. Phân tích & thiết kế | Actor, use case, sequence, ERD, thiết kế API, thiết kế AI | A + C | 12–15 |
| 4. Triển khai | Kiến trúc, cấu trúc code, luồng chính, giao diện, pipeline RAG | A + B | 12–15 |
| 5. Kiểm thử & đánh giá | Test case, kết quả, hiệu năng, đánh giá 20 câu RAG, hạn chế | C | 6–8 |
| 6. Kết luận | Kết quả đạt được, bài học, hướng phát triển | A | 2–3 |
| Phụ lục | API, 20 câu hỏi, phân công, hướng dẫn chạy | cả nhóm | 5–8 |

**Bắt buộc có trong báo cáo:** ERD, sơ đồ kiến trúc, sơ đồ tuần tự đặt vé + chat, ảnh chụp 10–12 màn hình, bảng kết quả 20 câu, bảng phân công.

## 7. Dàn ý slide (16 slide)

1. Tiêu đề + thành viên
2. Bài toán & động lực
3. Mục tiêu & phạm vi (must/cut)
4. Người dùng & use case tổng quan
5. Kiến trúc hệ thống (sơ đồ)
6. Công nghệ & lý do chọn
7. Thiết kế CSDL (ERD rút gọn)
8. Luồng đặt vé + chống oversell
9. **RAG pipeline** (sơ đồ 5 bước)
10. Guardrail & trích nguồn
11. Demo: khách hàng (ảnh + 3 bullet)
12. Demo: organizer + AI soạn mô tả
13. Demo: admin + số liệu AI
14. Kiểm thử & kết quả (bảng)
15. Hạn chế & hướng phát triển
16. Q&A + cảm ơn

## 8. Phân vai nói & luyện tập

- C: mở đầu, RAG, kiến trúc, kết luận (nói nhiều nhất về AI).
- B: giao diện, luồng người dùng, responsive.
- A: nghiệp vụ, CSDL, bảo mật, chống oversell.

**Lịch luyện:** D19 sáng 1 lần bấm giờ → sửa → chiều 1 lần nữa. Ai vấp quá 2 lần ở một bước thì đơn giản hoá bước đó.
