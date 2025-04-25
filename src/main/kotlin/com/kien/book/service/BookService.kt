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
        bookCondition.title?.let { if (it.isNotEmpty()) bookCondition.title = "%$it%" }
        bookCondition.author?.let { if (it.isNotEmpty()) bookCondition.author = "%$it%" }

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

    fun registerBook(bookCreate: BookCreate): Int {
        return bookMapper.save(bookCreate)
    }

    fun registerBooks(bookCreates: List<BookCreate>): Boolean {
        val res = batchService.batchInsert(
            dataList = bookCreates,
            mapperClass = BookMapper::class.java,
            insertOperation = BookMapper::save
        )
        return res == bookCreates.size
    }

    fun deleteBook(id: Long): Int {
        return bookMapper.delete(id)
    }

    fun deleteBooks(ids: List<Long>): Int {
        return bookMapper.deleteBatch(ids)
    }

    @Transactional
    fun updateBook(book: Book): BookView {
        val updatedCount = bookMapper.update(book)
        if (updatedCount > 0) {
            val bookId = book.id ?: throw Exception("...")
            val bookView = bookMapper.getById(bookId)
            return bookView ?: throw Exception("...")
        } else {
            throw Exception("...")
        }
    }
}