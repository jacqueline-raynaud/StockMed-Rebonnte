package com.openclassrooms.rebonnte.ui

import com.openclassrooms.rebonnte.data.repository.impl.InMemoryAisleRepository
import com.openclassrooms.rebonnte.fake.FakeNetworkMonitor
import com.openclassrooms.rebonnte.fake.FakeUserRepository
import com.openclassrooms.rebonnte.ui.model.toUi
import com.openclassrooms.rebonnte.util.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `an open session is visible immediately without waiting for the flow`() = runTest {
        val viewModel = MainViewModel(FakeUserRepository(), InMemoryAisleRepository(), FakeNetworkMonitor())

        // Valeur initiale lue de maniere synchrone : c'est ce qui evite le
        // passage eclair par l'ecran de connexion au demarrage.
        //
        // La comparaison porte sur le modele d'affichage : le ViewModel expose
        // un UserUi, qui ne porte pas l'UID Firebase.
        assertEquals(FakeUserRepository.SIGNED_IN_USER.toUi(), viewModel.uiState.value.user)
    }

    @Test
    fun `no session exposes a null user`() = runTest {
        val viewModel = MainViewModel(FakeUserRepository(initialUser = null), InMemoryAisleRepository(), FakeNetworkMonitor())

        assertNull(viewModel.uiState.value.user)
    }

    /**
     * L'ecran d'accueil doit etre revalide a chaque demarrage : sur un
     * telephone partage, c'est le garde-fou demande par le Product Owner.
     */
    @Test
    fun `the welcome screen starts unacknowledged`() = runTest {
        val viewModel = MainViewModel(FakeUserRepository(), InMemoryAisleRepository(), FakeNetworkMonitor())

        assertFalse(viewModel.uiState.value.welcomeAcknowledged)
    }

    @Test
    fun `acknowledging the welcome screen opens the stock`() = runTest {
        val viewModel = MainViewModel(FakeUserRepository(), InMemoryAisleRepository(), FakeNetworkMonitor())

        viewModel.acknowledgeWelcome()

        assertTrue(viewModel.uiState.value.welcomeAcknowledged)
    }

    /**
     * Sans cette remise a zero, l'operateur suivant qui se connecte sauterait
     * l'ecran d'accueil et n'aurait jamais l'avertissement.
     */
    @Test
    fun `signing out resets the welcome acknowledgement`() = runTest {
        val userRepository = FakeUserRepository()
        val viewModel = MainViewModel(userRepository, InMemoryAisleRepository(), FakeNetworkMonitor())
        viewModel.acknowledgeWelcome()

        viewModel.signOut()

        assertFalse(viewModel.uiState.value.welcomeAcknowledged)
        assertEquals(1, userRepository.signOutCount)
        assertNull(viewModel.uiState.value.user)
    }

    // --- Etat applicatif (T-24) ----------------------------------------------

    /**
     * Hors ligne, Firestore ne remonte aucune erreur : il sert son cache et met
     * les ecritures en attente. Sans ce signal, un stock vide faute de cache se
     * lirait comme un stock reellement vide.
     */
    @Test
    fun `losing the network switches the application to offline`() = runTest {
        val network = FakeNetworkMonitor(initiallyOnline = true)
        val viewModel = MainViewModel(FakeUserRepository(), InMemoryAisleRepository(), network)
        backgroundScope.launch { viewModel.uiState.collect { } }
        assertEquals(AppState.READY, viewModel.uiState.value.appState)

        network.setOnline(false)

        assertEquals(AppState.OFFLINE, viewModel.uiState.value.appState)
    }

    /** Le bandeau doit disparaitre tout seul : il n'y a rien a fermer. */
    @Test
    fun `recovering the network switches back to ready`() = runTest {
        val network = FakeNetworkMonitor(initiallyOnline = false)
        val viewModel = MainViewModel(FakeUserRepository(), InMemoryAisleRepository(), network)
        backgroundScope.launch { viewModel.uiState.collect { } }
        assertEquals(AppState.OFFLINE, viewModel.uiState.value.appState)

        network.setOnline(true)

        assertEquals(AppState.READY, viewModel.uiState.value.appState)
    }
}