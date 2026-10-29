ALTER TABLE interview_sessions
    ADD COLUMN adaptive_mode BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE interview_questions
    ADD COLUMN selection_reasons_json LONGTEXT NULL;