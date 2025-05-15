package com.kien.book.repository

import com.kien.book.mapper.BooksTestMapper
import com.kien.book.model.Book
import com.kien.book.model.dto.book.BookView
import org.junit.jupiter.api.Test
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import java.sql.SQLIntegrityConstraintViolationException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
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
    @Sql(scripts = ["/schema.sql"], executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
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
                    id = 1L,
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

    @Nested
    @Sql(scripts = ["/schema.sql"], executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
    @Sql(
        scripts = [
            "/repository/data/books/save/publisher.sql",
            "/repository/data/books/save/user.sql",
            "/repository/data/books/save/books.sql"
        ],
        executionPhase = ExecutionPhase.BEFORE_TEST_CLASS
    )
    inner class SaveTest {

        @Autowired
        private lateinit var booksTestMapper: BooksTestMapper

        /**
         * 新規登録するデータの主キーidを指定せずに登録するテスト
         */
        @Test
        fun `save should insert book without id`() {
            //現在の最大idが4で登録されていることを検証
            val currentMaxId = booksTestMapper.getMaxId()
            assertThat(currentMaxId).isEqualTo(4L)

            val currentTime = LocalDateTime.of(2025, 5, 4, 13, 20, 10)

            val book = Book(
                id = null,
                title = "Python入門",
                titleKana = "パイソン ニュウモン",
                author = "佐藤花子",
                publisherId = 1L,
                userId = 100L,
                price = 2500,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            val affectedRows = bookMapper.save(book)
            // 挿入された行数が1であること
            assertThat(affectedRows).isEqualTo(1)
            // mybatisのuseGeneratedKey機能が正しく挿入されたデータのidを賦与
            assertThat(book.id).isEqualTo(5L)

            val insertedBook = bookMapper.getById(5L)
            val expectedBook = BookView(
                id = 5L,
                title = "Python入門",
                titleKana = "パイソン ニュウモン",
                author = "佐藤花子",
                publisherId = 1L,
                publisherName = "技術出版社",
                userId = 100L,
                userName = "テストユーザー",
                price = 2500,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            assertThat(insertedBook).isEqualTo(expectedBook)
        }

        /**
         * 新規登録するデータの主キーidを指定して登録するテスト
         */
        @Test
        fun `save should insert book with specified id`() {
            // 事前にid=10のデータが存在しないことを検証
            val temp = bookMapper.getById(10L)
            assertThat(temp).isEqualTo(null)

            val currentTime = LocalDateTime.of(2025, 5, 4, 13, 20, 10)

            val book = Book(
                id = 10L,
                title = "Go入門",
                titleKana = "ゴー ニュウモン",
                author = "山本一郎",
                publisherId = 1L,
                userId = 100L,
                price = 2800,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            val affectedRows = bookMapper.save(book)
            // 挿入された行数が1であること
            assertThat(affectedRows).isEqualTo(1)
            // mybatisのuseGeneratedKey機能が正しく挿入されたデータのidを賦与
            assertThat(book.id).isEqualTo(10L)

            val insertedBook = bookMapper.getById(10L)
            val expectedBook = BookView(
                id = 10L,
                title = "Go入門",
                titleKana = "ゴー ニュウモン",
                author = "山本一郎",
                publisherId = 1L,
                publisherName = "技術出版社",
                userId = 100L,
                userName = "テストユーザー",
                price = 2800,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            assertThat(insertedBook).isEqualTo(expectedBook)
        }

        /**
         * idのincrement機能をテストする
         *
         * 1. 最初にID指定なしのINSERTテストし、id = 5のデータをINSERTする
         * 2. 次にID指定でid = 10 のデータをINSERTする
         * 3. 最後にもう一度ID指定なしで登録し、正しくid = 11のプライマリーキーが生成されることを検証
         */
        @Test
        fun `save should respect auto increment after manual id insertion`() {
            // ----------------------------------- 1 -----------------------------------------
            //現在の最大idが4で登録されていることを検証
            val currentMaxId = booksTestMapper.getMaxId()
            assertThat(currentMaxId).isEqualTo(4L)

            val currentTime = LocalDateTime.of(2025, 5, 4, 13, 20, 10)

            val book = Book(
                id = null,
                title = "Java実践ガイド",
                titleKana = "ジャバ ジッセン ガイド",
                author = "田中太郎",
                publisherId = 1L,
                userId = 100L,
                price = 3200,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            val affectedRows = bookMapper.save(book)
            // 挿入された行数が1であること
            assertThat(affectedRows).isEqualTo(1)
            // mybatisのuseGeneratedKey機能が正しく挿入されたデータのidを賦与
            assertThat(book.id).isEqualTo(5L)

            val insertedBook = bookMapper.getById(5L)
            val expectedBook = BookView(
                id = 5L,
                title = "Java実践ガイド",
                titleKana = "ジャバ ジッセン ガイド",
                author = "田中太郎",
                publisherId = 1L,
                publisherName = "技術出版社",
                userId = 100L,
                userName = "テストユーザー",
                price = 3200,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            assertThat(insertedBook).isEqualTo(expectedBook)

            // ----------------------------------- 2 -----------------------------------------
            val book2 = Book(
                id = 10L,
                title = "Kotlin実践ガイド",
                titleKana = "コトリン ジッセン ガイド",
                author = "山田花子",
                publisherId = 1L,
                userId = 100L,
                price = 3500,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            val affectedRows2 = bookMapper.save(book2)
            assertThat(affectedRows2).isEqualTo(1)
            assertThat(book2.id).isEqualTo(10L)

            val insertedBook2 = bookMapper.getById(10L)
            val expectedBook2 = BookView(
                id = 10L,
                title = "Kotlin実践ガイド",
                titleKana = "コトリン ジッセン ガイド",
                author = "山田花子",
                publisherId = 1L,
                publisherName = "技術出版社",
                userId = 100L,
                userName = "テストユーザー",
                price = 3500,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            assertThat(insertedBook2).isEqualTo(expectedBook2)

            // ----------------------------------- 3 -----------------------------------------
            val book3 = Book(
                id = null,
                title = "Spring Bootガイド",
                titleKana = "スプリング ブート ガイド",
                author = "鈴木一郎",
                publisherId = 1L,
                userId = 100L,
                price = 3800,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            val affectedRows3 = bookMapper.save(book3)
            assertThat(affectedRows3).isEqualTo(1)
            assertThat(book3.id).isEqualTo(11L)

            val insertedBook3 = bookMapper.getById(11L)
            val expectedBook3 = BookView(
                id = 11L,
                title = "Spring Bootガイド",
                titleKana = "スプリング ブート ガイド",
                author = "鈴木一郎",
                publisherId = 1L,
                publisherName = "技術出版社",
                userId = 100L,
                userName = "テストユーザー",
                price = 3800,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            assertThat(insertedBook3).isEqualTo(expectedBook3)
        }

        /**
         * 主キー重複の場合の動作をテスト
         *
         * テストデータとして、id=1~4のデータが事前に挿入される。
         * このテスト内でid=1で指定してinsertを試すと、主キー重複のエラーになる。
         */
        @Test
        fun `save should throw DuplicateKeyException when id is duplicated`() {
            val book = Book(
                id = 1L,
                title = "Python入門",
                titleKana = "パイソン ニュウモン",
                author = "佐藤花子",
                publisherId = 1L,
                userId = 100L,
                price = 2500,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            assertFailsWith<DuplicateKeyException> {
                bookMapper.save(book)
            }
        }

        /**
         * 以下の動作を検証する：
         * 指定した外部キーが存在しない場合はDataIntegrityViolationExceptionが投げられ、
         * その中にSQLIntegrityConstraintViolationExceptionとエラーコード1452が含まれる
         */
        @Test
        fun `save should throw DataIntegrityViolationException when publisherId does not exist`() {
            val book = Book(
                id = null,
                title = "Python入門",
                titleKana = "パイソン ニュウモン",
                author = "佐藤花子",
                publisherId = 999L,
                userId = 100L,
                price = 2500,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            val e = assertFailsWith<DataIntegrityViolationException> {
                bookMapper.save(book)
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
        fun `save should throw DataIntegrityViolationException when userId does not exist`() {
            val book = Book(
                id = null,
                title = "Python入門",
                titleKana = "パイソン ニュウモン",
                author = "佐藤花子",
                publisherId = 1L,
                userId = 999L,
                price = 2500,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            val e = assertFailsWith<DataIntegrityViolationException> {
                bookMapper.save(book)
            }
            val rootCause = e.rootCause
            assertThat(rootCause is SQLIntegrityConstraintViolationException)
            val errorCode = (rootCause as SQLIntegrityConstraintViolationException).errorCode
            assertThat(errorCode).isEqualTo(1452)
        }
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
