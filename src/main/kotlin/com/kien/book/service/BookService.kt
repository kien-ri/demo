package com.kien.book.service

import com.kien.book.common.CustomException
import com.kien.book.common.Page
import com.kien.book.model.Book
import com.kien.book.model.dto.book.*
import com.kien.book.repository.BookMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.ceil

@Service
class BookService(
    private val bookMapper: BookMapper,
    private val batchService: BatchService,
) {

    @Value("\${messages.errors.unexpectedError}")
    val MSG_UNEXPECTED_ERROR: String = ""

    @Value("\${messages.errors.invalidValue}")
    val MSG_INVALID_VALUE: String = ""

    @Value("\${messages.errors.invalidBookId}")
    val MSG_INVALID_BOOK_ID: String = ""

    @Value("\${messages.errors.invalidPirce}")
    val MSG_INVALID_PRICE: String = ""

    @Value("\${messages.errors.invalidPublisherId}")
    val MSG_INVALID_PUBLISHER_ID: String = ""

    @Value("\${messages.errors.invalidUserId}")
    val MSG_INVALID_USER_ID: String = ""

    @Value("\${messages.errors.nonExistentBook}")
    val MSG_NON_EXISTENT_BOOK: String = ""

    fun getBookById(id: Long): BookView? {
        require(id >= 1) { throw CustomException(MSG_INVALID_VALUE) }
        return bookMapper.getById(id)
    }

    fun getBooksByCondition(bookCondition: BookCondition): Page<BookView> {
        if (bookCondition.minPrice != null) {
            require(bookCondition.minPrice >= 0) {
                throw CustomException("エラー：下限金額にマイナスの値を指定できません。")
            }
        }
        if (bookCondition.maxPrice != null) {
            require(bookCondition.maxPrice >= 0) {
                throw CustomException("エラー：上限金額にマイナスの値を指定できません。")
            }
        }
        if (bookCondition.minPrice != null && bookCondition.maxPrice != null) {
            require(bookCondition.minPrice <= bookCondition.maxPrice) {
                throw CustomException("エラー：下限金額が上限金額より大きいです。")
            }
        }

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

    fun deleteBookLogically(id: Long) {
        bookMapper.deleteLogically(id)
    }

    fun deleteBooksLogically(ids: List<Long>) {
        bookMapper.deleteBatchLogically(ids)
    }

    @Transactional
    fun updateBook(bookUpdate: BookUpdate): BookUpdatedResponse {
        require(bookUpdate.id >= 1) {throw CustomException(MSG_INVALID_BOOK_ID)}
        if (bookUpdate.publisherId != null && bookUpdate.publisherId <= 0) throw CustomException(MSG_INVALID_PUBLISHER_ID)
        if (bookUpdate.userId != null && bookUpdate.userId <= 0) throw CustomException(MSG_INVALID_USER_ID)
        if (bookUpdate.price != null && bookUpdate.price < 0) throw CustomException(MSG_INVALID_PRICE)

        val book = bookUpdate.toEntity().apply {
            updatedAt = LocalDateTime.now()
        }

        val updatedCount = bookMapper.update(book)
        if (updatedCount <= 0) throw CustomException(MSG_NON_EXISTENT_BOOK)

        return BookUpdatedResponse(
            id = book.id ?: throw CustomException(MSG_UNEXPECTED_ERROR),
            title = book.title
        )
    }

    fun updateBooks(books: List<Book>) {
        batchService.batchProcess(
            dataList = books,
            mapperClass = BookMapper::class.java,
            operation = BookMapper::update
        )
    }
}
