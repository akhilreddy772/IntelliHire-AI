package com.intellihire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * IntelliHire AI — Intelligent Resume Analyzer & Job Matching System
 * Java / Spring Boot version — identical features to the Python/Flask version.
 *
 * Run:  mvn spring-boot:run
 * Open: http://localhost:8080
 * Login: recruiter / recruiter123   OR   candidate / candidate123
 */
@SpringBootApplication
public class IntelliHireApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntelliHireApplication.class, args);
    }
}
