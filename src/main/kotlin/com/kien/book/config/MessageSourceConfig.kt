package com.kien.book.config

import com.kien.book.common.ErrorMessageLoader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ErrorMessageConfig {

    @Bean
    fun errorMessageLoader(): ErrorMessageLoader {
        return ErrorMessageLoader()
    }
}