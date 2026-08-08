package com.openclassrooms.rebonnte.ui.auth

import com.openclassrooms.rebonnte.fake.FakeUserRepository
import com.openclassrooms.rebonnte.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var userRepository: FakeUserRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        userRepository = FakeUserRepository(initialUser = null)
        viewModel = AuthViewModel(userRepository)
    }

    // --- Validation ----------------------------------------------------------

    /**
     * La validation precede l'appel reseau : solliciter Firebase pour un champ
     * vide, c'est un aller-retour inutile et une attente pour l'utilisateur.
     */
    @Test
    fun `submit with an empty form does not reach the repository`() = runTest {
        viewModel.submit()

        assertEquals(0, userRepository.signInCount)
        assertNotNull(viewModel.uiState.value.emailError)
        assertNotNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun `an email without an at sign is rejected`() = runTest {
        viewModel.onEmailChange("operateur.rebonnte.fr")
        viewModel.onPasswordChange("motdepasse")

        viewModel.submit()

        assertNotNull(viewModel.uiState.value.emailError)
        assertEquals(0, userRepository.signInCount)
    }

    /** Firebase Authentication refuse en dessous de six caracteres. */
    @Test
    fun `a password shorter than six characters is rejected`() = runTest {
        viewModel.onEmailChange("operateur@rebonnte.fr")
        viewModel.onPasswordChange("12345")

        viewModel.submit()

        assertNotNull(viewModel.uiState.value.passwordError)
        assertEquals(0, userRepository.signInCount)
    }

    @Test
    fun `signing up without a name is rejected`() = runTest {
        viewModel.toggleMode()
        viewModel.onEmailChange("operateur@rebonnte.fr")
        viewModel.onPasswordChange("motdepasse")

        viewModel.submit()

        assertNotNull(viewModel.uiState.value.displayNameError)
        assertEquals(0, userRepository.signUpCount)
    }

    @Test
    fun `editing a field clears its error`() = runTest {
        viewModel.submit()
        assertNotNull(viewModel.uiState.value.emailError)

        viewModel.onEmailChange("operateur@rebonnte.fr")

        assertNull(viewModel.uiState.value.emailError)
    }

    // --- Soumission ----------------------------------------------------------

    @Test
    fun `a valid form signs in and leaves no error`() = runTest {
        viewModel.onEmailChange("operateur@rebonnte.fr")
        viewModel.onPasswordChange("motdepasse")

        viewModel.submit()

        assertEquals(1, userRepository.signInCount)
        assertNull(viewModel.uiState.value.formError)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `sign up mode calls signUp and not signIn`() = runTest {
        viewModel.toggleMode()
        viewModel.onDisplayNameChange("Operateur")
        viewModel.onEmailChange("operateur@rebonnte.fr")
        viewModel.onPasswordChange("motdepasse")

        viewModel.submit()

        assertEquals(1, userRepository.signUpCount)
        assertEquals(0, userRepository.signInCount)
    }

    /**
     * Les libelles bruts de Firebase sont en anglais et parlent de « credential
     * is incorrect » : on les traduit avant affichage.
     */
    @Test
    fun `a wrong password produces a readable message`() = runTest {
        userRepository.authResult =
            Result.failure(Exception("The supplied auth credential is incorrect"))
        viewModel.onEmailChange("operateur@rebonnte.fr")
        viewModel.onPasswordChange("motdepasse")

        viewModel.submit()

        assertEquals("E-mail ou mot de passe incorrect", viewModel.uiState.value.formError)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `a network failure produces a readable message`() = runTest {
        userRepository.authResult =
            Result.failure(Exception("A network error has occurred"))
        viewModel.onEmailChange("operateur@rebonnte.fr")
        viewModel.onPasswordChange("motdepasse")

        viewModel.submit()

        assertEquals(
            "Connexion impossible : verifiez votre reseau",
            viewModel.uiState.value.formError
        )
    }

    @Test
    fun `an already used address produces a readable message`() = runTest {
        userRepository.authResult =
            Result.failure(Exception("The email address is already in use by another account"))
        viewModel.toggleMode()
        viewModel.onDisplayNameChange("Operateur")
        viewModel.onEmailChange("operateur@rebonnte.fr")
        viewModel.onPasswordChange("motdepasse")

        viewModel.submit()

        assertEquals(
            "Un compte existe deja avec cette adresse",
            viewModel.uiState.value.formError
        )
    }

    // --- Bascule connexion / creation ---------------------------------------

    @Test
    fun `toggling the mode keeps the email and clears the password`() = runTest {
        viewModel.onEmailChange("operateur@rebonnte.fr")
        viewModel.onPasswordChange("motdepasse")

        viewModel.toggleMode()

        assertEquals(AuthMode.SIGN_UP, viewModel.uiState.value.mode)
        assertEquals("operateur@rebonnte.fr", viewModel.uiState.value.email)
        assertEquals("", viewModel.uiState.value.password)
    }
}
