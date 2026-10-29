import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Brain, ArrowLeft, CheckCircle2, Clock, AlertTriangle,
  Loader2, Send, ChevronDown, ChevronUp, Trophy, Zap, BarChart3, Mic, MicOff, Volume2,
} from 'lucide-react';
import { interviewApi } from '../services/api';
import type { QuestionAnswerDto } from '../types';
import {
  BrowserSpeechTranscriptionService,
  type SpeechTranscriptionService,
  type TranscriptionErrorCode,
  type TranscriptionState,
} from '../services/speech/SpeechTranscriptionService';

const TRANSCRIPTION_ERRORS: Record<TranscriptionErrorCode, string> = {
  unsupported: 'Speech recognition is unavailable in this browser. Type your answer instead.',
  'microphone-denied': 'Microphone permission was denied. Allow microphone access or type your answer.',
  'microphone-unavailable': 'No usable microphone was found. Check your microphone or type your answer.',
  'no-speech': 'No speech was detected. Try recording again or type your answer.',
  'transcription-failed': 'Speech transcription failed. Try again or type your answer.',
  'recording-failed': 'Recording could not start. Check microphone access or type your answer.',
};

const difficultyColor = (d: string) =>
  d === 'EASY' ? '#34d399' : d === 'HARD' ? '#f87171' : '#fbbf24';

const ScoreBar: React.FC<{ label: string; score?: number; color?: string }> = ({ label, score, color = '#818cf8' }) => (
  <div className="mb-2">
    <div className="d-flex justify-content-between mb-1">
      <span style={{ color: 'var(--text-muted)', fontSize: '0.78rem' }}>{label}</span>
      <span className="fw-semibold" style={{ color, fontSize: '0.8rem' }}>{score?.toFixed(1) ?? '—'}/10</span>
    </div>
    <div style={{ height: '4px', background: 'rgba(255,255,255,0.08)', borderRadius: '4px', overflow: 'hidden' }}>
      <div style={{ height: '100%', width: `${((score ?? 0) / 10) * 100}%`, background: `linear-gradient(90deg, ${color}88, ${color})`, borderRadius: '4px', transition: 'width 1s ease' }} />
    </div>
  </div>
);

const HistoryItem: React.FC<{ qa: QuestionAnswerDto }> = ({ qa }) => {
  const [expanded, setExpanded] = useState(false);
  const strengths = qa.strengthsList?.length ? qa.strengthsList : qa.strengths ? [qa.strengths] : [];
  const missingPoints = qa.missingPoints?.length ? qa.missingPoints
    : qa.areasOfImprovement ? [qa.areasOfImprovement] : [];
  return (
    <div className="glass-panel p-3 mb-3" style={{ borderLeft: `3px solid ${difficultyColor(qa.difficultyLevel)}` }}>
      <div className="d-flex align-items-start justify-content-between gap-2 mb-2">
        <div>
          <span className="badge me-2" style={{ background: `${difficultyColor(qa.difficultyLevel)}20`, color: difficultyColor(qa.difficultyLevel), border: `1px solid ${difficultyColor(qa.difficultyLevel)}40`, fontSize: '0.7rem' }}>
            Q{qa.questionOrder} · {qa.difficultyLevel}
          </span>
          {qa.overallScore !== undefined && (
            <span className="badge" style={{ background: qa.overallScore >= 7 ? 'rgba(52,211,153,0.15)' : qa.overallScore >= 5 ? 'rgba(251,191,36,0.15)' : 'rgba(248,113,113,0.15)', color: qa.overallScore >= 7 ? '#34d399' : qa.overallScore >= 5 ? '#fbbf24' : '#f87171', border: '1px solid transparent', fontSize: '0.7rem' }}>
              Score: {qa.overallScore?.toFixed(1)}/10
            </span>
          )}
        </div>
        <button
          onClick={() => setExpanded(!expanded)}
          className="btn btn-sm p-0 border-0"
          style={{ background: 'none', color: 'var(--text-dim)' }}>
          {expanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
        </button>
      </div>
      <p className="text-white mb-2" style={{ fontSize: '0.875rem', lineHeight: 1.6 }}>{qa.questionText}</p>
      {(qa.feedback || strengths[0] || missingPoints[0]) && (
        <div className="p-2 rounded-2 mb-2" style={{ background: 'rgba(255,255,255,0.035)' }}>
          {qa.feedback && <p className="mb-1" style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>{qa.feedback}</p>}
          {strengths[0] && <div className="small" style={{ color: '#6ee7b7' }}>Strength: {strengths[0]}</div>}
          {missingPoints[0] && <div className="small" style={{ color: '#fcd34d' }}>Focus: {missingPoints[0]}</div>}
        </div>
      )}
      {expanded && (
        <div style={{ animation: 'fadeIn 0.2s ease' }}>
          {!!qa.selectionReasons?.length && (
            <div className="small mb-3" style={{ color: 'var(--text-muted)' }}>
              <div className="fw-semibold text-white mb-1">Why this question?</div>
              <ul className="mb-0 ps-3">{qa.selectionReasons.map(reason => <li key={`${reason.category}-${reason.description}`}>{reason.description}</li>)}</ul>
            </div>
          )}
          <div className="p-3 rounded-3 mb-3" style={{ background: 'rgba(129,140,248,0.06)', border: '1px solid rgba(129,140,248,0.15)' }}>
            <div style={{ color: '#818cf8', fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '6px' }}>Your Answer</div>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', lineHeight: 1.7, marginBottom: 0 }}>{qa.answerText}</p>
          </div>
          {qa.overallScore !== undefined && (
            <>
              <div className="row g-2 mb-3">
                <div className="col-6 col-lg-4"><ScoreBar label="Relevance" score={qa.relevanceScore} color="#38bdf8" /></div>
                <div className="col-6 col-lg-4"><ScoreBar label="Accuracy" score={qa.technicalAccuracyScore} color="#818cf8" /></div>
                <div className="col-6 col-lg-4"><ScoreBar label="Completeness" score={qa.completenessScore} color="#34d399" /></div>
                <div className="col-6 col-lg-4"><ScoreBar label="Clarity" score={qa.clarityScore} color="#f472b6" /></div>
                <div className="col-6 col-lg-4"><ScoreBar label="Communication" score={qa.communicationScore} color="#fbbf24" /></div>
                <div className="col-6 col-lg-4"><ScoreBar label="Depth" score={qa.depthScore} color="#c084fc" /></div>
              </div>
              {strengths.length > 0 && (
                <div className="p-2 rounded-2 mb-2" style={{ background: 'rgba(52,211,153,0.06)', border: '1px solid rgba(52,211,153,0.15)' }}>
                  <div style={{ color: '#34d399', fontSize: '0.72rem', textTransform: 'uppercase', marginBottom: '4px' }}>Strengths</div>
                  <ul className="mb-0 ps-3" style={{ color: 'var(--text-muted)', fontSize: '0.82rem' }}>
                    {strengths.map((item, index) => <li key={`${index}-${item}`}>{item}</li>)}
                  </ul>
                </div>
              )}
              {missingPoints.length > 0 && (
                <div className="p-2 rounded-2" style={{ background: 'rgba(251,191,36,0.06)', border: '1px solid rgba(251,191,36,0.15)' }}>
                  <div style={{ color: '#fbbf24', fontSize: '0.72rem', textTransform: 'uppercase', marginBottom: '4px' }}>Missing points</div>
                  <ul className="mb-0 ps-3" style={{ color: 'var(--text-muted)', fontSize: '0.82rem' }}>
                    {missingPoints.map((item, index) => <li key={`${index}-${item}`}>{item}</li>)}
                  </ul>
                </div>
              )}
              {qa.suggestedImprovement && (
                <p className="small mt-2 mb-0" style={{ color: 'var(--text-muted)' }}>
                  Suggested improvement: {qa.suggestedImprovement}
                </p>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
};

export const InterviewSessionPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const sessionId = parseInt(id!);
  const [answer, setAnswer] = useState('');
  const [elapsed, setElapsed] = useState(0);
  const [transcriptionState, setTranscriptionState] = useState<TranscriptionState>('idle');
  const [voiceError, setVoiceError] = useState<string | null>(null);
  const [transcriptionService] = useState<SpeechTranscriptionService>(() => new BrowserSpeechTranscriptionService());
  const answerBeforeRecording = useRef('');
  const isSpeechSupported = transcriptionService.isSupported();
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const { data: session, isLoading, error } = useQuery({
    queryKey: ['interview-session', sessionId],
    queryFn: () => interviewApi.getSession(sessionId),
    enabled: !!sessionId,
    refetchOnWindowFocus: false,
  });

  const reportKey = ['interview-report', sessionId];
  const reportQuery = useQuery({
    queryKey: reportKey,
    queryFn: async () => {
      try {
        return await interviewApi.getReport(sessionId);
      } catch (requestError) {
        const status = (requestError as { response?: { status?: number } }).response?.status;
        if (status !== 404) throw requestError;
        return interviewApi.generateReport(sessionId);
      }
    },
    enabled: session?.status === 'COMPLETED',
    retry: false,
  });
  const evolutionQuery = useQuery({
    queryKey: ['adaptive-profile'],
    queryFn: interviewApi.getAdaptiveProfile,
    enabled: session?.status === 'COMPLETED',
    refetchOnWindowFocus: false,
  });

  const { mutate: submitAnswer, isPending: submitting, error: submitError } = useMutation({
    mutationFn: (answerText: string) =>
      interviewApi.submitAnswer(sessionId, session!.currentQuestion!.questionId, {
        answerText,
        timeTakenSeconds: elapsed,
      }),
    onSuccess: (updated) => {
      queryClient.setQueryData(['interview-session', sessionId], updated);
      void queryClient.invalidateQueries({ queryKey: ['interview-session', sessionId] });
      if (updated.status === 'COMPLETED') {
        void queryClient.invalidateQueries({ queryKey: ['my-interview-sessions'] });
        void queryClient.invalidateQueries({ queryKey: ['adaptive-profile'] });
      }
      setAnswer('');
      setElapsed(0);
      textareaRef.current?.focus();
    },
  });

  const submitErrorMessage = submitError
    ? (submitError as { response?: { data?: { message?: string } } }).response?.data?.message
      || 'Your answer could not be submitted. Please try again.'
    : null;

  const startRecording = () => {
    setVoiceError(null);
    answerBeforeRecording.current = answer.trim();
    transcriptionService.start({
      onTranscript: (transcript) => {
        setAnswer([answerBeforeRecording.current, transcript].filter(Boolean).join(' '));
      },
      onStateChange: setTranscriptionState,
      onComplete: (transcript) => {
        setTranscriptionState('idle');
        if (!transcript) setVoiceError(TRANSCRIPTION_ERRORS['no-speech']);
      },
      onError: (code) => {
        setTranscriptionState('idle');
        setVoiceError(TRANSCRIPTION_ERRORS[code]);
      },
    });
  };

  const stopRecording = () => transcriptionService.stop();

  const readQuestionAloud = () => {
    if (!('speechSynthesis' in window)) {
      setVoiceError(TRANSCRIPTION_ERRORS.unsupported);
      return;
    }
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(new SpeechSynthesisUtterance(session?.currentQuestion?.questionText ?? ''));
  };

  useEffect(() => () => transcriptionService.abort(), [transcriptionService]);

  // Timer
  useEffect(() => {
    if (session?.status === 'IN_PROGRESS' && session.currentQuestion) {
      timerRef.current = setInterval(() => setElapsed(e => e + 1), 1000);
    }
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, [session?.currentQuestion?.questionId, session?.status]);

  const formatTime = (s: number) => `${Math.floor(s / 60).toString().padStart(2, '0')}:${(s % 60).toString().padStart(2, '0')}`;

  const isCompleted = session?.status === 'COMPLETED';
  const progress = session ? (session.completedQuestionCount / session.targetQuestionCount) * 100 : 0;
  const avgScore = session?.history?.length
    ? session.history.reduce((sum, qa) => sum + (qa.overallScore ?? 0), 0) / session.history.length
    : 0;

  if (isLoading) return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div className="text-center">
        <Loader2 size={40} style={{ color: '#f472b6', animation: 'spin 1s linear infinite' }} />
        <p className="mt-3" style={{ color: 'var(--text-muted)' }}>Loading session…</p>
      </div>
    </div>
  );

  if (error || !session) return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div className="text-center glass-panel p-5">
        <AlertTriangle size={40} style={{ color: '#f87171' }} className="mb-3" />
        <h2 className="text-white mb-2">Session not found</h2>
        <Link to="/dashboard" className="btn" style={{ background: 'rgba(129,140,248,0.15)', color: '#a5b4fc', border: '1px solid rgba(129,140,248,0.3)', textDecoration: 'none', borderRadius: '8px' }}>
          Back to Dashboard
        </Link>
      </div>
    </div>
  );

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-primary)' }}>
      {/* Nav */}
      <nav className="px-4 py-3 d-flex align-items-center justify-content-between"
        style={{ background: 'rgba(15,15,25,0.9)', borderBottom: '1px solid var(--border-color)', backdropFilter: 'blur(12px)', position: 'sticky', top: 0, zIndex: 100 }}>
        <div className="d-flex align-items-center gap-3">
          <Link to="/dashboard" className="d-flex align-items-center gap-2 text-decoration-none"
            style={{ color: 'var(--text-muted)' }}>
            <ArrowLeft size={16} />
            <span style={{ fontSize: '0.875rem' }}>Dashboard</span>
          </Link>
          <span style={{ color: 'var(--border-color)' }}>|</span>
          <span className="fw-semibold text-white" style={{ fontSize: '0.9rem' }}>{session.sessionTitle}</span>
          {session.adaptiveMode && <span className="badge" style={{ color: '#7dd3fc', background: 'rgba(56,189,248,0.12)' }}>Adaptive</span>}
        </div>
        <div className="d-flex align-items-center gap-3">
          {session.status === 'IN_PROGRESS' && session.currentQuestion && (
            <div className="d-flex align-items-center gap-2 px-3 py-1 rounded-pill"
              style={{ background: 'rgba(244,114,182,0.1)', border: '1px solid rgba(244,114,182,0.25)' }}>
              <Clock size={12} style={{ color: '#f472b6' }} />
              <span style={{ color: '#f9a8d4', fontSize: '0.82rem', fontFamily: 'monospace' }}>{formatTime(elapsed)}</span>
            </div>
          )}
          <span className={`badge-tech ${isCompleted ? 'badge-status-up' : 'badge-status-down'}`}>
            {isCompleted ? '✓ Completed' : `Q${session.completedQuestionCount + 1}/${session.targetQuestionCount}`}
          </span>
        </div>
      </nav>

      {/* Progress Bar */}
      <div style={{ height: '3px', background: 'rgba(255,255,255,0.05)' }}>
        <div style={{ height: '100%', width: `${progress}%`, background: 'linear-gradient(90deg, #f472b6, #a855f7)', transition: 'width 0.6s ease' }} />
      </div>

      <div className="container py-4" style={{ maxWidth: '820px' }}>
        {/* ── COMPLETED STATE ─────────────────────────────────────────────── */}
        {isCompleted ? (
          <div style={{ animation: 'fadeIn 0.5s ease' }}>
            <div className="text-center mb-5">
              <div className="d-inline-flex p-3 rounded-4 mb-3"
                style={{ background: 'linear-gradient(135deg, rgba(251,191,36,0.2), rgba(245,158,11,0.1))', border: '1px solid rgba(251,191,36,0.3)' }}>
                <Trophy size={40} style={{ color: '#fbbf24' }} />
              </div>
              <h1 className="h3 fw-bold text-white mb-2">Interview Complete! 🎉</h1>
              <p style={{ color: 'var(--text-muted)' }}>{session.targetQuestionCount} questions answered</p>
            </div>

            {/* Score Summary */}
            <div className="glass-panel p-4 mb-4">
              <div className="d-flex align-items-center gap-2 mb-4">
                <BarChart3 size={18} style={{ color: '#fbbf24' }} />
                <span className="fw-semibold text-white">Performance Summary</span>
              </div>
              <div className="row g-3 mb-4">
                {[
                  { label: 'Overall Score', val: avgScore, color: '#818cf8' },
                  { label: 'Questions Done', val: session.completedQuestionCount, color: '#34d399', isCount: true },
                  { label: 'Avg Accuracy', val: session.history.reduce((s, q) => s + (q.technicalAccuracyScore ?? 0), 0) / (session.history.length || 1), color: '#f472b6' },
                ].map(({ label, val, color, isCount }) => (
                  <div key={label} className="col-4 text-center">
                    <div className="fw-bold" style={{ fontSize: '2rem', color }}>{isCount ? val : val.toFixed(1)}{!isCount && <span style={{ fontSize: '1rem' }}>/10</span>}</div>
                    <div style={{ color: 'var(--text-dim)', fontSize: '0.78rem' }}>{label}</div>
                  </div>
                ))}
              </div>
              <div>
                <ScoreBar label="Technical Accuracy" score={session.history.reduce((s, q) => s + (q.technicalAccuracyScore ?? 0), 0) / (session.history.length || 1)} color="#818cf8" />
                <ScoreBar label="Completeness" score={session.history.reduce((s, q) => s + (q.completenessScore ?? 0), 0) / (session.history.length || 1)} color="#34d399" />
                <ScoreBar label="Clarity" score={session.history.reduce((s, q) => s + (q.clarityScore ?? 0), 0) / (session.history.length || 1)} color="#f472b6" />
              </div>
            </div>

            <section className="glass-panel p-4 mb-4" aria-labelledby="interview-evolution-heading">
              <div className="d-flex align-items-center gap-2 mb-3">
                <BarChart3 size={18} style={{ color: '#38bdf8' }} />
                <h2 id="interview-evolution-heading" className="h5 fw-semibold text-white mb-0">Interview evolution</h2>
              </div>
              {evolutionQuery.isLoading ? (
                <p role="status" className="small mb-0" style={{ color: 'var(--text-muted)' }}>Updating your performance history…</p>
              ) : evolutionQuery.isError ? (
                <p role="alert" className="small mb-0" style={{ color: '#fca5a5' }}>Your interview is saved, but performance history could not be loaded.</p>
              ) : (() => {
                const history = evolutionQuery.data?.history ?? [];
                const current = history.find(item => item.sessionId === session.sessionId);
                const previous = history.find(item => item.sessionId !== session.sessionId);
                if (!current) return <p className="small mb-0" style={{ color: 'var(--text-muted)' }}>No scored interview topics are available yet.</p>;
                const delta = previous ? current.overallScorePercent - previous.overallScorePercent : null;
                return <>
                  <div className="row g-3 mb-3">
                    <div className="col-6 col-md-4">
                      <div className="fw-bold text-white" style={{ fontSize: '1.7rem' }}>{Math.round(current.overallScorePercent)}%</div>
                      <div className="small" style={{ color: 'var(--text-muted)' }}>This interview</div>
                    </div>
                    {previous && <>
                      <div className="col-6 col-md-4">
                        <div className="fw-bold text-white" style={{ fontSize: '1.7rem' }}>{Math.round(previous.overallScorePercent)}%</div>
                        <div className="small" style={{ color: 'var(--text-muted)' }}>Previous interview</div>
                      </div>
                      <div className="col-6 col-md-4">
                        <div className="fw-bold" style={{ color: delta !== null && delta >= 0 ? '#34d399' : '#f87171', fontSize: '1.7rem' }}>
                          {delta !== null && delta > 0 ? '+' : ''}{Math.round(delta ?? 0)}%
                        </div>
                        <div className="small" style={{ color: 'var(--text-muted)' }}>Change</div>
                      </div>
                    </>}
                  </div>
                  <div className="d-flex flex-column gap-2">
                    {current.skillScores.map(skill => {
                      const previousSkill = previous?.skillScores.find(item => item.skillName.toLowerCase() === skill.skillName.toLowerCase());
                      const skillDelta = previousSkill ? skill.scorePercent - previousSkill.scorePercent : null;
                      return <div key={skill.skillName} className="d-flex flex-wrap justify-content-between gap-2 py-2" style={{ borderTop: '1px solid var(--border-color)' }}>
                        <span className="text-white">{skill.skillName}</span>
                        <span className="small" style={{ color: skill.level === 'WEAK' ? '#fca5a5' : skill.level === 'MODERATE' ? '#fde68a' : '#6ee7b7' }}>
                          {Math.round(skill.scorePercent)}% · {skill.level.toLowerCase()}
                          {skillDelta !== null && <span style={{ color: skillDelta >= 0 ? '#34d399' : '#f87171' }}> · {skillDelta > 0 ? '+' : ''}{Math.round(skillDelta)}%</span>}
                        </span>
                      </div>;
                    })}
                  </div>
                  <p className="small mt-3 mb-0" style={{ color: 'var(--text-muted)' }}>
                    Your next adaptive interview will use these scores to prioritize topics and calibrate difficulty.
                  </p>
                </>;
              })()}
            </section>

            <section className="glass-panel p-4 mb-4" aria-labelledby="interview-report-heading">
              <div className="d-flex align-items-center justify-content-between gap-3 mb-3">
                <div>
                  <h2 id="interview-report-heading" className="h5 fw-bold text-white mb-1">Final performance report</h2>
                  <p className="small mb-0" style={{ color: 'var(--text-muted)' }}>
                    Based on your answers in this session
                  </p>
                </div>
                {reportQuery.isLoading && (
                  <span role="status" className="d-flex align-items-center gap-2 small" style={{ color: 'var(--text-muted)' }}>
                    <Loader2 size={15} className="spin-icon" /> Generating report…
                  </span>
                )}
              </div>
              {reportQuery.data ? (
                <>
                  <div className="d-flex flex-wrap align-items-center gap-3 mb-3">
                    <div className="fw-bold text-white" style={{ fontSize: '2rem' }}>
                      {Math.round(reportQuery.data.overallScore)}<span className="fs-6" style={{ color: 'var(--text-muted)' }}>/100</span>
                    </div>
                    <p className="mb-0 flex-grow-1" style={{ color: 'var(--text-muted)' }}>{reportQuery.data.executiveSummary}</p>
                  </div>
                  <div className="row g-2 mb-3">
                    {[
                      ['Technical', reportQuery.data.technicalScore],
                      ['Communication', reportQuery.data.communicationScore],
                      ['Problem solving', reportQuery.data.problemSolvingScore],
                      ['Project knowledge', reportQuery.data.projectKnowledgeScore],
                      ['Answer quality', reportQuery.data.answerQualityScore],
                    ].map(([label, score]) => (
                      <div key={label} className="col-6 col-md">
                        <div className="p-2 rounded-2 text-center" style={{ background: 'rgba(255,255,255,0.035)' }}>
                          <div className="fw-semibold text-white">{Math.round(Number(score))}</div>
                          <small style={{ color: 'var(--text-muted)' }}>{label}</small>
                        </div>
                      </div>
                    ))}
                  </div>
                  <div className="row g-3">
                    <div className="col-md-6">
                      <h3 className="small text-uppercase fw-semibold" style={{ color: '#6ee7b7' }}>Strengths</h3>
                      {reportQuery.data.strengths.length
                        ? <ul className="small mb-0" style={{ color: 'var(--text-muted)' }}>{reportQuery.data.strengths.map(item => <li key={item}>{item}</li>)}</ul>
                        : <p className="small mb-0" style={{ color: 'var(--text-muted)' }}>No strengths were reported.</p>}
                    </div>
                    <div className="col-md-6">
                      <h3 className="small text-uppercase fw-semibold" style={{ color: '#fcd34d' }}>Focus areas</h3>
                      {reportQuery.data.areasToImprove.length
                        ? <ul className="small mb-0" style={{ color: 'var(--text-muted)' }}>{reportQuery.data.areasToImprove.map(item => <li key={item}>{item}</li>)}</ul>
                        : <p className="small mb-0" style={{ color: 'var(--text-muted)' }}>No specific gaps were reported.</p>}
                      {!!reportQuery.data.strugglingQuestionOrders.length && (
                        <p className="small mt-2 mb-0" style={{ color: 'var(--text-muted)' }}>
                          Questions to revisit: {reportQuery.data.strugglingQuestionOrders.map(order => `Q${order}`).join(', ')}
                        </p>
                      )}
                    </div>
                    <div className="col-12">
                      <h3 className="small text-uppercase fw-semibold" style={{ color: '#a5b4fc' }}>Recommended preparation</h3>
                      <div className="d-flex flex-wrap gap-2">
                        {reportQuery.data.recommendedTopics.map(topic => <span key={topic} className="badge-tech">{topic}</span>)}
                      </div>
                      {reportQuery.data.improvementSuggestions.map(suggestion => (
                        <p key={suggestion} className="small mt-2 mb-0" style={{ color: 'var(--text-muted)' }}>{suggestion}</p>
                      ))}
                    </div>
                  </div>
                </>
              ) : reportQuery.isError ? (
                <div role="alert" className="d-flex flex-wrap align-items-center justify-content-between gap-2">
                  <span className="small" style={{ color: '#fca5a5' }}>
                    The interview is complete, but report generation failed. You can retry without losing your results.
                  </span>
                  <button type="button" onClick={() => reportQuery.refetch()} disabled={reportQuery.isFetching}
                    className="btn btn-sm btn-outline-light">Retry report</button>
                </div>
              ) : null}
            </section>

            {/* Q&A Review */}
            <h2 className="h5 fw-bold text-white mb-3">Question Review</h2>
            {session.history.map(qa => <HistoryItem key={qa.questionId} qa={qa} />)}

            <div className="d-flex flex-wrap gap-3 mt-4">
              <Link to="/preparation-plan" className="btn d-flex align-items-center gap-2 px-4 py-2"
                style={{ background: 'rgba(56,189,248,0.1)', border: '1px solid rgba(56,189,248,0.3)', color: '#7dd3fc', textDecoration: 'none', borderRadius: '10px' }}>
                <BarChart3 size={16} /> View Preparation Plan
              </Link>
              <Link to="/interview/start?adaptive=1" className="btn d-flex align-items-center gap-2 px-4 py-2"
                style={{ background: 'rgba(52,211,153,0.1)', border: '1px solid rgba(52,211,153,0.3)', color: '#6ee7b7', textDecoration: 'none', borderRadius: '10px' }}>
                <Brain size={16} /> Start Adaptive Interview
              </Link>
              <button
                onClick={() => navigate('/interview/start')}
                className="btn d-flex align-items-center gap-2 px-4 py-2"
                style={{ background: 'linear-gradient(135deg, #f472b6, #a855f7)', border: 'none', color: 'white', borderRadius: '10px', fontWeight: 600 }}>
                <Brain size={16} /> New Interview
              </button>
              <Link to="/dashboard" className="btn d-flex align-items-center gap-2 px-4 py-2"
                style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid var(--border-color)', color: 'var(--text-muted)', textDecoration: 'none', borderRadius: '10px' }}>
                ← Dashboard
              </Link>
            </div>
          </div>
        ) : (
          /* ── ACTIVE INTERVIEW STATE ────────────────────────────────────── */
          <div className="row g-4">
            {/* Main: Current Question */}
            <div className="col-lg-8">
              {session.currentQuestion && (
                <div style={{ animation: 'fadeIn 0.3s ease' }}>
                  {/* Question Card */}
                  <div className="glass-panel p-4 mb-4" style={{ borderLeft: `3px solid ${difficultyColor(session.currentQuestion.difficultyLevel)}` }}>
                    <div className="d-flex align-items-center gap-2 mb-3">
                      <span className="badge" style={{ background: `${difficultyColor(session.currentQuestion.difficultyLevel)}20`, color: difficultyColor(session.currentQuestion.difficultyLevel), border: `1px solid ${difficultyColor(session.currentQuestion.difficultyLevel)}40`, fontSize: '0.75rem' }}>
                        {session.currentQuestion.difficultyLevel}
                      </span>
                      <span style={{ color: 'var(--text-dim)', fontSize: '0.78rem' }}>
                        {session.currentQuestion.category} · {session.currentQuestion.targetSkill}
                      </span>
                      <button type="button" onClick={readQuestionAloud} aria-label="Read question aloud" title="Read question aloud"
                        className="btn btn-sm ms-auto" style={{ color: 'var(--text-muted)', border: '1px solid var(--border-color)' }}>
                        <Volume2 size={15} />
                      </button>
                    </div>
                    <p className="text-white mb-0" style={{ fontSize: '1rem', lineHeight: 1.75, fontWeight: 400 }}>
                      {session.currentQuestion.questionText}
                    </p>
                    {!!session.currentQuestion.selectionReasons?.length && (
                      <details className="mt-3 pt-3" style={{ borderTop: '1px solid var(--border-color)' }}>
                        <summary className="small fw-semibold" style={{ color: '#7dd3fc', cursor: 'pointer' }}>Why this question?</summary>
                        <ul className="small mt-2 mb-0 ps-3" style={{ color: 'var(--text-muted)' }}>
                          {session.currentQuestion.selectionReasons.map(reason => <li key={`${reason.category}-${reason.description}`} className="mb-1">{reason.description}</li>)}
                        </ul>
                      </details>
                    )}
                  </div>

                  {/* Answer Textarea */}
                  <div className="glass-panel p-4">
                    {submitErrorMessage && (
                      <div role="alert" className="d-flex align-items-center gap-2 p-3 rounded-3 mb-3"
                        style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', color: '#fca5a5' }}>
                        <AlertTriangle size={16} />
                        <span style={{ fontSize: '0.875rem' }}>{submitErrorMessage}</span>
                      </div>
                    )}
                    <label className="form-label text-white mb-2" style={{ fontSize: '0.875rem', fontWeight: 600 }}>
                      Your Answer
                    </label>
                    <div className="d-flex flex-wrap align-items-center gap-2 mb-3">
                      <button type="button" onClick={startRecording}
                        disabled={!isSpeechSupported || transcriptionState !== 'idle' || submitting}
                        className="btn btn-sm d-flex align-items-center gap-2"
                        style={{ background: 'rgba(244,114,182,0.12)', color: '#f9a8d4', border: '1px solid rgba(244,114,182,0.3)' }}>
                        <Mic size={15} /> Start Recording
                      </button>
                      <button type="button" onClick={stopRecording}
                        disabled={transcriptionState !== 'recording' || submitting}
                        className="btn btn-sm d-flex align-items-center gap-2"
                        style={{ background: 'rgba(248,113,113,0.12)', color: '#fca5a5', border: '1px solid rgba(248,113,113,0.3)' }}>
                        <MicOff size={15} /> Stop Recording
                      </button>
                      <span role="status" aria-live="polite" className="small" style={{ color: transcriptionState === 'recording' ? '#f9a8d4' : 'var(--text-muted)' }}>
                        {transcriptionState === 'recording' ? 'Recording…'
                          : transcriptionState === 'transcribing' ? 'Transcribing…'
                            : isSpeechSupported ? 'Type an answer or record one.' : 'Type your answer; speech recognition is not supported here.'}
                      </span>
                    </div>
                    {voiceError && (
                      <div role="alert" className="d-flex align-items-center gap-2 p-3 rounded-3 mb-3"
                        style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', color: '#fca5a5' }}>
                        <AlertTriangle size={16} />
                        <span style={{ fontSize: '0.85rem' }}>{voiceError}</span>
                      </div>
                    )}
                    <textarea
                      id="answer-textarea"
                      ref={textareaRef}
                      rows={7}
                      className="form-control mb-3"
                      placeholder="Type your answer here. Be thorough — explain your reasoning, give examples, and mention trade-offs where relevant..."
                      value={answer}
                      onChange={e => { setAnswer(e.target.value); setVoiceError(null); }}
                      disabled={submitting || transcriptionState === 'transcribing'}
                      style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid var(--border-color)', color: 'white', borderRadius: '10px', fontSize: '0.9rem', resize: 'vertical', lineHeight: 1.7 }}
                    />
                    <div className="d-flex align-items-center justify-content-between">
                      <span style={{ color: 'var(--text-dim)', fontSize: '0.8rem' }}>
                        {answer.split(/\s+/).filter(Boolean).length} words
                      </span>
                      <button
                        id="submit-answer-btn"
                        onClick={() => answer.trim() && submitAnswer(answer.trim())}
                        disabled={!answer.trim() || submitting || transcriptionState !== 'idle'}
                        className="btn px-4 py-2 d-flex align-items-center gap-2"
                        style={{
                          background: answer.trim() && !submitting ? 'linear-gradient(135deg, #f472b6, #a855f7)' : 'rgba(255,255,255,0.06)',
                          border: 'none', color: 'white', borderRadius: '10px', fontWeight: 600, transition: 'all 0.2s',
                          boxShadow: answer.trim() && !submitting ? '0 0 20px rgba(244,114,182,0.3)' : 'none',
                        }}>
                        {submitting ? <><Loader2 size={16} className="spin-icon" /> Evaluating…</> : <><Send size={15} /> Submit Answer</>}
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* Sidebar: Stats + History */}
            <div className="col-lg-4">
              {/* Session Stats */}
              <div className="glass-panel p-3 mb-3">
                <div className="d-flex align-items-center gap-2 mb-3">
                  <Zap size={14} style={{ color: '#fbbf24' }} />
                  <span className="fw-semibold text-white" style={{ fontSize: '0.875rem' }}>Session Stats</span>
                </div>
                <div className="d-flex flex-column gap-2">
                  {[
                    { k: 'Type', v: session.interviewType.replace(/_/g, ' ') },
                    { k: 'Difficulty', v: session.currentDifficulty },
                    { k: 'Progress', v: `${session.completedQuestionCount}/${session.targetQuestionCount}` },
                    { k: 'Avg Score', v: session.history.length ? `${avgScore.toFixed(1)}/10` : '—' },
                  ].map(({ k, v }) => (
                    <div key={k} className="d-flex justify-content-between">
                      <span style={{ color: 'var(--text-dim)', fontSize: '0.78rem' }}>{k}</span>
                      <code style={{ color: 'var(--text-muted)', fontSize: '0.78rem' }}>{v}</code>
                    </div>
                  ))}
                </div>
              </div>

              {/* Previous Q&A */}
              {session.history.length > 0 && (
                <div>
                  <div className="d-flex align-items-center gap-2 mb-2">
                    <CheckCircle2 size={14} style={{ color: '#34d399' }} />
                    <span className="fw-semibold text-white" style={{ fontSize: '0.875rem' }}>
                      Answered ({session.history.length})
                    </span>
                  </div>
                  {[...session.history].reverse().map(qa => (
                    <HistoryItem key={qa.questionId} qa={qa} />
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      <style>{`
        .spin-icon { animation: spin 1s linear infinite; }
        @keyframes spin { to { transform: rotate(360deg); } }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
        .form-control:focus { border-color: rgba(244,114,182,0.5) !important; box-shadow: 0 0 0 3px rgba(244,114,182,0.1) !important; outline: none; }
      `}</style>
    </div>
  );
};
