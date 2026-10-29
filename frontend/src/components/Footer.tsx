import React from 'react';
import { Cpu, Heart } from 'lucide-react';

export const Footer: React.FC = () => {
  return (
    <footer className="mt-5 py-4 border-top" style={{ borderColor: 'var(--border-color)', background: 'var(--bg-secondary)' }}>
      <div className="container d-flex flex-column flex-md-row align-items-center justify-content-between gap-3 text-center text-md-start">
        <div>
          <div className="fw-semibold text-white d-flex align-items-center justify-content-center justify-content-md-start gap-2">
            <Cpu size={16} className="text-primary" />
            <span>Resume analysis, job targeting, and REST interview practice</span>
          </div>
          <small style={{ color: 'var(--text-dim)' }}>
            Java 21 • Spring Boot 3 • React 19 • H2 / MySQL
          </small>
        </div>
        <div className="d-flex align-items-center gap-2" style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
          <span>Connected to the existing backend APIs</span>
          <span>•</span>
          <span className="d-flex align-items-center gap-1">
            Built for Final Year Project <Heart size={14} className="text-danger fill-danger" />
          </span>
        </div>
      </div>
    </footer>
  );
};
