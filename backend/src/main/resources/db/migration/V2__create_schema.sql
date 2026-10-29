-- Phase 2 Database Schema Migration
-- Defines all relational tables, foreign key constraints, and performance indexes.

-- 1. Users
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'ROLE_CANDIDATE',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_user_email ON users(email);

-- 2. Resumes
CREATE TABLE IF NOT EXISTS resumes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),
    raw_text LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resume_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_resume_user_id ON resumes(user_id);

-- 3. Resume Structured AI Analysis
CREATE TABLE IF NOT EXISTS resume_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id BIGINT NOT NULL UNIQUE,
    extracted_skills_json LONGTEXT,
    experience_years DOUBLE,
    summary TEXT,
    domains_json TEXT,
    education_json TEXT,
    raw_ai_response LONGTEXT,
    analyzed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resume_analysis_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE
);
CREATE INDEX idx_resume_analysis_resume_id ON resume_analysis(resume_id);

-- 4. Job Descriptions
CREATE TABLE IF NOT EXISTS job_descriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    company_name VARCHAR(150),
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    raw_text LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_jd_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_jd_user_id ON job_descriptions(user_id);

-- 5. Job Description Structured AI Analysis
CREATE TABLE IF NOT EXISTS job_description_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_description_id BIGINT NOT NULL UNIQUE,
    role_title VARCHAR(200),
    required_skills_json LONGTEXT,
    nice_to_have_skills_json LONGTEXT,
    minimum_experience_years DOUBLE,
    role_summary TEXT,
    raw_ai_response LONGTEXT,
    analyzed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_jd_analysis_jd FOREIGN KEY (job_description_id) REFERENCES job_descriptions(id) ON DELETE CASCADE
);
CREATE INDEX idx_jd_analysis_jd_id ON job_description_analysis(job_description_id);

-- 6. Skills Catalog
CREATE TABLE IF NOT EXISTS skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    normalized_name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_skill_normalized_name ON skills(normalized_name);
CREATE INDEX idx_skill_category ON skills(category);

-- 7. Candidate Skills (Resume to Skill link)
CREATE TABLE IF NOT EXISTS candidate_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    proficiency_level VARCHAR(30) DEFAULT 'INTERMEDIATE',
    years_of_experience DOUBLE,
    context_snippet VARCHAR(500),
    CONSTRAINT fk_cand_skill_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE,
    CONSTRAINT fk_cand_skill_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
);
CREATE INDEX idx_cand_skill_resume ON candidate_skills(resume_id);
CREATE INDEX idx_cand_skill_skill ON candidate_skills(skill_id);

-- 8. Job Required Skills (JD to Skill link)
CREATE TABLE IF NOT EXISTS job_required_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_description_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    importance VARCHAR(30) NOT NULL DEFAULT 'REQUIRED',
    min_years_experience DOUBLE,
    CONSTRAINT fk_jd_req_skill_jd FOREIGN KEY (job_description_id) REFERENCES job_descriptions(id) ON DELETE CASCADE,
    CONSTRAINT fk_jd_req_skill_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
);
CREATE INDEX idx_jd_req_skill_jd ON job_required_skills(job_description_id);
CREATE INDEX idx_jd_req_skill_skill ON job_required_skills(skill_id);

-- 9. Skill Gap Analysis
CREATE TABLE IF NOT EXISTS skill_gap_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id BIGINT NOT NULL,
    job_description_id BIGINT NOT NULL,
    match_percentage DOUBLE NOT NULL,
    matched_skills_json LONGTEXT,
    partial_skills_json LONGTEXT,
    missing_skills_json LONGTEXT,
    recommendations_json LONGTEXT,
    analyzed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gap_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE,
    CONSTRAINT fk_gap_jd FOREIGN KEY (job_description_id) REFERENCES job_descriptions(id) ON DELETE CASCADE
);
CREATE INDEX idx_skill_gap_resume_jd ON skill_gap_analysis(resume_id, job_description_id);

-- 10. Interview Sessions
CREATE TABLE IF NOT EXISTS interview_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    resume_id BIGINT NULL,
    job_description_id BIGINT NULL,
    session_title VARCHAR(200) NOT NULL,
    interview_type VARCHAR(40) NOT NULL DEFAULT 'COMPREHENSIVE_ADAPTIVE',
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    current_difficulty VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    target_question_count INT NOT NULL DEFAULT 5,
    completed_question_count INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_session_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE SET NULL,
    CONSTRAINT fk_session_jd FOREIGN KEY (job_description_id) REFERENCES job_descriptions(id) ON DELETE SET NULL
);
CREATE INDEX idx_interview_user_id ON interview_sessions(user_id);
CREATE INDEX idx_interview_status ON interview_sessions(status);

-- 11. Interview Questions
CREATE TABLE IF NOT EXISTS interview_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    interview_session_id BIGINT NOT NULL,
    question_order INT NOT NULL,
    question_text TEXT NOT NULL,
    category VARCHAR(100),
    target_skill VARCHAR(100),
    difficulty_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    expected_key_points_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_question_session FOREIGN KEY (interview_session_id) REFERENCES interview_sessions(id) ON DELETE CASCADE
);
CREATE INDEX idx_question_session_id ON interview_questions(interview_session_id);

-- 12. Candidate Answers
CREATE TABLE IF NOT EXISTS candidate_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    interview_question_id BIGINT NOT NULL UNIQUE,
    answer_text LONGTEXT NOT NULL,
    audio_url VARCHAR(500),
    time_taken_seconds INT,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_answer_question FOREIGN KEY (interview_question_id) REFERENCES interview_questions(id) ON DELETE CASCADE
);
CREATE INDEX idx_answer_question_id ON candidate_answers(interview_question_id);

-- 13. Answer Evaluations
CREATE TABLE IF NOT EXISTS answer_evaluations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    candidate_answer_id BIGINT NOT NULL UNIQUE,
    technical_accuracy_score DOUBLE,
    completeness_score DOUBLE,
    clarity_score DOUBLE,
    overall_score DOUBLE NOT NULL,
    strengths TEXT,
    areas_of_improvement TEXT,
    detailed_feedback LONGTEXT,
    follow_up_topic VARCHAR(200),
    evaluated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eval_answer FOREIGN KEY (candidate_answer_id) REFERENCES candidate_answers(id) ON DELETE CASCADE
);
CREATE INDEX idx_eval_answer_id ON answer_evaluations(candidate_answer_id);

-- 14. Interview Reports
CREATE TABLE IF NOT EXISTS interview_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    interview_session_id BIGINT NOT NULL UNIQUE,
    overall_score DOUBLE NOT NULL,
    technical_score DOUBLE,
    communication_score DOUBLE,
    problem_solving_score DOUBLE,
    executive_summary LONGTEXT,
    strengths_json LONGTEXT,
    weaknesses_json LONGTEXT,
    claim_verifications_json LONGTEXT,
    preparation_roadmap_json LONGTEXT,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_session FOREIGN KEY (interview_session_id) REFERENCES interview_sessions(id) ON DELETE CASCADE
);
CREATE INDEX idx_report_session_id ON interview_reports(interview_session_id);

-- 15. Preparation Plans
CREATE TABLE IF NOT EXISTS preparation_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    interview_session_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    plan_content_json LONGTEXT NOT NULL,
    duration_days INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_session FOREIGN KEY (interview_session_id) REFERENCES interview_sessions(id) ON DELETE SET NULL
);
CREATE INDEX idx_plan_user_id ON preparation_plans(user_id);

-- 16. Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100),
    entity_id BIGINT,
    ip_address VARCHAR(50),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);

-- 17. Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    notification_type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_notif_user_unread ON notifications(user_id, is_read);
