import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getEvent, getStock } from '../api/events';
import { reserve } from '../api/reservations';
import { extractErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import type { EventResponse } from '../api/types';

const STOCK_POLL_INTERVAL_MS = 3000;

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function EventDetailPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const id = Number(eventId);
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [event, setEvent] = useState<EventResponse | null>(null);
  const [remainingStock, setRemainingStock] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reserving, setReserving] = useState(false);
  const [reserveResult, setReserveResult] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getEvent(id)
      .then((data) => {
        if (!cancelled) {
          setEvent(data);
          setRemainingStock(data.remainingStock);
        }
      })
      .catch((err) => {
        if (!cancelled) setError(extractErrorMessage(err, '이벤트 정보를 불러오지 못했습니다.'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  const pollStock = useCallback(() => {
    getStock(id)
      .then((data) => setRemainingStock(data.remainingStock))
      .catch(() => {
        /* 잔여 수량 폴링 실패는 조용히 무시 - 다음 폴링에서 다시 시도 */
      });
  }, [id]);

  useEffect(() => {
    if (!event) return;
    const timer = setInterval(pollStock, STOCK_POLL_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [event, pollStock]);

  async function handleReserve() {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: { pathname: `/events/${id}` } } });
      return;
    }
    setReserving(true);
    setReserveResult(null);
    try {
      await reserve(id);
      setReserveResult('success');
      pollStock();
    } catch (err) {
      setReserveResult(extractErrorMessage(err, '예매에 실패했습니다.'));
    } finally {
      setReserving(false);
    }
  }

  if (loading) return <div className="page">불러오는 중...</div>;
  if (error || !event) return <div className="page form-error">{error ?? '이벤트를 찾을 수 없습니다.'}</div>;

  const soldOut = (remainingStock ?? event.remainingStock) <= 0;
  const now = new Date();
  const notOpenYet = now < new Date(event.reservationStartAt);
  const closed = now > new Date(event.reservationEndAt);

  return (
    <div className="page">
      <div className="card event-detail-card">
        <h1>{event.name}</h1>
        {event.venue && <p className="event-venue">📍 {event.venue}</p>}
        <p className="event-period">
          예매 기간: {formatDateTime(event.reservationStartAt)} ~ {formatDateTime(event.reservationEndAt)}
        </p>
        {event.description && <p className="event-description">{event.description}</p>}

        <div className="stock-display">
          <span className="stock-label">실시간 잔여 수량</span>
          <span className={`stock-number ${soldOut ? 'sold-out' : ''}`}>
            {remainingStock ?? event.remainingStock} / {event.totalStock}
          </span>
        </div>

        {reserveResult === 'success' && (
          <p className="form-success">예매가 완료되었습니다! "내 예매"에서 확인하세요.</p>
        )}
        {reserveResult && reserveResult !== 'success' && <p className="form-error">{reserveResult}</p>}

        <button
          className="btn btn-primary btn-large"
          disabled={soldOut || notOpenYet || closed || reserving}
          onClick={handleReserve}
        >
          {reserving
            ? '예매 처리 중...'
            : soldOut
              ? '매진되었습니다'
              : notOpenYet
                ? '예매 오픈 전'
                : closed
                  ? '예매 마감'
                  : '선착순 예매하기'}
        </button>
      </div>
    </div>
  );
}
