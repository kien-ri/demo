package com.kien.book.service

import com.kien.book.common.*
import com.kien.book.model.Book
import com.kien.book.model.dto.book.*
import com.kien.book.repository.BookMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.sql.SQLIntegrityConstraintViolationException
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

@SpringBootTest
class BookServiceTest {

    @MockitoBean
    private lateinit var bookMapper: BookMapper

    @MockitoBean
    private lateinit var batchService: BatchService

    @Autowired
    private lateinit var bookService: BookService

    @Nested
    inner class GetBookByIdTest {

        @Test
        fun `return BookView when book exists`() {
            val bookId = 1L
            val bookView = BookView(
                id = bookId,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                publisherName = "技術出版社",
                userId = 100L,
                userName = "テストユーザー",
                price = 2500,
                isDeleted = false,
                createdAt = LocalDateTime.of(2025, 4, 28, 10, 0),
                updatedAt = LocalDateTime.of(2025, 4, 28, 10, 0)
            )
            whenever(bookMapper.getById(bookId)).thenReturn(bookView)

            val result = bookService.getBookById(bookId)
            assertThat(result).isEqualTo(bookView)
        }

        /**
         * mapperからnullが返ってくる場合を想定
         * その理由が論理削除か存在しないかはserviceと関係ない
         */
        @Test
        fun `return null when book does not exist`() {
            val bookId = 1L
            whenever(bookMapper.getById(bookId)).thenReturn(null)
            val result = bookService.getBookById(bookId)
            assertThat(result).isNull()
        }

        @Test
        fun `throw CustomException when id is negative`() {
            val bookId = -1L
            val e = assertThrows<CustomException> {
                bookService.getBookById(bookId)
            }
            val expectedMsg = "入力された値が無効です。"
            assertThat(e.message).isEqualTo(expectedMsg)
        }

        @Test
        fun `throw CustomException when id is zero`() {
            val bookId = 0L
            val e = assertThrows<CustomException> {
                bookService.getBookById(bookId)
            }
            val expectedMsg = "入力された値が無効です。"
            assertThat(e.message).isEqualTo(expectedMsg)
        }
    }


    @Test
    fun `getBooksByCondition should return bookPage`() {
        val condition = BookCondition(
            title = "Kotlin",
            titleKana = "コトリン",
            author = "山田",
            pageSize = 10,
            currentPage = 1
        )
        val bookView = BookView(
            id = 1L,
            title = "Kotlin入門",
            titleKana = "コトリン ニュウモン",
            author = "山田太郎",
            publisherId = 1L,
            publisherName = "技術出版社",
            userId = 100L,
            userName = "テストユーザー",
            isDeleted = false,
            createdAt = LocalDateTime.of(2025, 4, 28, 10, 0),
            updatedAt = LocalDateTime.of(2025, 4, 28, 10, 0)
        )
        val bookViews = listOf(bookView)
        whenever(bookMapper.getCountByCondition(condition)).thenReturn(1)
        whenever(bookMapper.getListByCondition(condition)).thenReturn(bookViews)

        val result = bookService.getBooksByCondition(condition)
        assertThat(result).isEqualTo(
            Page(
                pageSize = 10,
                currentPage = 1,
                totalCount = 1,
                totalPages = 1,
                content = bookViews
            )
        )
    }

    @Test
    fun `getBooksByCondition should return bookPage with multiple books when multiple records match`() {
        val condition = BookCondition(
            title = "入門",
            pageSize = 10,
            currentPage = 1
        )
        val bookViews = listOf(
            BookView(
                id = 1L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                publisherName = "技術出版社",
                userId = 100L,
                userName = "テストユーザー",
                isDeleted = false,
                createdAt = LocalDateTime.of(2025, 4, 28, 10, 0),
                updatedAt = LocalDateTime.of(2025, 4, 28, 10, 0)
            ),
            BookView(
                id = 2L,
                title = "Java入門",
                titleKana = "ジャバー ニュウモン",
                author = "田中太郎",
                publisherId = 2L,
                publisherName = "教育出版社",
                userId = 101L,
                userName = "佐藤花子",
                isDeleted = false,
                createdAt = LocalDateTime.of(2025, 4, 28, 10, 0),
                updatedAt = LocalDateTime.of(2025, 4, 28, 10, 0)
            ),
            BookView(
                id = 4L,
                title = "Spring Boot 入門",
                titleKana = "スプリング ブート ニュウモン",
                author = "佐藤次郎",
                publisherId = 3L,
                publisherName = "文芸出版社",
                userId = 102L,
                userName = "鈴木一郎",
                isDeleted = false,
                createdAt = LocalDateTime.of(2025, 4, 28, 10, 0),
                updatedAt = LocalDateTime.of(2025, 4, 28, 10, 0)
            )
        )
        whenever(bookMapper.getCountByCondition(condition)).thenReturn(3)
        whenever(bookMapper.getListByCondition(condition)).thenReturn(bookViews)

        val result = bookService.getBooksByCondition(condition)
        assertThat(result).isEqualTo(
            Page(
                pageSize = 10,
                currentPage = 1,
                totalCount = 3,
                totalPages = 1,
                content = bookViews
            )
        )
    }

    @Test
    fun `getBooksByCondition should return empty page when no books found`() {
        val condition = BookCondition(
            title = "Kotlin",
            pageSize = 10,
            currentPage = 1
        )
        whenever(bookMapper.getCountByCondition(condition)).thenReturn(0)

        val result = bookService.getBooksByCondition(condition)
        assertThat(result).isEqualTo(
            Page(
                pageSize = 10,
                currentPage = 0,
                totalCount = 0,
                totalPages = 0,
                content = emptyList<BookView>()
            )
        )
    }

    @Test
    fun `registerBook should call save on BookMapper`() {
        val bookCreate = BookCreate(
            title = "Kotlin入門",
            titleKana = "コトリン ニュウモン",
            author = "山田太郎",
            publisherId = 1L,
            userId = 100L
        )
        bookService.registerBook(bookCreate)
        verify(bookMapper).save(bookCreate)
    }

    @Test
    fun `registerBooks should call batchService with save operation`() {
        val bookCreates = listOf(
            BookCreate(
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                userId = 100L
            ),
            BookCreate(
                title = "Spring Boot入門",
                titleKana = "スプリング ブート ニュウモン",
                author = "佐藤花子",
                publisherId = 2L,
                userId = 101L
            )
        )
        bookService.registerBooks(bookCreates)
        verify(batchService).batchProcess(
            dataList = bookCreates,
            mapperClass = BookMapper::class.java,
            operation = BookMapper::save
        )
    }

    @Test
    fun `deleteBookLogically should call delete on BookMapper`() {
        val bookId = 1L
        bookService.deleteBookLogically(bookId)
        verify(bookMapper).deleteLogically(bookId)
    }

    @Test
    fun `deleteBooksLogically should call deleteBatch on BookMapper`() {
        val ids = listOf(1L, 2L)
        bookService.deleteBooksLogically(ids)
        verify(bookMapper).deleteBatchLogically(ids)
    }

    @Nested
    inner class UpdateBookTest {
        @Test
        fun `return id and title when update succeeds`() {
            val bookUpdate = BookUpdate(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 100L,
                price = 4200
            )

            val expectedResult = BookUpdatedResponse(
                id = 1L,
                title = "Kotlin応用ガイド"
            )

            whenever(bookMapper.update(any())).thenReturn(1)
            val bookUpdatedResponse = bookService.updateBook(bookUpdate)

            assertThat(bookUpdatedResponse).isEqualTo(expectedResult)

            verify(bookMapper, times(1)).update(any())
        }

        @Test
        fun `throw exception when id is negative`() {
            val bookUpdate = BookUpdate(
                id = -1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 100L,
                price = 4200
            )

            val expectedError = InvalidParamCustomException(
                message = "入力された値が無効です。",
                field = "id",
                value = -1L
            )

            val realError = assertFailsWith<InvalidParamCustomException> {
                bookService.updateBook(bookUpdate)
            }

            assertThat(realError.message).isEqualTo(expectedError.message)
            assertThat(realError.field).isEqualTo(expectedError.field)
            assertThat(realError.value).isEqualTo(expectedError.value)

            verify(bookMapper, never()).update(any())
        }

        @Test
        fun `throw exception when id is 0`() {
            val bookUpdate = BookUpdate(
                id = 0L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 100L,
                price = 4200
            )

            val expectedError = InvalidParamCustomException(
                message = "入力された値が無効です。",
                field = "id",
                value = 0L
            )

            val realError = assertFailsWith<InvalidParamCustomException> {
                bookService.updateBook(bookUpdate)
            }

            assertThat(realError.message).isEqualTo(expectedError.message)
            assertThat(realError.field).isEqualTo(expectedError.field)
            assertThat(realError.value).isEqualTo(expectedError.value)

            verify(bookMapper, never()).update(any())
        }

        @Test
        fun `throw exception when publisherId is negative`() {
            val bookUpdate = BookUpdate(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = -1L,
                userId = 100L,
                price = 4200
            )

            val expectedError = InvalidParamCustomException(
                message = "入力された値が無効です。",
                field = "publisherId",
                value = -1L
            )

            val realError = assertFailsWith<InvalidParamCustomException> {
                bookService.updateBook(bookUpdate)
            }

            assertThat(realError.message).isEqualTo(expectedError.message)
            assertThat(realError.field).isEqualTo(expectedError.field)
            assertThat(realError.value).isEqualTo(expectedError.value)

            verify(bookMapper, never()).update(any())
        }

        @Test
        fun `throw exception when publisherId is 0`() {
            val bookUpdate = BookUpdate(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 0L,
                userId = 100L,
                price = 4200
            )

            val expectedError = InvalidParamCustomException(
                message = "入力された値が無効です。",
                field = "publisherId",
                value = 0L
            )

            val realError = assertFailsWith<InvalidParamCustomException> {
                bookService.updateBook(bookUpdate)
            }

            assertThat(realError.message).isEqualTo(expectedError.message)
            assertThat(realError.field).isEqualTo(expectedError.field)
            assertThat(realError.value).isEqualTo(expectedError.value)

            verify(bookMapper, never()).update(any())
        }

        @Test
        fun `throw exception when userId is negative`() {
            val bookUpdate = BookUpdate(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = -1L,
                price = 4200
            )

            val expectedError = InvalidParamCustomException(
                message = "入力された値が無効です。",
                field = "userId",
                value = -1L
            )

            val realError = assertFailsWith<InvalidParamCustomException> {
                bookService.updateBook(bookUpdate)
            }

            assertThat(realError.message).isEqualTo(expectedError.message)
            assertThat(realError.field).isEqualTo(expectedError.field)
            assertThat(realError.value).isEqualTo(expectedError.value)

            verify(bookMapper, never()).update(any())
        }

        @Test
        fun `throw exception when userId is zero`() {
            val bookUpdate = BookUpdate(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 0L,
                price = 4200
            )

            val expectedError = InvalidParamCustomException(
                message = "入力された値が無効です。",
                field = "userId",
                value = 0L
            )

            val realError = assertFailsWith<InvalidParamCustomException> {
                bookService.updateBook(bookUpdate)
            }

            assertThat(realError.message).isEqualTo(expectedError.message)
            assertThat(realError.field).isEqualTo(expectedError.field)
            assertThat(realError.value).isEqualTo(expectedError.value)

            verify(bookMapper, never()).update(any())
        }

        @Test
        fun `throw exception when price is negative`() {
            val bookUpdate = BookUpdate(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 100L,
                price = -1
            )

            val expectedError = InvalidParamCustomException(
                message = "入力された値が無効です。",
                field = "price",
                value = -1
            )

            val realError = assertFailsWith<InvalidParamCustomException> {
                bookService.updateBook(bookUpdate)
            }

            assertThat(realError.message).isEqualTo(expectedError.message)
            assertThat(realError.field).isEqualTo(expectedError.field)
            assertThat(realError.value).isEqualTo(expectedError.value)

            verify(bookMapper, never()).update(any())
        }

        /**
         * 以下の動作を検証する：
         * 指定した外部キーが存在しない場合はDataIntegrityViolationExceptionが投げられ、
         * その中にSQLIntegrityConstraintViolationExceptionとエラーコード1452が含まれる
         */
        @Test
        fun `throw exception when publisherId does not exist`() {
            val bookUpdate = BookUpdate(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 999L,
                userId = 100L,
                price = 4200
            )

            val expectedError = NonExistentForeignKeyCustomException(
                message = "存在しない外部キーです。",
                field = "publisherId",
                value = 999L
            )

            val sqlException = SQLIntegrityConstraintViolationException(
                "FOREIGN KEY (`publisher_id`) violation",
                "FOREIGN_KEY",
                1452,
                null
            )
            val springException = DataIntegrityViolationException(
                "FOREIGN KEY (`publisher_id`) violation",
                sqlException
            )
            whenever(bookMapper.update(any())).thenThrow(springException)

            val realError = assertFailsWith<NonExistentForeignKeyCustomException> {
                bookService.updateBook(bookUpdate)
            }
            assertThat(realError.message).isEqualTo(expectedError.message)
            assertThat(realError.field).isEqualTo(expectedError.field)
            assertThat(realError.value).isEqualTo(expectedError.value)

            verify(bookMapper, times(1)).update(any())
        }

        /**
         * 以下の動作を検証する：
         * 指定した外部キーが存在しない場合はDataIntegrityViolationExceptionが投げられ、
         * その中にSQLIntegrityConstraintViolationExceptionとエラーコード1452が含まれる
         */
        @Test
        fun `throw exception when userId does not exist`() {
            val bookUpdate = BookUpdate(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 999L,
                price = 4200
            )

            val expectedError = NonExistentForeignKeyCustomException(
                message = "存在しない外部キーです。",
                field = "userId",
                value = 999L
            )

            val sqlException = SQLIntegrityConstraintViolationException(
                "FOREIGN KEY (`user_id`) violation",
                "FOREIGN_KEY",
                1452,
                null
            )
            val springException = DataIntegrityViolationException(
                "FOREIGN KEY (`user_id`) violation",
                sqlException
            )
            whenever(bookMapper.update(any())).thenThrow(springException)

            val realError = assertFailsWith<NonExistentForeignKeyCustomException> {
                bookService.updateBook(bookUpdate)
            }
            assertThat(realError.message).isEqualTo(expectedError.message)
            assertThat(realError.field).isEqualTo(expectedError.field)
            assertThat(realError.value).isEqualTo(expectedError.value)

            verify(bookMapper, times(1)).update(any())
        }

        /**
         * 外部キーが存在しない以外の場合のDataIntegrityViolationExceptionは、
         * 予想外エラーとしてみられ、そのままthrowされる。
         */
        @Test
        fun `should throw exception when vendor code is not 1452`() {
            val bookUpdate = BookUpdate(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 100L,
                price = 4200
            )

            val sqlException = SQLIntegrityConstraintViolationException("模擬予想外SQLIntegrityConstraintViolationException", "予想外", 1111, null)
            val springException = DataIntegrityViolationException("模擬予想外DataIntegrityViolationException", sqlException)
            whenever(bookMapper.update(any())).thenThrow(springException)

            assertFailsWith<DataIntegrityViolationException> {
                bookService.updateBook(bookUpdate)
            }

            verify(bookMapper, times(1)).update(any())
        }

        @Test
        fun `throw exception when book does not exist`() {
            val bookUpdate = BookUpdate(
                id = 999L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 100L,
                price = 4200
            )

            val expectedError = NotFoundCustomException(
                message = "指定IDの書籍情報が存在しません",
                field = "id",
                value = 999L
            )

            whenever(bookMapper.update(any())).thenReturn(0)

            val realError = assertFailsWith<NotFoundCustomException> {
                bookService.updateBook(bookUpdate)
            }
            assertThat(realError.message).isEqualTo(expectedError.message)
            assertThat(realError.field).isEqualTo(expectedError.field)
            assertThat(realError.value).isEqualTo(expectedError.value)

            verify(bookMapper, times(1)).update(any())
        }

        @Test
        fun `should throw exception when unexpected error occurs`() {
            val bookUpdate = BookUpdate(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 100L,
                price = 4200
            )
            whenever(bookMapper.update(any())).thenThrow(RuntimeException("模擬予想外エラー"))

            assertFailsWith<RuntimeException> {
                bookService.updateBook(bookUpdate)
            }

            verify(bookMapper, times(1)).update(any())
        }
    }

    @Test
    fun `updateBooks should call batchService with update operation`() {
        val books = listOf(
            Book(
                id = 1L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                userId = 100L
            ),
            Book(
                id = 2L,
                title = "Spring Boot入門",
                titleKana = "スプリング ブート ニュウモン",
                author = "佐藤花子",
                publisherId = 2L,
                userId = 101L
            )
        )
        bookService.updateBooks(books)
        verify(batchService).batchProcess(
            dataList = books,
            mapperClass = BookMapper::class.java,
            operation = BookMapper::update
        )
    }
}
