package com.bookstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.bookstore.entity.User;
import com.bookstore.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;


    // Show Register Page
    @GetMapping("/register")
    public String registerPage(Model model) {

        model.addAttribute("user", new User());

        return "register";
    }


    // Save New User
    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {

            model.addAttribute("error",
                    "Email already registered!");

            return "register";
        }

        user.setRole("USER");
        userRepository.save(user);
        return "redirect:/login";
    }


    // Show Login Page
    @GetMapping("/login")
    public String loginPage() {

        return "login";
    }


    // Process Login
    @PostMapping("/login")
    public String loginUser(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        User user = userRepository
                .findByEmail(email)
                .orElse(null);

        if (user != null &&
            user.getPassword().equals(password)) {

            // Store logged-in user in session
            session.setAttribute("loggedInUser", user);

            return "redirect:/";
        }

        model.addAttribute("error",
                "Invalid email or password!");

        return "login";
    }


    // Logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:/login";
    }
}