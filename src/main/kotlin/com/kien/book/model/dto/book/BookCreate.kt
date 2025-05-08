package com.kien.book.model.dto.book

import com.kien.book.model.Book
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

// 書籍情報新規登録するのに必要な情報をまとめたDTOクラス
data class BookCreate(

    @field:Min(1)
    val id: Long?,

    // String?（null許容）に設定した理由：
    // JSONリクエストでtitleが欠落した場合、Jacksonがnullを割り当て、@NotBlankによるバリデーションエラーをMethodArgumentNotValidExceptionで捕捉できるようにするため。
    // 非null型（String）にすると、Jacksonがデシリアライズに失敗し、JsonMappingExceptionが発生するため、バリデーションエラーの一貫したハンドリングが困難になる。
    @field:NotBlank
    val title: String?,

    @field:NotBlank
    val titleKana: String?,

    @field:NotBlank
    val author: String?,

    @field:Min(1)
    val publisherId: Long,

    @field:Min(1)
    val userId: Long,

    @field:NotNull
    @field:Min(0)
    val price: Int?
) {
    fun toEntity() = Book(
        id = id,
        title = title,
        titleKana =  titleKana,
        author = author,
        publisherId = publisherId,
        userId = userId,
        price = price
    )
}
