package com.openclassrooms.rebonnte.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * L'application a-t-elle un reseau utilisable.
 *
 * Une interface, pour que les tests puissent decider de la reponse : la
 * connectivite reelle ne se simule pas dans un test unitaire.
 */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}

/**
 * Hors ligne, Firestore ne signale aucune erreur : il sert son cache local et
 * met les ecritures en attente. C'est le bon comportement, mais il rend la
 * panne **invisible** — un stock vide parce que le cache est vide ressemble a
 * un stock vide tout court. D'ou cette surveillance : elle ne corrige rien,
 * elle rend l'etat visible.
 *
 * NET_CAPABILITY_VALIDATED et pas seulement INTERNET : un wifi d'hotel auquel
 * on est connecte sans pouvoir sortir doit compter comme hors ligne.
 */
@Singleton
class ConnectivityNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkMonitor {

    override val isOnline: Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        if (manager == null) {
            // Sans service de connectivite, mieux vaut laisser l'application
            // travailler que la declarer hors ligne a tort.
            trySend(true)
            awaitClose { }
            return@callbackFlow
        }

        fun currentlyOnline(): Boolean {
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
            return capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(currentlyOnline())
            }

            override fun onLost(network: Network) {
                trySend(currentlyOnline())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                trySend(currentlyOnline())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        manager.registerNetworkCallback(request, callback)

        // Etat initial : les rappels ne se declenchent qu'au prochain
        // changement, et il peut ne jamais y en avoir.
        trySend(currentlyOnline())

        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged().conflate()
}
