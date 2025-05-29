package com.kien.book.common

import org.springframework.http.HttpStatus

data class CustomException(
    override val message: String,
    val httpStatus: HttpStatus,
    val field: String,
    val value: Any?,
): RuntimeException(message)
