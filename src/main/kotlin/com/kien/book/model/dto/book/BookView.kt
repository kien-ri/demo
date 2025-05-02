package com.kien.book.model.dto.book

import com.kien.book.model.Book
import java.time.LocalDateTime

data class BookView(
    val id: Long?,
    val title: String?,
    val titleKana: String?,
    val author: String?,
    val publisherId: Long?,
    val publisherName: String?,
    val userId: Long?,
    val userName: String?,
    val price: Int?,
    val isDeleted: Boolean = false,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)