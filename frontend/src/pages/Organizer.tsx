import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import {
  aiDescribe,
  createEvent,
  createTour,
  getErrorMessage,
  orgBookings,
  orgCheckin,
  orgEvents,
  orgStats,
} from '../lib/api'
import { formatCurrency, formatDateTime } from '../lib/format'

export default function Organizer() {
  const client = useQueryClient()
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const stats = useQuery({ queryKey: ['org-stats'], queryFn: orgStats })
  const bookings = useQuery({ queryKey: ['org-bookings'], queryFn: orgBookings })
  const events = useQuery({ queryKey: ['org-events'], queryFn: orgEvents })

  const [eventForm, setEventForm] = useState({
    title: '',
    category: 'music',
    startAt: '',
    price: 500000,
    quota: 100,
    description: '',
  })
  const [tourForm, setTourForm] = useState({
    title: '',
    destination: '',
    departureCity: 'TP. Hồ Chí Minh',
    days: 3,
    price: 2500000,
    departureDate: '',
    seats: 20,
    description: '',
  })
  const [describeInput, setDescribeInput] = useState({ destination: '', days: 3, interests: 'biển, ẩm thực' })
  const [checkinForm, setCheckinForm] = useState({ bookingCode: '', ticketId: '' })
  const [checkinMessage, setCheckinMessage] = useState('')

  const checkin = useMutation({
    mutationFn: () => orgCheckin(checkinForm.bookingCode.trim(), Number(checkinForm.ticketId)),
    onSuccess: (message) => {
      setCheckinMessage(message)
      setError('')
    },
    onError: (err) => {
      setCheckinMessage('')
      setError(getErrorMessage(err))
    },
  })

  const submitEvent = useMutation({
    mutationFn: () =>
      createEvent({
        title: eventForm.title,
        category: eventForm.category,
        description: eventForm.description,
        startAt: eventForm.startAt,
        tickets: [
          { name: 'Standard', price: Number(eventForm.price), quota: Number(eventForm.quota), maxPerOrder: 4 },
        ],
      }),
    onSuccess: () => {
      setMessage('Đã gửi sự kiện, chờ quản trị duyệt.')
      setError('')
      client.invalidateQueries({ queryKey: ['org-events'] })
      client.invalidateQueries({ queryKey: ['org-stats'] })
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const submitTour = useMutation({
    mutationFn: () =>
      createTour({
        title: tourForm.title,
        destination: tourForm.destination,
        departureCity: tourForm.departureCity,
        durationDays: Number(tourForm.days),
        durationNights: Math.max(0, Number(tourForm.days) - 1),
        basePrice: Number(tourForm.price),
        description: tourForm.description,
        schedules: tourForm.departureDate
          ? [{ departureDate: tourForm.departureDate, price: Number(tourForm.price), seatsTotal: Number(tourForm.seats) }]
          : [],
      }),
    onSuccess: () => {
      setMessage('Đã gửi tour, chờ quản trị duyệt.')
      setError('')
      client.invalidateQueries({ queryKey: ['org-stats'] })
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const describe = useMutation({
    mutationFn: () => aiDescribe(describeInput),
    onSuccess: (result) => setTourForm((prev) => ({ ...prev, description: result.description })),
    onError: (err) => setError(getErrorMessage(err)),
  })

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <h1 className="text-2xl font-bold text-slate-900">Kênh nhà tổ chức</h1>

      {message && <p className="mt-3 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">{message}</p>}
      {error && <p className="mt-3 rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}

      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[
          { label: 'Doanh thu', value: formatCurrency(stats.data?.revenue ?? 0) },
          { label: 'Đơn đã xác nhận', value: stats.data?.orders ?? 0 },
          { label: 'Vé đã bán', value: stats.data?.ticketsSold ?? 0 },
          { label: 'Tỷ lệ lấp đầy', value: `${Math.round((stats.data?.fillRate ?? 0) * 100)}%` },
        ].map((card) => (
          <div key={card.label} className="rounded-2xl border border-slate-200 bg-white p-5">
            <p className="text-sm text-slate-500">{card.label}</p>
            <p className="mt-1 text-xl font-bold text-slate-900">{card.value}</p>
          </div>
        ))}
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <section className="rounded-2xl border border-slate-200 bg-white p-5">
          <h2 className="font-semibold text-slate-900">Sự kiện của tôi</h2>
          <div className="mt-3 space-y-2 text-sm">
            {events.data?.length === 0 && <p className="text-slate-500">Chưa có sự kiện.</p>}
            {events.data?.map((event) => (
              <div key={event.id} className="flex items-center justify-between border-b border-slate-100 pb-2">
                <div>
                  <p className="font-medium text-slate-800">{event.title}</p>
                  <p className="text-xs text-slate-500">
                    {formatDateTime(event.startAt)} · {event.sold}/{event.quota} vé · {event.status}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-5">
          <h2 className="font-semibold text-slate-900">Đơn hàng gần đây</h2>
          <div className="mt-3 max-h-64 space-y-2 overflow-auto text-sm">
            {bookings.data?.length === 0 && <p className="text-slate-500">Chưa có đơn.</p>}
            {bookings.data?.map((booking) => (
              <div key={booking.code} className="flex items-center justify-between border-b border-slate-100 pb-2">
                <div>
                  <p className="font-medium text-slate-800">{booking.code}</p>
                  <p className="text-xs text-slate-500">
                    {booking.contactName} · {booking.itemSummary}
                  </p>
                </div>
                <div className="text-right">
                  <p className="font-semibold">{formatCurrency(booking.totalAmount)}</p>
                  <p className="text-xs text-slate-500">{booking.status}</p>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <form
          className="space-y-3 rounded-2xl border border-slate-200 bg-white p-5"
          onSubmit={(e) => {
            e.preventDefault()
            submitEvent.mutate()
          }}
        >
          <h2 className="font-semibold text-slate-900">Tạo sự kiện mới</h2>
          <input
            required
            placeholder="Tên sự kiện"
            value={eventForm.title}
            onChange={(e) => setEventForm({ ...eventForm, title: e.target.value })}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
          />
          <div className="grid grid-cols-2 gap-3">
            <select
              value={eventForm.category}
              onChange={(e) => setEventForm({ ...eventForm, category: e.target.value })}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
            >
              <option value="music">Âm nhạc</option>
              <option value="theater">Sân khấu</option>
              <option value="workshop">Workshop</option>
              <option value="sport">Thể thao</option>
            </select>
            <input
              required
              type="datetime-local"
              value={eventForm.startAt}
              onChange={(e) => setEventForm({ ...eventForm, startAt: e.target.value })}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <input
              type="number"
              min={0}
              value={eventForm.price}
              onChange={(e) => setEventForm({ ...eventForm, price: Number(e.target.value) })}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
              placeholder="Giá vé"
            />
            <input
              type="number"
              min={1}
              value={eventForm.quota}
              onChange={(e) => setEventForm({ ...eventForm, quota: Number(e.target.value) })}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
              placeholder="Số vé"
            />
          </div>
          <textarea
            rows={3}
            placeholder="Mô tả sự kiện"
            value={eventForm.description}
            onChange={(e) => setEventForm({ ...eventForm, description: e.target.value })}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
          />
          <button
            disabled={submitEvent.isPending}
            className="w-full rounded-lg bg-slate-900 px-4 py-2 font-semibold text-white disabled:opacity-60"
          >
            {submitEvent.isPending ? 'Đang gửi...' : 'Gửi duyệt sự kiện'}
          </button>
        </form>

        <form
          className="space-y-3 rounded-2xl border border-slate-200 bg-white p-5"
          onSubmit={(e) => {
            e.preventDefault()
            submitTour.mutate()
          }}
        >
          <h2 className="font-semibold text-slate-900">Tạo tour mới</h2>
          <input
            required
            placeholder="Tên tour"
            value={tourForm.title}
            onChange={(e) => setTourForm({ ...tourForm, title: e.target.value })}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
          />
          <div className="grid grid-cols-2 gap-3">
            <input
              required
              placeholder="Điểm đến"
              value={tourForm.destination}
              onChange={(e) => setTourForm({ ...tourForm, destination: e.target.value })}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
            />
            <input
              placeholder="Khởi hành từ"
              value={tourForm.departureCity}
              onChange={(e) => setTourForm({ ...tourForm, departureCity: e.target.value })}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <div className="grid grid-cols-3 gap-3">
            <input
              type="number"
              min={1}
              value={tourForm.days}
              onChange={(e) => setTourForm({ ...tourForm, days: Number(e.target.value) })}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
              placeholder="Số ngày"
            />
            <input
              type="number"
              min={0}
              value={tourForm.price}
              onChange={(e) => setTourForm({ ...tourForm, price: Number(e.target.value) })}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
              placeholder="Giá"
            />
            <input
              type="number"
              min={1}
              value={tourForm.seats}
              onChange={(e) => setTourForm({ ...tourForm, seats: Number(e.target.value) })}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
              placeholder="Số chỗ"
            />
          </div>
          <input
            type="date"
            value={tourForm.departureDate}
            onChange={(e) => setTourForm({ ...tourForm, departureDate: e.target.value })}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
          />
          <textarea
            rows={4}
            placeholder="Mô tả tour (hoặc bấm AI viết giúp)"
            value={tourForm.description}
            onChange={(e) => setTourForm({ ...tourForm, description: e.target.value })}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
          />
          <div className="flex flex-wrap items-end gap-2 rounded-xl bg-slate-50 p-3">
            <div className="flex-1">
              <p className="text-xs font-semibold text-slate-600">AI viết mô tả</p>
              <div className="mt-1 flex gap-2">
                <input
                  placeholder="Điểm đến"
                  value={describeInput.destination}
                  onChange={(e) => setDescribeInput({ ...describeInput, destination: e.target.value })}
                  className="w-28 rounded-lg border border-slate-300 px-2 py-1.5 text-xs"
                />
                <input
                  placeholder="Sở thích"
                  value={describeInput.interests}
                  onChange={(e) => setDescribeInput({ ...describeInput, interests: e.target.value })}
                  className="flex-1 rounded-lg border border-slate-300 px-2 py-1.5 text-xs"
                />
              </div>
            </div>
            <button
              type="button"
              onClick={() => describe.mutate()}
              disabled={describe.isPending}
              className="rounded-lg border border-brand-500 px-3 py-1.5 text-xs font-semibold text-brand-700 disabled:opacity-60"
            >
              {describe.isPending ? 'Đang viết...' : 'AI viết'}
            </button>
          </div>
          <button
            disabled={submitTour.isPending}
            className="w-full rounded-lg bg-slate-900 px-4 py-2 font-semibold text-white disabled:opacity-60"
          >
            {submitTour.isPending ? 'Đang gửi...' : 'Gửi duyệt tour'}
          </button>
        </form>
      </div>

      <section className="mt-8 rounded-2xl border border-slate-200 bg-white p-5">
        <h2 className="font-semibold text-slate-900">Soát vé QR</h2>
        <p className="mt-1 text-sm text-slate-500">
          Nhập mã đơn và mã vé (ticket ID) để xác nhận vé đã sử dụng. Mỗi vé chỉ soát được một lần.
        </p>
        <form
          className="mt-3 flex flex-wrap items-end gap-3"
          onSubmit={(e) => {
            e.preventDefault()
            setCheckinMessage('')
            checkin.mutate()
          }}
        >
          <input
            required
            placeholder="Mã đơn, ví dụ TKT..."
            value={checkinForm.bookingCode}
            onChange={(e) => setCheckinForm({ ...checkinForm, bookingCode: e.target.value })}
            className="min-w-48 flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm"
          />
          <input
            required
            type="number"
            min={1}
            placeholder="Ticket ID"
            value={checkinForm.ticketId}
            onChange={(e) => setCheckinForm({ ...checkinForm, ticketId: e.target.value })}
            className="w-32 rounded-lg border border-slate-300 px-3 py-2 text-sm"
          />
          <button
            disabled={checkin.isPending}
            className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
          >
            {checkin.isPending ? 'Đang soát...' : 'Soát vé'}
          </button>
        </form>
        {checkinMessage && <p className="mt-3 text-sm font-medium text-emerald-700">{checkinMessage}</p>}
      </section>
    </div>
  )
}
