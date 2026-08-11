package com.openclassrooms.rebonnte

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.openclassrooms.rebonnte.data.model.StorageLocations
import com.openclassrooms.rebonnte.fake.FakeUserRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertEquals
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
 *
 * Les libelles sont lus dans les ressources plutot qu'ecrits en dur. L'emulateur
 * de l'integration continue tourne en en-US : des chaines francaises figees ici
 * passeraient tant que les ressources par defaut sont francaises, puis
 * echoueraient toutes le jour ou elles deviendront anglaises — et elles
 * echoueraient en CI, pas sur le poste de developpement.
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

    /**
     * Le premier emplacement amorce, lu a la source plutot que recopie : si son
     * libelle change, le test suit au lieu de casser.
     */
    private val standardStorage = StorageLocations.DEFAULTS.first().name

    /**
     * Resout un libelle dans la langue de l'appareil de test, exactement comme
     * l'application le fait a l'affichage.
     */
    private fun string(@StringRes id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

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
        composeRule.onNodeWithText(string(R.string.welcome_continue)).performClick()
        composeRule.waitForIdle()
    }

    /**
     * Passe par le formulaire : le bouton « + » ouvre desormais un ecran de
     * creation au lieu d'ajouter un medicament au nom et au stock aleatoires.
     */
    private fun createMedicine(name: String) {
        composeRule.onNodeWithText(string(R.string.nav_medicines)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.action_add)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.form_medicine_name)).performTextInput(name)
        composeRule.onNodeWithText(string(R.string.form_storage_location)).performClick()
        // Nom d'emplacement en dur : c'est une donnee amorcee en base, pas un
        // libelle d'interface, et elle reste en francais quelle que soit la
        // langue de l'application.
        composeRule.onNodeWithText(standardStorage).performClick()
        composeRule.onNodeWithText(string(R.string.form_submit)).performClick()
        composeRule.waitForIdle()
    }

    /**
     * Retour envoye directement au dispatcher de l'Activity, et non via
     * Espresso.pressBack().
     *
     * Espresso exige que la fenetre ait le focus avant d'injecter un evenement
     * (RootViewPicker). Sur un emulateur de CI, ce focus arrive parfois apres
     * son delai d'attente : les tests passaient en local et sur un run,
     * echouaient sur le suivant. Le dispatcher emprunte exactement le meme
     * chemin que le bouton systeme, BackHandler compris, sans dependre du focus.
     */
    private fun performBack() {
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
    }

    /** finish() est asynchrone : on attend l'etat plutot que de le lire aussitot. */
    private fun assertBackClosesTheApp() {
        performBack()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            scenario.state == Lifecycle.State.DESTROYED
        }
    }

    // --- Verrouillage de l'acces ---------------------------------------------

    @Test
    fun withoutSession_theSignInScreenIsShown() {
        userRepository.setSignedIn(null)

        launchApp()

        composeRule.onNodeWithText(string(R.string.auth_action_sign_in)).assertIsDisplayed()
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

        composeRule
            .onNodeWithText(
                string(R.string.welcome_greeting, FakeUserRepository.TEST_DISPLAY_NAME)
            )
            .assertIsDisplayed()

        // Le clic est l'assertion : sur une interface figee, il n'a aucun effet.
        enterStock()
        composeRule.onNodeWithText(standardStorage).assertIsDisplayed()
    }

    @Test
    fun signingOutFromTheWelcomeScreen_returnsToSignIn() {
        userRepository.setSignedIn(FakeUserRepository.TEST_USER)
        launchApp()

        composeRule.onNodeWithText(string(R.string.action_sign_out)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.auth_action_sign_in)).assertIsDisplayed()
    }

    @Test
    fun signingOutFromTheStock_returnsToSignIn() {
        userRepository.setSignedIn(FakeUserRepository.TEST_USER)
        launchApp()
        enterStock()

        composeRule.onNodeWithContentDescription(string(R.string.action_sign_out)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.auth_action_sign_in)).assertIsDisplayed()
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

        createMedicine("Doliprane")

        composeRule.onNodeWithText("Doliprane").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(string(R.string.history_title)).assertIsDisplayed()

        performBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Doliprane").assertIsDisplayed()
        // Le pendant de assertBackClosesTheApp : le gestionnaire de retour ne
        // doit pas etre trop large, l'application reste ouverte.
        assertEquals(Lifecycle.State.RESUMED, scenario.state)
    }

    // --- Creation et suppression ---------------------------------------------

    /** T-15 : le medicament cree porte le nom et l'emplacement choisis. */
    @Test
    fun creatingAMedicine_addsItToTheList() {
        userRepository.setSignedIn(FakeUserRepository.TEST_USER)
        launchApp()
        enterStock()

        createMedicine("Doliprane")

        composeRule.onNodeWithText("Doliprane").assertIsDisplayed()
    }

    /**
     * T-16 : la suppression etait impossible depuis l'interface. Elle passe
     * par une confirmation, puis renvoie a la liste.
     */
    @Test
    fun deletingAMedicine_removesItFromTheList() {
        userRepository.setSignedIn(FakeUserRepository.TEST_USER)
        launchApp()
        enterStock()
        createMedicine("Doliprane")

        composeRule.onNodeWithText("Doliprane").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(string(R.string.detail_delete_medicine)).performClick()
        composeRule.onNodeWithText(string(R.string.action_delete)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Doliprane").assertIsNotDisplayed()
    }
}
