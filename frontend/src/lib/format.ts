const currency = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

export function formatCurrency(value: number | string | null | undefined): string {
  if (value === null || value === undefined) return '—'
  const amount = typeof value === 'string' ? Number(value) : value
  return Number.isFinite(amount) ? currency.format(amount) : '—'
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: 'Asia/Ho_Chi_Minh',
  }).format(new Date(iso))
}
