/**
 * TypeScript Type Definitions for Smart Interview Platform
 */

export interface HealthResponse {
  status: string;
  application: string;
  version: string;
  timestamp: string;
  details: {
    activeProfiles?: string[];
    javaVersion?: string;
    osName?: string;
    [key: string]: unknown;
  };
}

export type UserRole = 'ROLE_CANDIDATE' | 'ROLE_ADMIN';

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: UserRole;
  enabled: boolean;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  userId: number;
  email: string;
  fullName: string;
  role: UserRole;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export type SkillMatchType = 'MATCHED' | 'PARTIAL' | 'NOT_IDENTIFIED';

export interface SkillGapItem {
  skillName: string;
  category: string;
  matchType: SkillMatchType;
  resumeEvidence?: string;
  jdRequirement?: string;
  suggestedPreparation?: string;
}

export interface SkillGapAnalysisItem {
  skillName: string;
  resumeEvidence: string | null;
  jdRequirement: string;
  suggestedPreparation: string | null;
}

export interface SkillGapAnalysisResponse {
  analysisId: number;
  resumeId: number;
  resumeFileName: string;
  jobDescriptionId: number;
  jobTitle: string;
  matchPercentage: number;
  matchedSkills: SkillGapAnalysisItem[];
  partialSkills: SkillGapAnalysisItem[];
  missingSkills: SkillGapAnalysisItem[];
  recommendations: string[];
  analyzedAt: string;
}

export interface PreparationPlanSkillArea {
  skillName: string;
  currentStatus: 'WEAK' | 'MISSING';
  priority: 'HIGH' | 'MEDIUM';
  estimatedDays: number;
  estimatedHours: number;
  recommendedTopics: string[];
  practiceTasks: string[];
  interviewSuggestions: string[];
}

export interface PreparationPlanResponse {
  planId: number;
  title: string;
  durationDays: number;
  generatedAt: string;
  skillGapAnalysisId: number;
  resumeFileName: string;
  jobTitle: string;
  matchPercentage: number;
  strongSkills: string[];
  skillAreas: PreparationPlanSkillArea[];
}

export type InterviewType = 'TECHNICAL' | 'RESUME_BASED' | 'JD_BASED' | 'COMPREHENSIVE_ADAPTIVE';

export type InterviewState =
  | 'CREATED'
  | 'WAITING'
  | 'IN_PROGRESS'
  | 'PAUSED'
  | 'COMPLETED'
  | 'ABANDONED';

export interface PhaseInfo {
  phase: number;
  title: string;
  description: string;
  status: 'COMPLETED' | 'IN_PROGRESS' | 'PENDING';
  badge: string;
}

export interface SchemaStatusResponse {
  databaseEngine: string;
  jpaInitialized: boolean;
  totalUsers: number;
  totalResumes: number;
  totalJobDescriptions: number;
  totalSkills: number;
  totalInterviewSessions: number;
  entityCounts: Record<string, number>;
  timestamp: string;
  phase: string;
  message: string;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp: string;
  fieldErrors?: Record<string, string>;
}

export interface ResumeUploadResponse {
  resumeId: number;
  originalFileName: string;
  fileSizeBytes: number;
  extractedTextLength: number;
  uploadedAt: string;
  analysisId: number;
  extractedSkills: string[];
  experienceYears: number;
  summary: string;
  domains: string[];
  educationDetails: string[];
  aiProvider: string;
  processingTimeMs: number;
  message: string;
}

export interface ResumeSummaryResponse {
  resumeId: number;
  originalFileName: string;
  fileSizeBytes: number;
  uploadedAt: string;
  hasAnalysis: boolean;
  topSkills: string[];
  experienceYears: number;
  summary: string | null;
}

// ── Job Description ──────────────────────────────────────────────────────────

export interface JdUploadRequest {
  title: string;
  companyName?: string;
  rawText: string;
}

export interface JdUploadResponse {
  jdId: number;
  title: string;
  companyName?: string;
  rawTextLength: number;
  uploadedAt: string;
  analysisId: number;
  roleTitle: string;
  requiredSkills: string[];
  niceToHaveSkills: string[];
  minimumExperienceYears: number;
  domains: string[];
  aiProvider: string;
  processingTimeMs: number;
  message: string;
}

export interface JdSummaryResponse {
  jdId: number;
  title: string;
  companyName?: string;
  uploadedAt: string;
  hasAnalysis: boolean;
  topRequiredSkills: string[];
  minimumExperienceYears: number;
  roleTitle?: string;
}

// ── Interview Session ─────────────────────────────────────────────────────────

export interface StartInterviewRequest {
  sessionTitle: string;
  targetRole?: string;
  resumeId?: number;
  jobDescriptionId?: number;
  interviewType?: string;
  targetQuestionCount?: number;
  difficulty?: string;
}

export interface QuestionDto {
  questionId: number;
  questionOrder: number;
  questionText: string;
  category: string;
  targetSkill: string;
  difficultyLevel: string;
  selectionReasons: AdaptiveReason[];
}

export interface AdaptiveReason {
  category: 'SKILL_GAP' | 'PREVIOUS_MISTAKE' | 'TARGET_ROLE_REQUIREMENT' | 'PREPARATION_PLAN' | 'LOW_PERFORMANCE' | 'DIFFICULTY_PROGRESSION' | string;
  description: string;
}

export interface SkillPerformance {
  skillName: string;
  scorePercent: number;
  technicalAccuracyPercent: number;
  completenessPercent: number;
  clarityPercent: number;
  communicationPercent: number | null;
  level: 'WEAK' | 'MODERATE' | 'STRONG';
  previousMistakes: number;
}

export interface InterviewTrend {
  sessionId: number;
  sessionTitle: string;
  completedAt: string;
  overallScorePercent: number;
  skillScores: SkillPerformance[];
}

export interface AdaptiveProfileResponse {
  hasHistory: boolean;
  completedInterviewCount: number;
  suggestedDifficulty: 'EASY' | 'MEDIUM' | 'HARD';
  skillProfile: SkillPerformance[];
  history: InterviewTrend[];
}

export interface QuestionAnswerDto {
  questionId: number;
  questionOrder: number;
  questionText: string;
  difficultyLevel: string;
  answerId?: number;
  answerText?: string;
  timeTakenSeconds?: number;
  evaluationId?: number;
  technicalAccuracyScore?: number;
  completenessScore?: number;
  clarityScore?: number;
  overallScore?: number;
  strengths?: string;
  areasOfImprovement?: string;
  detailedFeedback?: string;
  relevanceScore?: number;
  communicationScore?: number;
  depthScore?: number;
  answeredQuestion?: boolean;
  followUpNeeded?: boolean;
  strengthsList: string[];
  missingPoints: string[];
  feedback?: string;
  suggestedImprovement?: string;
  followUpTopic?: string;
  selectionReasons: AdaptiveReason[];
}

export interface InterviewSessionResponse {
  sessionId: number;
  sessionTitle: string;
  targetRole?: string;
  interviewType: string;
  status: string;
  adaptiveMode: boolean;
  currentDifficulty: string;
  targetQuestionCount: number;
  completedQuestionCount: number;
  startedAt?: string;
  completedAt?: string;
  createdAt: string;
  currentQuestion?: QuestionDto;
  history: QuestionAnswerDto[];
  resumeId?: number;
  jobDescriptionId?: number;
}

export interface InterviewReportResponse {
  reportId: number;
  sessionId: number;
  overallScore: number;
  technicalScore: number;
  communicationScore: number;
  problemSolvingScore: number;
  projectKnowledgeScore: number;
  answerQualityScore: number;
  executiveSummary: string;
  strengths: string[];
  areasToImprove: string[];
  strugglingQuestionOrders: number[];
  recommendedTopics: string[];
  improvementSuggestions: string[];
  claimVerifications: string[];
  generatedAt: string;
}

export interface SubmitAnswerRequest {
  answerText: string;
  timeTakenSeconds?: number;
}
