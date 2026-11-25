package com.intellihire.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellihire.model.AnalysisResult;
import com.intellihire.model.Candidate;
import com.intellihire.repository.CandidateRepository;
import com.intellihire.service.JobMatchService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;
import java.util.stream.Collectors;

/** Dashboard: KPI cards, charts, candidate table. */
@Controller
public class DashboardController {

    @Autowired private CandidateRepository candidateRepo;
    @Autowired private JobMatchService jobMatchService;
    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/")
    public String dashboard(Model model, HttpSession session) {
        List<Candidate> candidates = candidateRepo.findAll();
        int total = candidates.size();
        double avgScore = total > 0
            ? candidates.stream().mapToInt(Candidate::getScore).average().orElse(0) : 0;

        int bestMatch = candidates.stream()
            .mapToInt(Candidate::getTopMatch).max().orElse(0);

        int openJobs = jobMatchService.getAllJobs().size();

        // Score distribution buckets for bar chart
        int[] buckets = new int[5]; // 0-20, 21-40, 41-60, 61-80, 81-100
        for (Candidate c : candidates) {
            int idx = Math.min(4, c.getScore() / 20);
            buckets[idx]++;
        }

        // Top skills across all candidates
        Map<String, Integer> skillFreq = new HashMap<>();
        for (Candidate c : candidates) {
            try {
                AnalysisResult ar = mapper.readValue(c.getData(), AnalysisResult.class);
                for (String skill : ar.skills) skillFreq.merge(skill, 1, Integer::sum);
            } catch (Exception ignored) {}
        }
        // Top 10 skills
        List<Map.Entry<String, Integer>> topSkills = skillFreq.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10).collect(Collectors.toList());

        model.addAttribute("candidates", candidates);
        model.addAttribute("totalCandidates", total);
        model.addAttribute("avgScore", Math.round(avgScore));
        model.addAttribute("bestMatch", bestMatch);
        model.addAttribute("openJobs", openJobs);
        model.addAttribute("scoreBuckets", buckets);
        model.addAttribute("topSkills", topSkills);
        model.addAttribute("currentPage", "dashboard");
        addSessionAttrs(model, session);
        return "dashboard";
    }

    static void addSessionAttrs(Model model, HttpSession session) {
        model.addAttribute("sessionUsername", session.getAttribute("username"));
        model.addAttribute("sessionRole", session.getAttribute("role"));
    }
}
