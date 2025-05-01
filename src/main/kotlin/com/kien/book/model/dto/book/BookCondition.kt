package com.kien.book.model.dto.book

import jakarta.validation.constraints.Min

data class BookCondition(
    val title: String? = null,
    val titleKana: String? = null,
    val author: String? = null,
    val publisherId: Long? = null,
    val userId: Long? = null,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    @field:Min(value = 1)
    val pageSize: Int,
    @field:Min(value = 1)
    val currentPage: Int
)
