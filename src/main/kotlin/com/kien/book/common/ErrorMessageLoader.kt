package com.kien.book.common

import org.yaml.snakeyaml.Yaml
import java.util.*

class ErrorMessageLoader {
    private val messages: Map<String, String>

    init {
        val yaml = Yaml()
        val inputStream = this::class.java.classLoader.getResourceAsStream("messages.yml")
            ?: throw IllegalStateException("messages.ymlを見つけませんでした。/resourcesを確認してください。")
        val yamlContent = inputStream.bufferedReader().use { it.readText() }

        val yamlMap = yaml.load<Map<String, Map<String, String>>>(yamlContent)
        messages = yamlMap["errors"] ?: throw IllegalStateException("messages.ymlにerrorsの階層がありません。")
    }

    fun getMessage(code: String): String {
        return messages[code] ?: throw NoSuchElementException("以下指定のエラーメッセージを取得できませんでした: $code")
    }
}