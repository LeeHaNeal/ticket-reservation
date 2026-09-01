import { Navigate, Route, Routes } from 'react-router-dom';
import { Navbar } from './components/Navbar';
import { ProtectedRoute, AdminRoute } from './components/ProtectedRoute';
import { LoginPage } from './pages/LoginPage';
import { SignupPage } from './pages/SignupPage';
import { EventListPage } from './pages/EventListPage';
import { EventDetailPage } from './pages/EventDetailPage';
import { MyReservationsPage } from './pages/MyReservationsPage';
import { AdminEventCreatePage } from './pages/AdminEventCreatePage';

export default function App() {
  return (
    <>
      <Navbar />
      <main className="app-main">
        <Routes>
          <Route path="/" element={<Navigate to="/events" replace />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/events" element={<EventListPage />} />
          <Route path="/events/:eventId" element={<EventDetailPage />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/my-reservations" element={<MyReservationsPage />} />
          </Route>

          <Route element={<AdminRoute />}>
            <Route path="/admin/events/new" element={<AdminEventCreatePage />} />
          </Route>

          <Route path="*" element={<Navigate to="/events" replace />} />
        </Routes>
      </main>
    </>
  );
}
