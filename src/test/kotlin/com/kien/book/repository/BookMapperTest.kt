package com.kien.book.repository

import com.kien.book.model.Book
import com.kien.book.mapper.BooksTestMapper
import com.kien.book.model.dto.book.BookView
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
import org.junit.jupiter.api.*
import org.springframework.dao.DuplicateKeyException
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

        @BeforeEach
        fun `resetAutoIncrement`() {
            booksTestMapper.resetAutoIncrement()
        }

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
    }
}
