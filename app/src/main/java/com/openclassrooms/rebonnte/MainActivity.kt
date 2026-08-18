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
import com.openclassrooms.rebonnte.ui.component.ActionErrorDialog
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
import com.openclassrooms.rebonnte.ui.welcome.DeleteAccountControls
import com.openclassrooms.rebonnte.ui.welcome.WelcomeScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * Warning : BroadcastReceiver
 * As it stands, the broadcast receiver is dead code;
 * the application does not receive any external information.
 * Check if the repositories are sending anything; otherwise, delete the code.
 */
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

    private fun registerUpdateReceiver() {
        ContextCompat.registerReceiver(
            this,
            myBroadcastReceiver,
            IntentFilter(ACTION_UPDATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

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

    val isOffline = mainUiState.appState == AppState.OFFLINE
    val hidesAppBars = isDetail || isOutsideApp || isForm || isOffline

    var showAddAisleDialog by remember { mutableStateOf(false) }

    // for all destination for error message
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    val startDestination = remember {
        if (mainViewModel.uiState.value.user == null) Destinations.AUTH else Destinations.WELCOME
    }

    LaunchedEffect(currentUser, welcomeAcknowledged, route) {
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
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    val activity = LocalContext.current as? Activity
    BackHandler(enabled = navController.previousBackStackEntry == null) {
        activity?.finish()
    }


    val medicineActionError by medicineViewModel.actionError.collectAsState()
    val aisleActionError by aisleViewModel.actionError.collectAsState()

    (medicineActionError ?: aisleActionError)?.let { message ->
        ActionErrorDialog(
            message = message,
            onDismiss = {
                medicineViewModel.actionErrorShown()
                aisleViewModel.actionErrorShown()
            }
        )
    }

    val darkTheme = when (mainUiState.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    RebonnteTheme(darkTheme = darkTheme) {
        if (showAddAisleDialog) {
            val newAisleError by aisleViewModel.newAisleError.collectAsState()
            val aisleCreated by aisleViewModel.aisleCreated.collectAsState()

            LaunchedEffect(aisleCreated) {
                if (aisleCreated) {
                    showAddAisleDialog = false
                    aisleViewModel.aisleCreatedShown()
                }
            }

            AddAisleDialog(
                onDismiss = {
                    showAddAisleDialog = false
                    aisleViewModel.clearNewAisleError()
                },
                onConfirm = aisleViewModel::addAisle,
                errorMessage = newAisleError,
                onNameChange = aisleViewModel::clearNewAisleError
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
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
                            // Collection happens here, rather than at the top level of
                            // MyApp: `uiState` is shared using `WhileSubscribed`,
                            // so observing it opens the Firestore listeners.
                            // Doing it at the top level opened them as early as the
                            // login screen, where security rules deny all read operations.
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
                    currentUser?.let { user ->
                        WelcomeScreen(
                            user = user,
                            onContinue = mainViewModel::acknowledgeWelcome,
                            onSignOut = mainViewModel::signOut,
                            deleteAccount = DeleteAccountControls(
                                isDeleting = mainUiState.isDeletingAccount,
                                error = mainUiState.deleteAccountError,
                                onDelete = mainViewModel::deleteAccount,
                                onErrorShown = mainViewModel::deleteAccountErrorShown
                            )
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
                    val medicineId = entry.arguments
                        ?.getString(Destinations.MEDICINE_ID_ARG).orEmpty()
                    MedicineDetailScreen(
                        medicineId = medicineId,
                        medicineViewModel = medicineViewModel,
                        snackbarHostState = snackbarHostState,
                        onDeleted = { navController.navigateUp() },
                        onEdit = {
                            navController.navigate(Destinations.medicineEdit(medicineId))
                        }
                    )
                }

                composable(Destinations.MEDICINE_EDIT) {
                    MedicineFormScreen(
                        viewModel = hiltViewModel<MedicineFormViewModel>(),
                        onSaved = { navController.navigateUp() }
                    )
                }
            }

            if (isOffline) {

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
