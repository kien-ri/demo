package com.kien.book.common

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@ControllerAdvice
@ResponseBody
class GlobalExceptionHandler {

    @Value("\${messages.errors.invalidRequest}")
    val MSG_INVALID_REQUEST: String = ""

    @Value("\${messages.errors.invalidValue}")
    val MSG_INVALID_VALUE: String = ""

    @Value("\${messages.errors.unexpectedError}")
    val MSG_UNEXPECTED_ERROR: String = ""

    @Value("\${messages.errors.typeMissmatch}")
    val MSG_TYPE_MISSMATCH: String = ""

    val MSG_STR: String = "message"

    @ExceptionHandler(CustomException::class)
    fun customExceptionHandler(e: CustomException): ResponseEntity<Map<String, Any?>> {
        val responseBody = mapOf(
            e.field to e.value,
            MSG_STR to e.message
        )
        return ResponseEntity.status(e.httpStatus).body(responseBody)
    }

    /**
     * request body のプロパティが要件を満たさないエラー
     */
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

    /**
     * パラメータの変数型が違うエラー
     */
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

    /**
     * パラメータの値が要件を満たさないエラー(id　> 0 など)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<Any> {
        val responseBody = mapOf(
            e.name to e.value,
            MSG_STR to MSG_TYPE_MISSMATCH
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody)
    }

    /**
     * リクエストURLの間違いで発生するエラー
     */
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(e: NoResourceFoundException): ResponseEntity<Any> {
        val responseBody = mapOf(
            e.httpMethod to "/" + e.resourcePath,
            MSG_STR to MSG_INVALID_REQUEST
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody)
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
