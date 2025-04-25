package com.kien.book.model.dto.book

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class BookCreate(
    @field:NotBlank
    val title: String,

    @field:NotBlank
    val titleKana: String,

    @field:NotBlank
    val author: String,

    @field:Min(value = 1)
    val publisherId: Long,

    @field:Min(value = 1)
    val userId: Long
)
