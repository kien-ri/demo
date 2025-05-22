package com.kien.book.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.kien.book.common.NonExistentForeignKeyCustomException
import com.kien.book.common.NotFoundCustomException
import com.kien.book.common.Page
import com.kien.book.model.dto.book.*
import com.kien.book.common.DuplicateKeyCustomException
import com.kien.book.model.dto.book.BookCreate
import com.kien.book.model.dto.book.BookView
import com.kien.book.service.BookService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.*
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post


@SpringBootTest
@AutoConfigureMockMvc
class BookControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var bookService: BookService

    @Nested
    inner class GetBookByIdTest {

        @Test
        fun `return 200 and book when exists`() {
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
            whenever(bookService.getBookById(bookId)).thenReturn(bookView)

            val mvcResult = mockMvc.get("/books/$bookId")
            mvcResult.andExpect {
                status { isOk() }
                content { json(objectMapper.writeValueAsString(bookView)) }
            }
        }

        @Test
        fun `return 204 when not found`() {
            val bookId = 1L
            whenever(bookService.getBookById(bookId)).thenReturn(null)

            // debugでresponseを確認するため一旦変数宣言
            val mvcResult = mockMvc.get("/books/$bookId")
            mvcResult.andExpect {
                status { isNoContent() }
                content { null }
            }
        }

        @Test
        fun `return 400 when id is negative`() {
            val expectedResponse = mapOf(
                "id" to "-1",
                "message" to "入力された値が無効です。"
            )

            val mvcResult = mockMvc.get("/books/-1")
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponse) }
            }
        }

        @Test
        fun `return 400 when id is zero`() {
            val expectedResponse = mapOf(
                "id" to "0",
                "message" to "入力された値が無効です。"
            )

            val mvcResult = mockMvc.get("/books/0")
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponse) }
            }
        }

        @Test
        fun `return 400 when id type is mismatched float`() {
            val expectedResponse = mapOf(
                "id" to "1.5",
                "message" to "パラメータの型が間違っています"
            )

            val mvcResult = mockMvc.get("/books/1.5")
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponse) }
            }
        }

        @Test
        fun `return 400 when id type is mismatched str`() {
            val expectedResponse = mapOf(
                "id" to "abc",
                "message" to "パラメータの型が間違っています"
            )

            val mvcResult = mockMvc.get("/books/abc")
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponse) }
            }
        }

        @Test
        fun `return 400 when no param`() {
            val expectedResponse = mapOf(
                "GET" to "/books",
                "message" to "無効なリクエストです。URLをチェックしてください。"
            )

            val mvcResult = mockMvc.get("/books")
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponse) }
            }
        }
    }

    @Test
    fun `getBooksByCondition should return paginated books`() {
        val condition = BookCondition(
            title = "Kotlin",
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
        val page = Page(10, 1, 1, 1, listOf(bookView))
        whenever(bookService.getBooksByCondition(condition)).thenReturn(page)

        mockMvc.get("/books") {
            param("title", condition.title!!)
            param("pageSize", condition.pageSize.toString())
            param("currentPage", condition.currentPage.toString())
        }.andExpect {
            status { isOk() }
            content { json(objectMapper.writeValueAsString(page)) }
        }
    }

    @Nested
    inner class RegisterBookTest {

        /**
         * 新規登録するデータの主キーidを指定せずに登録するテスト
         */
        @Test
        fun `return 200(id,title) when register without id`() {
            val bookCreate = BookCreate(
                id = null,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                price = 2500,
                userId = 100L
            )

            val expectedResult = BookBasicInfo(
                id = 1L,
                title = "Kotlin入門"
            )
            whenever(bookService.registerBook(bookCreate)).thenReturn(expectedResult)

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isOk() }
                content { objectMapper.writeValueAsString(expectedResult) }
            }

            verify(bookService, times(1)).registerBook(any())
        }

        /**
         * 新規登録するデータの主キーidを指定して登録するテスト
         */
        @Test
        fun `return 200(id,title) when register with id`() {
            val bookCreate = BookCreate(
                id = 222L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                price = 2500,
                userId = 100L
            )

            val expectedResult = BookBasicInfo(
                id = 222L,
                title = "Kotlin入門"
            )
            whenever(bookService.registerBook(bookCreate)).thenReturn(expectedResult)

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isOk() }
                content { objectMapper.writeValueAsString(expectedResult) }
            }

            verify(bookService, times(1)).registerBook(any())
        }

        @Test
        fun `return 400 when id is negative`() {
            val bookCreate = BookCreate(
                id = -1L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                price = 2500,
                userId = 100L
            )

            val expectedResponseBody = arrayOf(
                mapOf(
                    "id" to "-1",
                    "message" to "入力された値が無効です"
                )
            )
            val m = objectMapper.writeValueAsString(expectedResponseBody)

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }

            verify(bookService, never()).registerBook(any())
        }

        @Test
        fun `return 400 when id is 0`() {
            val bookCreate = BookCreate(
                id = 0L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                price = 2500,
                userId = 100L
            )

            val expectedResponseBody = arrayOf(
                mapOf(
                    "id" to "0",
                    "message" to "入力された値が無効です"
                )
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }

            verify(bookService, never()).registerBook(any())
        }

        @Test
        fun `return 400 when price is negative`() {
            val bookCreate = BookCreate(
                id = 1L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                price = -1,
                userId = 100L
            )

            val expectedResponseBody = arrayOf(
                mapOf(
                    "price" to "-1",
                    "message" to "入力された値が無効です"
                )
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }

            verify(bookService, never()).registerBook(any())
        }

        @Test
        fun `return 400 when publisher id is negative`() {
            val bookCreate = BookCreate(
                id = 1L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = -1L,
                price = 2500,
                userId = 100L
            )

            val expectedResponseBody = arrayOf(
                mapOf(
                    "publisherId" to "-1",
                    "message" to "入力された値が無効です"
                )
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }

            verify(bookService, never()).registerBook(any())
        }

        @Test
        fun `return 400 when publisher id is 0`() {
            val bookCreate = BookCreate(
                id = 1L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 0L,
                price = 2500,
                userId = 100L
            )

            val expectedResponseBody = arrayOf(
                mapOf(
                    "publisherId" to "0",
                    "message" to "入力された値が無効です"
                )
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }

            verify(bookService, never()).registerBook(any())
        }

        @Test
        fun `return 400 when user id is negative`() {
            val bookCreate = BookCreate(
                id = 1L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                price = 2500,
                userId = -1L
            )

            val expectedResponseBody = arrayOf(
                mapOf(
                    "userId" to "-1",
                    "message" to "入力された値が無効です"
                )
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }

            verify(bookService, never()).registerBook(any())
        }

        @Test
        fun `return 400 when user id is 0`() {
            val bookCreate = BookCreate(
                id = 1L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                price = 2500,
                userId = 0L
            )

            val expectedResponseBody = arrayOf(
                mapOf(
                    "userId" to "0",
                    "message" to "入力された値が無効です"
                )
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }

            verify(bookService, never()).registerBook(any())
        }

        @Test
        fun `return 409 when id duplicated`() {
            val bookCreate = BookCreate(
                id = 1L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                price = 2500,
                userId = 100L
            )

            val expectedError = DuplicateKeyCustomException(
                message = "プライマリキーが重複しました。別の値にしてください",
                field = "id",
                value = 1L
            )
            whenever(bookService.registerBook(bookCreate)).thenThrow(expectedError)
            val expectedResponseBody = mapOf(
                "id" to 1L,
                "message" to "プライマリキーが重複しました。別の値にしてください"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isConflict() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }

            verify(bookService, times(1)).registerBook(any())
        }

        @Test
        fun `return 404 when publisher id non exist`() {
            val bookCreate = BookCreate(
                id = 1L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 11111111L,
                price = 2500,
                userId = 100L
            )

            val expectedError = NonExistentForeignKeyCustomException(
                message = "存在しない外部キーです。",
                field = "publisherId",
                value = 11111111L
            )
            whenever(bookService.registerBook(bookCreate)).thenThrow(expectedError)
            val expectedResponseBody = mapOf(
                "publisherId" to 11111111L,
                "message" to "存在しない外部キーです。"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isNotFound() }
                content { json(objectMapper.writeValueAsString(expectedResponseBody)) }
            }

            verify(bookService, times(1)).registerBook(any())
        }

        @Test
        fun `return 404 when user id non exist`() {
            val bookCreate = BookCreate(
                id = 1L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                price = 2500,
                userId = 10000000L
            )

            val expectedError = NonExistentForeignKeyCustomException(
                message = "存在しない外部キーです。",
                field = "userId",
                value = 10000000L
            )
            whenever(bookService.registerBook(bookCreate)).thenThrow(expectedError)
            val expectedResponseBody = mapOf(
                "userId" to 10000000L,
                "message" to "存在しない外部キーです。"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isNotFound() }
                content { json(objectMapper.writeValueAsString(expectedResponseBody)) }
            }

            verify(bookService, times(1)).registerBook(any())
        }

        @Test
        fun `registerBook should return 500 when creation fails`() {
            val bookCreate = BookCreate(
                id = null,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                price = 2500,
                userId = 100L
            )
            whenever(bookService.registerBook(bookCreate)).thenThrow(RuntimeException("エラー詳細"))
            val expectedResponseBody = mapOf(
                "error" to "予想外のエラーが発生しました。エラー内容：エラー詳細"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isInternalServerError() }
            }

            verify(bookService, times(1)).registerBook(any())
        }
    }

    @Test
    fun `deleteBookLogically should return 204 when deletion succeeds`() {
        val bookId = 1L
        mockMvc.delete("/books/$bookId")
            .andExpect {
                status { isNoContent() }
            }
    }

    @Test
    fun `deleteBooksLogically should return 204 when batch deletion succeeds`() {
        mockMvc.delete("/books/batch") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(listOf(1L, 2L))
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Nested
    inner class UpdateBookTest {
        @Test
        fun `return 200(id, title) when update succeeds`() {
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
            whenever(bookService.updateBook(bookUpdate)).thenReturn(expectedResult)

            val mvcResult = mockMvc.put("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookUpdate)
            }
            mvcResult.andExpect {
                status { isOk() }
                content { json(objectMapper.writeValueAsString(expectedResult)) }
            }

            verify(bookService, times(1)).updateBook(any())
        }

        @Test
        fun `return 400 when id is negative`() {
            val bookUpdate = BookUpdate(
                id = -1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 100L,
                price = 4200
            )

            val responseMap = mapOf(
                "id" to "-1",
                "message" to "入力された値が無効です。"
            )
            val expectedResponse = arrayOf(responseMap)

            val mvcResult = mockMvc.put("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookUpdate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { json(objectMapper.writeValueAsString(expectedResponse)) }
            }

            verify(bookService, never()).updateBook(any())
        }

        @Test
        fun `return 400 when id is 0`() {
            val bookUpdate = BookUpdate(
                id = 0L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 100L,
                price = 4200
            )

            val responseMap = mapOf(
                "id" to "0",
                "message" to "入力された値が無効です。"
            )
            val expectedResponse = arrayOf(responseMap)

            val mvcResult = mockMvc.put("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookUpdate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { json(objectMapper.writeValueAsString(expectedResponse)) }
            }

            verify(bookService, never()).updateBook(any())
        }

        @Test
        fun `return 400 when publisherId is negative`() {
            val bookUpdate = BookUpdate(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = -1L,
                userId = 100L,
                price = 4200
            )

            val responseMap = mapOf(
                "publisherId" to "-1",
                "message" to "入力された値が無効です。"
            )
            val expectedResponse = arrayOf(responseMap)

            val mvcResult = mockMvc.put("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookUpdate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { json(objectMapper.writeValueAsString(expectedResponse)) }
            }

            verify(bookService, never()).updateBook(any())
        }

        @Test
        fun `return 400 when publisherId is 0`() {
            val bookUpdate = BookUpdate(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 0L,
                userId = 100L,
                price = 4200
            )

            val responseMap = mapOf(
                "publisherId" to "0",
                "message" to "入力された値が無効です。"
            )
            val expectedResponse = arrayOf(responseMap)

            val mvcResult = mockMvc.put("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookUpdate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { json(objectMapper.writeValueAsString(expectedResponse)) }
            }

            verify(bookService, never()).updateBook(any())
        }

        @Test
        fun `return 400 when userId is negative`() {
            val bookUpdate = BookUpdate(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = -1L,
                price = 4200
            )

            val responseMap = mapOf(
                "userId" to "-1",
                "message" to "入力された値が無効です。"
            )
            val expectedResponse = arrayOf(responseMap)

            val mvcResult = mockMvc.put("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookUpdate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { json(objectMapper.writeValueAsString(expectedResponse)) }
            }

            verify(bookService, never()).updateBook(any())
        }

        @Test
        fun `return 400 when userId is 0`() {
            val bookUpdate = BookUpdate(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 0L,
                price = 4200
            )

            val responseMap = mapOf(
                "userId" to "0",
                "message" to "入力された値が無効です。"
            )
            val expectedResponse = arrayOf(responseMap)

            val mvcResult = mockMvc.put("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookUpdate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { json(objectMapper.writeValueAsString(expectedResponse)) }
            }

            verify(bookService, never()).updateBook(any())
        }

        @Test
        fun `return 400 when price is negative`() {
            val bookUpdate = BookUpdate(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 100L,
                price = -1
            )

            val responseMap = mapOf(
                "price" to "-1",
                "message" to "入力された値が無効です。"
            )
            val expectedResponse = arrayOf(responseMap)

            val mvcResult = mockMvc.put("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookUpdate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { json(objectMapper.writeValueAsString(expectedResponse)) }
            }

            verify(bookService, never()).updateBook(any())
        }

        @Test
        fun `return 404 when publisherId does not exist`() {
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
                field = "publisher_id",
                value = 999L
            )
            whenever(bookService.updateBook(bookUpdate)).thenThrow(expectedError)
            val expectedResponse = mapOf(
                "publisher_id" to 999,
                "message" to "存在しない外部キーです。"
            )

            val mvcResult = mockMvc.put("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookUpdate)
            }
            mvcResult.andExpect {
                status { isNotFound() }
                content { json(objectMapper.writeValueAsString(expectedResponse)) }
            }

            verify(bookService, times(1)).updateBook(any())
        }

        @Test
        fun `return 404 when userId does not exist`() {
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
                field = "user_id",
                value = 999L
            )
            whenever(bookService.updateBook(bookUpdate)).thenThrow(expectedError)
            val expectedResponse = mapOf(
                "user_id" to 999,
                "message" to "存在しない外部キーです。"
            )

            val mvcResult = mockMvc.put("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookUpdate)
            }
            mvcResult.andExpect {
                status { isNotFound() }
                content { json(objectMapper.writeValueAsString(expectedResponse)) }
            }

            verify(bookService, times(1)).updateBook(any())
        }

        @Test
        fun `return 404 when book does not exist`() {
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
            whenever(bookService.updateBook(bookUpdate)).thenThrow(expectedError)

            val expectedResponse = mapOf(
                "id" to 999,
                "message" to "指定IDの書籍情報が存在しません"
            )

            val mvcResult = mockMvc.put("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookUpdate)
            }
            mvcResult.andExpect {
                status { isNotFound() }
                content { json(objectMapper.writeValueAsString(expectedResponse)) }
            }

            verify(bookService, times(1)).updateBook(any())
        }

        @Test
        fun `return 500 when unexpected error`() {
            val bookUpdate = BookUpdate(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 100L,
                price = 4200
            )

            whenever(bookService.updateBook(bookUpdate)).thenThrow(RuntimeException("予想外のエラー"))
            val expectedResponse = mapOf(
                "error" to "予想外のエラーが発生しました。エラー内容：予想外のエラー"
            )

            val mvcResult = mockMvc.put("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookUpdate)
            }
            mvcResult.andExpect {
                status { isInternalServerError() }
                content { json(objectMapper.writeValueAsString(expectedResponse)) }
            }

            verify(bookService, times(1)).updateBook(any())
        }
    }

    @Test
    fun `updateBooks should return 204 when batch update succeeds`() {
        val bookUpdates = listOf(
            BookUpdate(
                id = 1L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                userId = 100L
            ),
            BookUpdate(
                id = 2L,
                title = "Spring Boot入門",
                titleKana = "スプリング ブート ニュウモン",
                author = "佐藤花子",
                publisherId = 2L,
                userId = 101L
            )
        )

        mockMvc.put("/books/batch") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(bookUpdates)
        }.andExpect {
            status { isNoContent() }
        }
    }
}
