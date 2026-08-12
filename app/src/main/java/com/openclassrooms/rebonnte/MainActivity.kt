package com.openclassrooms.rebonnte

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.openclassrooms.rebonnte.ui.AppState
import com.openclassrooms.rebonnte.ui.MainViewModel
import com.openclassrooms.rebonnte.ui.component.OfflineBanner
import com.openclassrooms.rebonnte.ui.component.OfflineContent
import com.openclassrooms.rebonnte.ui.aisle.AisleDetailScreen
import com.openclassrooms.rebonnte.ui.aisle.AisleScreen
import com.openclassrooms.rebonnte.ui.aisle.AisleViewModel
import com.openclassrooms.rebonnte.ui.auth.AuthScreen
import com.openclassrooms.rebonnte.ui.auth.AuthViewModel
import com.openclassrooms.rebonnte.ui.aisle.AddAisleDialog
import com.openclassrooms.rebonnte.ui.medicine.MedicineDetailScreen
import com.openclassrooms.rebonnte.ui.medicine.MedicineFormScreen
import com.openclassrooms.rebonnte.ui.medicine.MedicineFormViewModel
import com.openclassrooms.rebonnte.ui.medicine.MedicineScreen
import com.openclassrooms.rebonnte.ui.medicine.MedicineViewModel
import com.openclassrooms.rebonnte.data.preferences.ThemeMode
import com.openclassrooms.rebonnte.data.repository.MedicineSort
import com.openclassrooms.rebonnte.ui.navigation.Destinations
import com.openclassrooms.rebonnte.ui.theme.RebonnteTheme
import com.openclassrooms.rebonnte.ui.welcome.WelcomeScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val myBroadcastReceiver = MyBroadcastReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
        registerUpdateReceiver()
        scheduleUpdateBroadcast()
    }

    override fun onDestroy() {
        unregisterReceiver(myBroadcastReceiver)
        super.onDestroy()
    }

    /**
     * Un enregistrement unique pour toute la duree de vie de l'Activity, avec
     * un desenregistrement symetrique dans onDestroy.
     *
     * L'ancien code enregistrait un nouveau receiver toutes les 200 ms sans
     * jamais desenregistrer le precedent : startBroadcastReceiver programmait
     * startMyBroadcast, qui rappelait startBroadcastReceiver. Le nombre de
     * receivers vivants croissait indefiniment, chacun retenant l'Activity, et
     * le thread principal etait reveille cinq fois par seconde pour afficher un
     * Toast.
     *
     * ContextCompat.registerReceiver applique le flag d'export sur toutes les
     * versions, la ou l'ancienne branche pre-Tiramisu l'omettait.
     */
    private fun registerUpdateReceiver() {
        ContextCompat.registerReceiver(
            this,
            myBroadcastReceiver,
            IntentFilter(ACTION_UPDATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    /**
     * lifecycleScope est annule avec l'Activity : la closure ne peut plus lui
     * survivre. Le Handler() precedent laissait au contraire son message en
     * file avec une reference vers une Activity potentiellement detruite.
     */
    private fun scheduleUpdateBroadcast() {
        lifecycleScope.launch {
            delay(BROADCAST_DELAY_MS)
            // setPackage restreint la diffusion a notre propre application :
            // un intent implicite serait visible des autres applications du
            // telephone, pour un message qui ne concerne que nous.
            sendBroadcast(Intent(ACTION_UPDATE).setPackage(packageName))
        }
    }

    class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Le contexte fourni par le systeme, et non une reference statique
            // vers l'Activity.
            val target = context ?: return
            Toast.makeText(target, R.string.broadcast_update_received, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val ACTION_UPDATE = "com.rebonnte.ACTION_UPDATE"
        private const val BROADCAST_DELAY_MS = 200L
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp() {
    val navController = rememberNavController()
    // Fournis par Hilt et portes par le ViewModelStore de l'Activity, donc
    // partages par toutes les destinations du graphe. C'est ce qui remplace la
    // reference statique vers MainActivity dont les anciennes Activity de
    // detail avaient besoin.
    val mainViewModel: MainViewModel = hiltViewModel()
    val medicineViewModel: MedicineViewModel = hiltViewModel()
    val aisleViewModel: AisleViewModel = hiltViewModel()

    val mainUiState by mainViewModel.uiState.collectAsState()
    val currentUser = mainUiState.user
    val welcomeAcknowledged = mainUiState.welcomeAcknowledged

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val route = navBackStackEntry?.destination?.route
    val isDetail = Destinations.isDetail(route)
    val isOutsideApp = Destinations.isOutsideApp(route)
    val isMedicineList = route == Destinations.MEDICINE_LIST
    val isForm = Destinations.isForm(route)
    // Hors ligne, la barre d'onglets et le bouton d'ajout disparaissent aussi :
    // rien ne doit rester actionnable au-dessus de l'ecran de blocage.
    val isOffline = mainUiState.appState == AppState.OFFLINE
    val hidesAppBars = isDetail || isOutsideApp || isForm || isOffline

    var showAddAisleDialog by remember { mutableStateOf(false) }
    // Partage par toutes les destinations : il servira aussi aux messages
    // d'erreur reseau (T-24).
    val snackbarHostState = remember { SnackbarHostState() }

    // Seul l'etat « la barre est-elle ouverte » reste ici : c'est de la mise en
    // forme. Le texte cherche, lui, appartient au ViewModel — il pilote la
    // requete envoyee a Firestore.
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    // Calcule une seule fois : la valeur initiale de currentUser est lue de
    // maniere synchrone, donc pas de passage eclair par l'ecran de connexion
    // quand une session est deja ouverte.
    val startDestination = remember {
        if (mainViewModel.uiState.value.user == null) Destinations.AUTH else Destinations.WELCOME
    }

    /**
     * L'acces au stock est conditionne a une session ouverte *et* validee.
     * La pile est videe a chaque bascule : apres une deconnexion, le bouton
     * retour ne doit pas ramener sur les ecrans de stock.
     */
    LaunchedEffect(currentUser, welcomeAcknowledged, route) {
        // Tant que le NavHost n'a pas publie sa destination, route est null et
        // le graphe demarre deja sur la bonne. Naviguer ici relancerait l'effet
        // en boucle : l'interface se fige sans planter.
        if (route == null) return@LaunchedEffect

        val target = when {
            currentUser == null -> Destinations.AUTH
            !welcomeAcknowledged -> Destinations.WELCOME
            route == Destinations.AUTH || route == Destinations.WELCOME ->
                Destinations.AISLE_LIST

            else -> null
        }
        if (target != null && route != target) {
            navController.navigate(target) {
                // Vider la pile en remontant jusqu'a la destination de depart
                // incluse, plutot que popUpTo(0) qui detruit aussi l'entree de
                // graphe et laisse le NavController dans un etat instable.
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    /**
     * Apres une navigation qui vide la pile, il ne reste qu'une seule entree.
     * Le retour systeme la depilerait a son tour et le NavHost n'aurait plus
     * rien a afficher : ecran noir, application vivante mais vide.
     *
     * Quand il n'y a rien en dessous, le retour doit donc fermer l'application.
     * Sur un ecran de detail, previousBackStackEntry existe, ce gestionnaire est
     * desactive et le retour reprend son comportement normal.
     */
    val activity = LocalContext.current as? Activity
    BackHandler(enabled = navController.previousBackStackEntry == null) {
        activity?.finish()
    }

    /**
     * Les echecs d'ecriture s'affichent ou que l'on soit.
     *
     * Ces deux flux ne touchent pas Firestore : les observer en permanence
     * n'ouvre aucun ecouteur, contrairement aux etats d'ecran.
     */
    val medicineActionError by medicineViewModel.actionError.collectAsState()
    val aisleActionError by aisleViewModel.actionError.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(medicineActionError) {
        medicineActionError?.let { message ->
            snackbarHostState.showSnackbar(context.getString(message))
            medicineViewModel.actionErrorShown()
        }
    }
    LaunchedEffect(aisleActionError) {
        aisleActionError?.let { message ->
            snackbarHostState.showSnackbar(context.getString(message))
            aisleViewModel.actionErrorShown()
        }
    }

    // Le mode choisi l'emporte sur celui du telephone ; « Systeme » le suit.
    val darkTheme = when (mainUiState.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    RebonnteTheme(darkTheme = darkTheme) {
        if (showAddAisleDialog) {
            AddAisleDialog(
                onDismiss = { showAddAisleDialog = false },
                onConfirm = aisleViewModel::addAisle
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                // Le bandeau hors ligne s'affiche aussi sur les ecrans sans
                // barre superieure : la connexion et le formulaire de creation
                // en dependent autant que les listes.
                Column {
                    if (isOffline) {
                        OfflineBanner()
                    }

                    if (!isOutsideApp && !isForm && !isOffline) Column(
                        verticalArrangement = Arrangement.spacedBy((-1).dp)
                    ) {
                    TopAppBar(
                        title = { Text(text = stringResource(titleFor(route))) },
                        navigationIcon = {
                            if (isDetail) {
                                IconButton(onClick = { navController.navigateUp() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = stringResource(R.string.action_back)
                                    )
                                }
                            }
                        },
                        actions = {
                            // La collecte est faite ici, et non en tete de
                            // MyApp : `uiState` est partage en WhileSubscribed,
                            // donc l'observer ouvre les ecouteurs Firestore.
                            // Le faire en permanence les ouvrait des l'ecran de
                            // connexion, ou les regles de securite refusent
                            // toute lecture.
                            if (isMedicineList) {
                                val sort by medicineViewModel.uiState
                                    .collectAsState()
                                SortMenu(
                                    currentSort = sort.sort,
                                    onSortSelected = medicineViewModel::sortBy
                                )
                            }
                            ThemeMenu(
                                currentMode = mainUiState.themeMode,
                                onModeSelected = mainViewModel::setThemeMode
                            )
                            // Deconnexion accessible en permanence : sur un
                            // telephone partage, l'operateur suivant doit
                            // pouvoir reprendre la main sans chercher.
                            if (!isDetail) {
                                IconButton(onClick = mainViewModel::signOut) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = stringResource(R.string.action_sign_out)
                                    )
                                }
                            }
                        }
                    )
                    if (isMedicineList) {
                        val medicineUiState by medicineViewModel.uiState.collectAsState()
                        EmbeddedSearchBar(
                            query = medicineUiState.query,
                            onQueryChange = medicineViewModel::filterByName,
                            isSearchActive = isSearchActive,
                            onActiveChanged = { isSearchActive = it }
                        )
                    }
                    }
                }
            },
            bottomBar = {
                if (!hidesAppBars) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_aisles)) },
                            selected = route == Destinations.AISLE_LIST,
                            onClick = { navController.switchTab(Destinations.AISLE_LIST) }
                        )
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.List,
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(R.string.nav_medicines)) },
                            selected = route == Destinations.MEDICINE_LIST,
                            onClick = { navController.switchTab(Destinations.MEDICINE_LIST) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (!hidesAppBars) {
                    FloatingActionButton(onClick = {
                        when (route) {
                            // Un formulaire, plus une creation aleatoire.
                            Destinations.MEDICINE_LIST ->
                                navController.navigate(Destinations.MEDICINE_NEW)

                            Destinations.AISLE_LIST -> showAddAisleDialog = true
                        }
                    }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.action_add)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                // Une surface opaque cache l'ecran a l'oeil, pas a TalkBack :
                // sans cela le lecteur d'ecran continuerait d'annoncer des
                // stocks que l'on a justement decide de ne pas montrer.
                modifier = if (isOffline) {
                    Modifier.clearAndSetSemantics { }
                } else {
                    Modifier
                },
                navController = navController,
                startDestination = startDestination
            ) {
                composable(Destinations.AUTH) {
                    AuthScreen(viewModel = hiltViewModel<AuthViewModel>())
                }
                composable(Destinations.WELCOME) {
                    // currentUser ne peut pas etre null ici : l'effet de
                    // navigation renvoie sur AUTH des qu'il l'est.
                    currentUser?.let { user ->
                        WelcomeScreen(
                            user = user,
                            onContinue = mainViewModel::acknowledgeWelcome,
                            onSignOut = mainViewModel::signOut,
                            onDeleteAccount = mainViewModel::deleteAccount,
                            isDeletingAccount = mainUiState.isDeletingAccount,
                            deleteAccountError = mainUiState.deleteAccountError,
                            onDeleteAccountErrorShown = mainViewModel::deleteAccountErrorShown
                        )
                    }
                }
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
                // Declaree avant medicine/{id} : sans cela, « new » serait
                // capture comme un identifiant de medicament.
                composable(Destinations.MEDICINE_NEW) {
                    MedicineFormScreen(
                        viewModel = hiltViewModel<MedicineFormViewModel>(),
                        onSaved = { navController.navigateUp() }
                    )
                }
                composable(Destinations.MEDICINE_DETAIL) { entry ->
                    MedicineDetailScreen(
                        medicineId = entry.arguments
                            ?.getString(Destinations.MEDICINE_ID_ARG).orEmpty(),
                        medicineViewModel = medicineViewModel,
                        snackbarHostState = snackbarHostState,
                        onDeleted = { navController.navigateUp() }
                    )
                }
            }

            if (isOffline) {
                // Surface opaque par-dessus la navigation : aucune donnee n'est
                // lisible et aucun appui n'atteint l'ecran en dessous.
                //
                // Le NavHost reste compose plutot que retire : la pile de
                // navigation survit a la coupure, et l'operateur retrouve son
                // ecran au retour du reseau au lieu de repartir de l'accueil.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .pointerInput(Unit) { detectTapGestures { } }
                ) {
                    OfflineContent()
                }
            }
            }
        }
    }
}

/**
 * Bascule d'onglet.
 *
 * Sans popUpTo ni launchSingleTop, chaque appui empilait une destination de
 * plus : au bout de dix allers-retours, il fallait dix retours arriere pour
 * quitter l'application.
 */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@StringRes
private fun titleFor(route: String?): Int = when (route) {
    Destinations.AISLE_LIST, Destinations.AISLE_DETAIL -> R.string.title_aisles
    else -> R.string.title_medicines
}

/**
 * Choix du theme, accessible depuis tous les ecrans de l'application.
 *
 * Trois etats et non deux : « Systeme » suit le telephone, mais on ne peut pas
 * imposer un mode sans connaitre les besoins visuels de l'operateur. Le sombre
 * n'est pas universellement plus lisible.
 */
@Composable
private fun ThemeMenu(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val options = listOf(
        ThemeMode.SYSTEM to R.string.theme_system,
        ThemeMode.LIGHT to R.string.theme_light,
        ThemeMode.DARK to R.string.theme_dark
    )

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.theme_menu)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (mode, labelRes) ->
                DropdownMenuItem(
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    },
                    text = { Text(stringResource(labelRes)) },
                    trailingIcon = {
                        if (mode == currentMode) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = stringResource(R.string.theme_active)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SortMenu(
    currentSort: MedicineSort,
    onSortSelected: (MedicineSort) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Libelles explicites plutot que « Sort by Name » : avec deux sens de tri,
    // il faut dire lequel.
    val options = listOf(
        MedicineSort.NONE to R.string.sort_none,
        MedicineSort.NAME_ASC to R.string.sort_name_asc,
        MedicineSort.NAME_DESC to R.string.sort_name_desc,
        MedicineSort.STOCK_ASC to R.string.sort_stock_asc,
        MedicineSort.STOCK_DESC to R.string.sort_stock_desc
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 8.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.sort_menu)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(x = 0.dp, y = 0.dp)
            ) {
                options.forEach { (criterion, labelRes) ->
                    DropdownMenuItem(
                        onClick = {
                            onSortSelected(criterion)
                            expanded = false
                        },
                        text = { Text(stringResource(labelRes)) },
                        // Le critere actif est coche : sans cela, on ne sait pas
                        // ce qui s'applique.
                        trailingIcon = {
                            if (criterion == currentSort) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = stringResource(R.string.sort_active)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmbeddedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearchActive: Boolean,
    onActiveChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // L'ancienne version gardait en plus un rememberSaveable interne : deux
    // sources de verite pour la meme saisie, qui pouvaient diverger.
    val activeChanged: (Boolean) -> Unit = { active ->
        onQueryChange("")
        onActiveChanged(active)
    }

    val shape: Shape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSearchActive) {
            IconButton(onClick = { activeChanged(false) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.search_close),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                innerTextField()
            }
        )

        if (isSearchActive && query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.search_clear),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
