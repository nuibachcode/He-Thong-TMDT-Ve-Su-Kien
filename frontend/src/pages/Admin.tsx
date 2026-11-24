import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import {
  adminAiMetrics,
  adminPending,
  adminSetContentStatus,
  adminStats,
  adminUsers,
  getErrorMessage,
} from '../lib/api'
import { formatCurrency } from '../lib/format'

export default function Admin() {
  const client = useQueryClient()
  const [error, setError] = useState('')

  const stats = useQuery({ queryKey: ['admin-stats'], queryFn: adminStats })
  const metrics = useQuery({ queryKey: ['admin-ai'], queryFn: adminAiMetrics })
  const pending = useQuery({ queryKey: ['admin-pending'], queryFn: adminPending })
  const users = useQuery({ queryKey: ['admin-users'], queryFn: adminUsers })

  const review = useMutation({
    mutationFn: (input: { type: string; id: number; status: string }) =>
      adminSetContentStatus(input.type, input.id, input.status),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['admin-pending'] })
      client.invalidateQueries({ queryKey: ['admin-stats'] })
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <h1 className="text-2xl font-bold text-slate-900">Quản trị hệ thống</h1>
      {error && <p className="mt-3 rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}

      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        {[
          { label: 'Người dùng', value: stats.data?.users ?? 0 },
          { label: 'Sự kiện', value: stats.data?.events ?? 0 },
          { label: 'Tour', value: stats.data?.tours ?? 0 },
          { label: 'Đơn hàng', value: stats.data?.bookings ?? 0 },
          { label: 'Doanh thu', value: formatCurrency(stats.data?.revenue ?? 0) },
        ].map((card) => (
          <div key={card.label} className="rounded-2xl border border-slate-200 bg-white p-4">
            <p className="text-xs text-slate-500">{card.label}</p>
            <p className="mt-1 text-lg font-bold text-slate-900">{card.value}</p>
          </div>
        ))}
      </div>

      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[
          { label: 'Câu hỏi AI', value: metrics.data?.questions ?? 0 },
          { label: 'Thời gian TB', value: `${metrics.data?.avgLatencyMs ?? 0} ms` },
          { label: 'Tỷ lệ dự phòng', value: metrics.data?.answers ? `${Math.round(((metrics.data.fallback) / metrics.data.answers) * 100)}%` : '0%' },
          { label: 'Đánh giá TB', value: metrics.data?.feedbackAvg ?? 0 },
        ].map((card) => (
          <div key={card.label} className="rounded-2xl border border-brand-100 bg-brand-50 p-4">
            <p className="text-xs text-brand-700">{card.label}</p>
            <p className="mt-1 text-lg font-bold text-brand-900">{card.value}</p>
          </div>
        ))}
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <section className="rounded-2xl border border-slate-200 bg-white p-5">
          <h2 className="font-semibold text-slate-900">Nội dung chờ duyệt</h2>
          <div className="mt-3 space-y-2 text-sm">
            {pending.data?.length === 0 && <p className="text-slate-500">Không có nội dung chờ duyệt.</p>}
            {pending.data?.map((item) => (
              <div key={`${item.type}-${item.id}`} className="flex items-center justify-between border-b border-slate-100 pb-2">
                <div>
                  <p className="font-medium text-slate-800">{item.title}</p>
                  <p className="text-xs text-slate-500">{item.type}</p>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => review.mutate({ type: item.type, id: item.id, status: 'PUBLISHED' })}
                    className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white"
                  >
                    Duyệt
                  </button>
                  <button
                    onClick={() => review.mutate({ type: item.type, id: item.id, status: 'REJECTED' })}
                    className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700"
                  >
                    Từ chối
                  </button>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-5">
          <h2 className="font-semibold text-slate-900">Người dùng</h2>
          <div className="mt-3 max-h-72 space-y-2 overflow-auto text-sm">
            {users.data?.map((user) => (
              <div key={user.id} className="flex items-center justify-between border-b border-slate-100 pb-2">
                <div>
                  <p className="font-medium text-slate-800">{user.fullName}</p>
                  <p className="text-xs text-slate-500">
                    {user.email} · {user.role}
                  </p>
                </div>
                <span className={`text-xs font-semibold ${user.enabled ? 'text-emerald-600' : 'text-rose-600'}`}>
                  {user.enabled ? 'Hoạt động' : 'Đã khoá'}
                </span>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  )
}
