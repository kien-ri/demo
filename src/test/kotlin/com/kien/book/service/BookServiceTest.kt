package com.kien.book.service

import com.kien.book.common.*
import com.kien.book.model.Book
import com.kien.book.model.dto.book.*
import com.kien.book.repository.BookMapper
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.sql.SQLIntegrityConstraintViolationException
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

@SpringBootTest
class BookServiceTest {

    @MockitoBean
    private lateinit var bookMapper: BookMapper

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

    @Nested
    inner class RegisterBookTest {

        private val bookCreate = BookCreate(
            id = null,
            title = "Kotlin入門",
            titleKana = "コトリン ニュウモン",
            author = "山田太郎",
            publisherId = 1L,
            userId = 1L,
            price = 2500
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

            val expectedResult = BookBasicInfo(id = 1L, title = "Kotlin入門")
            val result = bookService.registerBook(bookCreate)

            assertEquals(expectedResult, result)

            verify(bookMapper, times(1)).save(any())
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

            val expectedResult = BookBasicInfo(id = 222L, title = "Kotlin入門")
            val result = bookService.registerBook(bookCreateWithId)

            assertEquals(expectedResult, result)

            verify(bookMapper, times(1)).save(any())
        }

        @Test
        fun `should throw CustomException when id is negative`() {
            val bookCreateWithNegativeId = bookCreate.copy(id = -1L)

            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithNegativeId)
            }
            assertEquals("書籍IDは正数である必要があります", e.message)

            verify(bookMapper, never()).save(any())
        }

        @Test
        fun `should throw CustomException when id is zero`() {
            val bookCreateWithZeroId = bookCreate.copy(id = 0L)

            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithZeroId)
            }
            assertEquals("書籍IDは正数である必要があります", e.message)

            verify(bookMapper, never()).save(any())
        }

        @Test
        fun `should throw CustomException when price is negative`() {
            val bookCreateWithNegativePrice = bookCreate.copy(price = -1)

            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithNegativePrice)
            }
            assertEquals("価格は0以上である必要があります", e.message)

            verify(bookMapper, never()).save(any())
        }

        @Test
        fun `should throw CustomException when publisherId is negative`() {
            val bookCreateWithNegativePublisherId = bookCreate.copy(publisherId = -1L)

            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithNegativePublisherId)
            }
            assertEquals("出版社IDは正数である必要があります", e.message)

            verify(bookMapper, never()).save(any())
        }

        @Test
        fun `should throw CustomException when publisherId is zero`() {
            val bookCreateWithZeroPublisherId = bookCreate.copy(publisherId = 0L)

            val exception = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithZeroPublisherId)
            }
            assertEquals("出版社IDは正数である必要があります", exception.message)

            verify(bookMapper, never()).save(any())
        }

        @Test
        fun `should throw CustomException when userId is negative`() {
            val bookCreateWithNegativeUserId = bookCreate.copy(userId = -1L)

            val exception = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithNegativeUserId)
            }
            assertEquals("ユーザーIDは正数である必要があります", exception.message)

            verify(bookMapper, never()).save(any())
        }

        @Test
        fun `should throw CustomException when userId is zero`() {
            val bookCreateWithZeroUserId = bookCreate.copy(userId = 0L)

            val exception = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithZeroUserId)
            }
            assertEquals("ユーザーIDは正数である必要があります", exception.message)

            verify(bookMapper, never()).save(any())
        }

        @Test
        fun `should throw DuplicateKeyException when id is duplicated`() {
            val bookCreateWithId = bookCreate.copy(id = 1L)

            val expectedError = DuplicateKeyCustomException(
                message = "プライマリキーが重複しました。別の値にしてください",
                field = "id",
                value = 1L
            )

            val springError = DuplicateKeyException("模擬主キー重複エラー")
            whenever(bookMapper.save(any())).thenThrow(springError)

            val realError = assertFailsWith<DuplicateKeyCustomException> {
                bookService.registerBook(bookCreateWithId)
            }
            assertThat(realError.message).isEqualTo(expectedError.message)
            assertThat(realError.field).isEqualTo(expectedError.field)
            assertThat(realError.value).isEqualTo(expectedError.value)

            verify(bookMapper, times(1)).save(any())
        }

        /**
         * 以下の動作を検証する：
         * 指定した外部キーが存在しない場合はDataIntegrityViolationExceptionが投げられ、
         * その中にSQLIntegrityConstraintViolationExceptionとエラーコード1452が含まれる
         */
        @Test
        fun `should throw DataIntegrityViolationException when publisherId does not exist`() {
            val bookCreateWithInvalidPublisherId = bookCreate.copy(publisherId = 11111111L)
            val expectedError = NonExistentForeignKeyCustomException(
                message = "存在しない外部キーです。",
                field = "publisherId",
                value = 11111111L
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
            whenever(bookMapper.save(any())).thenThrow(springException)

            val realError = assertFailsWith<NonExistentForeignKeyCustomException> {
                bookService.registerBook(bookCreateWithInvalidPublisherId)
            }
            assertThat(realError.message).isEqualTo(expectedError.message)
            assertThat(realError.field).isEqualTo(expectedError.field)
            assertThat(realError.value).isEqualTo(expectedError.value)

            verify(bookMapper, times(1)).save(any())
        }

        /**
         * 以下の動作を検証する：
         * 指定した外部キーが存在しない場合はDataIntegrityViolationExceptionが投げられ、
         * その中にSQLIntegrityConstraintViolationExceptionとエラーコード1452が含まれる
         */
        @Test
        fun `should throw DataIntegrityViolationException when userId does not exist`() {
            val bookCreateWithInvalidUserId = bookCreate.copy(userId = 10000000L)
            val expectedError = NonExistentForeignKeyCustomException(
                message = "存在しない外部キーです。",
                field = "userId",
                value = 10000000L
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
            whenever(bookMapper.save(any())).thenThrow(springException)

            val realError = assertFailsWith<NonExistentForeignKeyCustomException> {
                bookService.registerBook(bookCreateWithInvalidUserId)
            }
            assertThat(realError.message).isEqualTo(expectedError.message)
            assertThat(realError.field).isEqualTo(expectedError.field)
            assertThat(realError.value).isEqualTo(expectedError.value)

            verify(bookMapper, times(1)).save(any())
        }

        /**
         * 外部キーが存在しない以外の場合のDataIntegrityViolationExceptionは、
         * 予想外エラーとしてみられ、そのままthrowされる。
         */
        @Test
        fun `should throw exception when vendor code is not 1452`() {
            val sqlException = SQLIntegrityConstraintViolationException("模擬予想外SQLIntegrityConstraintViolationException", "予想外", 1111, null)
            val expectedException = DataIntegrityViolationException("模擬予想外DataIntegrityViolationException", sqlException)
            whenever(bookMapper.save(any())).thenThrow(expectedException)

            assertFailsWith<DataIntegrityViolationException> {
                bookService.registerBook(bookCreate)
            }

            verify(bookMapper, times(1)).save(any())
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

            verify(bookMapper, times(1)).save(any())
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

            verify(bookMapper, times(1)).save(any())
        }

        @Test
        fun `should throw RuntimeException when unexpected error occurs`() {
            whenever(bookMapper.save(any())).thenThrow(RuntimeException("模擬予想外エラー"))

            assertFailsWith<RuntimeException> {
                bookService.registerBook(bookCreate)
            }

            verify(bookMapper, times(1)).save(any())
        }
    }

    @Nested
    inner class RegisterBooksTest{
        val bookCreate1 = BookCreate(
            id = null,
            title = "はじめてのKotlinプログラミング",
            titleKana = "ハジメテノ コトリン プログラミング",
            author = "山田太郎",
            publisherId = 1L,
            price = 2500,
            userId = 100L
        )
        val bookCreate2 = BookCreate(
            id = null,
            title = "Kotlinで学ぶ関数型プログラミング",
            titleKana = "コトリンデ マナブ カンスウガタ プログラミング",
            author = "鈴木花子",
            publisherId = 1L,
            price = 3000,
            userId = 100L
        )
        val bookCreate3 = BookCreate(
            id = null,
            title = "実践Kotlinアプリ開発",
            titleKana = "ジッセン コトリン アプリ カイハツ",
            author = "佐藤次郎",
            publisherId = 1L,
            price = 2800,
            userId = 100L
        )

        @Test
        fun `return suceess when register succeeds without id`() {
            val bookCreates = listOf(
                bookCreate1,
                bookCreate2,
                bookCreate3
            )

            val captor = argumentCaptor<List<Book>>()
            whenever(bookMapper.batchSaveWithoutId(captor.capture())).thenAnswer {
                val books = captor.firstValue
                books.forEachIndexed { index, book ->
                    book.id = (1 + index).toLong()
                }
                books.size
            }
            whenever(bookMapper.batchSaveWithSpecifiedId(captor.capture())).thenAnswer {
                val books = captor.firstValue
                books.size
            }

            val expectedResult = BookBatchProcessedResult(
                httpStatus = HttpStatus.OK,
                successfulItems = listOf(
                    ProcessedBook(id = 1, title = "はじめてのKotlinプログラミング", error = null),
                    ProcessedBook(id = 2, title = "Kotlinで学ぶ関数型プログラミング", error = null),
                    ProcessedBook(id = 3, title = "実践Kotlinアプリ開発", error = null)
                ),
                failedItems = emptyList()
            )

            val result = bookService.registerBooks(bookCreates)
            assertEquals(expectedResult, result)
            verify(bookMapper, times(1)).batchSaveWithoutId(any())
            verify(bookMapper, never()).batchSaveWithSpecifiedId(any())
        }

        @Test
        fun `return suceess when register succeeds with specified ids`() {
            val bookCreates = listOf(
                bookCreate1.copy(id = 100L),
                bookCreate2.copy(id = 105L),
                bookCreate3.copy(id = 110L)
            )

            val captor = argumentCaptor<List<Book>>()
            whenever(bookMapper.batchSaveWithoutId(captor.capture())).thenAnswer {
                val books = captor.firstValue
                books.forEachIndexed { index, book ->
                    book.id = (1 + index).toLong()
                }
                books.size
            }
            whenever(bookMapper.batchSaveWithSpecifiedId(captor.capture())).thenAnswer {
                val books = captor.firstValue
                books.size
            }

            val expectedResult = BookBatchProcessedResult(
                httpStatus = HttpStatus.OK,
                successfulItems = listOf(
                    ProcessedBook(id = 100L, title = "はじめてのKotlinプログラミング", error = null),
                    ProcessedBook(id = 105L, title = "Kotlinで学ぶ関数型プログラミング", error = null),
                    ProcessedBook(id = 110L, title = "実践Kotlinアプリ開発", error = null)
                ),
                failedItems = emptyList()
            )

            val result = bookService.registerBooks(bookCreates)
            assertEquals(expectedResult, result)
            verify(bookMapper, times(1)).batchSaveWithSpecifiedId(anyList())
            verify(bookMapper, never()).batchSaveWithoutId(anyList())
        }

        @Test
        fun `return partial when some books have invalid id`() {
            val bookCreates = listOf(
                bookCreate1.copy(id = -1L),
                bookCreate2.copy(id = 1L),
                bookCreate3.copy(id = 0L)
            )

            val captor = argumentCaptor<List<Book>>()
            whenever(bookMapper.batchSaveWithoutId(captor.capture())).thenAnswer {
                val books = captor.firstValue
                books.forEachIndexed { index, book ->
                    book.id = (1 + index).toLong()
                }
                books.size
            }
            whenever(bookMapper.batchSaveWithSpecifiedId(captor.capture())).thenAnswer {
                val books = captor.firstValue
                books.size
            }

            val expectedResult = BookBatchProcessedResult(
                httpStatus = HttpStatus.MULTI_STATUS,
                successfulItems = listOf(
                    ProcessedBook(id = 1, title = "Kotlinで学ぶ関数型プログラミング", error = null)
                ),
                failedItems = listOf(
                    ProcessedBook(
                        id = -1L,
                        title = "はじめてのKotlinプログラミング",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "id",
                            value = -1L
                        )
                    ),
                    ProcessedBook(
                        id = 0L,
                        title = "実践Kotlinアプリ開発",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "id",
                            value = 0L
                        )
                    )
                )
            )

            val result = bookService.registerBooks(bookCreates)
            assertEquals(expectedResult, result)
            verify(bookMapper, times(1)).batchSaveWithSpecifiedId(anyList())
            verify(bookMapper, never()).batchSaveWithoutId(anyList())
        }

        @Test
        fun `return partial when some books have invalid publisherId`() {
            val bookCreates = listOf(
                bookCreate1.copy(publisherId = -1L),
                bookCreate2.copy(publisherId = 1L),
                bookCreate3.copy(publisherId = 0L)
            )

            val captor = argumentCaptor<List<Book>>()
            whenever(bookMapper.batchSaveWithoutId(captor.capture())).thenAnswer {
                val books = captor.firstValue
                books.forEachIndexed { index, book ->
                    book.id = (1 + index).toLong()
                }
                books.size
            }
            whenever(bookMapper.batchSaveWithSpecifiedId(captor.capture())).thenAnswer {
                val books = captor.firstValue
                books.size
            }

            val expectedResult = BookBatchProcessedResult(
                httpStatus = HttpStatus.MULTI_STATUS,
                successfulItems = listOf(
                    ProcessedBook(id = 1, title = "Kotlinで学ぶ関数型プログラミング", error = null)
                ),
                failedItems = listOf(
                    ProcessedBook(
                        id = null,
                        title = "はじめてのKotlinプログラミング",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "publisherId",
                            value = -1L
                        )
                    ),
                    ProcessedBook(
                        id = null,
                        title = "実践Kotlinアプリ開発",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "publisherId",
                            value = 0L
                        )
                    )
                )
            )

            val result = bookService.registerBooks(bookCreates)
            assertEquals(expectedResult, result)
            verify(bookMapper, never()).batchSaveWithSpecifiedId(anyList())
            verify(bookMapper, times(1)).batchSaveWithoutId(anyList())
        }

        @Test
        fun `return partial when some books have invalid userId`() {
            val bookCreates = listOf(
                bookCreate1.copy(userId = -1L),
                bookCreate2.copy(userId = 1L),
                bookCreate3.copy(userId = 0L)
            )

            val captor = argumentCaptor<List<Book>>()
            whenever(bookMapper.batchSaveWithoutId(captor.capture())).thenAnswer {
                val books = captor.firstValue
                books.forEachIndexed { index, book ->
                    book.id = (1 + index).toLong()
                }
                books.size
            }
            whenever(bookMapper.batchSaveWithSpecifiedId(captor.capture())).thenAnswer {
                val books = captor.firstValue
                books.size
            }

            val expectedResult = BookBatchProcessedResult(
                httpStatus = HttpStatus.MULTI_STATUS,
                successfulItems = listOf(
                    ProcessedBook(id = 1, title = "Kotlinで学ぶ関数型プログラミング", error = null)
                ),
                failedItems = listOf(
                    ProcessedBook(
                        id = null,
                        title = "はじめてのKotlinプログラミング",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "userId",
                            value = -1L
                        )
                    ),
                    ProcessedBook(
                        id = null,
                        title = "実践Kotlinアプリ開発",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "userId",
                            value = 0L
                        )
                    )
                )
            )

            val result = bookService.registerBooks(bookCreates)
            assertEquals(expectedResult, result)
            verify(bookMapper, times(1)).batchSaveWithoutId(anyList())
            verify(bookMapper, never()).batchSaveWithSpecifiedId(anyList())
        }

        @Test
        fun `return partial when some books have invalid price`() {
            val bookCreates = listOf(
                bookCreate1.copy(price = -1),
                bookCreate2.copy(price = 1000),
                bookCreate3.copy(price = -500)
            )

            val captor = argumentCaptor<List<Book>>()
            whenever(bookMapper.batchSaveWithoutId(captor.capture())).thenAnswer {
                val books = captor.firstValue
                books.forEachIndexed { index, book ->
                    book.id = (1 + index).toLong()
                }
                books.size
            }
            whenever(bookMapper.batchSaveWithSpecifiedId(captor.capture())).thenAnswer {
                val books = captor.firstValue
                books.size
            }

            val expectedResult = BookBatchProcessedResult(
                httpStatus = HttpStatus.MULTI_STATUS,
                successfulItems = listOf(
                    ProcessedBook(id = 1, title = "Kotlinで学ぶ関数型プログラミング", error = null)
                ),
                failedItems = listOf(
                    ProcessedBook(
                        id = null,
                        title = "はじめてのKotlinプログラミング",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "price",
                            value = -1
                        )
                    ),
                    ProcessedBook(
                        id = null,
                        title = "実践Kotlinアプリ開発",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "price",
                            value = -500
                        )
                    )
                )
            )

            val result = bookService.registerBooks(bookCreates)
            assertEquals(expectedResult, result)
            verify(bookMapper, times(1)).batchSaveWithoutId(anyList())
            verify(bookMapper, never()).batchSaveWithSpecifiedId(anyList())
        }

        @Test
        fun `return failed when all books have invalid property`() {
            val bookCreates = listOf(
                bookCreate1.copy(id = -1L),
                bookCreate2.copy(publisherId = -1L),
                bookCreate3.copy(price = -500)
            )

            val expectedResult = BookBatchProcessedResult(
                httpStatus = HttpStatus.BAD_REQUEST,
                successfulItems = emptyList(),
                failedItems = listOf(
                    ProcessedBook(
                        id = -1L,
                        title = "はじめてのKotlinプログラミング",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "id",
                            value = -1L
                        )
                    ),
                    ProcessedBook(
                        id = null,
                        title = "Kotlinで学ぶ関数型プログラミング",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "publisherId",
                            value = -1L
                        )
                    ),
                    ProcessedBook(
                        id = null,
                        title = "実践Kotlinアプリ開発",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "price",
                            value = -500
                        )
                    )
                )
            )

            val result = bookService.registerBooks(bookCreates)
            assertEquals(expectedResult, result)
            verify(bookMapper, never()).batchSaveWithSpecifiedId(anyList())
            verify(bookMapper, never()).batchSaveWithoutId(anyList())
        }

        @Test
        fun `throw exception when register failed`() {
            val bookCreates = listOf(
                bookCreate1,
                bookCreate2,
                bookCreate3
            )

            // 何かの原因でINSERTできた行数が0の状況をmock
            whenever(bookMapper.batchSaveWithoutId(anyList())).thenReturn(0)

            val expectedError = CustomException(
                message = "書籍情報が正しく登録されませんでした。"
            )

            val error = assertFailsWith<CustomException> {
                bookService.registerBooks(bookCreates)
            }
            assertEquals(error.message, expectedError.message)
            verify(bookMapper, times(1)).batchSaveWithoutId(anyList())
        }

        @Test
        fun `throw exception when other exception occurred`() {
            val bookCreates = listOf(
                bookCreate1,
                bookCreate2,
                bookCreate3
            )

            val expectedError = RuntimeException("Unexpected error")
            whenever(bookMapper.batchSaveWithoutId(anyList())).thenThrow(expectedError)

            val error = assertFailsWith<RuntimeException> {
                bookService.registerBooks(bookCreates)
            }
            assertEquals(expectedError, error)
            verify(bookMapper, times(1)).batchSaveWithoutId(anyList())
        }
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

    @Nested
    inner class UpdateBooksTest{
        val bookUpdate1 = BookUpdate(
            id = 1L,
            title = "はじめてのKotlinプログラミング",
            titleKana = "ハジメテノ コトリン プログラミング",
            author = "山田太郎",
            publisherId = 1L,
            price = 2500,
            userId = 100L
        )
        val bookUpdate2 = BookUpdate(
            id = 2L,
            title = "Kotlinで学ぶ関数型プログラミング",
            titleKana = "コトリンデ マナブ カンスウガタ プログラミング",
            author = "鈴木花子",
            publisherId = 1L,
            price = 3000,
            userId = 100L
        )
        val bookUpdate3 = BookUpdate(
            id = 3L,
            title = "実践Kotlinアプリ開発",
            titleKana = "ジッセン コトリン アプリ カイハツ",
            author = "佐藤次郎",
            publisherId = 1L,
            price = 2800,
            userId = 100L
        )

        @Test
        fun `return success when update succeeds`() {
            val bookUpdates = listOf(
                bookUpdate1,
                bookUpdate2,
                bookUpdate3
            )

            whenever(bookMapper.batchUpdate(any())).thenReturn(3)

            val expectedResult = BookBatchProcessedResult(
                httpStatus = HttpStatus.OK,
                successfulItems = listOf(
                    ProcessedBook(id = 1L, title = "はじめてのKotlinプログラミング", error = null),
                    ProcessedBook(id = 2L, title = "Kotlinで学ぶ関数型プログラミング", error = null),
                    ProcessedBook(id = 3L, title = "実践Kotlinアプリ開発", error = null)
                ),
                failedItems = emptyList()
            )

            val result = bookService.updateBooks(bookUpdates)
            assertEquals(expectedResult, result)
            verify(bookMapper, times(1)).batchUpdate(anyList())
        }

        @Test
        fun `return partial when some books have invalid id`() {
            val bookUpdates = listOf(
                bookUpdate1.copy(id = -1L),
                bookUpdate2.copy(id = 1L),
                bookUpdate3.copy(id = 0L)
            )

            whenever(bookMapper.batchUpdate(any())).thenReturn(1)

            val expectedResult = BookBatchProcessedResult(
                httpStatus = HttpStatus.MULTI_STATUS,
                successfulItems = listOf(
                    ProcessedBook(id = 1, title = "Kotlinで学ぶ関数型プログラミング", error = null)
                ),
                failedItems = listOf(
                    ProcessedBook(
                        id = -1L,
                        title = "はじめてのKotlinプログラミング",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "id",
                            value = -1L
                        )
                    ),
                    ProcessedBook(
                        id = 0L,
                        title = "実践Kotlinアプリ開発",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "id",
                            value = 0L
                        )
                    )
                )
            )

            val result = bookService.updateBooks(bookUpdates)
            assertEquals(expectedResult, result)
            verify(bookMapper, times(1)).batchUpdate(anyList())
        }

        @Test
        fun `return partial when some books have invalid publisherId`() {
            val bookUpdates = listOf(
                bookUpdate1.copy(publisherId = -1L),
                bookUpdate2.copy(publisherId = 1L),
                bookUpdate3.copy(publisherId = 0L)
            )

            whenever(bookMapper.batchUpdate(any())).thenReturn(1)

            val expectedResult = BookBatchProcessedResult(
                httpStatus = HttpStatus.MULTI_STATUS,
                successfulItems = listOf(
                    ProcessedBook(id = 2L, title = "Kotlinで学ぶ関数型プログラミング", error = null)
                ),
                failedItems = listOf(
                    ProcessedBook(
                        id = 1L,
                        title = "はじめてのKotlinプログラミング",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "publisherId",
                            value = -1L
                        )
                    ),
                    ProcessedBook(
                        id = 3L,
                        title = "実践Kotlinアプリ開発",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "publisherId",
                            value = 0L
                        )
                    )
                )
            )

            val result = bookService.updateBooks(bookUpdates)
            assertEquals(expectedResult, result)
            verify(bookMapper, times(1)).batchUpdate(anyList())
        }

        @Test
        fun `return partial when some books have invalid userId`() {
            val bookUpdates = listOf(
                bookUpdate1.copy(userId = -1L),
                bookUpdate2.copy(userId = 1L),
                bookUpdate3.copy(userId = 0L)
            )

            whenever(bookMapper.batchUpdate(any())).thenReturn(1)

            val expectedResult = BookBatchProcessedResult(
                httpStatus = HttpStatus.MULTI_STATUS,
                successfulItems = listOf(
                    ProcessedBook(id = 2L, title = "Kotlinで学ぶ関数型プログラミング", error = null)
                ),
                failedItems = listOf(
                    ProcessedBook(
                        id = 1L,
                        title = "はじめてのKotlinプログラミング",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "userId",
                            value = -1L
                        )
                    ),
                    ProcessedBook(
                        id = 3L,
                        title = "実践Kotlinアプリ開発",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "userId",
                            value = 0L
                        )
                    )
                )
            )

            val result = bookService.updateBooks(bookUpdates)
            assertEquals(expectedResult, result)
            verify(bookMapper, times(1)).batchUpdate(anyList())
        }

        @Test
        fun `return partial when some books have invalid price`() {
            val bookUpdates = listOf(
                bookUpdate1.copy(price = -1),
                bookUpdate2.copy(price = 1000),
                bookUpdate3.copy(price = -500)
            )

            whenever(bookMapper.batchUpdate(any())).thenReturn(1)

            val expectedResult = BookBatchProcessedResult(
                httpStatus = HttpStatus.MULTI_STATUS,
                successfulItems = listOf(
                    ProcessedBook(id = 2L, title = "Kotlinで学ぶ関数型プログラミング", error = null)
                ),
                failedItems = listOf(
                    ProcessedBook(
                        id = 1L,
                        title = "はじめてのKotlinプログラミング",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "price",
                            value = -1
                        )
                    ),
                    ProcessedBook(
                        id = 3L,
                        title = "実践Kotlinアプリ開発",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "price",
                            value = -500
                        )
                    )
                )
            )

            val result = bookService.updateBooks(bookUpdates)
            assertEquals(expectedResult, result)
            verify(bookMapper, times(1)).batchUpdate(anyList())
        }

        @Test
        fun `return failed when all books have invalid property`() {
            val bookUpdates = listOf(
                bookUpdate1.copy(id = -1L),
                bookUpdate2.copy(publisherId = -1L),
                bookUpdate3.copy(price = -500)
            )

            val expectedResult = BookBatchProcessedResult(
                httpStatus = HttpStatus.BAD_REQUEST,
                successfulItems = emptyList(),
                failedItems = listOf(
                    ProcessedBook(
                        id = -1L,
                        title = "はじめてのKotlinプログラミング",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "id",
                            value = -1L
                        )
                    ),
                    ProcessedBook(
                        id = 2L,
                        title = "Kotlinで学ぶ関数型プログラミング",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "publisherId",
                            value = -1L
                        )
                    ),
                    ProcessedBook(
                        id = 3L,
                        title = "実践Kotlinアプリ開発",
                        error = InvalidParamCustomException(
                            message = "入力された値が無効です。",
                            field = "price",
                            value = -500
                        )
                    )
                )
            )

            val result = bookService.updateBooks(bookUpdates)
            assertEquals(expectedResult, result)
            verify(bookMapper, never()).batchUpdate(anyList())
        }

        @Test
        fun `throw exception when update failed`() {
            val bookUpdates = listOf(
                bookUpdate1,
                bookUpdate2,
                bookUpdate3
            )

            // UPDATEできた行数が0の状況をmock
            whenever(bookMapper.batchUpdate(anyList())).thenReturn(0)

            val expectedError = CustomException(
                message = "書籍情報が正しく更新されませんでした。"
            )

            val error = assertFailsWith<CustomException> {
                bookService.updateBooks(bookUpdates)
            }
            assertEquals(error.message, expectedError.message)
            verify(bookMapper, times(1)).batchUpdate(anyList())
        }

        @Test
        fun `throw exception when exception occurred`() {
            val bookUpdates = listOf(
                bookUpdate1,
                bookUpdate2,
                bookUpdate3
            )

            val expectedError = RuntimeException("Unexpected error")
            whenever(bookMapper.batchUpdate(anyList())).thenThrow(expectedError)

            val error = assertFailsWith<RuntimeException> {
                bookService.updateBooks(bookUpdates)
            }
            assertEquals(expectedError, error)
            verify(bookMapper, times(1)).batchUpdate(anyList())
        }
    }
}
