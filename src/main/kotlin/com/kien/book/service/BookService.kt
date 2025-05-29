package com.kien.book.service

import com.kien.book.common.*
import com.kien.book.model.dto.book.BookCondition
import com.kien.book.common.Page
import com.kien.book.util.DBExceptionUtils
import com.kien.book.util.StringUtils.toCamelCase
import com.kien.book.util.ValidationUtils
import com.kien.book.model.Book
import com.kien.book.model.dto.book.*
import com.kien.book.repository.BookMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
//                throw CustomException("エラー：下限金額にマイナスの値を指定できません。")
            }
        }
        if (bookCondition.maxPrice != null) {
            require(bookCondition.maxPrice >= 0) {
//                throw CustomException("エラー：上限金額にマイナスの値を指定できません。")
            }
        }
        if (bookCondition.minPrice != null && bookCondition.maxPrice != null) {
            require(bookCondition.minPrice <= bookCondition.maxPrice) {
//                throw CustomException("エラー：下限金額が上限金額より大きいです。")
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
    fun registerBook(bookCreate: BookCreate): BookBasicInfo {
        // 1. DTO to Entity
        val currentTime = LocalDateTime.now()
        val book = bookCreate.toEntity(
            current = currentTime,
        )

        // 2. パラメータのバリデーション
        validateBookParam(book)

        // 3. INSERT実行
        var insertedCount: Int = -1
        try {
            insertedCount = bookMapper.save(book)
        } catch (e: DuplicateKeyException) {
            // 3.1 主キー重複エラー
            throw CustomException(
                message = MSG_DUPLICATE_KEY,
                httpStatus = HttpStatus.CONFLICT,
                field = Book::id.name,
                value = bookCreate.id
            )
        } catch (e: DataIntegrityViolationException) {
            // 3.2 外部キー存在しないエラー
            if (DBExceptionUtils.isForeignKeyViolation(e)) {
                val errorMsg = e.message ?: ""
                val propertyName = DBExceptionUtils.extractForeignKeyColumn(errorMsg)?.toCamelCase() ?: ""
                val property = BookCreate::class.memberProperties.find { it.name == propertyName }
                val propertyValue = property?.get(bookCreate)
                throw CustomException(
                    message = MSG_NONEXISTENT_FK,
                    httpStatus = HttpStatus.NOT_FOUND,
                    field = propertyName,
                    value = propertyValue
                )
            }
            throw e
        }

        // 4. INSERT結果の検証
        // 4.1 挿入件数のチェック
        if (insertedCount <= 0) {
            throw CustomException(
                message = MSG_INSERT_ERROR,
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                field = "",
                value = null
            )
        }
        // 4.2 インクリメントのIDが付与されたかをチェック(Mybatis UseGeneratedKeys)
        val bookId = book.id ?: throw CustomException(
            message = MSG_NO_ID_GENERATED,
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            field = Book::id.name,
            value = null
        )

        // 5. 戻り値DTO構成
        return BookBasicInfo(
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
                throw CustomException(
                    message = MSG_NONEXISTENT_FK,
                    httpStatus = HttpStatus.NOT_FOUND,
                    field = propertyName,
                    value = propertyValue
                )
            }
            throw e
        }

        if (updatedCount <= 0) {
            throw CustomException(
                message = MSG_NON_EXISTENT_BOOK,
                httpStatus = HttpStatus.NOT_FOUND,
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
            throw CustomException(
                message = MSG_INVALID_VALUE,
                httpStatus = HttpStatus.BAD_REQUEST,
                field = Book::price.name,
                value = book.price
            )
        }
    }
}
