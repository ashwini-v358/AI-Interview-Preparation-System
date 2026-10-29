import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Navbar } from './components/Navbar';
import { Footer } from './components/Footer';
import { Home } from './pages/Home';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { Dashboard } from './pages/Dashboard';
import { ResumeUploadPage } from './pages/ResumeUploadPage';
import { JdUploadPage } from './pages/JdUploadPage';
import { InterviewSetupPage } from './pages/InterviewSetupPage';
import { InterviewSessionPage } from './pages/InterviewSessionPage';
import { SkillGapAnalysisPage } from './pages/SkillGapAnalysisPage';
import { PreparationPlanPage } from './pages/PreparationPlanPage';
import { AuthProvider, useAuth } from './context/AuthContext';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      staleTime: 1000 * 30,
    },
  },
});

// ─── Loading spinner (reused by guards) ───────────────────────────────────────
const FullPageSpinner: React.FC = () => (
  <div
    className="d-flex align-items-center justify-content-center"
    style={{ minHeight: '100vh', background: 'var(--bg-primary)' }}
  >
    <div className="text-center">
      <span
        className="spinner-border text-primary mb-3"
        style={{ width: '3rem', height: '3rem' }}
      />
      <p style={{ color: 'var(--text-muted)' }}>Verifying session…</p>
    </div>
  </div>
);

// ─── ProtectedRoute ───────────────────────────────────────────────────────────
// Redirects unauthenticated users to /login.
// Renders children only when isAuthenticated = true.
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();
  if (isLoading) return <FullPageSpinner />;
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
};

// ─── PublicOnlyRoute ──────────────────────────────────────────────────────────
// Redirects already-authenticated users away from login/register to dashboard.
const PublicOnlyRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();
  if (isLoading) return null;
  return !isAuthenticated ? <>{children}</> : <Navigate to="/dashboard" replace />;
};

// ─── Public layout: Navbar + Footer chrome ────────────────────────────────────
// Used ONLY for public pages (Home). Uses React Router <Outlet> so it works
// as a parent layout route — no nested <Routes> that could swallow paths.
const PublicLayout: React.FC = () => (
  <div className="d-flex flex-column min-vh-100">
    <Navbar />
    <main className="flex-grow-1">
      <Outlet />
    </main>
    <Footer />
  </div>
);

// ─── App ──────────────────────────────────────────────────────────────────────
export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <AuthProvider>
          <Routes>
            {/* ── Auth pages (no shell) ─────────────────────────────────── */}
            <Route
              path="/login"
              element={<PublicOnlyRoute><LoginPage /></PublicOnlyRoute>}
            />
            <Route
              path="/register"
              element={<PublicOnlyRoute><RegisterPage /></PublicOnlyRoute>}
            />

            {/* ── Protected pages (no shell, own nav bar) ───────────────── */}
            <Route
              path="/dashboard"
              element={<ProtectedRoute><Dashboard /></ProtectedRoute>}
            />
            <Route
              path="/resume/upload"
              element={<ProtectedRoute><ResumeUploadPage /></ProtectedRoute>}
            />
            <Route
              path="/jd/upload"
              element={<ProtectedRoute><JdUploadPage /></ProtectedRoute>}
            />
            <Route
              path="/interview/start"
              element={<ProtectedRoute><InterviewSetupPage /></ProtectedRoute>}
            />
            <Route
              path="/interview/:id"
              element={<ProtectedRoute><InterviewSessionPage /></ProtectedRoute>}
            />
            <Route
              path="/skill-gap"
              element={<ProtectedRoute><SkillGapAnalysisPage /></ProtectedRoute>}
            />
            <Route
              path="/preparation-plan"
              element={<ProtectedRoute><PreparationPlanPage /></ProtectedRoute>}
            />

            {/* ── Public pages with Navbar/Footer chrome ────────────────── */}
            {/*
             *  Using a layout route with <Outlet> instead of a "/*" wildcard
             *  with nested <Routes>. This prevents the shell from swallowing
             *  protected routes and causing the "Navbar + blank content" bug
             *  that occurred when stale HMR state poisoned the module graph.
             */}
            <Route element={<PublicLayout />}>
              <Route path="/" element={<Home />} />
            </Route>

            {/* ── Fallback: unknown routes → Home ───────────────────────── */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthProvider>
      </Router>
    </QueryClientProvider>
  );
};

export default App;
