package tw.nekomimi.nekogram.transtale.source

import android.text.TextUtils
import cn.hutool.core.util.StrUtil
import cn.hutool.http.HttpUtil
import org.json.JSONObject
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import tw.nekomimi.nekogram.transtale.Translator
import xyz.nextalone.nagram.NaConfig
import java.util.Locale

object DeepLXTranslator : Translator {

    private val targetLanguages = listOf(
        "bg", "cs", "da", "de", "el", "en-GB", "en-US", "en", "es", "et",
        "fi", "fr", "hu", "id", "it", "ja", "lt", "lv", "nl", "pl", "pt-BR",
        "pt-PT", "pt", "ro", "ru", "sk", "sl", "sv", "tr", "uk", "zh"
    )

    @JvmStatic
    fun convertLanguageCode(language: String, country: String): String {
        val languageLowerCase: String = language.lowercase(Locale.getDefault())
        val code: String = if (!TextUtils.isEmpty(country)) {
            val countryUpperCase: String = country.uppercase(Locale.getDefault())
            if (targetLanguages.contains("$languageLowerCase-$countryUpperCase")) {
                "$languageLowerCase-$countryUpperCase"
            } else {
                languageLowerCase
            }
        } else {
            languageLowerCase
        }
        return code
    }

    override suspend fun doTranslate(from: String, to: String, query: String): String {
        if (from == to) {
            return query
        }
        if (to !in targetLanguages) {
            throw UnsupportedOperationException(LocaleController.getString(R.string.TranslateApiUnsupported))
        }
        val translateApi = NaConfig.deepLxCustomApi.String()
        if (StrUtil.isBlank(translateApi)) error("Missing DeepLx Translate Api")

        val response = HttpUtil.createPost(translateApi)
            .header("Content-Type", "application/json; charset=UTF-8")
            .body(JSONObject().apply {
                put("text", query)
                put("source_lang", from)
                put("target_lang", to)
            }.toString())
            .execute()

        if (response.status != 200) {
            error("HTTP ${response.status} : ${response.body()}")
        }

        val jsonObject = JSONObject(response.body())
        if (jsonObject.has("error")) {
            error(jsonObject.getString("message"))
        }
        return jsonObject.getString("data")
    }
}
