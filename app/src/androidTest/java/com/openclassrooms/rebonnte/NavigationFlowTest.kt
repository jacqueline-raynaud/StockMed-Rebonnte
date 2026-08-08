package com.openclassrooms.rebonnte

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.NoActivityResumedException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.openclassrooms.rebonnte.fake.FakeUserRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Parcours de navigation de bout en bout.
 *
 * Chaque test correspond a un defaut reellement rencontre, ou au garde-fou qui
 * l'empeche de revenir. Les tests de mise en forme sont volontairement absents :
 * ils cassent au moindre ajustement visuel sans rien proteger.
 *
 * L'Activity est lancee a la main, et non par une regle, pour pouvoir installer
 * une session avant son demarrage.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Inject
    lateinit var userRepository: FakeUserRepository

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    private fun launchApp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitForIdle()
    }

    private fun enterStock() {
        composeRule.onNodeWithText("OK, c'est bien moi").performClick()
        composeRule.waitForIdle()
    }

    /**
     * Espresso leve NoActivityResumedException quand le retour ferme
     * l'application. C'est deterministe, contrairement a une lecture de l'etat
     * du cycle de vie : finish() est asynchrone et l'Activity passe par STARTED
     * avant DESTROYED.
     */
    private fun assertBackClosesTheApp() {
        try {
            Espresso.pressBack()
            fail("Le retour aurait du fermer l'application")
        } catch (expected: NoActivityResumedException) {
            // Comportement attendu : plus rien a depiler.
        }
    }

    // --- Verrouillage de l'acces ---------------------------------------------

    @Test
    fun withoutSession_theSignInScreenIsShown() {
        userRepository.setSignedIn(null)

        launchApp()

        composeRule.onNodeWithText("Se connecter").assertIsDisplayed()
    }

    /**
     * Regression : au demarrage avec une session deja ouverte, l'effet de
     * navigation renavigait en boucle vers la destination de depart. L'ecran
     * s'affichait mais ne repondait plus a aucun clic.
     */
    @Test
    fun withSession_theWelcomeScreenIsShownAndItsButtonsRespond() {
        userRepository.setSignedIn(FakeUserRepository.TEST_USER)

        launchApp()

        composeRule.onNodeWithText("Bonjour ${FakeUserRepository.TEST_DISPLAY_NAME}")
            .assertIsDisplayed()

        // Le clic est l'assertion : sur une interface figee, il n'a aucun effet.
        enterStock()
        composeRule.onNodeWithText("Main Aisle").assertIsDisplayed()
    }

    @Test
    fun signingOutFromTheWelcomeScreen_returnsToSignIn() {
        userRepository.setSignedIn(FakeUserRepository.TEST_USER)
        launchApp()

        composeRule.onNodeWithText("Se deconnecter").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Se connecter").assertIsDisplayed()
    }

    @Test
    fun signingOutFromTheStock_returnsToSignIn() {
        userRepository.setSignedIn(FakeUserRepository.TEST_USER)
        launchApp()
        enterStock()

        composeRule.onNodeWithContentDescription("Se deconnecter").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Se connecter").assertIsDisplayed()
    }

    // --- Bouton retour --------------------------------------------------------

    /**
     * Regression : la pile etant videe a chaque bascule, il ne restait qu'une
     * entree. Le retour la depilait et le NavHost n'avait plus rien a afficher :
     * ecran noir, application vivante mais vide.
     */
    @Test
    fun backFromTheStockRoot_closesTheApp() {
        userRepository.setSignedIn(FakeUserRepository.TEST_USER)
        launchApp()
        enterStock()

        assertBackClosesTheApp()
    }

    @Test
    fun backFromTheSignInScreen_closesTheApp() {
        userRepository.setSignedIn(null)
        launchApp()

        assertBackClosesTheApp()
    }

    /**
     * Le pendant du test precedent : le gestionnaire de retour ne doit pas etre
     * trop large. Sur un ecran de detail, le retour reprend son role normal.
     */
    @Test
    fun backFromAMedicineDetail_returnsToTheList() {
        userRepository.setSignedIn(FakeUserRepository.TEST_USER)
        launchApp()
        enterStock()

        composeRule.onNodeWithText("Medicine").performClick()
        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Medicine 1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("History").assertIsDisplayed()

        // pressBack et non pressBackUnconditionally : s'il fermait
        // l'application, l'exception ferait echouer le test.
        Espresso.pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Medicine 1").assertIsDisplayed()
        assertEquals(Lifecycle.State.RESUMED, scenario.state)
    }
}
