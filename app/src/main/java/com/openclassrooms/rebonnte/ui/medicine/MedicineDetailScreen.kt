package com.openclassrooms.rebonnte.ui.medicine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.data.model.HistoryDto
import com.openclassrooms.rebonnte.ui.aisle.AisleViewModel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Champ vide au depart et apres chaque mouvement : les boutons Retirer et
 * Ajouter sont alors desactives.
 *
 * Repartir de « 1 » serait plus rapide pour un mouvement unitaire, mais
 * laisserait les boutons actifs en permanence. Sur un telephone partage,
 * un doigt qui traine suffirait a produire un mouvement de stock intempestif —
 * et il serait trace dans l'historique comme un mouvement legitime.
 */
private const val EMPTY_QUANTITY = ""

@Composable
fun MedicineDetailScreen(
    medicineId: String,
    medicineViewModel: MedicineViewModel,
    aisleViewModel: AisleViewModel,
    snackbarHostState: SnackbarHostState,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    // remember(medicineId) : sans cela, un nouveau Flow serait cree a chaque
    // recomposition.
    val medicineFlow = remember(medicineId) { medicineViewModel.observeMedicine(medicineId) }
    val historyFlow = remember(medicineId) { medicineViewModel.observeHistory(medicineId) }

    val medicine by medicineFlow.collectAsState(initial = null)
    val histories by historyFlow.collectAsState(initial = emptyList())
    val aisles by aisleViewModel.aisles.collectAsState()

    var quantity by rememberSaveable { mutableStateOf(EMPTY_QUANTITY) }
    var confirmDelete by remember { mutableStateOf(false) }

    val historyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // stringResource n'est appelable que depuis un composable ; le message du
    // snackbar depend de la quantite saisie au moment du clic, donc il se
    // resout via le contexte.
    val context = LocalContext.current

    // L'historique est trie du plus recent au plus ancien : une nouvelle entree
    // apparait en tete. Si l'operateur avait fait defiler la liste, il ne la
    // verrait pas — d'ou le retour en haut quand la plus recente change.
    LaunchedEffect(histories.firstOrNull()?.id) {
        if (histories.isNotEmpty()) {
            historyListState.animateScrollToItem(0)
        }
    }

    val currentMedicine = medicine
    if (currentMedicine == null) {
        // L'ancien code faisait un `return` au milieu du composable : ecran
        // blanc sans explication.
        Text(
            text = stringResource(R.string.detail_not_found),
            modifier = modifier.padding(16.dp)
        )
        return
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.detail_delete_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.detail_delete_message, currentMedicine.name)
                    )
                    // Le stock restant est rappele explicitement : supprimer un
                    // medicament encore en stock est une decision qui doit etre
                    // prise en connaissance de cause.
                    if (currentMedicine.stock > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.detail_delete_remaining,
                                currentMedicine.stock
                            ),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    medicineViewModel.deleteMedicine(currentMedicine.id)
                    onDeleted()
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    /**
     * Confirme le mouvement et vide le champ.
     *
     * Sans retour visible, l'operateur doute d'avoir appuye et recommence — un
     * double retrait de cinquante boites passe inapercu jusqu'a l'inventaire.
     *
     * Vider le champ desactive les deux boutons : le geste doit etre repris
     * deliberement, il ne peut pas se repeter par inadvertance.
     */
    fun applyMovement(delta: Int, message: String) {
        medicineViewModel.updateStock(currentMedicine.id, delta)
        quantity = EMPTY_QUANTITY
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Column(modifier = modifier.padding(16.dp)) {
        TextField(
            value = currentMedicine.name,
            onValueChange = {},
            label = { Text(stringResource(R.string.detail_field_name)) },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            // Le medicament ne porte que l'identifiant de son emplacement ; le
            // libelle se resout ici, a l'affichage.
            value = aisles.firstOrNull { it.id == currentMedicine.aisleId }?.name
                ?: stringResource(R.string.detail_unknown_location),
            onValueChange = {},
            label = { Text(stringResource(R.string.detail_field_location)) },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = currentMedicine.stock.toString(),
            onValueChange = {},
            label = { Text(stringResource(R.string.detail_field_stock)) },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Une quantite saisie plutot que des appuis repetes : retirer cinquante
        // boites produit une seule operation, donc une seule entree
        // d'historique.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it.filter(Char::isDigit).take(5) },
                label = { Text(stringResource(R.string.detail_field_quantity)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(120.dp)
            )
            val amount = quantity.toIntOrNull() ?: 0
            OutlinedButton(
                onClick = {
                    applyMovement(
                        -amount,
                        context.getString(R.string.detail_units_removed, amount)
                    )
                },
                enabled = amount > 0
            ) {
                Text(stringResource(R.string.detail_action_remove))
            }
            Button(
                onClick = {
                    applyMovement(
                        amount,
                        context.getString(R.string.detail_units_added, amount)
                    )
                },
                enabled = amount > 0
            ) {
                Text(stringResource(R.string.detail_action_add))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { confirmDelete = true }) {
            Text(
                text = stringResource(R.string.detail_delete_medicine),
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            state = historyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(histories, key = { it.id }) { history ->
                HistoryItem(history = history)
            }
        }
    }
}

@Composable
fun HistoryItem(history: HistoryDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = history.medicineName, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(
                    R.string.history_user,
                    history.userEmail.ifEmpty { stringResource(R.string.history_unknown_user) }
                )
            )
            Text(
                text = stringResource(
                    R.string.history_date,
                    formatHistoryDate(history.date) ?: stringResource(R.string.history_no_date)
                )
            )
            Text(
                text = stringResource(
                    R.string.history_stock,
                    history.stockBefore,
                    history.stockAfter
                )
            )
            Text(text = stringResource(R.string.history_details, history.details))
        }
    }
}

/** null quand la date est absente : le libelle de remplacement est une ressource. */
private fun formatHistoryDate(epochMillis: Long): String? =
    if (epochMillis == 0L) {
        null
    } else {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(epochMillis))
    }
