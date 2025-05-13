package com.kien.book.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.kien.book.common.Page
import com.kien.book.model.dto.book.*
import com.kien.book.service.BookService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.*
import java.sql.SQLIntegrityConstraintViolationException

@SpringBootTest
@AutoConfigureMockMvc
class BookControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var bookService: BookService

    @TestConfiguration
    class TestConfig {
        @Bean
        fun bookService(): BookService = mock(BookService::class.java)
    }

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
            val negativeId = -1
            val expectedMsg = "入力された値が無効です。"
            val expectedResponse = mapOf("id" to expectedMsg)

            val mvcResult = mockMvc.get("/books/$negativeId")
            mvcResult.andExpect {
                status { isBadRequest() }
                content { json(objectMapper.writeValueAsString(expectedResponse)) }
            }
        }

        @Test
        fun `return 400 when id is zero`() {
            val zeroId = 0
            val expectedMsg = "入力された値が無効です。"
            val expectedResponse = mapOf("id" to expectedMsg)

            val mvcResult = mockMvc.get("/books/$zeroId")
            mvcResult.andExpect {
                status { isBadRequest() }
                content { json(objectMapper.writeValueAsString(expectedResponse)) }
            }
        }

        @Test
        fun `return 400 when id type is mismatched float`() {
            val doubleId = 1.5
            val expectedMsg = "無効なリクエストです。URLをチェックしてください。"
            val expectedResponse = mapOf("error" to expectedMsg)

            val mvcResult = mockMvc.get("/books/$doubleId")
            mvcResult.andExpect {
                status { isBadRequest() }
                content { json(objectMapper.writeValueAsString(expectedResponse)) }
            }
        }

        @Test
        fun `return 400 when id type is mismatched str`() {
            var strId = "abc"
            val expectedMsg = "無効なリクエストです。URLをチェックしてください。"
            val expectedResponse = mapOf("error" to expectedMsg)

            val mvcResult = mockMvc.get("/books/$strId")
            mvcResult.andExpect {
                status { isBadRequest() }
                content { json(objectMapper.writeValueAsString(expectedResponse)) }
            }
        }

        @Test
        fun `return 400 when no param`() {
            val expectedMsg = "無効なリクエストです。URLをチェックしてください。"
            val expectedResponse = mapOf("error" to expectedMsg)

            val mvcResult = mockMvc.get("/books/")
            mvcResult.andExpect {
                status { isBadRequest() }
                content { json(objectMapper.writeValueAsString(expectedResponse)) }
            }
        }
    }

    @Test
    fun `registerBook should return 204`() {
        val bookCreate = BookCreate(
            title = "Kotlin入門",
            titleKana = "コトリン ニュウモン",
            author = "山田太郎",
            publisherId = 1L,
            userId = 100L
        )

        mockMvc.post("/books") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(bookCreate)
        }.andExpect {
            status { isNoContent() }
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

            val expectedResult = BookCreatedResponse(
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

            val expectedResult = BookCreatedResponse(
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

            val expectedResponseBody = mapOf(
                "id" to "入力された値が無効です"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }
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

            val expectedResponseBody = mapOf(
                "id" to "入力された値が無効です"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }
        }

        @Test
        fun `return 400 when title is blank`() {
            val bookCreate = BookCreate(
                id = 1L,
                title = "",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                price = 2500,
                userId = 100L
            )

            val expectedResponseBody = mapOf(
                "title" to "入力された値が無効です"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }
        }

        @Test
        fun `return 400 when titleKana is blank`() {
            val bookCreate = BookCreate(
                id = 1L,
                title = "Kotlin入門",
                titleKana = "",
                author = "山田太郎",
                publisherId = 1L,
                price = 2500,
                userId = 100L
            )

            val expectedResponseBody = mapOf(
                "titleKana" to "入力された値が無効です"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }
        }

        @Test
        fun `return 400 when author is blank`() {
            val bookCreate = BookCreate(
                id = 1L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "",
                publisherId = 1L,
                price = 2500,
                userId = 100L
            )

            val expectedResponseBody = mapOf(
                "author" to "入力された値が無効です"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }
        }

        @Test
        fun `return 400 when price is null`() {
            val bookCreate = BookCreate(
                id = 1L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                price = null,
                userId = 100L
            )

            val expectedResponseBody = mapOf(
                "price" to "入力された値が無効です"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }
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

            val expectedResponseBody = mapOf(
                "price" to "入力された値が無効です"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }
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

            val expectedResponseBody = mapOf(
                "publisherId" to "入力された値が無効です"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }
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

            val expectedResponseBody = mapOf(
                "publisherId" to "入力された値が無効です"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }
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

            val expectedResponseBody = mapOf(
                "userId" to "入力された値が無効です"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }
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

            val expectedResponseBody = mapOf(
                "userId" to "入力された値が無効です"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isBadRequest() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }
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

            whenever(bookService.registerBook(bookCreate)).thenThrow(DuplicateKeyException(""))
            val expectedResponseBody = mapOf(
                "error" to "プライマリキーが重複しました。別の値にしてください"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isConflict() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }
        }

        @Test
        fun `return 409 when publisher id non exist`() {
            val bookCreate = BookCreate(
                id = 1L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 11111111L,
                price = 2500,
                userId = 100L
            )

            val sqlException = SQLIntegrityConstraintViolationException("Foreign key constraint violation", "FOREIGN_KEY", 1452, null)
            val dataIntegrityViolation = DataIntegrityViolationException("Foreign key violation", sqlException)
            whenever(bookService.registerBook(bookCreate)).thenThrow(dataIntegrityViolation)
            val expectedResponseBody = mapOf(
                "error" to "存在しない外部キーです。"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isConflict() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }
        }

        @Test
        fun `return 409 when user id non exist`() {
            val bookCreate = BookCreate(
                id = 1L,
                title = "Kotlin入門",
                titleKana = "コトリン ニュウモン",
                author = "山田太郎",
                publisherId = 1L,
                price = 2500,
                userId = 10000000L
            )

            val sqlException = SQLIntegrityConstraintViolationException("Foreign key constraint violation", "FOREIGN_KEY", 1452, null)
            val dataIntegrityViolation = DataIntegrityViolationException("Foreign key violation", sqlException)
            whenever(bookService.registerBook(bookCreate)).thenThrow(dataIntegrityViolation)
            val expectedResponseBody = mapOf(
                "error" to "存在しない外部キーです。"
            )

            val mvcResult = mockMvc.post("/books") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(bookCreate)
            }
            mvcResult.andExpect {
                status { isConflict() }
                content { objectMapper.writeValueAsString(expectedResponseBody) }
            }
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

    @Test
    fun `updateBook should return 204 when update succeeds`() {
        val bookId = 1L
        val bookUpdate = BookUpdate(
            id = bookId,
            title = "Kotlin入門 Old Edition",
            titleKana = "コトリン ニュウモン",
            author = "山田太郎",
            publisherId = 1L,
            userId = 100L
        )

        mockMvc.put("/books/$bookId") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(bookUpdate)
        }.andExpect {
            status { isNoContent() }
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
