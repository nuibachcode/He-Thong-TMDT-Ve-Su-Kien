import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../features/auth/auth-context'
import { createBooking, getErrorMessage, getEvent } from '../lib/api'
import { formatCurrency, formatDateTime } from '../lib/format'

export default function EventDetail() {
  const { slug = '' } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [quantities, setQuantities] = useState<Record<number, number>>({})
  const [voucher, setVoucher] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const event = useQuery({ queryKey: ['event', slug], queryFn: () => getEvent(slug), enabled: !!slug })

  if (event.isLoading) return <p className="mx-auto max-w-5xl px-4 py-16 text-slate-500">Đang tải...</p>
  if (event.isError || !event.data)
    return <p className="mx-auto max-w-5xl px-4 py-16 text-rose-600">{getErrorMessage(event.error)}</p>

  const data = event.data
  const selected = Object.entries(quantities).filter(([, qty]) => qty > 0)
  const total = data.tickets.reduce((sum, t) => sum + t.price * (quantities[t.id] ?? 0), 0)

  const change = (id: number, delta: number, max: number) => {
    const current = quantities[id] ?? 0
    const next = Math.min(Math.max(0, current + delta), max)
    setQuantities((prev) => ({ ...prev, [id]: next }))
  }

  const submit = async () => {
    if (!user) {
      navigate('/login', { state: { from: `/events/${slug}` } })
      return
    }
    if (selected.length === 0) {
      setError('Vui lòng chọn ít nhất 1 vé')
      return
    }
    setError('')
    setSubmitting(true)
    try {
      const booking = await createBooking({
        bookingType: 'EVENT',
        items: selected.map(([refId, qty]) => ({ refId: Number(refId), quantity: qty })),
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
      <Link to="/events" className="text-sm text-brand-700 hover:underline">
        ← Danh sách sự kiện
      </Link>

      <div className="mt-4 overflow-hidden rounded-2xl border border-slate-200 bg-white">
        <img src={data.coverImageUrl} alt={data.title} className="h-64 w-full bg-slate-200 object-cover" />
        <div className="p-6">
          <p className="text-xs font-semibold uppercase tracking-wide text-brand-600">{data.category}</p>
          <h1 className="mt-1 text-2xl font-bold text-slate-900">{data.title}</h1>
          <p className="mt-2 text-slate-600">
            {formatDateTime(data.startAt)} — {formatDateTime(data.endAt)}
          </p>
          <p className="text-slate-600">
            {data.venueName}
            {data.address ? ` · ${data.address}` : ''}
            {data.city ? ` · ${data.city}` : ''}
          </p>
          {data.description && (
            <p className="mt-4 whitespace-pre-line leading-relaxed text-slate-700">{data.description}</p>
          )}
        </div>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 space-y-3">
          {data.tickets.map((ticket) => {
            const qty = quantities[ticket.id] ?? 0
            const max = Math.min(ticket.maxPerOrder, ticket.remaining)
            return (
              <div key={ticket.id} className="flex items-center justify-between rounded-xl border border-slate-200 bg-white p-4">
                <div>
                  <p className="font-semibold text-slate-900">{ticket.name}</p>
                  <p className="text-sm text-slate-600">
                    {formatCurrency(ticket.price)} · còn {ticket.remaining} vé · tối đa {ticket.maxPerOrder}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => change(ticket.id, -1, max)}
                    disabled={qty === 0}
                    className="size-9 rounded-lg border border-slate-300 font-bold disabled:opacity-40"
                  >
                    −
                  </button>
                  <span className="w-6 text-center font-semibold">{qty}</span>
                  <button
                    onClick={() => change(ticket.id, 1, max)}
                    disabled={qty >= max}
                    className="size-9 rounded-lg border border-slate-300 font-bold disabled:opacity-40"
                  >
                    +
                  </button>
                </div>
              </div>
            )
          })}
        </div>

        <aside className="h-fit rounded-2xl border border-slate-200 bg-white p-5">
          <h2 className="font-semibold text-slate-900">Đơn của bạn</h2>
          <div className="mt-3 space-y-1 text-sm text-slate-600">
            {selected.length === 0 && <p>Chưa chọn vé.</p>}
            {data.tickets
              .filter((t) => (quantities[t.id] ?? 0) > 0)
              .map((t) => (
                <p key={t.id}>
                  {t.name} × {quantities[t.id]}
                </p>
              ))}
          </div>
          <p className="mt-4 text-lg font-bold text-slate-900">Tạm tính: {formatCurrency(total)}</p>

          <input
            value={voucher}
            onChange={(e) => setVoucher(e.target.value)}
            placeholder="Mã giảm giá (WELCOME10)"
            className="mt-4 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
          />

          {error && <p className="mt-3 text-sm text-rose-600">{error}</p>}

          <button
            onClick={submit}
            disabled={submitting}
            className="mt-4 w-full rounded-lg bg-brand-600 px-4 py-2.5 font-semibold text-white hover:bg-brand-700 disabled:opacity-60"
          >
            {submitting ? 'Đang giữ chỗ...' : 'Đặt vé (giữ chỗ 10 phút)'}
          </button>
          <p className="mt-2 text-center text-xs text-slate-500">Thanh toán mô phỏng, không trừ tiền thật.</p>
        </aside>
      </div>
    </div>
  )
}
