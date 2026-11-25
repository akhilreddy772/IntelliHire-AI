-- ============================================================
-- IntelliHire AI — MySQL Schema
-- Run this in MySQL Workbench (or via CLI) BEFORE starting the app.
-- ============================================================

-- 1. Create the database (skip if it already exists)
CREATE DATABASE IF NOT EXISTS intellihire
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE intellihire;

-- ── users ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(64)  PRIMARY KEY,
    username      VARCHAR(100) UNIQUE NOT NULL,
    password_hash TEXT         NOT NULL,
    role          VARCHAR(50)  NOT NULL   COMMENT 'recruiter | candidate',
    created       VARCHAR(50)  NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── candidates ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS candidates (
    id        VARCHAR(64)  PRIMARY KEY,
    name      VARCHAR(255) NOT NULL,
    email     VARCHAR(255),
    filename  VARCHAR(255),
    score     INT,
    years     INT,
    top_job   VARCHAR(255),
    top_match INT,
    data      TEXT         NOT NULL  COMMENT 'Full JSON analysis blob',
    created   VARCHAR(50)  NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── custom_jobs ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS custom_jobs (
    id          VARCHAR(64)  PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    company     VARCHAR(255) NOT NULL,
    location    VARCHAR(255) NOT NULL,
    type        VARCHAR(50)  NOT NULL,
    salary      VARCHAR(100) NOT NULL,
    description TEXT         NOT NULL,
    skills      TEXT         NOT NULL  COMMENT 'Comma-separated skill list',
    created     VARCHAR(50)  NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Seed demo accounts ───────────────────────────────────────
-- Note: The Flask app auto-seeds these on first run.
-- Only use the INSERT statements below if you need to pre-populate
-- via MySQL Workbench without starting the Flask app first.
-- Password hashes below are for:
--   recruiter → recruiter123
--   candidate → candidate123
--
-- INSERT INTO users (id, username, password_hash, role, created) VALUES
-- ('rec00001', 'recruiter',
--  'pbkdf2:sha256:260000$...paste werkzeug hash here...',
--  'recruiter', NOW()),
-- ('can00001', 'candidate',
--  'pbkdf2:sha256:260000$...paste werkzeug hash here...',
--  'candidate', NOW());
