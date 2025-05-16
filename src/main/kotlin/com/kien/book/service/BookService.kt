package com.kien.book.service

import com.kien.book.common.*
import com.kien.book.model.Book
import com.kien.book.model.dto.book.*
import com.kien.book.repository.BookMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLIntegrityConstraintViolationException
import java.time.LocalDateTime
import kotlin.math.ceil
import kotlin.reflect.full.memberProperties

@Service
class BookService(
    private val bookMapper: BookMapper,
    private val batchService: BatchService,
) {

    @Value("\${messages.errors.unexpectedError}")
    val MSG_UNEXPECTED_ERROR: String = ""

    @Value("\${messages.errors.invalidValue}")
    val MSG_INVALID_VALUE: String = ""

    @Value("\${messages.errors.nonExistentBook}")
    val MSG_NON_EXISTENT_BOOK: String = ""

    @Value("\${messages.errors.nonExistentFK}")
    val MSG_NONEXISTENT_FK: String = ""

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
        require(bookUpdate.id >= 1) {
            throw InvalidParamCustomException(
                message = MSG_INVALID_VALUE,
                field = Book::id.name,
                value = bookUpdate.id
            )
        }
        val bookId = bookUpdate.id

        if (bookUpdate.publisherId != null && bookUpdate.publisherId <= 0) {
            throw InvalidParamCustomException(
                message = MSG_INVALID_VALUE,
                field = Book::publisherId.name,
                value = bookUpdate.publisherId
            )
        }
        if (bookUpdate.userId != null && bookUpdate.userId <= 0) {
            throw InvalidParamCustomException(
                message = MSG_INVALID_VALUE,
                field = Book::userId.name,
                value = bookUpdate.userId
            )
        }
        if (bookUpdate.price != null && bookUpdate.price < 0) {
            throw InvalidParamCustomException(
                message = MSG_INVALID_VALUE,
                field = Book::price.name,
                value = bookUpdate.price
            )
        }

        val book = bookUpdate.toEntity(
            updatedAt = LocalDateTime.now()
        )

        var updatedCount: Int = -1
        try {
            updatedCount = bookMapper.update(book)
        } catch (e: DataIntegrityViolationException) {
            if (isForeignKeyViolation(e)) {
                val errorMsg = e.message ?: ""
                val propertyName = extractForeignKeyColumn(errorMsg)?.toCamelCase() ?: ""
                val property = BookUpdate::class.memberProperties.find { it.name == propertyName }
                val propertyValue = property?.get(bookUpdate)
                throw NonExistentForeignKeyCustomException(
                    message = MSG_NONEXISTENT_FK,
                    field = propertyName,
                    value = propertyValue
                )
            }
            throw e
        }

        if (updatedCount <= 0) {
            throw NotFoundCustomException(
                message = MSG_NON_EXISTENT_BOOK,
                field = Book::id.name,
                value = bookId
            )
        }

        return BookUpdatedResponse(
            id = bookId,
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

    private fun isForeignKeyViolation(e: DataIntegrityViolationException): Boolean {
        val rootCause = e.rootCause
        return rootCause is SQLIntegrityConstraintViolationException && rootCause.errorCode == 1452
    }

    private fun extractForeignKeyColumn(errorMessage: String): String? {
        val regex = Regex("FOREIGN KEY \\(`(\\w+)`\\)")
        val matchResult = regex.find(errorMessage)
        return matchResult?.groupValues?.get(1)
    }

    private fun String.toCamelCase(): String {
        return this.split("_").mapIndexed { index, word ->
            if (index == 0) word else word.replaceFirstChar { it.uppercase() }
        }.joinToString("")
    }
}
