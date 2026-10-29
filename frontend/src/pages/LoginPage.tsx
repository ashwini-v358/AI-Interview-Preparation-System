import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { Eye, EyeOff, Sparkles, LogIn, AlertCircle } from 'lucide-react';
import { authApi } from '../services/api';
import { useAuth } from '../context/AuthContext';
import type { ApiError } from '../types';
import { AxiosError } from 'axios';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ email: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);

  const mutation = useMutation({
    mutationFn: () => authApi.login({ email: form.email, password: form.password }),
    onSuccess: (data) => {
      login(data);
      navigate('/dashboard');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate();
  };

  const errorMessage = mutation.error
    ? (mutation.error as AxiosError<ApiError>).response?.data?.message || 'Login failed. Please check your credentials.'
    : null;

  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center py-5"
      style={{ background: 'var(--bg-primary)' }}>
      <div className="w-100" style={{ maxWidth: '420px', padding: '0 1rem' }}>

        {/* Logo */}
        <div className="text-center mb-4">
          <div className="d-inline-flex align-items-center justify-content-center mb-3"
            style={{ width: '56px', height: '56px', borderRadius: '16px', background: 'linear-gradient(135deg, #6366f1 0%, #a855f7 100%)', boxShadow: '0 0 24px rgba(99,102,241,0.45)' }}>
            <Sparkles size={26} className="text-white" />
          </div>
          <h1 className="h3 fw-bold text-white mb-1">Welcome back</h1>
          <p style={{ color: 'var(--text-muted)' }}>Sign in to your interview platform</p>
        </div>

        {/* Quick-fill for demo */}
        <div className="mb-4">
          <div className="d-flex gap-2">
            {[
              { label: '👤 Candidate', email: 'candidate@smartinterview.ai', pass: 'Candidate@123456' },
              { label: '🛡️ Admin', email: 'admin@smartinterview.ai', pass: 'Admin@123456' },
            ].map(({ label, email, pass }) => (
              <button key={label}
                type="button"
                onClick={() => setForm({ email, password: pass })}
                className="btn btn-sm flex-fill"
                style={{ background: 'rgba(99,102,241,0.1)', border: '1px solid rgba(99,102,241,0.3)', color: '#a5b4fc', borderRadius: '8px', fontSize: '0.8rem' }}>
                {label}
              </button>
            ))}
          </div>
          <p className="text-center mt-1 mb-0" style={{ color: 'var(--text-dim)', fontSize: '0.75rem' }}>Quick-fill seed accounts</p>
        </div>

        {/* Card */}
        <div className="glass-panel p-4 p-md-5">
          {errorMessage && (
            <div className="d-flex align-items-center gap-2 p-3 rounded-3 mb-4"
              style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)' }}>
              <AlertCircle size={18} className="text-danger flex-shrink-0" />
              <span style={{ color: '#fca5a5', fontSize: '0.9rem' }}>{errorMessage}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} noValidate>
            {/* Email */}
            <div className="mb-3">
              <label className="form-label" style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
                Email Address
              </label>
              <input
                id="login-email"
                type="email"
                className="form-control"
                placeholder="you@example.com"
                value={form.email}
                onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
                required
                style={{ background: 'rgba(255,255,255,0.06)', border: '1px solid var(--border-color)', color: 'white', borderRadius: '10px' }}
              />
            </div>

            {/* Password */}
            <div className="mb-4">
              <label className="form-label" style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
                Password
              </label>
              <div className="position-relative">
                <input
                  id="login-password"
                  type={showPassword ? 'text' : 'password'}
                  className="form-control pe-5"
                  placeholder="Your password"
                  value={form.password}
                  onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                  required
                  style={{ background: 'rgba(255,255,255,0.06)', border: '1px solid var(--border-color)', color: 'white', borderRadius: '10px' }}
                />
                <button type="button" onClick={() => setShowPassword(s => !s)}
                  className="btn btn-sm position-absolute top-50 end-0 translate-middle-y me-2"
                  style={{ color: 'var(--text-dim)', background: 'transparent', border: 'none', padding: '4px 8px' }}>
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <button
              id="login-submit"
              type="submit"
              disabled={mutation.isPending}
              className="btn w-100 fw-semibold d-flex align-items-center justify-content-center gap-2"
              style={{ background: 'linear-gradient(135deg, #6366f1, #a855f7)', color: 'white', borderRadius: '10px', padding: '12px', border: 'none' }}>
              {mutation.isPending
                ? <><span className="spinner-border spinner-border-sm" /><span>Signing in...</span></>
                : <><LogIn size={18} /><span>Sign In</span></>}
            </button>
          </form>

          <div className="text-center mt-4 pt-3 border-top" style={{ borderColor: 'var(--border-color)' }}>
            <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Don't have an account? </span>
            <Link to="/register" className="fw-semibold" style={{ color: '#818cf8', textDecoration: 'none' }}>Create one</Link>
          </div>
        </div>
      </div>
    </div>
  );
};
