package com.kien.book.common.util

import com.kien.book.common.InvalidParamCustomException
import org.springframework.beans.factory.annotation.Value

object ValidationUtils {
    @Value("\${messages.errors.invalidValue}")
    private val MSG_INVALID_VALUE: String = ""

    fun validatePositiveId(id: Long?, fieldName: String) {
        if (id != null && id <= 0) {
            throw InvalidParamCustomException(
                message = MSG_INVALID_VALUE,
                field = fieldName,
                value = id
            )
        }
    }
}
