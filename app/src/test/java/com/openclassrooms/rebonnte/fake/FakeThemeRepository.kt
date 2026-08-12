package com.openclassrooms.rebonnte.fake

import com.openclassrooms.rebonnte.data.preferences.ThemeMode
import com.openclassrooms.rebonnte.data.preferences.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Reglage de theme en memoire.
 *
 * L'implementation reelle ecrit dans les preferences partagees, qui demandent
 * un Context Android : un test unitaire ne peut pas s'en servir. C'est la
 * raison d'etre de l'interface.
 */
class FakeThemeRepository(initialMode: ThemeMode = ThemeMode.SYSTEM) : ThemeRepository {

    private val mode = MutableStateFlow(initialMode)

    override val themeMode: Flow<ThemeMode> = mode

    override fun setThemeMode(mode: ThemeMode) {
        this.mode.value = mode
    }
}
