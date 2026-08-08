package com.openclassrooms.rebonnte

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
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
import com.openclassrooms.rebonnte.ui.MainViewModel
import com.openclassrooms.rebonnte.ui.aisle.AisleDetailScreen
import com.openclassrooms.rebonnte.ui.aisle.AisleScreen
import com.openclassrooms.rebonnte.ui.aisle.AisleViewModel
import com.openclassrooms.rebonnte.ui.auth.AuthScreen
import com.openclassrooms.rebonnte.ui.auth.AuthViewModel
import com.openclassrooms.rebonnte.ui.medicine.MedicineDetailScreen
import com.openclassrooms.rebonnte.ui.medicine.MedicineScreen
import com.openclassrooms.rebonnte.ui.medicine.MedicineViewModel
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
            Toast.makeText(target, "Update reçu", Toast.LENGTH_SHORT).show()
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

    val currentUser by mainViewModel.currentUser.collectAsState()
    val welcomeAcknowledged by mainViewModel.welcomeAcknowledged.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val route = navBackStackEntry?.destination?.route
    val isDetail = Destinations.isDetail(route)
    val isOutsideApp = Destinations.isOutsideApp(route)
    val isMedicineList = route == Destinations.MEDICINE_LIST

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Calcule une seule fois : la valeur initiale de currentUser est lue de
    // maniere synchrone, donc pas de passage eclair par l'ecran de connexion
    // quand une session est deja ouverte.
    val startDestination = remember {
        if (mainViewModel.currentUser.value == null) Destinations.AUTH else Destinations.WELCOME
    }

    /**
     * L'acces au stock est conditionne a une session ouverte *et* validee.
     * popUpTo(0) vide la pile : apres une deconnexion, le bouton retour ne doit
     * pas ramener sur les ecrans de stock.
     */
    LaunchedEffect(currentUser, welcomeAcknowledged, route) {
        val target = when {
            currentUser == null -> Destinations.AUTH
            !welcomeAcknowledged -> Destinations.WELCOME
            route == Destinations.AUTH || route == Destinations.WELCOME ->
                Destinations.AISLE_LIST

            else -> null
        }
        if (target != null && route != target) {
            navController.navigate(target) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    RebonnteTheme {
        Scaffold(
            topBar = {
                if (!isOutsideApp) Column(verticalArrangement = Arrangement.spacedBy((-1).dp)) {
                    TopAppBar(
                        title = { Text(text = titleFor(route)) },
                        navigationIcon = {
                            if (isDetail) {
                                IconButton(onClick = { navController.navigateUp() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Retour"
                                    )
                                }
                            }
                        },
                        actions = {
                            if (isMedicineList) {
                                SortMenu(medicineViewModel)
                            }
                            // Deconnexion accessible en permanence : sur un
                            // telephone partage, l'operateur suivant doit
                            // pouvoir reprendre la main sans chercher.
                            if (!isDetail) {
                                IconButton(onClick = mainViewModel::signOut) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "Se deconnecter"
                                    )
                                }
                            }
                        }
                    )
                    if (isMedicineList) {
                        EmbeddedSearchBar(
                            query = searchQuery,
                            onQueryChange = {
                                searchQuery = it
                                medicineViewModel.filterByName(it)
                            },
                            isSearchActive = isSearchActive,
                            onActiveChanged = { isSearchActive = it }
                        )
                    }
                }
            },
            bottomBar = {
                if (!isDetail && !isOutsideApp) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text("Aisle") },
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
                            label = { Text("Medicine") },
                            selected = route == Destinations.MEDICINE_LIST,
                            onClick = { navController.switchTab(Destinations.MEDICINE_LIST) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (!isDetail && !isOutsideApp) {
                    FloatingActionButton(onClick = {
                        when (route) {
                            Destinations.MEDICINE_LIST ->
                                medicineViewModel.addRandomMedicine(aisleViewModel.aisles.value)

                            Destinations.AISLE_LIST -> aisleViewModel.addRandomAisle()
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                modifier = Modifier.padding(innerPadding),
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
                            onSignOut = mainViewModel::signOut
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
                composable(Destinations.MEDICINE_DETAIL) { entry ->
                    MedicineDetailScreen(
                        medicineId = entry.arguments
                            ?.getString(Destinations.MEDICINE_ID_ARG).orEmpty(),
                        medicineViewModel = medicineViewModel,
                        aisleViewModel = aisleViewModel
                    )
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

private fun titleFor(route: String?): String = when (route) {
    Destinations.AISLE_LIST, Destinations.AISLE_DETAIL -> "Aisle"
    else -> "Medicines"
}

@Composable
private fun SortMenu(medicineViewModel: MedicineViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 8.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Trier")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(x = 0.dp, y = 0.dp)
            ) {
                DropdownMenuItem(
                    onClick = {
                        medicineViewModel.sortByNone()
                        expanded = false
                    },
                    text = { Text("Sort by None") }
                )
                DropdownMenuItem(
                    onClick = {
                        medicineViewModel.sortByName()
                        expanded = false
                    },
                    text = { Text("Sort by Name") }
                )
                DropdownMenuItem(
                    onClick = {
                        medicineViewModel.sortByStock()
                        expanded = false
                    },
                    text = { Text("Sort by Stock") }
                )
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
                    contentDescription = "Fermer la recherche",
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
                        text = "Search",
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
                    contentDescription = "Effacer la recherche",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
