package com.kien.book.model.condition

import jakarta.validation.constraints.Min

data class BookCondition(
    var title: String? = null,
    var author: String? = null,
    val publisherId: Long? = null,
    val userId: Long? = null,
    @field:Min(value = 1)
    val pageSize: Int,
    @field:Min(value = 1)
    val currentPage: Int
)
