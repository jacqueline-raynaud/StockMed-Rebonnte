package com.openclassrooms.rebonnte.ui

import com.openclassrooms.rebonnte.data.repository.InMemoryAisleRepository
import com.openclassrooms.rebonnte.fake.FakeUserRepository
import com.openclassrooms.rebonnte.util.MainDispatcherRule
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
        val viewModel = MainViewModel(FakeUserRepository(), InMemoryAisleRepository())

        // Valeur initiale lue de maniere synchrone : c'est ce qui evite le
        // passage eclair par l'ecran de connexion au demarrage.
        assertEquals(FakeUserRepository.SIGNED_IN_USER, viewModel.currentUser.value)
    }

    @Test
    fun `no session exposes a null user`() = runTest {
        val viewModel = MainViewModel(FakeUserRepository(initialUser = null), InMemoryAisleRepository())

        assertNull(viewModel.currentUser.value)
    }

    /**
     * L'ecran d'accueil doit etre revalide a chaque demarrage : sur un
     * telephone partage, c'est le garde-fou demande par le Product Owner.
     */
    @Test
    fun `the welcome screen starts unacknowledged`() = runTest {
        val viewModel = MainViewModel(FakeUserRepository(), InMemoryAisleRepository())

        assertFalse(viewModel.welcomeAcknowledged.value)
    }

    @Test
    fun `acknowledging the welcome screen opens the stock`() = runTest {
        val viewModel = MainViewModel(FakeUserRepository(), InMemoryAisleRepository())

        viewModel.acknowledgeWelcome()

        assertTrue(viewModel.welcomeAcknowledged.value)
    }

    /**
     * Sans cette remise a zero, l'operateur suivant qui se connecte sauterait
     * l'ecran d'accueil et n'aurait jamais l'avertissement.
     */
    @Test
    fun `signing out resets the welcome acknowledgement`() = runTest {
        val userRepository = FakeUserRepository()
        val viewModel = MainViewModel(userRepository, InMemoryAisleRepository())
        viewModel.acknowledgeWelcome()

        viewModel.signOut()

        assertFalse(viewModel.welcomeAcknowledged.value)
        assertEquals(1, userRepository.signOutCount)
        assertNull(viewModel.currentUser.value)
    }
}
