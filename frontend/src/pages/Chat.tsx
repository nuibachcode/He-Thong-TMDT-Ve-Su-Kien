import { useState } from 'react'
import { askAi, getErrorMessage, sendFeedback } from '../lib/api'
import type { Citation } from '../lib/api'

type Message = {
  id: string
  role: 'user' | 'assistant'
  content: string
  citations?: Citation[]
  messageId?: number
  meta?: string
}

const suggestions = [
  'Tour Đà Lạt 3 ngày và 4 ngày khác nhau thế nào?',
  'Huỷ tour trước 5 ngày có được hoàn tiền không?',
  'Tour Phú Quốc có bao gồm vé máy bay không?',
]

export default function Chat() {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: 'welcome',
      role: 'assistant',
      content:
        'Xin chào! Mình là trợ lý của TicketGo. Bạn có thể hỏi về giá, lịch khởi hành, chính sách huỷ vé hoặc so sánh các tour.',
    },
  ])
  const [input, setInput] = useState('')
  const [sessionId, setSessionId] = useState<number | undefined>()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const send = async (text: string) => {
    const question = text.trim()
    if (!question || loading) return
    setInput('')
    setError('')
    setMessages((prev) => [...prev, { id: `u-${Date.now()}`, role: 'user', content: question }])
    setLoading(true)
    try {
      const result = await askAi(question, sessionId)
      setSessionId(result.sessionId)
      setMessages((prev) => [
        ...prev,
        {
          id: `a-${result.messageId}`,
          role: 'assistant',
          content: result.answer,
          citations: result.citations,
          messageId: result.messageId,
          meta: `${result.provider} · ${result.latencyMs} ms${result.fallback ? ' · dự phòng' : ''}`,
        },
      ])
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  const vote = async (messageId: number, rating: number) => {
    try {
      await sendFeedback(messageId, rating)
    } catch {
      // bo qua loi danh gia
    }
  }

  return (
    <div className="mx-auto flex max-w-3xl flex-col px-4 py-8">
      <h1 className="text-2xl font-bold text-slate-900">Trợ lý AI</h1>
      <p className="mt-1 text-sm text-slate-600">
        Câu trả lời dựa trên dữ liệu sự kiện, tour và chính sách của hệ thống, có trích nguồn.
      </p>

      <div className="mt-5 flex-1 space-y-4 rounded-2xl border border-slate-200 bg-white p-4">
        {messages.map((message) => (
          <div key={message.id} className={message.role === 'user' ? 'text-right' : ''}>
            <div
              className={`inline-block max-w-[85%] whitespace-pre-line rounded-2xl px-4 py-2 text-sm ${
                message.role === 'user' ? 'bg-brand-600 text-white' : 'bg-slate-100 text-slate-800'
              }`}
            >
              {message.content}
            </div>

            {message.citations && message.citations.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-2">
                {message.citations.map((citation) => (
                  <span
                    key={`${citation.sourceType}-${citation.sourceId}-${citation.title}`}
                    className="rounded-full bg-slate-100 px-2.5 py-1 text-xs text-slate-600 ring-1 ring-slate-200"
                    title={`Độ tương đồng ${citation.score}`}
                  >
                    {citation.sourceType}: {citation.title}
                  </span>
                ))}
              </div>
            )}

            {message.role === 'assistant' && message.messageId && (
              <div className="mt-2 flex items-center gap-2 text-xs text-slate-500">
                <span>{message.meta}</span>
                <button onClick={() => vote(message.messageId as number, 1)} className="hover:text-emerald-600">
                  👍 Hữu ích
                </button>
                <button onClick={() => vote(message.messageId as number, -1)} className="hover:text-rose-600">
                  👎
                </button>
              </div>
            )}
          </div>
        ))}
        {loading && <p className="text-sm text-slate-500">Trợ lý đang tìm trong dữ liệu...</p>}
        {error && <p className="text-sm text-rose-600">{error}</p>}
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        {suggestions.map((suggestion) => (
          <button
            key={suggestion}
            onClick={() => send(suggestion)}
            className="rounded-full border border-slate-300 px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-100"
          >
            {suggestion}
          </button>
        ))}
      </div>

      <form
        className="mt-3 flex gap-2"
        onSubmit={(e) => {
          e.preventDefault()
          send(input)
        }}
      >
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Hỏi về tour, vé, chính sách..."
          className="flex-1 rounded-xl border border-slate-300 px-4 py-3 text-sm outline-none focus:border-brand-500"
        />
        <button
          disabled={loading}
          className="rounded-xl bg-brand-600 px-5 py-3 font-semibold text-white hover:bg-brand-700 disabled:opacity-60"
        >
          Gửi
        </button>
      </form>
    </div>
  )
}
