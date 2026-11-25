package com.intellihire.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellihire.model.AnalysisResult;
import com.intellihire.model.Candidate;
import com.intellihire.repository.CandidateRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** /compare and /compare-result routes. */
@Controller
public class CompareController {

    @Autowired private CandidateRepository candidateRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/compare")
    public String comparePage(Model model, HttpSession session) {
        model.addAttribute("candidates", candidateRepo.findAll());
        model.addAttribute("currentPage", "compare");
        DashboardController.addSessionAttrs(model, session);
        return "compare";
    }

    @PostMapping("/compare")
    public String compareResumes(@RequestParam String id1,
                                 @RequestParam String id2,
                                 Model model, HttpSession session) {
        var opt1 = candidateRepo.findById(id1);
        var opt2 = candidateRepo.findById(id2);

        if (opt1.isEmpty() || opt2.isEmpty()) {
            model.addAttribute("error", "Could not find one or both candidates.");
            model.addAttribute("candidates", candidateRepo.findAll());
            return "compare";
        }

        try {
            Candidate c1 = opt1.get();
            Candidate c2 = opt2.get();
            AnalysisResult r1 = mapper.readValue(c1.getData(), AnalysisResult.class);
            AnalysisResult r2 = mapper.readValue(c2.getData(), AnalysisResult.class);

            // Skill diff
            Set<String> s1 = new HashSet<>(r1.skills);
            Set<String> s2 = new HashSet<>(r2.skills);
            List<String> onlyIn1 = new ArrayList<>(s1); onlyIn1.removeAll(s2);
            List<String> onlyIn2 = new ArrayList<>(s2); onlyIn2.removeAll(s1);
            List<String> both = new ArrayList<>(s1); both.retainAll(s2);

            model.addAttribute("candidate1", c1);
            model.addAttribute("candidate2", c2);
            model.addAttribute("result1", r1);
            model.addAttribute("result2", r2);
            model.addAttribute("onlyIn1", onlyIn1);
            model.addAttribute("onlyIn2", onlyIn2);
            model.addAttribute("commonSkills", both);
            model.addAttribute("currentPage", "compare");
            DashboardController.addSessionAttrs(model, session);
        } catch (Exception e) {
            model.addAttribute("error", "Comparison failed: " + e.getMessage());
        }
        return "compare_result";
    }
}
