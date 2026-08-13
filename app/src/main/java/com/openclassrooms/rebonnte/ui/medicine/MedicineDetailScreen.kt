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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.ui.component.ErrorState
import com.openclassrooms.rebonnte.ui.component.LoadingState
import com.openclassrooms.rebonnte.ui.model.HistoryUi
import com.openclassrooms.rebonnte.ui.model.MedicineUi
import com.openclassrooms.rebonnte.ui.theme.RebonnteTheme
import kotlinx.coroutines.launch

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

private const val CONTENT_TYPE_HISTORY = "history"

@Composable
fun MedicineDetailScreen(
    medicineId: String,
    medicineViewModel: MedicineViewModel,
    snackbarHostState: SnackbarHostState,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    // remember(medicineId) : sans cela, un nouveau Flow serait cree a chaque
    // recomposition.
    val detailFlow = remember(medicineId) { medicineViewModel.observeDetail(medicineId) }
    val uiState by detailFlow.collectAsState(initial = MedicineDetailUiState())

    var quantity by rememberSaveable { mutableStateOf(EMPTY_QUANTITY) }

    val scope = rememberCoroutineScope()
    // stringResource n'est appelable que depuis un composable ; le message du
    // snackbar depend de la quantite saisie au moment du clic, donc il se
    // resout via le contexte.
    val context = LocalContext.current

    /**
     * La confirmation vient du ViewModel, une fois le mouvement **enregistre**.
     *
     * Sans retour visible, l'operateur doute d'avoir appuye et recommence — un
     * double retrait de cinquante boites passe inapercu jusqu'a l'inventaire.
     *
     * Le champ n'est vide qu'a ce moment-la : un retrait refuse conserve la
     * saisie, l'operateur n'a pas a la retaper. Et le vider desactive les deux
     * boutons, pour que le geste doive etre repris deliberement.
     */
    val confirmation by medicineViewModel.movementConfirmed.collectAsState()
    LaunchedEffect(confirmation) {
        val message = confirmation ?: return@LaunchedEffect
        quantity = EMPTY_QUANTITY
        medicineViewModel.movementConfirmationShown()
        snackbarHostState.showSnackbar(
            context.getString(message.res, *message.args.toTypedArray())
        )
    }

    if (uiState.isLoading) {
        LoadingState(modifier)
        return
    }
    uiState.errorMessage?.let { message ->
        ErrorState(message, modifier)
        return
    }

    val currentMedicine = uiState.medicine
    if (currentMedicine == null) {
        // L'ancien code faisait un `return` au milieu du composable : ecran
        // blanc sans explication. Ce cas ne se confond plus avec un chargement
        // en cours ni avec une lecture qui a echoue.
        Text(
            text = stringResource(R.string.detail_not_found),
            modifier = modifier.padding(16.dp)
        )
        return
    }

    MedicineDetailContent(
        medicine = currentMedicine,
        histories = uiState.histories,
        quantity = quantity,
        onQuantityChange = { quantity = it.filter(Char::isDigit).take(5) },
        onRemove = { medicineViewModel.updateStock(medicineId, -it) },
        onAdd = { medicineViewModel.updateStock(medicineId, it) },
        onDelete = {
            medicineViewModel.deleteMedicine(currentMedicine.id)
            onDeleted()
        },
        modifier = modifier
    )
}

@Composable
fun MedicineDetailContent(
    medicine: MedicineUi,
    histories: List<HistoryUi>,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val historyListState = rememberLazyListState()

    // L'historique est trie du plus recent au plus ancien : une nouvelle entree
    // apparait en tete. Si l'operateur avait fait defiler la liste, il ne la
    // verrait pas — d'ou le retour en haut quand la plus recente change.
    LaunchedEffect(histories.firstOrNull()?.id) {
        if (histories.isNotEmpty()) {
            historyListState.animateScrollToItem(0)
        }
    }

    if (confirmDelete) {
        DeleteMedicineDialog(
            medicine = medicine,
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false }
        )
    }

    Column(modifier = modifier.padding(16.dp)) {
        ReadOnlyField(
            label = stringResource(R.string.detail_field_name),
            value = medicine.name
        )
        Spacer(modifier = Modifier.height(12.dp))
        ReadOnlyField(
            label = stringResource(R.string.detail_field_location),
            // Le libelle arrive deja resolu par le ViewModel : l'ecran n'a plus
            // a croiser la liste des emplacements.
            value = medicine.locationName
                ?: stringResource(R.string.detail_unknown_location)
        )
        Spacer(modifier = Modifier.height(12.dp))
        ReadOnlyField(
            label = stringResource(R.string.detail_field_stock),
            value = medicine.stock.toString()
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
                onValueChange = onQuantityChange,
                label = { Text(stringResource(R.string.detail_field_quantity)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(120.dp)
            )
            val amount = quantity.toIntOrNull() ?: 0
            // Les deux boutons restent visiblement inactifs tant qu'aucune
            // quantite n'est saisie — c'est le garde-fou voulu — mais leur
            // libelle doit rester lisible. L'attenuation par defaut de Material
            // (38 %) disparaissait dans le fond sombre.
            OutlinedButton(
                onClick = { onRemove(amount) },
                enabled = amount > 0,
                colors = ButtonDefaults.outlinedButtonColors(
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            ) {
                Text(stringResource(R.string.detail_action_remove))
            }
            Button(
                onClick = { onAdd(amount) },
                enabled = amount > 0,
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor =
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
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
            items(
                items = histories,
                key = { it.id },
                contentType = { CONTENT_TYPE_HISTORY }
            ) { history ->
                HistoryItem(history = history)
            }
        }
    }
}

/**
 * Une donnee en lecture, et non un champ de saisie desactive.
 *
 * Ces trois valeurs etaient des `TextField(enabled = false)`. Material atténue
 * volontairement le contenu desactive — c'est correct pour un champ momentanement
 * indisponible, et faux ici : ce n'est pas une saisie qu'on interdit, c'est
 * **la donnee** que l'ecran est venu montrer. En mode sombre, le stock
 * s'affichait en gris clair sur gris fonce.
 *
 * Les contenus desactives echappent aux exigences de contraste WCAG, justement
 * parce qu'ils ne portent pas d'information utile. Raison de plus pour ne pas
 * s'en servir comme affichage.
 */
@Composable
private fun ReadOnlyField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DeleteMedicineDialog(
    medicine: MedicineUi,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_delete_title)) },
        text = {
            Column {
                Text(stringResource(R.string.detail_delete_message, medicine.name))
                // Le stock restant est rappele explicitement : supprimer un
                // medicament encore en stock est une decision qui doit etre
                // prise en connaissance de cause.
                if (medicine.stock > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.detail_delete_remaining, medicine.stock),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun HistoryItem(history: HistoryUi) {
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
                    history.dateLabel ?: stringResource(R.string.history_no_date)
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

private val previewMedicine = MedicineUi(
    id = "1",
    name = "Doliprane 1000 mg",
    stock = 42,
    aisleId = "standard",
    locationName = "Stockage standard"
)

private val previewHistories = listOf(
    HistoryUi(
        id = "h1",
        medicineName = "Doliprane 1000 mg",
        userEmail = "operateur@rebonnte.fr",
        dateLabel = "12/08/26 09:14",
        stockBefore = 92,
        stockAfter = 42,
        details = "Stock modifie de 92 a 42"
    ),
    // Entree ancienne, sans auteur ni date : le cas que l'historique d'origine
    // produisait, et que l'affichage doit encaisser.
    HistoryUi(
        id = "h2",
        medicineName = "Doliprane 1000 mg",
        userEmail = "",
        dateLabel = null,
        stockBefore = 0,
        stockAfter = 92,
        details = "Medicament cree"
    )
)

@Preview(showBackground = true)
@Composable
private fun MedicineDetailContentPreview() {
    RebonnteTheme {
        MedicineDetailContent(
            medicine = previewMedicine,
            histories = previewHistories,
            quantity = "",
            onQuantityChange = {},
            onRemove = {},
            onAdd = {},
            onDelete = {}
        )
    }
}

/** Quantite saisie : les deux boutons deviennent actifs. */
@Preview(showBackground = true)
@Composable
private fun MedicineDetailContentWithQuantityPreview() {
    RebonnteTheme {
        MedicineDetailContent(
            medicine = previewMedicine.copy(locationName = null),
            histories = emptyList(),
            quantity = "50",
            onQuantityChange = {},
            onRemove = {},
            onAdd = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeleteMedicineDialogPreview() {
    RebonnteTheme {
        DeleteMedicineDialog(medicine = previewMedicine, onConfirm = {}, onDismiss = {})
    }
}
