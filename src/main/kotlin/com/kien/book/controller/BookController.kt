package com.kien.book.controller;

import com.kien.book.common.Page
import com.kien.book.model.dto.book.*
import com.kien.book.service.BookService
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books")
class BookController(private val bookService: BookService) {

    @GetMapping("/{id}")
    fun getBookById(@PathVariable @Positive id: Long): ResponseEntity<BookView> {
        val bookView = bookService.getBookById(id)
        return if (bookView == null) {
            ResponseEntity.noContent().build()
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
    fun registerBook(@Valid @RequestBody bookCreate: BookCreate): ResponseEntity<BookBasicInfo> {
        val createdResponse = bookService.registerBook(bookCreate)
        return ResponseEntity.ok(createdResponse)
    }

    @PostMapping("/batch")
    fun registerBooks(@RequestBody bookCreates: List<BookCreate>): ResponseEntity<BookBatchProcessedResult> {
        val result = bookService.registerBooks(bookCreates)
        val httpStatus = result.httpStatus
        return when (httpStatus) {
            HttpStatus.OK -> ResponseEntity.ok(result)
            HttpStatus.BAD_REQUEST -> ResponseEntity.badRequest().body(result)
            else -> ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result)
        }
    }

    @DeleteMapping("/{id}")
    fun deleteBookLogically(@PathVariable @Positive id: Long): ResponseEntity<Void> {
        bookService.deleteBookLogically(id)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/batch")
    fun deleteBooksLogically(@RequestParam @Positive ids: List<Long>): ResponseEntity<Void> {
        bookService.deleteBooksLogically(ids)
        return ResponseEntity.noContent().build()
    }

    @PutMapping
    fun updateBook(@RequestBody @Valid bookUpdate: BookUpdate): ResponseEntity<BookUpdatedResponse> {
        val bookUpdatedResponse = bookService.updateBook(bookUpdate)
        return ResponseEntity.ok(bookUpdatedResponse)
    }

    @PutMapping("/batch")
    fun updateBooks(@RequestBody @Valid bookUpdates: List<BookUpdate>): ResponseEntity<Void> {
        val books = bookUpdates.map { it.toEntity() }
        bookService.updateBooks(books)
        return ResponseEntity.noContent().build()
    }

}
