package tw.nekomimi.nekogram.transtale.source

import cn.hutool.core.util.StrUtil
import io.ktor.http.ContentType
import org.json.JSONArray
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import tw.nekomimi.nekogram.NekoConfig
import tw.nekomimi.nekogram.transtale.Translator
import xyz.nextalone.nagram.network.NetworkRequestBuilder

object GoogleCloudTranslator : Translator {

    override suspend fun doTranslate(from: String, to: String, query: String): String {

        if (to !in targetLanguages) {

            throw UnsupportedOperationException(LocaleController.getString(R.string.TranslateApiUnsupported))

        }

        if (StrUtil.isBlank(NekoConfig.googleCloudTranslateKey.String())) error("Missing Cloud Translate Key")

        val srclang = from.ifEmpty { "auto" }

        val jsonBody = JSONArray().apply {
            put(JSONArray().apply {
                put(JSONArray().apply { put(query) })
                put(srclang)
                put(to)
            })
            put("wt_lib")
        }

        val response = NetworkRequestBuilder.post("https://translate-pa.googleapis.com/v1/translateHtml") {
            header("X-Goog-Api-Key", NekoConfig.googleCloudTranslateKey.String())
            contentType(ContentType.parse("application/json+protobuf"))
            setBody(jsonBody.toString())
        }.execute()

        if (response.statusCode != 200) {

            error("HTTP ${response.statusCode} : ${response.body}")

        }

        val respArr = JSONArray(response.body)

        if (respArr.length() == 0) error("Empty translation result")

        val innerArr = respArr.getJSONArray(0)

        if (innerArr.length() == 0) error("Empty translation result")

        return innerArr.getString(0)

    }

    private val targetLanguages = listOf(
            "sq", "ar", "am", "az", "ga", "et", "eu", "be", "bg", "is", "pl", "bs", "fa",
            "af", "da", "de", "ru", "fr", "tl", "fi", "fy", "km", "ka", "gu", "kk", "ht",
            "ko", "ha", "nl", "ky", "gl", "ca", "cs", "kn", "co", "hr", "ku", "la", "lv",
            "lo", "lt", "lb", "ro", "mg", "mt", "mr", "ml", "ms", "mk", "mi", "mn", "bn",
            "my", "hmn", "xh", "zu", "ne", "no", "pa", "pt", "ps", "ny", "ja", "sv", "sm",
            "sr", "st", "si", "eo", "sk", "sl", "sw", "gd", "ceb", "so", "tg", "te", "ta",
            "th", "tr", "cy", "ur", "uk", "uz", "es", "iw", "el", "haw", "sd", "hu", "sn",
            "hy", "ig", "it", "yi", "hi", "su", "id", "jw", "en", "yo", "vi", "zh-TW", "zh-CN", "zh")

}