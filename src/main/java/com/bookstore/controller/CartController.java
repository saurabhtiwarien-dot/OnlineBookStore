package com.bookstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.bookstore.entity.Book;
import com.bookstore.entity.Cart;
import com.bookstore.entity.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class CartController {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private BookRepository bookRepository;


    // Add book to cart
    @GetMapping("/cart/add/{id}")
    public String addToCart(
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

        Cart existingCart =
                cartRepository.findByUserIdAndBookId(
                        user.getId(),
                        book.getId());

        if (existingCart != null) {

            existingCart.setQuantity(
                    existingCart.getQuantity() + 1);

            cartRepository.save(existingCart);

        } else {

            Cart cart = new Cart();

            cart.setUserId(user.getId());
            cart.setBookId(book.getId());
            cart.setBookTitle(book.getTitle());
            cart.setPrice(book.getPrice());
            cart.setQuantity(1);

            cartRepository.save(cart);
        }

        return "redirect:/cart";
    }


    // View cart
    @GetMapping("/cart")
    public String viewCart(
            HttpSession session,
            Model model) {

        User user =
                (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "cartItems",
                cartRepository.findByUserId(user.getId()));

        return "cart";
    }


    // Remove item from cart
    @GetMapping("/cart/remove/{id}")
    public String removeFromCart(
            @PathVariable Long id,
            HttpSession session) {

        User user =
                (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        Cart cart =
                cartRepository.findById(id).orElse(null);

        if (cart != null &&
            cart.getUserId().equals(user.getId())) {

            cartRepository.delete(cart);
        }

        return "redirect:/cart";
    }
}