package com.intellihire.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellihire.model.AnalysisResult;
import com.intellihire.model.Candidate;
import com.intellihire.repository.CandidateRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Generates a simple PDF report for a candidate.
 * Pure-Java PDF generation (no external PDF library needed at runtime).
 */
@Controller
public class PdfExportController {

    @Autowired private CandidateRepository candidateRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/export-pdf/{id}")
    public void exportPdf(@PathVariable String id, HttpServletResponse response) throws Exception {
        var opt = candidateRepo.findById(id);
        if (opt.isEmpty()) { response.sendError(404); return; }

        Candidate c = opt.get();
        AnalysisResult r = mapper.readValue(c.getData(), AnalysisResult.class);

        // Build PDF content as plain text report (PDF 1.4 format)
        String report = buildTextReport(c, r);
        byte[] pdfBytes = generateSimplePdf(report, c.getName() + " — IntelliHire AI Report");

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"intellihire_" + id + ".pdf\"");
        response.setContentLength(pdfBytes.length);
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();
    }

    private String buildTextReport(Candidate c, AnalysisResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("IntelliHire AI — Resume Analysis Report\n");
        sb.append("=".repeat(50)).append("\n\n");
        sb.append("Candidate: ").append(r.name).append("\n");
        sb.append("Email:     ").append(r.email).append("\n");
        sb.append("Phone:     ").append(r.phone).append("\n");
        sb.append("LinkedIn:  ").append(r.hasLinkedIn ? "Yes" : "No").append("\n");
        sb.append("GitHub:    ").append(r.hasGitHub ? "Yes" : "No").append("\n");
        sb.append("Experience:").append(r.estimatedYears).append(" years\n\n");

        sb.append("OVERALL SCORE: ").append(r.overallScore).append(" / 100\n\n");

        sb.append("SCORE BREAKDOWN:\n");
        r.scoreBreakdown.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));

        sb.append("\nSKILLS (").append(r.skills.size()).append(" detected):\n");
        sb.append("  ").append(String.join(", ", r.skills)).append("\n");

        sb.append("\nSECTIONS:\n");
        r.sections.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v ? "✓" : "✗").append("\n"));

        sb.append("\nIMPROVEMENT TIPS:\n");
        for (String tip : r.tips) sb.append("  • ").append(tip).append("\n");

        sb.append("\nTOP JOB MATCHES:\n");
        int rank = 1;
        for (var jm : r.jobMatches) {
            sb.append("  ").append(rank++).append(". ").append(jm.title)
              .append(" @ ").append(jm.company)
              .append(" — ").append(jm.matchPercent).append("% match\n");
        }
        return sb.toString();
    }

    /** Minimal PDF-1.4 generator — text only, no graphics needed. */
    private byte[] generateSimplePdf(String content, String title) throws IOException {
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        PrintWriter w = new PrintWriter(new OutputStreamWriter(pdf, StandardCharsets.US_ASCII));

        String[] lines = content.split("\n");
        int pageHeight = 800;
        int lineHeight = 14;
        int margin = 50;
        int y = pageHeight - margin;

        // We'll produce a simple PDF with a single content stream
        // Collect stream content first
        StringBuilder stream = new StringBuilder();
        stream.append("BT\n/F1 11 Tf\n");
        for (String line : lines) {
            String safe = line.replace("\\", "\\\\").replace("(","\\(").replace(")","\\)").replace("\r","");
            safe = safe.replaceAll("[^\\x20-\\x7E]", "?"); // strip non-ASCII
            stream.append(margin).append(" ").append(y).append(" Td\n");
            stream.append("(").append(safe).append(") Tj\n");
            stream.append("0 -").append(lineHeight).append(" Td\n");
            y -= lineHeight;
        }
        stream.append("ET\n");
        byte[] streamBytes = stream.toString().getBytes(StandardCharsets.US_ASCII);

        // Object offsets for xref
        int[] offsets = new int[6];

        // %PDF header
        String header = "%PDF-1.4\n";
        pdf.reset();
        pdf.write(header.getBytes());

        // obj 1: catalog
        offsets[1] = pdf.size();
        String obj1 = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n";
        pdf.write(obj1.getBytes());

        // obj 2: pages
        offsets[2] = pdf.size();
        String obj2 = "2 0 obj\n<< /Type /Pages /Kids [4 0 R] /Count 1 >>\nendobj\n";
        pdf.write(obj2.getBytes());

        // obj 3: font
        offsets[3] = pdf.size();
        String obj3 = "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>\nendobj\n";
        pdf.write(obj3.getBytes());

        // obj 4: page
        offsets[4] = pdf.size();
        String obj4 = "4 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 " + pageHeight + "] " +
                      "/Contents 5 0 R /Resources << /Font << /F1 3 0 R >> >> >>\nendobj\n";
        pdf.write(obj4.getBytes());

        // obj 5: content stream
        offsets[5] = pdf.size();
        String obj5head = "5 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n";
        pdf.write(obj5head.getBytes());
        pdf.write(streamBytes);
        pdf.write("\nendstream\nendobj\n".getBytes());

        // xref
        int xrefOffset = pdf.size();
        StringBuilder xref = new StringBuilder("xref\n0 6\n");
        xref.append("0000000000 65535 f \n");
        for (int i = 1; i <= 5; i++) {
            xref.append(String.format("%010d 00000 n \n", offsets[i]));
        }
        pdf.write(xref.toString().getBytes());

        // trailer
        String trailer = "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF\n";
        pdf.write(trailer.getBytes());

        return pdf.toByteArray();
    }
}
