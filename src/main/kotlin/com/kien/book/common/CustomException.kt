package com.kien.book.common

import com.fasterxml.jackson.annotation.JsonIncludeProperties

// JSONに、上のRuntimeExceptionの情報を出力しないようにする
@JsonIncludeProperties("message")
open class CustomException(
    override val message: String
): RuntimeException(message)

@JsonIncludeProperties("message")
class NotFoundCustomException(
    message: String,
    val field: String,
    val value: Any?,
): CustomException(message)

@JsonIncludeProperties("message")
data class InvalidParamCustomException(
    override val message: String,
    val field: String,
    val value: Any?,
): CustomException(message)

@JsonIncludeProperties("message")
class NonExistentForeignKeyCustomException(
    message: String,
    val field: String,
    val value: Any?,
): CustomException(message)

@JsonIncludeProperties("message")
class DuplicateKeyCustomException(
    message: String,
    val field: String,
    val value: Any?
): CustomException(message)
