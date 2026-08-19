package com.openclassrooms.rebonnte

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.openclassrooms.rebonnte.data.preferences.ThemeMode
import com.openclassrooms.rebonnte.ui.AppState
import com.openclassrooms.rebonnte.ui.MainViewModel
import com.openclassrooms.rebonnte.ui.aisle.AddAisleDialogHost
import com.openclassrooms.rebonnte.ui.aisle.AisleViewModel
import com.openclassrooms.rebonnte.ui.component.ActionErrorDialog
import com.openclassrooms.rebonnte.ui.component.AppChrome
import com.openclassrooms.rebonnte.ui.component.OfflineOverlay
import com.openclassrooms.rebonnte.ui.component.StockBottomBar
import com.openclassrooms.rebonnte.ui.component.StockFab
import com.openclassrooms.rebonnte.ui.component.StockTopBar
import com.openclassrooms.rebonnte.ui.component.TopBarActions
import com.openclassrooms.rebonnte.ui.medicine.MedicineViewModel
import com.openclassrooms.rebonnte.ui.navigation.Destinations
import com.openclassrooms.rebonnte.ui.navigation.FinishOnBackFromRoot
import com.openclassrooms.rebonnte.ui.navigation.SessionRedirect
import com.openclassrooms.rebonnte.ui.navigation.sessionDestinations
import com.openclassrooms.rebonnte.ui.navigation.stockDestinations
import com.openclassrooms.rebonnte.ui.navigation.switchTab
import com.openclassrooms.rebonnte.ui.theme.RebonnteTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val route = navBackStackEntry?.destination?.route

    val isOffline = mainUiState.appState == AppState.OFFLINE
    val chrome = AppChrome(route = route, isOffline = isOffline)

    var showAddAisleDialog by remember { mutableStateOf(false) }

    // for all destination for error message
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    val startDestination = remember {
        if (mainViewModel.uiState.value.user == null) Destinations.AUTH else Destinations.WELCOME
    }

    SessionRedirect(navController = navController, state = mainUiState, route = route)
    FinishOnBackFromRoot(navController)
    ActionErrorHost(medicineViewModel = medicineViewModel, aisleViewModel = aisleViewModel)

    RebonnteTheme(darkTheme = mainUiState.themeMode.isDark()) {
        if (showAddAisleDialog) {
            AddAisleDialogHost(
                aisleViewModel = aisleViewModel,
                onClose = { showAddAisleDialog = false }
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                StockTopBar(
                    chrome = chrome,
                    themeMode = mainUiState.themeMode,
                    medicineViewModel = medicineViewModel,
                    isSearchActive = isSearchActive,
                    onSearchActiveChange = { isSearchActive = it },
                    actions = TopBarActions(
                        onBack = navController::navigateUp,
                        onSignOut = mainViewModel::signOut,
                        onThemeSelected = mainViewModel::setThemeMode
                    )
                )
            },
            bottomBar = {
                StockBottomBar(chrome = chrome, onTabSelected = navController::switchTab)
            },
            floatingActionButton = {
                StockFab(
                    chrome = chrome,
                    onAddMedicine = { navController.navigate(Destinations.MEDICINE_NEW) },
                    onAddAisle = { showAddAisleDialog = true }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavHost(
                    // Hors ligne, l'arbre est masque aux services d'accessibilite :
                    // le voile qui le recouvre bloque le doigt, pas TalkBack.
                    modifier = if (isOffline) Modifier.clearAndSetSemantics { } else Modifier,
                    navController = navController,
                    startDestination = startDestination
                ) {
                    sessionDestinations(mainViewModel, mainUiState)
                    stockDestinations(
                        navController = navController,
                        medicineViewModel = medicineViewModel,
                        aisleViewModel = aisleViewModel,
                        snackbarHostState = snackbarHostState
                    )
                }

                OfflineOverlay(isOffline = isOffline)
            }
        }
    }
}

/**
 * Les echecs d'ecriture, quel que soit l'ecran.
 *
 * Une fenetre a acquitter et non un message ephemere : un refus doit etre vu.
 */
@Composable
private fun ActionErrorHost(
    medicineViewModel: MedicineViewModel,
    aisleViewModel: AisleViewModel
) {
    val medicineError by medicineViewModel.actionError.collectAsState()
    val aisleError by aisleViewModel.actionError.collectAsState()

    val message = medicineError ?: aisleError ?: return
    ActionErrorDialog(
        message = message,
        onDismiss = {
            medicineViewModel.actionErrorShown()
            aisleViewModel.actionErrorShown()
        }
    )
}

/** Le mode Systeme suit le reglage du telephone ; les deux autres l'ignorent. */
@Composable
private fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
