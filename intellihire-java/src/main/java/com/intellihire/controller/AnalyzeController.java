package com.intellihire.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellihire.model.AnalysisResult;
import com.intellihire.model.Candidate;
import com.intellihire.repository.CandidateRepository;
import com.intellihire.service.JobMatchService;
import com.intellihire.service.ResumeAnalyzerService;
import com.intellihire.service.ResumeParserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Handles /analyze and /result/{id} routes. */
@Controller
public class AnalyzeController {

    @Autowired private ResumeParserService parser;
    @Autowired private ResumeAnalyzerService analyzer;
    @Autowired private JobMatchService jobMatcher;
    @Autowired private CandidateRepository candidateRepo;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/analyze")
    public String analyzePage(Model model, HttpSession session) {
        model.addAttribute("currentPage", "analyze");
        DashboardController.addSessionAttrs(model, session);
        return "analyze";
    }

    @PostMapping("/analyze")
    public String analyzeResume(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false, defaultValue = "") String pastedText,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            String text;
            String filename = "pasted_text.txt";

            if (file != null && !file.isEmpty()) {
                text = parser.parse(file);
                filename = ResumeParserService.sanitiseFilename(file.getOriginalFilename());
                // Save file to uploads/
                Path dir = Paths.get(uploadDir);
                Files.createDirectories(dir);
                String uniqueName = UUID.randomUUID().toString().substring(0, 8) + "_" + filename;
                file.transferTo(new File(dir + "/" + uniqueName));
                filename = uniqueName;
            } else if (!pastedText.isBlank()) {
                text = parser.parsePastedText(pastedText);
            } else {
                ra.addFlashAttribute("error", "Please upload a file or paste resume text.");
                return "redirect:/analyze";
            }

            // Run analysis
            AnalysisResult result = analyzer.analyse(text);
            // Add job matches
            result.jobMatches = jobMatcher.match(result, 6);

            // Build Candidate record
            Candidate c = new Candidate();
            String id = UUID.randomUUID().toString().replace("-","").substring(0,10);
            c.setId(id);
            c.setName(result.name);
            c.setEmail(result.email);
            c.setFilename(filename);
            c.setScore(result.overallScore);
            c.setYears(result.estimatedYears);
            c.setTopJob(result.jobMatches.isEmpty() ? "" : result.jobMatches.get(0).title);
            c.setTopMatch(result.jobMatches.isEmpty() ? 0 : result.jobMatches.get(0).matchPercent);
            c.setData(mapper.writeValueAsString(result));
            c.setCreated(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            candidateRepo.save(c);

            return "redirect:/result/" + id;

        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/analyze";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Analysis failed: " + e.getMessage());
            return "redirect:/analyze";
        }
    }

    @GetMapping("/result/{id}")
    public String resultPage(@PathVariable String id, Model model, HttpSession session) {
        var opt = candidateRepo.findById(id);
        if (opt.isEmpty()) return "redirect:/";

        Candidate c = opt.get();
        try {
            AnalysisResult result = mapper.readValue(c.getData(), AnalysisResult.class);
            model.addAttribute("candidate", c);
            model.addAttribute("result", result);
            model.addAttribute("currentPage", "analyze");
            DashboardController.addSessionAttrs(model, session);
        } catch (Exception e) {
            return "redirect:/";
        }
        return "result";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable String id, RedirectAttributes ra) {
        candidateRepo.deleteById(id);
        ra.addFlashAttribute("success", "Candidate deleted.");
        return "redirect:/";
    }
}
