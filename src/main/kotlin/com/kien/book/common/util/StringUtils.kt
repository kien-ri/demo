package com.kien.book.common.util

object StringUtils {
    fun String.toCamelCase(): String {
        return this.split("_").mapIndexed { index, word ->
            if (index == 0) word else word.replaceFirstChar { it.uppercase() }
        }.joinToString("")
    }
}
