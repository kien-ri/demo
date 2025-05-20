package com.kien.book.service

import com.kien.book.common.*
import com.kien.book.model.dto.book.BookCondition
import com.kien.book.common.Page
import com.kien.book.common.util.DBExceptionUtils
import com.kien.book.common.util.StringUtils.toCamelCase
import com.kien.book.common.util.ValidationUtils
import com.kien.book.model.Book
import com.kien.book.model.dto.book.*
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

    @Value("\${messages.errors.nonExistentBook}")
    val MSG_NON_EXISTENT_BOOK: String = ""

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
        ValidationUtils.validatePositiveId(
            id = id,
            fieldName = Book::id.name,
            errorMsg = MSG_INVALID_VALUE
        )
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
        // 作成時間と更新時間を設定
        val currentTime = LocalDateTime.now()
        val book = bookCreate.toEntity(
            createdAt = currentTime,
            updatedAt = currentTime
        )

        validateBookParam(book)

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
            if (DBExceptionUtils.isForeignKeyViolation(e)) {
                val errorMsg = e.message ?: ""
                val propertyName = DBExceptionUtils.extractForeignKeyColumn(errorMsg)?.toCamelCase() ?: ""
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

    fun registerBooks(bookCreates: List<BookCreate>): BookBatchProcessResult {
        val currentTime = LocalDateTime.now()
        val books = bookCreates.map {
            it.toEntity(
                createdAt = currentTime,
                updatedAt = currentTime
            )
        }

        val successfulItems = mutableListOf<SuccessfulItem>()
        val failedItems = mutableListOf<FailedItem>()
        // パラメータのバリデーションに通過した書籍情報を保存用
        val validBooksWithIndex = mutableListOf<Pair<Int, Book>>()

        books.forEachIndexed { index, book ->
            try {
                validateBookParam(book)
                validBooksWithIndex.add(
                    Pair(index, book)
                )
            } catch (e: CustomException) {
                failedItems.add(
                    FailedItem(index = index)
                )
            }
        }

        // パラメータのバリデーションに通過した書籍情報のみをINSERTする
        if (validBooksWithIndex.isNotEmpty()) {
            val validBooks = validBooksWithIndex.map { it.second }
            try {
                val insertedCount = bookMapper.batchSave(validBooks)
                if (insertedCount == validBooks.size) {
                    validBooksWithIndex.forEach { (index, book) ->
                        successfulItems.add(
                            SuccessfulItem(
                                index = index,
                                id = book.id ?: throw CustomException(""),
                                title = book.title
                            )
                        )
                    }
                } else {
                    // DBで問題が生じたら全て失敗したことにする
                    successfulItems.clear()
                    failedItems.clear()
                    failedItems.addAll(bookCreates.indices.map { FailedItem(index = it) })
                }
            } catch (e: DuplicateKeyException) {
                successfulItems.clear()
                failedItems.clear()
                failedItems.addAll(bookCreates.indices.map { FailedItem(index = it) })
            } catch (e: DataIntegrityViolationException) {
                successfulItems.clear()
                failedItems.clear()
                failedItems.addAll(bookCreates.indices.map { FailedItem(index = it) })
            } catch (e: CustomException) {
                successfulItems.clear()
                failedItems.clear()
                failedItems.addAll(bookCreates.indices.map { FailedItem(index = it) })
            }
        }

        return when {
            failedItems.isEmpty() -> BookBatchProcessResult.AllSuccess(items = successfulItems)
            successfulItems.isEmpty() -> BookBatchProcessResult.AllFailure(items = failedItems)
            else -> {
                BookBatchProcessResult.Partial(
                    successfulItems = successfulItems,
                    failedItems = failedItems
                )
            }
        }
    }

    fun deleteBookLogically(id: Long) {
        bookMapper.deleteLogically(id)
    }

    fun deleteBooksLogically(ids: List<Long>) {
        bookMapper.deleteBatchLogically(ids)
    }

    @Transactional
    fun updateBook(bookUpdate: BookUpdate): BookUpdatedResponse {
        val bookId = bookUpdate.id
        val book = bookUpdate.toEntity(
            updatedAt = LocalDateTime.now()
        )

        validateBookParam(book)

        var updatedCount: Int = -1
        try {
            updatedCount = bookMapper.update(book)
        } catch (e: DataIntegrityViolationException) {
            if (DBExceptionUtils.isForeignKeyViolation(e)) {
                val errorMsg = e.message ?: ""
                val propertyName = DBExceptionUtils.extractForeignKeyColumn(errorMsg)?.toCamelCase() ?: ""
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

    private fun validateBookParam(book: Book) {
        // 書籍情報IDのチェック
        ValidationUtils.validatePositiveId(
            id = book.id,
            fieldName = Book::id.name,
            errorMsg = MSG_INVALID_VALUE
        )
        // 出版社IDのチェック
        ValidationUtils.validatePositiveId(
            id = book.publisherId,
            fieldName = Book::publisherId.name,
            errorMsg = MSG_INVALID_VALUE
        )
        // 登録者(ユーザ)IDのチェック
        ValidationUtils.validatePositiveId(
            id = book.userId,
            fieldName = Book::userId.name,
            errorMsg = MSG_INVALID_VALUE
        )
        // 金額のチェック
        if (book.price?.let { it < 0 } == true) {
            throw InvalidParamCustomException(
                message = MSG_INVALID_VALUE,
                field = Book::price.name,
                value = book.price
            )
        }
    }
}
