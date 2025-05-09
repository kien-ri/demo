package com.kien.book.common

import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.sql.SQLIntegrityConstraintViolationException

@ControllerAdvice
@ResponseBody
class GlobalExceptionHandler {

    @Value("\${messages.errors.duplicateKey}")
    val MSG_DUPLICATE_KEY: String = ""

    @Value("\${messages.errors.nonExistentFK}")
    val MSG_NONEXISTENT_FK: String = ""

    @ExceptionHandler(CustomException::class)
    fun customExceptionHandler(e: CustomException): ResponseEntity<String> {
        return ResponseEntity.unprocessableEntity().body(e.message)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(e: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> {
        val errors = e.bindingResult.fieldErrors.associate { error ->
            error.field to (error.defaultMessage ?: "入力された値が無効です。")
        }
        return ResponseEntity(errors, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidationException(e: HandlerMethodValidationException): ResponseEntity<Map<String, String>> {
        val errors = e.getAllErrors().associate { error ->
            val fieldName = when (error) {
                is FieldError -> error.field
                else -> error.codes?.lastOrNull()?.split(".")?.lastOrNull() ?: "unknown"
            }
            fieldName to (error.defaultMessage ?: "入力された値が無効です。")
        }
        return ResponseEntity(errors, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("無効なリクエストです。URLをチェックしてください。")
    }

    @ExceptionHandler(DuplicateKeyException::class)
    fun handleDuplicateKey(e: DuplicateKeyException): ResponseEntity<Any> {
        val responseBody = object {
            val error: String = MSG_DUPLICATE_KEY
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(responseBody)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleSQLIntegrityConstrainViolation(e: DataIntegrityViolationException): ResponseEntity<Any> {
        val rootCause = e.rootCause
        if (rootCause is SQLIntegrityConstraintViolationException) {
            val vendorCode = rootCause.errorCode
            when (vendorCode) {
                1452 -> {
                    val responseBody = object {
                        val error: String = MSG_NONEXISTENT_FK
                    }
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(responseBody)
                }
            }
        }
        throw e
    }

    @ExceptionHandler(RuntimeException::class)
    fun exceptionHandler(e: RuntimeException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("予想外のエラーが発生しました。 エラー内容：" + e.message)
    }
}