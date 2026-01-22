package tw.nekomimi.nekogram.transtale.source

import android.text.TextUtils
import cn.hutool.core.util.StrUtil
import cn.hutool.http.HttpUtil
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import tw.nekomimi.nekogram.transtale.Translator
import xyz.nextalone.nagram.NaConfig
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

object LLMTranslator : Translator {

    // Provider constants
    const val PROVIDER_OPENAI = 0
    const val PROVIDER_GEMINI = 1
    const val PROVIDER_GROQ = 2
    const val PROVIDER_DEEPSEEK = 3
    const val PROVIDER_XAI = 4
    const val PROVIDER_ZHIPUAI = 5

    private const val MAX_RETRIES = 4
    private const val BASE_DELAY_MS = 1000L

    override suspend fun doTranslate(from: String, to: String, query: String): String {
        if (from == to) {
            return query
        }

        val provider = NaConfig.llmProvider.Int()
        val apiKeys = NaConfig.llmApiKeys.String()
        val apiUrl = NaConfig.llmApiUrl.String()

        if (StrUtil.isBlank(apiKeys)) {
            error("Missing LLM API Keys")
        }
        if (StrUtil.isBlank(apiUrl)) {
            error("Missing LLM API URL")
        }

        val keyList = apiKeys!!.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (keyList.isEmpty()) {
            error("No valid API keys found")
        }

        // Get model based on provider
        val model = when (provider) {
            PROVIDER_OPENAI -> NaConfig.llmOpenAIModel.String() ?: "gpt-4o-mini"
            PROVIDER_GEMINI -> NaConfig.llmGeminiModel.String() ?: "gemini-2.0-flash-exp"
            PROVIDER_GROQ -> NaConfig.llmGroqModel.String() ?: "llama-3.3-70b-versatile"
            PROVIDER_DEEPSEEK -> NaConfig.llmDeepSeekModel.String() ?: "deepseek-chat"
            PROVIDER_XAI -> NaConfig.llmXAIModel.String() ?: "grok-2-latest"
            PROVIDER_ZHIPUAI -> NaConfig.llmZhipuAIModel.String() ?: "GLM-4-Flash"
            else -> "gpt-4o-mini"
        }

        val systemPrompt = NaConfig.llmSystemPrompt.String()
            ?: "You are a professional translation engine. Translate the text to {target_language}, keep the format."
        val temperature = NaConfig.llmTemperature.String()?.toDoubleOrNull() ?: 0.3

        var lastException: Exception? = null

        // Retry logic with exponential backoff
        for (attempt in 0 until MAX_RETRIES) {
            try {
                val apiKey = keyList[attempt % keyList.size]
                val result = performTranslation(
                    apiUrl = apiUrl!!,
                    apiKey = apiKey,
                    model = model,
                    systemPrompt = systemPrompt.replace("{target_language}", getLanguageName(to)),
                    temperature = temperature,
                    query = query
                )
                return result
            } catch (e: Exception) {
                lastException = e
                val errorMessage = e.message ?: ""

                // Check if it's a rate limit error
                if (errorMessage.contains("429") || errorMessage.contains("rate limit", ignoreCase = true)) {
                    if (attempt < MAX_RETRIES - 1) {
                        val delayTime = calculateBackoff(attempt)
                        delay(delayTime)
                        continue
                    }
                }

                // For other errors, retry with next key if available
                if (attempt < MAX_RETRIES - 1 && keyList.size > 1) {
                    delay(500L) // Short delay before trying next key
                    continue
                }
            }
        }

        // All retries failed, throw the last exception
        throw lastException ?: Exception("Translation failed after $MAX_RETRIES attempts")
    }

    private fun calculateBackoff(attempt: Int): Long {
        val exponentialDelay = BASE_DELAY_MS * (2.0.pow(attempt.toDouble())).toLong()
        val jitter = Random.nextLong(0, 1000)
        return min(exponentialDelay + jitter, 30000L) // Cap at 30 seconds
    }

    private suspend fun performTranslation(
        apiUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        temperature: Double,
        query: String
    ): String {
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", query)
            })
        }

        val requestBody = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("temperature", temperature)
        }

        val response = HttpUtil.createPost(apiUrl)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .body(requestBody.toString())
            .timeout(30000)
            .execute()

        if (response.status != 200) {
            error("HTTP ${response.status}: ${response.body()}")
        }

        val jsonResponse = JSONObject(response.body())

        // Check for error in response
        if (jsonResponse.has("error")) {
            val errorObj = jsonResponse.getJSONObject("error")
            val errorMessage = errorObj.optString("message", "Unknown error")
            error(errorMessage)
        }

        // Extract translated text from response
        val choices = jsonResponse.optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            error("No translation result in response")
        }

        val firstChoice = choices.getJSONObject(0)
        val message = firstChoice.optJSONObject("message")
        if (message == null) {
            error("Invalid response format")
        }

        return message.optString("content", "").trim()
    }

    private fun getLanguageName(code: String): String {
        return when (code.lowercase()) {
            "zh-cn", "zh-hans", "zh" -> "Simplified Chinese"
            "zh-tw", "zh-hant", "zh-hk" -> "Traditional Chinese"
            "en", "en-us", "en-gb" -> "English"
            "ja" -> "Japanese"
            "ko" -> "Korean"
            "fr" -> "French"
            "de" -> "German"
            "es" -> "Spanish"
            "ru" -> "Russian"
            "ar" -> "Arabic"
            "pt" -> "Portuguese"
            "it" -> "Italian"
            "nl" -> "Dutch"
            "pl" -> "Polish"
            "tr" -> "Turkish"
            "vi" -> "Vietnamese"
            "th" -> "Thai"
            "id" -> "Indonesian"
            "ms" -> "Malay"
            "hi" -> "Hindi"
            else -> code
        }
    }
}
