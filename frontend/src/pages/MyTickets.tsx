import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { getErrorMessage, getTickets, listBookings } from '../lib/api'
import { formatCurrency, formatDateTime } from '../lib/format'

export default function MyTickets() {
  const bookings = useQuery({ queryKey: ['bookings'], queryFn: listBookings })
  const [openCode, setOpenCode] = useState<string | null>(null)

  const tickets = useQuery({
    queryKey: ['tickets', openCode],
    queryFn: () => getTickets(openCode as string),
    enabled: !!openCode,
  })

  return (
    <div className="mx-auto max-w-4xl px-4 py-10">
      <h1 className="text-2xl font-bold text-slate-900">Vé của tôi</h1>

      {bookings.isLoading && <p className="mt-6 text-slate-500">Đang tải...</p>}
      {bookings.isError && <p className="mt-6 text-rose-600">{getErrorMessage(bookings.error)}</p>}
      {bookings.data?.length === 0 && (
        <p className="mt-6 text-slate-600">
          Bạn chưa có đơn nào. <Link to="/events" className="text-brand-700 underline">Khám phá sự kiện</Link>
        </p>
      )}

      <div className="mt-6 space-y-3">
        {bookings.data?.map((booking) => (
          <div key={booking.code} className="rounded-2xl border border-slate-200 bg-white p-5">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <p className="font-semibold text-slate-900">{booking.code}</p>
                <p className="text-sm text-slate-600">
                  {booking.bookingType} · {formatDateTime(booking.createdAt)} · {booking.itemCount} mục
                </p>
              </div>
              <div className="text-right">
                <p className="font-bold text-brand-700">{formatCurrency(booking.totalAmount)}</p>
                <p className="text-xs font-medium text-slate-500">{booking.status}</p>
              </div>
            </div>

            <div className="mt-3 flex gap-3 text-sm">
              {booking.status === 'PENDING' && (
                <Link to={`/pay/${booking.code}`} className="rounded-lg bg-brand-600 px-3 py-1.5 font-medium text-white">
                  Thanh toán
                </Link>
              )}
              {booking.status === 'CONFIRMED' && (
                <button
                  onClick={() => setOpenCode(openCode === booking.code ? null : booking.code)}
                  className="rounded-lg border border-slate-300 px-3 py-1.5 font-medium text-slate-700"
                >
                  {openCode === booking.code ? 'Đóng vé' : 'Xem vé QR'}
                </button>
              )}
            </div>

            {openCode === booking.code && (
              <div className="mt-4 rounded-xl bg-slate-50 p-4">
                {tickets.isLoading && <p className="text-sm text-slate-500">Đang tạo mã QR...</p>}
                {tickets.data?.map((ticket) => (
                  <div key={ticket.ticketId} className="mb-4 flex items-center gap-4 last:mb-0">
                    <img
                      src={`data:image/png;base64,${ticket.qrPngBase64}`}
                      alt="QR vé"
                      className="size-36 rounded-lg bg-white p-2 ring-1 ring-slate-200"
                    />
                    <div>
                      <p className="font-medium text-slate-900">{ticket.itemName}</p>
                      <p className="text-sm text-slate-600">Số lượng: {ticket.quantity}</p>
                      <p className="mt-1 text-xs text-slate-500">Xuất trình mã này tại cổng soát vé.</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
