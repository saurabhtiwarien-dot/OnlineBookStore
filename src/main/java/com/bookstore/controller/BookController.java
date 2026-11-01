package com.bookstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.bookstore.entity.Book;
import com.bookstore.service.BookService;

@Controller
public class BookController {

    @Autowired
    private BookService service;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("books", service.getAllBooks());
        return "index";
    }

    @GetMapping("/add")
    public String addBook(Model model) {
        model.addAttribute("book", new Book());
        return "addBook";
    }

    @PostMapping("/save")
    public String saveBook(@ModelAttribute Book book) {
        service.saveBook(book);
        return "redirect:/";
    }
    
    @GetMapping("/edit/{id}")
    public String editBook(@PathVariable Long id, Model model) {

        Book book = service.getBookById(id);
        model.addAttribute("book", book);

        return "editBook";
    }
    
    @GetMapping("/details/{id}")
    public String bookDetails(@PathVariable Long id, Model model) {

        Book book = service.getBookById(id);

        model.addAttribute("book", book);

        return "bookDetails";
    }

    @PostMapping("/update")
    public String updateBook(@ModelAttribute Book book) {

        service.saveBook(book);

        return "redirect:/";
    }

    @GetMapping("/delete/{id}")
    public String deleteBook(@PathVariable Long id) {

        service.deleteBook(id);

        return "redirect:/";
    }
}