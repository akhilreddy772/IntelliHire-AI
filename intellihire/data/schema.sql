-- =============================================================================
-- IntelliHire AI - PostgreSQL / SQL Standard Database Schema
-- Run this script on your PostgreSQL database (e.g. psql -U postgres -d intellihire -f schema.sql)
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role VARCHAR(50) NOT NULL,
    created VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS candidates (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    filename VARCHAR(255),
    score INT,
    years INT,
    top_job VARCHAR(255),
    top_match INT,
    data TEXT NOT NULL,
    created VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS custom_jobs (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    company VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    salary VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    skills TEXT NOT NULL,
    created VARCHAR(50) NOT NULL
);

-- Seed default demo accounts:
-- recruiter: recruiter / recruiter123
-- candidate: candidate / candidate123
INSERT INTO users (id, username, password_hash, role, created)
VALUES 
    ('adm001', 'recruiter', 'scrypt:32768:8:1$n49E10eD8vX49Y1h$3cce19a164b3ef8b725203798bcdeef568973b185ec4315fe2b781dfaa8098c772cb3320c2423ef821ad22fecae0fecfcfe2ab912e75e3c150c18228392cf99a', 'recruiter', NOW()::text),
    ('cnd001', 'candidate', 'scrypt:32768:8:1$V40ZzE2Y8tQ49Y1h$87e7421ce5aa050a4176cf99e5a1b32d1667d4fc4f2bf05cf39f8260b0ec8dc1b066cf87d039860655c65f97bc35ba801a243ca46241a87754eb4be1c706d860', 'candidate', NOW()::text)
ON CONFLICT (username) DO NOTHING;
