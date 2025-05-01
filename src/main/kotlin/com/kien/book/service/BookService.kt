package com.kien.book.service

import com.kien.book.model.dto.book.BookCondition
import com.kien.book.common.Page
import com.kien.book.model.Book
import com.kien.book.model.dto.book.BookCreate
import com.kien.book.model.dto.book.BookView
import com.kien.book.repository.BookMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.ceil

@Service
class BookService(
    private val bookMapper: BookMapper,
    private val batchService: BatchService
) {
    fun getBookById(id: Long): BookView? {
        return bookMapper.getById(id)
    }

    fun getBooksByCondition(bookCondition: BookCondition): Page<BookView> {
        val totalCount = bookMapper.getCountByCondition(bookCondition);

        val bookViews: List<BookView>
        val totalPages: Int
        if (totalCount > 0) {
            totalPages = ceil(totalCount.toDouble() / bookCondition.pageSize.toDouble()).toInt()
            bookViews = bookMapper.getListByCondition(bookCondition)
        } else {
            totalPages = 0
            bookViews = emptyList()
        }
        val actualCurrentPage = if (bookCondition.currentPage > totalPages) totalPages else bookCondition.currentPage

        return Page(
            pageSize = bookCondition.pageSize,
            currentPage = actualCurrentPage,
            totalCount = totalCount,
            totalPages = totalPages,
            content = bookViews
        )
    }

    fun registerBook(bookCreate: BookCreate) {
        bookMapper.save(bookCreate)
    }

    fun registerBooks(bookCreates: List<BookCreate>) {
        batchService.batchProcess(
            dataList = bookCreates,
            mapperClass = BookMapper::class.java,
            operation = BookMapper::save
        )
    }

    fun deleteBook(id: Long) {
        bookMapper.delete(id)
    }

    fun deleteBooks(ids: List<Long>) {
        bookMapper.deleteBatch(ids)
    }

    @Transactional
    fun updateBook(book: Book) {
        bookMapper.update(book)
    }

    fun updateBooks(books: List<Book>) {
        batchService.batchProcess(
            dataList = books,
            mapperClass = BookMapper::class.java,
            operation = BookMapper::update
        )
    }
}