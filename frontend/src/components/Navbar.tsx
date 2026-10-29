import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { API_BASE_URL, healthApi } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { Sparkles, Terminal, BookOpen, Layers, LogIn, LayoutDashboard, LogOut } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { data: health, isSuccess, isLoading } = useQuery({
    queryKey: ['system-health'],
    queryFn: healthApi.checkHealth,
    refetchInterval: 15000,
    retry: 1,
  });

  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className="navbar-custom py-3 px-4 mb-4">
      <div className="container-fluid d-flex flex-wrap align-items-center justify-content-between">
        {/* Brand */}
        <Link to="/" className="d-flex align-items-center gap-3 text-decoration-none">
          <div
            className="d-flex align-items-center justify-content-center"
            style={{
              width: '42px',
              height: '42px',
              borderRadius: '12px',
              background: 'linear-gradient(135deg, #6366f1 0%, #a855f7 100%)',
              boxShadow: '0 0 16px rgba(99, 102, 241, 0.5)',
            }}
          >
            <Sparkles size={22} className="text-white" />
          </div>
          <div>
            <div className="fw-bold fs-5 text-white lh-1">Smart Interview AI</div>
            <small style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>
              Adaptive Real-Time Interview Simulation Platform
            </small>
          </div>
        </Link>

        {/* Links */}
        <div className="d-flex align-items-center gap-4 my-2 my-lg-0">
          <a href="#roadmap" className="d-flex align-items-center gap-2 text-decoration-none"
            style={{ color: 'var(--text-muted)', fontSize: '0.9rem', transition: 'color 0.2s' }}
            onMouseEnter={e => (e.currentTarget.style.color = '#ffffff')}
            onMouseLeave={e => (e.currentTarget.style.color = 'var(--text-muted)')}>
            <Layers size={16} /><span>Phases</span>
          </a>
          <a href="#architecture" className="d-flex align-items-center gap-2 text-decoration-none"
            style={{ color: 'var(--text-muted)', fontSize: '0.9rem', transition: 'color 0.2s' }}
            onMouseEnter={e => (e.currentTarget.style.color = '#ffffff')}
            onMouseLeave={e => (e.currentTarget.style.color = 'var(--text-muted)')}>
            <Terminal size={16} /><span>Architecture</span>
          </a>
          <a href={`${API_BASE_URL}/swagger-ui/index.html`} target="_blank" rel="noopener noreferrer"
            className="d-flex align-items-center gap-2 text-decoration-none"
            style={{ color: 'var(--text-muted)', fontSize: '0.9rem', transition: 'color 0.2s' }}
            onMouseEnter={e => (e.currentTarget.style.color = '#ffffff')}
            onMouseLeave={e => (e.currentTarget.style.color = 'var(--text-muted)')}>
            <BookOpen size={16} /><span>API Docs</span>
          </a>
        </div>

        {/* Right side: backend status + auth buttons */}
        <div className="d-flex align-items-center gap-2">
          {/* Backend status */}
          {isLoading ? (
            <div className="badge-tech" style={{ background: 'rgba(255,255,255,0.05)' }}>
              <span className="spinner-border spinner-border-sm" style={{ width: '12px', height: '12px' }} />
              <span>Checking…</span>
            </div>
          ) : isSuccess && health?.status === 'UP' ? (
            <div className="badge-tech badge-status-up">
              <span className="pulse-indicator" />
              <span>Backend v{health.version}</span>
            </div>
          ) : (
            <div className="badge-tech badge-status-in-progress" title={`Start the backend at ${API_BASE_URL}`}>
              <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#f59e0b' }} />
              <span>Backend Offline</span>
            </div>
          )}

          {/* Auth buttons */}
          {isAuthenticated ? (
            <>
              <Link to="/dashboard" id="navbar-dashboard-link"
                className="btn btn-sm d-flex align-items-center gap-1"
                style={{ background: 'rgba(99,102,241,0.15)', border: '1px solid rgba(99,102,241,0.35)', color: '#a5b4fc', borderRadius: '8px' }}>
                <LayoutDashboard size={14} />
                <span>{user?.fullName?.split(' ')[0]}</span>
              </Link>
              <button onClick={handleLogout} id="navbar-logout" aria-label="Log out" title="Log out"
                className="btn btn-sm d-flex align-items-center gap-1"
                style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.25)', color: '#fca5a5', borderRadius: '8px' }}>
                <LogOut size={14} />
              </button>
            </>
          ) : (
            <Link to="/login" id="navbar-login-link"
              className="btn btn-sm d-flex align-items-center gap-1 fw-semibold"
              style={{ background: 'linear-gradient(135deg, #6366f1, #a855f7)', color: 'white', border: 'none', borderRadius: '8px', padding: '6px 14px' }}>
              <LogIn size={14} /> Sign In
            </Link>
          )}
        </div>
      </div>
    </nav>
  );
};
