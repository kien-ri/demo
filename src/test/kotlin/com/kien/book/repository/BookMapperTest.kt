package com.kien.book.repository

import com.kien.book.model.Book
import com.kien.book.model.dto.book.BookCondition
import com.kien.book.model.dto.book.BookCreate
import com.kien.book.model.dto.book.BookView
import org.junit.jupiter.api.Test
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDateTime

@MybatisTest
@ActiveProfiles("test")
@Sql(scripts = ["/schema.sql"], executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = ["/data.sql"], executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
class BookMapperTest {

    @Autowired
    private lateinit var bookMapper: BookMapper

    @Test
    fun `getById should return BookView when book exists`() {
        val result = bookMapper.getById(1L)

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(
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
                createdAt = LocalDateTime.of(2023, 1, 1, 10, 0),
                updatedAt = LocalDateTime.of(2023, 1, 1, 10, 0)
            )
        )
    }

    @Test
    fun `getById should return null when book does not exist or is deleted`() {
        val result = bookMapper.getById(999L)
        assertThat(result).isNull()
    }

    @Test
    fun `getCountByCondition should return correct count`() {
        val condition = BookCondition(
            title = "%Kotlin%",
            author = "%山田%",
            publisherId = 1L,
            userId = 100L,
            pageSize = 10,
            currentPage = 1
        )
        val count = bookMapper.getCountByCondition(condition)
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun `getCountByCondition should return 0 when no books match`() {
        val condition = BookCondition(
            title = "%Nonexistent%",
            pageSize = 10,
            currentPage = 1
        )
        val count = bookMapper.getCountByCondition(condition)
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `getListByCondition should return paginated books with conditions`() {
        val condition = BookCondition(
            title = "%Kotlin%",
            pageSize = 10,
            currentPage = 1
        )
        val result = bookMapper.getListByCondition(condition)

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(
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
                createdAt = LocalDateTime.of(2023, 1, 1, 10, 0),
                updatedAt = LocalDateTime.of(2023, 1, 1, 10, 0)
            )
        )
    }

    @Test
    fun `getListByCondition should return empty list when no books match`() {
        val condition = BookCondition(
            title = "%Nonexistent%",
            pageSize = 10,
            currentPage = 1
        )
        val result = bookMapper.getListByCondition(condition)
        assertThat(result).isEmpty()
    }

    @Test
    fun `save should insert book`() {
        val bookCreate = BookCreate(
            title = "Spring Boot入門",
            titleKana = "スプリング ブート ニュウモン",
            author = "佐藤花子",
            publisherId = 2L,
            userId = 101L
        )
        val affectedRows = bookMapper.save(bookCreate)
        assertThat(affectedRows).isEqualTo(1)

        val insertedBook = bookMapper.getById(2L)
        assertThat(insertedBook).isNotNull()
        assertThat(insertedBook?.title).isEqualTo("Spring Boot入門")
        assertThat(insertedBook?.titleKana).isEqualTo("スプリング ブート ニュウモン")
        assertThat(insertedBook?.author).isEqualTo("佐藤花子")
        assertThat(insertedBook?.publisherId).isEqualTo(2L)
        assertThat(insertedBook?.userId).isEqualTo(101L)
    }

    @Test
    fun `delete should soft delete book and return affected rows`() {
        val affectedRows = bookMapper.delete(1L)
        assertThat(affectedRows).isEqualTo(1)
        val deletedBook = bookMapper.getById(1L)
        assertThat(deletedBook).isNull()
    }

    @Test
    fun `deleteBatch should soft delete multiple books and return affected rows`() {
        val affectedRows = bookMapper.deleteBatch(listOf(1L))
        assertThat(affectedRows).isEqualTo(1)
        val deletedBook = bookMapper.getById(1L)
        assertThat(deletedBook).isNull()
    }

    @Test
    fun `update should update book and return affected rows`() {
        val book = Book(
            id = 1L,
            title = "Kotlin入門 Updated",
            titleKana = "コトリン ニュウモン",
            author = "山田太郎",
            publisherId = 1L,
            userId = 100L
        )
        val affectedRows = bookMapper.update(book)

        assertThat(affectedRows).isEqualTo(1)
        val updatedBook = bookMapper.getById(1L)
        assertThat(updatedBook).isNotNull()
        assertThat(updatedBook?.title).isEqualTo("Kotlin入門 Updated")
        assertThat(updatedBook?.titleKana).isEqualTo("コトリン ニュウモン")
        assertThat(updatedBook?.author).isEqualTo("山田太郎")
        assertThat(updatedBook?.publisherId).isEqualTo(1L)
        assertThat(updatedBook?.userId).isEqualTo(100L)
    }
}