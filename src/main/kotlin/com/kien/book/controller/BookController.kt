package com.kien.book.controller;

import com.kien.book.model.dto.book.BookCondition
import com.kien.book.common.Page
import com.kien.book.model.Book
import com.kien.book.model.dto.book.BookCreate
import com.kien.book.model.dto.book.BooksDelete
import com.kien.book.model.dto.book.BookView
import com.kien.book.service.BookService
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping;
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

    @PostMapping
    fun registerBook(@Valid @RequestBody bookCreate: BookCreate): ResponseEntity<Int> {
        val result = bookService.registerBook(bookCreate)
        return if (result > 0) {
            ResponseEntity.ok(result)
        } else{
            ResponseEntity.status(500).build()
        }
    }

    @PostMapping("/batch")
    fun registerBooks(@Valid @RequestBody bookCreates: List<BookCreate>): ResponseEntity<Boolean> {
        val result = bookService.registerBooks(bookCreates)
        return if (result) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.status(500).build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteBook(@PathVariable @Positive id: Long): ResponseEntity<Void> {
        val result = bookService.deleteBook(id)
        return if (result > 0) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.status(500).build()
        }
    }

    @DeleteMapping("/batch")
    fun deleteBooks(@RequestBody @Valid deleteReq: BooksDelete): ResponseEntity<Void> {
        val ids = deleteReq.ids
        val deleted = bookService.deleteBooks(ids)
        return if (deleted == ids.size) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.status(500).build()
        }
    }

    @PutMapping("/{id}")
    fun updateBook(@PathVariable @Positive id: Long, @RequestBody book: Book): ResponseEntity<BookView> {
        val updatedBook = bookService.updateBook(book)
        return ResponseEntity.ok(updatedBook)
    }

}
