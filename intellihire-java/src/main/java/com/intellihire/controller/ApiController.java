package com.intellihire.controller;

import com.intellihire.model.Candidate;
import com.intellihire.repository.CandidateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/** REST API — GET /api/candidates returns JSON list. */
@RestController
public class ApiController {

    @Autowired private CandidateRepository candidateRepo;

    @GetMapping("/api/candidates")
    public List<Map<String, Object>> getCandidates() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Candidate c : candidateRepo.findAll()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId());
            row.put("name", c.getName());
            row.put("email", c.getEmail());
            row.put("score", c.getScore());
            row.put("topJob", c.getTopJob());
            row.put("topMatch", c.getTopMatch());
            row.put("created", c.getCreated());
            result.add(row);
        }
        return result;
    }
}
