import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { cancelReservation, getMyReservations } from '../api/reservations';
import { extractErrorMessage } from '../api/client';
import type { ReservationResponse } from '../api/types';

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function MyReservationsPage() {
  const [reservations, setReservations] = useState<ReservationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancellingId, setCancellingId] = useState<number | null>(null);

  function load() {
    setLoading(true);
    getMyReservations()
      .then(setReservations)
      .catch((err) => setError(extractErrorMessage(err, '예매 내역을 불러오지 못했습니다.')))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function handleCancel(reservationId: number) {
    setCancellingId(reservationId);
    try {
      await cancelReservation(reservationId);
      load();
    } catch (err) {
      setError(extractErrorMessage(err, '취소에 실패했습니다.'));
    } finally {
      setCancellingId(null);
    }
  }

  return (
    <div className="page">
      <h1>내 예매 내역</h1>
      {error && <p className="form-error">{error}</p>}
      {loading ? (
        <p>불러오는 중...</p>
      ) : reservations.length === 0 ? (
        <p className="empty-state">
          예매 내역이 없습니다. <Link to="/events">이벤트 둘러보기</Link>
        </p>
      ) : (
        <ul className="reservation-list">
          {reservations.map((r) => (
            <li key={r.reservationId} className="card reservation-item">
              <div>
                <Link to={`/events/${r.eventId}`} className="reservation-event-name">
                  {r.eventName}
                </Link>
                <p className="reservation-meta">
                  예매일시: {formatDateTime(r.reservedAt)} · 상태:{' '}
                  <span className={`status-badge status-${r.status.toLowerCase()}`}>
                    {r.status === 'CONFIRMED' ? '예매 확정' : '취소됨'}
                  </span>
                </p>
              </div>
              {r.status === 'CONFIRMED' && (
                <button
                  className="btn btn-ghost"
                  disabled={cancellingId === r.reservationId}
                  onClick={() => handleCancel(r.reservationId)}
                >
                  {cancellingId === r.reservationId ? '취소 중...' : '예매 취소'}
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
