package de.ltsdesign.android.control.i18n

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 持有当前语言 tag,提供 setLanguage(),Settings 切换后调用。
 * Activity recreate 由 [AppHost] 发起。
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val _langTag = MutableStateFlow(LocaleHelper.currentTag(app))
    val langTag: StateFlow<String> = _langTag.asStateFlow()

    fun setLanguage(tag: String) {
        LocaleHelper.setTag(getApplication(), tag)
        _langTag.value = tag
    }
}