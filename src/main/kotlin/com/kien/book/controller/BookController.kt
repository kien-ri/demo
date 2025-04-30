package com.kien.book.controller;

import com.kien.book.common.Page
import com.kien.book.model.Book
import com.kien.book.model.dto.book.*
import com.kien.book.service.BookService
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.beans.BeanUtils
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books")
class BookController(private val bookService: BookService) {

    @GetMapping("/{id}")
    fun getBookById(@PathVariable id: Long): ResponseEntity<BookView> {
        val bookView = bookService.getBookById(id)
        return if (bookView == null) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(bookView)
        }
    }

    @GetMapping
    fun getBooksByCondition(@Valid bookCondition: BookCondition): ResponseEntity<Page<BookView>> {
        val bookPage = bookService.getBooksByCondition(bookCondition);
        return if (bookPage.content.isNotEmpty()) {
            ResponseEntity.ok(bookPage)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun registerBook(@Valid @RequestBody bookCreate: BookCreate): ResponseEntity<Void> {
        bookService.registerBook(bookCreate)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/batch")
    fun registerBooks(@Valid @RequestBody bookCreates: List<BookCreate>): ResponseEntity<Void> {
        bookService.registerBooks(bookCreates)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}")
    fun deleteBook(@PathVariable @Positive id: Long): ResponseEntity<Void> {
        bookService.deleteBook(id)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/batch")
    fun deleteBooks(@RequestBody @Valid deleteReq: BooksDelete): ResponseEntity<Void> {
        bookService.deleteBooks(deleteReq.ids)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{id}")
    fun updateBook(@PathVariable @Positive id: Long, @RequestBody @Valid bookUpdate: BookUpdate): ResponseEntity<Void> {
        val book = bookUpdate.toEntity()
        bookService.updateBook(book)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/batch")
    fun updateBooks(@RequestBody @Valid bookUpdates: List<BookUpdate>): ResponseEntity<Void> {
        val books = bookUpdates.map { it.toEntity() }
        bookService.updateBooks(books)
        return ResponseEntity.noContent().build()
    }

}
