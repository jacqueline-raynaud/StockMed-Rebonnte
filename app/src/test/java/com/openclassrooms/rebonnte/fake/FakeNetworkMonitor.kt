package com.openclassrooms.rebonnte.fake

import com.openclassrooms.rebonnte.data.network.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Reseau pilote par le test.
 *
 * C'est la raison d'etre de l'interface [NetworkMonitor] : la connectivite
 * reelle passe par le systeme, et aucun test unitaire ne peut couper le wifi.
 */
class FakeNetworkMonitor(initiallyOnline: Boolean = true) : NetworkMonitor {

    private val online = MutableStateFlow(initiallyOnline)

    override val isOnline: Flow<Boolean> = online

    fun setOnline(value: Boolean) {
        online.value = value
    }
}
