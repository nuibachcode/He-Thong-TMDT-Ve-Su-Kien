import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div className="mx-auto max-w-xl px-4 py-24 text-center">
      <p className="text-6xl font-black text-brand-600">404</p>
      <h1 className="mt-4 text-2xl font-bold text-slate-900">Trang này chưa tồn tại</h1>
      <p className="mt-2 text-slate-600">
        Có thể trang đang được nhóm phát triển ở tuần tiếp theo của kế hoạch.
      </p>
      <Link
        to="/"
        className="mt-6 inline-block rounded-xl bg-brand-600 px-5 py-3 font-semibold text-white hover:bg-brand-700"
      >
        Về trang chủ
      </Link>
    </div>
  )
}
