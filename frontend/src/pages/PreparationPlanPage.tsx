import React from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, ArrowLeft, BookOpen, CalendarDays, CheckCircle2, ClipboardList, Clock3, Loader2, RefreshCw, Target } from 'lucide-react';
import { AxiosError } from 'axios';
import { preparationPlanApi } from '../services/api';
import type { ApiError, PreparationPlanResponse, PreparationPlanSkillArea } from '../types';

const priorityColor = (priority: PreparationPlanSkillArea['priority']) =>
  priority === 'HIGH' ? '#f87171' : '#fbbf24';

const TaskList: React.FC<{ title: string; items: string[]; icon: React.ReactNode }> = ({ title, items, icon }) => (
  <div>
    <h3 className="small text-uppercase fw-semibold d-flex align-items-center gap-2 mb-2" style={{ color: 'var(--text-muted)' }}>
      {icon}{title}
    </h3>
    <ul className="small mb-0 ps-3" style={{ color: 'var(--text-muted)' }}>
      {items.map(item => <li key={item} className="mb-1">{item}</li>)}
    </ul>
  </div>
);

const SkillArea: React.FC<{ area: PreparationPlanSkillArea; index: number }> = ({ area, index }) => (
  <article className="glass-panel p-4" aria-labelledby={`plan-skill-${index}`}>
    <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-3">
      <div>
        <h2 id={`plan-skill-${index}`} className="h5 fw-semibold text-white mb-2">{area.skillName}</h2>
        <div className="d-flex flex-wrap gap-2">
          <span className="badge" style={{ color: area.currentStatus === 'MISSING' ? '#fca5a5' : '#fde68a', background: area.currentStatus === 'MISSING' ? 'rgba(248,113,113,0.14)' : 'rgba(251,191,36,0.14)' }}>
            {area.currentStatus === 'MISSING' ? 'Missing' : 'Needs more evidence'}
          </span>
          <span className="badge" style={{ color: priorityColor(area.priority), background: `${priorityColor(area.priority)}20` }}>
            {area.priority} priority
          </span>
        </div>
      </div>
      <div className="d-flex align-items-center gap-3 small" style={{ color: 'var(--text-muted)' }}>
        <span className="d-inline-flex align-items-center gap-1"><CalendarDays size={15} />{area.estimatedDays} days</span>
        <span className="d-inline-flex align-items-center gap-1"><Clock3 size={15} />~{area.estimatedHours} hours</span>
      </div>
    </div>
    <div className="row g-4">
      <div className="col-lg-4"><TaskList title="Recommended topics" items={area.recommendedTopics} icon={<BookOpen size={14} />} /></div>
      <div className="col-lg-4"><TaskList title="Practice tasks" items={area.practiceTasks} icon={<CheckCircle2 size={14} />} /></div>
      <div className="col-lg-4"><TaskList title="Interview practice" items={area.interviewSuggestions} icon={<Target size={14} />} /></div>
    </div>
  </article>
);

export const PreparationPlanPage: React.FC = () => {
  const queryClient = useQueryClient();
  const planQuery = useQuery({
    queryKey: ['preparation-plan'],
    queryFn: async (): Promise<PreparationPlanResponse | null> => {
      try {
        return await preparationPlanApi.getCurrent();
      } catch (error) {
        if ((error as AxiosError).response?.status === 404) return null;
        throw error;
      }
    },
    retry: false,
  });
  const generateMutation = useMutation({
    mutationFn: preparationPlanApi.generate,
    onSuccess: plan => queryClient.setQueryData(['preparation-plan'], plan),
  });

  const plan = planQuery.data;
  const requestError = generateMutation.error ?? planQuery.error;
  const errorMessage = requestError
    ? (requestError as AxiosError<ApiError>).response?.data?.message
      ?? 'The preparation plan could not be loaded. Please try again.'
    : null;

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-primary)' }}>
      <nav className="px-4 py-3 d-flex align-items-center gap-3"
        style={{ background: 'rgba(15,15,25,0.9)', borderBottom: '1px solid var(--border-color)', position: 'sticky', top: 0, zIndex: 100 }}>
        <Link to="/dashboard" className="d-flex align-items-center gap-2 text-decoration-none" style={{ color: 'var(--text-muted)' }}>
          <ArrowLeft size={16} /><span style={{ fontSize: '0.875rem' }}>Dashboard</span>
        </Link>
        <span style={{ color: 'var(--border-color)' }}>|</span>
        <div className="d-flex align-items-center gap-2 text-white"><ClipboardList size={17} style={{ color: '#38bdf8' }} />Preparation Plan</div>
      </nav>

      <main className="container py-5" style={{ maxWidth: '1080px' }}>
        <header className="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
          <div>
            <h1 className="h3 fw-bold text-white mb-2">Your preparation roadmap</h1>
            <p className="mb-0" style={{ color: 'var(--text-muted)' }}>
              Focused practice generated from your latest resume-to-role skill comparison.
            </p>
          </div>
          <button type="button" className="btn d-flex align-items-center gap-2"
            onClick={() => generateMutation.mutate()}
            disabled={generateMutation.isPending || planQuery.isLoading}
            style={{ background: '#38bdf8', color: '#07151b', border: 0, fontWeight: 650 }}>
            {generateMutation.isPending ? <Loader2 size={16} className="spin-icon" /> : plan ? <RefreshCw size={16} /> : <ClipboardList size={16} />}
            {generateMutation.isPending ? 'Generating…' : plan ? 'Regenerate plan' : 'Generate plan'}
          </button>
        </header>

        {planQuery.isLoading && (
          <p role="status" className="d-flex align-items-center gap-2" style={{ color: 'var(--text-muted)' }}>
            <Loader2 size={18} className="spin-icon" />Loading your latest plan…
          </p>
        )}

        {errorMessage && (
          <div role="alert" className="d-flex align-items-center gap-2 p-3 rounded-3 mb-4" style={{ color: '#fca5a5', background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.25)' }}>
            <AlertTriangle size={17} />
            <span>{errorMessage}</span>
          </div>
        )}

        {!planQuery.isLoading && !planQuery.isError && !plan && (
          <section className="glass-panel p-4 p-md-5 text-center">
            <div className="d-inline-flex p-3 rounded-3 mb-3" style={{ color: '#38bdf8', background: 'rgba(56,189,248,0.12)' }}>
              <ClipboardList size={30} />
            </div>
            <h2 className="h5 fw-bold text-white mb-2">Start with a skill comparison</h2>
            <p className="mx-auto mb-4" style={{ color: 'var(--text-muted)', maxWidth: '540px' }}>
              Generate a skill-gap analysis from one of your resumes and job descriptions. Your plan will use those saved results to prioritize learning and practice.
            </p>
            <Link to="/skill-gap" className="btn d-inline-flex align-items-center gap-2" style={{ background: '#38bdf8', color: '#07151b', fontWeight: 650 }}>
              <Target size={16} />Open skill gap analysis
            </Link>
          </section>
        )}

        {plan && (
          <div aria-live="polite">
            <section className="glass-panel p-4 mb-4">
              <div className="row g-3 align-items-center">
                <div className="col-md">
                  <div className="small text-uppercase mb-1" style={{ color: 'var(--text-muted)' }}>Target role</div>
                  <h2 className="h5 fw-semibold text-white mb-1">{plan.jobTitle}</h2>
                  <div className="small" style={{ color: 'var(--text-muted)' }}>Resume: {plan.resumeFileName}</div>
                  <div className="small mt-2" style={{ color: 'var(--text-dim)' }}>
                    Generated {new Date(plan.generatedAt).toLocaleString()}
                  </div>
                </div>
                <div className="col-6 col-md-auto text-md-center">
                  <div className="fw-bold text-white" style={{ fontSize: '1.8rem' }}>{plan.durationDays}</div>
                  <div className="small" style={{ color: 'var(--text-muted)' }}>study days</div>
                </div>
                <div className="col-6 col-md-auto text-md-center">
                  <div className="fw-bold" style={{ color: '#34d399', fontSize: '1.8rem' }}>{Math.round(plan.matchPercentage)}%</div>
                  <div className="small" style={{ color: 'var(--text-muted)' }}>starting match</div>
                </div>
              </div>
            </section>

            {!!plan.strongSkills.length && (
              <section className="mb-4" aria-labelledby="strong-skills-heading">
                <h2 id="strong-skills-heading" className="h6 fw-semibold text-white mb-2">Skills already matched</h2>
                <div className="d-flex flex-wrap gap-2">
                  {plan.strongSkills.map(skill => <span key={skill} className="badge" style={{ color: '#6ee7b7', background: 'rgba(52,211,153,0.12)', border: '1px solid rgba(52,211,153,0.25)' }}>{skill}</span>)}
                </div>
              </section>
            )}

            {plan.skillAreas.length ? (
              <section className="d-flex flex-column gap-3" aria-label="Preparation tasks by skill">
                {plan.skillAreas.map((area, index) => <SkillArea key={`${area.skillName}-${index}`} area={area} index={index} />)}
              </section>
            ) : (
              <section className="glass-panel p-4" role="status">
                <div className="d-flex align-items-start gap-3">
                  <CheckCircle2 size={22} style={{ color: '#34d399' }} />
                  <div>
                    <h2 className="h6 fw-semibold text-white">No weak or missing skills found</h2>
                    <p className="small mb-0" style={{ color: 'var(--text-muted)' }}>
                      Your required skills are already matched. Keep them current and use a practice interview to rehearse explaining your experience.
                    </p>
                    <Link to="/interview/start" className="btn btn-sm mt-3" style={{ color: '#f9a8d4', border: '1px solid rgba(244,114,182,0.3)' }}>Start a practice interview</Link>
                  </div>
                </div>
              </section>
            )}
          </div>
        )}
      </main>
    </div>
  );
};