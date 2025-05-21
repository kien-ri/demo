package com.kien.book.model.dto.book

import com.kien.book.model.Book
import jakarta.validation.constraints.Min
import java.time.LocalDateTime
import java.util.UUID

// 書籍情報新規登録するのに必要な情報をまとめたDTOクラス
data class BookCreate(

    // id指定して登録する場合はidを入力し、指定しない場合はnull
    @field:Min(1)
    val id: Long?,

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
    /**
     * UUIDが生成される
     */
    fun toEntity(
        createdAt: LocalDateTime? = null,
        updatedAt: LocalDateTime? = null
    ) = Book(
        id = id,
        uuid = UUID.randomUUID().toString(),
        title = title,
        titleKana = titleKana,
        author = author,
        publisherId = publisherId,
        userId = userId,
        price = price,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
