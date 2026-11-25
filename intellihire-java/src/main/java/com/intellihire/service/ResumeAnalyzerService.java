package com.intellihire.service;

import com.intellihire.model.AnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;

/**
 * Core resume analysis engine — identical logic to analyzer.py.
 * Scoring: Sections 20% | Skills 25% | Action Verbs 15% |
 *          Measurable Impact 15% | Contact 10% | Length 15%
 */
@Service
public class ResumeAnalyzerService {

    @Autowired private SkillExtractorService skillExtractor;

    private static final List<String> ACTION_VERBS = Arrays.asList(
        "led","built","designed","implemented","developed","created","managed",
        "optimized","improved","reduced","increased","achieved","deployed",
        "architected","launched","collaborated","mentored","automated",
        "delivered","spearheaded","established","streamlined","coordinated",
        "engineered","integrated","migrated","refactored","scaled","secured"
    );

    private static final List<String> SECTION_HEADERS = Arrays.asList(
        "summary","objective","profile","experience","work experience",
        "education","skills","technical skills","projects","certifications",
        "awards","publications","languages"
    );

    /** Run the full analysis pipeline and return an AnalysisResult. */
    public AnalysisResult analyse(String text) {
        AnalysisResult r = new AnalysisResult();
        r.resumeText = text;
        String lower = text.toLowerCase();

        // 1. Extract info
        r.name            = extractName(text);
        r.email           = extractEmail(text);
        r.phone           = extractPhone(text);
        r.hasLinkedIn     = lower.contains("linkedin");
        r.hasGitHub       = lower.contains("github");
        r.estimatedYears  = estimateYears(text);

        // 2. Skills
        r.skillsByCategory = skillExtractor.extractSkillsByCategory(text);
        for (List<String> cats : r.skillsByCategory.values()) r.skills.addAll(cats);

        // 3. Sections
        r.sections = detectSections(lower);

        // 4. Score
        r.scoreBreakdown = computeBreakdown(text, lower, r);
        r.overallScore   = r.scoreBreakdown.values().stream().mapToInt(Integer::intValue).sum();

        // 5. Tips
        r.tips = generateTips(r, text, lower);

        return r;
    }

    // ── Info extraction ───────────────────────────────────────

    private String extractName(String text) {
        String[] lines = text.strip().split("\n");
        for (String line : lines) {
            line = line.strip();
            if (line.length() > 2 && line.length() < 60
                    && !line.contains("@") && !line.matches(".*\\d{4}.*")) {
                // Looks like a name line
                return line.replaceAll("[^a-zA-Z .'-]", "").strip();
            }
        }
        return "Candidate";
    }

    private String extractEmail(String text) {
        Matcher m = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}")
                           .matcher(text);
        return m.find() ? m.group() : "";
    }

    private String extractPhone(String text) {
        Matcher m = Pattern.compile(
            "(?:\\+?1[-. ]?)?\\(?\\d{3}\\)?[-. ]?\\d{3}[-. ]?\\d{4}")
                           .matcher(text);
        return m.find() ? m.group() : "";
    }

    private int estimateYears(String text) {
        int max = 0;
        Matcher m = Pattern.compile("(\\d+)\\s+years?\\s+(?:of\\s+)?experience",
                Pattern.CASE_INSENSITIVE).matcher(text);
        while (m.find()) {
            try { max = Math.max(max, Integer.parseInt(m.group(1))); } catch (NumberFormatException ignored) {}
        }
        return max;
    }

    // ── Section detection ─────────────────────────────────────

    private Map<String, Boolean> detectSections(String lower) {
        Map<String, Boolean> sections = new LinkedHashMap<>();
        sections.put("summary",        matchesSection(lower, "summary","objective","profile","about"));
        sections.put("experience",     matchesSection(lower, "experience","employment","work history"));
        sections.put("education",      matchesSection(lower, "education","academic","degree","university","college"));
        sections.put("skills",         matchesSection(lower, "skills","technologies","competencies","tools"));
        sections.put("projects",       matchesSection(lower, "projects","portfolio","work samples"));
        sections.put("certifications", matchesSection(lower, "certifi","credential","license","award"));
        return sections;
    }

    private boolean matchesSection(String lower, String... keywords) {
        for (String kw : keywords) if (lower.contains(kw)) return true;
        return false;
    }

    // ── Scoring ───────────────────────────────────────────────

    private Map<String, Integer> computeBreakdown(String text, String lower, AnalysisResult r) {
        Map<String, Integer> bd = new LinkedHashMap<>();

        // Sections: 20 pts
        long sectionCount = r.sections.values().stream().filter(Boolean::booleanValue).count();
        bd.put("sections", (int) Math.round(sectionCount / 6.0 * 20));

        // Skills: 25 pts
        int skillScore = Math.min(25, (int) Math.round(r.skills.size() / 15.0 * 25));
        bd.put("skills", skillScore);

        // Action verbs: 15 pts
        int verbCount = 0;
        for (String verb : ACTION_VERBS) {
            if (Pattern.compile("\\b" + verb + "\\b", Pattern.CASE_INSENSITIVE).matcher(lower).find()) verbCount++;
        }
        bd.put("actionVerbs", Math.min(15, verbCount * 2));

        // Measurable impact: 15 pts
        long metricCount = Pattern.compile("\\d+%|\\$\\d+|\\d+x\\b|\\d+k\\b|\\d+m\\b|\\d+ users|\\d+ clients",
                Pattern.CASE_INSENSITIVE).matcher(lower).results().count();
        bd.put("measurableImpact", (int) Math.min(15, metricCount * 4));

        // Contact: 10 pts
        int contact = 0;
        if (!r.email.isEmpty()) contact += 3;
        if (!r.phone.isEmpty()) contact += 3;
        if (r.hasLinkedIn) contact += 2;
        if (r.hasGitHub) contact += 2;
        bd.put("contactInfo", contact);

        // Length: 15 pts
        int words = text.split("\\s+").length;
        int lengthScore;
        if (words >= 300 && words <= 800) lengthScore = 15;
        else if (words >= 200 && words < 300) lengthScore = 10;
        else if (words > 800 && words <= 1200) lengthScore = 10;
        else lengthScore = 5;
        bd.put("length", lengthScore);

        return bd;
    }

    // ── Improvement tips ──────────────────────────────────────

    private List<String> generateTips(AnalysisResult r, String text, String lower) {
        List<String> tips = new ArrayList<>();

        if (!r.sections.getOrDefault("summary", false))
            tips.add("Add a professional Summary/Objective section at the top of your resume.");
        if (!r.sections.getOrDefault("experience", false))
            tips.add("Include a detailed Work Experience section with job titles, companies, and dates.");
        if (!r.sections.getOrDefault("projects", false))
            tips.add("Showcase your Projects section to demonstrate hands-on experience.");
        if (!r.sections.getOrDefault("certifications", false))
            tips.add("Add Certifications or Awards to stand out from other candidates.");
        if (r.skills.size() < 5)
            tips.add("List more technical skills — aim for at least 10 relevant technologies.");
        if (!r.hasLinkedIn)
            tips.add("Include your LinkedIn profile URL to increase recruiter visibility.");
        if (!r.hasGitHub)
            tips.add("Add your GitHub profile URL to show your coding portfolio.");
        if (r.phone.isEmpty())
            tips.add("Include your phone number for easy recruiter contact.");

        int words = text.split("\\s+").length;
        if (words < 300)
            tips.add("Your resume is too short (" + words + " words). Aim for 300-800 words.");
        if (words > 1200)
            tips.add("Your resume is very long (" + words + " words). Try to condense it to 1-2 pages.");

        long metricCount = Pattern.compile("\\d+%|\\$\\d+|\\d+x\\b").matcher(lower).results().count();
        if (metricCount < 2)
            tips.add("Add measurable achievements (e.g., 'Improved performance by 40%', 'Led a team of 8').");

        int verbCount = 0;
        for (String verb : ACTION_VERBS) {
            if (Pattern.compile("\\b" + verb + "\\b", Pattern.CASE_INSENSITIVE).matcher(lower).find()) verbCount++;
        }
        if (verbCount < 4)
            tips.add("Use strong action verbs like 'Led', 'Built', 'Optimized' to describe your work.");

        return tips;
    }
}
