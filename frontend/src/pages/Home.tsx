import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth } from '../features/auth/auth-context'
import { getHealth, getRecommendations } from '../lib/api'
import { formatCurrency } from '../lib/format'

export default function Home() {
  const { user } = useAuth()
  const health = useQuery({ queryKey: ['health'], queryFn: getHealth, retry: false })
  const recommendations = useQuery({
    queryKey: ['recommendations', user?.id ?? 'guest'],
    queryFn: () => getRecommendations(4),
  })

  return (
    <div>
      <section className="bg-gradient-to-br from-slate-900 via-brand-700 to-brand-500 text-white">
        <div className="mx-auto grid max-w-6xl gap-10 px-4 py-16 md:grid-cols-2 md:py-24">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs font-medium ring-1 ring-white/20">
              Đồ án cuối học phần · AI/RAG
            </span>
            <h1 className="mt-5 text-4xl font-bold leading-tight md:text-5xl">
              Vé sự kiện và tour du lịch, gợi ý bởi AI hiểu dữ liệu của bạn
            </h1>
            <p className="mt-4 max-w-xl text-white/80">
              Một nền tảng cho người mua, nhà tổ chức và quản trị. Trợ lý ảo trả lời dựa trên dữ liệu thật,
              có trích nguồn và biết từ chối khi không có thông tin.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/events" className="rounded-xl bg-white px-5 py-3 font-semibold text-brand-700 hover:bg-brand-50">
                Khám phá sự kiện
              </Link>
              <Link
                to="/chat"
                className="rounded-xl border border-white/40 px-5 py-3 font-semibold text-white hover:bg-white/10"
              >
                Hỏi trợ lý AI
              </Link>
              {!user && (
                <Link to="/register" className="rounded-xl px-5 py-3 font-semibold text-white/90 underline-offset-4 hover:underline">
                  Tạo tài khoản →
                </Link>
              )}
            </div>
          </div>

          <div className="rounded-2xl bg-white/10 p-6 ring-1 ring-white/20 backdrop-blur">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-white/70">Trạng thái hệ thống</h2>
            <div className="mt-4 rounded-xl bg-slate-950/40 p-4 font-mono text-sm">
              {health.isLoading && <p className="text-white/70">Đang kiểm tra backend…</p>}
              {health.isError && (
                <div className="space-y-1 text-rose-200">
                  <p>✗ Không kết nối được API</p>
                  <p className="text-xs text-white/60">Chạy: cd backend &amp;&amp; mvn spring-boot:run</p>
                </div>
              )}
              {health.data && (
                <div className="space-y-1 text-emerald-200">
                  <p>✓ status: {health.data.status}</p>
                  <p>✓ app: {health.data.app}</p>
                </div>
              )}
            </div>

            <div className="mt-5 rounded-xl bg-slate-950/30 p-4 text-sm text-white/80">
              <p className="font-semibold text-white">Tài khoản demo</p>
              <ul className="mt-2 space-y-1 font-mono text-xs">
                <li>customer@demo.vn / User@12345</li>
                <li>organizer@demo.vn / Org@12345</li>
                <li>admin@demo.vn / Admin@123</li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-14">
        <div className="flex items-end justify-between">
          <h2 className="text-2xl font-bold text-slate-900">
            {user ? `Gợi ý cho ${user.fullName.split(' ')[0]}` : 'Tour đang được quan tâm'}
          </h2>
          <Link to="/tours" className="text-sm font-medium text-brand-700 hover:underline">
            Xem tất cả →
          </Link>
        </div>

        {recommendations.isLoading && <p className="mt-4 text-slate-500">Đang tải gợi ý...</p>}
        <div className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {recommendations.data?.map((tour) => (
            <Link
              key={tour.id}
              to={`/tours/${tour.slug}`}
              className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
            >
              <img src={tour.coverImageUrl} alt={tour.title} className="h-32 w-full bg-slate-200 object-cover" loading="lazy" />
              <div className="p-4">
                <p className="text-xs font-medium uppercase tracking-wide text-brand-600">{tour.destination}</p>
                <h3 className="mt-1 line-clamp-2 text-sm font-semibold text-slate-900">{tour.title}</h3>
                <p className="mt-2 text-sm font-bold text-brand-700">{formatCurrency(tour.basePrice)}</p>
                <p className="mt-1 line-clamp-1 text-xs text-slate-500">{tour.reason}</p>
              </div>
            </Link>
          ))}
        </div>
      </section>

      <section className="border-t border-slate-200 bg-white">
        <div className="mx-auto grid max-w-6xl gap-6 px-4 py-14 md:grid-cols-3">
          {[
            ['🎟️', 'Đặt vé & tour một luồng', 'Giữ chỗ 10 phút, áp voucher, thanh toán mô phỏng và nhận vé QR.'],
            ['🤖', 'AI trả lời có nguồn', 'RAG trên dữ liệu sự kiện, tour, chính sách — trích nguồn và biết từ chối.'],
            ['📊', 'Nền tảng hai phía', 'Nhà tổ chức tự đăng bán, xem doanh thu; quản trị duyệt nội dung và giám sát AI.'],
          ].map(([icon, title, body]) => (
            <article key={title} className="rounded-2xl border border-slate-200 p-6">
              <div className="text-3xl">{icon}</div>
              <h3 className="mt-3 font-semibold text-slate-900">{title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-slate-600">{body}</p>
            </article>
          ))}
        </div>
      </section>
    </div>
  )
}
