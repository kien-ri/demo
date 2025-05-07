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
import org.junit.jupiter.api.Nested
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
    fun `getById should return BookView with null publisherName when publisher is deleted`() {
        val result = bookMapper.getById(5L)

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(
            BookView(
                id = 5L,
                title = "データベース基礎",
                titleKana = "データベース キソ",
                author = "山本花子",
                publisherId = null,
                publisherName = null,
                userId = 102L,
                userName = "鈴木一郎",
                isDeleted = false,
                createdAt = LocalDateTime.of(2023, 2, 2, 10, 0),
                updatedAt = LocalDateTime.of(2023, 2, 2, 10, 0)
            )
        )
    }

    @Test
    fun `getById should return BookView with null userName when user is deleted`() {
        val result = bookMapper.getById(6L)

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(
            BookView(
                id = 6L,
                title = "アルゴリズム入門",
                titleKana = "アルゴリズム ニュウモン",
                author = "田中一",
                publisherId = 3L,
                publisherName = "文芸出版社",
                userId = null,
                userName = null,
                isDeleted = false,
                createdAt = LocalDateTime.of(2023, 2, 3, 10, 0),
                updatedAt = LocalDateTime.of(2023, 2, 3, 10, 0)
            )
        )
    }

    @Test
    fun `getById should return BookView with null publisherName and userName when both are deleted`() {
        val result = bookMapper.getById(7L)

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(
            BookView(
                id = 7L,
                title = "ネットワーク入門",
                titleKana = "ネットワーク ニュウモン",
                author = "高橋健",
                publisherId = null,
                publisherName = null,
                userId = null,
                userName = null,
                isDeleted = false,
                createdAt = LocalDateTime.of(2023, 2, 4, 10, 0),
                updatedAt = LocalDateTime.of(2023, 2, 4, 10, 0)
            )
        )
    }

    @Test
    fun `getById should return null when book does not exist`() {
        val result = bookMapper.getById(999L)
        assertThat(result).isNull()
    }

    @Test
    fun `getById should return null when book is logically deleted`() {
        val result = bookMapper.getById(3L)
        assertThat(result).isNull()
    }

    @Test
    fun `getCountByCondition should return correct count`() {
        val condition1 = BookCondition(
            title = "入門",
            author = null,
            publisherId = 3L,
            userId = 102L,
            pageSize = 10,
            currentPage = 1
        )
        val condition2 = BookCondition(
            title = null,
            author = "田中",
            publisherId = null,
            userId = null,
            pageSize = 10,
            currentPage = 1
        )
        assertThat(bookMapper.getCountByCondition(condition1)).isEqualTo(1)
        assertThat(bookMapper.getCountByCondition(condition2)).isEqualTo(2)
    }

    @Test
    fun `getCountByCondition should return 0 when no books match`() {
        val condition1 = BookCondition(
            title = "Nonexistent",
            pageSize = 10,
            currentPage = 1
        )
        val condition2 = BookCondition(
            author = "Nonexistent",
            pageSize = 10,
            currentPage = 1
        )
        val condition3 = BookCondition(
            publisherId = 999L,
            pageSize = 10,
            currentPage = 1
        )
        val condition4 = BookCondition(
            userId = 999L,
            pageSize = 10,
            currentPage = 1
        )

        assertThat(bookMapper.getCountByCondition(condition1)).isEqualTo(0)
        assertThat(bookMapper.getCountByCondition(condition2)).isEqualTo(0)
        assertThat(bookMapper.getCountByCondition(condition3)).isEqualTo(0)
        assertThat(bookMapper.getCountByCondition(condition4)).isEqualTo(0)
    }

    @Test
    fun `getListByCondition should return multiple books when multiple records match`() {
        val condition = BookCondition(
            title = "入門",
            pageSize = 10,
            currentPage = 1
        )
        val result = bookMapper.getListByCondition(condition)

        assertThat(result).hasSize(5)
        assertThat(result).contains(
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
                createdAt = LocalDateTime.of(2023, 1, 1, 10, 0),
                updatedAt = LocalDateTime.of(2023, 1, 1, 10, 0)
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
                createdAt = LocalDateTime.of(2023, 2, 1, 10, 0),
                updatedAt = LocalDateTime.of(2023, 2, 1, 10, 0)
            )
        )
    }

    @Test
    fun `getListByCondition should return empty list when title does not match`() {
        val condition = BookCondition(
            title = "Nonexistent",
            pageSize = 10,
            currentPage = 1
        )
        val result = bookMapper.getListByCondition(condition)
        assertThat(result).isEmpty()
    }

    @Test
    fun `getListByCondition should return empty list when author does not match`() {
        val condition = BookCondition(
            author = "Nonexistent",
            pageSize = 10,
            currentPage = 1
        )
        val result = bookMapper.getListByCondition(condition)
        assertThat(result).isEmpty()
    }

    @Test
    fun `getListByCondition should return empty list when publisherId does not match`() {
        val condition = BookCondition(
            publisherId = 999L,
            pageSize = 10,
            currentPage = 1
        )
        val result = bookMapper.getListByCondition(condition)
        assertThat(result).isEmpty()
    }

    @Test
    fun `getListByCondition should return empty list when userId does not match`() {
        val condition = BookCondition(
            userId = 999L,
            pageSize = 10,
            currentPage = 1
        )
        val result = bookMapper.getListByCondition(condition)
        assertThat(result).isEmpty()
    }

    @Test
    fun `getListByCondition should return books with null publisher info when publisher is logically deleted`() {
        val condition = BookCondition(
            publisherId = 4L,
            pageSize = 10,
            currentPage = 1
        )
        val result = bookMapper.getListByCondition(condition)

        assertThat(result).hasSize(2)
        assertThat(result).contains(
            BookView(
                id = 5L,
                title = "データベース基礎",
                titleKana = "データベース キソ",
                author = "山本花子",
                publisherId = null,
                publisherName = null,
                userId = 102L,
                userName = "鈴木一郎",
                isDeleted = false,
                createdAt = LocalDateTime.of(2023, 2, 2, 10, 0),
                updatedAt = LocalDateTime.of(2023, 2, 2, 10, 0)
            ),
            BookView(
                id = 7L,
                title = "ネットワーク入門",
                titleKana = "ネットワーク ニュウモン",
                author = "高橋健",
                publisherId = null,
                publisherName = null,
                userId = null,
                userName = null,
                isDeleted = false,
                createdAt = LocalDateTime.of(2023, 2, 4, 10, 0),
                updatedAt = LocalDateTime.of(2023, 2, 4, 10, 0)
            )
        )
    }

    @Test
    fun `getListByCondition should return books with null user info when user is logically deleted`() {
        val condition = BookCondition(
            userId = 103L,
            pageSize = 10,
            currentPage = 1
        )
        val result = bookMapper.getListByCondition(condition)

        assertThat(result).hasSize(2)
        assertThat(result).contains(
            BookView(
                id = 6L,
                title = "アルゴリズム入門",
                titleKana = "アルゴリズム ニュウモン",
                author = "田中一",
                publisherId = 3L,
                publisherName = "文芸出版社",
                userId = null,
                userName = null,
                isDeleted = false,
                createdAt = LocalDateTime.of(2023, 2, 3, 10, 0),
                updatedAt = LocalDateTime.of(2023, 2, 3, 10, 0)
            ),
            BookView(
                id = 7L,
                title = "ネットワーク入門",
                titleKana = "ネットワーク ニュウモン",
                author = "高橋健",
                publisherId = null,
                publisherName = null,
                userId = null,
                userName = null,
                isDeleted = false,
                createdAt = LocalDateTime.of(2023, 2, 4, 10, 0),
                updatedAt = LocalDateTime.of(2023, 2, 4, 10, 0)
            )
        )
    }

    @Test
    fun `getListByCondition should exclude deleted books`() {
        val condition = BookCondition(
            title = "入門",
            pageSize = 10,
            currentPage = 1
        )
        val result = bookMapper.getListByCondition(condition)

        assertThat(result).hasSize(5)
        assertThat(result).doesNotContain(
            BookView(
                id = 3L,
                title = "PHP入門",
                titleKana = "ピーエイチピー ニュウモン",
                author = "田中太郎",
                publisherId = 2L,
                publisherName = "教育出版社",
                userId = 101L,
                userName = "佐藤花子",
                isDeleted = true,
                createdAt = LocalDateTime.of(2023, 1, 1, 10, 0),
                updatedAt = LocalDateTime.of(2023, 1, 1, 10, 0)
            ),
            BookView(
                id = 8L,
                title = "AI入門",
                titleKana = "エーアイ ニュウモン",
                author = "山田健太",
                publisherId = 5L,
                publisherName = "歴史出版社",
                userId = 104L,
                userName = "中村健太",
                isDeleted = true,
                createdAt = LocalDateTime.of(2023, 2, 7, 10, 0),
                updatedAt = LocalDateTime.of(2023, 2, 7, 10, 0)
            )
        )
    }

    @Test
    fun `getListByCondition should respect LIMIT and OFFSET for pagination`() {
        val condition = BookCondition(
            title = "入門",
            pageSize = 2,
            currentPage = 2
        )
        val result = bookMapper.getListByCondition(condition)

        assertThat(result).hasSize(2)
        assertThat(result).containsExactly(
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
                createdAt = LocalDateTime.of(2023, 2, 1, 10, 0),
                updatedAt = LocalDateTime.of(2023, 2, 1, 10, 0)
            ),
            BookView(
                id = 6L,
                title = "アルゴリズム入門",
                titleKana = "アルゴリズム ニュウモン",
                author = "田中一",
                publisherId = 3L,
                publisherName = "文芸出版社",
                userId = null,
                userName = null,
                isDeleted = false,
                createdAt = LocalDateTime.of(2023, 2, 3, 10, 0),
                updatedAt = LocalDateTime.of(2023, 2, 3, 10, 0)
            )
        )
    }

    @Test
    fun `save should insert book`() {
        val temp = bookMapper.getById(9L)
        assertThat(temp).isEqualTo(null)

        val startTime = LocalDateTime.now()

        val bookCreate = BookCreate(
            title = "Python入門",
            titleKana = "パイソン ニュウモン",
            author = "佐藤花子",
            publisherId = 5L,
            userId = 104L
        )
        val affectedRows = bookMapper.save(bookCreate)
        assertThat(affectedRows).isEqualTo(1)

        val endTime = LocalDateTime.now()

        val insertedBook = bookMapper.getById(9L)
        assertThat(insertedBook).isNotNull()
        assertThat(insertedBook?.title).isEqualTo("Python入門")
        assertThat(insertedBook?.titleKana).isEqualTo("パイソン ニュウモン")
        assertThat(insertedBook?.author).isEqualTo("佐藤花子")
        assertThat(insertedBook?.publisherId).isEqualTo(5L)
        assertThat(insertedBook?.publisherName).isEqualTo("歴史出版社")
        assertThat(insertedBook?.userId).isEqualTo(104L)
        assertThat(insertedBook?.userName).isEqualTo("中村健太")
        assertThat(insertedBook?.createdAt).isNotNull()
        assertThat(insertedBook?.createdAt).isAfterOrEqualTo(startTime)
        assertThat(insertedBook?.createdAt).isBeforeOrEqualTo(endTime)
    }

    @Test
    fun `deleteLogically should soft delete book and return affected rows`() {
        val book = bookMapper.getById(1L)
        assertThat(book).isNotNull()
        assertThat(book?.id).isEqualTo(1L)
        assertThat(book?.isDeleted).isEqualTo(false)

        val affectedRows = bookMapper.deleteLogically(1L)
        assertThat(affectedRows).isEqualTo(1)
        val deletedBook = bookMapper.getById(1L)
        assertThat(deletedBook).isNull()
    }

    @Test
    fun `deleteLogically should return 0 when book id does not exist`() {
        val notExistRow = bookMapper.getById(999L)
        assertThat(notExistRow).isNull()

        val affectedRows = bookMapper.deleteLogically(999L)
        assertThat(affectedRows).isEqualTo(0)
    }

    @Test
    fun `deleteLogically should return 0 when book is already logically deleted`() {
        val deletedRow = bookMapper.getById(3L)
        assertThat(deletedRow).isNull()

        val affectedRows = bookMapper.deleteLogically(3L)
        assertThat(affectedRows).isEqualTo(0)
    }

    @Test
    fun `deleteBatchLogically should soft delete multiple books and return affected rows`() {
        val book1 = bookMapper.getById(1L)
        val book2 = bookMapper.getById(2L)
        val book4 = bookMapper.getById(4L)
        assertThat(book1).isNotNull()
        assertThat(book2).isNotNull()
        assertThat(book4).isNotNull()

        val affectedRows = bookMapper.deleteBatchLogically(listOf(1L, 2L, 4L))
        assertThat(affectedRows).isEqualTo(3)
        assertThat(bookMapper.getById(1L)).isNull()
        assertThat(bookMapper.getById(2L)).isNull()
        assertThat(bookMapper.getById(4L)).isNull()
    }

    @Test
    fun `deleteBatchLogically should return 0 book is deleted or not exsit`() {
        val book3 = bookMapper.getById(3L)
        val book999 = bookMapper.getById(999L)
        assertThat(book3).isNull()
        assertThat(book999).isNull()

        val affectedRows = bookMapper.deleteBatchLogically(listOf(3L, 999L))
        assertThat(affectedRows).isEqualTo(0)
        assertThat(bookMapper.getById(3L)).isNull()
        assertThat(bookMapper.getById(999L)).isNull()
    }

    @Nested
    inner class UpdateTest {
        @Test
        fun `update should update book and return affected rows`() {
            val book = Book(
                id = 1L,
                title = "最新版Kotlin",
                titleKana = "サイシンバン コトリン",
                author = "田中四",
                publisherId = 2L,
                userId = 101L,
            )

            val pre = bookMapper.getById(1L)
            assertThat(pre?.title).isEqualTo("Kotlin入門")
            assertThat(pre?.titleKana).isEqualTo("コトリン ニュウモン")
            assertThat(pre?.author).isEqualTo("山田太郎")
            assertThat(pre?.publisherId).isEqualTo(1L)
            assertThat(pre?.userId).isEqualTo(100L)

            val startTime = LocalDateTime.now()
            val affectedRows = bookMapper.update(book)
            val endTime = LocalDateTime.now()

            assertThat(affectedRows).isEqualTo(1)
            val updatedBook = bookMapper.getById(1L)
            assertThat(updatedBook).isNotNull()
            assertThat(updatedBook?.title).isEqualTo("最新版Kotlin")
            assertThat(updatedBook?.titleKana).isEqualTo("サイシンバン コトリン")
            assertThat(updatedBook?.author).isEqualTo("田中四")
            assertThat(updatedBook?.publisherId).isEqualTo(2L)
            assertThat(updatedBook?.userId).isEqualTo(101L)
            assertThat(updatedBook?.updatedAt).isAfterOrEqualTo(startTime)
            assertThat(updatedBook?.updatedAt).isBeforeOrEqualTo(endTime)
        }

        @Test
        fun `update should return 0 when book id does not exist`() {
            val book = Book(
                id = 999L,
                title = "存在しない書籍",
                titleKana = "ソンザイシナイショセキ",
                author = "佐藤花子",
                publisherId = 5L,
                userId = 104L
            )

            val notExistBook = bookMapper.getById(999L)
            assertThat(notExistBook).isNull()

            val affectedRows = bookMapper.update(book)
            assertThat(affectedRows).isEqualTo(0)
            val bookAfterUpdate = bookMapper.getById(999L)
            assertThat(bookAfterUpdate).isNull()
        }

        @Test
        fun `update should return 0 when book is deleted`() {
            val book = Book(
                id = 3L,
                title = "削除された書籍",
                titleKana = "サクジョサレタショセキ",
                author = "佐藤花子",
                publisherId = 5L,
                userId = 104L
            )

            val deletedBook = bookMapper.getById(3L)
            assertThat(deletedBook).isNull()

            val affectedRows = bookMapper.update(book)
            assertThat(affectedRows).isEqualTo(0)
            val bookAfterUpdate = bookMapper.getById(3L)
            assertThat(bookAfterUpdate).isNull()
        }
    }
}