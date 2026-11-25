"""Automated test suite for IntelliHire AI Enterprise & Optional Upgrades."""
import io
import json
import os
import sys
import unittest

sys.path.insert(0, os.path.abspath(os.path.dirname(__file__)))

import analyzer as az
import pdf_exporter
import llm_service
from app import app, get_db


class IntelliHireEnterpriseTestCase(unittest.TestCase):
    def setUp(self):
        app.config["TESTING"] = True
        app.config["WTF_CSRF_ENABLED"] = False
        self.client = app.test_client()
        
        sample_path = os.path.join(os.path.dirname(__file__), "sample_resume.txt")
        with open(sample_path, "r", encoding="utf-8") as f:
            self.sample_text = f.read()

        jobs_path = os.path.join(os.path.dirname(__file__), "data", "jobs.json")
        with open(jobs_path, "r", encoding="utf-8") as f:
            self.jobs = json.load(f)

    def test_analyzer_skill_dictionary_count(self):
        """Verify 100+ skills in 6 distinct categories."""
        self.assertGreaterEqual(len(az.ALL_SKILLS), 100)
        self.assertEqual(len(az.SKILLS), 6)

    def test_skill_boundary_matching(self):
        """Test boundary matching handles C++, C#, node.js, CI/CD, .NET accurately."""
        sample = "Proficient in C++, C#, Node.js, and CI/CD pipelines with .NET and Go."
        found = az.find_skills(sample)
        self.assertIn("c++", found)
        self.assertIn("c#", found)
        self.assertIn("node.js", found)
        self.assertIn("ci/cd", found)
        self.assertIn("go", found)

    def test_contact_and_years_extraction(self):
        """Test contact information and years of experience estimation."""
        contact = az.extract_contact(self.sample_text)
        self.assertEqual(contact["name"], "Priya Sharma")
        self.assertEqual(contact["email"], "priya.sharma@email.com")
        self.assertIn("98765", contact["phone"])
        self.assertTrue(contact["linkedin"])
        self.assertTrue(contact["github"])

        years = az.estimate_years(self.sample_text)
        self.assertEqual(years, 3)

    def test_resume_analysis_and_breakdown(self):
        """Test score calculation (0-100), weighted components, and tips generation."""
        res = az.analyze_resume(self.sample_text)
        self.assertIn("score", res)
        self.assertTrue(0 <= res["score"] <= 100)
        for dim in ["Sections", "Skills", "Action Verbs", "Measurable Impact", "Contact Info", "Length"]:
            self.assertIn(dim, res["breakdown"])

    def test_dashboard_and_jobs_route(self):
        """Test GET / and /jobs return 200."""
        r1 = self.client.get("/")
        self.assertEqual(r1.status_code, 200)
        r2 = self.client.get("/jobs")
        self.assertEqual(r2.status_code, 200)

    def test_auth_login_and_logout(self):
        """Test authentication flow for recruiter and candidate."""
        # 1. Login with seeded recruiter account
        resp = self.client.post("/login", data={"username": "recruiter", "password": "recruiter123"}, follow_redirects=True)
        self.assertEqual(resp.status_code, 200)
        self.assertIn(b"recruiter", resp.data.lower())
        
        # 2. Logout
        logout_resp = self.client.get("/logout", follow_redirects=True)
        self.assertEqual(logout_resp.status_code, 200)

    def test_auth_register(self):
        """Test registering a new candidate account."""
        import uuid
        uname = f"talent_{uuid.uuid4().hex[:6]}"
        resp = self.client.post(
            "/register",
            data={"username": uname, "password": "password123", "role": "candidate"},
            follow_redirects=True
        )
        self.assertEqual(resp.status_code, 200)
        self.assertIn(uname.encode("utf-8"), resp.data)

    def test_custom_job_requisition_creation(self):
        """Test upgrade: adding a custom job requisition."""
        resp = self.client.post(
            "/custom-job",
            data={
                "title": "Lead MLOps Architect",
                "company": "NextGen Systems",
                "location": "Bengaluru",
                "type": "Full-time",
                "salary": "35-50 LPA",
                "description": "Architect enterprise machine learning workflows with Docker, Kubernetes, AWS, and PyTorch.",
                "skills": "python, kubernetes, docker, aws, pytorch"
            },
            follow_redirects=True
        )
        self.assertEqual(resp.status_code, 200)
        self.assertIn(b"Lead MLOps Architect", resp.data)

    def test_bulk_resume_upload(self):
        """Test upgrade: bulk multi-resume upload and leaderboard ranking."""
        file1 = (io.BytesIO(self.sample_text.encode("utf-8")), "priya_resume.txt")
        resume2_text = """Alex Johnson
alex.j@email.com | +1 555-0199 | linkedin.com/in/alexj | github.com/alexj

SUMMARY
Full stack web developer with 4 years of experience building modern web apps.

EXPERIENCE
Full Stack Developer, WebCorp (2021 - 2025)
- Developed responsive web applications using React, Node.js, and TypeScript.
- Architected MongoDB database schemas serving 10000+ active users.
- Automated deployment with Docker and GitHub Actions, improving release cycle by 30%.

SKILLS
JavaScript, TypeScript, React, Node.js, HTML, CSS, MongoDB, Docker, Git, CI/CD, Agile

EDUCATION
B.S. in Software Engineering, 2021
"""
        file2 = (io.BytesIO(resume2_text.encode("utf-8")), "alex_resume.txt")

        resp = self.client.post(
            "/bulk-upload",
            data={"resumes": [file1, file2]},
            content_type="multipart/form-data",
            follow_redirects=True
        )
        self.assertEqual(resp.status_code, 200)
        self.assertIn(b"Batch Evaluation Leaderboard", resp.data)
        self.assertIn(b"Priya Sharma", resp.data)
        self.assertIn(b"Alex Johnson", resp.data)

    def test_candidate_comparison(self):
        """Test upgrade: side-by-side resume comparison."""
        # Create two candidates
        self.client.post("/analyze", data={"text": self.sample_text}, follow_redirects=True)
        resume2 = self.sample_text.replace("Priya Sharma", "Rohan Mehta").replace("priya.sharma@email.com", "rohan@email.com")
        self.client.post("/analyze", data={"text": resume2}, follow_redirects=True)

        with app.app_context():
            db_conn = get_db()
            rows = db_conn.execute("SELECT id FROM candidates ORDER BY created DESC LIMIT 2").fetchall()
            c1_id = rows[0]["id"]
            c2_id = rows[1]["id"]

        resp = self.client.post("/compare", data={"candidate1": c1_id, "candidate2": c2_id}, follow_redirects=True)
        self.assertEqual(resp.status_code, 200)
        self.assertIn(b"Candidate Comparison", resp.data)
        self.assertIn(b"Comparative Dimension Radar Breakdown", resp.data)

    def test_pdf_export_generation(self):
        """Test upgrade: pure-Python PDF report generation."""
        # Create a candidate
        self.client.post("/analyze", data={"text": self.sample_text}, follow_redirects=True)
        with app.app_context():
            db_conn = get_db()
            row = db_conn.execute("SELECT id FROM candidates ORDER BY created DESC LIMIT 1").fetchone()
            cid = row["id"]

        resp = self.client.get(f"/export-pdf/{cid}")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.content_type, "application/pdf")
        self.assertTrue(resp.data.startswith(b"%PDF-1.4"))
        self.assertIn(b"IntelliHire AI", resp.data)

    def test_llm_intelligence_service(self):
        """Test upgrade: LLM interview question and upskilling generation."""
        insights = llm_service.generate_llm_insights(
            candidate_name="Priya Sharma",
            skills=["python", "machine learning", "docker"],
            score=88,
            top_job="Python Backend Developer",
            experience_years=3,
            tips=[]
        )
        self.assertIn("summary", insights)
        self.assertIn("interview_questions", insights)
        self.assertIn("upskill_recommendations", insights)
        self.assertEqual(len(insights["interview_questions"]), 3)


if __name__ == "__main__":
    unittest.main()
