package com.kien.book.model.delete

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive

data class DeleteBooksRequest(
    @field:NotEmpty
    @field:Valid
    val ids: List<@Positive Long>
)
