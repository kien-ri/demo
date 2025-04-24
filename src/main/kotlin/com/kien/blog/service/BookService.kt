package com.kien.blog.service

import com.kien.blog.model.condition.BookCondition
import com.kien.blog.common.Page
import com.kien.blog.model.view.BookView
import com.kien.blog.repository.BookMapper
import org.springframework.stereotype.Service
import kotlin.math.ceil

@Service
class BookService(private val bookMapper: BookMapper) {
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
}