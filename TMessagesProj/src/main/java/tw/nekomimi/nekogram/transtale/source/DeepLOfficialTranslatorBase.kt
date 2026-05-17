package tw.nekomimi.nekogram.transtale.source

import android.text.TextUtils
import io.ktor.http.ContentType
import org.json.JSONArray
import org.json.JSONObject
import xyz.nextalone.nagram.network.NetworkRequestBuilder

/**
 * Shared implementation for the two official DeepL endpoints:
 *
 *   - DeepL Pro:  https://api.deepl.com
 *   - DeepL Free: https://api-free.deepl.com
 *
 * Both expose the same `/v2/translate` JSON contract and only differ in the
 * base URL and the type of authentication key the user is expected to
 * provide, which makes them a natural fit for a single template-method base
 * class.
 *
 * Subclasses provide the [baseUrl] and the user-configured [apiKey].
 */
abstract class DeepLOfficialTranslatorBase : AbstractDeepLTranslator() {

    /** Base URL such as `https://api.deepl.com` or `https://api-free.deepl.com`. */
    protected abstract val baseUrl: String

    /** Auth key configured by the user. Empty / blank means "not configured". */
    protected abstract val apiKey: String?

    /** Human-readable name used inside the "missing API key" error. */
    protected abstract val displayName: String

    override suspend fun performRequest(from: String, to: String, query: String): String {
        val key = apiKey
        if (key.isNullOrBlank()) {
            error("Missing $displayName API Key")
        }

        val endpoint = "${baseUrl.trimEnd('/')}/v2/translate"

        val body = JSONObject().apply {
            put("text", JSONArray().apply { put(query) })
            if (from.isNotBlank() && from != "auto") {
                put("source_lang", from.uppercase())
            }
            put("target_lang", to.uppercase())
            put("formality", getFormalityString())
        }.toString()

        val response = NetworkRequestBuilder.post(endpoint) {
            contentType(ContentType.Application.Json)
            header("Authorization", "DeepL-Auth-Key $key")
            setBody(body)
        }.execute()

        if (response.statusCode != 200) {
            error("HTTP ${response.statusCode} : ${response.body}")
        }

        val json = JSONObject(response.body)
        if (json.has("message") && !json.has("translations")) {
            // DeepL returns { "message": "..." } on most error responses.
            error(json.getString("message"))
        }

        val translations = json.optJSONArray("translations")
            ?: error("Malformed DeepL response: ${response.body}")
        if (translations.length() == 0) {
            error("Empty DeepL response")
        }
        return translations.getJSONObject(0).getString("text")
    }
}
