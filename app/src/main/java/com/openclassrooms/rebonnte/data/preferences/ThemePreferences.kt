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
 * Les trois etats du reglage de theme.
 *
 * [SYSTEM] est le defaut : une personne ayant des besoins visuels particuliers
 * a le plus souvent deja regle son telephone en consequence, et ce reglage est
 * lui-meme un choix d'accessibilite. Demarrer sur « Sombre » l'ecraserait.
 *
 * Le reglage manuel existe parce qu'on ne peut pas imposer un mode sans
 * connaitre les besoins de l'operateur : le sombre n'est pas universellement
 * plus lisible — les personnes astigmates lisent souvent moins bien du clair
 * sur fond sombre, a cause du halo autour des caracteres.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

interface ThemeRepository {
    val themeMode: Flow<ThemeMode>
    fun setThemeMode(mode: ThemeMode)
}

/**
 * Persiste le choix dans les preferences partagees.
 *
 * Sans persistance, le reglage serait perdu a chaque lancement — un utilisateur
 * qui a besoin du mode clair devrait le redemander tous les matins.
 *
 * `callbackFlow` sur l'ecouteur de changement plutot qu'une simple lecture :
 * le theme doit s'appliquer au moment du choix, sans redemarrage.
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
     * Une valeur inconnue — reglage ecrit par une version anterieure, ou fichier
     * abime — retombe sur le defaut plutot que de faire planter le demarrage.
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
