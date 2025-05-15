package com.kien.book.common

open class CustomException(
    override val message: String
): RuntimeException(message)

class NotFoundCustomException(
    message: String,
    val field: String,
    val value: Any?,
): CustomException(message)

class InvalidParamCustomException(
    message: String,
    val field: String,
    val value: Any?,
): CustomException(message)

class NonExistentForeignKeyCustomException(
    message: String,
    val field: String,
    val value: Any?,
): CustomException(message)

class DuplicateKeyCustomException(
    message: String,
    val field: String,
    val value: Any?
): CustomException(message)