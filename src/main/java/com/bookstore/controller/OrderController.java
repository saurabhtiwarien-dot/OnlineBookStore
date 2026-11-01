package com.bookstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.bookstore.entity.Book;
import com.bookstore.entity.Order;
import com.bookstore.entity.User;
import jakarta.servlet.http.HttpSession;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.OrderRepository;

@Controller
public class OrderController {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private OrderRepository orderRepository;


    @GetMapping("/buy/{id}")
    public String buyBook(
            @PathVariable Long id,
            HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        Book book = bookRepository.findById(id).orElse(null);

        if (book == null) {
            return "redirect:/";
        }

        if (book.getQuantity() <= 0) {
            return "redirect:/";
        }

        // Store the selected book for Buy Now checkout
        session.setAttribute("buyNowBookId", book.getId());
        session.setAttribute("buyNowQuantity", 1);

        // Open the common checkout page
        return "redirect:/checkout?buyNow=true";
    }


    @GetMapping("/orders")
    public String viewOrders(
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "orders",
                orderRepository.findByUserId(user.getId())
        );

        return "orders";
    }


    @GetMapping("/order/{id}")
    public String orderDetails(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        Order order = orderRepository.findById(id).orElse(null);

        if (order == null) {
            return "redirect:/orders";
        }

        // Security check
        if (order.getUserId() == null ||
            !order.getUserId().equals(user.getId())) {

            return "redirect:/orders";
        }

        model.addAttribute("order", order);

        return "orderDetails";
    }


    @GetMapping("/order/cancel/{id}")
    public String cancelOrder(
            @PathVariable Long id,
            HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        Order order = orderRepository.findById(id).orElse(null);

        if (order == null) {
            return "redirect:/orders";
        }

        // Security check
        if (order.getUserId() == null ||
            !order.getUserId().equals(user.getId())) {

            return "redirect:/orders";
        }
        
        
        // Only PLACED orders can be cancelled
        if (!"PLACED".equals(order.getStatus())) {
            return "redirect:/orders";
        }


        // Restore stock using Book ID
        if (order.getBookId() != null) {

            Book book = bookRepository
                    .findById(order.getBookId())
                    .orElse(null);

            if (book != null) {

                book.setQuantity(
                        book.getQuantity() + order.getQuantity()
                );

                bookRepository.save(book);
            }
        }


        // Mark order as cancelled
        order.setStatus("CANCELLED");
        orderRepository.save(order);

        return "redirect:/orders";
    }
}