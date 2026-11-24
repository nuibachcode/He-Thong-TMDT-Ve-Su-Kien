# TicketGo — Hệ thống TMDT vé sự kiện & tour du lịch tích hợp AI/RAG

> Đồ án cuối học phần · Nhóm 3 người · **4 tuần**
> Java 25 · Spring Boot 4.2 · React 19 · Vite 8 · Tailwind 4 · MySQL 8 / H2 · RAG tự viết

Bộ khung + nghiệp vụ đã được build và kiểm thử thật trên máy của bạn: đăng nhập JWT, danh mục sự kiện/tour,
đặt vé giữ chỗ + thanh toán mô phỏng + QR, chat RAG có trích nguồn và từ chối ngoài phạm vi, dashboard organizer,
quản trị duyệt nội dung và số liệu AI.

---

## 1. Chạy trong 5 phút

Mở **2 terminal** tại thư mục gốc dự án.

```powershell
# Terminal 1 — Backend (cổng 8080)
cd backend
mvn spring-boot:run
```

```powershell
# Terminal 2 — Frontend (cổng 5173)
cd frontend
npm install     # chỉ lần đầu
npm run dev
```

Mở http://localhost:5173 → trang chủ hiển thị trạng thái `✓ status: UP` nghĩa là FE đã nói chuyện được với BE.

**Tài khoản demo** (tự tạo khi backend khởi động lần đầu):

| Email | Mật khẩu | Vai trò |
|---|---|---|
| `customer@demo.vn` | `User@12345` | CUSTOMER |
| `organizer@demo.vn` | `Org@12345` | ORGANIZER |
| `admin@demo.vn` | `Admin@123` | ADMIN |

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:file:./data/devdb`, user `sa`, mật khẩu rỗng)

## 2. Cấu trúc

```
.
├── backend/                 # Spring Boot 4.2 (JDK 25)
│   ├── pom.xml
│   └── src/main/java/vn/edu/tmdt/
│       ├── auth/            # User, JWT, đăng ký/đăng nhập  ← đã xong
│       ├── config/          # Security, OpenAPI, seeder tài khoản demo
│       ├── common/          # ApiResponse, xử lý lỗi tập trung
│       └── web/             # /api/health
├── frontend/                # React + TS + Vite + Tailwind 4
│   └── src/
│       ├── lib/api.ts       # axios client + kiểu dữ liệu + xử lý lỗi
│       ├── features/auth/   # AuthContext (token, user, login/logout)
│       ├── components/      # Layout, header/footer
│       └── pages/           # Home, Login, Register, NotFound
├── docs/                    # 8 tài liệu kế hoạch (đọc từ 01 → 08)
├── database/schema.sql      # DDL MySQL 16 bảng
├── docker-compose.yml       # MySQL 8.4 + Adminer (tuỳ chọn)
├── .env.example             # mẫu biến môi trường
└── README.md
```

## 3. Tài liệu dự án — đọc theo thứ tự

| # | Tài liệu | Nội dung |
|---|---|---|
| 01 | [Tổng quan](docs/01-tong-quan.md) | Bài toán, mục tiêu, phạm vi must/should/cut, 3 tính năng AI |
| 02 | [Kiến trúc](docs/02-kien-truc.md) | Sơ đồ hệ thống, stack, luồng đặt vé, luồng RAG, lớp AI |
| 03 | [Cơ sở dữ liệu](docs/03-co-so-du-lieu.md) | ERD, DDL, dữ liệu mẫu, query quan trọng |
| 04 | [API](docs/04-api.md) | Hợp đồng API cho cả nhóm code song song |
| 05 | [Kế hoạch 4 tuần](docs/05-ke-hoach-4-tuan.md) | Phân công từng ngày cho 3 người, mốc M1–M4 |
| 06 | [Demo & bảo vệ](docs/06-demo-va-bao-ve.md) | Kịch bản 12 phút, 20 câu phản biện, dàn ý báo cáo/slide |
| 07 | [Rủi ro](docs/07-rui-ro.md) | Bảng rủi ro, thang cắt scope, bẫy kỹ thuật đã gặp |
| 08 | [RAG Playbook](docs/08-rag-playbook.md) | Công thức + code mẫu triển khai RAG |

## 4. Chạy với MySQL (tuỳ chọn)

Mặc định dùng H2 file (không cần Docker). Khi cần giống production:

```powershell
docker compose up -d          # MySQL 8.4 :3306, Adminer :8081
$env:SPRING_PROFILES_ACTIVE = 'mysql'
cd backend; mvn spring-boot:run
```

Adminer: http://localhost:8081 — server `mysql`, user `tmdt`, mật khẩu `tmdt123`, database `tmdt`.

> Nếu Docker Desktop chưa chạy, cứ dùng H2. Đừng để việc cài Docker chặn tiến độ tuần 1.

## 5. Cấu hình AI (làm ở tuần 2)

Sao chép `.env.example` → `.env` ở thư mục gốc rồi điền. Backend tự đọc `.env` (đã cấu hình `spring.config.import`).

```env
AI_PROVIDER=mock          # mock | gemini | groq
GEMINI_API_KEY=...
GROQ_API_KEY=...
```

- `mock`: chạy offline, không tốn quota — **dùng khi dev và khi demo dự phòng**.
- `gemini`: chat + embedding (khuyến nghị cho RAG).
- `groq`: chat nhanh, không có embedding.

Lấy key free: [Google AI Studio](https://aistudio.google.com/) · [Groq Console](https://console.groq.com/).

## 6. Lệnh hữu ích

| Việc | Lệnh |
|---|---|
| Chạy test backend | `cd backend; mvn test` |
| Build jar | `cd backend; mvn -DskipTests package` |
| Build frontend | `cd frontend; npm run build` |
| Xem bản build tĩnh (có proxy API) | `cd frontend; npm run preview` → :4173 |
| Kiểm tra compose | `docker compose config --quiet` |
| Reset DB dev (H2) | xoá thư mục `backend/data` rồi chạy lại |

## 7. Ghi chú kỹ thuật (đã kiểm chứng)

- **Spring Boot 4 dùng Jackson 3** (`tools.jackson.*`). Không import `com.fasterxml.jackson.databind` — sẽ lỗi biên dịch.
- Thuộc tính `spring.jackson.serialization.write-dates-as-timestamps` **không còn tồn tại**; Jackson 3 mặc định ISO-8601.
- Không dùng Lombok trong bộ khung để tránh rủi ro annotation processor với JDK 25. Muốn thêm thì build ngay.
- Frontend dev/preview đều proxy `/api` → `http://localhost:8080`, không cần cấu hình CORS khi dev.
- Token JWT lưu `localStorage` (đơn giản cho đồ án). Hướng cải tiến: httpOnly cookie + refresh token — ghi vào phần hạn chế của báo cáo.
- H2 file mode đã bật `AUTO_SERVER=TRUE` để tránh khoá file khi mở console.

## 8. Tính năng đã có

| Nhóm | Đã triển khai |
|---|---|
| Auth | Đăng ký, đăng nhập JWT, phân quyền CUSTOMER/ORGANIZER/ADMIN |
| Danh mục | 4 sự kiện + 5 tour mẫu, lọc/tìm kiếm, chi tiết kèm loại vé/lịch khởi hành |
| Đặt vé | Giữ chỗ 10 phút, atomic update chống oversell, voucher, job nhả vé hết hạn |
| Thanh toán | Cổng mô phỏng, xác nhận đơn idempotent, vé điện tử QR (ZXing) |
| AI/RAG | Ingest → chunk → embedding → top-k → prompt → trả lời + citation, guardrail từ chối, chế độ mock/lexical khi không có key, feedback 👍/👎 |
| Cá nhân hoá | Gợi ý tour theo sở thích (vector + luật), fallback "đang được quan tâm" |
| Organizer | Dashboard doanh thu/tỷ lệ lấp đầy, danh sách đơn, tạo sự kiện/tour, AI viết mô tả, soát vé QR |
| Admin | Thống kê, duyệt/từ chối nội dung, khoá người dùng, số liệu chất lượng AI |

## 9. Bước tiếp theo

1. Chạy thử toàn bộ luồng theo `docs/06-demo-va-bao-ve.md` (mua vé → QR → chat → organizer → admin).
2. Điền `GEMINI_API_KEY` vào `.env` rồi đặt `AI_PROVIDER=gemini` để so sánh chất lượng RAG với chế độ mock (ghi số liệu vào báo cáo).
3. Bổ sung theo thứ tự ưu tiên trong `docs/05-ke-hoach-4-tuan.md` nếu còn thời gian: review, check-in, email, deploy Docker.
