import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { login } from '../api/auth';
import { extractErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const { accessToken } = await login({ email, password });
      auth.login(accessToken);
      const redirectTo = (location.state as { from?: Location })?.from?.pathname ?? '/events';
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(extractErrorMessage(err, '로그인에 실패했습니다.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={handleSubmit}>
        <h1>로그인</h1>
        {error && <p className="form-error">{error}</p>}
        <label>
          이메일
          <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <label>
          비밀번호
          <input
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? '로그인 중...' : '로그인'}
        </button>
        <p className="auth-switch">
          계정이 없으신가요? <Link to="/signup">회원가입</Link>
        </p>
        <p className="hint">관리자 데모 계정: admin@ticket.com / admin1234!</p>
      </form>
    </div>
  );
}
