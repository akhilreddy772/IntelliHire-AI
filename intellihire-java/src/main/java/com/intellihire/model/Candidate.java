package com.intellihire.model;

/** Candidate record stored in the database. */
public class Candidate {
    private String id;
    private String name;
    private String email;
    private String filename;
    private int score;
    private int years;
    private String topJob;
    private int topMatch;
    private String data;      // JSON-serialized AnalysisResult
    private String created;

    public Candidate() {}

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getYears() { return years; }
    public void setYears(int years) { this.years = years; }

    public String getTopJob() { return topJob; }
    public void setTopJob(String topJob) { this.topJob = topJob; }

    public int getTopMatch() { return topMatch; }
    public void setTopMatch(int topMatch) { this.topMatch = topMatch; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getCreated() { return created; }
    public void setCreated(String created) { this.created = created; }

    /** Score colour for UI: green >= 75, yellow >= 50, red otherwise */
    public String getScoreColor() {
        if (score >= 75) return "#22c55e";
        if (score >= 50) return "#f59e0b";
        return "#ef4444";
    }
}
