import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Brain, ArrowLeft, FileText, Briefcase, Zap, Target, TrendingUp,
  AlertTriangle, Loader2, Settings, ChevronRight,
} from 'lucide-react';
import { interviewApi, jdApi, resumeApi } from '../services/api';
import type { StartInterviewRequest } from '../types';

const INTERVIEW_TYPES = [
  { value: 'COMPREHENSIVE_ADAPTIVE', label: 'Comprehensive Adaptive', desc: 'Full-spectrum questions adapting to your answers', color: '#818cf8' },
  { value: 'TECHNICAL', label: 'Technical Deep Dive', desc: 'Focused technical and coding concepts', color: '#34d399' },
  { value: 'RESUME_BASED', label: 'Resume Based', desc: 'Questions derived from your uploaded resume', color: '#f472b6' },
  { value: 'JD_BASED', label: 'JD Based', desc: 'Targeted to a specific job description', color: '#fbbf24' },
];

const DIFFICULTIES = [
  { value: 'EASY', label: 'Easy', desc: 'Fundamental concepts', color: '#34d399' },
  { value: 'MEDIUM', label: 'Medium', desc: 'Industry standard depth', color: '#fbbf24' },
  { value: 'HARD', label: 'Hard', desc: 'Senior+ level complexity', color: '#f87171' },
];

export const InterviewSetupPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const requestedJdId = Number(searchParams.get('jdId'));
  const prefillJdId = Number.isSafeInteger(requestedJdId) && requestedJdId > 0
    ? requestedJdId
    : undefined;
  const [adaptiveMode, setAdaptiveMode] = useState(searchParams.get('adaptive') === '1');

  const [form, setForm] = useState<StartInterviewRequest>({
    sessionTitle: 'My Interview Session',
    interviewType: 'COMPREHENSIVE_ADAPTIVE',
    targetQuestionCount: 5,
    difficulty: 'MEDIUM',
    jobDescriptionId: prefillJdId,
  });

  const resumesQuery = useQuery({
    queryKey: ['my-resumes'],
    queryFn: resumeApi.getMyResumes,
  });

  const jdsQuery = useQuery({
    queryKey: ['my-jds'],
    queryFn: jdApi.getMyJds,
  });

  const adaptiveProfileQuery = useQuery({
    queryKey: ['adaptive-profile'],
    queryFn: interviewApi.getAdaptiveProfile,
    enabled: adaptiveMode,
  });

  const { mutate, isPending, error } = useMutation({
    mutationFn: () => adaptiveMode
      ? interviewApi.startAdaptiveSession(form)
      : interviewApi.startSession(form),
    onSuccess: (session) => {
      void queryClient.invalidateQueries({ queryKey: ['my-interview-sessions'] });
      navigate(`/interview/${session.sessionId}`);
    },
  });

  const errorMsg = error
    ? ((error as any).response?.data?.message || 'Failed to start session.')
    : null;

  const questionCountOptions = [3, 5, 7, 10];

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
          <div style={{ width: '28px', height: '28px', borderRadius: '7px', background: 'linear-gradient(135deg, #f472b6, #a855f7)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Brain size={14} className="text-white" />
          </div>
          <span className="fw-semibold text-white" style={{ fontSize: '0.9rem' }}>Start Interview</span>
        </div>
      </nav>

      <div className="container py-5" style={{ maxWidth: '780px' }}>
        {/* Header */}
        <div className="text-center mb-5">
          <div className="d-inline-flex p-3 rounded-4 mb-3"
            style={{ background: 'linear-gradient(135deg, rgba(244,114,182,0.15), rgba(168,85,247,0.1))', border: '1px solid rgba(244,114,182,0.2)' }}>
            <Brain size={36} style={{ color: '#f472b6' }} />
          </div>
          <h1 className="h3 fw-bold text-white mb-2">Configure Your Interview</h1>
          <p style={{ color: 'var(--text-muted)', maxWidth: '480px', margin: '0 auto' }}>
            Set up an adaptive mock interview session. Our AI will generate tailored questions
            and evaluate your answers in real time.
          </p>
        </div>

        <section className="glass-panel p-3 mb-4 d-flex flex-wrap align-items-center justify-content-between gap-3"
          aria-label="Interview personalization mode">
          <div>
            <div className="fw-semibold text-white">Personalization</div>
            <div className="small" style={{ color: 'var(--text-muted)' }}>
              Adaptive mode uses saved interview scores, skill gaps, and the current preparation plan.
            </div>
          </div>
          <div className="btn-group" role="group" aria-label="Choose interview mode">
            <button type="button" aria-pressed={!adaptiveMode}
              onClick={() => setAdaptiveMode(false)}
              className={`btn btn-sm ${!adaptiveMode ? 'btn-light' : 'btn-outline-light'}`}>Standard</button>
            <button type="button" aria-pressed={adaptiveMode}
              onClick={() => setAdaptiveMode(true)}
              className={`btn btn-sm ${adaptiveMode ? 'btn-info' : 'btn-outline-info'}`}>Adaptive</button>
          </div>
        </section>

        {adaptiveMode && (
          <section className="glass-panel p-4 mb-4" aria-labelledby="adaptive-preview-heading">
            <div className="d-flex align-items-center gap-2 mb-3">
              <TrendingUp size={17} style={{ color: '#38bdf8' }} />
              <h2 id="adaptive-preview-heading" className="h6 fw-semibold text-white mb-0">Adaptive focus preview</h2>
            </div>
            {adaptiveProfileQuery.isLoading ? (
              <p role="status" className="small mb-0" style={{ color: 'var(--text-muted)' }}>Loading your performance history…</p>
            ) : adaptiveProfileQuery.isError ? (
              <p role="alert" className="small mb-0" style={{ color: '#fca5a5' }}>Your history could not be loaded. The interview can still use the selected role and saved preparation data.</p>
            ) : adaptiveProfileQuery.data?.hasHistory ? (
              <>
                <p className="small mb-3" style={{ color: 'var(--text-muted)' }}>
                  {adaptiveProfileQuery.data.completedInterviewCount} completed interview(s) · suggested starting level {adaptiveProfileQuery.data.suggestedDifficulty.toLowerCase()}
                </p>
                <div className="d-flex flex-wrap gap-2">
                  {adaptiveProfileQuery.data.skillProfile.slice(0, 5).map(skill => (
                    <span key={skill.skillName} className="badge d-inline-flex align-items-center gap-1"
                      style={{ color: skill.level === 'WEAK' ? '#fca5a5' : skill.level === 'MODERATE' ? '#fde68a' : '#6ee7b7', background: 'rgba(255,255,255,0.06)' }}>
                      <Target size={12} />{skill.skillName} · {skill.level.toLowerCase()} · {Math.round(skill.scorePercent)}%
                    </span>
                  ))}
                </div>
              </>
            ) : (
              <p className="small mb-0" style={{ color: 'var(--text-muted)' }}>
                No completed interview scores yet. This first session will establish the baseline; role requirements will still guide its questions.
              </p>
            )}
          </section>
        )}

        {/* Session Title */}
        <div className="glass-panel p-4 mb-4">
          <div className="row g-3">
            <div className="col-md-6">
              <label htmlFor="session-title" className="form-label text-white" style={{ fontSize: '0.875rem', fontWeight: 600 }}>
                Session Name
              </label>
              <input
                id="session-title"
                type="text"
                className="form-control"
                value={form.sessionTitle}
                onChange={e => setForm(f => ({ ...f, sessionTitle: e.target.value }))}
                placeholder="e.g. Spring Boot Engineer Interview"
                style={{ background: 'rgba(255,255,255,0.06)', border: '1px solid var(--border-color)', color: 'white', borderRadius: '10px' }}
              />
            </div>
            <div className="col-md-6">
              <label htmlFor="target-role" className="form-label text-white" style={{ fontSize: '0.875rem', fontWeight: 600 }}>
                Target Role <span className="small" style={{ color: 'var(--text-dim)' }}>(optional)</span>
              </label>
              <input
                id="target-role"
                type="text"
                className="form-control"
                value={form.targetRole ?? ''}
                onChange={e => setForm(f => ({ ...f, targetRole: e.target.value }))}
                placeholder="e.g. Java Backend Developer"
                style={{ background: 'rgba(255,255,255,0.06)', border: '1px solid var(--border-color)', color: 'white', borderRadius: '10px' }}
              />
            </div>
          </div>
        </div>

        {/* Interview Type */}
        <div className="glass-panel p-4 mb-4">
          <div className="d-flex align-items-center gap-2 mb-3">
            <Settings size={16} style={{ color: '#818cf8' }} />
            <span className="fw-semibold text-white">Interview Type</span>
          </div>
          <div className="row g-2">
            {INTERVIEW_TYPES.map(({ value, label, desc, color }) => (
              <div key={value} className="col-md-6">
                <div
                  role="button"
                  id={`type-${value}`}
                  onClick={() => setForm(f => ({ ...f, interviewType: value }))}
                  className="p-3 rounded-3 d-flex align-items-start gap-3"
                  style={{
                    border: `1px solid ${form.interviewType === value ? color : 'var(--border-color)'}`,
                    background: form.interviewType === value ? `${color}12` : 'rgba(255,255,255,0.02)',
                    cursor: 'pointer', transition: 'all 0.15s',
                  }}>
                  <div style={{ width: '10px', height: '10px', borderRadius: '50%', background: color, marginTop: '4px', flexShrink: 0 }} />
                  <div>
                    <div className="fw-semibold text-white" style={{ fontSize: '0.875rem' }}>{label}</div>
                    <div style={{ color: 'var(--text-dim)', fontSize: '0.78rem' }}>{desc}</div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Difficulty + Question Count */}
        <div className="row g-4 mb-4">
          {/* Starting Difficulty */}
          <div className="col-md-7">
            <div className="glass-panel p-4 h-100">
              <div className="d-flex align-items-center gap-2 mb-3">
                <Zap size={16} style={{ color: '#fbbf24' }} />
                <span className="fw-semibold text-white">Starting Difficulty</span>
              </div>
              <div className="d-flex gap-2">
                {DIFFICULTIES.map(({ value, label, desc, color }) => (
                  <div
                    key={value}
                    role="button"
                    id={`diff-${value}`}
                    onClick={() => setForm(f => ({ ...f, difficulty: value }))}
                    className="flex-fill p-2 rounded-3 text-center"
                    style={{
                      border: `1px solid ${form.difficulty === value ? color : 'var(--border-color)'}`,
                      background: form.difficulty === value ? `${color}14` : 'rgba(255,255,255,0.02)',
                      cursor: 'pointer', transition: 'all 0.15s',
                    }}>
                    <div className="fw-semibold" style={{ color, fontSize: '0.85rem' }}>{label}</div>
                    <div style={{ color: 'var(--text-dim)', fontSize: '0.72rem' }}>{desc}</div>
                  </div>
                ))}
              </div>
              <p style={{ color: 'var(--text-dim)', fontSize: '0.78rem', marginTop: '10px', marginBottom: 0 }}>
                {adaptiveMode ? 'Adaptive mode calibrates each topic from your recorded performance.' : 'Difficulty adapts to your answers during this session.'}
              </p>
            </div>
          </div>

          {/* Question Count */}
          <div className="col-md-5">
            <div className="glass-panel p-4 h-100">
              <div className="d-flex align-items-center gap-2 mb-3">
                <Brain size={16} style={{ color: '#f472b6' }} />
                <span className="fw-semibold text-white">Questions</span>
              </div>
              <div className="d-flex gap-2 flex-wrap">
                {questionCountOptions.map(n => (
                  <div
                    key={n}
                    role="button"
                    id={`qcount-${n}`}
                    onClick={() => setForm(f => ({ ...f, targetQuestionCount: n }))}
                    className="px-3 py-2 rounded-3 text-center"
                    style={{
                      border: `1px solid ${form.targetQuestionCount === n ? '#f472b6' : 'var(--border-color)'}`,
                      background: form.targetQuestionCount === n ? 'rgba(244,114,182,0.12)' : 'rgba(255,255,255,0.02)',
                      cursor: 'pointer', transition: 'all 0.15s', minWidth: '52px',
                    }}>
                    <div className="fw-bold text-white" style={{ fontSize: '1.2rem' }}>{n}</div>
                    <div style={{ color: 'var(--text-dim)', fontSize: '0.7rem' }}>Qs</div>
                  </div>
                ))}
              </div>
              <p style={{ color: 'var(--text-dim)', fontSize: '0.78rem', marginTop: '10px', marginBottom: 0 }}>
                ≈{(form.targetQuestionCount ?? 5) * 2}–{(form.targetQuestionCount ?? 5) * 4} mins
              </p>
            </div>
          </div>
        </div>

        {/* Context Links */}
        <div className="glass-panel p-4 mb-4">
          <div className="d-flex align-items-center gap-2 mb-2">
            <FileText size={16} style={{ color: '#34d399' }} />
            <span className="fw-semibold text-white">Interview context</span>
            <span className="small ms-auto" style={{ color: 'var(--text-dim)' }}>Optional</span>
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Choose your saved resume and job description to tailor the session.
          </p>
          <div className="row g-3">
            <div className="col-md-6">
              <label htmlFor="interview-resume" className="form-label text-white">Resume</label>
              <select
                id="interview-resume"
                className="form-select"
                value={form.resumeId ?? ''}
                onChange={e => setForm(f => ({ ...f, resumeId: e.target.value ? Number(e.target.value) : undefined }))}
                style={{ backgroundColor: '#171923', color: 'white', borderColor: 'var(--border-color)' }}>
                <option value="">No resume</option>
                {resumesQuery.data?.map(resume => (
                  <option key={resume.resumeId} value={resume.resumeId}>
                    {resume.originalFileName} · {new Date(resume.uploadedAt).toLocaleDateString()}
                  </option>
                ))}
              </select>
              {resumesQuery.isLoading && <small role="status" style={{ color: 'var(--text-muted)' }}>Loading resumes…</small>}
              {resumesQuery.isError && <small role="alert" className="d-block text-warning">Could not load saved resumes.</small>}
              {!resumesQuery.isLoading && !resumesQuery.isError && !resumesQuery.data?.length && (
                <small className="d-block" style={{ color: 'var(--text-muted)' }}>Upload a resume to use it as context.</small>
              )}
            </div>
            <div className="col-md-6">
              <label htmlFor="interview-jd" className="form-label text-white">Job description</label>
              <select
                id="interview-jd"
                className="form-select"
                value={form.jobDescriptionId ?? ''}
                onChange={e => setForm(f => ({ ...f, jobDescriptionId: e.target.value ? Number(e.target.value) : undefined }))}
                style={{ backgroundColor: '#171923', color: 'white', borderColor: 'var(--border-color)' }}>
                <option value="">No job description</option>
                {jdsQuery.data?.map(jd => (
                  <option key={jd.jdId} value={jd.jdId}>
                    {jd.title}{jd.companyName ? ` · ${jd.companyName}` : ''}
                  </option>
                ))}
              </select>
              {jdsQuery.isLoading && <small role="status" style={{ color: 'var(--text-muted)' }}>Loading job descriptions…</small>}
              {jdsQuery.isError && <small role="alert" className="d-block text-warning">Could not load saved job descriptions.</small>}
              {!jdsQuery.isLoading && !jdsQuery.isError && !jdsQuery.data?.length && (
                <small className="d-block" style={{ color: 'var(--text-muted)' }}>Paste a job description to use it as context.</small>
              )}
            </div>
          </div>
          {prefillJdId && jdsQuery.data?.some(jd => jd.jdId === prefillJdId) && (
            <div className="d-flex align-items-center gap-2 mt-3" role="status"
              style={{ color: '#a5b4fc', fontSize: '0.85rem' }}>
              <Briefcase size={14} /> Job description selected from your saved list.
            </div>
          )}
        </div>

        {errorMsg && (
          <div className="d-flex align-items-center gap-2 p-3 rounded-3 mb-4"
            style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', color: '#fca5a5' }}>
            <AlertTriangle size={16} />
            <span style={{ fontSize: '0.875rem' }}>{errorMsg}</span>
          </div>
        )}

        {/* Start Button */}
        <div className="d-flex justify-content-between align-items-center">
          <p style={{ color: 'var(--text-dim)', fontSize: '0.82rem', marginBottom: 0 }}>
            {form.targetQuestionCount} questions · {form.difficulty?.toLowerCase()} difficulty · {form.interviewType?.replace(/_/g, ' ').toLowerCase()}
          </p>
          <button
            id="start-interview-btn"
            onClick={() => mutate()}
            disabled={!form.sessionTitle.trim() || isPending}
            className="btn px-5 py-2 d-flex align-items-center gap-2"
            style={{
              background: !isPending ? 'linear-gradient(135deg, #f472b6, #a855f7)' : 'rgba(255,255,255,0.06)',
              border: 'none', color: 'white', borderRadius: '12px', fontSize: '0.95rem', fontWeight: 700,
              boxShadow: !isPending ? '0 0 24px rgba(244,114,182,0.35)' : 'none', transition: 'all 0.2s',
            }}>
            {isPending ? <><Loader2 size={16} className="spin-icon" /> Launching…</> : <>{adaptiveMode ? 'Start Adaptive Interview' : 'Launch Interview'} <ChevronRight size={16} /></>}
          </button>
        </div>
      </div>

      <style>{`
        .spin-icon { animation: spin 1s linear infinite; }
        @keyframes spin { to { transform: rotate(360deg); } }
        .form-control:focus { border-color: rgba(129,140,248,0.5) !important; box-shadow: 0 0 0 3px rgba(129,140,248,0.1) !important; outline: none; }
      `}</style>
    </div>
  );
};
