package com.bookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bookstore.entity.Book;

public interface BookRepository extends JpaRepository<Book, Long> {

    Book findByTitle(String title);
}