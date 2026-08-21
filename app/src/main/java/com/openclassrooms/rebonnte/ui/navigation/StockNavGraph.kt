package com.openclassrooms.rebonnte.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.openclassrooms.rebonnte.ui.MainUiState
import com.openclassrooms.rebonnte.ui.MainViewModel
import com.openclassrooms.rebonnte.ui.aisle.AisleDetailScreen
import com.openclassrooms.rebonnte.ui.aisle.AisleScreen
import com.openclassrooms.rebonnte.ui.aisle.AisleViewModel
import com.openclassrooms.rebonnte.ui.auth.AuthScreen
import com.openclassrooms.rebonnte.ui.auth.AuthViewModel
import com.openclassrooms.rebonnte.ui.medicine.MedicineDetailScreen
import com.openclassrooms.rebonnte.ui.medicine.MedicineFormScreen
import com.openclassrooms.rebonnte.ui.medicine.MedicineFormViewModel
import com.openclassrooms.rebonnte.ui.medicine.MedicineScreen
import com.openclassrooms.rebonnte.ui.medicine.MedicineViewModel
import com.openclassrooms.rebonnte.ui.welcome.DeleteAccountControls
import com.openclassrooms.rebonnte.ui.welcome.WelcomeScreen

/**
 * Ramene l'operateur sur l'ecran que sa session impose.
 *
 * Sans session, l'ecran de connexion ; session ouverte mais accueil non valide,
 * l'accueil ; accueil valide, le stock. La pile est videe a chaque fois : apres
 * une deconnexion, le bouton retour ne doit pas ramener sur les ecrans de stock.
 */
@Composable
internal fun SessionRedirect(
    navController: NavHostController,
    state: MainUiState,
    route: String?
) {
    LaunchedEffect(state.user, state.welcomeAcknowledged, route) {
        if (route == null) return@LaunchedEffect

        val target = when {
            state.user == null -> Destinations.AUTH
            !state.welcomeAcknowledged -> Destinations.WELCOME
            Destinations.isOutsideApp(route) -> Destinations.AISLE_LIST
            else -> null
        }
        if (target == null || route == target) return@LaunchedEffect

        navController.navigate(target) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
    }
}

/**
 * A la racine, le retour arriere ferme l'application au lieu de ne rien faire.
 *
 * Sans cela, la pile videe par [SessionRedirect] laissait un ecran noir.
 */
@Composable
internal fun FinishOnBackFromRoot(navController: NavHostController) {
    val activity = LocalContext.current as? Activity
    BackHandler(enabled = navController.previousBackStackEntry == null) {
        activity?.finish()
    }
}

/**
 * Connexion et accueil : les deux ecrans qui precedent l'entree dans le stock.
 *
 * Le ViewModel est recu, **jamais son etat**. Le graphe de navigation n'est
 * construit qu'une fois : un etat passe en parametre y resterait fige sur sa
 * valeur du premier affichage. Au demarrage sans session, `user` vaut `null` ;
 * apres connexion l'accueil s'afficherait vide, faute de relire quoi que ce
 * soit. La collecte doit donc avoir lieu **dans** la destination, ou elle
 * s'abonne reellement a l'etat.
 */
internal fun NavGraphBuilder.sessionDestinations(mainViewModel: MainViewModel) {
    composable(Destinations.AUTH) {
        AuthScreen(viewModel = hiltViewModel<AuthViewModel>())
    }
    composable(Destinations.WELCOME) {
        val state by mainViewModel.uiState.collectAsState()

        state.user?.let { user ->
            WelcomeScreen(
                user = user,
                onContinue = mainViewModel::acknowledgeWelcome,
                onSignOut = mainViewModel::signOut,
                deleteAccount = DeleteAccountControls(
                    isDeleting = state.isDeletingAccount,
                    error = state.deleteAccountError,
                    onDelete = mainViewModel::deleteAccount,
                    onErrorShown = mainViewModel::deleteAccountErrorShown
                )
            )
        }
    }
}

/** Les six ecrans du stock : les deux listes, les deux details, les deux formulaires. */
internal fun NavGraphBuilder.stockDestinations(
    navController: NavHostController,
    medicineViewModel: MedicineViewModel,
    aisleViewModel: AisleViewModel,
    snackbarHostState: SnackbarHostState
) {
    composable(Destinations.AISLE_LIST) {
        AisleScreen(
            viewModel = aisleViewModel,
            onAisleClick = { navController.navigate(Destinations.aisleDetail(it)) }
        )
    }
    composable(Destinations.MEDICINE_LIST) {
        MedicineScreen(
            viewModel = medicineViewModel,
            onMedicineClick = { navController.navigate(Destinations.medicineDetail(it)) }
        )
    }
    composable(Destinations.AISLE_DETAIL) { entry ->
        AisleDetailScreen(
            aisleId = entry.arguments?.getString(Destinations.AISLE_ID_ARG).orEmpty(),
            medicineViewModel = medicineViewModel,
            onMedicineClick = { navController.navigate(Destinations.medicineDetail(it)) }
        )
    }
    // Declaree avant medicine/{id} : sans cela, « new » serait capture comme un
    // identifiant de medicament.
    composable(Destinations.MEDICINE_NEW) {
        MedicineFormScreen(
            viewModel = hiltViewModel<MedicineFormViewModel>(),
            onSaved = { navController.navigateUp() }
        )
    }
    composable(Destinations.MEDICINE_DETAIL) { entry ->
        val medicineId = entry.arguments?.getString(Destinations.MEDICINE_ID_ARG).orEmpty()
        MedicineDetailScreen(
            medicineId = medicineId,
            medicineViewModel = medicineViewModel,
            snackbarHostState = snackbarHostState,
            onDeleted = { navController.navigateUp() },
            onEdit = { navController.navigate(Destinations.medicineEdit(medicineId)) }
        )
    }
    composable(Destinations.MEDICINE_EDIT) {
        MedicineFormScreen(
            viewModel = hiltViewModel<MedicineFormViewModel>(),
            onSaved = { navController.navigateUp() }
        )
    }
}

internal fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
