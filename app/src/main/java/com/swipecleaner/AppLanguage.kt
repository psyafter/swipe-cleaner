package com.swipecleaner

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

data class AppLanguageOption(
    val tag: String,
)

object AppLanguage {
    val options = listOf(
        AppLanguageOption(""),
        AppLanguageOption("en"),
        AppLanguageOption("es"),
        AppLanguageOption("fr"),
        AppLanguageOption("de"),
        AppLanguageOption("it"),
        AppLanguageOption("pt-BR"),
        AppLanguageOption("pt-PT"),
        AppLanguageOption("ru"),
        AppLanguageOption("uk"),
        AppLanguageOption("pl"),
        AppLanguageOption("cs"),
        AppLanguageOption("ro"),
        AppLanguageOption("hu"),
        AppLanguageOption("tr"),
        AppLanguageOption("el"),
        AppLanguageOption("he"),
        AppLanguageOption("ar"),
        AppLanguageOption("fa"),
        AppLanguageOption("hi"),
        AppLanguageOption("ur"),
        AppLanguageOption("bn"),
        AppLanguageOption("pa"),
        AppLanguageOption("ta"),
        AppLanguageOption("te"),
        AppLanguageOption("mr"),
        AppLanguageOption("id"),
        AppLanguageOption("ms"),
        AppLanguageOption("vi"),
        AppLanguageOption("th"),
        AppLanguageOption("fil"),
        AppLanguageOption("ja"),
        AppLanguageOption("ko"),
        AppLanguageOption("zh-CN"),
        AppLanguageOption("zh-TW"),
        AppLanguageOption("nl"),
        AppLanguageOption("sv"),
        AppLanguageOption("no"),
        AppLanguageOption("da"),
        AppLanguageOption("fi"),
        AppLanguageOption("et"),
        AppLanguageOption("lv"),
        AppLanguageOption("lt"),
        AppLanguageOption("sk"),
        AppLanguageOption("sl"),
        AppLanguageOption("hr"),
        AppLanguageOption("bg"),
        AppLanguageOption("sr"),
        AppLanguageOption("sw"),
    )

    fun nativeName(tag: String): String {
        if (tag.isBlank()) return ""
        val locale = Locale.forLanguageTag(tag)
        return locale.getDisplayName(locale)
    }

    fun apply(context: Context, tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            localeManager?.applicationLocales = if (tag.isBlank()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }
}
