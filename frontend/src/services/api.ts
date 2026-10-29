import axios, { AxiosError } from 'axios';
import type {
  AuthResponse, HealthResponse, LoginRequest, RegisterRequest, SchemaStatusResponse,
  ResumeSummaryResponse, ResumeUploadResponse,
  JdUploadRequest, JdUploadResponse, JdSummaryResponse,
  StartInterviewRequest, InterviewSessionResponse, InterviewReportResponse, SubmitAnswerRequest,
  AdaptiveProfileResponse,
  SkillGapAnalysisResponse, PreparationPlanResponse,
} from '../types';

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
});

// ── Request Interceptor: attach JWT Bearer token if present ──────────────────
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ── Response Interceptor: centralised error logging ──────────────────────────
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('auth_user');
      const isPublicAuthRequest = /\/api\/v1\/auth\/(login|register)$/.test(error.config?.url ?? '');
      if (!isPublicAuthRequest && typeof window !== 'undefined') {
        window.dispatchEvent(new Event('smart-interview:unauthorized'));
      }
    }
    console.error('[API Error]:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

// ── Health API ────────────────────────────────────────────────────────────────
export const healthApi = {
  checkHealth: async (): Promise<HealthResponse> => {
    const response = await apiClient.get<HealthResponse>('/api/v1/health');
    return response.data;
  },
};

// ── System / Schema API ───────────────────────────────────────────────────────
export const systemApi = {
  getSchemaStatus: async (): Promise<SchemaStatusResponse> => {
    const response = await apiClient.get<SchemaStatusResponse>('/api/v1/system/schema-status');
    return response.data;
  },
};

// ── Auth API ──────────────────────────────────────────────────────────────────
export const authApi = {
  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>('/api/v1/auth/register', data);
    return response.data;
  },

  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>('/api/v1/auth/login', data);
    return response.data;
  },

  getProfile: async (): Promise<import('../types').User> => {
    const response = await apiClient.get<import('../types').User>('/api/v1/auth/me');
    return response.data;
  },
};

// ── Resume API ───────────────────────────────────────────────────────────────
export const resumeApi = {
  upload: async (file: File): Promise<ResumeUploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await apiClient.post<ResumeUploadResponse>(
      '/api/v1/resume/upload',
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
    return response.data;
  },

  getMyResumes: async (): Promise<ResumeSummaryResponse[]> => {
    const response = await apiClient.get<ResumeSummaryResponse[]>('/api/v1/resume/my');
    return response.data;
  },

  getResumeDetail: async (id: number): Promise<ResumeUploadResponse> => {
    const response = await apiClient.get<ResumeUploadResponse>(`/api/v1/resume/${id}`);
    return response.data;
  },
};

// ── Job Description API ───────────────────────────────────────────────────────
export const jdApi = {
  upload: async (data: JdUploadRequest): Promise<JdUploadResponse> => {
    const response = await apiClient.post<JdUploadResponse>('/api/v1/jd/upload', data);
    return response.data;
  },

  getMyJds: async (): Promise<JdSummaryResponse[]> => {
    const response = await apiClient.get<JdSummaryResponse[]>('/api/v1/jd/my');
    return response.data;
  },

  getJdDetail: async (id: number): Promise<JdUploadResponse> => {
    const response = await apiClient.get<JdUploadResponse>(`/api/v1/jd/${id}`);
    return response.data;
  },
};

// ── Interview API ─────────────────────────────────────────────────────────────
export const interviewApi = {
  startSession: async (data: StartInterviewRequest): Promise<InterviewSessionResponse> => {
    const response = await apiClient.post<InterviewSessionResponse>('/api/v1/interview/start', data);
    return response.data;
  },

  startAdaptiveSession: async (data: StartInterviewRequest): Promise<InterviewSessionResponse> => {
    const response = await apiClient.post<InterviewSessionResponse>('/api/v1/interview/adaptive/start', data);
    return response.data;
  },

  getAdaptiveProfile: async (): Promise<AdaptiveProfileResponse> => {
    const response = await apiClient.get<AdaptiveProfileResponse>('/api/v1/interview/adaptive/profile');
    return response.data;
  },

  getMySessions: async (): Promise<InterviewSessionResponse[]> => {
    const response = await apiClient.get<InterviewSessionResponse[]>('/api/v1/interview/my');
    return response.data;
  },

  getSession: async (id: number): Promise<InterviewSessionResponse> => {
    const response = await apiClient.get<InterviewSessionResponse>(`/api/v1/interview/${id}`);
    return response.data;
  },

  generateReport: async (id: number): Promise<InterviewReportResponse> => {
    const response = await apiClient.post<InterviewReportResponse>(`/api/v1/interview/${id}/report`);
    return response.data;
  },

  getReport: async (id: number): Promise<InterviewReportResponse> => {
    const response = await apiClient.get<InterviewReportResponse>(`/api/v1/interview/${id}/report`);
    return response.data;
  },

  submitAnswer: async (
    sessionId: number,
    questionId: number,
    data: SubmitAnswerRequest
  ): Promise<InterviewSessionResponse> => {
    const response = await apiClient.post<InterviewSessionResponse>(
      `/api/v1/interview/${sessionId}/answer/${questionId}`,
      data
    );
    return response.data;
  },
};

// ── Skill Gap API ─────────────────────────────────────────────────────────────
export const skillGapApi = {
  analyze: async (resumeId: number, jobDescriptionId: number): Promise<SkillGapAnalysisResponse> => {
    const response = await apiClient.post<SkillGapAnalysisResponse>('/api/v1/skill-gap/analyze', {
      resumeId,
      jobDescriptionId,
    });
    return response.data;
  },

  getLatest: async (resumeId: number, jobDescriptionId: number): Promise<SkillGapAnalysisResponse> => {
    const response = await apiClient.get<SkillGapAnalysisResponse>('/api/v1/skill-gap/latest', {
      params: { resumeId, jobDescriptionId },
    });
    return response.data;
  },
};

// ── Preparation Plan API ──────────────────────────────────────────────────────
export const preparationPlanApi = {
  getCurrent: async (): Promise<PreparationPlanResponse> => {
    const response = await apiClient.get<PreparationPlanResponse>('/api/v1/preparation-plan');
    return response.data;
  },

  generate: async (): Promise<PreparationPlanResponse> => {
    const response = await apiClient.post<PreparationPlanResponse>('/api/v1/preparation-plan/generate');
    return response.data;
  },
};

export default apiClient;
