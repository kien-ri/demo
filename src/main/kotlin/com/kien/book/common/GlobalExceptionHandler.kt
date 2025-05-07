package com.kien.book.common

import org.springframework.context.MessageSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.support.RequestContextUtils

@ControllerAdvice
@ResponseBody
class GlobalExceptionHandler(private val eml: ErrorMessageLoader) {

    @ExceptionHandler(CustomException::class)
    fun customExceptionHandler(e: CustomException): ResponseEntity<String> {
        return ResponseEntity.unprocessableEntity().body(e.message)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(e: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> {
        val errors = e.bindingResult.fieldErrors.associate { error ->
            error.field to (error.defaultMessage ?: eml.getMessage("invalidValue"))
        }
        return ResponseEntity(errors, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidationException(e: HandlerMethodValidationException): ResponseEntity<Map<String, String>> {
        val errors = e.allErrors.associate { error ->
            val fieldName = when (error) {
                is FieldError -> error.field
                else -> error.codes?.lastOrNull()?.split(".")?.lastOrNull() ?: "unknown"
            }
            fieldName to (error.defaultMessage ?: eml.getMessage("invalidValue"))
        }
        return ResponseEntity(errors, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<Any> {
        val responseBody = object {
            val error: String = eml.getMessage("invalidRequest")
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody)
    }

    @ExceptionHandler(RuntimeException::class)
    fun exceptionHandler(e: RuntimeException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(eml.getMessage("unexpectedError") + e.message)
    }

}