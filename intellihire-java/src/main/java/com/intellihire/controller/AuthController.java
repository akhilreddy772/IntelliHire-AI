package com.intellihire.controller;

import com.intellihire.model.User;
import com.intellihire.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Handles login, register, and logout. */
@Controller
public class AuthController {

    @Autowired private UserRepository userRepo;
    @Autowired private BCryptPasswordEncoder encoder;

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute("userId") != null) return "redirect:/";
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes ra) {
        var optUser = userRepo.findByUsername(username.trim());
        if (optUser.isEmpty() || !encoder.matches(password, optUser.get().getPasswordHash())) {
            ra.addFlashAttribute("error", "Invalid username or password.");
            return "redirect:/login";
        }
        User user = optUser.get();
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole());
        return "redirect:/";
    }

    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (session.getAttribute("userId") != null) return "redirect:/";
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam(defaultValue = "candidate") String role,
                           RedirectAttributes ra) {
        username = username.trim();
        if (username.length() < 3) {
            ra.addFlashAttribute("error", "Username must be at least 3 characters.");
            return "redirect:/register";
        }
        if (userRepo.existsByUsername(username)) {
            ra.addFlashAttribute("error", "Username already taken.");
            return "redirect:/register";
        }
        User u = new User();
        u.setId(UUID.randomUUID().toString().replace("-","").substring(0,8));
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(password));
        u.setRole(role);
        u.setCreated(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        userRepo.save(u);

        ra.addFlashAttribute("success", "Account created! Please log in.");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
