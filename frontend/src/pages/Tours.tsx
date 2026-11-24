import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../features/auth/auth-context'
import { createBooking, getErrorMessage, getTour, listTours } from '../lib/api'
import { formatCurrency } from '../lib/format'

export function Tours() {
  const [q, setQ] = useState('')
  const [search, setSearch] = useState('')
  const tours = useQuery({ queryKey: ['tours', search], queryFn: () => listTours(search || undefined) })

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <h1 className="text-2xl font-bold text-slate-900">Tour du lịch</h1>
      <form
        className="mt-5 flex gap-3"
        onSubmit={(e) => {
          e.preventDefault()
          setSearch(q.trim())
        }}
      >
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Tìm tour, điểm đến..."
          className="flex-1 rounded-lg border border-slate-300 px-3 py-2"
        />
        <button className="rounded-lg bg-brand-600 px-4 py-2 font-medium text-white hover:bg-brand-700">Tìm</button>
      </form>

      {tours.isLoading && <p className="mt-8 text-slate-500">Đang tải...</p>}
      {tours.isError && <p className="mt-8 text-rose-600">{getErrorMessage(tours.error)}</p>}
      {tours.data?.length === 0 && <p className="mt-8 text-slate-500">Không tìm thấy tour phù hợp.</p>}

      <div className="mt-6 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {tours.data?.map((tour) => (
          <Link
            key={tour.id}
            to={`/tours/${tour.slug}`}
            className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
          >
            <img src={tour.coverImageUrl} alt={tour.title} className="h-40 w-full bg-slate-200 object-cover" loading="lazy" />
            <div className="p-4">
              <p className="text-xs font-medium uppercase tracking-wide text-brand-600">{tour.destination}</p>
              <h2 className="mt-1 line-clamp-2 font-semibold text-slate-900">{tour.title}</h2>
              <p className="mt-2 text-sm text-slate-600">
                {tour.durationDays} ngày {tour.durationNights} đêm · khởi hành từ {tour.departureCity}
              </p>
              <p className="mt-3 font-bold text-brand-700">{formatCurrency(tour.basePrice)}</p>
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}

export function TourDetail() {
  const { slug = '' } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [scheduleId, setScheduleId] = useState<number | null>(null)
  const [guests, setGuests] = useState(1)
  const [voucher, setVoucher] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const tour = useQuery({ queryKey: ['tour', slug], queryFn: () => getTour(slug), enabled: !!slug })
  if (tour.isLoading) return <p className="mx-auto max-w-5xl px-4 py-16 text-slate-500">Đang tải...</p>
  if (tour.isError || !tour.data)
    return <p className="mx-auto max-w-5xl px-4 py-16 text-rose-600">{getErrorMessage(tour.error)}</p>

  const data = tour.data
  const schedule = data.schedules.find((s) => s.id === scheduleId) ?? data.schedules[0]

  const submit = async () => {
    if (!user) {
      navigate('/login', { state: { from: `/tours/${slug}` } })
      return
    }
    if (!schedule) {
      setError('Tour chưa có lịch khởi hành')
      return
    }
    setError('')
    setSubmitting(true)
    try {
      const booking = await createBooking({
        bookingType: 'TOUR',
        items: [{ refId: schedule.id, quantity: guests }],
        voucherCode: voucher.trim() || undefined,
      })
      navigate(`/pay/${booking.code}`)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-10">
      <Link to="/tours" className="text-sm text-brand-700 hover:underline">
        ← Danh sách tour
      </Link>

      <div className="mt-4 overflow-hidden rounded-2xl border border-slate-200 bg-white">
        <img src={data.coverImageUrl} alt={data.title} className="h-64 w-full bg-slate-200 object-cover" />
        <div className="p-6">
          <p className="text-xs font-semibold uppercase tracking-wide text-brand-600">{data.destination}</p>
          <h1 className="mt-1 text-2xl font-bold text-slate-900">{data.title}</h1>
          <p className="mt-2 text-slate-600">
            {data.durationDays} ngày {data.durationNights} đêm · khởi hành từ {data.departureCity} · từ{' '}
            {formatCurrency(data.basePrice)}
          </p>
          {data.description && <p className="mt-4 whitespace-pre-line leading-relaxed text-slate-700">{data.description}</p>}

          <div className="mt-5 grid gap-4 sm:grid-cols-3">
            <div>
              <p className="text-sm font-semibold text-slate-900">Điểm nổi bật</p>
              <ul className="mt-1 list-inside list-disc text-sm text-slate-600">
                {data.highlights.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900">Bao gồm</p>
              <ul className="mt-1 list-inside list-disc text-sm text-slate-600">
                {data.included.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900">Không bao gồm</p>
              <ul className="mt-1 list-inside list-disc text-sm text-slate-600">
                {data.excluded.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <div className="space-y-3 lg:col-span-2">
          <h2 className="font-semibold text-slate-900">Chọn lịch khởi hành</h2>
          {data.schedules.map((s) => (
            <label
              key={s.id}
              className={`flex cursor-pointer items-center justify-between rounded-xl border p-4 ${
                schedule?.id === s.id ? 'border-brand-500 bg-brand-50' : 'border-slate-200 bg-white'
              }`}
            >
              <div className="flex items-center gap-3">
                <input
                  type="radio"
                  name="schedule"
                  checked={schedule?.id === s.id}
                  onChange={() => setScheduleId(s.id)}
                />
                <div>
                  <p className="font-medium text-slate-900">
                    {s.departureDate} → {s.returnDate}
                  </p>
                  <p className="text-sm text-slate-600">Còn {s.remaining} chỗ</p>
                </div>
              </div>
              <p className="font-bold text-brand-700">{formatCurrency(s.price)}</p>
            </label>
          ))}
        </div>

        <aside className="h-fit rounded-2xl border border-slate-200 bg-white p-5">
          <h2 className="font-semibold text-slate-900">Đặt tour</h2>
          <label className="mt-3 block text-sm text-slate-600">
            Số khách
            <input
              type="number"
              min={1}
              max={schedule ? Math.min(10, schedule.remaining) : 1}
              value={guests}
              onChange={(e) => setGuests(Math.max(1, Number(e.target.value)))}
              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
            />
          </label>
          <input
            value={voucher}
            onChange={(e) => setVoucher(e.target.value)}
            placeholder="Mã giảm giá (TOUR500)"
            className="mt-3 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
          />
          {schedule && (
            <p className="mt-4 text-lg font-bold text-slate-900">
              Tạm tính: {formatCurrency(schedule.price * guests)}
            </p>
          )}
          {error && <p className="mt-3 text-sm text-rose-600">{error}</p>}
          <button
            onClick={submit}
            disabled={submitting || !schedule}
            className="mt-4 w-full rounded-lg bg-brand-600 px-4 py-2.5 font-semibold text-white hover:bg-brand-700 disabled:opacity-60"
          >
            {submitting ? 'Đang giữ chỗ...' : 'Đặt tour'}
          </button>
        </aside>
      </div>
    </div>
  )
}
