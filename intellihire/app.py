"""IntelliHire AI - Flask application with universal SQL database integration."""
import json
import os
import uuid
from datetime import datetime
from functools import wraps
from flask import (
    Flask,
    render_template,
    request,
    redirect,
    url_for,
    flash,
    jsonify,
    g,
    session,
    Response
)
from werkzeug.utils import secure_filename
from werkzeug.security import generate_password_hash, check_password_hash

import analyzer as az
import database as db_mod
import pdf_exporter
import llm_service

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
UPLOADS_DIR = os.path.join(BASE_DIR, "uploads")
DATA_DIR = os.path.join(BASE_DIR, "data")
JOBS_PATH = os.path.join(DATA_DIR, "jobs.json")
ALLOWED_EXTENSIONS = {"pdf", "docx", "txt"}

app = Flask(__name__)
app.secret_key = os.environ.get("SECRET_KEY", "intellihire-production-secret-key-2026")
app.config["MAX_CONTENT_LENGTH"] = 32 * 1024 * 1024

os.makedirs(UPLOADS_DIR, exist_ok=True)
os.makedirs(DATA_DIR, exist_ok=True)

with open(JOBS_PATH, "r", encoding="utf-8") as f:
    SEEDED_JOBS = json.load(f)


def get_db():
    """Retrieve or create request-scoped universal database connection."""
    if "db" not in g:
        g.db = db_mod.get_db_connection(DATA_DIR)
    return g.db


@app.teardown_appcontext
def close_db(exception=None):
    """Close request database handle."""
    db_conn = g.pop("db", None)
    if db_conn is not None:
        db_conn.close()


def get_all_jobs():
    """Retrieve combined list of seeded positions + custom recruiter jobs."""
    db_conn = get_db()
    cur = db_conn.execute("SELECT * FROM custom_jobs ORDER BY created DESC")
    custom_rows = cur.fetchall()
    custom_list = []
    for r in custom_rows:
        try:
            skills = json.loads(r["skills"])
        except Exception:
            skills = [s.strip().lower() for s in r["skills"].split(",") if s.strip()]
        custom_list.append({
            "id": r["id"],
            "title": r["title"],
            "company": r["company"],
            "location": r["location"],
            "type": r["type"],
            "salary": r["salary"],
            "description": r["description"],
            "skills": skills,
            "custom": True
        })
    return custom_list + SEEDED_JOBS


def login_required(role_needed=None):
    """Decorator to enforce login and optional role requirement."""
    def decorator(f):
        @wraps(f)
        def decorated_function(*args, **kwargs):
            if "user_id" not in session:
                flash("Please sign in to access this page.", "error")
                return redirect(url_for("login", next=request.url))
            if role_needed and session.get("user_role") != role_needed and session.get("user_role") != "recruiter":
                flash(f"Access restricted to {role_needed} role.", "error")
                return redirect(url_for("dashboard"))
            return f(*args, **kwargs)
        return decorated_function
    return decorator


@app.context_processor
def inject_user():
    """Make current user and role accessible to all Jinja templates."""
    db_type = "PostgreSQL" if getattr(g.get("db"), "is_pg", False) else "SQLite"
    return {
        "current_user": session.get("username"),
        "current_role": session.get("user_role"),
        "db_engine": db_type
    }


@app.errorhandler(413)
def request_entity_too_large(error):
    flash("The uploaded file exceeds the allowed upload limit. Please upload a smaller file.", "error")
    return redirect(url_for("analyze")), 413


# =============================================================================
# Authentication Routes
# =============================================================================

@app.route("/login", methods=["GET", "POST"])
def login():
    if request.method == "POST":
        username = request.form.get("username", "").strip()
        password = request.form.get("password", "")
        db_conn = get_db()
        cur = db_conn.execute("SELECT * FROM users WHERE username = ?", (username,))
        user = cur.fetchone()
        
        if user and check_password_hash(user["password_hash"], password):
            session["user_id"] = user["id"]
            session["username"] = user["username"]
            session["user_role"] = user["role"]
            flash(f"Welcome back, {username}! Signed in as {user['role'].title()}.", "ok")
            next_page = request.args.get("next")
            return redirect(next_page or url_for("dashboard"))
        
        flash("Invalid username or password.", "error")
    return render_template("login.html", page="login")


@app.route("/register", methods=["GET", "POST"])
def register():
    if request.method == "POST":
        username = request.form.get("username", "").strip()
        password = request.form.get("password", "")
        role = request.form.get("role", "candidate")
        
        if not username or not password:
            flash("Username and password are required.", "error")
            return redirect(url_for("register"))
            
        if role not in ("candidate", "recruiter"):
            role = "candidate"
            
        db_conn = get_db()
        cur = db_conn.execute("SELECT id FROM users WHERE username = ?", (username,))
        existing = cur.fetchone()
        if existing:
            flash("Username already taken. Please choose another.", "error")
            return redirect(url_for("register"))
            
        uid = uuid.uuid4().hex[:8]
        now = datetime.now().strftime("%Y-%m-%d %H:%M")
        db_conn.execute(
            "INSERT INTO users VALUES (?, ?, ?, ?, ?)",
            (uid, username, generate_password_hash(password), role, now)
        )
        db_conn.commit()
        
        session["user_id"] = uid
        session["username"] = username
        session["user_role"] = role
        flash(f"Account created successfully! Welcome, {username}.", "ok")
        return redirect(url_for("dashboard"))
        
    return render_template("register.html", page="register")


@app.route("/logout")
def logout():
    session.clear()
    flash("You have been signed out.", "ok")
    return redirect(url_for("dashboard"))


# =============================================================================
# Dashboard & Core App Routes
# =============================================================================

@app.route("/")
def dashboard():
    db_conn = get_db()
    cur = db_conn.execute("SELECT * FROM candidates ORDER BY created DESC")
    rows = cur.fetchall()
    total = len(rows)
    avg_score = round(sum(r["score"] for r in rows) / total) if total else 0
    best_match = max((r["top_match"] for r in rows), default=0)

    skill_counter = {}
    score_buckets = {"0-39": 0, "40-59": 0, "60-79": 0, "80-100": 0}

    for r in rows:
        try:
            d = json.loads(r["data"])
            candidate_skills = d.get("analysis", {}).get("skills", {})
            for s in candidate_skills:
                skill_counter[s] = skill_counter.get(s, 0) + 1
        except Exception:
            pass

        sc = r["score"]
        if sc < 40:
            score_buckets["0-39"] += 1
        elif sc < 60:
            score_buckets["40-59"] += 1
        elif sc < 80:
            score_buckets["60-79"] += 1
        else:
            score_buckets["80-100"] += 1

    top_skills = sorted(skill_counter.items(), key=lambda x: -x[1])[:10]
    all_jobs = get_all_jobs()

    return render_template(
        "dashboard.html",
        rows=rows,
        total=total,
        avg=avg_score,
        best=best_match,
        top_skills=top_skills,
        buckets=score_buckets,
        jobs=len(all_jobs),
        page="dashboard"
    )


def process_single_resume(text, label):
    all_jobs = get_all_jobs()
    analysis = az.analyze_resume(text)
    job_matches = az.match_jobs(text, analysis["skills"], all_jobs)
    
    cid = uuid.uuid4().hex[:10]
    contact = analysis["contact"]
    top_job_title = job_matches[0]["title"] if job_matches else "N/A"
    top_job_match = job_matches[0]["match"] if job_matches else 0

    record_payload = {
        "analysis": analysis,
        "matches": job_matches,
        "extracted_sample": text[:600]
    }

    db_conn = get_db()
    db_conn.execute(
        "INSERT INTO candidates (id, name, email, filename, score, years, top_job, top_match, data, created) VALUES (?,?,?,?,?,?,?,?,?,?)",
        (
            cid,
            contact["name"],
            contact["email"],
            label,
            analysis["score"],
            analysis["years"],
            top_job_title,
            top_job_match,
            json.dumps(record_payload),
            datetime.now().strftime("%Y-%m-%d %H:%M")
        )
    )
    db_conn.commit()
    return cid, analysis, job_matches


@app.route("/analyze", methods=["GET", "POST"])
def analyze():
    if request.method == "GET":
        return render_template("analyze.html", page="analyze")

    uploaded_file = request.files.get("resume")
    pasted_text = request.form.get("text", "").strip()

    has_file = uploaded_file and uploaded_file.filename and uploaded_file.filename.strip() != ""
    if not has_file and not pasted_text:
        flash("Please upload a resume file or paste resume text to begin.", "error")
        return redirect(url_for("analyze"))

    if has_file:
        filename = secure_filename(uploaded_file.filename)
        ext = filename.rsplit(".", 1)[-1].lower() if "." in filename else ""
        if ext not in ALLOWED_EXTENSIONS:
            flash("Unsupported file format. Please upload a PDF, DOCX, or TXT resume.", "error")
            return redirect(url_for("analyze"))

        unique_filename = f"{uuid.uuid4().hex[:8]}_{filename}"
        saved_path = os.path.join(UPLOADS_DIR, unique_filename)
        uploaded_file.save(saved_path)

        try:
            extracted = az.extract_text(saved_path, unique_filename)
        except Exception as e:
            flash(f"Error parsing resume file: {str(e)}. Try pasting plain text.", "error")
            return redirect(url_for("analyze"))
        label = filename
    else:
        extracted = pasted_text
        label = "Pasted Text Resume"

    if len(extracted.split()) < 30:
        flash("Not enough text detected in resume (minimum 30 words required). If you uploaded a scanned image PDF, please provide a text-based resume.", "error")
        return redirect(url_for("analyze"))

    cid, _, _ = process_single_resume(extracted, label)
    return redirect(url_for("result", cid=cid))


@app.route("/result/<cid>")
def result(cid):
    db_conn = get_db()
    cur = db_conn.execute("SELECT * FROM candidates WHERE id=?", (cid,))
    row = cur.fetchone()
    if not row:
        flash("Candidate evaluation not found.", "error")
        return redirect(url_for("dashboard"))

    data = json.loads(row["data"])
    analysis = data["analysis"]
    
    llm_insights = llm_service.generate_llm_insights(
        candidate_name=row["name"],
        skills=list(analysis.get("skills", {}).keys()),
        score=row["score"],
        top_job=row["top_job"],
        experience_years=row["years"],
        tips=analysis.get("tips", [])
    )

    return render_template(
        "result.html",
        r=row,
        a=analysis,
        matches=data["matches"],
        llm=llm_insights,
        page=""
    )


@app.route("/export-pdf/<cid>")
def export_pdf(cid):
    db_conn = get_db()
    cur = db_conn.execute("SELECT * FROM candidates WHERE id=?", (cid,))
    row = cur.fetchone()
    if not row:
        flash("Candidate not found.", "error")
        return redirect(url_for("dashboard"))

    data = json.loads(row["data"])
    pdf_bytes = pdf_exporter.generate_candidate_pdf(row, data)
    
    filename = f"IntelliHire_Evaluation_{secure_filename(row['name'])}_{cid}.pdf"
    return Response(
        pdf_bytes,
        mimetype="application/pdf",
        headers={"Content-Disposition": f"attachment; filename={filename}"}
    )


@app.route("/bulk-upload", methods=["GET", "POST"])
def bulk_upload():
    if request.method == "POST":
        files = request.files.getlist("resumes")
        if not files or all(f.filename == "" for f in files):
            flash("Please select at least one resume file for bulk upload.", "error")
            return redirect(url_for("bulk_upload"))

        processed_candidates = []
        for f in files:
            if not f or not f.filename:
                continue
            fname = secure_filename(f.filename)
            ext = fname.rsplit(".", 1)[-1].lower() if "." in fname else ""
            if ext not in ALLOWED_EXTENSIONS:
                continue
            
            unique_name = f"{uuid.uuid4().hex[:8]}_{fname}"
            fpath = os.path.join(UPLOADS_DIR, unique_name)
            f.save(fpath)
            
            try:
                extracted = az.extract_text(fpath, unique_name)
                if len(extracted.split()) >= 30:
                    cid, analysis, matches = process_single_resume(extracted, fname)
                    processed_candidates.append({
                        "id": cid,
                        "name": analysis["contact"]["name"],
                        "filename": fname,
                        "score": analysis["score"],
                        "skills_count": len(analysis["skills"]),
                        "top_job": matches[0]["title"] if matches else "N/A",
                        "top_match": matches[0]["match"] if matches else 0
                    })
            except Exception:
                continue

        if not processed_candidates:
            flash("No valid text resumes could be processed from the upload.", "error")
            return redirect(url_for("bulk_upload"))

        ranked = sorted(processed_candidates, key=lambda c: (c["score"], c["top_match"]), reverse=True)
        return render_template("bulk_results.html", candidates=ranked, page="bulk")

    return render_template("bulk_upload.html", page="bulk")


@app.route("/compare", methods=["GET", "POST"])
def compare():
    db_conn = get_db()
    cur = db_conn.execute("SELECT id, name, filename, score, top_job FROM candidates ORDER BY name ASC")
    candidates = cur.fetchall()

    if request.method == "POST":
        c1_id = request.form.get("candidate1")
        c2_id = request.form.get("candidate2")
        
        if not c1_id or not c2_id or c1_id == c2_id:
            flash("Please choose two distinct candidates to compare.", "error")
            return redirect(url_for("compare"))

        c1_cur = db_conn.execute("SELECT * FROM candidates WHERE id=?", (c1_id,))
        c2_cur = db_conn.execute("SELECT * FROM candidates WHERE id=?", (c2_id,))
        c1_row = c1_cur.fetchone()
        c2_row = c2_cur.fetchone()
        
        if not c1_row or not c2_row:
            flash("Candidate records not found.", "error")
            return redirect(url_for("compare"))

        c1_data = json.loads(c1_row["data"])
        c2_data = json.loads(c2_row["data"])

        c1_skills = set(c1_data["analysis"]["skills"].keys())
        c2_skills = set(c2_data["analysis"]["skills"].keys())

        common_skills = sorted(c1_skills.intersection(c2_skills))
        c1_only_skills = sorted(c1_skills - c2_skills)
        c2_only_skills = sorted(c2_skills - c1_skills)

        return render_template(
            "compare_result.html",
            c1=c1_row,
            c2=c2_row,
            a1=c1_data["analysis"],
            a2=c2_data["analysis"],
            common_skills=common_skills,
            c1_only=c1_only_skills,
            c2_only=c2_only_skills,
            candidates=candidates,
            page="compare"
        )

    return render_template("compare.html", candidates=candidates, page="compare")


@app.route("/custom-job", methods=["GET", "POST"])
def custom_job():
    db_conn = get_db()
    if request.method == "POST":
        title = request.form.get("title", "").strip()
        company = request.form.get("company", "").strip() or "Recruiting Team"
        location = request.form.get("location", "").strip() or "Remote / Hybrid"
        job_type = request.form.get("type", "Full-time")
        salary = request.form.get("salary", "").strip() or "Competitive"
        description = request.form.get("description", "").strip()
        skills_raw = request.form.get("skills", "").strip()

        if not title or not description:
            flash("Job title and description are required.", "error")
            return redirect(url_for("custom_job"))

        if not skills_raw:
            auto_skills = list(az.find_skills(description).keys())
            skills_list = auto_skills if auto_skills else ["communication", "problem solving"]
        else:
            skills_list = [s.strip().lower() for s in skills_raw.split(",") if s.strip()]

        jid = f"custom-{uuid.uuid4().hex[:6]}"
        now = datetime.now().strftime("%Y-%m-%d %H:%M")
        db_conn.execute(
            "INSERT INTO custom_jobs VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (jid, title, company, location, job_type, salary, description, json.dumps(skills_list), now)
        )
        db_conn.commit()
        flash(f"Job requisition '{title}' created successfully!", "ok")
        return redirect(url_for("jobs"))

    return render_template("custom_job.html", page="jobs")


@app.route("/jobs")
def jobs():
    all_jobs = get_all_jobs()
    return render_template("jobs.html", jobs=all_jobs, page="jobs")


@app.route("/delete/<cid>", methods=["POST"])
def delete(cid):
    db_conn = get_db()
    db_conn.execute("DELETE FROM candidates WHERE id=?", (cid,))
    db_conn.commit()
    flash("Candidate record successfully deleted.", "ok")
    return redirect(url_for("dashboard"))


@app.route("/api/candidates", methods=["GET"])
def api_candidates():
    db_conn = get_db()
    cur = db_conn.execute(
        "SELECT id, name, email, filename, score, years, top_job, top_match, created FROM candidates ORDER BY created DESC"
    )
    rows = cur.fetchall()
    return jsonify([dict(r) for r in rows])


if __name__ == "__main__":
    # Render (and most cloud platforms) set the PORT env variable.
    # Bind to 0.0.0.0 so the container is reachable from outside.
    import os as _os
    _port = int(_os.environ.get("PORT", 5000))
    _debug = _os.environ.get("FLASK_DEBUG", "0") == "1"
    app.run(host="0.0.0.0", port=_port, debug=_debug)
