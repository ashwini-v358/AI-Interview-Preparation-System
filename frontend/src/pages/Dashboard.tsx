import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Brain, FileText, Briefcase, LogOut, ChevronRight, Clock,
  BarChart3, ClipboardList, TrendingUp,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { interviewApi, jdApi, resumeApi } from '../services/api';

export const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const resumesQuery = useQuery({
    queryKey: ['my-resumes'],
    queryFn: resumeApi.getMyResumes,
  });
  const jdsQuery = useQuery({
    queryKey: ['my-jds'],
    queryFn: jdApi.getMyJds,
  });
  const sessionsQuery = useQuery({
    queryKey: ['my-interview-sessions'],
    queryFn: interviewApi.getMySessions,
  });
  const adaptiveProfileQuery = useQuery({
    queryKey: ['adaptive-profile'],
    queryFn: interviewApi.getAdaptiveProfile,
  });

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const stats = [
    { label: 'My Resumes', query: resumesQuery, icon: FileText, color: '#34d399' },
    { label: 'My Job Descriptions', query: jdsQuery, icon: Briefcase, color: '#818cf8' },
    { label: 'My Interviews', query: sessionsQuery, icon: Brain, color: '#f472b6' },
    { label: 'Completed Interviews', query: sessionsQuery, icon: Clock, color: '#fbbf24', completed: true },
  ];

  const quickActions = [
    { label: 'Resume', description: 'Upload and review your analyzed resumes', icon: FileText, to: '/resume/upload', color: '#34d399', comingSoon: false },
    { label: 'Job Description', description: 'Paste and analyze a target role', icon: Briefcase, to: '/jd/upload', color: '#818cf8', comingSoon: false },
    { label: 'Skill Gap', description: 'Compare resume evidence with role requirements', icon: BarChart3, to: '/skill-gap', color: '#fbbf24', comingSoon: false },
    { label: 'Preparation Plan', description: 'Turn skill gaps into a focused study roadmap', icon: ClipboardList, to: '/preparation-plan', color: '#38bdf8', comingSoon: false },
    { label: 'Interview', description: 'Start or continue a REST-based interview', icon: Brain, to: '/interview/start', color: '#f472b6', comingSoon: false },
  ];

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-primary)' }}>
      {/* Top Navigation */}
      <nav className="px-4 py-3 d-flex align-items-center justify-content-between"
        style={{ background: 'rgba(15,15,25,0.85)', borderBottom: '1px solid var(--border-color)', backdropFilter: 'blur(12px)', position: 'sticky', top: 0, zIndex: 100 }}>
        <Link to="/" className="d-flex align-items-center gap-2 text-decoration-none">
          <div style={{ width: '32px', height: '32px', borderRadius: '8px', background: 'linear-gradient(135deg, #6366f1, #a855f7)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Brain size={16} className="text-white" />
          </div>
          <span className="fw-bold text-white" style={{ fontSize: '0.95rem' }}>Smart Interview</span>
        </Link>

        <div className="d-flex align-items-center gap-3">
          <div className="d-flex align-items-center gap-2 px-3 py-1 rounded-pill"
            style={{ background: 'rgba(99,102,241,0.1)', border: '1px solid rgba(99,102,241,0.25)' }}>
            <div style={{ width: '28px', height: '28px', borderRadius: '50%', background: 'linear-gradient(135deg, #6366f1, #a855f7)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <span className="text-white fw-bold" style={{ fontSize: '0.75rem' }}>
                {user?.fullName?.charAt(0)?.toUpperCase() ?? 'U'}
              </span>
            </div>
            <span className="text-white" style={{ fontSize: '0.875rem' }}>{user?.fullName}</span>
            <span className="badge" style={{ background: 'rgba(99,102,241,0.3)', color: '#a5b4fc', fontSize: '0.7rem' }}>
              {user?.role === 'ROLE_ADMIN' ? 'Admin' : 'Candidate'}
            </span>
          </div>
          <button onClick={handleLogout} id="dashboard-logout"
            className="btn btn-sm d-flex align-items-center gap-1"
            style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.25)', color: '#fca5a5', borderRadius: '8px' }}>
            <LogOut size={14} /> Logout
          </button>
        </div>
      </nav>

      <div className="container py-5">
        {/* Welcome Banner */}
        <div className="glass-panel p-4 mb-4 d-flex flex-wrap align-items-center justify-content-between gap-3">
          <div>
            <h1 className="h4 fw-bold text-white mb-1">
              Welcome back, {user?.fullName?.split(' ')[0]} 👋
            </h1>
            <p className="mb-0" style={{ color: 'var(--text-muted)' }}>
              Your resumes, target roles, and interview practice in one place.
            </p>
          </div>
          <div className="d-flex align-items-center gap-2">
            <span className="badge-tech badge-status-up">Candidate workspace</span>
          </div>
        </div>

        <section className="glass-panel p-4 mb-4" aria-labelledby="interview-evolution-dashboard-heading">
          <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
            <div>
              <div className="d-flex align-items-center gap-2 mb-2">
                <TrendingUp size={17} style={{ color: '#38bdf8' }} />
                <h2 id="interview-evolution-dashboard-heading" className="h6 fw-semibold text-white mb-0">Interview Evolution</h2>
              </div>
              {adaptiveProfileQuery.isLoading ? (
                <span role="status" className="small" style={{ color: 'var(--text-muted)' }}>Loading performance history…</span>
              ) : adaptiveProfileQuery.isError ? (
                <span role="alert" className="small" style={{ color: '#fca5a5' }}>Performance history is unavailable.</span>
              ) : adaptiveProfileQuery.data?.hasHistory ? (() => {
                const [latest, previous] = adaptiveProfileQuery.data.history;
                const delta = previous ? latest.overallScorePercent - previous.overallScorePercent : null;
                const focusAreas = adaptiveProfileQuery.data.skillProfile.filter(skill => skill.level !== 'STRONG').slice(0, 3);
                return <div className="d-flex flex-wrap align-items-center gap-4">
                  <div>
                    <div className="fw-bold text-white">{Math.round(latest.overallScorePercent)}%</div>
                    <div className="small" style={{ color: 'var(--text-muted)' }}>Last interview</div>
                  </div>
                  {previous && <>
                    <div>
                      <div className="fw-bold text-white">{Math.round(previous.overallScorePercent)}%</div>
                      <div className="small" style={{ color: 'var(--text-muted)' }}>Previous</div>
                    </div>
                    <div>
                      <div className="fw-bold" style={{ color: delta !== null && delta >= 0 ? '#34d399' : '#f87171' }}>
                        {delta !== null && delta > 0 ? '+' : ''}{Math.round(delta ?? 0)}%
                      </div>
                      <div className="small" style={{ color: 'var(--text-muted)' }}>Change</div>
                    </div>
                  </>}
                  <div className="flex-grow-1">
                    <div className="small mb-1" style={{ color: 'var(--text-muted)' }}>Current focus</div>
                    <div className="d-flex flex-wrap gap-2">
                      {focusAreas.length ? focusAreas.map(skill => <span key={skill.skillName} className="badge" style={{ color: skill.level === 'WEAK' ? '#fca5a5' : '#fde68a', background: 'rgba(255,255,255,0.06)' }}>{skill.skillName}</span>) : <span className="small text-white">No weak topics</span>}
                    </div>
                  </div>
                </div>;
              })() : <span className="small" style={{ color: 'var(--text-muted)' }}>Complete an interview to establish your performance baseline.</span>}
            </div>
            <Link to="/interview/start?adaptive=1" className="btn btn-sm d-flex align-items-center gap-2"
              style={{ color: '#7dd3fc', border: '1px solid rgba(56,189,248,0.3)' }}>
              <Brain size={15} /> Start Adaptive Interview
            </Link>
          </div>
        </section>

        {/* Stats Row */}
        <div className="row g-3 mb-4">
          {stats.map(({ label, query, icon: Icon, color, completed }) => (
            <div key={label} className="col-6 col-lg-3">
              <div className="glass-panel p-3 h-100">
                <div className="d-flex align-items-center justify-content-between mb-2">
                  <Icon size={20} style={{ color }} />
                  <span className="fw-bold text-white" style={{ fontSize: '1.5rem' }}>
                    {query.isLoading ? '…' : query.isError ? '—' : completed
                      ? query.data?.filter(session => session.status === 'COMPLETED').length ?? 0
                      : query.data?.length ?? 0}
                  </span>
                </div>
                <div style={{ color: 'var(--text-muted)', fontSize: '0.82rem' }}>{label}</div>
              </div>
            </div>
          ))}
        </div>

        {/* Quick Actions */}
        <div className="mb-4">
          <h2 className="h5 fw-bold text-white mb-3">Quick Actions</h2>
          <div className="row g-3">
            {quickActions.map(({ label, description, icon: Icon, color, comingSoon, to }) => (
              <div key={label} className="col-md-6 col-lg-4">
                {comingSoon ? (
                  <div className="glass-panel p-4 h-100 d-flex flex-column" style={{ position: 'relative', opacity: 0.75 }}>
                    <span className="badge position-absolute top-0 end-0 mt-2 me-2"
                      style={{ background: 'rgba(245,158,11,0.2)', color: '#fbbf24', border: '1px solid rgba(245,158,11,0.3)', fontSize: '0.65rem' }}>
                      Phase 5+
                    </span>
                    <div className="d-inline-flex p-2 rounded-3 mb-3" style={{ background: `${color}18`, width: 'fit-content' }}>
                      <Icon size={22} style={{ color }} />
                    </div>
                    <div className="fw-semibold text-white mb-1">{label}</div>
                    <p className="small flex-grow-1" style={{ color: 'var(--text-muted)', fontSize: '0.82rem' }}>{description}</p>
                    <div className="d-flex align-items-center gap-1 mt-2" style={{ color: 'var(--text-dim)', fontSize: '0.8rem' }}>
                      <span>Coming soon</span>
                    </div>
                  </div>
                ) : (
                  <Link to={to} className="text-decoration-none">
                    <div className="glass-panel p-4 h-100 d-flex flex-column"
                      style={{ cursor: 'pointer', transition: 'border-color 0.2s, transform 0.2s', borderColor: `${color}40` }}
                      onMouseEnter={e => { (e.currentTarget as HTMLElement).style.transform = 'translateY(-2px)'; }}
                      onMouseLeave={e => { (e.currentTarget as HTMLElement).style.transform = 'translateY(0)'; }}>
                      <span className="badge position-absolute top-0 end-0 mt-2 me-2"
                        style={{ background: 'rgba(52,211,153,0.2)', color: '#34d399', border: '1px solid rgba(52,211,153,0.3)', fontSize: '0.65rem' }}>
                        Available
                      </span>
                      <div className="d-inline-flex p-2 rounded-3 mb-3" style={{ background: `${color}18`, width: 'fit-content' }}>
                        <Icon size={22} style={{ color }} />
                      </div>
                      <div className="fw-semibold text-white mb-1">{label}</div>
                      <p className="small flex-grow-1" style={{ color: 'var(--text-muted)', fontSize: '0.82rem' }}>{description}</p>
                      <div className="d-flex align-items-center gap-1 mt-2" style={{ color, fontSize: '0.8rem' }}>
                        <span>Open</span> <ChevronRight size={14} />
                      </div>
                    </div>
                  </Link>
                )}
              </div>
            ))}
          </div>
        </div>

        <section aria-labelledby="recent-work-heading" className="mb-4">
          <h2 id="recent-work-heading" className="h5 fw-bold text-white mb-3">Your preparation activity</h2>
          <div className="row g-3">
            <div className="col-lg-4">
              <div className="glass-panel p-4 h-100">
                <div className="d-flex align-items-center justify-content-between mb-3">
                  <h3 className="h6 fw-semibold text-white mb-0">Resumes</h3>
                  <Link to="/resume/upload" className="small text-decoration-none" style={{ color: '#6ee7b7' }}>Manage</Link>
                </div>
                {resumesQuery.isLoading ? <p role="status" style={{ color: 'var(--text-muted)' }}>Loading resumes…</p>
                  : resumesQuery.isError ? <p role="alert" style={{ color: '#fca5a5' }}>Could not load resumes.</p>
                    : resumesQuery.data?.length ? resumesQuery.data.slice(0, 3).map(resume => (
                      <div key={resume.resumeId} className="py-2" style={{ borderTop: '1px solid var(--border-color)' }}>
                        <div className="text-white text-truncate">{resume.originalFileName}</div>
                        <small style={{ color: 'var(--text-muted)' }}>{resume.topSkills.slice(0, 3).join(' · ') || 'Analysis available'}</small>
                      </div>
                    )) : <p className="mb-0" style={{ color: 'var(--text-muted)' }}>No resumes yet. Upload one to get started.</p>}
              </div>
            </div>
            <div className="col-lg-4">
              <div className="glass-panel p-4 h-100">
                <div className="d-flex align-items-center justify-content-between mb-3">
                  <h3 className="h6 fw-semibold text-white mb-0">Job descriptions</h3>
                  <Link to="/jd/upload" className="small text-decoration-none" style={{ color: '#a5b4fc' }}>Manage</Link>
                </div>
                {jdsQuery.isLoading ? <p role="status" style={{ color: 'var(--text-muted)' }}>Loading job descriptions…</p>
                  : jdsQuery.isError ? <p role="alert" style={{ color: '#fca5a5' }}>Could not load job descriptions.</p>
                    : jdsQuery.data?.length ? jdsQuery.data.slice(0, 3).map(jd => (
                      <div key={jd.jdId} className="py-2" style={{ borderTop: '1px solid var(--border-color)' }}>
                        <div className="d-flex justify-content-between gap-2">
                          <span className="text-white text-truncate">{jd.title}</span>
                          <Link to={`/interview/start?jdId=${jd.jdId}`} aria-label={`Start interview for ${jd.title}`}>
                            <ChevronRight size={16} />
                          </Link>
                        </div>
                        <small style={{ color: 'var(--text-muted)' }}>{jd.roleTitle || jd.companyName || 'Saved role'}</small>
                      </div>
                    )) : <p className="mb-0" style={{ color: 'var(--text-muted)' }}>No job descriptions yet. Paste one to target a role.</p>}
              </div>
            </div>
            <div className="col-lg-4">
              <div className="glass-panel p-4 h-100">
                <div className="d-flex align-items-center justify-content-between mb-3">
                  <h3 className="h6 fw-semibold text-white mb-0">Interview sessions</h3>
                  <Link to="/interview/start" className="small text-decoration-none" style={{ color: '#f9a8d4' }}>Start</Link>
                </div>
                {sessionsQuery.isLoading ? <p role="status" style={{ color: 'var(--text-muted)' }}>Loading sessions…</p>
                  : sessionsQuery.isError ? <p role="alert" style={{ color: '#fca5a5' }}>Could not load interviews.</p>
                    : sessionsQuery.data?.length ? sessionsQuery.data.slice(0, 3).map(session => (
                      <Link key={session.sessionId} to={`/interview/${session.sessionId}`}
                        className="d-flex justify-content-between align-items-center gap-2 py-2 text-decoration-none"
                        style={{ borderTop: '1px solid var(--border-color)' }}>
                        <span className="text-white text-truncate">{session.sessionTitle}</span>
                        <span className="badge-tech flex-shrink-0">{session.completedQuestionCount}/{session.targetQuestionCount}</span>
                      </Link>
                    )) : <p className="mb-0" style={{ color: 'var(--text-muted)' }}>No interview sessions yet. Start your first practice.</p>}
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
};
