package com.kien.book.controller;

import com.kien.book.model.condition.BookCondition
import com.kien.book.common.Page
import com.kien.book.model.view.BookView
import com.kien.book.service.BookService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books")
class BookController(private val bookService: BookService) {

    @GetMapping("/{id}")
    fun getBookById(@PathVariable id: Long): ResponseEntity<BookView> {
        val bookView = bookService.getBookById(id)
        return if (bookView != null) {
            ResponseEntity.ok(bookView)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping
    fun getBooksByCondition(@Valid bookConditoin: BookCondition): ResponseEntity<Page<BookView>> {
        val bookPage = bookService.getBooksByCondition(bookConditoin);
        return ResponseEntity.ok(bookPage)
    }
}
