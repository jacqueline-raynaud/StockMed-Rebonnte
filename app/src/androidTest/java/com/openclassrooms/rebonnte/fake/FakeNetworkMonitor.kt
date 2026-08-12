package com.openclassrooms.rebonnte.fake

import com.openclassrooms.rebonnte.data.network.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reseau toujours disponible pendant les tests instrumentes.
 *
 * Sans lui, les tests dependraient de la connectivite de l'emulateur : hors
 * ligne, l'application affiche son ecran de blocage et plus aucun parcours
 * n'est jouable. L'emulateur de l'integration continue n'a pas toujours un
 * reseau considere comme « valide » par Android.
 */
@Singleton
class FakeNetworkMonitor @Inject constructor() : NetworkMonitor {

    private val online = MutableStateFlow(true)

    override val isOnline: Flow<Boolean> = online

    fun setOnline(value: Boolean) {
        online.value = value
    }
}
