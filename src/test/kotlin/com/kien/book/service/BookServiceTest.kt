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

            val expectedError = CustomException(
                message = "入力された値が無効です。",
                httpStatus = HttpStatus.BAD_REQUEST,
                field = "id",
                value = -1L
            )
            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithNegativeId)
            }

            assertThat(expectedError).isEqualTo(e)
            verify(bookMapper, never()).save(any())
        }

        @Test
        fun `should throw CustomException when id is zero`() {
            val bookCreateWithZeroId = bookCreate.copy(id = 0L)

            val expectedError = CustomException(
                message = "入力された値が無効です。",
                httpStatus = HttpStatus.BAD_REQUEST,
                field = "id",
                value = 0L
            )
            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithZeroId)
            }
            assertThat(expectedError).isEqualTo(e)

            verify(bookMapper, never()).save(any())
        }

        @Test
        fun `should throw CustomException when price is negative`() {
            val bookCreateWithNegativePrice = bookCreate.copy(price = -1)

            val expectedError = CustomException(
                message = "入力された値が無効です。",
                httpStatus = HttpStatus.BAD_REQUEST,
                field = "price",
                value = -1
            )
            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithNegativePrice)
            }
            assertThat(expectedError).isEqualTo(e)

            verify(bookMapper, never()).save(any())
        }

        @Test
        fun `should throw CustomException when publisherId is negative`() {
            val bookCreateWithNegativePublisherId = bookCreate.copy(publisherId = -1L)

            val expectedError = CustomException(
                message = "入力された値が無効です。",
                httpStatus = HttpStatus.BAD_REQUEST,
                field = "publisherId",
                value = -1L
            )
            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithNegativePublisherId)
            }
            assertThat(expectedError).isEqualTo(e)

            verify(bookMapper, never()).save(any())
        }

        @Test
        fun `should throw CustomException when publisherId is zero`() {
            val bookCreateWithZeroPublisherId = bookCreate.copy(publisherId = 0L)

            val expectedError = CustomException(
                message = "入力された値が無効です。",
                httpStatus = HttpStatus.BAD_REQUEST,
                field = "publisherId",
                value = 0L
            )
            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithZeroPublisherId)
            }
            assertThat(expectedError).isEqualTo(e)

            verify(bookMapper, never()).save(any())
        }

        @Test
        fun `should throw CustomException when userId is negative`() {
            val bookCreateWithNegativeUserId = bookCreate.copy(userId = -1L)

            val expectedError = CustomException(
                message = "入力された値が無効です。",
                httpStatus = HttpStatus.BAD_REQUEST,
                field = "userId",
                value = -1L
            )
            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithNegativeUserId)
            }
            assertThat(expectedError).isEqualTo(e)

            verify(bookMapper, never()).save(any())
        }

        @Test
        fun `should throw CustomException when userId is zero`() {
            val bookCreateWithZeroUserId = bookCreate.copy(userId = 0L)

            val expectedError = CustomException(
                message = "入力された値が無効です。",
                httpStatus = HttpStatus.BAD_REQUEST,
                field = "userId",
                value = 0L
            )
            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithZeroUserId)
            }
            assertThat(expectedError).isEqualTo(e)

            verify(bookMapper, never()).save(any())
        }

        @Test
        fun `should throw DuplicateKeyException when id is duplicated`() {
            val bookCreateWithId = bookCreate.copy(id = 1L)

            val expectedError = CustomException(
                message = "プライマリキーが重複しました。別の値にしてください",
                httpStatus = HttpStatus.CONFLICT,
                field = "id",
                value = 1L
            )

            val springError = DuplicateKeyException("模擬主キー重複エラー")
            whenever(bookMapper.save(any())).thenThrow(springError)

            val realError = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithId)
            }
            assertThat(expectedError).isEqualTo(realError)

            verify(bookMapper, times(1)).save(any())
        }

        /**
         * 以下の動作を検証する：
         * 指定した外部キーが存在しない場合はDataIntegrityViolationExceptionが投げられ、
         * その中にSQLIntegrityConstraintViolationExceptionとエラーコード1452が含まれる
         */
        @Test
        fun `should throw DataIntegrityViolationException when publisherId does not exist`() {
            val bookCreateWithInvalidPublisherId = bookCreate.copy(publisherId = 999L)
            val expectedError = CustomException(
                message = "存在しない外部キーです。",
                httpStatus = HttpStatus.NOT_FOUND,
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
            whenever(bookMapper.save(any())).thenThrow(springException)

            val realError = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithInvalidPublisherId)
            }
            assertThat(expectedError).isEqualTo(realError)

            verify(bookMapper, times(1)).save(any())
        }

        /**
         * 以下の動作を検証する：
         * 指定した外部キーが存在しない場合はDataIntegrityViolationExceptionが投げられ、
         * その中にSQLIntegrityConstraintViolationExceptionとエラーコード1452が含まれる
         */
        @Test
        fun `should throw DataIntegrityViolationException when userId does not exist`() {
            val bookCreateWithInvalidUserId = bookCreate.copy(userId = 999L)
            val expectedError = CustomException(
                message = "存在しない外部キーです。",
                httpStatus = HttpStatus.NOT_FOUND,
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
            whenever(bookMapper.save(any())).thenThrow(springException)

            val realError = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreateWithInvalidUserId)
            }
            assertThat(expectedError).isEqualTo(realError)

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

            val expectedError = CustomException(
                message = "書籍情報が正しく登録されませんでした。",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                field = "",
                value = null
            )
            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreate)
            }
            assertThat(expectedError).isEqualTo(e)

            verify(bookMapper, times(1)).save(any())
        }

        /**
         * このテストは、MyBatisのuseGeneratedKeys機能が、書籍登録後にIDが生成されない状況を検証します。
         * mapperのメソッドはmockで模擬し、実際動いていないので、ここれはMybatisが機能しません。それでIDが生成されない状況を模擬します。
         */
        @Test
        fun `should throw CustomException when id is not generated`() {
            whenever(bookMapper.save(any())).thenReturn(1)

            val expectedError = CustomException(
                message = "書籍情報保存に失敗しました：IDが生成されませんでした",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                field = "id",
                value = null
            )
            val e = assertFailsWith<CustomException> {
                bookService.registerBook(bookCreate)
            }
            assertThat(expectedError).isEqualTo(e)

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

            val expectedError = CustomException(
                message = "入力された値が無効です。",
                httpStatus = HttpStatus.BAD_REQUEST,
                field = "id",
                value = -1L
            )

            val realError = assertFailsWith<CustomException> {
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

            val expectedError = CustomException(
                message = "入力された値が無効です。",
                httpStatus = HttpStatus.BAD_REQUEST,
                field = "id",
                value = 0L
            )

            val realError = assertFailsWith<CustomException> {
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

            val expectedError = CustomException(
                message = "入力された値が無効です。",
                httpStatus = HttpStatus.BAD_REQUEST,
                field = "publisherId",
                value = -1L
            )

            val realError = assertFailsWith<CustomException> {
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

            val expectedError = CustomException(
                message = "入力された値が無効です。",
                httpStatus = HttpStatus.BAD_REQUEST,
                field = "publisherId",
                value = 0L
            )

            val realError = assertFailsWith<CustomException> {
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

            val expectedError = CustomException(
                message = "入力された値が無効です。",
                httpStatus = HttpStatus.BAD_REQUEST,
                field = "userId",
                value = -1L
            )

            val realError = assertFailsWith<CustomException> {
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

            val expectedError = CustomException(
                message = "入力された値が無効です。",
                httpStatus = HttpStatus.BAD_REQUEST,
                field = "userId",
                value = 0L
            )

            val realError = assertFailsWith<CustomException> {
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

            val expectedError = CustomException(
                message = "入力された値が無効です。",
                httpStatus = HttpStatus.BAD_REQUEST,
                field = "price",
                value = -1
            )

            val realError = assertFailsWith<CustomException> {
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

            val expectedError = CustomException(
                message = "存在しない外部キーです。",
                httpStatus = HttpStatus.NOT_FOUND,
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

            val realError = assertFailsWith<CustomException> {
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

            val expectedError = CustomException(
                message = "存在しない外部キーです。",
                httpStatus = HttpStatus.NOT_FOUND,
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

            val realError = assertFailsWith<CustomException> {
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

            val expectedError = CustomException(
                message = "指定IDの書籍情報が存在しません",
                httpStatus = HttpStatus.NOT_FOUND,
                field = "id",
                value = 999L
            )

            whenever(bookMapper.update(any())).thenReturn(0)

            val realError = assertFailsWith<CustomException> {
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
}
