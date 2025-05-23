package com.kien.book.model.dto.book

import com.kien.book.model.Book
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

/**
 * DTO
 * 書籍情報更新に必要なパラメータのまとめ
 */
data class BookUpdate(
    @field:Min(1)
    val id: Long,

    val title: String?,

    val titleKana: String?,

    val author: String?,

    @field:Min(1)
    val publisherId: Long?,

    @field:Min(1)
    val userId: Long?,

    @field:Min(0)
    val price: Int?
) {
    fun toEntity(
        updatedAt: LocalDateTime? = null
    ) = Book(
        id,
        title,
        titleKana,
        author,
        publisherId,
        userId,
        price
    )
}
