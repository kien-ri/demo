package com.kien.book.common

data class CustomException(
    override val message: String
): RuntimeException(message)

data class NotFoundException(
    override val message: String,

): RuntimeException(message)
