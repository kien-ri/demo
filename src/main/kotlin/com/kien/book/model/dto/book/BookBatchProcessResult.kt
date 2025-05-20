package com.kien.book.model.dto.book

sealed class BookBatchProcessResult {
    data class AllSuccess(
        val items: List<SuccessfulItem>
    ) : BookBatchProcessResult()

    data class AllFailure(
        val items: List<FailedItem>
    ) : BookBatchProcessResult()

    data class Partial(
        val successfulItems: List<SuccessfulItem>,
        val failedItems: List<FailedItem>
    ) : BookBatchProcessResult()
}

data class SuccessfulItem(
    val index: Int,
    val id: Long,
    val title: String?
)

data class FailedItem(
    val index: Int,
    // TODO: 一件ずつでないとエラーキャッチが難しい
    //val error: Exception
)