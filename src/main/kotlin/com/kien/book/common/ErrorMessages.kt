package com.kien.book.common

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class ErrorMessages(
    @Value("\${messages.errors.invalidRequest}")
    val invalidRequest: String,

    @Value("\${messages.errors.invalidValue}")
    val invalidValue: String,

    @Value("\${messages.errors.unexpectedError}")
    val unexpectedError: String
)