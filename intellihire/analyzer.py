"""IntelliHire AI - resume parsing, scoring and job matching (pure Python NLP)."""
import math
import re
from collections import Counter

# 100+ skills categorized across 6 key tech and business domains
SKILLS = {
    "Programming": [
        "python", "java", "javascript", "typescript", "c++", "c#", "go", "rust",
        "php", "ruby", "kotlin", "swift", "r", "scala", "bash", "shell", "dart",
        "perl", "matlab", "c"
    ],
    "Web": [
        "html", "css", "react", "angular", "vue", "node.js", "django", "flask",
        "fastapi", "spring boot", "rest api", "graphql", "tailwind", "bootstrap",
        "sass", "next.js", "express", "svelte", "jquery", "asp.net", "webrtc"
    ],
    "Data & AI": [
        "machine learning", "deep learning", "nlp", "tensorflow", "pytorch",
        "scikit-learn", "pandas", "numpy", "data analysis", "statistics",
        "computer vision", "power bi", "tableau", "excel", "llm", "spark",
        "hadoop", "keras", "opencv", "bigquery", "data visualization", "generative ai"
    ],
    "Database": [
        "sql", "mysql", "postgresql", "mongodb", "redis", "sqlite", "oracle",
        "elasticsearch", "cassandra", "mariadb", "dynamodb", "neo4j", "snowflake"
    ],
    "Cloud & DevOps": [
        "aws", "azure", "gcp", "docker", "kubernetes", "jenkins", "git", "github",
        "gitlab", "ci/cd", "linux", "terraform", "devops", "ansible", "nginx",
        "helm", "prometheus", "grafana", "circleci"
    ],
    "Soft Skills": [
        "leadership", "communication", "teamwork", "problem solving",
        "project management", "agile", "scrum", "time management", "critical thinking",
        "adaptability", "collaboration", "mentoring", "negotiation", "conflict resolution",
        "analytical thinking"
    ],
}

ALL_SKILLS = {s: cat for cat, lst in SKILLS.items() for s in lst}

# Word boundary pattern that safely handles c++, c#, node.js, ci/cd, .net, r, c, go etc.
def build_skill_pattern(skill):
    esc = re.escape(skill)
    # If the skill itself ends with a dot (unlikely) or contains symbols
    # We do NOT include '.' in right lookahead unless skill ends with alphanumeric and might be part of an extension like node.js
    # Standard sentence full-stop should be allowed after a skill word.
    # Therefore, left lookbehind checks alphanumeric and symbols; right checks alphanumeric and symbols,
    # except a trailing period followed by whitespace/end-of-string is allowed as sentence punctuation.
    if skill in {"c", "r"}:
        # Very short single letters need stricter isolation (e.g. " C ", " R language", "programming in R")
        return re.compile(r"(?<![a-zA-Z0-9+#_])" + esc + r"(?![a-zA-Z0-9+#_])", re.IGNORECASE)
    
    left = r"(?<![a-zA-Z0-9+#])"
    # To allow trailing period when followed by whitespace or end of string, but avoid matching "go" in "google" or "gold"
    right = r"(?![a-zA-Z0-9+#])(?!\.[a-zA-Z0-9])"
    return re.compile(left + esc + right, re.IGNORECASE)

SKILL_PATTERNS = {s: (build_skill_pattern(s), cat) for s, cat in ALL_SKILLS.items()}

SECTIONS = {
    "Summary": r"\b(summary|objective|profile|about\s*me|professional\s*summary)\b",
    "Experience": r"\b(experience|employment|work\s*history|internship|work\s*experience|professional\s*experience)\b",
    "Education": r"\b(education|academic|degree|university|college|qualifications)\b",
    "Skills": r"\b(skills|technologies|technical\s*skills|competencies|core\s*competencies)\b",
    "Projects": r"\b(projects?|portfolio|personal\s*projects|academic\s*projects)\b",
    "Certifications": r"\b(certifications?|certificates?|licenses?|courses?)\b",
}

ACTION_VERBS = [
    "developed", "built", "designed", "led", "created", "implemented", "improved",
    "managed", "optimized", "launched", "automated", "analyzed", "delivered",
    "reduced", "increased", "deployed", "spearheaded", "architected", "engineered",
    "orchestrated", "streamlined", "transformed", "established", "formulated"
]

STOP = set(
    "a an the and or of to in for on with is are was were be by at as from that this it its we you your our i have has had will can into about but if then them their so also each".split()
)


def extract_text(path, filename):
    """Extract text from PDF, DOCX, or plain text file."""
    ext = filename.rsplit(".", 1)[-1].lower() if "." in filename else ""
    if ext == "pdf":
        from pypdf import PdfReader
        reader = PdfReader(path)
        pages = [(p.extract_text() or "") for p in reader.pages]
        return "\n".join(pages)
    if ext == "docx":
        from docx import Document
        doc = Document(path)
        return "\n".join(p.text for p in doc.paragraphs)
    # Default to text
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return f.read()


def find_skills(text):
    """Extract identified skills using specialized boundary regexes."""
    found = {}
    for skill, (pat, cat) in SKILL_PATTERNS.items():
        if pat.search(text):
            found[skill] = cat
    return found


def extract_contact(text):
    """Extract candidate name, email, phone, and profile links."""
    email_match = re.search(r"[\w.+-]+@[\w-]+\.[\w.-]+", text)
    phone_match = re.search(r"(\+?\d[\d\s\-()]{8,}\d)", text)
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    
    # Heuristic for name: first non-empty line that does not look like an email or label
    name = "Candidate"
    for line in lines[:5]:
        clean = re.sub(r"[|\-•,].*", "", line).strip()
        if clean and "@" not in clean and not re.search(r"^(resume|curriculum|cv|profile|contact)", clean, re.I):
            name = clean[:60]
            break

    linkedin_found = bool(re.search(r"linkedin\.com", text, re.I))
    github_found = bool(re.search(r"github\.com", text, re.I))

    return {
        "name": name,
        "email": email_match.group(0) if email_match else "",
        "phone": phone_match.group(0).strip() if phone_match else "",
        "linkedin": linkedin_found,
        "github": github_found,
    }


def estimate_years(text):
    """Estimate total years of experience from explicit mentions and year spans."""
    explicit_matches = re.findall(r"(\d{1,2})\+?\s*(?:years?|yrs?)(?:\s+of)?(?:\s+experience)?", text, re.I)
    if explicit_matches:
        try:
            return max(int(x) for x in explicit_matches if int(x) <= 45)
        except ValueError:
            pass
            
    years = [int(y) for y in re.findall(r"\b(20\d{2}|19\d{2})\b", text)]
    if len(years) >= 2:
        diff = max(years) - min(years)
        return max(0, min(diff, 35))
    return 0


def analyze_resume(text):
    """Compute score, component breakdown, extracted skills, sections, and improvement tips."""
    skills = find_skills(text)
    contact = extract_contact(text)
    low = text.lower()
    words = len(text.split())
    
    sections = {k: bool(re.search(p, low)) for k, p in SECTIONS.items()}
    verbs = [v for v in ACTION_VERBS if re.search(r"\b" + re.escape(v) + r"\b", low)]
    
    # Detect metrics and measurable results (e.g. 40%, $50k, 10+ users, 5 clients)
    metric_matches = re.findall(
        r"\d+\s*%|\$\s?\d+[\d,.]*(?:[kmbKMB])?|\b\d+\+?\s*(?:users|clients|projects|members|customers|requests|services|engineers|teams|downloads)\b",
        low
    )
    metrics_count = len(metric_matches)

    # 1. Sections (20%)
    c_sections = (sum(sections.values()) / len(sections)) * 100
    # 2. Skills (25%) -> target ~12+ skills
    c_skills = min(len(skills) / 12, 1.0) * 100
    # 3. Action verbs (15%) -> target ~6+ strong verbs
    c_verbs = min(len(verbs) / 6, 1.0) * 100
    # 4. Measurable impact (15%) -> target ~4+ metrics
    c_metrics = min(metrics_count / 4, 1.0) * 100
    # 5. Contact info (10%)
    contact_items = [
        bool(contact["email"]),
        bool(contact["phone"]),
        contact["linkedin"],
        contact["github"]
    ]
    c_contact = (sum(contact_items) / len(contact_items)) * 100
    # 6. Length (15%) -> Ideal: 300 to 900 words
    if 300 <= words <= 900:
        c_length = 100
    elif (180 <= words < 300) or (900 < words <= 1400):
        c_length = 65
    else:
        c_length = 35

    breakdown = {
        "Sections": round(c_sections),
        "Skills": round(c_skills),
        "Action Verbs": round(c_verbs),
        "Measurable Impact": round(c_metrics),
        "Contact Info": round(c_contact),
        "Length": round(c_length)
    }

    weights = {
        "Sections": 0.20,
        "Skills": 0.25,
        "Action Verbs": 0.15,
        "Measurable Impact": 0.15,
        "Contact Info": 0.10,
        "Length": 0.15
    }
    score = round(sum(breakdown[k] * weights[k] for k in breakdown))
    score = max(0, min(100, score))

    # Actionable Improvement Tips
    tips = []
    for s, ok in sections.items():
        if not ok:
            tips.append(f"Add a dedicated '{s}' section to help ATS and recruiters find key details.")
    if len(skills) < 8:
        tips.append("Expand your technical and soft skill keywords (aim for 10-15+ relevant skills).")
    if len(verbs) < 5:
        tips.append("Begin bullet points with high-impact action verbs (e.g., spearheaded, architected, optimized, automated).")
    if metrics_count < 3:
        tips.append("Quantify accomplishments with measurable metrics (e.g., 'reduced latency by 35%', 'managed team of 6').")
    if not contact["email"]:
        tips.append("Add a clear professional email address at the top.")
    if not contact["phone"]:
        tips.append("Include a reachable phone number in your contact header.")
    if not contact["linkedin"]:
        tips.append("Include your LinkedIn profile URL so recruiters can view your full network.")
    if not contact["github"]:
        tips.append("Include your GitHub or portfolio URL to showcase real-world code and projects.")
    if words < 250:
        tips.append("Resume is too concise — elaborate on achievements, technical stack, and responsibilities.")
    elif words > 950:
        tips.append("Resume length is high — condense older experience to maintain a tight 1-2 page document.")

    return {
        "contact": contact,
        "skills": skills,
        "sections": sections,
        "score": score,
        "breakdown": breakdown,
        "tips": tips,
        "words": words,
        "years": estimate_years(text),
        "verbs": verbs,
        "metrics": metrics_count
    }


# ---------- Pure Python TF-IDF Cosine Similarity ----------

def tokens(text):
    """Tokenize and filter stop words."""
    cleaned = re.sub(r"[^a-zA-Z0-9+#.]", " ", text.lower())
    return [w for w in cleaned.split() if len(w) > 1 and w not in STOP]


def tfidf_cosine(query, docs):
    """Compute cosine similarity between query and multiple documents."""
    doc_tokens = [tokens(d) for d in docs]
    query_tokens = tokens(query)
    all_docs = doc_tokens + [query_tokens]
    n_docs = len(all_docs)

    # Document frequency
    df = Counter()
    for doc in all_docs:
        for w in set(doc):
            df[w] += 1

    def compute_vector(toks):
        tf = Counter(toks)
        vec = {}
        for w, count in tf.items():
            # Standard tf-idf weighting
            w_tf = 1.0 + math.log(count)
            w_idf = math.log((n_docs + 1) / (df[w] + 1)) + 1.0
            vec[w] = w_tf * w_idf
        return vec

    query_vec = compute_vector(query_tokens)
    query_norm = math.sqrt(sum(v * v for v in query_vec.values())) if query_vec else 0.0

    similarities = []
    for d_toks in doc_tokens:
        d_vec = compute_vector(d_toks)
        d_norm = math.sqrt(sum(v * v for v in d_vec.values())) if d_vec else 0.0

        if query_norm == 0.0 or d_norm == 0.0:
            similarities.append(0.0)
            continue

        dot = sum(query_vec[w] * d_vec[w] for w in query_vec if w in d_vec)
        similarities.append(dot / (query_norm * d_norm))

    return similarities


def match_jobs(resume_text, resume_skills, jobs):
    """Match resume against jobs using 70% skill overlap + 30% TF-IDF cosine similarity."""
    job_docs = [
        f"{j['title']} {j['company']} {j['description']} {' '.join(j.get('skills', []))}"
        for j in jobs
    ]
    sims = tfidf_cosine(resume_text, job_docs)
    have_skills = {s.lower() for s in resume_skills}

    results = []
    for job, sim in zip(jobs, sims):
        req_skills = [s.lower() for s in job.get("skills", [])]
        matched = [s for s in req_skills if s in have_skills]
        missing = [s for s in req_skills if s not in have_skills]
        
        skill_pct = (len(matched) / len(req_skills)) if req_skills else 0.0
        # Normalize tfidf score scaling (sim is usually 0.15 - 0.6 in text overlap)
        tfidf_scaled = min(sim * 2.2, 1.0)
        
        total_match = round(((0.70 * skill_pct) + (0.30 * tfidf_scaled)) * 100)
        total_match = max(0, min(100, total_match))

        results.append({
            **job,
            "match": total_match,
            "matched": matched,
            "missing": missing,
            "skill_pct": round(skill_pct * 100),
            "sim_pct": round(tfidf_scaled * 100)
        })

    return sorted(results, key=lambda r: r["match"], reverse=True)
