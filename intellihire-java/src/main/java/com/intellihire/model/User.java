package com.intellihire.model;

/** Application user (recruiter or candidate role). */
public class User {
    private String id;
    private String username;
    private String passwordHash;
    private String role;   // "recruiter" or "candidate"
    private String created;

    public User() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCreated() { return created; }
    public void setCreated(String created) { this.created = created; }

    public boolean isRecruiter() { return "recruiter".equals(role); }
}
