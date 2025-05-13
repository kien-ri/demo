package com.kien.book.service

import com.kien.book.common.CustomException
import com.kien.book.model.Book
import com.kien.book.model.dto.book.BookCreate
import com.kien.book.model.dto.book.BookView
import com.kien.book.model.dto.book.BookCreatedResponse
import com.kien.book.repository.BookMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
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
            price = 2500,
            userId = 100L
        )

        /**
         * ID指定なしでの書籍情報登録テスト
         */
        @Test
        fun `return BookCreatedResponse when register succeeds without id`() {
            // mybatisの生成されたidを賦与する機能をmockする
            val captor = argumentCaptor<Book>()
            whenever(bookMapper.save(captor.capture())).then {
                captor.firstValue.id = 1L
                1   // mapperのsaveメソッドの戻り値指定
            }

            val expectedResult = BookCreatedResponse(id = 1L, title = "Kotlin入門")
            val result = bookService.registerBook(bookCreate)

            assertEquals(expectedResult, result)
        }

        /**
         * ID指定して書籍情報登録するテスト
         */
        @Test
        fun `return BookCreatedResponse when register succeeds with valid id`() {
            val bookCreateWithId = bookCreate.copy(id = 222L)

            // mybatisの生成されたidを賦与する機能をmockする
            val captor = argumentCaptor<Book>()
            whenever(bookMapper.save(captor.capture())).then {
                captor.firstValue.id = 222L
                1   // mapperのsaveメソッドの戻り値指定
            }

            val expectedResult = BookCreatedResponse(id = 222L, title = "Kotlin入門")
            val result = bookService.registerBook(bookCreateWithId)

            assertEquals(expectedResult, result)
        }

        @Test
        fun `should throw CustomException when id is negative`() {
            val bookCreateWithNegativeId = bookCreate.copy(id = -1L)

            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithNegativeId)
            }
            assertEquals("書籍IDは正数である必要があります", e.message)
        }

        @Test
        fun `should throw CustomException when id is zero`() {
            val bookCreateWithZeroId = bookCreate.copy(id = 0L)

            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithZeroId)
            }
            assertEquals("書籍IDは正数である必要があります", e.message)
        }

        @Test
        fun `should throw CustomException when price is negative`() {
            val bookCreateWithNegativePrice = bookCreate.copy(price = -1)

            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithNegativePrice)
            }
            assertEquals("価格は0以上である必要があります", e.message)
        }

        @Test
        fun `should throw CustomException when publisherId is negative`() {
            val bookCreateWithNegativePublisherId = bookCreate.copy(publisherId = -1L)

            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithNegativePublisherId)
            }
            assertEquals("出版社IDは正数である必要があります", e.message)
        }

        @Test
        fun `should throw CustomException when publisherId is zero`() {
            val bookCreateWithZeroPublisherId = bookCreate.copy(publisherId = 0L)

            val exception = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithZeroPublisherId)
            }
            assertEquals("出版社IDは正数である必要があります", exception.message)
        }

        @Test
        fun `should throw CustomException when userId is negative`() {
            val bookCreateWithNegativeUserId = bookCreate.copy(userId = -1L)

            val exception = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithNegativeUserId)
            }
            assertEquals("ユーザーIDは正数である必要があります", exception.message)
        }

        @Test
        fun `should throw CustomException when userId is zero`() {
            val bookCreateWithZeroUserId = bookCreate.copy(userId = 0L)

            val exception = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithZeroUserId)
            }
            assertEquals("ユーザーIDは正数である必要があります", exception.message)
        }

        @Test
        fun `should throw DuplicateKeyException when id is duplicated`() {
            val bookCreateWithId = bookCreate.copy(id = 1L)
            whenever(bookMapper.save(any())).thenThrow(DuplicateKeyException("Duplicate key"))

            assertFailsWith<DuplicateKeyException> {
                bookService.registerBook(bookCreateWithId)
            }
        }

        /**
         * 以下の動作を検証する：
         * 指定した外部キーが存在しない場合はDataIntegrityViolationExceptionが投げられ、
         * その中にSQLIntegrityConstraintViolationExceptionとエラーコード1452が含まれる
         */
        @Test
        fun `should throw DataIntegrityViolationException when publisherId does not exist`() {
            val bookCreateWithInvalidPublisherId = bookCreate.copy(publisherId = 11111111L)
            val sqlException = SQLIntegrityConstraintViolationException("Foreign key constraint violation", "FOREIGN_KEY", 1452, null)

            whenever(bookMapper.save(any())).thenThrow(DataIntegrityViolationException("Foreign key violation", sqlException))

            val e = assertFailsWith<DataIntegrityViolationException> {
                bookService.registerBook(bookCreateWithInvalidPublisherId)
            }
            val rootCause = e.rootCause
            assertThat(rootCause is SQLIntegrityConstraintViolationException)
            val errorCode = (rootCause as SQLIntegrityConstraintViolationException).errorCode
            assertThat(errorCode).isEqualTo(1452)
        }

        /**
         * 以下の動作を検証する：
         * 指定した外部キーが存在しない場合はDataIntegrityViolationExceptionが投げられ、
         * その中にSQLIntegrityConstraintViolationExceptionとエラーコード1452が含まれる
         */
        @Test
        fun `should throw DataIntegrityViolationException when userId does not exist`() {
            val bookCreateWithInvalidUserId = bookCreate.copy(userId = 10000000L)
            val sqlException = SQLIntegrityConstraintViolationException("Foreign key constraint violation", "FOREIGN_KEY", 1452, null)
            whenever(bookMapper.save(any())).thenThrow(DataIntegrityViolationException("Foreign key violation", sqlException))

            val e = assertFailsWith<DataIntegrityViolationException> {
                bookService.registerBook(bookCreateWithInvalidUserId)
            }
            val rootCause = e.rootCause
            assertThat(rootCause is SQLIntegrityConstraintViolationException)
            val errorCode = (rootCause as SQLIntegrityConstraintViolationException).errorCode
            assertThat(errorCode).isEqualTo(1452)
        }

        /**
         * データ1件をINSERT後、mapperから影響件数として1が返ってくるはず
         * その戻り値が何らかの原因で0となった場合の動作をテストする
         */
        @Test
        fun `should throw CustomException when insert fails`() {
            whenever(bookMapper.save(any())).thenReturn(0)

            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreate)
            }
            assertEquals("書籍情報が正しく登録されませんでした。", e.message)
        }

        /**
         * このテストは、MyBatisのuseGeneratedKeys機能が、書籍登録後にIDが生成されない状況を検証します。
         * mapperのメソッドはmockで模擬し、実際動いていないので、ここれはMybatisが機能しません。それでIDが生成されない状況を模擬します。
         */
        @Test
        fun `should throw CustomException when id is not generated`() {
            whenever(bookMapper.save(any())).thenReturn(1)

            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreate)
            }
            assertEquals("書籍情報保存に失敗しました：IDが生成されませんでした", e.message)
        }

        @Test
        fun `should throw RuntimeException when unexpected error occurs`() {
            whenever(bookMapper.save(any())).thenThrow(RuntimeException("模擬予想外エラー"))

            assertFailsWith<RuntimeException> {
                bookService.registerBook(bookCreate)
            }
        }

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

    @Test
    fun `updateBook should call update on BookMapper`() {
        val book = Book(
            id = 1L,
            title = "Kotlin入門",
            titleKana = "コトリン ニュウモン",
            author = "山田太郎",
            publisherId = 1L,
            userId = 100L
        )
        bookService.updateBook(book)
        verify(bookMapper).update(book)
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
