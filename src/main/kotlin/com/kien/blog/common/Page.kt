package com.kien.blog.common

data class Page<T> (
    val pageSize: Int,
    val currentPage: Int,
    val totalCount: Int,
    val totalPages: Int,
    val content: List<T>
)
