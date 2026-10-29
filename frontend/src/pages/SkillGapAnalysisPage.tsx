import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, ArrowLeft, BarChart3, CheckCircle2, ClipboardList, Loader2, Target } from 'lucide-react';
import { AxiosError } from 'axios';
import { jdApi, resumeApi, skillGapApi } from '../services/api';
import type { ApiError, SkillGapAnalysisItem, SkillGapAnalysisResponse } from '../types';

const SkillList: React.FC<{ title: string; items: SkillGapAnalysisItem[]; color: string }> = ({ title, items, color }) => (
  <section className="glass-panel p-4 h-100" aria-label={title}>
    <div className="d-flex align-items-center justify-content-between mb-3">
      <h2 className="h6 fw-semibold text-white mb-0">{title}</h2>
      <span className="badge" style={{ color, background: `${color}20` }}>{items.length}</span>
    </div>
    {items.length ? items.map(item => (
      <div key={item.skillName} className="py-2" style={{ borderTop: '1px solid var(--border-color)' }}>
        <div className="fw-semibold text-white">{item.skillName}</div>
        {item.resumeEvidence && <div className="small mt-1" style={{ color: 'var(--text-muted)' }}>Resume: {item.resumeEvidence}</div>}
        {item.suggestedPreparation && <div className="small mt-1" style={{ color }}>{item.suggestedPreparation}</div>}
      </div>
    )) : <p className="small mb-0" style={{ color: 'var(--text-muted)' }}>No skills in this category.</p>}
  </section>
);

export const SkillGapAnalysisPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [resumeId, setResumeId] = useState('');
  const [jobDescriptionId, setJobDescriptionId] = useState('');
  const selectedResumeId = Number(resumeId);
  const selectedJdId = Number(jobDescriptionId);
  const analysisKey = ['skill-gap', selectedResumeId, selectedJdId];

  const resumesQuery = useQuery({ queryKey: ['my-resumes'], queryFn: resumeApi.getMyResumes });
  const jdsQuery = useQuery({ queryKey: ['my-jds'], queryFn: jdApi.getMyJds });
  const analysisQuery = useQuery({
    queryKey: analysisKey,
    queryFn: async (): Promise<SkillGapAnalysisResponse | null> => {
      try {
        return await skillGapApi.getLatest(selectedResumeId, selectedJdId);
      } catch (error) {
        if ((error as AxiosError).response?.status === 404) return null;
        throw error;
      }
    },
    enabled: selectedResumeId > 0 && selectedJdId > 0,
    retry: false,
  });
  const analysisMutation = useMutation({
    mutationFn: () => skillGapApi.analyze(selectedResumeId, selectedJdId),
    onSuccess: result => queryClient.setQueryData(analysisKey, result),
  });

  const analysis = analysisQuery.data;
  const error = analysisMutation.error ?? analysisQuery.error ?? resumesQuery.error ?? jdsQuery.error;
  const errorMessage = error
    ? (error as AxiosError<ApiError>).response?.data?.message ?? 'The analysis could not be loaded. Please try again.'
    : null;
  const isBusy = resumesQuery.isLoading || jdsQuery.isLoading || analysisQuery.isFetching || analysisMutation.isPending;

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-primary)' }}>
      <nav className="px-4 py-3 d-flex align-items-center gap-3"
        style={{ background: 'rgba(15,15,25,0.9)', borderBottom: '1px solid var(--border-color)', position: 'sticky', top: 0, zIndex: 100 }}>
        <Link to="/dashboard" className="d-flex align-items-center gap-2 text-decoration-none" style={{ color: 'var(--text-muted)' }}>
          <ArrowLeft size={16} /><span style={{ fontSize: '0.875rem' }}>Dashboard</span>
        </Link>
        <span style={{ color: 'var(--border-color)' }}>|</span>
        <div className="d-flex align-items-center gap-2 text-white"><BarChart3 size={17} style={{ color: '#fbbf24' }} />Skill Gap Analysis</div>
      </nav>

      <main className="container py-5" style={{ maxWidth: '1000px' }}>
        <header className="mb-4">
          <h1 className="h3 fw-bold text-white mb-2">Match your resume to a role</h1>
          <p className="mb-0" style={{ color: 'var(--text-muted)' }}>
            Compare extracted resume skills with the required skills from one of your saved job descriptions.
          </p>
        </header>

        <section className="glass-panel p-4 mb-4" aria-label="Choose a resume and job description">
          <div className="row g-3 align-items-end">
            <div className="col-md-5">
              <label htmlFor="skill-gap-resume" className="form-label text-white">Resume</label>
              <select id="skill-gap-resume" className="form-select" value={resumeId} onChange={event => setResumeId(event.target.value)}
                disabled={resumesQuery.isLoading || !resumesQuery.data?.length}>
                <option value="">Choose a resume</option>
                {resumesQuery.data?.map(resume => <option key={resume.resumeId} value={resume.resumeId}>{resume.originalFileName}</option>)}
              </select>
            </div>
            <div className="col-md-5">
              <label htmlFor="skill-gap-jd" className="form-label text-white">Job description</label>
              <select id="skill-gap-jd" className="form-select" value={jobDescriptionId} onChange={event => setJobDescriptionId(event.target.value)}
                disabled={jdsQuery.isLoading || !jdsQuery.data?.length}>
                <option value="">Choose a job description</option>
                {jdsQuery.data?.map(jd => <option key={jd.jdId} value={jd.jdId}>{jd.title}{jd.companyName ? ` · ${jd.companyName}` : ''}</option>)}
              </select>
            </div>
            <div className="col-md-2">
              <button type="button" className="btn w-100 d-flex align-items-center justify-content-center gap-2"
                onClick={() => analysisMutation.mutate()}
                disabled={selectedResumeId <= 0 || selectedJdId <= 0 || analysisMutation.isPending || resumesQuery.isLoading || jdsQuery.isLoading}
                style={{ background: '#d99a24', border: 0, color: '#17130b', fontWeight: 650 }}>
                {analysisMutation.isPending ? <Loader2 size={16} className="spin-icon" /> : <Target size={16} />}
                Analyze
              </button>
            </div>
          </div>
          {!resumesQuery.isLoading && !resumesQuery.data?.length && (
            <p className="small mt-3 mb-0" style={{ color: 'var(--text-muted)' }}>Upload a resume before comparing skills.</p>
          )}
          {!jdsQuery.isLoading && !jdsQuery.data?.length && (
            <p className="small mt-2 mb-0" style={{ color: 'var(--text-muted)' }}>Add a job description before comparing skills.</p>
          )}
        </section>

        {errorMessage && <div role="alert" className="d-flex align-items-center gap-2 p-3 rounded-3 mb-4" style={{ color: '#fca5a5', background: 'rgba(239,68,68,0.1)' }}>
          <AlertTriangle size={17} />{errorMessage}
        </div>}

        {isBusy && selectedResumeId > 0 && selectedJdId > 0 && (
          <p role="status" className="d-flex align-items-center gap-2" style={{ color: 'var(--text-muted)' }}><Loader2 size={16} className="spin-icon" />Loading analysis…</p>
        )}

        {analysis && (
          <div aria-live="polite">
            <section className="glass-panel p-4 mb-4">
              <div className="row align-items-center g-3">
                <div className="col-auto">
                  <div className="fw-bold text-white" style={{ fontSize: '2.5rem', lineHeight: 1 }}>{Math.round(analysis.matchPercentage)}%</div>
                  <div className="small mt-1" style={{ color: 'var(--text-muted)' }}>Skill match</div>
                </div>
                <div className="col">
                  <div className="d-flex align-items-center gap-2 text-white fw-semibold"><CheckCircle2 size={17} style={{ color: '#34d399' }} />{analysis.jobTitle}</div>
                  <div className="small mt-1" style={{ color: 'var(--text-muted)' }}>Compared with {analysis.resumeFileName}</div>
                  <div className="progress mt-3" role="progressbar" aria-label="Skill match percentage" aria-valuenow={Math.round(analysis.matchPercentage)} aria-valuemin={0} aria-valuemax={100} style={{ height: 7 }}>
                    <div className="progress-bar" style={{ width: `${Math.min(100, analysis.matchPercentage)}%`, background: '#34d399' }} />
                  </div>
                </div>
                <div className="col-12 small" style={{ color: 'var(--text-muted)' }}>
                  {analysis.matchedSkills.length} matched · {analysis.partialSkills.length} partial · {analysis.missingSkills.length} missing
                </div>
              </div>
            </section>

            <div className="row g-3 mb-4">
              <div className="col-lg-4"><SkillList title="Matched" items={analysis.matchedSkills} color="#34d399" /></div>
              <div className="col-lg-4"><SkillList title="Partial evidence" items={analysis.partialSkills} color="#fbbf24" /></div>
              <div className="col-lg-4"><SkillList title="Missing" items={analysis.missingSkills} color="#f87171" /></div>
            </div>

            {!!analysis.recommendations.length && (
              <section className="glass-panel p-4" aria-labelledby="skill-gap-recommendations">
                <h2 id="skill-gap-recommendations" className="h6 fw-semibold text-white mb-3">Preparation priorities</h2>
                <ul className="small mb-0" style={{ color: 'var(--text-muted)' }}>
                  {analysis.recommendations.map(item => <li key={item} className="mb-2">{item}</li>)}
                </ul>
              </section>
            )}
            <div className="mt-4">
              <Link to="/preparation-plan" className="btn d-inline-flex align-items-center gap-2"
                style={{ background: '#38bdf8', color: '#07151b', fontWeight: 650 }}>
                <ClipboardList size={16} />Build a preparation plan
              </Link>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};