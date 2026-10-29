import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Brain, Briefcase, ArrowLeft, CheckCircle2, Zap,
  AlertTriangle, Loader2, Star, ChevronRight,
} from 'lucide-react';
import { jdApi } from '../services/api';
import type { JdUploadResponse } from '../types';

const EXAMPLE_JD = `Senior Software Engineer – Backend (Java/Spring Boot)
Company: TechCorp Inc.

We are seeking an experienced Backend Engineer to join our platform team.

Requirements:
- 4+ years of professional software development experience
- Strong proficiency in Java (17+) and Spring Boot framework
- Experience with REST API design and microservices architecture
- Solid understanding of relational databases (MySQL, PostgreSQL) and ORM (Hibernate/JPA)
- Experience with Docker and Kubernetes for containerised deployments
- Familiarity with cloud platforms (AWS or GCP)
- Proficiency with Git and CI/CD pipelines

Nice to Have:
- Experience with Kafka or RabbitMQ for event-driven systems
- Knowledge of Redis for caching
- Exposure to GraphQL APIs
- Contributions to open-source projects

Responsibilities:
- Design and implement scalable, high-availability backend services
- Collaborate with frontend and data engineering teams
- Write unit and integration tests with high coverage
- Participate in code reviews and technical discussions
- Mentor junior engineers`;

export const JdUploadPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [title, setTitle] = useState('');
  const [company, setCompany] = useState('');
  const [rawText, setRawText] = useState('');
  const [result, setResult] = useState<JdUploadResponse | null>(null);

  const jdsQuery = useQuery({
    queryKey: ['my-jds'],
    queryFn: jdApi.getMyJds,
  });

  const { mutate, isPending, error } = useMutation({
    mutationFn: () => jdApi.upload({ title, companyName: company, rawText }),
    onSuccess: (data) => {
      setResult(data);
      void queryClient.invalidateQueries({ queryKey: ['my-jds'] });
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || rawText.trim().length < 50) return;
    mutate();
  };

  const errorMsg = error
    ? ((error as any).response?.data?.message || 'Upload failed. Please try again.')
    : null;

  const charCount = rawText.length;
  const isValid = title.trim().length > 0 && charCount >= 50;


  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-primary)' }}>
      {/* Nav */}
      <nav className="px-4 py-3 d-flex align-items-center gap-3"
        style={{ background: 'rgba(15,15,25,0.9)', borderBottom: '1px solid var(--border-color)', backdropFilter: 'blur(12px)', position: 'sticky', top: 0, zIndex: 100 }}>
        <Link to="/dashboard" className="d-flex align-items-center gap-2 text-decoration-none"
          style={{ color: 'var(--text-muted)' }}>
          <ArrowLeft size={16} />
          <span style={{ fontSize: '0.875rem' }}>Dashboard</span>
        </Link>
        <span style={{ color: 'var(--border-color)' }}>|</span>
        <div className="d-flex align-items-center gap-2">
          <div style={{ width: '28px', height: '28px', borderRadius: '7px', background: 'linear-gradient(135deg, #818cf8, #a855f7)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Briefcase size={14} className="text-white" />
          </div>
          <span className="fw-semibold text-white" style={{ fontSize: '0.9rem' }}>Job Description Upload</span>
        </div>
      </nav>

      <div className="container py-5" style={{ maxWidth: '900px' }}>
        {!result ? (
          <>
            {/* Header */}
            <div className="text-center mb-5">
              <div className="d-inline-flex p-3 rounded-4 mb-3"
                style={{ background: 'linear-gradient(135deg, rgba(129,140,248,0.15), rgba(168,85,247,0.1))', border: '1px solid rgba(129,140,248,0.2)' }}>
                <Briefcase size={36} style={{ color: '#818cf8' }} />
              </div>
              <h1 className="h3 fw-bold text-white mb-2">Analyse Job Description</h1>
              <p style={{ color: 'var(--text-muted)', maxWidth: '520px', margin: '0 auto' }}>
                Paste a job description and our AI will extract required skills, experience levels,
                and key responsibilities to tailor your interview preparation.
              </p>
            </div>

            {/* Form */}
            <form onSubmit={handleSubmit}>
              <div className="glass-panel p-4 mb-4">
                <div className="row g-3 mb-4">
                  <div className="col-md-7">
                    <label className="form-label text-white" style={{ fontSize: '0.875rem' }}>
                      Job Title <span style={{ color: '#f87171' }}>*</span>
                    </label>
                    <input
                      id="jd-title"
                      type="text"
                      className="form-control"
                      placeholder="e.g. Senior Backend Engineer"
                      value={title}
                      onChange={e => setTitle(e.target.value)}
                      style={{ background: 'rgba(255,255,255,0.06)', border: '1px solid var(--border-color)', color: 'white', borderRadius: '10px' }}
                    />
                  </div>
                  <div className="col-md-5">
                    <label className="form-label text-white" style={{ fontSize: '0.875rem' }}>
                      Company Name <span style={{ color: 'var(--text-dim)' }}>(optional)</span>
                    </label>
                    <input
                      id="jd-company"
                      type="text"
                      className="form-control"
                      placeholder="e.g. TechCorp Inc."
                      value={company}
                      onChange={e => setCompany(e.target.value)}
                      style={{ background: 'rgba(255,255,255,0.06)', border: '1px solid var(--border-color)', color: 'white', borderRadius: '10px' }}
                    />
                  </div>
                </div>

                <div>
                  <div className="d-flex align-items-center justify-content-between mb-2">
                    <label className="form-label text-white mb-0" style={{ fontSize: '0.875rem' }}>
                      Job Description Text <span style={{ color: '#f87171' }}>*</span>
                    </label>
                    <span style={{ color: charCount < 50 ? '#f87171' : 'var(--text-dim)', fontSize: '0.78rem' }}>
                      {charCount} chars {charCount < 50 && `(min 50)`}
                    </span>
                  </div>
                  <textarea
                    id="jd-text"
                    rows={16}
                    className="form-control"
                    placeholder="Paste the full job description here..."
                    value={rawText}
                    onChange={e => setRawText(e.target.value)}
                    style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid var(--border-color)', color: 'white', borderRadius: '10px', fontFamily: 'monospace', fontSize: '0.85rem', resize: 'vertical' }}
                  />
                  <button
                    type="button"
                    onClick={() => setRawText(EXAMPLE_JD)}
                    className="btn btn-sm mt-2"
                    style={{ background: 'rgba(129,140,248,0.1)', border: '1px solid rgba(129,140,248,0.25)', color: '#a5b4fc', fontSize: '0.78rem', borderRadius: '6px' }}>
                    Load Example JD
                  </button>
                </div>
              </div>

              {errorMsg && (
                <div className="d-flex align-items-center gap-2 p-3 rounded-3 mb-4"
                  style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', color: '#fca5a5' }}>
                  <AlertTriangle size={16} />
                  <span style={{ fontSize: '0.875rem' }}>{errorMsg}</span>
                </div>
              )}

              <div className="d-flex justify-content-between align-items-center">
                <p style={{ color: 'var(--text-dim)', fontSize: '0.82rem', marginBottom: 0 }}>
                  AI analysis typically takes 1–3 seconds with the mock provider.
                </p>
                <button
                  id="jd-submit"
                  type="submit"
                  disabled={!isValid || isPending}
                  className="btn px-4 py-2 d-flex align-items-center gap-2"
                  style={{
                    background: isValid && !isPending ? 'linear-gradient(135deg, #818cf8, #a855f7)' : 'rgba(255,255,255,0.06)',
                    border: 'none', color: 'white', borderRadius: '10px', fontSize: '0.9rem', fontWeight: 600, transition: 'all 0.2s',
                  }}>
                  {isPending ? <><Loader2 size={16} className="spin-icon" /> Analysing…</> : <><Zap size={16} /> Analyse with AI</>}
                </button>
              </div>
            </form>
          </>
        ) : (
          /* ── Results Panel ── */
          <div className="animate-fade-in">
            <div className="d-flex align-items-center gap-3 mb-4">
              <div style={{ width: '42px', height: '42px', borderRadius: '12px', background: 'linear-gradient(135deg, #34d399, #059669)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <CheckCircle2 size={22} className="text-white" />
              </div>
              <div>
                <h1 className="h4 fw-bold text-white mb-0">Analysis Complete</h1>
                <p className="mb-0" style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                  Processed in {result.processingTimeMs}ms · {result.aiProvider} AI
                </p>
              </div>
            </div>

            {/* Summary Card */}
            <div className="glass-panel p-4 mb-4">
              <div className="row g-3 mb-3">
                <div className="col-md-6">
                  <div style={{ color: 'var(--text-dim)', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Role Title</div>
                  <div className="fw-semibold text-white" style={{ fontSize: '1.1rem' }}>{result.roleTitle || result.title}</div>
                  {result.companyName && <div style={{ color: '#818cf8', fontSize: '0.85rem' }}>{result.companyName}</div>}
                </div>
                <div className="col-md-3">
                  <div style={{ color: 'var(--text-dim)', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Min Experience</div>
                  <div className="fw-bold text-white" style={{ fontSize: '1.4rem' }}>{result.minimumExperienceYears}<span style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>yr{result.minimumExperienceYears !== 1 ? 's' : ''}</span></div>
                </div>
                <div className="col-md-3">
                  <div style={{ color: 'var(--text-dim)', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Required Skills</div>
                  <div className="fw-bold text-white" style={{ fontSize: '1.4rem' }}>{result.requiredSkills.length}</div>
                </div>
              </div>
            </div>

            {/* Required Skills */}
            <div className="glass-panel p-4 mb-4">
              <div className="d-flex align-items-center gap-2 mb-3">
                <Star size={16} style={{ color: '#f87171' }} />
                <span className="fw-semibold text-white">Required Skills</span>
                <span className="badge" style={{ background: 'rgba(248,113,113,0.15)', color: '#fca5a5', border: '1px solid rgba(248,113,113,0.3)', fontSize: '0.7rem' }}>Must Have</span>
              </div>
              <div className="d-flex flex-wrap gap-2">
                {result.requiredSkills.map(skill => (
                  <span key={skill} className="badge px-3 py-1" style={{ background: 'rgba(248,113,113,0.12)', border: '1px solid rgba(248,113,113,0.3)', color: '#fca5a5', borderRadius: '20px', fontSize: '0.82rem' }}>
                    {skill}
                  </span>
                ))}
              </div>
            </div>

            {/* Nice-to-Have Skills */}
            {result.niceToHaveSkills.length > 0 && (
              <div className="glass-panel p-4 mb-4">
                <div className="d-flex align-items-center gap-2 mb-3">
                  <Zap size={16} style={{ color: '#fbbf24' }} />
                  <span className="fw-semibold text-white">Nice-to-Have Skills</span>
                  <span className="badge" style={{ background: 'rgba(251,191,36,0.15)', color: '#fde68a', border: '1px solid rgba(251,191,36,0.3)', fontSize: '0.7rem' }}>Preferred</span>
                </div>
                <div className="d-flex flex-wrap gap-2">
                  {result.niceToHaveSkills.map(skill => (
                    <span key={skill} className="badge px-3 py-1" style={{ background: 'rgba(251,191,36,0.1)', border: '1px solid rgba(251,191,36,0.25)', color: '#fde68a', borderRadius: '20px', fontSize: '0.82rem' }}>
                      {skill}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* CTA Row */}
            <div className="d-flex flex-wrap gap-3">
              <button
                onClick={() => navigate(`/interview/start?jdId=${result.jdId}`)}
                className="btn d-flex align-items-center gap-2 px-4 py-2"
                style={{ background: 'linear-gradient(135deg, #f472b6, #a855f7)', border: 'none', color: 'white', borderRadius: '10px', fontWeight: 600 }}>
                <Brain size={16} /> Start Interview with this JD <ChevronRight size={14} />
              </button>
              <button
                onClick={() => { setResult(null); setTitle(''); setCompany(''); setRawText(''); }}
                className="btn d-flex align-items-center gap-2 px-4 py-2"
                style={{ background: 'rgba(129,140,248,0.1)', border: '1px solid rgba(129,140,248,0.3)', color: '#a5b4fc', borderRadius: '10px' }}>
                <Briefcase size={16} /> Upload Another JD
              </button>
              <Link to="/dashboard" className="btn d-flex align-items-center gap-2 px-4 py-2"
                style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid var(--border-color)', color: 'var(--text-muted)', borderRadius: '10px', textDecoration: 'none' }}>
                ← Dashboard
              </Link>
            </div>
          </div>
        )}

        <section className="mt-5" aria-labelledby="jd-list-heading">
          <div className="d-flex align-items-center justify-content-between mb-3">
            <div>
              <h2 id="jd-list-heading" className="h5 fw-bold text-white mb-1">Your job descriptions</h2>
              <p className="mb-0" style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                Saved job descriptions from your account
              </p>
            </div>
            <span className="badge-tech">{jdsQuery.data?.length ?? 0} saved</span>
          </div>
          {jdsQuery.isLoading ? (
            <div className="glass-panel p-4 text-center" role="status">
              <span className="spinner-border spinner-border-sm text-primary" />
              <span className="ms-2" style={{ color: 'var(--text-muted)' }}>Loading job descriptions…</span>
            </div>
          ) : jdsQuery.isError ? (
            <div className="glass-panel p-4" role="alert" style={{ color: '#fca5a5' }}>
              Could not load your saved job descriptions. Refresh the page to try again.
            </div>
          ) : jdsQuery.data?.length ? (
            <div className="glass-panel p-3">
              {jdsQuery.data.map((jd) => (
                <div key={jd.jdId} className="d-flex flex-wrap align-items-center justify-content-between gap-3 py-3"
                  style={{ borderBottom: '1px solid var(--border-color)' }}>
                  <div>
                    <div className="fw-semibold text-white">{jd.title}</div>
                    <small style={{ color: 'var(--text-muted)' }}>
                      {[jd.roleTitle, jd.companyName, new Date(jd.uploadedAt).toLocaleDateString()]
                        .filter(Boolean).join(' · ')}
                    </small>
                    <div className="d-flex flex-wrap gap-2 mt-2">
                      {jd.topRequiredSkills.map((skill) => <span key={skill} className="badge-tech">{skill}</span>)}
                    </div>
                  </div>
                  <Link to={`/interview/start?jdId=${jd.jdId}`} className="btn btn-sm btn-outline-light">
                    Prepare interview <ChevronRight size={14} />
                  </Link>
                </div>
              ))}
            </div>
          ) : (
            <div className="glass-panel p-4 text-center">
              <Briefcase size={24} className="mb-2" style={{ color: 'var(--text-dim)' }} />
              <p className="mb-0" style={{ color: 'var(--text-muted)' }}>No job descriptions saved yet.</p>
            </div>
          )}
        </section>
      </div>

      <style>{`
        .spin-icon { animation: spin 1s linear infinite; }
        @keyframes spin { to { transform: rotate(360deg); } }
        .animate-fade-in { animation: fadeIn 0.4s ease; }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
        .form-control:focus { border-color: rgba(129,140,248,0.5) !important; box-shadow: 0 0 0 3px rgba(129,140,248,0.1) !important; outline: none; }
      `}</style>
    </div>
  );
};
