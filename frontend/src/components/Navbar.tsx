import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function Navbar() {
  const { isAuthenticated, email, role, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <Link to="/events" className="brand">
          🎫 선착순 티켓 예매
        </Link>
        <nav className="nav-links">
          <Link to="/events">이벤트 목록</Link>
          {isAuthenticated && <Link to="/my-reservations">내 예매</Link>}
          {role === 'ADMIN' && <Link to="/admin/events/new">이벤트 등록</Link>}
        </nav>
        <div className="nav-auth">
          {isAuthenticated ? (
            <>
              <span className="user-email">{email}</span>
              <button className="btn btn-ghost" onClick={handleLogout}>
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost">
                로그인
              </Link>
              <Link to="/signup" className="btn btn-primary">
                회원가입
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
