package com.kien.book.service

import com.kien.book.common.CustomException
import com.kien.book.common.DuplicateKeyCustomException
import com.kien.book.common.NonExistentForeignKeyCustomException
import com.kien.book.model.dto.book.BookCondition
import com.kien.book.common.Page
import com.kien.book.model.Book
import com.kien.book.model.dto.book.BookCreate
import com.kien.book.model.dto.book.BookCreatedResponse
import com.kien.book.model.dto.book.BookView
import com.kien.book.repository.BookMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.cglib.core.Local
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
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

    @Value("\${messages.errors.invalidValue}")
    val MSG_INVALID_VALUE: String = ""

    @Value("\${messages.errors.insertError}")
    val MSG_INSERT_ERROR: String = ""

    @Value("\${messages.errors.noIdGenerated}")
    val MSG_NO_ID_GENERATED: String = ""

    @Value("\${messages.errors.invalidBookId}")
    val MSG_INVALID_BOOK_ID: String = ""

    @Value("\${messages.errors.invalidPirce}")
    val MSG_INVALID_PRICE: String = ""

    @Value("\${messages.errors.invalidPublisherId}")
    val MSG_INVALID_PUBLISHER_ID: String = ""

    @Value("\${messages.errors.invalidUserId}")
    val MSG_INVALID_USER_ID: String = ""

    @Value("\${messages.errors.nonExistentFK}")
    val MSG_NONEXISTENT_FK: String = ""

    @Value("\${messages.errors.duplicateKey}")
    val MSG_DUPLICATE_KEY: String = ""

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

    /**
     *  書籍情報保存、保存された書籍情報のidとタイトルを戻す
     *
     *  MyBatis の useGeneratedKeys 機能を使用して、INSERT 操作後にデータベースで自動生成された主キー ID が
     *  book オブジェクトの id フィールドに設定されます。
     *  また、ID を明示的に指定した場合、指定した ID が使用され、自動生成は行われません。
     */
    @Transactional
    fun registerBook(bookCreate: BookCreate): BookCreatedResponse {

        // バリデーション
        bookCreate.id?.let { id ->
            if (id <= 0) {
                throw CustomException(
                    message = MSG_INVALID_BOOK_ID
                )
            }
        }
        if (bookCreate.publisherId != null && bookCreate.publisherId <= 0) {
            throw CustomException(
                message = MSG_INVALID_PUBLISHER_ID
            )
        }
        if (bookCreate.userId != null && bookCreate.userId <= 0) {
            throw CustomException(
                message = MSG_INVALID_USER_ID)
        }
        if (bookCreate.price != null && bookCreate.price < 0) {
            throw CustomException(
                message = MSG_INVALID_PRICE)
        }

        // 作成時間と更新時間を設定
        val currentTime = LocalDateTime.now()
        val book = bookCreate.toEntity(
            createdAt = currentTime,
            updatedAt = currentTime
        )

        var insertedCount: Int = -1
        try {
            insertedCount = bookMapper.save(book)
        } catch (e: DuplicateKeyException) {
            throw DuplicateKeyCustomException(
                message = MSG_DUPLICATE_KEY,
                field = Book::id.name,
                value = bookCreate.id
            )
        } catch (e: DataIntegrityViolationException) {
            if (isForeignKeyViolation(e)) {
                val errorMsg = e.message ?: ""
                val propertyName = extractForeignKeyColumn(errorMsg)?.toCamelCase() ?: ""
                val property = BookCreate::class.memberProperties.find { it.name == propertyName }
                val propertyValue = property?.get(bookCreate)
                throw NonExistentForeignKeyCustomException(
                    message = MSG_NONEXISTENT_FK,
                    field = propertyName,
                    value = propertyValue
                )
            }
            throw e
        }

        if (insertedCount <= 0) {
            throw CustomException(
                message = MSG_INSERT_ERROR
            )
        }
        val bookId = book.id ?: throw CustomException(message = MSG_NO_ID_GENERATED)

        return BookCreatedResponse(
            id = bookId,
            title = book.title
        )
    }

    fun registerBooks(bookCreates: List<BookCreate>) {
        val books = bookCreates.map { it.toEntity() }

        batchService.batchProcess(
            dataList = books,
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
