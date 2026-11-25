package com.intellihire.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Parses resume files (PDF, DOCX, TXT) into plain text.
 * Equivalent to the pypdf + python-docx parsing in analyzer.py.
 */
@Service
public class ResumeParserService {

    private static final long MAX_SIZE = 8 * 1024 * 1024; // 8 MB
    private static final int MIN_CHARS = 50;

    /**
     * Parse a MultipartFile into clean resume text.
     * @throws IllegalArgumentException for invalid files
     */
    public String parse(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("File exceeds 8 MB limit.");
        }

        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase() : "";

        String text;
        if (filename.endsWith(".pdf")) {
            text = parsePdf(file.getInputStream());
        } else if (filename.endsWith(".docx")) {
            text = parseDocx(file.getInputStream());
        } else if (filename.endsWith(".txt")) {
            text = parseTxt(file.getInputStream());
        } else {
            throw new IllegalArgumentException(
                "Unsupported file type. Upload PDF, DOCX, or TXT.");
        }

        text = text.trim();
        if (text.length() < MIN_CHARS) {
            throw new IllegalArgumentException(
                "Resume text is too short or could not be extracted. Please upload a readable file.");
        }
        return text;
    }

    /** Parse a plain-text string (paste-text feature). */
    public String parsePastedText(String text) {
        if (text == null || text.trim().length() < MIN_CHARS) {
            throw new IllegalArgumentException("Pasted text is too short (minimum 50 characters).");
        }
        return text.trim();
    }

    // ── Private parsers ───────────────────────────────────────

    private String parsePdf(InputStream is) throws IOException {
        byte[] bytes = is.readAllBytes();
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String parseDocx(InputStream is) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs()) {
                sb.append(para.getText()).append("\n");
            }
            return sb.toString();
        }
    }

    private String parseTxt(InputStream is) throws IOException {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Sanitise filename for safe storage. */
    public static String sanitiseFilename(String original) {
        if (original == null) return "resume.txt";
        return original.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
