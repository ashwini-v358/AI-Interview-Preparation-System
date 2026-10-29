import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { API_BASE_URL, healthApi, systemApi } from '../services/api';
import type { PhaseInfo } from '../types';
import {
  Server,
  Database,
  Cpu,
  FileText,
  CheckCircle2,
  Clock,
  ArrowRight,
  Code2,
  ShieldCheck,
  Zap,
  LayoutList,
  Users,
  BrainCircuit,
} from 'lucide-react';

const PHASES: PhaseInfo[] = [
  {
    phase: 1,
    title: 'Project Setup & Scaffolding',
    description: 'Spring Boot 3, React TypeScript, MySQL 8, Maven, Git, and Docker.',
    status: 'COMPLETED',
    badge: 'Phase 1 - Active',
  },
  {
    phase: 2,
    title: 'Database & JPA Entities',
    description: '18 JPA entities, 17 Spring Data Repositories, Flyway V2 migration, and full entity lifecycle integration tests.',
    status: 'COMPLETED',
    badge: 'Phase 2 - Done',
  },
  {
    phase: 3,
    title: 'Authentication & Security',
    description: 'JWT (HS256), BCrypt, Spring Security 6 filter chain, Register/Login/Me endpoints, AuthContext, ProtectedRoute, Login & Register pages.',
    status: 'COMPLETED',
    badge: 'Phase 3 - Done',
  },
  {
    phase: 4,
    title: 'Resume Upload & AI Analysis',
    description: 'PDFBox 3.x extraction, FileStorageService, AIService abstraction, MockAIService, ResumeService pipeline, drag-and-drop upload page.',
    status: 'COMPLETED',
    badge: 'Phase 4 - Done',
  },
  {
    phase: 5,
    title: 'Job Description PDF Processing',
    description: 'JD parsing, structure verification, keyword storage, and role indexing.',
    status: 'PENDING',
    badge: 'Phase 5',
  },
  {
    phase: 6,
    title: 'AI Resume & JD Structured Analysis',
    description: 'AIService abstraction, JSON schema validation, structured skill & experience extraction.',
    status: 'PENDING',
    badge: 'Phase 6',
  },
  {
    phase: 7,
    title: 'Skill Gap Analysis Engine',
    description: 'Matched, Partial, and Not-Identified skill categorization and dashboard.',
    status: 'PENDING',
    badge: 'Phase 7',
  },
  {
    phase: 8,
    title: 'Interview Session Creation',
    description: 'Technical, Resume-based, JD-based, and Full interview mode configuration.',
    status: 'PENDING',
    badge: 'Phase 8',
  },
  {
    phase: 9,
    title: 'WebSocket Real-Time Session',
    description: 'Bi-directional STOMP over WebSocket protocol with secured candidate channels.',
    status: 'PENDING',
    badge: 'Phase 9',
  },
  {
    phase: 10,
    title: 'Adaptive Question Engine',
    description: 'Dynamic question difficulty adaptation based on prior candidate answer depth.',
    status: 'PENDING',
    badge: 'Phase 10',
  },
  {
    phase: 11,
    title: 'Real-Time Answer Evaluation',
    description: 'Internal conceptual & technical scoring, feedback loop, missing concept identification.',
    status: 'PENDING',
    badge: 'Phase 11',
  },
  {
    phase: 12,
    title: 'Detailed Performance Report',
    description: 'Comprehensive report, question-by-question breakdown, personalized prep plan.',
    status: 'PENDING',
    badge: 'Phase 12',
  },
  {
    phase: 13,
    title: 'Candidate Analytics Dashboard',
    description: 'Interview history, skill gap evolution, radar charts, progress metrics.',
    status: 'PENDING',
    badge: 'Phase 13',
  },
  {
    phase: 14,
    title: 'Admin Governance Dashboard',
    description: 'System-wide interview metrics, AI usage stats, candidate volume monitors.',
    status: 'PENDING',
    badge: 'Phase 14',
  },
  {
    phase: 15,
    title: 'Automated Testing Suite',
    description: 'Unit tests, Mockito mocks, Spring Boot Test, and integration tests.',
    status: 'PENDING',
    badge: 'Phase 15',
  },
  {
    phase: 16,
    title: 'Docker Orchestration',
    description: 'Multi-stage Docker containers, Docker Compose for backend, frontend, and MySQL.',
    status: 'PENDING',
    badge: 'Phase 16',
  },
  {
    phase: 17,
    title: 'Production Readiness & Deployment',
    description: 'Performance optimization, connection pooling, security hardening, final deliverables.',
    status: 'PENDING',
    badge: 'Phase 17',
  },
];

export const Home: React.FC = () => {
  const { data: health, isLoading, refetch } = useQuery({
    queryKey: ['system-health-diagnostics'],
    queryFn: healthApi.checkHealth,
    retry: 1,
  });

  const { data: schemaStatus, isLoading: schemaLoading } = useQuery({
    queryKey: ['schema-status'],
    queryFn: systemApi.getSchemaStatus,
    retry: 1,
    enabled: health?.status === 'UP',
  });

  return (
    <div className="container py-4">
      {/* Hero Section */}
      <div className="text-center py-5">
        <div className="d-inline-flex align-items-center gap-2 mb-3 flex-wrap justify-content-center">
          <span className="badge-tech">
            <Zap size={14} className="text-warning" /> Final Year Project Architecture
          </span>
          <span className="badge-tech badge-status-up">
            <CheckCircle2 size={14} /> Phase 1 &amp; 2 Verified
          </span>
          <span className="badge-tech" style={{ background: 'rgba(168, 85, 247, 0.15)', color: '#c084fc', border: '1px solid rgba(168, 85, 247, 0.35)' }}>
            <Database size={14} /> 18 JPA Entities Active
          </span>
        </div>
        <h1 className="display-4 fw-extrabold mb-3">
          AI-Based Adaptive Interview Preparation & <br />
          <span className="gradient-text">Real-Time Simulation Platform</span>
        </h1>
        <p className="lead mx-auto mb-4" style={{ maxWidth: '780px', color: 'var(--text-muted)' }}>
          Upload a <strong>resume PDF</strong>, analyze pasted <strong>job descriptions</strong>, and practice with an adaptive interview through the platform's authenticated REST APIs.
        </p>

        <div className="d-flex flex-wrap align-items-center justify-content-center gap-3">
          <a href="#diagnostics" className="btn-primary-custom">
            <span>Explore System Diagnostics</span>
            <ArrowRight size={18} />
          </a>
          <a
            href={`${API_BASE_URL}/swagger-ui/index.html`}
            target="_blank"
            rel="noreferrer"
            className="btn-secondary-custom"
          >
            <Code2 size={18} />
            <span>Open Swagger UI</span>
          </a>
        </div>
      </div>

      {/* Live System Diagnostics */}
      <div id="diagnostics" className="my-5">
        <div className="glass-panel p-4 p-md-5">
          <div className="d-flex flex-wrap align-items-center justify-content-between mb-4 pb-3 border-bottom" style={{ borderColor: 'var(--border-color)' }}>
            <div>
              <h3 className="h4 mb-1 d-flex align-items-center gap-2">
                <Server size={20} className="text-primary" />
                <span>Phase 1 System Verification & Live Diagnostics</span>
              </h3>
              <p className="small mb-0" style={{ color: 'var(--text-muted)' }}>
                Validating end-to-end communication between the React 19 frontend and Spring Boot 3.3.4 (Java 21) backend.
              </p>
            </div>
            <button
              onClick={() => refetch()}
              className="btn btn-sm btn-outline-light d-flex align-items-center gap-2 mt-2 mt-md-0"
              style={{ borderRadius: '8px', fontSize: '0.8rem' }}
            >
              <span>Refresh Status</span>
            </button>
          </div>

          <div className="row g-4">
            <div className="col-md-4">
              <div className="p-3 rounded-3" style={{ background: 'rgba(0, 0, 0, 0.25)', border: '1px solid var(--border-color)' }}>
                <div className="small text-uppercase fw-semibold" style={{ color: 'var(--text-dim)' }}>
                  Backend Health Status
                </div>
                <div className="d-flex align-items-center gap-2 mt-2">
                  {isLoading ? (
                    <span className="spinner-border spinner-border-sm text-primary"></span>
                  ) : health?.status === 'UP' ? (
                    <>
                      <div className="pulse-indicator"></div>
                      <span className="fw-bold fs-5 text-success">ONLINE (UP)</span>
                    </>
                  ) : (
                    <>
                      <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#ef4444' }}></span>
                      <span className="fw-bold fs-5 text-danger">OFFLINE (Port 8080)</span>
                    </>
                  )}
                </div>
                <small className="d-block mt-2" style={{ color: 'var(--text-muted)' }}>
                  Endpoint: <code>/api/v1/health</code>
                </small>
              </div>
            </div>

            <div className="col-md-4">
              <div className="p-3 rounded-3" style={{ background: 'rgba(0, 0, 0, 0.25)', border: '1px solid var(--border-color)' }}>
                <div className="small text-uppercase fw-semibold" style={{ color: 'var(--text-dim)' }}>
                  Backend Runtime
                </div>
                <div className="fw-bold fs-5 text-white mt-2">
                  Java 21 (Temurin LTS)
                </div>
                <small className="d-block mt-2" style={{ color: 'var(--text-muted)' }}>
                  Framework: Spring Boot 3.3.4
                </small>
              </div>
            </div>

            <div className="col-md-4">
              <div className="p-3 rounded-3" style={{ background: 'rgba(0, 0, 0, 0.25)', border: '1px solid var(--border-color)' }}>
                <div className="small text-uppercase fw-semibold" style={{ color: 'var(--text-dim)' }}>
                  Frontend Client
                </div>
                <div className="fw-bold fs-5 text-white mt-2">
                  React 19 + TypeScript + Vite
                </div>
                <small className="d-block mt-2" style={{ color: 'var(--text-muted)' }}>
                  UI: Bootstrap 5 & Custom Tokens
                </small>
              </div>
            </div>
          </div>

          {health && (
            <div className="mt-4 p-3 rounded-3" style={{ background: 'rgba(99, 102, 241, 0.08)', border: '1px solid rgba(99, 102, 241, 0.2)' }}>
              <div className="d-flex align-items-center gap-2 mb-2">
                <ShieldCheck size={18} className="text-primary" />
                <span className="fw-semibold text-white">Live Health Response</span>
              </div>
              <pre className="mb-0 text-white" style={{ fontSize: '0.85rem', fontFamily: 'monospace' }}>
                {JSON.stringify(health, null, 2)}
              </pre>
            </div>
          )}
        </div>
      </div>

      {/* Phase 2 Schema Status Panel */}
      <div id="schema-status" className="my-5">
        <div className="glass-panel p-4 p-md-5">
          <div className="d-flex flex-wrap align-items-center justify-content-between mb-4 pb-3 border-bottom" style={{ borderColor: 'var(--border-color)' }}>
            <div>
              <h3 className="h4 mb-1 d-flex align-items-center gap-2">
                <Database size={20} className="text-success" />
                <span>Phase 2 — Live Database Entity Telemetry</span>
              </h3>
              <p className="small mb-0" style={{ color: 'var(--text-muted)' }}>
                18 JPA entities and 17 Spring Data JPA Repositories mapped to H2 in-memory (local) / MySQL 8 (production).
              </p>
            </div>
            <span className="badge-tech badge-status-up mt-2 mt-md-0">
              <CheckCircle2 size={14} /> Phase 2 Verified
            </span>
          </div>

          {schemaLoading && (
            <div className="text-center py-4">
              <span className="spinner-border spinner-border-sm text-primary" />
              <span className="ms-2" style={{ color: 'var(--text-muted)' }}>Loading schema status…</span>
            </div>
          )}

          {!schemaLoading && !schemaStatus && (
            <div className="p-3 rounded-3 text-center" style={{ background: 'rgba(245,158,11,0.08)', border: '1px solid rgba(245,158,11,0.25)' }}>
              <p className="mb-0" style={{ color: '#fbbf24' }}>Start the Spring Boot backend on port 8080 to see live entity counts.</p>
            </div>
          )}

          {schemaStatus && (
            <>
              <div className="row g-3 mb-4">
                {[
                  { icon: Users, label: 'Users', val: schemaStatus.totalUsers, color: '#818cf8' },
                  { icon: FileText, label: 'Resumes', val: schemaStatus.totalResumes, color: '#34d399' },
                  { icon: BrainCircuit, label: 'Skills Catalog', val: schemaStatus.totalSkills, color: '#fbbf24' },
                  { icon: LayoutList, label: 'Interview Sessions', val: schemaStatus.totalInterviewSessions, color: '#c084fc' },
                ].map(({ icon: Icon, label, val, color }) => (
                  <div key={label} className="col-6 col-md-3">
                    <div className="p-3 rounded-3 text-center" style={{ background: 'rgba(0,0,0,0.3)', border: '1px solid var(--border-color)' }}>
                      <Icon size={22} style={{ color }} className="mb-2" />
                      <div className="fw-bold text-white" style={{ fontSize: '1.6rem' }}>{val}</div>
                      <div className="small" style={{ color: 'var(--text-muted)' }}>{label}</div>
                    </div>
                  </div>
                ))}
              </div>

              <div className="p-3 rounded-3" style={{ background: 'rgba(0,0,0,0.25)', border: '1px solid var(--border-color)' }}>
                <div className="small text-uppercase fw-semibold mb-3" style={{ color: 'var(--text-dim)' }}>All Entity Counts</div>
                <div className="row g-2">
                  {Object.entries(schemaStatus.entityCounts).map(([key, count]) => (
                    <div key={key} className="col-6 col-md-4 col-lg-3">
                      <div className="d-flex justify-content-between align-items-center px-2 py-1 rounded-2" style={{ background: 'rgba(255,255,255,0.03)' }}>
                        <code className="small" style={{ color: 'var(--text-muted)', fontSize: '0.78rem' }}>{key}</code>
                        <span className="badge" style={{ background: 'rgba(99,102,241,0.2)', color: '#a5b4fc', fontFamily: 'monospace' }}>{count}</span>
                      </div>
                    </div>
                  ))}
                </div>
                <div className="mt-3 pt-3 border-top" style={{ borderColor: 'var(--border-color)' }}>
                  <small style={{ color: 'var(--text-dim)' }}>
                    <strong style={{ color: 'var(--text-muted)' }}>Engine:</strong> {schemaStatus.databaseEngine} &nbsp;·&nbsp;
                    <strong style={{ color: 'var(--text-muted)' }}>Phase:</strong> {schemaStatus.phase}
                  </small>
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {/* Architecture Highlights */}
      <div id="architecture" className="my-5">
        <div className="text-center mb-5">
          <h2 className="h3 fw-bold mb-2">Platform Architecture Overview</h2>
          <p style={{ color: 'var(--text-muted)' }}>
            Engineered as a clean modular monolith adhering to SOLID design principles.
          </p>
        </div>

        <div className="row g-4">
          <div className="col-md-6 col-lg-3">
            <div className="glass-panel p-4 h-100">
              <div
                className="d-inline-flex p-3 rounded-3 mb-3"
                style={{ background: 'rgba(99, 102, 241, 0.15)', color: '#818cf8' }}
              >
                <Cpu size={24} />
              </div>
              <h3 className="h5 fw-bold mb-2">AI Service Abstraction</h3>
              <p className="small mb-0" style={{ color: 'var(--text-muted)' }}>
                Resume, JD, question, and evaluation workflows use the <code>AIService</code> interface; the current backend runs its mock provider.
              </p>
            </div>
          </div>

          <div className="col-md-6 col-lg-3">
            <div className="glass-panel p-4 h-100">
              <div
                className="d-inline-flex p-3 rounded-3 mb-3"
                style={{ background: 'rgba(16, 185, 129, 0.15)', color: '#34d399' }}
              >
                <Server size={24} />
              </div>
              <h3 className="h5 fw-bold mb-2">REST Interview Workflow</h3>
              <p className="small mb-0" style={{ color: 'var(--text-muted)' }}>
                Existing REST endpoints create sessions, deliver questions, accept answers, and return evaluation and progress.
              </p>
            </div>
          </div>

          <div className="col-md-6 col-lg-3">
            <div className="glass-panel p-4 h-100">
              <div
                className="d-inline-flex p-3 rounded-3 mb-3"
                style={{ background: 'rgba(245, 158, 11, 0.15)', color: '#fbbf24' }}
              >
                <FileText size={24} />
              </div>
              <h3 className="h5 fw-bold mb-2">PDF Extraction Pipeline</h3>
              <p className="small mb-0" style={{ color: 'var(--text-muted)' }}>
                Apache PDFBox extracts text from uploaded resume PDFs; job descriptions are currently submitted as text.
              </p>
            </div>
          </div>

          <div className="col-md-6 col-lg-3">
            <div className="glass-panel p-4 h-100">
              <div
                className="d-inline-flex p-3 rounded-3 mb-3"
                style={{ background: 'rgba(168, 85, 247, 0.15)', color: '#c084fc' }}
              >
                <Database size={24} />
              </div>
              <h3 className="h5 fw-bold mb-2">MySQL & Flyway</h3>
              <p className="small mb-0" style={{ color: 'var(--text-muted)' }}>
                Version-controlled schema migrations with Flyway, robust entity relationships, JPA/Hibernate ORM, and DTO layers.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* 17 Phase Development Roadmap */}
      <div id="roadmap" className="my-5">
        <div className="text-center mb-5">
          <h2 className="h3 fw-bold mb-2">Incremental 17-Phase Roadmap</h2>
          <p style={{ color: 'var(--text-muted)' }}>
            Each phase produces an incrementally working, fully tested milestone.
          </p>
        </div>

        <div className="row g-3">
          {PHASES.map((p) => {
            const isCompleted = p.status === 'COMPLETED';
            return (
              <div key={p.phase} className="col-md-6 col-lg-4">
                <div
                  className="glass-panel p-3 h-100 d-flex flex-column justify-content-between"
                  style={{
                    borderColor: isCompleted ? 'rgba(16, 185, 129, 0.4)' : 'var(--border-color)',
                    background: isCompleted ? 'rgba(16, 185, 129, 0.05)' : 'var(--bg-card)',
                  }}
                >
                  <div>
                    <div className="d-flex align-items-center justify-content-between mb-2">
                      <span className={`badge-tech ${isCompleted ? 'badge-status-up' : 'badge-status-pending'}`}>
                        {p.badge}
                      </span>
                      {isCompleted ? (
                        <CheckCircle2 size={18} className="text-success" />
                      ) : (
                        <Clock size={16} style={{ color: 'var(--text-dim)' }} />
                      )}
                    </div>
                    <div className="fw-semibold text-white mb-1" style={{ fontSize: '0.95rem' }}>
                      {p.title}
                    </div>
                    <p className="small mb-0" style={{ color: 'var(--text-muted)', fontSize: '0.82rem' }}>
                      {p.description}
                    </p>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
