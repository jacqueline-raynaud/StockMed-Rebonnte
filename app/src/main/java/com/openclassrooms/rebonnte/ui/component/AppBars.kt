package com.openclassrooms.rebonnte.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.data.preferences.ThemeMode
import com.openclassrooms.rebonnte.ui.medicine.MedicineViewModel
import com.openclassrooms.rebonnte.ui.navigation.Destinations
/**
 * Ce que la destination courante impose a l'habillage de l'application.
 *
 * Les quatre drapeaux etaient calcules en tete de [MyApp] et relus un peu
 * partout. Regroupes, ils rendent visible ce qui n'allait pas de soi : la barre
 * du haut reste sur une fiche de detail — c'est elle qui porte la fleche de
 * retour — alors que la barre du bas et le bouton d'ajout disparaissent.
 */
@Immutable
internal data class AppChrome(
    val route: String?,
    val isOffline: Boolean
) {
    val isDetail: Boolean get() = Destinations.isDetail(route)
    val isOutsideApp: Boolean get() = Destinations.isOutsideApp(route)
    val isForm: Boolean get() = Destinations.isForm(route)
    val isMedicineList: Boolean get() = route == Destinations.MEDICINE_LIST

    /** Hors ligne, l'application ne propose plus rien : tout l'habillage tombe. */
    val hidesNavigation: Boolean get() = isDetail || isOutsideApp || isForm || isOffline
    val showsTopBar: Boolean get() = !isOutsideApp && !isForm && !isOffline
}

/** Les trois sorties de la barre du haut. */
@Immutable
internal data class TopBarActions(
    val onBack: () -> Unit,
    val onSignOut: () -> Unit,
    val onThemeSelected: (ThemeMode) -> Unit
)

/**
 * Barre superieure : bandeau hors ligne, titre, et les commandes de l'ecran
 * courant.
 *
 * [medicineViewModel] est recu tel quel, et non son etat deja collecte. C'est
 * volontaire : `uiState` est partage en `WhileSubscribed`, donc l'observer
 * ouvre les ecouteurs Firestore. Collecte plus haut, elle les ouvrait des
 * l'ecran de connexion, ou les regles de securite refusent toute lecture — et
 * l'application se fermait sur un PERMISSION_DENIED. La collecte doit rester
 * **a l'interieur** des gardes `isMedicineList` ci-dessous.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StockTopBar(
    chrome: AppChrome,
    themeMode: ThemeMode,
    medicineViewModel: MedicineViewModel,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    actions: TopBarActions
) {
    Column {
        if (chrome.isOffline) {
            OfflineBanner()
        }
        if (!chrome.showsTopBar) return@Column

        Column(verticalArrangement = Arrangement.spacedBy((-1).dp)) {
            TopAppBar(
                title = { Text(text = stringResource(titleFor(chrome.route))) },
                navigationIcon = { BackIcon(visible = chrome.isDetail, onBack = actions.onBack) },
                actions = {
                    if (chrome.isMedicineList) {
                        val sort by medicineViewModel.uiState.collectAsState()
                        SortMenu(
                            currentSort = sort.sort,
                            onSortSelected = medicineViewModel::sortBy
                        )
                    }
                    ThemeMenu(
                        currentMode = themeMode,
                        onModeSelected = actions.onThemeSelected
                    )
                    SignOutIcon(visible = !chrome.isDetail, onSignOut = actions.onSignOut)
                }
            )
            if (chrome.isMedicineList) {
                val medicineUiState by medicineViewModel.uiState.collectAsState()
                EmbeddedSearchBar(
                    query = medicineUiState.query,
                    onQueryChange = medicineViewModel::filterByName,
                    isSearchActive = isSearchActive,
                    onActiveChanged = onSearchActiveChange
                )
            }
        }
    }
}

/** Les deux onglets. Absents des ecrans de detail, des formulaires et hors ligne. */
@Composable
internal fun StockBottomBar(chrome: AppChrome, onTabSelected: (String) -> Unit) {
    if (chrome.hidesNavigation) return

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_aisles)) },
            selected = chrome.route == Destinations.AISLE_LIST,
            onClick = { onTabSelected(Destinations.AISLE_LIST) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_medicines)) },
            selected = chrome.route == Destinations.MEDICINE_LIST,
            onClick = { onTabSelected(Destinations.MEDICINE_LIST) }
        )
    }
}

/** Le bouton d'ajout : c'est l'onglet courant qui decide de ce qu'il ajoute. */
@Composable
internal fun StockFab(
    chrome: AppChrome,
    onAddMedicine: () -> Unit,
    onAddAisle: () -> Unit
) {
    if (chrome.hidesNavigation) return

    FloatingActionButton(
        onClick = {
            when (chrome.route) {
                // Un formulaire, plus une creation aleatoire.
                Destinations.MEDICINE_LIST -> onAddMedicine()
                Destinations.AISLE_LIST -> onAddAisle()
            }
        }
    ) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
    }
}

@Composable
private fun BackIcon(visible: Boolean, onBack: () -> Unit) {
    if (!visible) return
    IconButton(onClick = onBack) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.action_back)
        )
    }
}

@Composable
private fun SignOutIcon(visible: Boolean, onSignOut: () -> Unit) {
    if (!visible) return
    IconButton(onClick = onSignOut) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = stringResource(R.string.action_sign_out)
        )
    }
}

@StringRes
private fun titleFor(route: String?): Int = when (route) {
    Destinations.AISLE_LIST, Destinations.AISLE_DETAIL -> R.string.title_aisles
    else -> R.string.title_medicines
}
