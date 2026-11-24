import { useMutation, useQuery } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getBooking, getErrorMessage, payBooking } from '../lib/api'
import { formatCurrency, formatDateTime } from '../lib/format'

export default function Pay() {
  const { code = '' } = useParams()
  const navigate = useNavigate()
  const booking = useQuery({ queryKey: ['booking', code], queryFn: () => getBooking(code), enabled: !!code })

  const pay = useMutation({
    mutationFn: () => payBooking(code),
    onSuccess: () => navigate('/tickets'),
  })

  if (booking.isLoading) return <p className="mx-auto max-w-3xl px-4 py-16 text-slate-500">Đang tải đơn...</p>
  if (booking.isError || !booking.data)
    return <p className="mx-auto max-w-3xl px-4 py-16 text-rose-600">{getErrorMessage(booking.error)}</p>

  const data = booking.data
  const paid = data.status === 'CONFIRMED'

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <h1 className="text-2xl font-bold text-slate-900">Thanh toán đơn {data.code}</h1>
      <p className="mt-1 text-sm text-slate-600">
        Trạng thái: <strong>{data.status}</strong>
        {data.expiresAt ? ` · giữ chỗ đến ${formatDateTime(data.expiresAt)}` : ''}
      </p>

      <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-6">
        <h2 className="font-semibold text-slate-900">Chi tiết</h2>
        <ul className="mt-3 divide-y divide-slate-100">
          {data.items.map((item) => (
            <li key={item.id} className="flex items-center justify-between py-3 text-sm">
              <div>
                <p className="font-medium text-slate-800">{item.itemName}</p>
                <p className="text-slate-500">
                  {item.quantity} × {formatCurrency(item.unitPrice)}
                </p>
              </div>
              <p className="font-semibold text-slate-900">{formatCurrency(item.amount)}</p>
            </li>
          ))}
        </ul>

        <dl className="mt-4 space-y-2 border-t border-slate-100 pt-4 text-sm">
          <div className="flex justify-between">
            <dt className="text-slate-600">Tạm tính</dt>
            <dd>{formatCurrency(data.subtotal)}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-600">Giảm giá</dt>
            <dd className="text-emerald-600">−{formatCurrency(data.discountAmount)}</dd>
          </div>
          <div className="flex justify-between text-base font-bold">
            <dt>Tổng cộng</dt>
            <dd className="text-brand-700">{formatCurrency(data.totalAmount)}</dd>
          </div>
        </dl>

        {pay.isError && <p className="mt-4 text-sm text-rose-600">{getErrorMessage(pay.error)}</p>}

        {paid ? (
          <div className="mt-6 rounded-xl bg-emerald-50 p-4 text-sm text-emerald-800">
            Đơn đã thanh toán. Vé điện tử đã sẵn sàng.{' '}
            <Link to="/tickets" className="font-semibold underline">
              Xem vé của tôi
            </Link>
          </div>
        ) : (
          <button
            onClick={() => pay.mutate()}
            disabled={pay.isPending}
            className="mt-6 w-full rounded-xl bg-brand-600 px-4 py-3 font-semibold text-white hover:bg-brand-700 disabled:opacity-60"
          >
            {pay.isPending ? 'Đang xử lý...' : 'Thanh toán mô phỏng'}
          </button>
        )}
        <p className="mt-2 text-center text-xs text-slate-500">
          Đây là cổng thanh toán mô phỏng phục vụ đồ án, không phát sinh giao dịch thật.
        </p>
      </div>
    </div>
  )
}
