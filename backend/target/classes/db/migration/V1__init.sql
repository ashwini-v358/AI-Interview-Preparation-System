-- Phase 1 Initial Migration: Setup and schema verification
CREATE TABLE IF NOT EXISTS schema_init_check (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    initialized_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    environment_name VARCHAR(100) NOT NULL,
    description VARCHAR(255)
);

INSERT INTO schema_init_check (environment_name, description)
VALUES ('SYSTEM', 'Smart Interview Platform Phase 1 Database Migration Initialized');
