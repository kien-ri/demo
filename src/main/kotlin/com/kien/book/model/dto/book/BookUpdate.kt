package com.kien.book.model.dto.book

import com.kien.book.model.Book
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class BookUpdate(
    @field:Min(1) val id: Long,
    @field:NotBlank val title: String,
    @field:NotBlank val titleKana: String,
    @field:NotBlank val author: String,
    @field:Min(1) val publisherId: Long,
    @field:Min(1) val userId: Long,
    @field:Min(0) val price: Int
) {
    fun toEntity() = Book(
        id,
        title,
        titleKana,
        author,
        publisherId,
        userId,
        price
    )
}
