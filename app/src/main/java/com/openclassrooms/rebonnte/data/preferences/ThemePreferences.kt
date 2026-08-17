package com.openclassrooms.rebonnte.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manually setting the theme is an accessibility choice.
 * We can't force a dark theme on a user who would struggle to read light text on a dark background,
 * nor can you assume the user knows the specific steps required to change the theme on their device.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

interface ThemeRepository {
    val themeMode: Flow<ThemeMode>
    fun setThemeMode(mode: ThemeMode)
}

/**
 * The last choice is persisted in the shared preferences.
 * If users use the same phones, they will not have to select the theme again.
 */
@Singleton
class SharedPreferencesThemeRepository @Inject constructor(
    @ApplicationContext context: Context
) : ThemeRepository {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override val themeMode: Flow<ThemeMode> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_THEME_MODE) trySend(read())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    override fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    /**
     * Upon startup, if nothing has been configured, the theme used is the device's theme.
     */
    private fun read(): ThemeMode {
        val stored = preferences.getString(KEY_THEME_MODE, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.SYSTEM)
    }

    private companion object {
        const val PREFERENCES_NAME = "stockmed_settings"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
