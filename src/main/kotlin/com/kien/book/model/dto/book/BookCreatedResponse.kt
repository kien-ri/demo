package com.kien.book.model.dto.book

/*
    書籍情報新規登録成功のレスポンスボディに使う
 */
data class BookCreatedResponse(
    val id: Long,
    val title: String?
)
