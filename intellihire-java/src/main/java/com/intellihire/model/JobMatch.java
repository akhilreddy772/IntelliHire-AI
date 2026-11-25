package com.intellihire.model;

import java.util.ArrayList;
import java.util.List;

/** Represents a single job opening and how well a resume matches it. */
public class JobMatch {
    public String id = "";
    public String title = "";
    public String company = "";
    public String location = "";
    public String type = "";
    public String salary = "";
    public String description = "";
    public List<String> requiredSkills = new ArrayList<>();
    public int matchPercent = 0;
    public List<String> matchedSkills = new ArrayList<>();
    public List<String> missingSkills = new ArrayList<>();
    public boolean isCustom = false;

    // Getters for Thymeleaf
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getLocation() { return location; }
    public String getType() { return type; }
    public String getSalary() { return salary; }
    public String getDescription() { return description; }
    public List<String> getRequiredSkills() { return requiredSkills; }
    public int getMatchPercent() { return matchPercent; }
    public List<String> getMatchedSkills() { return matchedSkills; }
    public List<String> getMissingSkills() { return missingSkills; }
    public boolean isCustom() { return isCustom; }
}
