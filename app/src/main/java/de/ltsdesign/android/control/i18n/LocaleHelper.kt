package de.ltsdesign.android.control.i18n

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

/**
 * Language switcher (EN / ZH / 跟随系统)
 *
 * - 存储在 SharedPreferences("app_prefs") 中,key = "app_lang_tag"
 * - 可能的值:""(跟随系统)、"en"、"zh"
 * - 调用 [applyLocale] 应用到当前 Context
 * - Activity 在 onCreate 调用 [wrap] 才能拿到本地化资源
 */
object LocaleHelper {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_LANG_TAG = "app_lang_tag"

    const val SYSTEM = ""        // 跟随系统
    const val EN = "en"
    const val ZH = "zh"

    fun supportedTags(): List<String> = listOf(SYSTEM, EN, ZH)

    fun currentTag(ctx: Context): String =
        prefs(ctx).getString(KEY_LANG_TAG, SYSTEM) ?: SYSTEM

    fun setTag(ctx: Context, tag: String) {
        prefs(ctx).edit().putString(KEY_LANG_TAG, tag).apply()
    }

    /**
     * 用 wrap(Context) 装饰 Context,使其使用当前语言环境;
     * 然后用这个新 Context 去 inflate / setContentView。
     */
    fun wrap(base: Context): Context {
        val tag = currentTag(base)
        val locale = if (tag == SYSTEM) {
            // 跟随系统:用 Configuration 里的默认
            Locale.getDefault()
        } else {
            Locale.forLanguageTag(tag)
        }
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    fun applyLocale(base: Context): Context {
        val ctx = wrap(base)
        return ctx
    }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}