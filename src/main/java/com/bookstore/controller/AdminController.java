package com.bookstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookstore.entity.User;
import com.bookstore.entity.Order;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class AdminController {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/admin")
    public String adminDashboard(
            HttpSession session,
            Model model) {

        User user =
                (User) session.getAttribute("loggedInUser");

        // Check if user is logged in
        if (user == null) {
            return "redirect:/login";
        }

        // Check if user is ADMIN
        if (!"ADMIN".equals(user.getRole())) {
            return "redirect:/";
        }

        // Dashboard statistics
        long totalBooks = bookRepository.count();
        long totalUsers = userRepository.count();
        long totalOrders = orderRepository.count();

        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("admin", user);

        return "adminDashboard";
    }
    
    @GetMapping("/admin/books")
    public String adminBooks(
            HttpSession session,
            Model model) {

        User user =
                (User) session.getAttribute("loggedInUser");

        // Check if user is logged in
        if (user == null) {
            return "redirect:/login";
        }

        // Check if user is ADMIN
        if (!"ADMIN".equals(user.getRole())) {
            return "redirect:/";
        }

        // Get all books
        model.addAttribute(
                "books",
                bookRepository.findAll());

        model.addAttribute("admin", user);

        return "adminBooks";
    }
    
    @GetMapping("/admin/orders")
    public String adminOrders(
            HttpSession session,
            Model model) {

        User user =
                (User) session.getAttribute("loggedInUser");

        // Check if user is logged in
        if (user == null) {
            return "redirect:/login";
        }

        // Check if user is ADMIN
        if (!"ADMIN".equals(user.getRole())) {
            return "redirect:/";
        }

        // Get all orders
        model.addAttribute(
                "orders",
                orderRepository.findAll());

        model.addAttribute("admin", user);

        return "adminOrders";
    }
    
    @GetMapping("/admin/users")
    public String adminUsers(
            HttpSession session,
            Model model) {

        User user =
                (User) session.getAttribute("loggedInUser");

        // Check if user is logged in
        if (user == null) {
            return "redirect:/login";
        }

        // Check if user is ADMIN
        if (!"ADMIN".equals(user.getRole())) {
            return "redirect:/";
        }

        // Get all registered users
        model.addAttribute(
                "users",
                userRepository.findAll());

        model.addAttribute("admin", user);

        return "adminUsers";
    }
    
    @PostMapping("/admin/order/update-status/{id}")
    public String updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status,
            HttpSession session) {

        User user =
                (User) session.getAttribute("loggedInUser");

        // Check if user is logged in
        if (user == null) {
            return "redirect:/login";
        }

        // Check if user is ADMIN
        if (!"ADMIN".equals(user.getRole())) {
            return "redirect:/";
        }

        // Find order
        Order order =
                orderRepository.findById(id).orElse(null);

        if (order == null) {
            return "redirect:/admin/orders";
        }

        // Update status
        order.setStatus(status);
        orderRepository.save(order);

        return "redirect:/admin/orders";
    }
}