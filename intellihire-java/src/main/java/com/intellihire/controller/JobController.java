package com.intellihire.controller;

import com.intellihire.model.JobMatch;
import com.intellihire.service.JobMatchService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** /jobs and /custom-job routes. */
@Controller
public class JobController {

    @Autowired private JobMatchService jobMatchService;
    @Autowired private JdbcTemplate jdbc;

    @GetMapping("/jobs")
    public String jobsPage(Model model, HttpSession session) {
        List<JobMatch> jobs = jobMatchService.getAllJobs();
        model.addAttribute("jobs", jobs);
        model.addAttribute("currentPage", "jobs");
        DashboardController.addSessionAttrs(model, session);
        return "jobs";
    }

    @GetMapping("/custom-job")
    public String customJobPage(Model model, HttpSession session) {
        model.addAttribute("currentPage", "custom-job");
        DashboardController.addSessionAttrs(model, session);
        return "custom_job";
    }

    @PostMapping("/custom-job")
    public String createCustomJob(
            @RequestParam String title,
            @RequestParam String company,
            @RequestParam String location,
            @RequestParam String type,
            @RequestParam String salary,
            @RequestParam String description,
            @RequestParam String skills,
            RedirectAttributes ra) {
        try {
            String id = "custom-" + UUID.randomUUID().toString().substring(0, 6);
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            jdbc.update(
                "INSERT INTO custom_jobs (id,title,company,location,type,salary,description,skills,created) VALUES (?,?,?,?,?,?,?,?,?)",
                id, title, company, location, type, salary, description, skills, now
            );
            ra.addFlashAttribute("success", "Job posting created successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create job: " + e.getMessage());
        }
        return "redirect:/jobs";
    }
}
