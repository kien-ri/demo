package com.kien.book.model.dto.book

import org.springframework.http.HttpStatus

/**
 * 一括登録/更新APIの戻り値
 */
data class BookBatchProcessedResult(
    val httpStatus: HttpStatus,
    val successfulItems: List<ProcessedBook>,
    val failedItems: List<ProcessedBook>
)

// 書籍の基礎情報
data class ProcessedBook(
    val id: Long?,
    val title: String?,
    val error: Exception?
)
