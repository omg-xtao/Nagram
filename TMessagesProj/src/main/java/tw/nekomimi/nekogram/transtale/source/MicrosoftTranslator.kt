package tw.nekomimi.nekogram.transtale.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import tw.nekomimi.nekogram.transtale.Translator
import xyz.nextalone.nagram.network.NetworkRequestBuilder
import java.net.URLEncoder
import java.util.*

object MicrosoftTranslator : Translator {

    private val targetLanguages = Arrays.asList(
            "ar", "as", "bn", "bs", "bg", "yue", "ca", "zh", "zh-Hans", "zh-Hant",
            "hr", "cs", "da", "prs", "nl", "en", "et", "fj", "fil", "fi",
            "fr", "de", "el", "gu", "ht", "he", "hi", "mww", "hu", "is",
            "id", "ga", "it", "ja", "kn", "kk", "tlh", "ko", "ku", "kmr",
            "lv", "lt", "mg", "ms", "ml", "mt", "mi", "mr", "nb", "or", "ps",
            "fa", "pl", "pt", "pa", "otq", "ro", "ru", "sm", "sr", "sk", "sl",
            "es", "sw", "sv", "ty", "ta", "te", "th", "to", "tr", "uk", "ur",
            "vi", "cy", "yua")
    private var useCN = false

    override suspend fun doTranslate(from: String, to: String, query: String): String {
        if (to !in targetLanguages) {
            throw UnsupportedOperationException(LocaleController.getString(R.string.TranslateApiUnsupported))
        }

        return withContext(Dispatchers.IO) {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val param = "fromLang=auto-detect&text=$encodedQuery&to=$to"
            val response = request(param)
            val jsonObject = JSONArray(response).getJSONObject(0)
            if (!jsonObject.has("translations")) {
                throw Exception(response)
            }
            val array = jsonObject.getJSONArray("translations")
            array.getJSONObject(0).getString("text")
        }
    }

    private fun request(param: String): String {
        val baseUrl = "https://" + (if (useCN) "cn" else "www") + ".bing.com/ttranslatev3"

        val httpResponse = NetworkRequestBuilder.post(baseUrl) {
            header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1")
            setBody(param)
        }.execute()

        if (httpResponse.statusCode == 302 || httpResponse.statusCode == 301) {
            useCN = !useCN
            FileLog.e("Move to " + if (useCN) "cn" else "www")
            return request(param)
        }

        if (httpResponse.statusCode != 200) {
            FileLog.e("HTTP ${httpResponse.statusCode} : ${httpResponse.body}")
        }

        return httpResponse.body
    }

}