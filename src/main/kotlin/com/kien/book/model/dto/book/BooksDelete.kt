package com.kien.book.model.dto.book

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive

data class BooksDelete(
    @field:NotEmpty
    @field:Valid
    val ids: List<@Positive Long>
)
