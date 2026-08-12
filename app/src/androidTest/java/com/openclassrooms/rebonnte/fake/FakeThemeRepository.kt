package com.openclassrooms.rebonnte.fake

import com.openclassrooms.rebonnte.data.preferences.ThemeMode
import com.openclassrooms.rebonnte.data.preferences.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Theme en memoire pendant les tests instrumentes.
 *
 * L'implementation reelle ecrit dans les preferences partagees, qui survivent
 * d'une execution a l'autre : un mode sombre choisi a la main lors d'un essai
 * precedent s'appliquerait aux tests suivants.
 */
@Singleton
class FakeThemeRepository @Inject constructor() : ThemeRepository {

    private val mode = MutableStateFlow(ThemeMode.SYSTEM)

    override val themeMode: Flow<ThemeMode> = mode

    override fun setThemeMode(mode: ThemeMode) {
        this.mode.value = mode
    }
}
