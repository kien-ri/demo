package com.kien.book.common

import com.fasterxml.jackson.annotation.JsonIncludeProperties

// JSONに、上のRuntimeExceptionの情報を出力しないようにする
@JsonIncludeProperties("message")
open class CustomException(
    override val message: String
): RuntimeException(message)

@JsonIncludeProperties("message", "field", "value")
class NotFoundCustomException(
    message: String,
    val field: String,
    val value: Any?,
): CustomException(message)

@JsonIncludeProperties("message", "field", "value")
class InvalidParamCustomException(
    message: String,
    val field: String,
    val value: Any?,
): CustomException(message)

@JsonIncludeProperties("message", "field", "value")
class NonExistentForeignKeyCustomException(
    message: String,
    val field: String,
    val value: Any?,
): CustomException(message)

@JsonIncludeProperties("message", "field", "value")
class DuplicateKeyCustomException(
    message: String,
    val field: String,
    val value: Any?
): CustomException(message)
