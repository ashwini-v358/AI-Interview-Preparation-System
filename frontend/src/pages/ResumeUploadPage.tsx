import React, { useCallback, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Upload, FileText, CheckCircle2, AlertCircle, Brain,
  ArrowLeft, Zap, Clock, BarChart3, X,
} from 'lucide-react';

import { resumeApi } from '../services/api';
import { AxiosError } from 'axios';

export const ResumeUploadPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [dragOver, setDragOver] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);
  const [result, setResult] = useState<Awaited<ReturnType<typeof resumeApi.upload>> | null>(null);

  const resumesQuery = useQuery({
    queryKey: ['my-resumes'],
    queryFn: resumeApi.getMyResumes,
  });

  // ── Upload mutation ────────────────────────────────────────────────────────
  const mutation = useMutation({
    mutationFn: resumeApi.upload,
    onSuccess: (data) => {
      setResult(data);
      void queryClient.invalidateQueries({ queryKey: ['my-resumes'] });
    },
  });

  const selectFile = (file: File) => {
    if (file.type !== 'application/pdf') {
      setFileError('Select a PDF file.');
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      setFileError('The PDF must be 10 MB or smaller.');
      return;
    }
    setFileError(null);
    setSelectedFile(file);
    setResult(null);
  };

  // ── Drag-and-drop handlers ─────────────────────────────────────────────────
  const onDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file) selectFile(file);
  }, []);

  const onFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) selectFile(file);
  };

  const handleUpload = () => {
    if (selectedFile) mutation.mutate(selectedFile);
  };

  const errorMessage = mutation.error
    ? (mutation.error as AxiosError<{ message: string }>).response?.data?.message || 'Upload failed. Please try again.'
    : null;

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-primary)' }}>
      {/* Top bar */}
      <div className="px-4 py-3 d-flex align-items-center gap-3"
        style={{ borderBottom: '1px solid var(--border-color)', background: 'rgba(15,15,25,0.85)', backdropFilter: 'blur(12px)', position: 'sticky', top: 0, zIndex: 100 }}>
        <Link to="/dashboard" className="btn btn-sm d-flex align-items-center gap-2"
          style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid var(--border-color)', color: 'var(--text-muted)', borderRadius: '8px' }}>
          <ArrowLeft size={14} /> Dashboard
        </Link>
        <div className="d-flex align-items-center gap-2">
          <FileText size={18} style={{ color: '#34d399' }} />
          <span className="fw-semibold text-white">Resume Upload & AI Analysis</span>
        </div>
        <span className="badge-tech badge-status-up ms-auto">Phase 4 Active</span>
      </div>

      <div className="container py-5" style={{ maxWidth: '860px' }}>

        {/* Header */}
        <div className="text-center mb-5">
          <div className="d-inline-flex p-3 rounded-4 mb-3"
            style={{ background: 'rgba(52,211,153,0.12)', border: '1px solid rgba(52,211,153,0.25)' }}>
            <Brain size={32} style={{ color: '#34d399' }} />
          </div>
          <h1 className="h3 fw-bold text-white mb-2">AI Resume Analysis</h1>
          <p style={{ color: 'var(--text-muted)' }}>
            Upload your PDF resume. Our AI will extract skills, experience and generate a professional summary.
          </p>
        </div>

        {/* Upload Panel */}
        {!result && (
          <div className="glass-panel p-4 p-md-5 mb-4">
            {/* Drop Zone */}
            <div
              id="resume-drop-zone"
              onClick={() => fileInputRef.current?.click()}
              onDrop={onDrop}
              onDragOver={e => { e.preventDefault(); setDragOver(true); }}
              onDragLeave={() => setDragOver(false)}
              className="d-flex flex-column align-items-center justify-content-center p-5 rounded-4"
              style={{
                border: `2px dashed ${dragOver ? '#34d399' : selectedFile ? '#818cf8' : 'var(--border-color)'}`,
                background: dragOver ? 'rgba(52,211,153,0.05)' : selectedFile ? 'rgba(99,102,241,0.05)' : 'rgba(255,255,255,0.02)',
                cursor: 'pointer',
                transition: 'all 0.25s ease',
                minHeight: '220px',
              }}>
              <input ref={fileInputRef} type="file" accept=".pdf,application/pdf" className="d-none" onChange={onFileChange} id="resume-file-input" />

              {selectedFile ? (
                <>
                  <div className="d-inline-flex p-3 rounded-3 mb-3" style={{ background: 'rgba(99,102,241,0.15)' }}>
                    <FileText size={32} style={{ color: '#818cf8' }} />
                  </div>
                  <div className="fw-semibold text-white mb-1">{selectedFile.name}</div>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                    {(selectedFile.size / 1024).toFixed(1)} KB · PDF
                  </div>
                  <button onClick={e => { e.stopPropagation(); setSelectedFile(null); }}
                    className="btn btn-sm mt-3 d-flex align-items-center gap-1"
                    style={{ color: '#fca5a5', background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: '8px' }}>
                    <X size={14} /> Remove
                  </button>
                </>
              ) : (
                <>
                  <div className="d-inline-flex p-3 rounded-3 mb-3" style={{ background: 'rgba(52,211,153,0.1)' }}>
                    <Upload size={32} style={{ color: '#34d399' }} />
                  </div>
                  <div className="fw-semibold text-white mb-1">Drop your PDF resume here</div>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>or click to browse · Max 10 MB</div>
                </>
              )}
            </div>

            {/* Error */}
            {errorMessage && (
              <div className="d-flex align-items-center gap-2 p-3 rounded-3 mt-4"
                style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)' }}>
                <AlertCircle size={18} className="text-danger flex-shrink-0" />
                <span style={{ color: '#fca5a5', fontSize: '0.9rem' }}>{errorMessage}</span>
              </div>
            )}
            {fileError && (
              <div role="alert" className="d-flex align-items-center gap-2 p-3 rounded-3 mt-4"
                style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)' }}>
                <AlertCircle size={18} className="text-danger flex-shrink-0" />
                <span style={{ color: '#fca5a5', fontSize: '0.9rem' }}>{fileError}</span>
              </div>
            )}

            {/* Upload Button */}
            <button
              id="resume-upload-btn"
              onClick={handleUpload}
              disabled={!selectedFile || mutation.isPending}
              className="btn w-100 fw-semibold d-flex align-items-center justify-content-center gap-2 mt-4"
              style={{
                background: selectedFile ? 'linear-gradient(135deg, #34d399, #059669)' : 'rgba(255,255,255,0.06)',
                color: selectedFile ? 'white' : 'var(--text-dim)',
                border: 'none', borderRadius: '10px', padding: '13px',
                transition: 'all 0.2s ease',
              }}>
              {mutation.isPending ? (
                <><span className="spinner-border spinner-border-sm" /><span>Analysing with AI…</span></>
              ) : (
                <><Zap size={18} /><span>Upload & Analyse with AI</span></>
              )}
            </button>

            {/* Processing Info */}
            {mutation.isPending && (
              <div className="mt-4">
                {[
                  { step: 'Storing PDF securely…', done: true },
                  { step: 'Extracting text with PDFBox…', done: true },
                  { step: 'Sending to AI for analysis…', done: false },
                ].map(({ step, done }, i) => (
                  <div key={i} className="d-flex align-items-center gap-2 mb-2">
                    {done
                      ? <CheckCircle2 size={15} className="text-success" />
                      : <span className="spinner-border spinner-border-sm" style={{ width: '15px', height: '15px', color: '#818cf8' }} />}
                    <span style={{ color: done ? 'var(--text-muted)' : 'var(--text-white)', fontSize: '0.85rem' }}>{step}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Analysis Result Panel */}
        {result && (
          <div className="glass-panel p-4 p-md-5">
            {/* Success Header */}
            <div className="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4 pb-3"
              style={{ borderBottom: '1px solid var(--border-color)' }}>
              <div className="d-flex align-items-center gap-3">
                <div className="d-flex p-2 rounded-3" style={{ background: 'rgba(52,211,153,0.15)' }}>
                  <CheckCircle2 size={24} className="text-success" />
                </div>
                <div>
                  <h2 className="h5 fw-bold text-white mb-0">Analysis Complete</h2>
                  <small style={{ color: 'var(--text-muted)' }}>{result.originalFileName} · {result.processingTimeMs}ms</small>
                </div>
              </div>
              <div className="d-flex gap-2">
                <button onClick={() => { setResult(null); setSelectedFile(null); }}
                  className="btn btn-sm"
                  style={{ background: 'rgba(99,102,241,0.1)', border: '1px solid rgba(99,102,241,0.3)', color: '#a5b4fc', borderRadius: '8px' }}>
                  Upload Another
                </button>
                <button onClick={() => navigate('/dashboard')}
                  className="btn btn-sm"
                  style={{ background: 'rgba(52,211,153,0.12)', border: '1px solid rgba(52,211,153,0.3)', color: '#34d399', borderRadius: '8px' }}>
                  Dashboard
                </button>
              </div>
            </div>

            {/* Stats Row */}
            <div className="row g-3 mb-4">
              {[
                { icon: FileText, label: 'Text Extracted', val: `${result.extractedTextLength.toLocaleString()} chars`, color: '#818cf8' },
                { icon: Clock, label: 'Experience', val: `${result.experienceYears} yrs`, color: '#fbbf24' },
                { icon: Zap, label: 'Skills Found', val: `${result.extractedSkills.length} skills`, color: '#34d399' },
                { icon: BarChart3, label: 'AI Provider', val: result.aiProvider, color: '#c084fc' },
              ].map(({ icon: Icon, label, val, color }) => (
                <div key={label} className="col-6 col-md-3">
                  <div className="p-3 rounded-3 text-center h-100"
                    style={{ background: 'rgba(0,0,0,0.25)', border: '1px solid var(--border-color)' }}>
                    <Icon size={20} style={{ color }} className="mb-2" />
                    <div className="fw-bold text-white mb-1">{val}</div>
                    <div className="small" style={{ color: 'var(--text-muted)', fontSize: '0.78rem' }}>{label}</div>
                  </div>
                </div>
              ))}
            </div>

            {/* Summary */}
            {result.summary && (
              <div className="p-3 rounded-3 mb-4" style={{ background: 'rgba(99,102,241,0.08)', border: '1px solid rgba(99,102,241,0.2)' }}>
                <div className="small text-uppercase fw-semibold mb-2" style={{ color: 'var(--text-dim)' }}>AI-Generated Summary</div>
                <p className="mb-0 text-white" style={{ fontSize: '0.9rem', lineHeight: '1.6' }}>{result.summary}</p>
              </div>
            )}

            {/* Extracted Skills */}
            <div className="mb-4">
              <div className="small text-uppercase fw-semibold mb-3" style={{ color: 'var(--text-dim)' }}>Extracted Skills</div>
              <div className="d-flex flex-wrap gap-2">
                {result.extractedSkills.map((skill) => (
                  <span key={skill} className="badge-tech" style={{ background: 'rgba(52,211,153,0.1)', color: '#34d399', border: '1px solid rgba(52,211,153,0.3)' }}>
                    {skill}
                  </span>
                ))}
              </div>
            </div>

            {/* Domains & Education */}
            <div className="row g-3">
              <div className="col-md-6">
                <div className="small text-uppercase fw-semibold mb-2" style={{ color: 'var(--text-dim)' }}>Domains</div>
                <div className="d-flex flex-wrap gap-2">
                  {result.domains.map(d => (
                    <span key={d} className="badge-tech" style={{ background: 'rgba(168,85,247,0.1)', color: '#c084fc', border: '1px solid rgba(168,85,247,0.3)' }}>{d}</span>
                  ))}
                </div>
              </div>
              <div className="col-md-6">
                <div className="small text-uppercase fw-semibold mb-2" style={{ color: 'var(--text-dim)' }}>Education</div>
                <div className="d-flex flex-wrap gap-2">
                  {result.educationDetails.map(e => (
                    <span key={e} className="badge-tech" style={{ background: 'rgba(251,191,36,0.1)', color: '#fbbf24', border: '1px solid rgba(251,191,36,0.3)' }}>{e}</span>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        <section className="mt-5" aria-labelledby="resume-list-heading">
          <div className="d-flex align-items-center justify-content-between mb-3">
            <div>
              <h2 id="resume-list-heading" className="h5 fw-bold text-white mb-1">Your resumes</h2>
              <p className="mb-0" style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                Resumes uploaded to your account
              </p>
            </div>
            <span className="badge-tech">{resumesQuery.data?.length ?? 0} saved</span>
          </div>
          {resumesQuery.isLoading ? (
            <div className="glass-panel p-4 text-center" role="status">
              <span className="spinner-border spinner-border-sm text-success" />
              <span className="ms-2" style={{ color: 'var(--text-muted)' }}>Loading resumes…</span>
            </div>
          ) : resumesQuery.isError ? (
            <div className="glass-panel p-4" role="alert" style={{ color: '#fca5a5' }}>
              Could not load your saved resumes. Refresh the page to try again.
            </div>
          ) : resumesQuery.data?.length ? (
            <div className="glass-panel p-3">
              {resumesQuery.data.map((resume) => (
                <div key={resume.resumeId} className="d-flex flex-wrap align-items-center justify-content-between gap-3 py-3"
                  style={{ borderBottom: '1px solid var(--border-color)' }}>
                  <div className="d-flex align-items-center gap-3">
                    <FileText size={18} style={{ color: '#34d399' }} />
                    <div>
                      <div className="text-white fw-semibold">{resume.originalFileName}</div>
                      <small style={{ color: 'var(--text-muted)' }}>
                        {new Date(resume.uploadedAt).toLocaleDateString()} · {resume.experienceYears} years experience
                      </small>
                    </div>
                  </div>
                  <div className="d-flex flex-wrap gap-2">
                    {resume.topSkills.map((skill) => (
                      <span key={skill} className="badge-tech">{skill}</span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="glass-panel p-4 text-center">
              <FileText size={24} className="mb-2" style={{ color: 'var(--text-dim)' }} />
              <p className="mb-0" style={{ color: 'var(--text-muted)' }}>No resumes uploaded yet.</p>
            </div>
          )}
        </section>
      </div>
    </div>
  );
};
