"""IntelliHire AI - PDF Report Generator without heavy external C dependencies.
Generates a valid, clean, single/multi-page PDF report using pure standard library.
"""
import io
import re
from datetime import datetime


class MinimalPDF:
    """Lightweight pure-Python PDF builder producing valid PDF-1.4 documents."""
    def __init__(self):
        self.objects = []
        self.pages = []
        self.current_stream = []
        
    def add_page(self):
        if self.current_stream:
            self._flush_stream()
        self.current_stream = []
        page_num = len(self.pages) + 1
        self.pages.append(page_num)
        
    def _flush_stream(self):
        content = "\n".join(self.current_stream).encode("latin1", errors="replace")
        self.objects.append(b"<< /Length " + str(len(content)).encode("ascii") + b" >>\nstream\n" + content + b"\nendstream")

    def draw_text(self, text, x, y, size=11, bold=False, color=(0.1, 0.1, 0.1)):
        # PDF coordinates: (0,0) is bottom-left, letter is 612x792
        font_name = "/F2" if bold else "/F1"
        r, g, b = color
        safe_text = re.sub(r"[\\()]", "", str(text))
        cmd = f"BT {r:.2f} {g:.2f} {b:.2f} rg {font_name} {size} Tf {x:.1f} {y:.1f} Td ({safe_text}) Tj ET"
        self.current_stream.append(cmd)

    def draw_rect(self, x, y, width, height, fill_color=(0.95, 0.95, 0.98), stroke_color=None):
        r, g, b = fill_color
        cmd = [f"{r:.2f} {g:.2f} {b:.2f} rg {x:.1f} {y:.1f} {width:.1f} {height:.1f} re f"]
        if stroke_color:
            sr, sg, sb = stroke_color
            cmd.append(f"{sr:.2f} {sg:.2f} {sb:.2f} RG {x:.1f} {y:.1f} {width:.1f} {height:.1f} re S")
        self.current_stream.append("\n".join(cmd))

    def draw_line(self, x1, y1, x2, y2, color=(0.8, 0.8, 0.8), width=1.0):
        r, g, b = color
        cmd = f"{r:.2f} {g:.2f} {b:.2f} RG {width} w {x1:.1f} {y1:.1f} m {x2:.1f} {y2:.1f} l S"
        self.current_stream.append(cmd)

    def build(self):
        if self.current_stream:
            self._flush_stream()

        # Object IDs layout:
        # 1: Catalog
        # 2: Pages
        # 3: Font Helvetica
        # 4: Font Helvetica-Bold
        # Stream objects and Page objects follow
        num_pages = len(self.pages)
        # Content streams are objects 5 to 4+num_pages
        # Page objects are objects 5+num_pages to 4+2*num_pages
        stream_base = 5
        page_base = stream_base + num_pages
        
        body = []
        body.append(b"%PDF-1.4\n")
        
        # We will build all objects
        all_objs = {}
        all_objs[1] = b"<< /Type /Catalog /Pages 2 0 R >>"
        
        kids_refs = " ".join(f"{page_base + i} 0 R" for i in range(num_pages))
        all_objs[2] = f"<< /Type /Pages /Kids [ {kids_refs} ] /Count {num_pages} >>".encode("ascii")
        all_objs[3] = b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
        all_objs[4] = b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"
        
        for i, stream_bytes in enumerate(self.objects):
            all_objs[stream_base + i] = stream_bytes
            
        for i in range(num_pages):
            page_id = page_base + i
            stream_id = stream_base + i
            page_dict = (
                f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                f"/Contents {stream_id} 0 R "
                f"/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> >>"
            ).encode("ascii")
            all_objs[page_id] = page_dict

        total_objects = len(all_objs)
        offsets = {}
        out = io.BytesIO()
        out.write(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
        
        for obj_id in sorted(all_objs.keys()):
            offsets[obj_id] = out.tell()
            out.write(f"{obj_id} 0 obj\n".encode("ascii"))
            out.write(all_objs[obj_id])
            out.write(b"\nendobj\n")
            
        xref_offset = out.tell()
        out.write(b"xref\n")
        out.write(f"0 {total_objects + 1}\n".encode("ascii"))
        out.write(b"0000000000 65535 f \n")
        for obj_id in sorted(all_objs.keys()):
            pos = offsets[obj_id]
            out.write(f"{pos:010d} 00000 n \n".encode("ascii"))
            
        out.write(b"trailer\n")
        out.write(f"<< /Size {total_objects + 1} /Root 1 0 R >>\n".encode("ascii"))
        out.write(b"startxref\n")
        out.write(f"{xref_offset}\n".encode("ascii"))
        out.write(b"%%EOF\n")
        
        return out.getvalue()


def generate_candidate_pdf(candidate_row, data):
    """Generate a high-grade 1-page candidate evaluation PDF report."""
    analysis = data.get("analysis", {})
    matches = data.get("matches", [])
    contact = analysis.get("contact", {})
    breakdown = analysis.get("breakdown", {})
    skills = analysis.get("skills", {})
    tips = analysis.get("tips", [])

    pdf = MinimalPDF()
    pdf.add_page()

    # 1. Header Banner
    pdf.draw_rect(36, 706, 540, 56, fill_color=(0.31, 0.27, 0.90))
    pdf.draw_text("IntelliHire AI - Candidate Evaluation Report", 54, 736, size=17, bold=True, color=(1, 1, 1))
    generated_on = datetime.now().strftime("%B %d, %Y - %H:%M")
    pdf.draw_text(f"Generated: {generated_on} | Candidate ID: {candidate_row['id']}", 54, 718, size=9, bold=False, color=(0.85, 0.85, 1.0))

    # 2. Candidate Overview Card
    pdf.draw_rect(36, 620, 540, 74, fill_color=(0.97, 0.98, 1.0), stroke_color=(0.88, 0.90, 0.95))
    candidate_name = contact.get("name") or candidate_row["name"] or "Candidate"
    pdf.draw_text(candidate_name, 52, 668, size=15, bold=True, color=(0.1, 0.12, 0.2))
    
    email = contact.get("email") or "No email provided"
    phone = contact.get("phone") or "No phone provided"
    years = candidate_row["years"]
    filename = candidate_row["filename"]
    pdf.draw_text(f"Email: {email}   |   Phone: {phone}", 52, 650, size=9, bold=False, color=(0.35, 0.4, 0.5))
    pdf.draw_text(f"Est. Experience: ~{years} yrs   |   Source File: {filename}", 52, 634, size=9, bold=False, color=(0.35, 0.4, 0.5))

    # 3. Overall Score Badge
    score = candidate_row["score"]
    score_color = (0.06, 0.72, 0.50) if score >= 75 else ((0.96, 0.62, 0.07) if score >= 50 else (0.93, 0.27, 0.27))
    pdf.draw_rect(440, 630, 120, 54, fill_color=score_color)
    pdf.draw_text("ATS SCORE", 468, 666, size=8, bold=True, color=(1, 1, 1))
    pdf.draw_text(f"{score}/100", 460, 642, size=18, bold=True, color=(1, 1, 1))

    # 4. Score Dimensions Breakdown
    pdf.draw_text("ATS Evaluation Breakdown", 36, 596, size=12, bold=True, color=(0.1, 0.15, 0.3))
    pdf.draw_line(36, 590, 576, 590, color=(0.85, 0.88, 0.95), width=1)

    y_pos = 568
    dim_items = list(breakdown.items())
    for i, (dim, val) in enumerate(dim_items):
        col_x = 36 if i < 3 else 310
        cur_y = y_pos - (i % 3) * 22
        pdf.draw_text(f"- {dim}:", col_x, cur_y, size=9, bold=True, color=(0.2, 0.25, 0.35))
        # Draw small bar
        pdf.draw_rect(col_x + 130, cur_y - 2, 80, 8, fill_color=(0.90, 0.92, 0.95))
        bar_w = max(2, int(80 * (val / 100.0)))
        pdf.draw_rect(col_x + 130, cur_y - 2, bar_w, 8, fill_color=(0.31, 0.27, 0.90))
        pdf.draw_text(f"{val}%", col_x + 218, cur_y, size=8, bold=False, color=(0.3, 0.35, 0.45))

    # 5. Detected Skills (Cloud / Summary)
    pdf.draw_text(f"Detected Skills ({len(skills)})", 36, 492, size=12, bold=True, color=(0.1, 0.15, 0.3))
    pdf.draw_line(36, 486, 576, 486, color=(0.85, 0.88, 0.95), width=1)

    skill_str = ", ".join(sorted(skills.keys()))
    if len(skill_str) > 220:
        skill_str = skill_str[:220] + "... and more"
    pdf.draw_text(skill_str or "No prominent technical skills recognized", 36, 468, size=9, bold=False, color=(0.25, 0.3, 0.4))

    # 6. Top Job Matches
    pdf.draw_text("Top Recommended Position Matches", 36, 436, size=12, bold=True, color=(0.1, 0.15, 0.3))
    pdf.draw_line(36, 430, 576, 430, color=(0.85, 0.88, 0.95), width=1)

    job_y = 408
    for jm in matches[:3]:
        pdf.draw_rect(36, job_y - 8, 540, 36, fill_color=(0.98, 0.98, 1.0), stroke_color=(0.9, 0.92, 0.96))
        pdf.draw_text(f"{jm['title']} ({jm['company']} - {jm['location']})", 46, job_y + 12, size=10, bold=True, color=(0.15, 0.2, 0.3))
        pdf.draw_text(f"Match: {jm['match']}%   |   Salary: {jm['salary']}", 46, job_y - 2, size=8, bold=False, color=(0.4, 0.45, 0.55))
        
        # Matched vs Missing
        matched_txt = "Matched: " + ", ".join(jm.get("matched", [])[:4])
        missing_txt = "Missing: " + (", ".join(jm.get("missing", [])[:3]) or "None")
        pdf.draw_text(f"{matched_txt}   |   {missing_txt}", 260, job_y + 5, size=8, bold=False, color=(0.3, 0.35, 0.45))
        job_y -= 44

    # 7. Actionable Improvement Tips
    pdf.draw_text("Priority Improvement Suggestions", 36, job_y - 2, size=12, bold=True, color=(0.1, 0.15, 0.3))
    pdf.draw_line(36, job_y - 8, 576, job_y - 8, color=(0.85, 0.88, 0.95), width=1)

    tip_y = job_y - 26
    for t in tips[:4]:
        clean_tip = t[:95] + "..." if len(t) > 95 else t
        pdf.draw_text(f"* {clean_tip}", 42, tip_y, size=8.5, bold=False, color=(0.25, 0.3, 0.4))
        tip_y -= 16

    # Footer
    pdf.draw_line(36, 42, 576, 42, color=(0.85, 0.88, 0.95), width=1)
    pdf.draw_text("IntelliHire AI - Confidential Talent Assessment Document - Powered by Pure-Python NLP", 120, 28, size=8, bold=False, color=(0.5, 0.55, 0.65))

    return pdf.build()
