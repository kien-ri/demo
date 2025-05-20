package com.kien.book.common.util

import org.springframework.dao.DataIntegrityViolationException
import java.sql.SQLIntegrityConstraintViolationException

object DBExceptionUtils {
    fun isForeignKeyViolation(e: DataIntegrityViolationException): Boolean {
        val rootCause = e.rootCause
        return rootCause is SQLIntegrityConstraintViolationException && rootCause.errorCode == 1452
    }

    fun extractForeignKeyColumn(errorMessage: String): String? {
        val regex = Regex("FOREIGN KEY \\(`(\\w+)`\\)")
        val matchResult = regex.find(errorMessage)
        return matchResult?.groupValues?.get(1)
    }
}
