import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { Eye, EyeOff, Sparkles, UserPlus, AlertCircle } from 'lucide-react';
import { authApi } from '../services/api';
import { useAuth } from '../context/AuthContext';
import type { ApiError } from '../types';
import { AxiosError } from 'axios';

export const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ fullName: '', email: '', password: '', confirmPassword: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const mutation = useMutation({
    mutationFn: () => authApi.register({
      fullName: form.fullName,
      email: form.email,
      password: form.password,
    }),
    onSuccess: (data) => {
      login(data);
      navigate('/dashboard');
    },
    onError: (error: AxiosError<ApiError>) => {
      if (error.response?.data?.fieldErrors) {
        setFieldErrors(error.response.data.fieldErrors);
      }
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setFieldErrors({});
    if (form.password !== form.confirmPassword) {
      setFieldErrors({ confirmPassword: 'Passwords do not match' });
      return;
    }
    mutation.mutate();
  };

  const errorMessage = mutation.error
    ? (mutation.error as AxiosError<ApiError>).response?.data?.message || 'Registration failed'
    : null;

  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center py-5" style={{ background: 'var(--bg-primary)' }}>
      <div className="w-100" style={{ maxWidth: '480px', padding: '0 1rem' }}>

        {/* Logo */}
        <div className="text-center mb-4">
          <div className="d-inline-flex align-items-center justify-content-center mb-3"
            style={{ width: '56px', height: '56px', borderRadius: '16px', background: 'linear-gradient(135deg, #6366f1 0%, #a855f7 100%)', boxShadow: '0 0 24px rgba(99,102,241,0.45)' }}>
            <Sparkles size={26} className="text-white" />
          </div>
          <h1 className="h3 fw-bold text-white mb-1">Create your account</h1>
          <p style={{ color: 'var(--text-muted)' }}>Join the AI-powered interview platform</p>
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
            {/* Full Name */}
            <div className="mb-3">
              <label className="form-label" style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Full Name</label>
              <input
                id="register-fullname"
                type="text"
                className="form-control"
                placeholder="Jane Smith"
                value={form.fullName}
                onChange={e => setForm(f => ({ ...f, fullName: e.target.value }))}
                required
                style={{ background: 'rgba(255,255,255,0.06)', border: `1px solid ${fieldErrors.fullName ? '#ef4444' : 'var(--border-color)'}`, color: 'white', borderRadius: '10px' }}
              />
              {fieldErrors.fullName && <div className="mt-1" style={{ color: '#fca5a5', fontSize: '0.8rem' }}>{fieldErrors.fullName}</div>}
            </div>

            {/* Email */}
            <div className="mb-3">
              <label className="form-label" style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Email Address</label>
              <input
                id="register-email"
                type="email"
                className="form-control"
                placeholder="jane@example.com"
                value={form.email}
                onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
                required
                style={{ background: 'rgba(255,255,255,0.06)', border: `1px solid ${fieldErrors.email ? '#ef4444' : 'var(--border-color)'}`, color: 'white', borderRadius: '10px' }}
              />
              {fieldErrors.email && <div className="mt-1" style={{ color: '#fca5a5', fontSize: '0.8rem' }}>{fieldErrors.email}</div>}
            </div>

            {/* Password */}
            <div className="mb-3">
              <label className="form-label" style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Password</label>
              <div className="position-relative">
                <input
                  id="register-password"
                  type={showPassword ? 'text' : 'password'}
                  className="form-control pe-5"
                  placeholder="Minimum 8 characters"
                  value={form.password}
                  onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                  required
                  style={{ background: 'rgba(255,255,255,0.06)', border: `1px solid ${fieldErrors.password ? '#ef4444' : 'var(--border-color)'}`, color: 'white', borderRadius: '10px' }}
                />
                <button type="button" onClick={() => setShowPassword(s => !s)}
                  className="btn btn-sm position-absolute top-50 end-0 translate-middle-y me-2"
                  style={{ color: 'var(--text-dim)', background: 'transparent', border: 'none', padding: '4px 8px' }}>
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
              {fieldErrors.password && <div className="mt-1" style={{ color: '#fca5a5', fontSize: '0.8rem' }}>{fieldErrors.password}</div>}
            </div>

            {/* Confirm Password */}
            <div className="mb-4">
              <label className="form-label" style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Confirm Password</label>
              <input
                id="register-confirm-password"
                type={showPassword ? 'text' : 'password'}
                className="form-control"
                placeholder="Repeat your password"
                value={form.confirmPassword}
                onChange={e => setForm(f => ({ ...f, confirmPassword: e.target.value }))}
                required
                style={{ background: 'rgba(255,255,255,0.06)', border: `1px solid ${fieldErrors.confirmPassword ? '#ef4444' : 'var(--border-color)'}`, color: 'white', borderRadius: '10px' }}
              />
              {fieldErrors.confirmPassword && <div className="mt-1" style={{ color: '#fca5a5', fontSize: '0.8rem' }}>{fieldErrors.confirmPassword}</div>}
            </div>

            <button
              id="register-submit"
              type="submit"
              disabled={mutation.isPending}
              className="btn w-100 fw-semibold d-flex align-items-center justify-content-center gap-2"
              style={{ background: 'linear-gradient(135deg, #6366f1, #a855f7)', color: 'white', borderRadius: '10px', padding: '12px', border: 'none' }}>
              {mutation.isPending
                ? <><span className="spinner-border spinner-border-sm" /><span>Creating account...</span></>
                : <><UserPlus size={18} /><span>Create Account</span></>}
            </button>
          </form>

          <div className="text-center mt-4 pt-3 border-top" style={{ borderColor: 'var(--border-color)' }}>
            <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Already have an account? </span>
            <Link to="/login" className="fw-semibold" style={{ color: '#818cf8', textDecoration: 'none' }}>Sign in</Link>
          </div>
        </div>
      </div>
    </div>
  );
};
