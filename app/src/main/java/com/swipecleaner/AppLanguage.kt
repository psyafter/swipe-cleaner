package com.swipecleaner

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

data class AppLanguageOption(
    val tag: String,
    val nativeName: String,
)

object AppLanguage {
    val options = listOf(
        AppLanguageOption("", ""),
        AppLanguageOption("en", "English"),
        AppLanguageOption("es", "Español"),
        AppLanguageOption("fr", "Français"),
        AppLanguageOption("de", "Deutsch"),
        AppLanguageOption("it", "Italiano"),
        AppLanguageOption("pt-BR", "Português (Brasil)"),
        AppLanguageOption("pt-PT", "Português (Portugal)"),
        AppLanguageOption("ru", "Русский"),
        AppLanguageOption("uk", "Українська"),
        AppLanguageOption("pl", "Polski"),
        AppLanguageOption("cs", "Čeština"),
        AppLanguageOption("ro", "Română"),
        AppLanguageOption("hu", "Magyar"),
        AppLanguageOption("tr", "Türkçe"),
        AppLanguageOption("el", "Ελληνικά"),
        AppLanguageOption("he", "עברית"),
        AppLanguageOption("ar", "العربية"),
        AppLanguageOption("fa", "فارسی"),
        AppLanguageOption("hi", "हिन्दी"),
        AppLanguageOption("ur", "اردو"),
        AppLanguageOption("bn", "বাংলা"),
        AppLanguageOption("pa", "ਪੰਜਾਬੀ"),
        AppLanguageOption("ta", "தமிழ்"),
        AppLanguageOption("te", "తెలుగు"),
        AppLanguageOption("mr", "मराठी"),
        AppLanguageOption("id", "Bahasa Indonesia"),
        AppLanguageOption("ms", "Bahasa Melayu"),
        AppLanguageOption("vi", "Tiếng Việt"),
        AppLanguageOption("th", "ไทย"),
        AppLanguageOption("fil", "Filipino"),
        AppLanguageOption("ja", "日本語"),
        AppLanguageOption("ko", "한국어"),
        AppLanguageOption("zh-CN", "简体中文"),
        AppLanguageOption("zh-TW", "繁體中文"),
        AppLanguageOption("nl", "Nederlands"),
        AppLanguageOption("sv", "Svenska"),
        AppLanguageOption("no", "Norsk"),
        AppLanguageOption("da", "Dansk"),
        AppLanguageOption("fi", "Suomi"),
        AppLanguageOption("et", "Eesti"),
        AppLanguageOption("lv", "Latviešu"),
        AppLanguageOption("lt", "Lietuvių"),
        AppLanguageOption("sk", "Slovenčina"),
        AppLanguageOption("sl", "Slovenščina"),
        AppLanguageOption("hr", "Hrvatski"),
        AppLanguageOption("bg", "Български"),
        AppLanguageOption("sr", "Српски"),
        AppLanguageOption("sw", "Kiswahili"),
    )

    fun apply(context: Context, tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            localeManager?.applicationLocales = if (tag.isBlank()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }
}
