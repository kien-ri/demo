package com.kien.book.repository

import com.kien.book.model.Book
import com.kien.book.model.dto.book.BookView
import org.junit.jupiter.api.Test
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.dao.DataIntegrityViolationException
import java.sql.SQLIntegrityConstraintViolationException
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class BookMapperTest {

    @Autowired
    private lateinit var bookMapper: BookMapper

    /**
     * テスト実行する度に@Sqlで指定したSQLファイル内のINSERT文でテストデータが挿入されます。
     */
    @Nested
    @Sql(
        scripts = [
                    "/repository/data/books/getById/publisher.sql",
                    "/repository/data/books/getById/user.sql",
                    "/repository/data/books/getById/books.sql"
                  ],
        executionPhase = ExecutionPhase.BEFORE_TEST_CLASS
    )
    inner class GetByIdTest {

        @Test
        fun `return BookView when book exists`() {
            val bookId = 1L
            val result = bookMapper.getById(bookId)

            assertThat(result).isNotNull()
            assertThat(result).isEqualTo(
                BookView(
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
                    createdAt = LocalDateTime.of(2023, 1, 1, 10, 0),
                    updatedAt = LocalDateTime.of(2023, 1, 1, 10, 0)
                )
            )
        }

        /**
         * テーブルに存在しないIDの行を取得するテスト
         */
        @Test
        fun `return null when book does not exist`() {
            val result = bookMapper.getById(999L)
            assertThat(result).isNull()
        }

        /**
         * テーブルに存在するIDだが、is_deletedフラグがtrueのデータを取得するテスト
         */
        @Test
        fun `return null when book is logically deleted`() {
            val result = bookMapper.getById(2L)
            assertThat(result).isNull()
        }

        /**
         * 書籍情報と結びつく出版社情報が、そのテーブルで論理削除された場合をテスト
         */
        @Test
        fun `return BookView with null publisher info when publisher is deleted`() {
            val bookId = 3L
            val result = bookMapper.getById(bookId)

            assertThat(result).isNotNull()
            assertThat(result).isEqualTo(
                BookView(
                    id = bookId,
                    title = "Java入門",
                    titleKana = "ジャバー ニュウモン",
                    author = "田中太郎",
                    publisherId = null,
                    publisherName = null,
                    userId = 100L,
                    userName = "テストユーザー",
                    price = 2000,
                    isDeleted = false,
                    createdAt = LocalDateTime.of(2023, 1, 1, 10, 0),
                    updatedAt = LocalDateTime.of(2023, 1, 1, 10, 0)
                )
            )
        }

        /**
         * 書籍情報と結びつく登録者(ユーザ)情報が、そのテーブルで論理削除された場合をテスト
         */
        @Test
        fun `return BookView with null user info when user is deleted`() {
            // id = 4 の書籍情報の登録者(ユーザ)ID = 101、ユーザテーブルのID = 101の行は論理削除されている
            val bookId = 4L
            val result = bookMapper.getById(bookId)

            assertThat(result).isNotNull()
            assertThat(result).isEqualTo(
                BookView(
                    id = bookId,
                    title = "Spring Boot 入門",
                    titleKana = "スプリング ブート ニュウモン",
                    author = "佐藤次郎",
                    publisherId = 1L,
                    publisherName = "技術出版社",
                    userId = null,
                    userName = null,
                    price = 3000,
                    isDeleted = false,
                    createdAt = LocalDateTime.of(2023, 2, 1, 10, 0),
                    updatedAt = LocalDateTime.of(2023, 2, 1, 10, 0)
                )
            )
        }
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
    @Sql(scripts = ["/schema.sql"], executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
    @Sql(
        scripts = [
            "/repository/data/books/update/publisher.sql",
            "/repository/data/books/update/user.sql",
            "/repository/data/books/update/books.sql"
        ],
        executionPhase = ExecutionPhase.BEFORE_TEST_CLASS
    )
    inner class UpdateTest {
        @Test
        fun `update should update book and return affected rows`() {

            val current = LocalDateTime.of(2025, 5, 15, 16, 10, 30)
            val book = Book(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 100L,
                price = 4200,
                updatedAt = current
            )

            val expectedResult = BookView(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                publisherName = "技術出版社",
                userId = 100L,
                userName = "テストユーザー",
                price = 4200,
                isDeleted = false,
                createdAt = LocalDateTime.of(2023, 1, 1, 10, 0, 0),
                updatedAt = current
            )

            val pre = bookMapper.getById(1L)
            assertThat(pre?.title).isEqualTo("Kotlin入門")

            val affectedRows = bookMapper.update(book)
            assertThat(affectedRows).isEqualTo(1)

            val updatedBook = bookMapper.getById(1L)
            assertThat(updatedBook).isEqualTo(expectedResult)
        }

        @Test
        fun `return 0 when book does not exist`() {
            val current = LocalDateTime.of(2025, 5, 15, 16, 10, 30)
            val book = Book(
                id = 999L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 100L,
                price = 4200,
                updatedAt = current
            )

            val nothing = bookMapper.getById(999L)
            assertThat(nothing).isNull()

            val affectedRows = bookMapper.update(book)
            assertThat(affectedRows).isEqualTo(0)
        }

        @Test
        fun `return 0 when book is logically deleted`() {
            val current = LocalDateTime.of(2025, 5, 15, 16, 10, 30)
            val book = Book(
                id = 2L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 100L,
                price = 4200,
                updatedAt = current
            )

            val nothing = bookMapper.getById(2L)
            assertThat(nothing).isNull()

            val affectedRows = bookMapper.update(book)
            assertThat(affectedRows).isEqualTo(0)
        }

        @Test
        fun `throw exception when publisherId does not exist`() {
            val current = LocalDateTime.of(2025, 5, 15, 16, 10, 30)
            val book = Book(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 999L,
                userId = 100L,
                price = 4200,
                updatedAt = current
            )

            val e = assertFailsWith<DataIntegrityViolationException> {
                bookMapper.update(book)
            }
            val rootCause = e.rootCause
            assertThat(rootCause is SQLIntegrityConstraintViolationException)
            val errorCode = (rootCause as SQLIntegrityConstraintViolationException).errorCode
            assertThat(errorCode).isEqualTo(1452)
        }

        @Test
        fun `throw exception when userId does not exist`() {
            val current = LocalDateTime.of(2025, 5, 15, 16, 10, 30)
            val book = Book(
                id = 1L,
                title = "Kotlin応用ガイド",
                titleKana = "コトリン オウヨウ ガイド",
                author = "佐藤次郎",
                publisherId = 1L,
                userId = 999L,
                price = 4200,
                updatedAt = current
            )

            val e = assertFailsWith<DataIntegrityViolationException> {
                bookMapper.update(book)
            }
            val rootCause = e.rootCause
            assertThat(rootCause is SQLIntegrityConstraintViolationException)
            val errorCode = (rootCause as SQLIntegrityConstraintViolationException).errorCode
            assertThat(errorCode).isEqualTo(1452)
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
