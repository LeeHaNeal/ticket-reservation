import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listEvents } from '../api/events';
import { extractErrorMessage } from '../api/client';
import type { EventResponse } from '../api/types';

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function EventListPage() {
  const [events, setEvents] = useState<EventResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    listEvents(page)
      .then((data) => {
        if (cancelled) return;
        setEvents(data.content);
        setTotalPages(data.totalPages || 1);
        setError(null);
      })
      .catch((err) => {
        if (!cancelled) setError(extractErrorMessage(err, '이벤트 목록을 불러오지 못했습니다.'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [page]);

  return (
    <div className="page">
      <h1>이벤트 목록</h1>
      {error && <p className="form-error">{error}</p>}
      {loading ? (
        <p>불러오는 중...</p>
      ) : events.length === 0 ? (
        <p className="empty-state">등록된 이벤트가 없습니다.</p>
      ) : (
        <div className="event-grid">
          {events.map((event) => {
            const soldOut = event.remainingStock <= 0;
            return (
              <Link to={`/events/${event.id}`} key={event.id} className="card event-card">
                <h2>{event.name}</h2>
                {event.venue && <p className="event-venue">📍 {event.venue}</p>}
                <p className="event-period">
                  {formatDateTime(event.reservationStartAt)} ~ {formatDateTime(event.reservationEndAt)}
                </p>
                <p className={`event-stock ${soldOut ? 'sold-out' : ''}`}>
                  {soldOut ? '매진' : `잔여 ${event.remainingStock} / ${event.totalStock}`}
                </p>
              </Link>
            );
          })}
        </div>
      )}
      {totalPages > 1 && (
        <div className="pagination">
          <button className="btn btn-ghost" disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>
            이전
          </button>
          <span>
            {page + 1} / {totalPages}
          </span>
          <button
            className="btn btn-ghost"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
