package com.intellihire.model;

import java.util.*;

/**
 * Full analysis result for a resume — stored as JSON in the DB candidates.data column.
 * Public fields work with both Jackson (JSON) and Thymeleaf (SpEL).
 */
public class AnalysisResult {

    // ── Extracted contact info ────────────────────────────────
    public String name = "";
    public String email = "";
    public String phone = "";
    public boolean hasLinkedIn = false;
    public boolean hasGitHub = false;
    public int estimatedYears = 0;

    // ── Overall score 0-100 ───────────────────────────────────
    public int overallScore = 0;

    // ── Per-component breakdown ───────────────────────────────
    // Keys: sections, skills, actionVerbs, measurableImpact, contactInfo, length
    public Map<String, Integer> scoreBreakdown = new LinkedHashMap<>();

    // ── Skill extraction ──────────────────────────────────────
    public List<String> skills = new ArrayList<>();
    public Map<String, List<String>> skillsByCategory = new LinkedHashMap<>();

    // ── Section detection ─────────────────────────────────────
    // Keys: summary, experience, education, skills, projects, certifications
    public Map<String, Boolean> sections = new LinkedHashMap<>();

    // ── Improvement tips ──────────────────────────────────────
    public List<String> tips = new ArrayList<>();

    // ── Job matches (top 6) ───────────────────────────────────
    public List<JobMatch> jobMatches = new ArrayList<>();

    // ── Raw resume text (for compare feature) ─────────────────
    public String resumeText = "";

    // ── Convenience getters for Thymeleaf ────────────────────
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public boolean isHasLinkedIn() { return hasLinkedIn; }
    public boolean isHasGitHub() { return hasGitHub; }
    public int getEstimatedYears() { return estimatedYears; }
    public int getOverallScore() { return overallScore; }
    public Map<String, Integer> getScoreBreakdown() { return scoreBreakdown; }
    public List<String> getSkills() { return skills; }
    public Map<String, List<String>> getSkillsByCategory() { return skillsByCategory; }
    public Map<String, Boolean> getSections() { return sections; }
    public List<String> getTips() { return tips; }
    public List<JobMatch> getJobMatches() { return jobMatches; }
    public String getResumeText() { return resumeText; }
}
