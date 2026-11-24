# 01 — Tổng quan dự án

> Đồ án cuối học phần: **Hệ thống thương mại điện tử vé sự kiện và tour du lịch tích hợp AI/RAG**
> Thời gian: **4 tuần** · Nhân sự: **3 người** · Stack: **Spring Boot + React + MySQL**
> Cập nhật: 21/11/2026

---

## 1. Bài toán

Hiện nay việc mua vé sự kiện và đặt tour du lịch bị **phân mảnh**: vé ca nhạc bán trên một nền tảng, tour bán trên một nền tảng khác, chính sách hoàn huỷ mỗi nơi một kiểu. Ba hệ quả:

1. **Người mua** mất thời gian so sánh, khó biết "tour nào phù hợp với tôi 3 ngày ở Đà Lạt, ngân sách 5 triệu".
2. **Nhà tổ chức** phải tự quản lý tồn vé, đơn hàng, doanh thu bằng Excel.
3. **Câu hỏi lặp lại** (giờ mở cửa, chính sách hoàn tiền, hành lý, trẻ em...) khiến đội CSKH quá tải.

Dự án giải quyết cả ba bằng **một nền tảng hai phía (marketplace)** + **trợ lý ảo AI/RAG** trả lời dựa trên dữ liệu thật của hệ thống, có trích nguồn.

## 2. Mục tiêu

| # | Mục tiêu | Thước đo |
|---|---|---|
| M1 | Người dùng tìm – chọn – mua vé/tour trong **một luồng liền mạch** | Hoàn tất đơn hàng < 2 phút, không rời hệ thống |
| M2 | Nhà tổ chức tự đăng bán và theo dõi doanh thu | Tạo tour + xem dashboard trong 5 phút |
| M3 | Trợ lý AI trả lời **dựa trên dữ liệu hệ thống**, không bịa | ≥ 80% câu trả lời đúng trên bộ 20 câu kiểm thử, 100% câu trả lời có nguồn |
| M4 | Hệ thống chịu được tình huống thực tế | Không bán quá số vé (oversell) khi 10 request đồng thời; mọi API có mã lỗi rõ ràng |

## 3. Phạm vi chức năng

### 3.1 Bắt buộc (Must have — 4 tuần phải xong)

**Khách hàng**
- Đăng ký / đăng nhập (JWT), phân quyền 3 vai trò.
- Xem danh sách + chi tiết sự kiện; danh sách + chi tiết tour.
- Tìm kiếm, lọc theo danh mục / thành phố / khoảng giá / ngày.
- Chọn loại vé & số lượng, giữ chỗ có thời hạn (10 phút), áp voucher, thanh toán (cổng mô phỏng), nhận **vé điện tử QR**.
- Xem "Vé của tôi", huỷ đơn (theo chính sách).
- **Chat với trợ lý AI** (RAG) có trích nguồn + lịch sử hội thoại.
- **Gợi ý cá nhân hoá** dựa trên sở thích/hành vi.

**Nhà tổ chức (ORGANIZER)**
- CRUD sự kiện, loại vé, tour, lịch khởi hành.
- Xem đơn hàng, doanh thu, tỷ lệ lấp đầy.
- **AI soạn mô tả** tour/sự kiện (chỉ gợi ý, người dùng sửa lại).

**Quản trị (ADMIN)**
- Quản lý người dùng, khoá/mở tài khoản.
- Kiểm duyệt nội dung (duyệt / từ chối sự kiện, tour).
- Xem số liệu tổng hợp + **số liệu chất lượng AI** (số câu hỏi, tỷ lệ hài lòng, thời gian phản hồi).

### 3.2 Nên có (Should — làm nếu còn thời gian)

- Đánh giá / bình luận sau khi hoàn thành.
- Soát vé bằng QR tại cổng (check-in).
- Gửi email xác nhận (dùng log hoặc SMTP free).
- Xuất báo cáo doanh thu CSV.
- Upload ảnh (thay vì dán URL).

### 3.3 Cắt hẳn (Out of scope — nói rõ trong báo cáo)

- Thanh toán thật (VNPay/MoMo thật), đối soát tài chính.
- Đa ngôn ngữ, đa tiền tệ.
- Ứng dụng mobile, voice assistant.
- Fine-tune model, multi-agent, OCR.
- Hệ thống gợi ý học máy phức tạp (chỉ dùng embedding + luật).

> **Nguyên tắc vàng của 4 tuần:** mỗi tính năng phải *chạy được từ đầu đến cuối* trước khi làm tính năng tiếp theo. Không có "90% ở 10 màn hình".

## 4. Ba tính năng AI (linh hồn của đồ án)

| # | Tính năng | Cách làm | Điểm ăn điểm |
|---|---|---|---|
| AI-1 | **Chatbot tư vấn RAG** | Chunk dữ liệu sự kiện/tour/FAQ → embedding → lưu MySQL → truy vấn top-k → đưa vào prompt → LLM trả lời + trích nguồn | Trả lời được câu hỏi **chéo dữ liệu** ("so sánh tour Đà Lạt 3 ngày và 4 ngày"), **từ chối** câu ngoài phạm vi |
| AI-2 | **Gợi ý cá nhân hoá** | Vector sở thích người dùng ↔ vector tour/sự kiện + luật (ngân sách, ngày) | Hiển thị "Vì bạn quan tâm biển đảo" |
| AI-3 | **AI soạn nội dung cho organizer** | Prompt có cấu trúc (JSON) → trả về tiêu đề, mô tả, điểm nổi bật | Demo 2 phía: người bán dùng AI để bán hàng |

**Điểm khác biệt so với đồ án thường:** AI **có nguồn** (citation), **có phanh** (từ chối khi thiếu dữ liệu), **có đo lường** (bộ 20 câu kiểm thử + dashboard), **có phương án dự phòng** khi hết quota free tier.

## 5. Ba vai trò người dùng

| Vai trò | Việc chính | Màn hình chính |
|---|---|---|
| CUSTOMER | Tìm, mua, chat, xem vé | Trang chủ, danh sách, chi tiết, thanh toán, vé của tôi, chat |
| ORGANIZER | Đăng bán, quản lý đơn | Dashboard, quản lý sự kiện/tour, danh sách đơn, AI viết mô tả |
| ADMIN | Kiểm duyệt, giám sát | Dashboard, người dùng, duyệt nội dung, số liệu AI |

## 6. Ràng buộc & giả định

- **Thời gian:** 4 tuần ≈ 20 ngày làm việc ≈ **60 người-ngày**. Mọi kế hoạch trong tài liệu 05 đều tính theo ngân sách này.
- **Nhân sự:** 3 sinh viên, biết Java cơ bản; React có thể mới → chọn Vite + React + Tailwind để dựng nhanh.
- **AI:** chỉ dùng **free tier** (Gemini / Groq). Phải có **chế độ mock** để demo không cần mạng/không tốn quota.
- **Hạ tầng:** chạy local (Docker Desktop có thể tắt) → mặc định dùng H2 file, bật MySQL bằng Docker khi cần; deploy là *nice-to-have*, **video demo là bắt buộc**.
- **Không dùng thẻ tín dụng**, không thu tiền thật → cổng thanh toán mô phỏng, ghi rõ trong báo cáo.

## 7. Sản phẩm bàn giao

| # | Sản phẩm | Nơi lưu |
|---|---|---|
| 1 | Mã nguồn backend + frontend (có README chạy được) | repo `backend/`, `frontend/` |
| 2 | Script CSDL + dữ liệu mẫu | `docs/03-co-so-du-lieu.md`, seeder trong code |
| 3 | Tài liệu API (Swagger + `docs/04-api.md`) | `/swagger-ui.html` |
| 4 | Báo cáo đồ án (Word/PDF) | `docs/` → xuất bản cuối |
| 5 | Slide bảo vệ (15–18 slide) | `docs/06-demo-va-bao-ve.md` (dàn ý) |
| 6 | Video demo dự phòng (5–8 phút) | YouTube unlisted / Drive |
| 7 | Bộ 20 câu kiểm thử RAG + kết quả | `docs/08-rag-playbook.md` |

## 8. Tiêu chí "thành công" khi bảo vệ

- [ ] Demo **không cần sửa code**, chạy theo kịch bản đã tập ≥ 2 lần.
- [ ] Trả lời được: *"Vì sao RAG mà không fine-tune?"*, *"Làm sao chống bán quá vé?"*, *"Nguồn dữ liệu AI lấy từ đâu?"*.
- [ ] Mỗi thành viên giải thích được phần mình làm **và** phần người khác ở mức kiến trúc.
- [ ] Có số liệu: thời gian phản hồi AI, tỷ lệ trả lời đúng, số test pass.
- [ ] Có phương án B khi mạng/quota hỏng (mock mode + video).

## 9. Đọc tiếp

1. `02-kien-truc.md` — kiến trúc, stack, luồng nghiệp vụ, thiết kế RAG.
2. `03-co-so-du-lieu.md` — ERD + DDL.
3. `04-api.md` — hợp đồng API (chốt ngày 1, cả nhóm code song song theo đây).
4. `05-ke-hoach-4-tuan.md` — phân công từng ngày cho 3 người.
5. `06-demo-va-bao-ve.md` — kịch bản demo + câu hỏi phản biện.
6. `07-rui-ro.md` — rủi ro và cách xử lý.
7. `08-rag-playbook.md` — công thức triển khai RAG chi tiết.
