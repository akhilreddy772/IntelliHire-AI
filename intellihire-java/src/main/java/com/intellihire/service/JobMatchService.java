package com.intellihire.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellihire.model.AnalysisResult;
import com.intellihire.model.JobMatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads job openings and matches resumes against them.
 * Match % = 70% skill overlap + 30% TF-IDF cosine similarity.
 */
@Service
public class JobMatchService {

    @Autowired private TfIdfService tfIdf;
    @Autowired private JdbcTemplate jdbc;

    private final ObjectMapper mapper = new ObjectMapper();
    private List<Map<String, Object>> seededJobs = new ArrayList<>();

    @PostConstruct
    public void loadSeededJobs() {
        try {
            ClassPathResource res = new ClassPathResource("data/jobs.json");
            try (InputStream is = res.getInputStream()) {
                seededJobs = mapper.readValue(is, new TypeReference<>() {});
            }
        } catch (Exception e) {
            System.err.println("[JobMatch] Could not load jobs.json: " + e.getMessage());
        }
    }

    /** Return all jobs (seeded + custom) as JobMatch objects (without match data). */
    public List<JobMatch> getAllJobs() {
        List<JobMatch> all = new ArrayList<>();
        for (Map<String, Object> j : seededJobs) all.add(toJobMatch(j, false));
        // Load custom jobs from DB
        try {
            List<Map<String, Object>> customRows = jdbc.queryForList(
                "SELECT * FROM custom_jobs ORDER BY created DESC");
            for (Map<String, Object> row : customRows) {
                JobMatch jm = new JobMatch();
                jm.id = String.valueOf(row.get("id"));
                jm.title = String.valueOf(row.get("title"));
                jm.company = String.valueOf(row.get("company"));
                jm.location = String.valueOf(row.get("location"));
                jm.type = String.valueOf(row.get("type"));
                jm.salary = String.valueOf(row.get("salary"));
                jm.description = String.valueOf(row.get("description"));
                String skills = String.valueOf(row.get("skills"));
                jm.requiredSkills = Arrays.asList(skills.split(",\\s*"));
                jm.isCustom = true;
                all.add(jm);
            }
        } catch (Exception e) {
            System.err.println("[JobMatch] Error loading custom jobs: " + e.getMessage());
        }
        return all;
    }

    /**
     * Match a resume's AnalysisResult against all jobs.
     * Returns top N matches sorted by match%.
     */
    public List<JobMatch> match(AnalysisResult result, int topN) {
        List<JobMatch> allJobs = getAllJobs();
        Set<String> resumeSkillsLower = result.skills.stream()
                .map(String::toLowerCase).collect(Collectors.toSet());

        List<JobMatch> matches = new ArrayList<>();
        for (JobMatch job : allJobs) {
            // 1. Skill overlap score (70%)
            List<String> jobSkillsLower = job.requiredSkills.stream()
                    .map(String::toLowerCase).collect(Collectors.toList());
            long matched = jobSkillsLower.stream()
                    .filter(resumeSkillsLower::contains).count();
            double skillOverlap = jobSkillsLower.isEmpty() ? 0.0
                    : (double) matched / jobSkillsLower.size();

            // 2. TF-IDF cosine similarity (30%)
            String jobText = job.title + " " + job.description + " "
                    + String.join(" ", job.requiredSkills);
            double cosine = tfIdf.similarity(result.resumeText, jobText);

            // Combined match %
            int matchPct = (int) Math.round(skillOverlap * 70 + cosine * 30);
            matchPct = Math.max(0, Math.min(100, matchPct));

            JobMatch jm = new JobMatch();
            jm.id = job.id;
            jm.title = job.title;
            jm.company = job.company;
            jm.location = job.location;
            jm.type = job.type;
            jm.salary = job.salary;
            jm.description = job.description;
            jm.requiredSkills = job.requiredSkills;
            jm.matchPercent = matchPct;
            jm.isCustom = job.isCustom;

            jm.matchedSkills = jobSkillsLower.stream()
                    .filter(resumeSkillsLower::contains)
                    .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                    .collect(Collectors.toList());
            jm.missingSkills = jobSkillsLower.stream()
                    .filter(s -> !resumeSkillsLower.contains(s))
                    .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                    .collect(Collectors.toList());

            matches.add(jm);
        }

        matches.sort(Comparator.comparingInt(JobMatch::getMatchPercent).reversed());
        return matches.stream().limit(topN).collect(Collectors.toList());
    }

    private JobMatch toJobMatch(Map<String, Object> j, boolean isCustom) {
        JobMatch jm = new JobMatch();
        jm.id = String.valueOf(j.getOrDefault("id", ""));
        jm.title = String.valueOf(j.getOrDefault("title", ""));
        jm.company = String.valueOf(j.getOrDefault("company", ""));
        jm.location = String.valueOf(j.getOrDefault("location", ""));
        jm.type = String.valueOf(j.getOrDefault("type", ""));
        jm.salary = String.valueOf(j.getOrDefault("salary", ""));
        jm.description = String.valueOf(j.getOrDefault("description", ""));
        Object skills = j.get("required_skills");
        if (skills instanceof List) {
            jm.requiredSkills = (List<String>) skills;
        }
        jm.isCustom = isCustom;
        return jm;
    }
}
