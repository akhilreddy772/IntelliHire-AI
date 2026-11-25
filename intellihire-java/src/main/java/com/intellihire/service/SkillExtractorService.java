package com.intellihire.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;

/**
 * Extracts skills from resume text using a 100+ skill dictionary with
 * word-boundary regex matching (handles C++, C#, Node.js, CI/CD etc.)
 */
@Service
public class SkillExtractorService {

    // 100+ skills in 6 categories — same dictionary as the Python version
    private static final Map<String, List<String>> SKILL_CATEGORIES = new LinkedHashMap<>();

    static {
        SKILL_CATEGORIES.put("Programming", Arrays.asList(
            "python", "java", "javascript", "typescript", "c\\+\\+", "c#", "go", "rust",
            "kotlin", "swift", "ruby", "php", "scala", "r", "matlab", "dart", "perl",
            "bash", "powershell", "groovy", "elixir", "haskell", "lua", "julia"
        ));
        SKILL_CATEGORIES.put("Web", Arrays.asList(
            "react", "angular", "vue", "node\\.js", "express", "django", "flask",
            "spring boot", "html", "css", "sass", "rest api", "graphql", "webpack",
            "next\\.js", "svelte", "bootstrap", "tailwind", "jquery", "redux",
            "fastapi", "rails", "asp\\.net", "blazor"
        ));
        SKILL_CATEGORIES.put("Data & AI", Arrays.asList(
            "machine learning", "deep learning", "nlp", "tensorflow", "pytorch",
            "keras", "scikit-learn", "pandas", "numpy", "data analysis", "data science",
            "computer vision", "bert", "llm", "transformers", "hadoop", "spark",
            "tableau", "power bi", "matplotlib", "seaborn", "xgboost", "opencv",
            "data mining", "statistics", "a/b testing", "feature engineering"
        ));
        SKILL_CATEGORIES.put("Database", Arrays.asList(
            "sql", "mysql", "postgresql", "sqlite", "mongodb", "redis", "oracle",
            "sql server", "cassandra", "elasticsearch", "dynamodb", "firebase",
            "neo4j", "mariadb", "couchdb", "influxdb", "snowflake", "bigquery"
        ));
        SKILL_CATEGORIES.put("Cloud & DevOps", Arrays.asList(
            "aws", "azure", "gcp", "docker", "kubernetes", "ci/cd", "jenkins",
            "github actions", "terraform", "ansible", "linux", "git", "devops",
            "microservices", "serverless", "helm", "prometheus", "grafana",
            "nginx", "apache", "vagrant", "puppet", "chef", "gitlab", "bitbucket"
        ));
        SKILL_CATEGORIES.put("Soft Skills", Arrays.asList(
            "leadership", "communication", "teamwork", "problem solving", "agile",
            "scrum", "project management", "critical thinking", "time management",
            "collaboration", "mentoring", "presentation", "negotiation", "adaptability"
        ));
    }

    /**
     * Extract all skills from resume text.
     * Returns map of category -> list of matched skills.
     */
    public Map<String, List<String>> extractSkillsByCategory(String text) {
        String lower = text.toLowerCase();
        Map<String, List<String>> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : SKILL_CATEGORIES.entrySet()) {
            List<String> found = new ArrayList<>();
            for (String skill : entry.getValue()) {
                // Build word-boundary pattern — handles C++, C#, Node.js, CI/CD
                String pattern = buildPattern(skill);
                try {
                    if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(lower).find()) {
                        // Store the display name (unescape regex special chars)
                        found.add(skill.replace("\\+\\+", "++").replace("\\.", ".").replace("\\#", "#"));
                    }
                } catch (PatternSyntaxException ignored) {
                    // Skip malformed patterns
                }
            }
            if (!found.isEmpty()) {
                result.put(entry.getKey(), found);
            }
        }
        return result;
    }

    /** Flatten skillsByCategory to a single list. */
    public List<String> extractSkills(String text) {
        List<String> all = new ArrayList<>();
        for (List<String> skills : extractSkillsByCategory(text).values()) {
            all.addAll(skills);
        }
        return all;
    }

    /** Build a word-boundary regex pattern for a skill token. */
    private String buildPattern(String skill) {
        // Allow the skill to be surrounded by non-alphanumeric chars (but allow +, #, .)
        return "(?<![a-zA-Z0-9])" + skill + "(?![a-zA-Z0-9])";
    }

    public static Map<String, List<String>> getAllSkillCategories() {
        return SKILL_CATEGORIES;
    }
}
