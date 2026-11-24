import axios from 'axios'

export const TOKEN_KEY = 'tmdt_token'
export const GUEST_KEY = 'tmdt_guest_key'

function guestKey() {
  let key = localStorage.getItem(GUEST_KEY)
  if (!key) {
    key =
      typeof crypto !== 'undefined' && 'randomUUID' in crypto
        ? crypto.randomUUID()
        : `guest-${Date.now()}-${Math.random().toString(36).slice(2)}`
    localStorage.setItem(GUEST_KEY, key)
  }
  return key
}

export const api = axios.create({
  baseURL: '/api',
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['X-Guest-Key'] = guestKey()
  return config
})

export type ApiEnvelope<T> = {
  success: boolean
  message?: string
  data: T
}

export type User = {
  id: number
  email: string
  fullName: string
  phone?: string
  role: 'CUSTOMER' | 'ORGANIZER' | 'ADMIN'
}

export type AuthPayload = {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export type HealthPayload = { status: string; app: string; time: string }

export type EventCard = {
  id: number
  title: string
  slug: string
  category: string
  coverImageUrl?: string
  startAt: string
  venueName?: string
  city?: string
  minPrice: number
}

export type TicketType = { id: number; name: string; price: number; remaining: number; maxPerOrder: number }

export type EventDetail = EventCard & {
  endAt: string
  description?: string
  address?: string
  tickets: TicketType[]
}

export type TourCard = {
  id: number
  title: string
  slug: string
  destination: string
  departureCity: string
  durationDays: number
  durationNights: number
  basePrice: number
  coverImageUrl?: string
}

export type Schedule = {
  id: number
  departureDate: string
  returnDate: string
  price: number
  remaining: number
}

export type TourDetail = TourCard & {
  description?: string
  highlights: string[]
  included: string[]
  excluded: string[]
  schedules: Schedule[]
}

export type BookingSummary = {
  code: string
  bookingType: string
  status: string
  totalAmount: number
  createdAt: string
  expiresAt?: string
  itemCount: number
}

export type BookingItem = {
  id: number
  itemName: string
  quantity: number
  unitPrice: number
  amount: number
}

export type BookingDetail = BookingSummary & {
  subtotal: number
  discountAmount: number
  contactName: string
  contactEmail?: string
  contactPhone?: string
  confirmedAt?: string
  items: BookingItem[]
}

export type Ticket = { ticketId: number; itemName: string; quantity: number; qrPngBase64: string }

export type Citation = { sourceType: string; sourceId?: number; title: string; url?: string; score: number }

export type ChatAnswer = {
  sessionId: number
  messageId: number
  answer: string
  citations: Citation[]
  provider: string
  latencyMs: number
  refused: boolean
  fallback: boolean
}

export type Recommendation = {
  type: 'EVENT' | 'TOUR'
  id: number
  title: string
  slug: string
  subtitle: string
  price: number
  coverImageUrl?: string
  reason: string
  url: string
  tags?: string[]
}

export type OrgStats = {
  revenue: number
  orders: number
  ticketsSold: number
  fillRate: number
  pendingEvents: number
  pendingTours: number
}

export type OrgBooking = {
  code: string
  contactName: string
  contactEmail: string
  status: string
  totalAmount: number
  createdAt: string
  itemSummary: string
}

export type OrgEvent = {
  id: number
  title: string
  slug: string
  status: string
  category: string
  startAt: string
  sold: number
  quota: number
}

export type AdminUser = { id: number; email: string; fullName: string; role: string; enabled: boolean }
export type PendingItem = { id: number; title: string; type: string; status: string }
export type AdminStats = { users: number; events: number; tours: number; bookings: number; revenue: number }
export type AiMetrics = {
  questions: number
  answers: number
  fallback: number
  refused: number
  avgLatencyMs: number
  feedbackAvg: number
  positive: number
  negative: number
}

const unwrap = <T>(promise: Promise<{ data: ApiEnvelope<T> }>) => promise.then((res) => res.data.data)

export const getHealth = () => unwrap(api.get<ApiEnvelope<HealthPayload>>('/health'))

export const login = (email: string, password: string) =>
  unwrap(api.post<ApiEnvelope<AuthPayload>>('/auth/login', { email, password }))

export const register = (input: { email: string; password: string; fullName: string; phone?: string }) =>
  unwrap(api.post<ApiEnvelope<AuthPayload>>('/auth/register', input))

export const fetchMe = () => unwrap(api.get<ApiEnvelope<User>>('/auth/me'))

export const listEvents = (q?: string, category?: string) =>
  unwrap(api.get<ApiEnvelope<EventCard[]>>('/public/events', { params: { q, category } }))

export const getEvent = (slug: string) => unwrap(api.get<ApiEnvelope<EventDetail>>(`/public/events/${slug}`))

export const listTours = (q?: string, destination?: string) =>
  unwrap(api.get<ApiEnvelope<TourCard[]>>('/public/tours', { params: { q, destination } }))

export const getTour = (slug: string) => unwrap(api.get<ApiEnvelope<TourDetail>>(`/public/tours/${slug}`))

export const createBooking = (payload: {
  bookingType: 'EVENT' | 'TOUR'
  items: { refId: number; quantity: number }[]
  voucherCode?: string
  contactName?: string
  contactEmail?: string
  contactPhone?: string
  note?: string
}) => unwrap(api.post<ApiEnvelope<BookingSummary>>('/bookings', payload))

export const listBookings = () => unwrap(api.get<ApiEnvelope<BookingSummary[]>>('/bookings'))

export const getBooking = (code: string) => unwrap(api.get<ApiEnvelope<BookingDetail>>(`/bookings/${code}`))

export const cancelBooking = (code: string) => unwrap(api.post<ApiEnvelope<BookingSummary>>(`/bookings/${code}/cancel`))

export const payBooking = (code: string) =>
  unwrap(api.post<ApiEnvelope<{ status: string; txnRef: string; amount: number }>>(`/payments/${code}/pay`))

export const getTickets = (code: string) => unwrap(api.get<ApiEnvelope<Ticket[]>>(`/bookings/${code}/tickets`))

export const askAi = (message: string, sessionId?: number) =>
  unwrap(api.post<ApiEnvelope<ChatAnswer>>('/ai/chat', { message, sessionId }))

export const sendFeedback = (messageId: number, rating: number) =>
  unwrap(api.post<ApiEnvelope<string>>('/ai/feedback', { messageId, rating }))

export const getRecommendations = (limit = 4, interests: string[] = []) =>
  unwrap(
    api.get<ApiEnvelope<Recommendation[]>>('/ai/recommendations', {
      params: { limit, interests: interests.length ? interests.join(',') : undefined },
    }),
  )

export const saveInterests = (interests: string[]) =>
  unwrap(api.put<ApiEnvelope<string[]>>('/ai/interests', { interests }))

export const orgStats = () => unwrap(api.get<ApiEnvelope<OrgStats>>('/organizer/stats/overview'))
export const orgBookings = () => unwrap(api.get<ApiEnvelope<OrgBooking[]>>('/organizer/bookings'))
export const orgEvents = () => unwrap(api.get<ApiEnvelope<OrgEvent[]>>('/organizer/events'))

export const createEvent = (payload: {
  title: string
  category: string
  description?: string
  startAt: string
  endAt?: string
  coverImageUrl?: string
  tickets: { name: string; price: number; quota: number; maxPerOrder: number }[]
}) => unwrap(api.post<ApiEnvelope<OrgEvent>>('/organizer/events', payload))

export const createTour = (payload: {
  title: string
  destination: string
  departureCity: string
  durationDays: number
  durationNights: number
  basePrice: number
  description?: string
  highlights?: string
  included?: string
  excluded?: string
  schedules: { departureDate: string; price: number; seatsTotal: number }[]
}) => unwrap(api.post<ApiEnvelope<{ id: number; title: string; status: string }>>('/organizer/tours', payload))

export const aiDescribe = (payload: { destination: string; days: number; interests: string }) =>
  unwrap(api.post<ApiEnvelope<{ description: string }>>('/organizer/ai/describe', payload))

export const adminStats = () => unwrap(api.get<ApiEnvelope<AdminStats>>('/admin/stats/overview'))
export const adminUsers = () => unwrap(api.get<ApiEnvelope<AdminUser[]>>('/admin/users'))
export const adminPending = () => unwrap(api.get<ApiEnvelope<PendingItem[]>>('/admin/pending'))
export const adminSetContentStatus = (type: string, id: number, status: string) =>
  unwrap(api.patch<ApiEnvelope<string>>(`/admin/content/${type}/${id}/status`, { status }))
export const adminAiMetrics = () => unwrap(api.get<ApiEnvelope<AiMetrics>>('/ai/metrics'))

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const payload = error.response?.data as { message?: string } | undefined
    if (payload?.message) return payload.message
    if (error.response?.status === 401) return 'Ban can dang nhap de tiep tuc'
    if (!error.response) return 'Khong ket noi duoc toi may chu. Kiem tra backend da chay chua?'
  }
  return 'Da co loi xay ra, vui long thu lai'
}
