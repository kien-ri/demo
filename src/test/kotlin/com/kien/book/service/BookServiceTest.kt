package com.kien.book.service

import com.kien.book.common.CustomException
import com.kien.book.model.Book
import com.kien.book.model.dto.book.BookCondition
import com.kien.book.model.dto.book.BookCreate
import com.kien.book.model.dto.book.BookView
import com.kien.book.common.Page
import com.kien.book.repository.BookMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDateTime

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
        fun `should call update on BookMapper`() {
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
