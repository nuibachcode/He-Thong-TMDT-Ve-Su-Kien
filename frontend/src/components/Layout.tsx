import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useAuth } from '../features/auth/auth-context'

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  const navItems = [
    { to: '/', label: 'Trang chủ', end: true },
    { to: '/events', label: 'Sự kiện' },
    { to: '/tours', label: 'Tour du lịch' },
    { to: '/chat', label: 'Trợ lý AI' },
    ...(user ? [{ to: '/tickets', label: 'Vé của tôi' }] : []),
    ...(user && (user.role === 'ORGANIZER' || user.role === 'ADMIN')
      ? [{ to: '/organizer', label: 'Điều hành' }]
      : []),
    ...(user?.role === 'ADMIN' ? [{ to: '/admin', label: 'Quản trị' }] : []),
  ]

  const handleLogout = () => {
    logout()
    setOpen(false)
    navigate('/')
  }

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-4 px-4">
          <Link to="/" className="flex items-center gap-2 text-lg font-bold text-slate-900">
            <span className="grid size-9 place-items-center rounded-xl bg-brand-600 text-white">T</span>
            Ticket<span className="text-brand-600">Go</span>
          </Link>

          <nav className="hidden gap-1 lg:flex">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `rounded-lg px-3 py-2 text-sm font-medium transition ${
                    isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="ml-auto hidden items-center gap-2 lg:flex">
            {user ? (
              <>
                <span className="text-sm text-slate-600">
                  <strong className="text-slate-900">{user.fullName}</strong>
                  <span className="ml-1 rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-semibold text-slate-600">
                    {user.role}
                  </span>
                </span>
                <button
                  onClick={handleLogout}
                  className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
                >
                  Đăng xuất
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="rounded-lg px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100">
                  Đăng nhập
                </Link>
                <Link
                  to="/register"
                  className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700"
                >
                  Đăng ký
                </Link>
              </>
            )}
          </div>

          <button
            className="ml-auto rounded-lg p-2 text-slate-600 lg:hidden"
            onClick={() => setOpen((v) => !v)}
            aria-label="Mở menu"
          >
            ☰
          </button>
        </div>

        {open && (
          <div className="border-t border-slate-200 bg-white px-4 py-3 lg:hidden">
            <div className="flex flex-col gap-1">
              {navItems.map((item) => (
                <Link
                  key={item.to}
                  to={item.to}
                  onClick={() => setOpen(false)}
                  className="rounded-lg px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
                >
                  {item.label}
                </Link>
              ))}
              {user ? (
                <button
                  onClick={handleLogout}
                  className="rounded-lg px-3 py-2 text-left text-sm font-medium text-rose-600 hover:bg-rose-50"
                >
                  Đăng xuất
                </button>
              ) : (
                <Link
                  to="/login"
                  onClick={() => setOpen(false)}
                  className="rounded-lg px-3 py-2 text-sm font-medium text-brand-700 hover:bg-brand-50"
                >
                  Đăng nhập
                </Link>
              )}
            </div>
          </div>
        )}
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="border-t border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl flex-col gap-2 px-4 py-8 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between">
          <p>Đồ án cuối học phần — Hệ thống TMDT vé sự kiện &amp; tour du lịch tích hợp AI/RAG.</p>
          <p>Spring Boot 4 · React 19 · MySQL</p>
        </div>
      </footer>
    </div>
  )
}
