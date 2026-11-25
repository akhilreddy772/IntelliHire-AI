# IntelliHire AI Enterprise — Intelligent Resume Analyzer & Job Matching System

[![Python 3.10+](https://img.shields.io/badge/python-3.10+-blue.svg)](https://www.python.org/downloads/)
[![Flask 3.0+](https://img.shields.io/badge/flask-3.0+-green.svg)](https://palletsprojects.com/p/flask/)
[![Docker](https://img.shields.io/badge/docker-ready-blue.svg)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

An enterprise-ready recruitment intelligence platform. **IntelliHire AI** provides full-lifecycle talent evaluation: automated ATS scoring across 6 weighted dimensions, multi-format parsing (PDF, DOCX, TXT), specialized word-boundary skill extraction (100+ skills in 6 domains), side-by-side resume comparison, batch bulk uploading with leaderboard ranking, custom job description matching, branded PDF report generation, and Claude LLM interview question synthesis.

---

## 🌟 Full Feature Matrix (Including All Enterprise Upgrades)

1. **Multi-Format Resume Parsing**
   - Pure-Python text extraction from **PDF** (`pypdf`), **DOCX** (`python-docx`), and **TXT**.
   - Validates file size (up to 32 MB for bulk batch processing), whitelists extensions, and provides friendly errors on scanned/empty documents.

2. **Smart Candidate Extraction**
   - Automatically detects Candidate Name, Email address, Phone number, and LinkedIn / GitHub presence.
   - Calculates estimated years of experience using career year spans and regex heuristics.

3. **100+ Skill Extraction Engine (6 Categories)**
   - Regex-based word boundary matcher that reliably isolates tricky tech tokens like `C++`, `C#`, `Node.js`, `CI/CD`, `.NET`, `Go`, `SQL`, `AWS`, etc.
   - Six structured categories: *Programming*, *Web*, *Data & AI*, *Database*, *Cloud & DevOps*, and *Soft Skills*.

4. **6-Section ATS Architecture Verification**
   - Verifies standard resume sections: *Summary*, *Experience*, *Education*, *Skills*, *Projects*, and *Certifications*.

5. **Weighted ATS Resume Scoring (0–100)**
   - **Skills Count & Depth (25%)**
   - **Section Completeness (20%)**
   - **Action Verbs (15%)** (e.g., *spearheaded, architected, optimized, automated*)
   - **Measurable Impact & Metrics (15%)** (e.g., percentages, scale metrics)
   - **Resume Length & Density (15%)**
   - **Contact Information Quality (10%)**

6. **Targeted Improvement Recommendations**
   - Provides clear, actionable steps for missing sections, weak verbs, missing metrics, and contact details.

7. **Hybrid Job Matching Algorithm**
   - Matches candidates against seeded positions and custom recruiter requisitions.
   - Formula: **`70% Skill Overlap + 30% Pure-Python TF-IDF Cosine Similarity`**.
   - Highlights matched skills in green (`✓`) and missing required skills in red (`✗ missing`).

8. **Recruiter Dashboard & Analytics**
   - 4 KPI stat cards (Total Candidates, Average Resume Score, Top Match %, Active Jobs).
   - Chart.js score distribution histogram and top skills bar chart.
   - Candidate management table with live search links, score indicators, and deletion.

9. **RESTful JSON API**
   - `GET /api/candidates` for integration with external ATS and recruitment pipelines.

---

### 🚀 All Optional Upgrades Included:

- **1. User Authentication & Roles (`/login`, `/register`)**:
  - Secure password hashing (`werkzeug.security`) with Recruiter and Candidate roles.
  - Pre-seeded demo accounts:
    - 👔 **Recruiter**: `recruiter` / `recruiter123`
    - 🎓 **Candidate**: `candidate` / `candidate123`
- **2. Custom Job Description Matcher (`/custom-job`)**:
  - Upload or paste custom job descriptions with automatic skill extraction.
  - Custom requisitions are instantly fed into the candidate matching engine.
- **3. Side-by-Side Resume Comparison (`/compare`)**:
  - Select any two candidates to view radar overlay charts, shared skills, and unique capability gaps.
- **4. Branded PDF Report Export (`/export-pdf/<cid>`)**:
  - Built-in pure-Python PDF generator (`pdf_exporter.py`).
  - Downloads a professional single-page recruiter assessment report.
- **5. LLM-Powered Feedback & Interview Intelligence**:
  - Direct integration with Anthropic Claude API (`claude-3-haiku-20240307`) via `ANTHROPIC_API_KEY`.
  - Built-in AI recruiter heuristic fallback generates tailored technical questions and upskilling advice if no API key is set.
- **6. Docker & Container Deployment**:
  - Production-ready `Dockerfile`, `docker-compose.yml`, and `.dockerignore`.
- **7. Semantic Pure-Python TF-IDF & Cosine Similarity Engine**:
  - Instant cosine similarity matching with document frequency weighting without heavy ML downloads.
- **8. Bulk Multi-Resume Upload & Leaderboard Ranking (`/bulk-upload`)**:
  - Upload 2 to 50+ resumes at once.
  - Automatically parses, scores, and displays a ranked candidate leaderboard.

---

## 📂 Project Structure

```
intellihire/
├── app.py               # Enterprise Flask application, routes, auth & SQLite DB
├── analyzer.py          # Parsing, regex skill dictionary, TF-IDF cosine similarity & scoring
├── pdf_exporter.py      # Pure-Python PDF generation engine (no C dependencies required)
├── llm_service.py       # Anthropic Claude API & heuristic interview question engine
├── test_suite.py        # Automated test suite (12 unit & integration tests)
├── Dockerfile           # Docker container configuration
├── docker-compose.yml   # Multi-container orchestration
├── .dockerignore        # Docker build ignore rules
├── data/
│   └── jobs.json        # 10 seeded job requisitions with required skills
├── templates/
│   ├── base.html        # Main layout with sidebar navigation & role indicator
│   ├── dashboard.html   # Recruiter KPIs, distribution charts & candidate table
│   ├── analyze.html     # Drag-and-drop file upload zone & paste text box
│   ├── result.html      # Score ring, radar chart, LLM interview intelligence & PDF download
│   ├── jobs.html        # Grid of seeded and custom job openings
│   ├── custom_job.html  # Post custom job description form
│   ├── compare.html     # Candidate selection for comparison
│   ├── compare_result.html # Side-by-side radar overlay and skills diff
│   ├── bulk_upload.html # Batch upload multiple resumes
│   ├── bulk_results.html# Ranked leaderboard for batch uploads
│   ├── login.html       # Authentication sign in
│   └── register.html    # User registration
├── static/
│   └── style.css        # Responsive styles, gradients, modern dark mode
├── uploads/
│   └── .gitkeep         # Uploaded resume storage
├── sample_resume.txt    # Sample resume for quick testing
├── requirements.txt     # Minimal dependencies (Flask, pypdf, python-docx)
├── .gitignore           # Git ignore rules
└── README.md            # Documentation and setup instructions
```

---

## ⚡ Quickstart Guide

### 1. Install & Run Locally
```bash
# Navigate to project directory
cd intellihire

# Create and activate virtual environment
python -m venv venv
.\venv\Scripts\Activate.ps1   # Windows PowerShell (or source venv/bin/activate on Linux/macOS)

# Install required dependencies
pip install -r requirements.txt

# Start the Flask development server
python app.py
```
Open **`http://127.0.0.1:5000`** in your browser.

### 2. Optional: Enable Live Claude API Insights
Set your Anthropic API key in your environment:
```bash
export ANTHROPIC_API_KEY="sk-ant-api03-..."     # Linux/macOS
$env:ANTHROPIC_API_KEY="sk-ant-api03-..."      # Windows PowerShell
```
*(If omitted, IntelliHire AI automatically uses its built-in recruiter heuristic intelligence).*

### 3. Run with Docker Compose
```bash
docker-compose up --build
```
Navigate to **`http://localhost:5000`**.

---

## 🧪 Automated Testing

Run the automated test suite:
```bash
python test_suite.py
```
Expected output:
```
............
----------------------------------------------------------------------
Ran 12 tests in 0.73s

OK
```

---

## 🐙 Push to GitHub

```bash
git init
git add .
git commit -m "feat: complete IntelliHire AI enterprise with all optional upgrades"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/intellihire-ai.git
git push -u origin main
```
