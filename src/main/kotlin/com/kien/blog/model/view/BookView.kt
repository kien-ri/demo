package com.kien.blog.model.view

import java.time.LocalDateTime

data class BookView(
    val id: Long?,
    val title: String?,
    val titleKana: String?,
    val author: String?,
    val publisherName: String?,
    val userName: String?,
    val isDeleted: Boolean = false,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
