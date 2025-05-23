package com.kien.book.model.dto.book

import org.springframework.http.HttpStatus

/**
 * 一括登録APIの戻り値
 */
data class BookBatchProcessedResult(
    val httpStatus: HttpStatus,
    // 成功に登録された書籍
    val successfulItems: List<ProcessedBook>,
    // 登録に失敗した書籍
    val failedItems: List<ProcessedBook>
)

// 書籍の基礎情報
data class ProcessedBook(
    val id: Long?,
    val title: String?,
    val error: Exception?
)
