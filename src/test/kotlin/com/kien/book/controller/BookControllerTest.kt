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
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.*

@ExtendWith(SpringExtension::class, MockitoExtension::class)
@WebMvcTest(BookController::class)
@Import(BookControllerTest.TestConfig::class)
class BookControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var bookService: BookService

    @TestConfiguration
    class TestConfig {
        @Bean
        fun bookService(): BookService = mock(BookService::class.java)
    }

    @Test
    fun `getBookById should return book when exists`() {
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
            isDeleted = false,
            createdAt = LocalDateTime.of(2025, 4, 28, 10, 0),
            updatedAt = LocalDateTime.of(2025, 4, 28, 10, 0)
        )
        whenever(bookService.getBookById(bookId)).thenReturn(bookView)

        mockMvc.get("/books/$bookId")
            .andExpect {
                status { isOk() }
                content { json(objectMapper.writeValueAsString(bookView)) }
            }
    }

    @Test
    fun `getBookById should return 404 when book not found`() {
        val bookId = 1L
        whenever(bookService.getBookById(bookId)).thenReturn(null)

        mockMvc.get("/books/$bookId")
            .andExpect {
                status { isNotFound() }
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
    fun `registerBook should return 400 when param Invalid`() {
        val invalidParam = BookCreate(
            title = "",
            titleKana = "",
            author = "山田太郎",
            publisherId = 1L,
            userId = 100L
        )

        mockMvc.post("/books") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(invalidParam)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `registerBook should return 500 when creation fails`() {
        val bookCreate = BookCreate(
            title = "Kotlin入門",
            titleKana = "コトリン ニュウモン",
            author = "山田太郎",
            publisherId = 1L,
            userId = 100L
        )
        whenever(bookService.registerBook(bookCreate)).thenThrow(RuntimeException())

        mockMvc.post("/books") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(bookCreate)
        }.andExpect {
            status { isInternalServerError() }
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