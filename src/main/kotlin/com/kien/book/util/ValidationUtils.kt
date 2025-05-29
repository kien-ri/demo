package com.kien.book.util

import com.kien.book.common.CustomException
import org.springframework.http.HttpStatus

object ValidationUtils {

    // TODO: 引数のerrorMsgが必要ないように
    fun validatePositiveId(id: Long?, fieldName: String, errorMsg: String) {
        if (id != null && id <= 0) {
            throw CustomException(
                message = errorMsg,
                httpStatus = HttpStatus.BAD_REQUEST,
                field = fieldName,
                value = id
            )
        }
    }
}
