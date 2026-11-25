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
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** Handles /bulk-upload route. */
@Controller
public class BulkUploadController {

    @Autowired private ResumeParserService parser;
    @Autowired private ResumeAnalyzerService analyzer;
    @Autowired private JobMatchService jobMatcher;
    @Autowired private CandidateRepository candidateRepo;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/bulk-upload")
    public String bulkPage(Model model, HttpSession session) {
        model.addAttribute("currentPage", "bulk");
        DashboardController.addSessionAttrs(model, session);
        return "bulk_upload";
    }

    @PostMapping("/bulk-upload")
    public String bulkUpload(@RequestParam("files") MultipartFile[] files,
                             Model model, HttpSession session,
                             RedirectAttributes ra) {
        if (files == null || files.length == 0) {
            ra.addFlashAttribute("error", "Please select at least one file.");
            return "redirect:/bulk-upload";
        }

        List<Map<String, Object>> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String fname = ResumeParserService.sanitiseFilename(file.getOriginalFilename());
            try {
                String text = parser.parse(file);
                AnalysisResult ar = analyzer.analyse(text);
                ar.jobMatches = jobMatcher.match(ar, 3);

                Candidate c = new Candidate();
                String id = UUID.randomUUID().toString().replace("-","").substring(0,10);
                c.setId(id);
                c.setName(ar.name);
                c.setEmail(ar.email);
                c.setFilename(fname);
                c.setScore(ar.overallScore);
                c.setYears(ar.estimatedYears);
                c.setTopJob(ar.jobMatches.isEmpty() ? "" : ar.jobMatches.get(0).title);
                c.setTopMatch(ar.jobMatches.isEmpty() ? 0 : ar.jobMatches.get(0).matchPercent);
                c.setData(mapper.writeValueAsString(ar));
                c.setCreated(now);
                candidateRepo.save(c);

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", id);
                row.put("name", ar.name);
                row.put("score", ar.overallScore);
                row.put("topJob", c.getTopJob());
                row.put("topMatch", c.getTopMatch());
                row.put("skills", ar.skills.size());
                row.put("filename", fname);
                results.add(row);
            } catch (Exception e) {
                errors.add(fname + ": " + e.getMessage());
            }
        }

        // Sort by score desc
        results.sort((a, b) -> (int) b.get("score") - (int) a.get("score"));

        model.addAttribute("results", results);
        model.addAttribute("errors", errors);
        model.addAttribute("currentPage", "bulk");
        DashboardController.addSessionAttrs(model, session);
        return "bulk_results";
    }
}
