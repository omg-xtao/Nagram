package tw.nekomimi.nekogram.transtale.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import tw.nekomimi.nekogram.transtale.Translator
import xyz.nextalone.nagram.network.NetworkRequestBuilder
import java.net.URLEncoder
import java.util.*

object YouDaoTranslator : Translator {

    private val targetLanguages = Arrays.asList("zh-CHS", "en", "es", "fr", "ja", "ru", "ko", "pt", "vi", "de", "id", "ar")

    override suspend fun doTranslate(from: String, to: String, query: String): String {
        if (to !in targetLanguages) {
            throw UnsupportedOperationException(LocaleController.getString(R.string.TranslateApiUnsupported))
        }

        return withContext(Dispatchers.IO) {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val param = "q=$encodedQuery&from=Auto&to=en$to"
            val response = request(param)
            val jsonObject = JSONObject(response)
            if (!jsonObject.has("translation") && jsonObject.has("errorCode")) {
                throw Exception(response)
            }
            val array = jsonObject.getJSONArray("translation")
            array.getString(0)
        }
    }

    private fun request(param: String): String {
        val baseUrl = "https://aidemo.youdao.com/trans"

        val httpResponse = NetworkRequestBuilder.post(baseUrl) {
            header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1")
            setBody(param)
        }.execute()

        return httpResponse.body
    }
}