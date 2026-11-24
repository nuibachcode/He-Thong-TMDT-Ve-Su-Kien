import { BrowserRouter, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import { AuthProvider } from './features/auth/auth-context'
import Admin from './pages/Admin'
import Chat from './pages/Chat'
import EventDetail from './pages/EventDetail'
import Events from './pages/Events'
import Home from './pages/Home'
import Login from './pages/Login'
import MyTickets from './pages/MyTickets'
import NotFound from './pages/NotFound'
import Organizer from './pages/Organizer'
import Pay from './pages/Pay'
import Register from './pages/Register'
import { TourDetail, Tours } from './pages/Tours'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<Home />} />
            <Route path="events" element={<Events />} />
            <Route path="events/:slug" element={<EventDetail />} />
            <Route path="tours" element={<Tours />} />
            <Route path="tours/:slug" element={<TourDetail />} />
            <Route path="chat" element={<Chat />} />
            <Route path="tickets" element={<MyTickets />} />
            <Route path="pay/:code" element={<Pay />} />
            <Route path="organizer" element={<Organizer />} />
            <Route path="admin" element={<Admin />} />
            <Route path="login" element={<Login />} />
            <Route path="register" element={<Register />} />
            <Route path="*" element={<NotFound />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
