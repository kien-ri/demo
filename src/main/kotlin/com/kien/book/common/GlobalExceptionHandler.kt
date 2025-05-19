package com.kien.book.common

import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.sql.SQLIntegrityConstraintViolationException
import org.springframework.web.servlet.resource.NoResourceFoundException

@ControllerAdvice
@ResponseBody
class GlobalExceptionHandler {

    @Value("\${messages.errors.duplicateKey}")
    val MSG_DUPLICATE_KEY: String = ""

    @Value("\${messages.errors.nonExistentFK}")
    val MSG_NONEXISTENT_FK: String = ""

    @Value("\${messages.errors.invalidRequest}")
    val MSG_INVALID_REQUEST: String = ""

    @Value("\${messages.errors.invalidValue}")
    val MSG_INVALID_VALUE: String = ""

    @Value("\${messages.errors.unexpectedError}")
    val MSG_UNEXPECTED_ERROR: String = ""

    @Value("\${messages.errors.typeMissmatch}")
    val MSG_TYPE_MISSMATCH: String = ""

    val MSG_STR: String = "message"

    @ExceptionHandler(NonExistentForeignKeyCustomException::class)
    fun nonExistentForeignKeyCustomExceptionHandler(e: NonExistentForeignKeyCustomException): ResponseEntity<Any> {
        val responseBody = mapOf(
            e.field to e.value,
            MSG_STR to e.message
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseBody)
    }

    @ExceptionHandler(NotFoundCustomException::class)
    fun notFoundCustomExceptionHandler(e: NotFoundCustomException): ResponseEntity<Any> {
        val responseBody = mapOf(
            e.field to e.value,
            MSG_STR to e.message
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseBody)
    }

    @ExceptionHandler(InvalidParamCustomException::class)
    fun invalidParamExceptionHandler(e: InvalidParamCustomException): ResponseEntity<Any> {
        val responseBody = mapOf(
            e.field to e.value,
            MSG_STR to e.message
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody)
    }

    @ExceptionHandler(CustomException::class)
    fun customExceptionHandler(e: CustomException): ResponseEntity<String> {
        return ResponseEntity.unprocessableEntity().body(e.message)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(e: MethodArgumentNotValidException): ResponseEntity<List<Map<String, String>>> {
        val errors = e.bindingResult.fieldErrors.map { error: FieldError ->
            mapOf(
                error.field to (error.rejectedValue?.toString() ?: ""),
                MSG_STR to MSG_INVALID_VALUE
            )
        }
        return ResponseEntity(errors, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidationException(e: HandlerMethodValidationException): ResponseEntity<List<Map<String, String>>> {
        val errors = e.parameterValidationResults
            .filter { it.resolvableErrors.isNotEmpty() }
            .map { result ->
                val param = result.methodParameter.parameterName ?: "unknown"
                val rejectedValue = result.argument?.toString() ?: "null"

                mapOf(
                    param to rejectedValue,
                    MSG_STR to MSG_INVALID_VALUE
                )
            }

        return ResponseEntity(errors, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<Any> {
        val responseBody = mapOf(
            e.name to e.value,
            MSG_STR to MSG_TYPE_MISSMATCH
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody)
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(e: NoResourceFoundException): ResponseEntity<Any> {
        val responseBody = mapOf(
            e.httpMethod to "/" + e.resourcePath,
            MSG_STR to MSG_INVALID_REQUEST
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody)
    }

    /**
     * MySQL 主キー重複エラー
     */
    @ExceptionHandler(DuplicateKeyException::class)
    fun handleDuplicateKey(e: DuplicateKeyException): ResponseEntity<Any> {
        val responseBody = object {
            val error: String = MSG_DUPLICATE_KEY
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(responseBody)
    }

    /**
     * MySQL 外部キー制約エラー(存在しない外部キーを指定した場合)
     */
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
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseBody)
                }
            }
        }
        throw e
    }

    @ExceptionHandler(RuntimeException::class)
    fun exceptionHandler(e: RuntimeException): ResponseEntity<Any> {
        val responseBody = object {
            val error: String = MSG_UNEXPECTED_ERROR + e.message
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(responseBody)
    }

}
