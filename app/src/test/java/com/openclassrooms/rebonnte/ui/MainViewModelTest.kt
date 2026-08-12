package com.openclassrooms.rebonnte.ui

import com.openclassrooms.rebonnte.data.repository.impl.InMemoryAisleRepository
import com.openclassrooms.rebonnte.data.preferences.ThemeMode
import com.openclassrooms.rebonnte.fake.FakeNetworkMonitor
import com.openclassrooms.rebonnte.fake.FakeThemeRepository
import com.openclassrooms.rebonnte.fake.FakeUserRepository
import com.openclassrooms.rebonnte.ui.model.toUi
import com.openclassrooms.rebonnte.util.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import com.openclassrooms.rebonnte.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `an open session is visible immediately without waiting for the flow`() = runTest {
        val viewModel = MainViewModel(FakeUserRepository(), InMemoryAisleRepository(), FakeThemeRepository(), FakeNetworkMonitor())

        // Valeur initiale lue de maniere synchrone : c'est ce qui evite le
        // passage eclair par l'ecran de connexion au demarrage.
        //
        // La comparaison porte sur le modele d'affichage : le ViewModel expose
        // un UserUi, qui ne porte pas l'UID Firebase.
        assertEquals(FakeUserRepository.SIGNED_IN_USER.toUi(), viewModel.uiState.value.user)
    }

    @Test
    fun `no session exposes a null user`() = runTest {
        val viewModel = MainViewModel(FakeUserRepository(initialUser = null), InMemoryAisleRepository(), FakeThemeRepository(), FakeNetworkMonitor())

        assertNull(viewModel.uiState.value.user)
    }

    /**
     * L'ecran d'accueil doit etre revalide a chaque demarrage : sur un
     * telephone partage, c'est le garde-fou demande par le Product Owner.
     */
    @Test
    fun `the welcome screen starts unacknowledged`() = runTest {
        val viewModel = MainViewModel(FakeUserRepository(), InMemoryAisleRepository(), FakeThemeRepository(), FakeNetworkMonitor())

        assertFalse(viewModel.uiState.value.welcomeAcknowledged)
    }

    @Test
    fun `acknowledging the welcome screen opens the stock`() = runTest {
        val viewModel = MainViewModel(FakeUserRepository(), InMemoryAisleRepository(), FakeThemeRepository(), FakeNetworkMonitor())

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
        val viewModel = MainViewModel(userRepository, InMemoryAisleRepository(), FakeThemeRepository(), FakeNetworkMonitor())
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
        val viewModel = MainViewModel(FakeUserRepository(), InMemoryAisleRepository(), FakeThemeRepository(), network)
        backgroundScope.launch { viewModel.uiState.collect { } }
        assertEquals(AppState.READY, viewModel.uiState.value.appState)

        network.setOnline(false)

        assertEquals(AppState.OFFLINE, viewModel.uiState.value.appState)
    }

    /** Le bandeau doit disparaitre tout seul : il n'y a rien a fermer. */
    @Test
    fun `recovering the network switches back to ready`() = runTest {
        val network = FakeNetworkMonitor(initiallyOnline = false)
        val viewModel = MainViewModel(FakeUserRepository(), InMemoryAisleRepository(), FakeThemeRepository(), network)
        backgroundScope.launch { viewModel.uiState.collect { } }
        assertEquals(AppState.OFFLINE, viewModel.uiState.value.appState)

        network.setOnline(true)

        assertEquals(AppState.READY, viewModel.uiState.value.appState)
    }

    // --- Theme (T-32) ---------------------------------------------------------

    /** Le defaut retenu : suivre le telephone, qui est deja un reglage d'accessibilite. */
    @Test
    fun `the theme follows the system by default`() = runTest {
        val viewModel = MainViewModel(
            FakeUserRepository(),
            InMemoryAisleRepository(),
            FakeThemeRepository(),
            FakeNetworkMonitor()
        )
        backgroundScope.launch { viewModel.uiState.collect { } }

        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
    }

    /**
     * Le choix doit survivre au redemarrage : un operateur qui a besoin du mode
     * clair ne doit pas le redemander chaque matin. Ici le depot tient lieu de
     * stockage persistant.
     */
    @Test
    fun `choosing a mode is kept and exposed`() = runTest {
        val themeRepository = FakeThemeRepository()
        val viewModel = MainViewModel(
            FakeUserRepository(),
            InMemoryAisleRepository(),
            themeRepository,
            FakeNetworkMonitor()
        )
        backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
    }

    /** Un reglage deja enregistre est relu au demarrage. */
    @Test
    fun `a stored mode is restored`() = runTest {
        val viewModel = MainViewModel(
            FakeUserRepository(),
            InMemoryAisleRepository(),
            FakeThemeRepository(initialMode = ThemeMode.DARK),
            FakeNetworkMonitor()
        )
        backgroundScope.launch { viewModel.uiState.collect { } }

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    // --- Suppression de compte -----------------------------------------------

    @Test
    fun `deleting the account closes the session`() = runTest {
        val userRepository = FakeUserRepository()
        val viewModel = viewModel(userRepository)
        backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.deleteAccount("motdepasse")

        assertEquals(1, userRepository.deleteAccountCount)
        assertEquals("motdepasse", userRepository.lastDeletePassword)
        assertNull(viewModel.uiState.value.user)
        assertNull(viewModel.uiState.value.deleteAccountError)
    }

    /**
     * Le motif d'echec le plus frequent : Firebase exige une re-authentification
     * et la refuse si le mot de passe est faux. Le compte doit rester en place.
     */
    @Test
    fun `a wrong password reports an error and keeps the account`() = runTest {
        val userRepository = FakeUserRepository()
        userRepository.deleteResult =
            Result.failure(Exception("The supplied auth credential is incorrect"))
        val viewModel = viewModel(userRepository)
        backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.deleteAccount("mauvais")

        assertEquals(
            R.string.auth_error_bad_credentials,
            viewModel.uiState.value.deleteAccountError
        )
        assertNotNull(viewModel.uiState.value.user)
    }

    @Test
    fun `a network failure during deletion is reported`() = runTest {
        val userRepository = FakeUserRepository()
        userRepository.deleteResult = Result.failure(Exception("A network error has occurred"))
        val viewModel = viewModel(userRepository)
        backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.deleteAccount("motdepasse")

        assertEquals(R.string.error_network, viewModel.uiState.value.deleteAccountError)
    }

    @Test
    fun `acknowledging the deletion error clears it`() = runTest {
        val userRepository = FakeUserRepository()
        userRepository.deleteResult = Result.failure(Exception("network"))
        val viewModel = viewModel(userRepository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        viewModel.deleteAccount("motdepasse")

        viewModel.deleteAccountErrorShown()

        assertNull(viewModel.uiState.value.deleteAccountError)
    }

    private fun viewModel(userRepository: FakeUserRepository) = MainViewModel(
        userRepository,
        InMemoryAisleRepository(),
        FakeThemeRepository(),
        FakeNetworkMonitor()
    )
}