package com.kien.book.model

import java.time.LocalDateTime

data class Book(
    var id: Long? = null,
    var title: String,
    var titleKana: String,
    var author: String,
    var publisherId: Long,
    var userId: Long,
    var isDeleted: Boolean = false,
    var createdAt: LocalDateTime? = null,
    var updatedAt: LocalDateTime? = null
)