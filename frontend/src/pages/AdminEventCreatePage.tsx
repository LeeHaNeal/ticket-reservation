import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { createEvent } from '../api/events';
import { extractErrorMessage } from '../api/client';

export function AdminEventCreatePage() {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [venue, setVenue] = useState('');
  const [totalStock, setTotalStock] = useState(100);
  const [reservationStartAt, setReservationStartAt] = useState('');
  const [reservationEndAt, setReservationEndAt] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const event = await createEvent({
        name,
        description: description || undefined,
        venue: venue || undefined,
        totalStock,
        reservationStartAt,
        reservationEndAt,
      });
      navigate(`/events/${event.id}`);
    } catch (err) {
      setError(extractErrorMessage(err, '이벤트 생성에 실패했습니다.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page">
      <form className="card admin-form" onSubmit={handleSubmit}>
        <h1>이벤트(티켓) 등록</h1>
        {error && <p className="form-error">{error}</p>}
        <label>
          이벤트명
          <input required value={name} onChange={(e) => setName(e.target.value)} />
        </label>
        <label>
          장소
          <input value={venue} onChange={(e) => setVenue(e.target.value)} />
        </label>
        <label>
          설명
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
        </label>
        <label>
          총 재고 수량
          <input
            type="number"
            min={1}
            required
            value={totalStock}
            onChange={(e) => setTotalStock(Number(e.target.value))}
          />
        </label>
        <div className="form-row">
          <label>
            예매 시작
            <input
              type="datetime-local"
              required
              value={reservationStartAt}
              onChange={(e) => setReservationStartAt(e.target.value)}
            />
          </label>
          <label>
            예매 종료
            <input
              type="datetime-local"
              required
              value={reservationEndAt}
              onChange={(e) => setReservationEndAt(e.target.value)}
            />
          </label>
        </div>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? '등록 중...' : '이벤트 등록'}
        </button>
      </form>
    </div>
  );
}
