package tw.nekomimi.nekogram.transtale.source

import android.text.TextUtils
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import tw.nekomimi.nekogram.transtale.Translator
import xyz.nextalone.nagram.NaConfig
import java.util.Locale

/**
 * Common base translator for DeepL-family services.
 *
 * The DeepLX project, the official DeepL Pro API and the official DeepL Free
 * API all share the same target language list and the same notion of a
 * "formality" parameter. The wire format, however, differs slightly, so the
 * actual HTTP call is delegated to concrete subclasses through [performRequest].
 */
abstract class AbstractDeepLTranslator : Translator {

    companion object {
        const val FORMALITY_DEFAULT = 0
        const val FORMALITY_MORE = 1
        const val FORMALITY_LESS = 2

        /**
         * Target languages supported by all DeepL-family services.
         * Kept in sync with the official DeepL API documentation.
         */
        @JvmStatic
        protected val SUPPORTED_TARGET_LANGUAGES: List<String> = listOf(
            "bg", "cs", "da", "de", "el", "en-GB", "en-US", "en", "es", "et",
            "fi", "fr", "hu", "id", "it", "ja", "lt", "lv", "nl", "pl", "pt-BR",
            "pt-PT", "pt", "ro", "ru", "sk", "sl", "sv", "tr", "uk", "zh"
        )

        @JvmStatic
        fun convertLanguageCode(language: String, country: String): String {
            val languageLowerCase: String = language.lowercase(Locale.getDefault())
            return if (!TextUtils.isEmpty(country)) {
                val countryUpperCase: String = country.uppercase(Locale.getDefault())
                if (SUPPORTED_TARGET_LANGUAGES.contains("$languageLowerCase-$countryUpperCase")) {
                    "$languageLowerCase-$countryUpperCase"
                } else {
                    languageLowerCase
                }
            } else {
                languageLowerCase
            }
        }
    }

    /**
     * Tag used for log messages. Concrete subclasses override this to make
     * logs easier to filter.
     */
    protected open val logTag: String = "DeepL"

    protected fun getFormalityString(): String {
        return when (NaConfig.deepLFormality.Int()) {
            FORMALITY_MORE -> "more"
            FORMALITY_LESS -> "less"
            else -> "default"
        }
    }

    /**
     * Concrete subclasses provide the actual HTTP request implementation.
     *
     * Implementations should throw an [IllegalStateException] (via [error]) on
     * any non-recoverable failure and return the translated text on success.
     */
    @Throws(Exception::class)
    protected abstract suspend fun performRequest(from: String, to: String, query: String): String

    override suspend fun doTranslate(from: String, to: String, query: String): String {
        if (from == to) {
            return query
        }
        if (to !in SUPPORTED_TARGET_LANGUAGES) {
            throw UnsupportedOperationException(LocaleController.getString(R.string.TranslateApiUnsupported))
        }

        return try {
            performRequest(from, to, query)
        } catch (e: UnsupportedOperationException) {
            throw e
        } catch (e: Exception) {
            FileLog.e("[$logTag] translate failed: ${e.message}")
            throw e
        }
    }
}
