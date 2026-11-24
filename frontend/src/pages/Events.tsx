import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { getErrorMessage, listEvents } from '../lib/api'
import { formatCurrency, formatDateTime } from '../lib/format'

const categories = [
  { value: '', label: 'Tất cả' },
  { value: 'music', label: 'Âm nhạc' },
  { value: 'theater', label: 'Sân khấu' },
  { value: 'workshop', label: 'Workshop' },
  { value: 'sport', label: 'Thể thao' },
]

export default function Events() {
  const [q, setQ] = useState('')
  const [category, setCategory] = useState('')
  const [search, setSearch] = useState('')

  const events = useQuery({
    queryKey: ['events', search, category],
    queryFn: () => listEvents(search || undefined, category || undefined),
  })

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <h1 className="text-2xl font-bold text-slate-900">Sự kiện đang mở bán</h1>

      <form
        className="mt-5 flex flex-col gap-3 sm:flex-row"
        onSubmit={(e) => {
          e.preventDefault()
          setSearch(q.trim())
        }}
      >
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Tìm theo tên sự kiện..."
          className="flex-1 rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-brand-500"
        />
        <select
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          className="rounded-lg border border-slate-300 px-3 py-2"
        >
          {categories.map((c) => (
            <option key={c.value} value={c.value}>
              {c.label}
            </option>
          ))}
        </select>
        <button className="rounded-lg bg-brand-600 px-4 py-2 font-medium text-white hover:bg-brand-700">
          Tìm
        </button>
      </form>

      {events.isLoading && <p className="mt-8 text-slate-500">Đang tải...</p>}
      {events.isError && <p className="mt-8 text-rose-600">{getErrorMessage(events.error)}</p>}
      {events.data?.length === 0 && <p className="mt-8 text-slate-500">Không tìm thấy sự kiện phù hợp.</p>}

      <div className="mt-6 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {events.data?.map((event) => (
          <Link
            key={event.id}
            to={`/events/${event.slug}`}
            className="group overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
          >
            <img
              src={event.coverImageUrl}
              alt={event.title}
              className="h-40 w-full bg-slate-200 object-cover"
              loading="lazy"
            />
            <div className="p-4">
              <p className="text-xs font-medium uppercase tracking-wide text-brand-600">{event.category}</p>
              <h2 className="mt-1 line-clamp-2 font-semibold text-slate-900 group-hover:text-brand-700">
                {event.title}
              </h2>
              <p className="mt-2 text-sm text-slate-600">{formatDateTime(event.startAt)}</p>
              <p className="text-sm text-slate-600">
                {event.venueName} {event.city ? `· ${event.city}` : ''}
              </p>
              <p className="mt-3 font-bold text-brand-700">Từ {formatCurrency(event.minPrice)}</p>
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}
