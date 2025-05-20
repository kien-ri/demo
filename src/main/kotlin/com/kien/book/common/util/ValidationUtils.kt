package com.kien.book.common.util

import com.kien.book.common.InvalidParamCustomException

object ValidationUtils {

    // TODO: 引数のerrorMsgが必要ないように
    fun validatePositiveId(id: Long?, fieldName: String, errorMsg: String) {
        if (id != null && id <= 0) {
            throw InvalidParamCustomException(
                message = errorMsg,
                field = fieldName,
                value = id
            )
        }
    }
}
